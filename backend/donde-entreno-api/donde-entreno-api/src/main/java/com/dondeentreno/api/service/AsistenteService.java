package com.dondeentreno.api.service;

import com.dondeentreno.api.asistente.AsistenteProperties;
import com.dondeentreno.api.asistente.FiltrosResueltos;
import com.dondeentreno.api.asistente.InterpretacionRemota;
import com.dondeentreno.api.asistente.LimitadorConsultas;
import com.dondeentreno.api.asistente.MotorAsistenteRemoto;
import com.dondeentreno.api.asistente.ResolutorConsulta;
import com.dondeentreno.api.dto.ActividadDTO;
import com.dondeentreno.api.dto.AsistenteEnlaceDTO;
import com.dondeentreno.api.dto.AsistenteRespuestaDTO;
import com.dondeentreno.api.dto.BarrioDTO;
import com.dondeentreno.api.dto.CategoriaDeportivaDTO;
import com.dondeentreno.api.dto.DeporteDTO;
import com.dondeentreno.api.dto.FiltroOpcionesDTO;
import com.dondeentreno.api.exception.ConsultaAsistenteInvalidaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Asistente del lado del servidor.
 *
 * Traduce el mensaje a filtros del catálogo real, corre la búsqueda de
 * verdad y arma una respuesta que solo afirma lo que la base respalda.
 *
 * Reglas que sostienen todo el bloque:
 * - Ningún enlace se escribe fuera de acá, y siempre a partir de slugs
 *   que salieron de la base.
 * - Ningún número sale de otro lado que no sea el resultado de la
 *   búsqueda. Si no hay actividades, se dice; no se maquilla.
 * - El modelo remoto SOLO traduce a términos del catálogo, y esos
 *   términos vuelven a pasar por el resolutor determinístico. No escribe
 *   una sola palabra de lo que lee el usuario, ni un solo enlace.
 */
@Service
public class AsistenteService {

    private static final Logger log = LoggerFactory.getLogger(AsistenteService.class);

    /** Cuántas actividades se nombran como ejemplo en la respuesta. */
    private static final int EJEMPLOS_EN_RESPUESTA = 2;

    private static final String FUENTE_LOCAL = "local";
    private static final String FUENTE_GEMINI = "gemini";

    private final FiltroService filtroService;
    private final ActividadService actividadService;
    private final ResolutorConsulta resolutor;
    private final AsistenteProperties propiedades;
    private final MotorAsistenteRemoto motorRemoto;
    private final LimitadorConsultas limitador;

    public AsistenteService(
            FiltroService filtroService,
            ActividadService actividadService,
            ResolutorConsulta resolutor,
            AsistenteProperties propiedades,
            MotorAsistenteRemoto motorRemoto,
            LimitadorConsultas limitador
    ) {
        this.filtroService = filtroService;
        this.actividadService = actividadService;
        this.resolutor = resolutor;
        this.propiedades = propiedades;
        this.motorRemoto = motorRemoto;
        this.limitador = limitador;
    }

    /**
     * Responde una consulta del asistente.
     *
     * @param texto mensaje del usuario.
     * @return respuesta con texto, enlaces internos y opciones rápidas.
     */
    public AsistenteRespuestaDTO responder(String texto) {
        String consulta = validarEntrada(texto);

        FiltroOpcionesDTO opciones = filtroService.obtenerOpcionesDeFiltros();
        FiltrosResueltos filtros = resolutor.resolver(consulta, opciones);

        /*
          Metadata mínima y nada más: si se entendió y qué se entendió, que
          son valores del catálogo público. El texto que escribió la
          persona NO se loguea nunca.
        */
        log.info(
                "Asistente: resuelta={} deporte={} categoria={} barrio={}",
                filtros.hayAlgo(),
                filtros.deporteSlug(),
                filtros.categoriaSlug(),
                filtros.barrioId()
        );

        if (filtros.hayAlgo()) {
            return construirRespuesta(filtros, FUENTE_LOCAL);
        }

        /*
          Recién acá entra el modelo, y solo para traducir. Lo que devuelve
          vuelve a pasar por el mismo resolutor determinístico, así que de
          este punto en adelante el camino es idéntico al local: mismas
          búsquedas, mismos enlaces, mismos textos.
        */
        FiltrosResueltos delModelo = interpretarConMotorRemoto(consulta, opciones);

        if (delModelo.hayAlgo()) {
            return construirRespuesta(delModelo, FUENTE_GEMINI);
        }

        return responderSinEntender();
    }

    /**
     * Arma la respuesta a partir de filtros ya validados contra el
     * catálogo, sin importar quién los resolvió.
     */
    private AsistenteRespuestaDTO construirRespuesta(FiltrosResueltos filtros, String fuente) {
        /*
          La búsqueda por filtros no acepta categoría, así que una consulta
          que solo resolvió categoría se responde con el catálogo de esa
          categoría, sin inventar un conteo que no podemos calcular.
        */
        if (filtros.deporteSlug() == null && filtros.categoriaSlug() != null) {
            return responderConCategoria(filtros, fuente);
        }

        List<ActividadDTO> encontradas = buscar(filtros);

        if (!encontradas.isEmpty()) {
            return responderConResultados(filtros, encontradas, fuente);
        }

        /*
          Nada en ese barrio: antes de decir que no hay, probamos sin el
          barrio. Es la diferencia entre "no hay yoga" y "no hay yoga en
          Constitución, pero sí en la ciudad".
        */
        if (filtros.barrioId() != null) {
            FiltrosResueltos ampliados = filtros.sinBarrio();
            List<ActividadDTO> enLaCiudad = buscar(ampliados);

            if (!enLaCiudad.isEmpty()) {
                return responderAmpliandoZona(filtros, ampliados, enLaCiudad, fuente);
            }
        }

        return responderSinResultados(filtros, fuente);
    }

    /**
     * Le pide al modelo que traduzca la consulta a términos del catálogo.
     *
     * Tres compuertas antes de gastar: que esté encendido y con
     * credenciales, que quede cuota diaria, y que la llamada no falle. Si
     * cualquiera se cierra, se devuelve vacío y el asistente sigue con lo
     * que sabe hacer solo.
     */
    private FiltrosResueltos interpretarConMotorRemoto(
            String consulta,
            FiltroOpcionesDTO opciones
    ) {
        if (!motorRemoto.estaDisponible()) {
            return FiltrosResueltos.vacio();
        }

        if (!limitador.consumirCuotaGemini()) {
            log.info("Asistente: cuota diaria del modelo agotada, se responde con el motor local.");
            return FiltrosResueltos.vacio();
        }

        Optional<InterpretacionRemota> interpretacion =
                motorRemoto.interpretar(consulta, describirCatalogo(opciones));

        if (interpretacion.isEmpty()) {
            return FiltrosResueltos.vacio();
        }

        /*
          Acá está el candado: los términos que devuelve el modelo se
          resuelven con el MISMO resolutor que la consulta del usuario. Lo
          que invente no matchea contra el catálogo y se descarta solo, sin
          necesidad de lista negra.
        */
        FiltrosResueltos resueltos = resolutor.resolver(
                interpretacion.get().comoFrase(),
                opciones
        );

        log.info(
                "Asistente: el modelo interpreto resuelta={} deporte={} categoria={}",
                resueltos.hayAlgo(),
                resueltos.deporteSlug(),
                resueltos.categoriaSlug()
        );

        return resueltos;
    }

    /**
     * Lista compacta de lo que el modelo puede nombrar. Es todo dato
     * público del catálogo: nada sensible viaja en el prompt.
     */
    private String describirCatalogo(FiltroOpcionesDTO opciones) {
        return "Deportes: " + nombres(opciones.getDeportes(), DeporteDTO::getNombre)
                + "\nCategorias: " + nombres(opciones.getCategorias(), CategoriaDeportivaDTO::getNombre)
                + "\nBarrios: " + nombres(opciones.getBarrios(), BarrioDTO::getNombre)
                + "\nNiveles: " + String.join(", ", opciones.getNiveles())
                + "\nModalidades: " + String.join(", ", opciones.getModalidades());
    }

    private <T> String nombres(List<T> elementos, Function<T, String> nombreDe) {
        if (elementos == null) {
            return "";
        }

        return elementos.stream()
                .map(nombreDe)
                .filter(nombre -> nombre != null && !nombre.isBlank())
                .collect(Collectors.joining(", "));
    }

    private String validarEntrada(String texto) {
        String consulta = texto == null ? "" : texto.trim();

        if (consulta.isEmpty()) {
            throw new ConsultaAsistenteInvalidaException("Escribí una consulta.");
        }

        if (consulta.length() > propiedades.getMaxInputChars()) {
            throw new ConsultaAsistenteInvalidaException(
                    "La consulta no puede superar los "
                            + propiedades.getMaxInputChars() + " caracteres."
            );
        }

        return consulta;
    }

    /**
     * Corre la búsqueda pública real con los filtros entendidos.
     *
     * Devuelve la lista completa porque el catálogo es chico y necesitamos
     * el total exacto para la respuesta. Si algún día crece, acá va una
     * consulta de conteo.
     */
    private List<ActividadDTO> buscar(FiltrosResueltos filtros) {
        return actividadService.buscarActividadesConFiltros(
                null,
                filtros.deporteSlug(),
                null,
                filtros.ciudadSlug(),
                filtros.barrioId(),
                null,
                filtros.nivel(),
                filtros.modalidad(),
                null
        );
    }

    private AsistenteRespuestaDTO responderConResultados(
            FiltrosResueltos filtros,
            List<ActividadDTO> encontradas,
            String fuente
    ) {
        String queEs = describirBusqueda(filtros);
        String donde = describirZona(filtros);
        int total = encontradas.size();

        StringBuilder texto = new StringBuilder();
        texto.append(total == 1 ? "Encontré 1 actividad" : "Encontré " + total + " actividades");
        texto.append(queEs.isEmpty() ? "" : " de " + queEs);
        texto.append(donde.isEmpty() ? "" : " en " + donde);
        texto.append(". ");
        texto.append(nombrarEjemplos(encontradas));
        texto.append("En el detalle de cada una están los horarios, el precio y el contacto directo.");

        return new AsistenteRespuestaDTO(
                texto.toString(),
                List.of(new AsistenteEnlaceDTO(
                        urlExplorar(filtros),
                        etiquetaVer(filtros)
                )),
                List.of(
                        "¿Cómo contacto a un club?",
                        "¿Dónde veo precios y horarios?"
                ),
                fuente
        );
    }

    private AsistenteRespuestaDTO responderAmpliandoZona(
            FiltrosResueltos original,
            FiltrosResueltos ampliados,
            List<ActividadDTO> enLaCiudad,
            String fuente
    ) {
        String queEs = describirBusqueda(original);
        int total = enLaCiudad.size();

        String texto = "En " + original.barrioNombre() + " no encontré "
                + (queEs.isEmpty() ? "actividades" : queEs)
                + ", pero hay "
                + (total == 1 ? "1 actividad" : total + " actividades")
                + " en el resto de la ciudad. "
                + nombrarEjemplos(enLaCiudad)
                + "Podés ver todas y filtrar por la zona que te quede bien.";

        return new AsistenteRespuestaDTO(
                texto,
                List.of(new AsistenteEnlaceDTO(
                        urlExplorar(ampliados),
                        etiquetaVer(ampliados)
                )),
                List.of("¿Cómo filtro por barrio?", "¿Qué deportes hay?"),
                fuente
        );
    }

    private AsistenteRespuestaDTO responderSinResultados(FiltrosResueltos filtros, String fuente) {
        String queEs = describirBusqueda(filtros);

        String texto = "Por ahora no hay actividades"
                + (queEs.isEmpty() ? "" : " de " + queEs)
                + " publicadas"
                + (filtros.barrioNombre() == null ? "" : " en " + filtros.barrioNombre())
                + ". Todavía estamos sumando clubes y profes, así que puede aparecer pronto. "
                + "Mientras tanto podés mirar el catálogo completo.";

        return new AsistenteRespuestaDTO(
                texto,
                List.of(
                        new AsistenteEnlaceDTO("/deportes", "Ver todos los deportes"),
                        new AsistenteEnlaceDTO("/explorar", "Ir a Explorar")
                ),
                List.of("¿Qué deportes hay?", "No sé qué deporte elegir"),
                fuente
        );
    }

    private AsistenteRespuestaDTO responderConCategoria(FiltrosResueltos filtros, String fuente) {
        String texto = "Tenemos varias opciones dentro de "
                + filtros.categoriaNombre().toLowerCase()
                + ". Mirá el catálogo de esa categoría y elegí el deporte que más te tire.";

        return new AsistenteRespuestaDTO(
                texto,
                List.of(new AsistenteEnlaceDTO(
                        "/deportes?categoria=" + codificar(filtros.categoriaSlug()),
                        "Ver " + filtros.categoriaNombre()
                )),
                List.of("No sé qué deporte elegir", "¿Cómo filtro en Explorar?"),
                fuente
        );
    }

    /**
     * Nadie pudo interpretar la consulta: ni el motor local ni el modelo.
     * Se admite, que es mejor que adivinar.
     */
    private AsistenteRespuestaDTO responderSinEntender() {
        return new AsistenteRespuestaDTO(
                "Esa no la tengo del todo clara. ¿Me la contás con otras palabras? "
                        + "Podés nombrarme un deporte, una zona o un nivel, "
                        + "por ejemplo «yoga para principiantes en Centro».",
                List.of(
                        new AsistenteEnlaceDTO("/explorar", "Ir a Explorar"),
                        new AsistenteEnlaceDTO("/deportes", "Ver todos los deportes")
                ),
                List.of("¿Qué deportes hay?", "No sé qué deporte elegir"),
                FUENTE_LOCAL
        );
    }

    /** "Yoga", "Yoga para principiantes", "actividades presenciales"... */
    private String describirBusqueda(FiltrosResueltos filtros) {
        List<String> partes = new ArrayList<>();

        if (filtros.deporteNombre() != null) {
            partes.add(filtros.deporteNombre());
        }

        if (filtros.nivel() != null) {
            partes.add("nivel " + filtros.nivel().toLowerCase());
        }

        if (filtros.modalidad() != null) {
            partes.add("modalidad " + filtros.modalidad().toLowerCase());
        }

        return String.join(", ", partes);
    }

    private String describirZona(FiltrosResueltos filtros) {
        if (filtros.barrioNombre() != null) {
            return filtros.barrioNombre();
        }

        return filtros.ciudadNombre() == null ? "" : filtros.ciudadNombre();
    }

    /** Nombra un par de actividades reales, nunca inventadas. */
    private String nombrarEjemplos(List<ActividadDTO> encontradas) {
        List<String> titulos = encontradas.stream()
                .map(ActividadDTO::getTitulo)
                .filter(titulo -> titulo != null && !titulo.isBlank())
                .limit(EJEMPLOS_EN_RESPUESTA)
                .toList();

        if (titulos.isEmpty()) {
            return "";
        }

        return "Por ejemplo: " + String.join(" y ", titulos) + ". ";
    }

    /**
     * Arma la URL de Explorar con los mismos parámetros que la página
     * sabe leer (page, orden, ciudadSlug, barrioId, deporteSlug, nivel,
     * modalidad). Un parámetro que la página ignore es un enlace que
     * miente sobre lo que va a mostrar.
     */
    private String urlExplorar(FiltrosResueltos filtros) {
        StringBuilder url = new StringBuilder("/explorar?");

        if (filtros.deporteSlug() != null) {
            url.append("deporteSlug=").append(codificar(filtros.deporteSlug())).append("&");
        }

        if (filtros.ciudadSlug() != null) {
            url.append("ciudadSlug=").append(codificar(filtros.ciudadSlug())).append("&");
        }

        if (filtros.barrioId() != null) {
            url.append("barrioId=").append(filtros.barrioId()).append("&");
        }

        if (filtros.nivel() != null) {
            url.append("nivel=").append(codificar(filtros.nivel())).append("&");
        }

        if (filtros.modalidad() != null) {
            url.append("modalidad=").append(codificar(filtros.modalidad())).append("&");
        }

        return url.append("page=0").toString();
    }

    private String etiquetaVer(FiltrosResueltos filtros) {
        if (filtros.deporteNombre() != null) {
            return "Ver " + filtros.deporteNombre();
        }

        return "Ver actividades";
    }

    private String codificar(String valor) {
        return URLEncoder.encode(valor, StandardCharsets.UTF_8);
    }
}
