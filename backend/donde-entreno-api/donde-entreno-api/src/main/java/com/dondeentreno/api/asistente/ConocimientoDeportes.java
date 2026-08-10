package com.dondeentreno.api.asistente;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.dondeentreno.api.asistente.DeporteConocido.Escala.ALTA;
import static com.dondeentreno.api.asistente.DeporteConocido.Escala.BAJA;
import static com.dondeentreno.api.asistente.DeporteConocido.Escala.MEDIA;

/**
 * Base de conocimiento deportivo del asistente.
 *
 * Responde una sola pregunta: "¿de qué se trata este deporte y para quién
 * es?". NO responde "¿existe en DondeEntreno?" — eso lo contesta el
 * catálogo real de FiltroService, y el cruce se hace en tiempo de
 * respuesta.
 *
 * Por eso hay entradas de deportes que hoy NO están en la base (escalada,
 * baile, caminatas): son recomendación general legítima. Si mañana alguien
 * los carga, el cruce por nombre los reconoce solos, sin tocar este
 * archivo.
 *
 * Al revés también es tolerante: un deporte que esté en la base y no acá
 * sigue funcionando por el camino de búsqueda directa ("busco karate"), lo
 * único que no puede es aparecer en una recomendación abierta. Sumarlo es
 * agregar una línea.
 */
public final class ConocimientoDeportes {

    /*
      El orden de la lista es el desempate final: ante igual puntaje gana
      el que está declarado primero. Así la misma consulta devuelve siempre
      lo mismo.
    */
    private static final List<DeporteConocido> DEPORTES = List.of(
            /* ---- Fitness y entrenamiento ---- */
            new DeporteConocido(
                    "Funcional",
                    "circuitos que cambian todo el tiempo, mezclando fuerza y cardio en grupo",
                    6, ALTA, ALTA, MEDIA, false, true, false, true,
                    List.of("funcional grupal", "entrenamiento funcional", "training funcional")
            ),
            new DeporteConocido(
                    "Cross Training",
                    "alta intensidad y movimientos variados, con un grupo que te empuja",
                    2, ALTA, ALTA, ALTA, false, true, true, true,
                    List.of("crossfit", "cross fit", "entrenamiento cruzado")
            ),
            new DeporteConocido(
                    "Musculación",
                    "rutina de fuerza con pesas, a tu ritmo y fácil de medir",
                    1, MEDIA, BAJA, MEDIA, false, false, false, false,
                    List.of("gym", "gimnasio", "pesas", "fierros", "sala de musculacion")
            ),
            new DeporteConocido(
                    "Entrenamiento personalizado",
                    "un profe solo para vos, con el plan armado según tu estado actual",
                    2, MEDIA, BAJA, BAJA, false, true, false, true,
                    List.of("personal trainer", "entrenador personal", "clases personalizadas")
            ),
            new DeporteConocido(
                    "Calistenia",
                    "fuerza usando el peso del cuerpo, sin necesidad de máquinas",
                    1, MEDIA, MEDIA, MEDIA, false, true, false, true,
                    List.of("peso corporal", "street workout")
            ),

            /* ---- Deportes con raqueta ---- */
            new DeporteConocido(
                    "Pádel",
                    "social, se aprende rápido y engancha desde el primer partido",
                    6, MEDIA, ALTA, MEDIA, false, true, true, true,
                    List.of("paddle", "padle")
            ),
            new DeporteConocido(
                    "Tenis",
                    "uno contra uno, con técnica fina y mucho desplazamiento",
                    2, ALTA, MEDIA, ALTA, false, true, true, true,
                    List.of()
            ),
            new DeporteConocido(
                    "Squash",
                    "cuatro paredes y ritmo altísimo: mucho cardio en poco tiempo",
                    1, ALTA, MEDIA, ALTA, false, false, true, true,
                    List.of()
            ),

            /* ---- Deportes de equipo ---- */
            new DeporteConocido(
                    "Vóley",
                    "puro juego en grupo, sin contacto y con muy buen clima de equipo",
                    5, MEDIA, ALTA, MEDIA, false, true, true, true,
                    List.of("voleibol", "volley", "voly")
            ),
            new DeporteConocido(
                    "Fútbol",
                    "el clásico de siempre: equipo, partido y movimiento constante",
                    3, ALTA, ALTA, ALTA, false, true, true, true,
                    List.of("futbol 5", "futbol 7", "futbol 11", "fulbito", "picado")
            ),
            new DeporteConocido(
                    "Básquet",
                    "ritmo rápido, juego en equipo y mucha coordinación",
                    2, ALTA, ALTA, ALTA, false, true, true, true,
                    List.of("basket", "basketball", "baloncesto")
            ),
            new DeporteConocido(
                    "Hockey",
                    "deporte de equipo con palo y pelota, técnico y muy dinámico",
                    1, ALTA, ALTA, ALTA, false, true, true, true,
                    List.of("hockey sobre cesped")
            ),

            /* ---- Acuáticas ---- */
            new DeporteConocido(
                    "Natación",
                    "la más completa de todas: trabaja todo el cuerpo y cuida las articulaciones",
                    5, MEDIA, BAJA, BAJA, false, false, true, true,
                    List.of("nadar", "pileta", "natacion libre")
            ),
            new DeporteConocido(
                    "Aqua Gym",
                    "gimnasia dentro del agua, suave con el cuerpo y muy llevadera",
                    2, BAJA, MEDIA, BAJA, false, true, false, false,
                    List.of("aquagym", "gimnasia acuatica")
            ),

            /* ---- Bienestar ---- */
            new DeporteConocido(
                    "Yoga",
                    "posturas, respiración y movilidad; ideal para bajar revoluciones",
                    4, BAJA, MEDIA, BAJA, false, true, false, false,
                    List.of("yoga integral", "hatha yoga")
            ),
            new DeporteConocido(
                    "Pilates",
                    "trabajo de core y postura, controlado y de bajo impacto",
                    3, BAJA, MEDIA, BAJA, false, true, false, false,
                    List.of("pilates reformer", "pilates suelo")
            ),
            new DeporteConocido(
                    "Stretching",
                    "elongación guiada: ganás movilidad y soltás tensiones",
                    1, BAJA, MEDIA, BAJA, false, false, false, false,
                    List.of("elongacion", "flexibilidad", "movilidad")
            ),

            /* ---- Aire libre ---- */
            new DeporteConocido(
                    "Running",
                    "salir a correr, solo o en grupo: simple, barato y buenísimo para el aire",
                    3, MEDIA, MEDIA, ALTA, false, false, true, true,
                    List.of("correr", "running grupal", "trote", "salir a correr")
            ),
            new DeporteConocido(
                    "Ciclismo",
                    "kilómetros al aire libre, exigente pero amable con las rodillas",
                    2, MEDIA, MEDIA, BAJA, false, false, true, true,
                    List.of("bici", "bicicleta", "spinning", "mountain bike", "ciclismo de ruta")
            ),

            /* ---- Combate ---- */
            new DeporteConocido(
                    "Boxeo",
                    "descarga pura: guantes, bolsa y muchísimo cardio",
                    2, ALTA, MEDIA, ALTA, true, true, true, true,
                    List.of("box", "boxing")
            ),
            new DeporteConocido(
                    "Kickboxing",
                    "boxeo con patadas: coordinación, potencia y aire",
                    1, ALTA, MEDIA, ALTA, true, true, true, true,
                    List.of("kick boxing", "k1", "kick")
            ),
            new DeporteConocido(
                    "Muay Thai",
                    "el arte de las ocho armas: puños, codos, rodillas y patadas",
                    1, ALTA, MEDIA, ALTA, true, true, true, true,
                    List.of("muaythai", "boxeo tailandes")
            ),
            new DeporteConocido(
                    "MMA",
                    "mezcla golpes y lucha en el piso; exigente y muy técnico",
                    0, ALTA, MEDIA, ALTA, true, true, true, true,
                    List.of("artes marciales mixtas", "vale todo")
            ),

            /* ---- Artes marciales ---- */
            /*
              Social MEDIA y no ALTA, aunque se entrene de a dos: quien pide
              "algo social para conocer gente" no está pidiendo que lo
              estrangulen. Con ALTA se colaba entre las cinco primeras de una
              consulta social donde correspondían escalada o baile.
            */
            new DeporteConocido(
                    "Jiu Jitsu",
                    "lucha en el piso con palancas y estrangulaciones; más técnica que fuerza",
                    1, ALTA, MEDIA, MEDIA, true, true, true, true,
                    List.of("jiujitsu", "bjj", "brazilian jiu jitsu", "submission")
            ),
            new DeporteConocido(
                    "Karate",
                    "arte marcial tradicional: técnica, disciplina y katas",
                    1, MEDIA, MEDIA, MEDIA, true, true, true, false,
                    List.of("karate do", "kyokushin")
            ),
            new DeporteConocido(
                    "Taekwondo",
                    "arte marcial coreano, con mucho trabajo de patadas y flexibilidad",
                    1, MEDIA, MEDIA, MEDIA, true, true, true, false,
                    List.of("tae kwon do", "taekwondo itf", "taekwondo wtf")
            ),
            new DeporteConocido(
                    "Judo",
                    "proyecciones y agarres: fuerza, equilibrio y aprender a caer",
                    1, ALTA, MEDIA, MEDIA, true, true, true, false,
                    List.of()
            ),

            /*
              ---- Deportes SIN actividades en DondeEntreno ----

              No están en el catálogo de la base y por eso el asistente
              nunca va a poder ofrecer un enlace a ellos: se muestran
              declarados como recomendación general. Son los que el
              usuario más pide cuando busca "algo social y variado", así
              que no tenerlos era una respuesta peor, no más segura.
            */
            new DeporteConocido(
                    "Escalada",
                    "cada ruta es un problema distinto: fuerza, cabeza y técnica",
                    3, ALTA, ALTA, MEDIA, false, true, false, false,
                    List.of("boulder", "bouldering", "escalada deportiva", "muro de escalada")
            ),
            new DeporteConocido(
                    "Baile",
                    "danza en grupo: te movés un montón y ni te das cuenta",
                    3, MEDIA, ALTA, MEDIA, false, true, false, true,
                    List.of("danza", "zumba", "ritmos latinos", "salsa", "bachata", "folklore")
            ),
            new DeporteConocido(
                    "Caminatas",
                    "salir a caminar: el arranque más fácil de sostener en el tiempo",
                    2, BAJA, MEDIA, BAJA, false, false, false, true,
                    List.of("caminar", "caminata", "trekking", "senderismo")
            )
    );

    /**
     * Palabras con las que la gente rechaza el combate como grupo entero.
     * "no me gustan los deportes de pelea" tiene que sacar boxeo, MMA,
     * karate y todo lo demás de una, no deporte por deporte.
     */
    private static final List<String> PALABRAS_DE_COMBATE = List.of(
            "pelea", "peleas", "pelear", "peleando",
            "combate", "de combate", "contacto", "contacto fisico",
            "golpes", "golpear", "pegar", "pegarle", "trompadas", "piñas",
            "lucha", "luchar", "marciales", "artes marciales"
    );

    /** Índice nombre/alias normalizado -> deporte, armado una sola vez. */
    private static final Map<String, DeporteConocido> POR_TERMINO = indexar();

    private ConocimientoDeportes() {
    }

    public static List<DeporteConocido> todos() {
        return DEPORTES;
    }

    /** Busca por nombre exacto o alias exacto, ya normalizado. */
    public static Optional<DeporteConocido> porTermino(String termino) {
        if (termino == null || termino.isBlank()) {
            return Optional.empty();
        }

        return Optional.ofNullable(POR_TERMINO.get(ResolutorConsulta.normalizar(termino)));
    }

    /**
     * Todos los deportes nombrados dentro de una frase.
     *
     * Busca cada nombre y cada alias como frase completa con borde de
     * palabra, igual que ResolutorConsulta: nada de subcadenas sueltas, que
     * es lo que en su momento hacía que "mi" resolviera Jiu Jitsu por estar
     * dentro de "submission".
     */
    public static List<DeporteConocido> mencionadosEn(String texto) {
        String normalizado = ResolutorConsulta.normalizar(texto);

        if (normalizado.isBlank()) {
            return List.of();
        }

        String conBordes = " " + normalizado + " ";
        List<DeporteConocido> encontrados = new ArrayList<>();

        for (DeporteConocido deporte : DEPORTES) {
            if (nombresDe(deporte).stream().anyMatch(
                    nombre -> conBordes.contains(" " + nombre + " ")
            )) {
                encontrados.add(deporte);
            }
        }

        return encontrados;
    }

    /** ¿La frase habla de pelear, sin importar de qué deporte? */
    public static boolean mencionaCombate(String texto) {
        String conBordes = " " + ResolutorConsulta.normalizar(texto) + " ";

        return PALABRAS_DE_COMBATE.stream()
                .anyMatch(palabra -> conBordes.contains(" " + palabra + " "));
    }

    /** Nombre canónico normalizado; es la clave con la que se compara todo. */
    public static String claveDe(DeporteConocido deporte) {
        return ResolutorConsulta.normalizar(deporte.nombre());
    }

    private static List<String> nombresDe(DeporteConocido deporte) {
        List<String> nombres = new ArrayList<>();
        nombres.add(ResolutorConsulta.normalizar(deporte.nombre()));

        for (String alias : deporte.alias()) {
            nombres.add(ResolutorConsulta.normalizar(alias));
        }

        /*
          Descartamos términos de menos de tres letras por la misma razón
          que el resolutor: pegan por casualidad demasiado seguido. "MMA"
          es justo el límite y entra.
        */
        return nombres.stream().filter(nombre -> nombre.length() >= 3).toList();
    }

    private static Map<String, DeporteConocido> indexar() {
        Map<String, DeporteConocido> indice = new LinkedHashMap<>();

        for (DeporteConocido deporte : DEPORTES) {
            for (String nombre : nombresDe(deporte)) {
                indice.putIfAbsent(nombre, deporte);
            }
        }

        return Map.copyOf(indice);
    }
}
