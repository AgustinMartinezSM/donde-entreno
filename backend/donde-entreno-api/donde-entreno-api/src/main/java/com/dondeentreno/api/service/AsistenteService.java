package com.dondeentreno.api.service;

import com.dondeentreno.api.asistente.AnalizadorConversacion;
import com.dondeentreno.api.asistente.AsistenteProperties;
import com.dondeentreno.api.asistente.ConocimientoDeportes;
import com.dondeentreno.api.asistente.ConsultaRemota;
import com.dondeentreno.api.asistente.DeporteConocido;
import com.dondeentreno.api.asistente.DeporteSugerido;
import com.dondeentreno.api.asistente.DisponibilidadCatalogo;
import com.dondeentreno.api.asistente.FiltrosResueltos;
import com.dondeentreno.api.asistente.LimitadorConsultas;
import com.dondeentreno.api.asistente.MotorAsistenteRemoto;
import com.dondeentreno.api.asistente.PerfilConversacion;
import com.dondeentreno.api.asistente.RecomendadorDeportes;
import com.dondeentreno.api.asistente.RedactorRespuesta;
import com.dondeentreno.api.asistente.ResolutorConsulta;
import com.dondeentreno.api.asistente.RespuestaModelo;
import com.dondeentreno.api.asistente.SanitizadorTexto;
import com.dondeentreno.api.dto.ActividadDTO;
import com.dondeentreno.api.dto.AsistenteEnlaceDTO;
import com.dondeentreno.api.dto.AsistenteMensajeDTO;
import com.dondeentreno.api.dto.AsistenteRespuestaDTO;
import com.dondeentreno.api.dto.BarrioDTO;
import com.dondeentreno.api.dto.CategoriaDeportivaDTO;
import com.dondeentreno.api.dto.DeporteDTO;
import com.dondeentreno.api.dto.FiltroOpcionesDTO;
import com.dondeentreno.api.exception.ConsultaAsistenteInvalidaException;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Asistente del lado del servidor: coach deportivo local conversacional.
 *
 * Dos caminos, y la diferencia entre ellos es la que el usuario ve como
 * "consejo" contra "esto lo tenemos":
 *
 * 1. BÚSQUEDA. La persona nombró algo concreto ("busco karate"). Se
 *    resuelve contra el catálogo real, se corre la búsqueda de verdad y se
 *    responde con el total y los títulos reales.
 * 2. COACH. La persona pide ayuda para elegir, o corrige lo que le
 *    propusimos. Se arma un perfil con TODA la conversación, se
 *    recomiendan deportes (existan o no en la base) y se aclara cuáles
 *    tienen actividades publicadas.
 *
 * Reglas que sostienen el bloque:
 * - Ningún enlace se escribe fuera de RedactorRespuesta, y siempre a
 *   partir de slugs que salieron de la base.
 * - Ningún número sale de otro lado que no sea el resultado de la
 *   búsqueda. Si no hay actividades, se dice; no se maquilla.
 * - El modelo puede escribir el párrafo de consejo, pero cada deporte que
 *   propone se valida contra el catálogo y contra los rechazos del
 *   usuario, y cada afirmación sobre disponibilidad la escribe el backend.
 */
@Service
public class AsistenteService {

    private static final Logger log = LoggerFactory.getLogger(AsistenteService.class);

    private static final String FUENTE_LOCAL = "local";
    private static final String FUENTE_GEMINI = "gemini";

    /** Mínimo de deportes en una recomendación, para que valga la pena. */
    private static final int MINIMO_SUGERENCIAS = 3;

    private final FiltroService filtroService;
    private final ActividadService actividadService;
    private final ResolutorConsulta resolutor;
    private final AsistenteProperties propiedades;
    private final MotorAsistenteRemoto motorRemoto;
    private final LimitadorConsultas limitador;
    private final AnalizadorConversacion analizador;
    private final RecomendadorDeportes recomendador;
    private final RedactorRespuesta redactor;

    public AsistenteService(
            FiltroService filtroService,
            ActividadService actividadService,
            ResolutorConsulta resolutor,
            AsistenteProperties propiedades,
            MotorAsistenteRemoto motorRemoto,
            LimitadorConsultas limitador,
            AnalizadorConversacion analizador,
            RecomendadorDeportes recomendador,
            RedactorRespuesta redactor
    ) {
        this.filtroService = filtroService;
        this.actividadService = actividadService;
        this.resolutor = resolutor;
        this.propiedades = propiedades;
        this.motorRemoto = motorRemoto;
        this.limitador = limitador;
        this.analizador = analizador;
        this.recomendador = recomendador;
        this.redactor = redactor;
    }

    /**
     * Deja el estado del modelo en el log de arranque.
     *
     * Una línea por instancia, para no tener que esperar a que alguien
     * consulte —ni deducirlo de la ausencia de otras líneas— para saber si
     * Gemini va a entrar. En Render el servicio se reinicia solo (el plan
     * free hace spin down), así que esta línea aparece seguido y sirve para
     * ubicar desde cuándo la instancia está como está.
     */
    @PostConstruct
    void registrarEstadoDelModelo() {
        if (propiedades.geminiDisponible()) {
            log.info(
                    "Asistente: modelo remoto habilitado, tope diario de {} llamadas.",
                    propiedades.getGeminiDailyLimit()
            );
            return;
        }

        log.info(
                "Asistente: modelo remoto NO disponible. Motivo: {}. Se responde solo con el motor local.",
                propiedades.motivoGeminiNoDisponible()
        );
    }

    /**
     * Responde una consulta del asistente.
     *
     * @param texto     mensaje del usuario.
     * @param historial turnos previos que manda el frontend. Puede ser null.
     * @return respuesta con texto, enlaces internos y opciones rápidas.
     */
    public AsistenteRespuestaDTO responder(String texto, List<AsistenteMensajeDTO> historial) {
        String consulta = validarEntrada(texto);

        /*
          Dos vistas del mismo historial, y la diferencia importa: el
          perfil se calcula sobre TODO lo que mandó el frontend, porque
          acordarse de un rechazo es gratis; al modelo solo le van los
          últimos turnos, porque ahí cada carácter cuesta. Calcular el
          perfil sobre lo recortado hacía que "no quiero básquet" se
          olvidara a los pocos mensajes.
        */
        List<AsistenteMensajeDTO> conversacion = limpiarHistorial(historial);
        List<AsistenteMensajeDTO> recientes = ultimosTurnos(conversacion);

        FiltroOpcionesDTO opciones = filtroService.obtenerOpcionesDeFiltros();
        PerfilConversacion perfil = analizador.analizar(consulta, conversacion);
        FiltrosResueltos filtros = sinLoRechazado(resolutor.resolver(consulta, opciones), perfil);

        /*
          Metadata mínima y nada más: si se entendió, qué se entendió (que
          son valores del catálogo público) y el tamaño de la charla. El
          texto que escribió la persona NO se loguea nunca.
        */
        log.info(
                "Asistente: resuelta={} deporte={} categoria={} barrio={} turnos={} rechazos={} preferencias={}",
                filtros.hayAlgo(),
                filtros.deporteSlug(),
                filtros.categoriaSlug(),
                filtros.barrioId(),
                recientes.size(),
                perfil.nombresRechazados().size(),
                perfil.preferencias().size()
        );

        CatalogoDiferido catalogo = new CatalogoDiferido(opciones);

        if (filtros.hayAlgo()) {
            return construirRespuestaDeBusqueda(filtros, perfil, catalogo, FUENTE_LOCAL);
        }

        return responderComoCoach(consulta, recientes, perfil, opciones, catalogo);
    }

    /** Compatibilidad: una consulta suelta es una conversación de un turno. */
    public AsistenteRespuestaDTO responder(String texto) {
        return responder(texto, List.of());
    }

    /* ---------------------------------------------------------------
       Camino 1: la persona nombró algo concreto.
       --------------------------------------------------------------- */

    private AsistenteRespuestaDTO construirRespuestaDeBusqueda(
            FiltrosResueltos filtros,
            PerfilConversacion perfil,
            CatalogoDiferido catalogo,
            String fuente
    ) {
        /*
          La búsqueda por filtros no acepta categoría, así que una consulta
          que solo resolvió categoría se responde con el catálogo de esa
          categoría, sin inventar un conteo que no podemos calcular.
        */
        if (filtros.deporteSlug() == null && filtros.categoriaSlug() != null) {
            return redactor.categoria(filtros, fuente);
        }

        List<ActividadDTO> encontradas = buscar(filtros);

        if (!encontradas.isEmpty()) {
            return redactor.resultados(filtros, encontradas, fuente);
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
                return redactor.ampliandoZona(filtros, ampliados, enLaCiudad, fuente);
            }
        }

        /*
          El deporte existe pero nadie publicó todavía. En V1 esto era un
          callejón sin salida; ahora se aprovecha para ofrecer alternativas
          parecidas que sí tengan actividades.
        */
        PerfilConversacion conEsteDescartado = sumarRechazo(perfil, filtros.deporteNombre());

        return redactor.sinResultados(
                filtros,
                recomendador.recomendar(conEsteDescartado, catalogo.obtener(), MINIMO_SUGERENCIAS),
                perfil,
                fuente
        );
    }

    /* ---------------------------------------------------------------
       Camino 2: hay que ayudar a elegir.
       --------------------------------------------------------------- */

    private AsistenteRespuestaDTO responderComoCoach(
            String consulta,
            List<AsistenteMensajeDTO> historial,
            PerfilConversacion perfil,
            FiltroOpcionesDTO opciones,
            CatalogoDiferido catalogo
    ) {
        Optional<RespuestaModelo> delModelo = consultarModelo(consulta, historial, perfil, opciones, catalogo);

        if (delModelo.isPresent()) {
            Optional<AsistenteRespuestaDTO> conModelo =
                    armarConLoQueDijoElModelo(delModelo.get(), perfil, opciones, catalogo);

            if (conModelo.isPresent()) {
                return conModelo.get();
            }
        }

        /*
          Sin modelo (apagado, sin cuota, caído o con una respuesta que no
          sobrevivió a la validación) el asistente sigue funcionando: la
          recomendación determinística entiende los mismos ejes y respeta
          los mismos rechazos, solo que la redacción es nuestra.
        */
        if (!perfil.sinSenales() || analizador.pideRecomendacion(consulta)) {
            return recomendacionDeterministica(perfil, catalogo);
        }

        return sinEntender();
    }

    /**
     * Toma lo que dijo el modelo y lo convierte en respuesta, o devuelve
     * vacío si no quedó nada aprovechable.
     *
     * Acá está el candado del bloque: el texto se sanitiza, los deportes
     * se validan contra el catálogo y contra los rechazos, y los filtros
     * vuelven a pasar por el mismo resolutor determinístico del camino
     * local. Lo que el modelo invente no sobrevive a este método.
     */
    private Optional<AsistenteRespuestaDTO> armarConLoQueDijoElModelo(
            RespuestaModelo respuesta,
            PerfilConversacion perfil,
            FiltroOpcionesDTO opciones,
            CatalogoDiferido catalogo
    ) {
        List<RecomendadorDeportes.NombreYMotivo> propuestos = respuesta.deportesODefecto().stream()
                .map(deporte -> new RecomendadorDeportes.NombreYMotivo(
                        deporte.nombre(),
                        deporte.motivo()
                ))
                .toList();

        /*
          El hábito real del modelo en producción: campo "deportes" vacío
          y la lista entera escrita como enumeración DENTRO del mensaje,
          desde el primer carácter (sin apertura). Recortar la enumeración
          dejaba la prosa vacía y no había nada que aceptar. En vez de
          descartar su elección, la convertimos a la estructura que el
          diseño esperaba: se extraen los ítems del texto y pasan por el
          MISMO validar() que el campo — catálogo, rechazos y dedup en
          código, como siempre. Un ítem mal extraído no matchea nada y se
          cae solo.
        */
        String mensajeLimpio = SanitizadorTexto.limpiarMensaje(respuesta.mensaje());
        String origenPropuestas = "campo";

        if (propuestos.isEmpty()) {
            List<RecomendadorDeportes.NombreYMotivo> deLaProsa =
                    propuestasDeLaEnumeracion(mensajeLimpio);

            if (!deLaProsa.isEmpty()) {
                propuestos = deLaProsa;
                origenPropuestas = "prosa";
            }
        }

        RecomendadorDeportes.ResultadoValidacion validacion = recomendador.validarConDetalle(
                propuestos,
                perfil,
                catalogo.obtener(),
                RecomendadorDeportes.MAXIMO_SUGERENCIAS
        );
        List<DeporteSugerido> sugeridos = validacion.validos();

        if (!sugeridos.isEmpty()) {
            /*
              Si el modelo devolvió menos de lo pedido (o si la mitad
              estaban rechazados) se completa con el recomendador, para no
              dar una respuesta pobre solo porque el modelo fue corto.
            */
            List<DeporteSugerido> completos = recomendador.completar(
                    sugeridos,
                    perfil,
                    catalogo.obtener(),
                    MINIMO_SUGERENCIAS,
                    RecomendadorDeportes.MAXIMO_SUGERENCIAS
            );

            log.info(
                    "Asistente: GEMINI_VALIDADO via=deportes origen={} tipo={} propuestos={} validos={} completados={} rechazosActivos={}",
                    origenPropuestas,
                    campoParaLog(respuesta.tipoRespuesta()),
                    propuestos.size(),
                    sugeridos.size(),
                    completos.size(),
                    perfil.nombresRechazados().size()
            );

            return Optional.of(redactor.recomendacion(
                    introDelModelo(respuesta, perfil),
                    completos,
                    perfil,
                    SanitizadorTexto.limpiarFragmento(respuesta.preguntaSeguimiento()),
                    FUENTE_GEMINI
            ));
        }

        /*
          Sin deportes válidos, todavía puede haber entendido una búsqueda
          concreta que el motor determinístico no captó ("yoga en
          Constitución para arrancar").

          Se resuelve en dos pasos a propósito: "no matcheó nada" y
          "matcheó pero era justo lo rechazado" son diagnósticos distintos.
        */
        FiltrosResueltos resueltosCrudos = resolutor.resolver(respuesta.fraseDeFiltros(), opciones);
        FiltrosResueltos delModelo = sinLoRechazado(resueltosCrudos, perfil);

        if (delModelo.hayAlgo()) {
            log.info(
                    "Asistente: GEMINI_VALIDADO via=filtros tipo={} deporte={} categoria={} rechazosActivos={}",
                    campoParaLog(respuesta.tipoRespuesta()),
                    delModelo.deporteSlug(),
                    delModelo.categoriaSlug(),
                    perfil.nombresRechazados().size()
            );

            return Optional.of(
                    construirRespuestaDeBusqueda(delModelo, perfil, catalogo, FUENTE_GEMINI)
            );
        }

        /*
          Sin deportes válidos ni filtros, queda la prosa. Antes acá se
          descartaba todo, y en producción eso dejó el asistente en modo
          local durante una semana: el modelo empezó a devolver el consejo
          con la lista adentro del mensaje y "deportes" vacío, y su prosa
          —que era buena— se tiraba entera. Ahora se reparte como en el
          resto del V2: la apertura la escribe el modelo (sanitizada, y
          sin la enumeración que haya metido en el texto), los deportes
          los pone el recomendador determinístico, que respeta catálogo y
          rechazos por código.
        */
        String consejo = soloLaApertura(mensajeLimpio);

        /*
          El consejo NO se acepta si el modelo venía insistiendo con lo
          rechazado — deportes descartados por rechazo, o filtros que
          resolvían justo el deporte rechazado—: esa prosa es la más
          propensa a elogiar lo que la persona ya descartó ("el boxeo te
          va a encantar"), y el sanitizador no filtra nombres de
          deportes. Para ese caso se conserva el comportamiento anterior:
          descarte completo y respuesta determinística, cuya apertura
          reconoce el rechazo. El caso del bug real de producción —lista
          vacía con la prosa buena— no trae descartes por rechazo y entra
          igual.
        */
        boolean insistioConLoRechazado =
                !validacion.descartadosPorRechazo().isEmpty() || resueltosCrudos.hayAlgo();

        if (!consejo.isBlank() && !insistioConLoRechazado) {
            List<DeporteSugerido> sugeridosPropios = recomendador.recomendar(
                    perfil,
                    catalogo.obtener(),
                    RecomendadorDeportes.MAXIMO_SUGERENCIAS
            );

            log.info(
                    "Asistente: GEMINI_VALIDADO via=consejo-completado tipo={} propuestos={} porCatalogo={} rechazosActivos={}",
                    campoParaLog(respuesta.tipoRespuesta()),
                    propuestos.size(),
                    listaParaLog(validacion.descartadosPorCatalogo()),
                    perfil.nombresRechazados().size()
            );

            return Optional.of(redactor.recomendacion(
                    consejo,
                    sugeridosPropios,
                    perfil,
                    SanitizadorTexto.limpiarFragmento(respuesta.preguntaSeguimiento()),
                    FUENTE_GEMINI
            ));
        }

        /*
          El modelo respondió y nada sobrevivió: sin esta línea, este
          desenlace era invisible y en producción se confundió primero con
          la cuota y después con una llamada fallida. Solo nombres de
          deportes y metadata; nunca el mensaje del usuario ni la prosa
          del modelo.
        */
        log.info(
                "Asistente: GEMINI_DESCARTADO motivo={} origen={} tipo={} propuestos={} porCatalogo={} porRechazo={} duplicados={} invalidos={} filtrosPropuestos={} filtrosResueltos={} filtrosTrasRechazo=false mensajePresente={} aperturaTrasRecorte={} rechazosActivos={} fuenteFinal=local_fallback",
                motivoDelDescarte(propuestos, validacion, respuesta),
                origenPropuestas,
                campoParaLog(respuesta.tipoRespuesta()),
                listaParaLog(propuestos.stream().map(RecomendadorDeportes.NombreYMotivo::nombre).toList()),
                listaParaLog(validacion.descartadosPorCatalogo()),
                listaParaLog(validacion.descartadosPorRechazo()),
                validacion.duplicados(),
                validacion.invalidos(),
                !respuesta.fraseDeFiltros().isBlank(),
                resueltosCrudos.hayAlgo(),
                !mensajeLimpio.isBlank(),
                !consejo.isBlank(),
                perfil.nombresRechazados().size()
        );

        return Optional.empty();
    }

    /**
     * La causa dominante del descarte, para leer el log sin reconstruirla
     * a mano desde los contadores.
     */
    private String motivoDelDescarte(
            List<RecomendadorDeportes.NombreYMotivo> propuestos,
            RecomendadorDeportes.ResultadoValidacion validacion,
            RespuestaModelo respuesta
    ) {
        if (propuestos.isEmpty()) {
            /*
              El modelo no propuso deportes. Si encima trae mensaje, es el
              caso "respondió consejo general puro y el backend no tiene
              qué validar": importa distinguirlo porque la solución sería
              de diseño, no de datos.
            */
            return respuesta.fraseDeFiltros().isBlank()
                    ? "SIN_DEPORTES_NI_FILTROS"
                    : "SOLO_FILTROS_SIN_MATCH";
        }

        boolean porRechazo = !validacion.descartadosPorRechazo().isEmpty();
        boolean porCatalogo = !validacion.descartadosPorCatalogo().isEmpty();

        if (porRechazo && porCatalogo) {
            return "DEPORTES_RECHAZADOS_Y_FUERA_DE_CATALOGO";
        }

        if (porRechazo) {
            return "DEPORTES_RECHAZADOS";
        }

        if (porCatalogo) {
            return "DEPORTES_FUERA_DE_CATALOGO";
        }

        return "DEPORTES_DUPLICADOS_O_INVALIDOS";
    }

    /**
     * Una lista de nombres propuestos por el modelo, apta para un log:
     * cada nombre aplanado y recortado (el modelo puede devolver cualquier
     * cosa) y la lista limitada, con el resto resumido en un contador.
     */
    private String listaParaLog(List<String> nombres) {
        final int maxItems = 8;

        if (nombres.isEmpty()) {
            return "[]";
        }

        String visibles = nombres.stream()
                .limit(maxItems)
                .map(nombre -> SanitizadorTexto.limpiarParaPrompt(nombre, 30))
                .collect(Collectors.joining(", "));

        int restantes = nombres.size() - maxItems;

        return restantes > 0
                ? "[" + visibles + ", +" + restantes + "]"
                : "[" + visibles + "]";
    }

    /** Un valor suelto que escribió el modelo, aplanado y recortado. */
    private String campoParaLog(String valor) {
        String limpio = SanitizadorTexto.limpiarParaPrompt(valor, 24);

        return limpio.isBlank() ? "-" : limpio;
    }

    /*
      Dónde arranca una enumeración dentro de la prosa. OJO: esto corre
      DESPUÉS de limpiarMensaje, que ya aplanó todos los saltos de línea
      a espacios — por eso no hay una rama con \n, no serviría de nada.

      Se busca "1." o "1)" o "1-" tras inicio, espacio o dos puntos (las
      listas empiezan en uno, y un número suelto en una frase no suele
      venir como "1."), o un bullet real en esas mismas posiciones: ese
      carácter no aparece en prosa, y un mensaje que ES pura lista
      arranca con él en el primer carácter. El guion SOLO se mira pegado
      al uno ("1- Funcional", muy común en castellano): suelto, " - "
      aplanado es un inciso legítimo y no se toca.
    */
    private static final Pattern ARRANQUE_DE_ENUMERACION =
            Pattern.compile("(?:^|[\\s:])(?:1[.)\\-]|•)\\s");

    /**
     * La prosa de apertura, sin la enumeración que el modelo haya metido
     * en el texto.
     *
     * Cuando el modelo escribe la lista dentro del mensaje (el caso que
     * dejó el asistente en modo local), aceptar el mensaje entero
     * duplicaría la lista en pantalla: la suya en prosa y la nuestra
     * estructurada con enlaces. Nos quedamos con lo que haya antes de la
     * enumeración; si el mensaje ERA pura lista, queda vacío y el caller
     * decide.
     */
    private String soloLaApertura(String mensaje) {
        Matcher enumeracion = ARRANQUE_DE_ENUMERACION.matcher(mensaje);

        return enumeracion.find()
                ? mensaje.substring(0, enumeracion.start()).trim()
                : mensaje;
    }

    /*
      Marcador de CUALQUIER ítem de la enumeración (no solo el primero):
      número de uno o dos dígitos con "." ")" o "-", o un bullet, en las
      mismas posiciones que ARRANQUE_DE_ENUMERACION — si los dos patrones
      no ven lo mismo, un formato se recorta de la apertura pero no se
      extrae, o al revés.
    */
    private static final Pattern ITEM_DE_ENUMERACION =
            Pattern.compile("(?:^|[\\s:])(?:\\d{1,2}[.)\\-]|•)\\s");

    /** Más que esto es ruido, no una recomendación. */
    private static final int MAX_ITEMS_EXTRAIDOS = 8;

    /**
     * Los deportes que el modelo escribió como enumeración dentro del
     * mensaje, convertidos en propuestas.
     *
     * Cada ítem tiene la forma "Nombre: motivo" (o solo el nombre). El
     * nombre extraído NO se muestra: pasa por el mismo validar() que el
     * campo "deportes", así que un segmento mal cortado no matchea contra
     * el catálogo y desaparece — extraer de más es gratis, extraer de
     * menos pierde la elección del modelo.
     */
    private List<RecomendadorDeportes.NombreYMotivo> propuestasDeLaEnumeracion(
            String mensajeLimpio
    ) {
        if (mensajeLimpio.isBlank()) {
            return List.of();
        }

        /* El segmento 0 es la apertura (si la hay); el resto, los ítems. */
        String[] segmentos = ITEM_DE_ENUMERACION.split(mensajeLimpio);

        List<RecomendadorDeportes.NombreYMotivo> propuestas = new ArrayList<>();

        for (int indice = 1; indice < segmentos.length; indice++) {
            if (propuestas.size() >= MAX_ITEMS_EXTRAIDOS) {
                break;
            }

            String segmento = segmentos[indice].trim();

            if (segmento.isEmpty()) {
                continue;
            }

            int dosPuntos = segmento.indexOf(':');
            String nombre;
            String motivo;

            /*
              "Funcional: circuitos variados" → nombre y motivo. Un ":"
              muy lejos no separa un nombre de deporte, separa una frase.
            */
            if (dosPuntos > 0 && dosPuntos <= 40) {
                nombre = segmento.substring(0, dosPuntos).trim();
                motivo = segmento.substring(dosPuntos + 1).trim();
            } else {
                nombre = segmento.length() > 40
                        ? segmento.substring(0, 40).trim()
                        : segmento;
                motivo = "";
            }

            if (!nombre.isBlank()) {
                propuestas.add(new RecomendadorDeportes.NombreYMotivo(nombre, motivo));
            }
        }

        return List.copyOf(propuestas);
    }

    /**
     * El párrafo de apertura.
     *
     * Si el modelo escribió algo que sobrevive a la sanitización, va el
     * suyo; si no, el nuestro. Nunca se queda sin apertura, porque la
     * lista de deportes sola se lee como un formulario.
     */
    private String introDelModelo(RespuestaModelo respuesta, PerfilConversacion perfil) {
        /*
          soloLaApertura también acá: el hábito del modelo de repetir la
          lista adentro del mensaje no distingue caminos, y con el esquema
          exigiendo 3-5 deportes este es el camino por el que va a entrar
          casi todo. Sin el recorte, la pantalla mostraba la lista dos
          veces: la del mensaje en prosa (sin filtro de rechazos) y la
          estructurada del backend.
        */
        String limpio = soloLaApertura(SanitizadorTexto.limpiarMensaje(respuesta.mensaje()));

        return limpio.isBlank() ? redactor.introSegunPerfil(perfil) : limpio;
    }

    private AsistenteRespuestaDTO recomendacionDeterministica(
            PerfilConversacion perfil,
            CatalogoDiferido catalogo
    ) {
        List<DeporteSugerido> sugeridos = recomendador.recomendar(
                perfil,
                catalogo.obtener(),
                RecomendadorDeportes.MAXIMO_SUGERENCIAS
        );

        return redactor.recomendacion(
                redactor.introSegunPerfil(perfil),
                sugeridos,
                perfil,
                null,
                FUENTE_LOCAL
        );
    }

    /**
     * Nadie pudo interpretar la consulta. Se admite, que es mejor que
     * adivinar, pero se ofrece la salida concreta.
     */
    private AsistenteRespuestaDTO sinEntender() {
        return new AsistenteRespuestaDTO(
                "Esa no la tengo del todo clara. Contame qué buscás y te doy una mano: "
                        + "puedo ayudarte a elegir un deporte, buscar actividades cerca "
                        + "o explicarte cómo usar la app.",
                List.of(
                        new AsistenteEnlaceDTO("/explorar", "Ir a Explorar"),
                        new AsistenteEnlaceDTO("/deportes", "Ver todos los deportes")
                ),
                List.of("No sé qué entrenar", "Algo social", "Cómo publico una actividad"),
                FUENTE_LOCAL
        );
    }

    /* ---------------------------------------------------------------
       Modelo remoto: tres compuertas antes de gastar.
       --------------------------------------------------------------- */

    private Optional<RespuestaModelo> consultarModelo(
            String consulta,
            List<AsistenteMensajeDTO> historial,
            PerfilConversacion perfil,
            FiltroOpcionesDTO opciones,
            CatalogoDiferido catalogo
    ) {
        if (!motorRemoto.estaDisponible()) {
            /*
              Esta rama salía en silencio, y era la única de las tres: la
              cuota agotada y el fallo de la llamada sí dejaban rastro. El
              resultado era que "el modelo está apagado" y "el modelo falló"
              se veían idénticos en los logs, y distinguirlos obligaba a
              leer el código. El motivo nunca incluye el valor de la key.
            */
            log.info(
                    "Asistente: el modelo no esta disponible. Motivo: {}. Se responde con el motor local.",
                    propiedades.motivoGeminiNoDisponible()
            );
            return Optional.empty();
        }

        if (!limitador.consumirCuotaGemini()) {
            log.info("Asistente: cuota diaria del modelo agotada, se responde con el motor local.");
            return Optional.empty();
        }

        return motorRemoto.conversar(new ConsultaRemota(
                consulta,
                historial,
                describirCatalogo(opciones),
                vocabularioDeportes(perfil),
                perfil.nombresRechazados(),
                conActividadesPublicadas(catalogo.obtener())
        ));
    }

    /**
     * Los deportes que el modelo puede nombrar.
     *
     * Incluye los que NO están en DondeEntreno: recomendar pádel o
     * escalada cuando encajan es mejor que callarse porque nadie los
     * cargó. Lo que no incluye es lo que la persona ya rechazó.
     */
    private String vocabularioDeportes(PerfilConversacion perfil) {
        return ConocimientoDeportes.todos().stream()
                .filter(deporte -> !perfil.rechaza(deporte))
                .map(DeporteConocido::nombre)
                .collect(Collectors.joining(", "));
    }

    private Set<String> conActividadesPublicadas(DisponibilidadCatalogo catalogo) {
        Set<String> nombres = new LinkedHashSet<>();

        for (DisponibilidadCatalogo.EntradaCatalogo entrada : catalogo.porNombre().values()) {
            if (entrada.publicadas() > 0) {
                nombres.add(entrada.nombre());
            }
        }

        return nombres;
    }

    /**
     * Lista compacta de lo que el modelo puede nombrar en "filtros". Es
     * todo dato público del catálogo: nada sensible viaja en el prompt.
     */
    private String describirCatalogo(FiltroOpcionesDTO opciones) {
        return "Deportes: " + nombres(opciones.getDeportes(), DeporteDTO::getNombre)
                + "\nCategorias: " + nombres(opciones.getCategorias(), CategoriaDeportivaDTO::getNombre)
                + "\nBarrios: " + nombres(opciones.getBarrios(), BarrioDTO::getNombre)
                + "\nNiveles: " + unirTextos(opciones.getNiveles())
                + "\nModalidades: " + unirTextos(opciones.getModalidades());
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

    private String unirTextos(List<String> valores) {
        return valores == null ? "" : String.join(", ", valores);
    }

    /* --------------------------- helpers --------------------------- */

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
     * Descarta los mensajes que no sirven y acota el largo de cada uno.
     *
     * El cuerpo lo arma un cliente, así que puede venir con nulls, textos
     * vacíos, autores desconocidos o un mensaje de diez mil caracteres.
     */
    private List<AsistenteMensajeDTO> limpiarHistorial(List<AsistenteMensajeDTO> historial) {
        if (historial == null || historial.isEmpty()) {
            return List.of();
        }

        List<AsistenteMensajeDTO> validos = new ArrayList<>();

        for (AsistenteMensajeDTO mensaje : historial) {
            if (mensaje == null || mensaje.getTexto() == null || mensaje.getTexto().isBlank()) {
                continue;
            }

            if (!mensaje.esDelUsuario() && !mensaje.esDelAsistente()) {
                continue;
            }

            validos.add(new AsistenteMensajeDTO(
                    mensaje.esDelUsuario() ? "usuario" : "asistente",
                    SanitizadorTexto.limpiarParaPrompt(
                            mensaje.getTexto(),
                            propiedades.getMaxInputChars() * 4
                    )
            ));
        }

        return List.copyOf(validos);
    }

    /**
     * Los últimos turnos, que son los únicos que viajan al modelo.
     *
     * El tope existe por tres razones a la vez: el prompt no puede crecer
     * sin control, un historial largo diluye la instrucción de sistema, y
     * cada carácter de más es plata.
     */
    private List<AsistenteMensajeDTO> ultimosTurnos(List<AsistenteMensajeDTO> conversacion) {
        int maximo = Math.max(0, propiedades.getMaxMensajesHistorial());

        if (conversacion.size() <= maximo) {
            return conversacion;
        }

        return List.copyOf(conversacion.subList(conversacion.size() - maximo, conversacion.size()));
    }

    /**
     * Saca del resultado del resolutor lo que la persona ya rechazó.
     *
     * Es el arreglo del peor bug del asistente V1: "no quiero básquet"
     * resolvía el deporte Básquet y respondía con actividades de básquet,
     * porque el resolutor solo ve palabras y no ve la negación. El
     * analizador sí la ve, y acá se aplica.
     */
    private FiltrosResueltos sinLoRechazado(FiltrosResueltos filtros, PerfilConversacion perfil) {
        if (filtros.deporteNombre() == null || !perfil.rechazaNombre(filtros.deporteNombre())) {
            return filtros;
        }

        return new FiltrosResueltos(
                null,
                null,
                filtros.categoriaSlug(),
                filtros.categoriaNombre(),
                filtros.barrioId(),
                filtros.barrioNombre(),
                filtros.ciudadSlug(),
                filtros.ciudadNombre(),
                filtros.nivel(),
                filtros.modalidad()
        );
    }

    /** El deporte que buscó y no tiene actividades no vuelve como alternativa. */
    private PerfilConversacion sumarRechazo(PerfilConversacion perfil, String nombre) {
        if (nombre == null || nombre.isBlank()) {
            return perfil;
        }

        Set<String> rechazados = new LinkedHashSet<>(perfil.deportesRechazados());
        rechazados.add(ResolutorConsulta.normalizar(nombre));

        return new PerfilConversacion(
                Set.copyOf(rechazados),
                perfil.rechazaCombate(),
                perfil.preferencias(),
                perfil.yaSugeridos(),
                perfil.mencionaSalud()
        );
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

    /**
     * La foto de qué hay publicado se arma una sola vez y solo si hace
     * falta.
     *
     * El camino de búsqueda con resultados no la necesita, y es el más
     * común: sin esta demora, cada "busco karate" pagaba una consulta
     * extra que nadie iba a leer.
     */
    private final class CatalogoDiferido {

        private final FiltroOpcionesDTO opciones;
        private DisponibilidadCatalogo calculado;

        private CatalogoDiferido(FiltroOpcionesDTO opciones) {
            this.opciones = opciones;
        }

        private DisponibilidadCatalogo obtener() {
            if (calculado == null) {
                calculado = DisponibilidadCatalogo.desde(
                        opciones.getDeportes(),
                        actividadService.buscarActividadesConFiltros(
                                null, null, null, null, null, null, null, null, null
                        )
                );
            }

            return calculado;
        }
    }
}
