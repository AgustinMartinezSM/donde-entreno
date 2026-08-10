package com.dondeentreno.api.asistente;

import com.dondeentreno.api.asistente.PerfilConversacion.Preferencia;
import com.dondeentreno.api.dto.ActividadDTO;
import com.dondeentreno.api.dto.AsistenteEnlaceDTO;
import com.dondeentreno.api.dto.AsistenteRespuestaDTO;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Escribe TODO lo que ve el usuario.
 *
 * Concentrar la redacción en una sola clase es lo que sostiene la regla
 * del bloque: el modelo puede aportar el párrafo de consejo, pero cada
 * afirmación sobre qué hay en DondeEntreno, cada número y cada enlace se
 * arman acá, a partir de datos que salieron de la base.
 *
 * Reglas de forma, todas del pedido de V2:
 * - párrafos cortos, listas de 3 a 5 deportes con una línea cada uno;
 * - se distingue siempre consejo general de actividades reales;
 * - se cierra con una pregunta para seguir afinando;
 * - las opciones rápidas son pocas, contextuales, y NO aparecen cuando
 *   la respuesta no necesita continuar.
 */
@Component
public class RedactorRespuesta {

    /** Cuántas actividades se nombran como ejemplo. */
    private static final int EJEMPLOS_EN_RESPUESTA = 2;

    /** Tope de enlaces por respuesta: más que esto es una botonera. */
    private static final int MAX_ENLACES = 3;

    /** Tope de opciones rápidas por respuesta (el pedido: 2 o 3). */
    private static final int MAX_OPCIONES = 3;

    private static final AsistenteEnlaceDTO ENLACE_EXPLORAR =
            new AsistenteEnlaceDTO("/explorar", "Ir a Explorar");

    private static final AsistenteEnlaceDTO ENLACE_DEPORTES =
            new AsistenteEnlaceDTO("/deportes", "Ver todos los deportes");

    /**
     * Respuesta de coach: consejo + lista de deportes + qué hay de eso en
     * DondeEntreno + pregunta.
     *
     * @param intro     párrafo de apertura (del modelo o determinístico).
     * @param sugeridos deportes ya validados y cruzados con el catálogo.
     * @param perfil    para elegir las opciones rápidas que faltan saber.
     * @param pregunta  cierre para seguir afinando; puede venir vacío.
     * @param fuente    "local" o "gemini", solo para métricas.
     */
    public AsistenteRespuestaDTO recomendacion(
            String intro,
            List<DeporteSugerido> sugeridos,
            PerfilConversacion perfil,
            String pregunta,
            String fuente
    ) {
        StringBuilder texto = new StringBuilder();

        if (perfil.mencionaSalud()) {
            /*
              Determinístico y primero de todo: no depende de que el modelo
              se haya acordado de decirlo. El asistente no da consejo
              médico y tiene que ser explícito cuando aparece el tema.
            */
            texto.append("Antes que nada: si hay una lesión o algo que te duele de por medio, ")
                    .append("lo mejor es que lo veas con un profesional de la salud antes de arrancar. ")
                    .append("Dicho eso, te tiro opciones de bajo impacto.\n\n");
        }

        if (intro != null && !intro.isBlank()) {
            texto.append(intro.trim()).append("\n\n");
        }

        int numero = 1;

        for (DeporteSugerido sugerido : sugeridos) {
            texto.append(numero).append(". ").append(sugerido.nombre());

            if (sugerido.motivo() != null && !sugerido.motivo().isBlank()) {
                texto.append(": ").append(sugerido.motivo());
            }

            texto.append(terminaEnPunto(sugerido.motivo()) ? "" : ".").append("\n");
            numero += 1;
        }

        String realidad = describirDisponibilidad(sugeridos);

        if (!realidad.isEmpty()) {
            texto.append("\n").append(realidad);
        }

        String cierre = pregunta == null || pregunta.isBlank()
                ? preguntaPorDefecto(perfil)
                : pregunta.trim();

        if (!cierre.isEmpty()) {
            texto.append("\n\n").append(cierre);
        }

        return new AsistenteRespuestaDTO(
                texto.toString().trim(),
                enlacesDe(sugeridos),
                opcionesSegunPerfil(perfil),
                fuente
        );
    }

    /**
     * La línea que separa consejo de realidad.
     *
     * Es la frase más importante de todo el bloque: es la que impide que
     * el usuario crea que hay actividades donde no las hay. Nunca la
     * escribe el modelo.
     */
    private String describirDisponibilidad(List<DeporteSugerido> sugeridos) {
        if (sugeridos.isEmpty()) {
            return "";
        }

        List<String> conActividades = sugeridos.stream()
                .filter(DeporteSugerido::tieneActividades)
                .map(DeporteSugerido::nombre)
                .toList();

        if (conActividades.isEmpty()) {
            return "Te los paso como recomendación general: de estos todavía no hay "
                    + "actividades publicadas en DondeEntreno. Igual podés mirar todo "
                    + "lo que sí hay cargado.";
        }

        StringBuilder linea = new StringBuilder("En DondeEntreno ya hay actividades de ")
                .append(enumerar(conActividades))
                .append(". ");

        if (conActividades.size() < sugeridos.size()) {
            linea.append("El resto va como recomendación general: todavía no hay nada publicado.");
        } else {
            linea.append("Te dejo las búsquedas.");
        }

        return linea.toString();
    }

    /**
     * Un enlace por deporte que tenga actividades de verdad.
     *
     * Deliberadamente no se genera enlace para un deporte sin actividades:
     * mandar a alguien a una búsqueda vacía es peor que no ofrecer el
     * botón. Si no hay ninguno, se ofrece el catálogo.
     */
    private List<AsistenteEnlaceDTO> enlacesDe(List<DeporteSugerido> sugeridos) {
        List<AsistenteEnlaceDTO> enlaces = new ArrayList<>();

        for (DeporteSugerido sugerido : sugeridos) {
            if (enlaces.size() >= MAX_ENLACES) {
                break;
            }

            if (sugerido.tieneActividades()) {
                enlaces.add(new AsistenteEnlaceDTO(
                        "/explorar?deporteSlug=" + codificar(sugerido.slug()) + "&page=0",
                        "Ver " + sugerido.nombre()
                ));
            }
        }

        if (enlaces.isEmpty()) {
            return List.of(ENLACE_EXPLORAR, ENLACE_DEPORTES);
        }

        return List.copyOf(enlaces);
    }

    /**
     * Opciones rápidas: pocas, y solo de lo que todavía no sabemos.
     *
     * En V1 cada respuesta traía cuatro chips y siempre los mismos, así
     * que la conversación terminaba siendo una botonera. Acá se ofrece
     * únicamente el eje que falta definir, y cuando ya sabemos bastante no
     * se ofrece nada: la respuesta se banca sola.
     */
    private List<String> opcionesSegunPerfil(PerfilConversacion perfil) {
        List<String> opciones = new ArrayList<>();

        if (!perfil.quiere(Preferencia.SOCIAL)) {
            opciones.add("Algo social");
        }

        if (!perfil.quiere(Preferencia.TRANQUILO) && !perfil.quiere(Preferencia.INTENSO)) {
            opciones.add("Algo tranqui");
            opciones.add("Algo intenso");
        }

        if (!perfil.rechazaCombate() && !perfil.quiere(Preferencia.VARIEDAD)) {
            opciones.add("Que los ejercicios varíen");
        }

        if (!perfil.rechazaCombate()) {
            opciones.add("Sin deportes de pelea");
        }

        /*
          Perfil ya bastante definido: no hay nada útil que preguntar con
          un botón, así que no se muestra ninguno.
        */
        if (opciones.isEmpty()) {
            return List.of();
        }

        return List.copyOf(opciones.subList(0, Math.min(MAX_OPCIONES, opciones.size())));
    }

    private String preguntaPorDefecto(PerfilConversacion perfil) {
        boolean sabeSocial = perfil.quiere(Preferencia.SOCIAL);
        boolean sabeIntensidad = perfil.quiere(Preferencia.TRANQUILO)
                || perfil.quiere(Preferencia.INTENSO);

        if (!sabeSocial && !sabeIntensidad) {
            return "Si me contás si buscás algo social, tranqui o intenso, te afino la recomendación.";
        }

        if (!sabeIntensidad) {
            return "¿Lo querés bien tranqui o preferís transpirar?";
        }

        if (!sabeSocial) {
            return "¿Preferís algo en grupo o ir por tu cuenta?";
        }

        return "¿Te muestro alguna de estas o seguimos afinando?";
    }

    /* ---------------------------------------------------------------
       Camino de búsqueda concreta: el usuario nombró un deporte y hay
       que decirle qué hay. Nada de esto lo escribe el modelo.
       --------------------------------------------------------------- */

    public AsistenteRespuestaDTO resultados(
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

        /*
          Sin opciones rápidas: la respuesta está completa y el paso
          siguiente es el enlace, no otro botón de charla.
        */
        return new AsistenteRespuestaDTO(
                texto.toString(),
                List.of(new AsistenteEnlaceDTO(urlExplorar(filtros), etiquetaVer(filtros))),
                List.of(),
                fuente
        );
    }

    public AsistenteRespuestaDTO ampliandoZona(
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
                + "Podés verlas todas y filtrar por la zona que te quede cómoda.";

        return new AsistenteRespuestaDTO(
                texto,
                List.of(new AsistenteEnlaceDTO(urlExplorar(ampliados), etiquetaVer(ampliados))),
                List.of(),
                fuente
        );
    }

    /**
     * El deporte existe pero nadie publicó todavía.
     *
     * Con V2 esta respuesta deja de ser un callejón: se aprovecha para
     * proponer alternativas parecidas que sí tengan actividades.
     */
    public AsistenteRespuestaDTO sinResultados(
            FiltrosResueltos filtros,
            List<DeporteSugerido> alternativas,
            PerfilConversacion perfil,
            String fuente
    ) {
        String queEs = describirBusqueda(filtros);

        StringBuilder texto = new StringBuilder("Todavía no hay actividades")
                .append(queEs.isEmpty() ? "" : " de " + queEs)
                .append(" publicadas")
                .append(filtros.barrioNombre() == null ? "" : " en " + filtros.barrioNombre())
                .append(". Seguimos sumando clubes y profes, así que puede aparecer pronto.");

        List<DeporteSugerido> conActividades = alternativas.stream()
                .filter(DeporteSugerido::tieneActividades)
                .limit(3)
                .toList();

        if (conActividades.isEmpty()) {
            return new AsistenteRespuestaDTO(
                    texto + " Mientras tanto podés mirar el catálogo completo.",
                    List.of(ENLACE_DEPORTES, ENLACE_EXPLORAR),
                    opcionesSegunPerfil(perfil),
                    fuente
            );
        }

        texto.append("\n\nSi te sirve una alternativa parecida que sí tenga actividades:\n");

        int numero = 1;

        for (DeporteSugerido sugerido : conActividades) {
            texto.append(numero).append(". ").append(sugerido.nombre())
                    .append(": ").append(sugerido.motivo())
                    .append(terminaEnPunto(sugerido.motivo()) ? "" : ".").append("\n");
            numero += 1;
        }

        return new AsistenteRespuestaDTO(
                texto.toString().trim(),
                enlacesDe(conActividades),
                opcionesSegunPerfil(perfil),
                fuente
        );
    }

    public AsistenteRespuestaDTO categoria(FiltrosResueltos filtros, String fuente) {
        String texto = "Tenemos varias opciones dentro de "
                + filtros.categoriaNombre().toLowerCase()
                + ". Mirá el catálogo de esa categoría y elegí el deporte que más te tire.";

        return new AsistenteRespuestaDTO(
                texto,
                List.of(new AsistenteEnlaceDTO(
                        "/deportes?categoria=" + codificar(filtros.categoriaSlug()),
                        "Ver " + filtros.categoriaNombre()
                )),
                List.of(),
                fuente
        );
    }

    /* ---------------------------------------------------------------
       Textos determinísticos de apertura, para cuando el modelo no está.
       --------------------------------------------------------------- */

    /**
     * Párrafo de apertura sin modelo.
     *
     * No pretende sonar como Gemini: pretende sonar como alguien que te
     * escuchó. Se elige según lo que el perfil marcó, así que "algo social
     * y variado" y "algo tranqui" no abren igual.
     */
    public String introSegunPerfil(PerfilConversacion perfil) {
        /*
          Con un tema de salud, el párrafo de derivación ya hace de apertura
          y cierra con "te tiro opciones de bajo impacto". Agregarle encima
          el "depende de qué estés buscando" genérico sonaba a dos personas
          hablando.
        */
        if (perfil.mencionaSalud() && perfil.preferencias().isEmpty()) {
            return "";
        }

        if (perfil.rechazaCombate() && perfil.quiere(Preferencia.SOCIAL)) {
            return "Listo, saco todo lo que sea contacto o pelea. Con eso afuera, "
                    + "iría por opciones sociales:";
        }

        if (perfil.rechazaCombate()) {
            return "Perfecto, saco todo lo que sea contacto o pelea. Te quedan estas:";
        }

        if (!perfil.deportesRechazados().isEmpty()) {
            return "Va, lo descarto. Probemos por otro lado:";
        }

        if (perfil.quiere(Preferencia.VARIEDAD) && perfil.quiere(Preferencia.SOCIAL)) {
            return "Entonces evitaría todo lo repetitivo y me iría a lo grupal, "
                    + "donde cada clase es distinta:";
        }

        if (perfil.quiere(Preferencia.VARIEDAD)) {
            return "Si lo repetitivo te aburre, apuntaría a algo que cambie seguido:";
        }

        if (perfil.quiere(Preferencia.PROGRESIVO)) {
            return "Vamos de a poco entonces: mejor arrancar por algo que puedas "
                    + "sostener y subir después. Estas opciones son amables al principio:";
        }

        if (perfil.quiere(Preferencia.TRANQUILO)) {
            return "Para bajar un cambio y moverte sin exigirte de más, iría por acá:";
        }

        if (perfil.quiere(Preferencia.INTENSO)) {
            return "Si querés transpirar en serio, estas no fallan:";
        }

        if (perfil.quiere(Preferencia.COMPETITIVO)) {
            return "Si te copa competir, estas tienen partido, marcador y torneos:";
        }

        if (perfil.quiere(Preferencia.SOCIAL)) {
            return "Para conocer gente entrenando, estas son las que mejor funcionan:";
        }

        if (perfil.quiere(Preferencia.AIRE)) {
            return "Para ganar aire lo que mejor funciona es sostener el esfuerzo "
                    + "en el tiempo. Estas van bien:";
        }

        return "Depende de qué estés buscando, pero si querés algo que enganche de "
                + "verdad, yo probaría por acá:";
    }

    /* --------------------------- helpers --------------------------- */

    private boolean terminaEnPunto(String texto) {
        if (texto == null || texto.isBlank()) {
            return true;
        }

        char ultimo = texto.trim().charAt(texto.trim().length() - 1);

        return ultimo == '.' || ultimo == '!' || ultimo == '?';
    }

    private String enumerar(List<String> nombres) {
        if (nombres.size() == 1) {
            return nombres.get(0);
        }

        return String.join(", ", nombres.subList(0, nombres.size() - 1))
                + " y " + nombres.get(nombres.size() - 1);
    }

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
     * Arma la URL de Explorar con los mismos parámetros que la página sabe
     * leer. Un parámetro que la página ignore es un enlace que miente
     * sobre lo que va a mostrar.
     */
    public String urlExplorar(FiltrosResueltos filtros) {
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
