package com.dondeentreno.api.service;

import com.dondeentreno.api.asistente.AsistenteProperties;
import com.dondeentreno.api.asistente.FiltrosResueltos;
import com.dondeentreno.api.asistente.ResolutorConsulta;
import com.dondeentreno.api.dto.ActividadDTO;
import com.dondeentreno.api.dto.AsistenteEnlaceDTO;
import com.dondeentreno.api.dto.AsistenteRespuestaDTO;
import com.dondeentreno.api.dto.FiltroOpcionesDTO;
import com.dondeentreno.api.exception.ConsultaAsistenteInvalidaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Asistente del lado del servidor.
 *
 * Paso A del bloque G: sin Gemini. Traduce el mensaje a filtros del
 * catálogo real, corre la búsqueda de verdad y arma una respuesta que
 * solo afirma lo que la base respalda.
 *
 * Reglas que sostienen todo el bloque:
 * - Ningún enlace se escribe fuera de acá, y siempre a partir de slugs
 *   que salieron de la base.
 * - Ningún número sale de otro lado que no sea el resultado de la
 *   búsqueda. Si no hay actividades, se dice; no se maquilla.
 * - Cuando no se entiende la consulta, se admite. Ese es exactamente el
 *   hueco que va a cubrir Gemini en el paso D (ver responderSinEntender).
 */
@Service
public class AsistenteService {

    private static final Logger log = LoggerFactory.getLogger(AsistenteService.class);

    /** Cuántas actividades se nombran como ejemplo en la respuesta. */
    private static final int EJEMPLOS_EN_RESPUESTA = 2;

    private static final String FUENTE_LOCAL = "local";

    private final FiltroService filtroService;
    private final ActividadService actividadService;
    private final ResolutorConsulta resolutor;
    private final AsistenteProperties propiedades;

    public AsistenteService(
            FiltroService filtroService,
            ActividadService actividadService,
            ResolutorConsulta resolutor,
            AsistenteProperties propiedades
    ) {
        this.filtroService = filtroService;
        this.actividadService = actividadService;
        this.resolutor = resolutor;
        this.propiedades = propiedades;
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

        if (!filtros.hayAlgo()) {
            return responderSinEntender();
        }

        /*
          La búsqueda por filtros no acepta categoría, así que una consulta
          que solo resolvió categoría se responde con el catálogo de esa
          categoría, sin inventar un conteo que no podemos calcular.
        */
        if (filtros.deporteSlug() == null && filtros.categoriaSlug() != null) {
            return responderConCategoria(filtros);
        }

        List<ActividadDTO> encontradas = buscar(filtros);

        if (!encontradas.isEmpty()) {
            return responderConResultados(filtros, encontradas);
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
                return responderAmpliandoZona(filtros, ampliados, enLaCiudad);
            }
        }

        return responderSinResultados(filtros);
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
            List<ActividadDTO> encontradas
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
                FUENTE_LOCAL
        );
    }

    private AsistenteRespuestaDTO responderAmpliandoZona(
            FiltrosResueltos original,
            FiltrosResueltos ampliados,
            List<ActividadDTO> enLaCiudad
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
                FUENTE_LOCAL
        );
    }

    private AsistenteRespuestaDTO responderSinResultados(FiltrosResueltos filtros) {
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
                FUENTE_LOCAL
        );
    }

    private AsistenteRespuestaDTO responderConCategoria(FiltrosResueltos filtros) {
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
                FUENTE_LOCAL
        );
    }

    /**
     * Consulta que el motor local no supo interpretar.
     *
     * Es el único punto de entrada previsto para Gemini (paso D): si el
     * modelo está disponible y queda cuota, acá se lo llama; si no, se
     * devuelve esto mismo. La UI no cambia en ninguno de los dos casos.
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
