/*
  Base de conocimiento declarativa del Asistente DondeEntreno.

  Todo lo que el asistente resuelve SIN RED está acá: intenciones con
  palabras clave, respuestas en español rioplatense, enlaces internos y
  opciones rápidas de seguimiento. El motor (motorLocal.ts) solo normaliza,
  puntúa y elige; el contenido se edita en este archivo sin tocar lógica.

  Qué quedó de este lado después del asistente V2: la ayuda de la app y los
  deportes nombrados de forma directa. Son las consultas que se contestan
  igual sin importar el resto de la charla, así que resolverlas en el
  navegador es instantáneo, gratis y funciona aunque el backend esté caído.
  Todo lo conversacional (preferencias, rechazos, "no sé qué entrenar") lo
  responde el backend, que sí tiene memoria — ver motorCascada.ts.

  Las palabras clave se comparan normalizadas (sin tildes ni mayúsculas),
  usando la misma normalización que el buscador de deportes (deporteSearch).
*/

import type { Deporte } from "../../types/deporte";
import type {
  EnlaceAsistente,
  RespuestaAsistente,
  TipoRespuestaLocal,
} from "./tipos";

/*
  Prioridad de una intención frente a una coincidencia de deporte:
  - "alta": si la intención matchea, gana siempre (ej.: "cómo publico una clase de yoga"
    debe responder sobre publicar, no sobre yoga).
  - "baja": un deporte detectado con puntaje fuerte le gana (ej.: "hola, quiero hacer boxeo"
    debe responder sobre boxeo, no con el saludo).
*/
export type PrioridadIntencion = "alta" | "baja";

export type IntencionAsistente = {
  id: string;
  prioridad: PrioridadIntencion;
  /*
    Qué clase de respuesta es. Lo usa la cascada para decidir si alcanza
    con el navegador o si la consulta merece ir al backend.
  */
  tipo: Extract<
    TipoRespuestaLocal,
    "ayuda-app" | "conversacion" | "recomendacion"
  >;
  // Frases o palabras clave, ya en minúsculas y sin tildes.
  palabrasClave: string[];
  /*
    Frases que solo cuentan si son TODA la entrada.

    Para palabras muy generales que, sueltas, son la consulta entera
    ("deportes"), pero dentro de una frase pertenecen a otra cosa
    ("deportes de combate" es una categoría, no el catálogo completo).
    Sin esto, la intención de prioridad alta ganaba antes de que el motor
    pudiera resolver el deporte o la categoría que el usuario nombró.
  */
  palabrasClaveExactas?: string[];
  respuesta: RespuestaAsistente;
};

/*
  Catálogo estático de deportes para resolver texto libre sin ir a la red.
  Replica los deportes del seed local (mismos slugs que usa deporteSearch para
  sus aliases), con la categoría que les corresponde.
*/

type CategoriaConocimiento = {
  id: number;
  nombre: string;
  slug: string;
};

/*
  Categorías espejo de database/scripts/02_seed_data.sql.
  Mantener slugs e ids alineados con el seed: si un slug no existe en la
  base, los enlaces del asistente llevan a resultados vacíos.
*/
const CATEGORIAS: Record<string, CategoriaConocimiento> = {
  combate: { id: 1, nombre: "Deportes de combate", slug: "deportes-de-combate" },
  artesMarciales: { id: 2, nombre: "Artes marciales", slug: "artes-marciales" },
  fitness: {
    id: 3,
    nombre: "Fitness y entrenamiento",
    slug: "fitness-y-entrenamiento",
  },
  equipo: { id: 4, nombre: "Deportes de equipo", slug: "deportes-de-equipo" },
  acuaticas: {
    id: 5,
    nombre: "Actividades acuáticas",
    slug: "actividades-acuaticas",
  },
  bienestar: { id: 6, nombre: "Bienestar y salud", slug: "bienestar-y-salud" },
  raqueta: { id: 7, nombre: "Deportes con raqueta", slug: "deportes-con-raqueta" },
  aireLibre: {
    id: 8,
    nombre: "Actividades al aire libre",
    slug: "actividades-al-aire-libre",
  },
};

function crearDeporte(
  id: number,
  nombre: string,
  slug: string,
  categoria: CategoriaConocimiento
): Deporte {
  return {
    id,
    nombre,
    slug,
    descripcion: null,
    iconoUrl: null,
    orden: id,
    categoriaId: categoria.id,
    categoriaNombre: categoria.nombre,
    categoriaSlug: categoria.slug,
  };
}

/*
  Catálogo espejo de los deportes reales del seed (27 deportes,
  verificados contra el catálogo de producción el 2026-08-08).
  Los slugs deben existir en la base: son el destino de los enlaces
  /explorar?deporteSlug=... que genera el asistente.
*/
export const CATALOGO_DEPORTES_ASISTENTE: Deporte[] = [
  crearDeporte(1, "Boxeo", "boxeo", CATEGORIAS.combate),
  crearDeporte(2, "Kickboxing", "kickboxing", CATEGORIAS.combate),
  crearDeporte(3, "MMA", "mma", CATEGORIAS.combate),
  crearDeporte(4, "Muay Thai", "muay-thai", CATEGORIAS.combate),
  crearDeporte(5, "Jiu Jitsu", "jiu-jitsu", CATEGORIAS.artesMarciales),
  crearDeporte(6, "Karate", "karate", CATEGORIAS.artesMarciales),
  crearDeporte(7, "Taekwondo", "taekwondo", CATEGORIAS.artesMarciales),
  crearDeporte(8, "Judo", "judo", CATEGORIAS.artesMarciales),
  crearDeporte(9, "Funcional", "funcional", CATEGORIAS.fitness),
  crearDeporte(10, "Musculación", "musculacion", CATEGORIAS.fitness),
  crearDeporte(11, "Cross Training", "cross-training", CATEGORIAS.fitness),
  crearDeporte(
    12,
    "Entrenamiento personalizado",
    "entrenamiento-personalizado",
    CATEGORIAS.fitness
  ),
  crearDeporte(13, "Fútbol", "futbol", CATEGORIAS.equipo),
  crearDeporte(14, "Básquet", "basquet", CATEGORIAS.equipo),
  crearDeporte(15, "Hockey", "hockey", CATEGORIAS.equipo),
  crearDeporte(16, "Vóley", "voley", CATEGORIAS.equipo),
  crearDeporte(17, "Natación", "natacion", CATEGORIAS.acuaticas),
  crearDeporte(18, "Aqua Gym", "aqua-gym", CATEGORIAS.acuaticas),
  crearDeporte(19, "Yoga", "yoga", CATEGORIAS.bienestar),
  crearDeporte(20, "Pilates", "pilates", CATEGORIAS.bienestar),
  crearDeporte(21, "Stretching", "stretching", CATEGORIAS.bienestar),
  crearDeporte(22, "Tenis", "tenis", CATEGORIAS.raqueta),
  crearDeporte(23, "Pádel", "padel", CATEGORIAS.raqueta),
  crearDeporte(24, "Squash", "squash", CATEGORIAS.raqueta),
  crearDeporte(25, "Running", "running", CATEGORIAS.aireLibre),
  crearDeporte(26, "Ciclismo", "ciclismo", CATEGORIAS.aireLibre),
  crearDeporte(27, "Calistenia", "calistenia", CATEGORIAS.aireLibre),
];

/*
  Helpers para armar enlaces internos consistentes.
*/

export function crearEnlaceExplorarDeporte(
  slug: string,
  nombre: string
): EnlaceAsistente {
  return {
    href: `/explorar?deporteSlug=${encodeURIComponent(slug)}&page=0`,
    etiqueta: `Ver ${nombre}`,
  };
}

export function crearEnlaceCategoria(
  valor: string,
  nombre: string
): EnlaceAsistente {
  return {
    href: `/deportes?categoria=${encodeURIComponent(valor)}`,
    etiqueta: `Ver ${nombre}`,
  };
}

const ENLACE_EXPLORAR: EnlaceAsistente = {
  href: "/explorar",
  etiqueta: "Ir a Explorar",
};

const ENLACE_DEPORTES: EnlaceAsistente = {
  href: "/deportes",
  etiqueta: "Ver todos los deportes",
};

/*
  Mensaje de bienvenida que muestra el widget al abrirse.

  Es el único lugar donde se muestran cuatro opciones: es el estado vacío y
  hay que dar de dónde agarrarse. De ahí en adelante los chips son pocos y
  dependen de lo que falte saber.
*/
export const RESPUESTA_BIENVENIDA: RespuestaAsistente = {
  texto:
    "¡Hola! Soy el asistente de DondeEntreno. Te puedo ayudar a elegir qué entrenar, encontrar actividades cerca tuyo o entender cómo usar la app.",
  opcionesRapidas: [
    "No sé qué entrenar",
    "Quiero algo social",
    "Busco algo tranqui",
    "Cómo publico una actividad",
  ],
};

/*
  Respuesta cuando el navegador no entiende la consulta.

  La cascada la reconoce POR IDENTIDAD (es el mismo objeto), así que no hace
  falta comparar textos para saber que hay que preguntarle al backend.
*/
export const RESPUESTA_FALLBACK: RespuestaAsistente = {
  texto:
    "Mmm, esa no la tengo del todo clara. ¿Me lo contás con otras palabras? Por ejemplo, decime un deporte («boxeo», «yoga») o contame qué buscás.",
  enlaces: [ENLACE_EXPLORAR, ENLACE_DEPORTES],
  opcionesRapidas: ["No sé qué entrenar", "Quiero algo social"],
};

/*
  Respuesta cuando el motor resuelve un deporte concreto del catálogo.

  Sin opciones rápidas a propósito: la respuesta ya cierra y el paso
  siguiente es el enlace, no otro botón de charla.
*/
export function crearRespuestaDeporte(deporte: Deporte): RespuestaAsistente {
  return {
    texto: `¡${deporte.nombre} es una gran elección! Mirá las actividades de ${deporte.nombre} en tu ciudad: en el detalle de cada una vas a encontrar precios, horarios, barrio y el contacto directo.`,
    enlaces: [crearEnlaceExplorarDeporte(deporte.slug, deporte.nombre), ENLACE_DEPORTES],
  };
}

/*
  Respuesta cuando la consulta matchea mejor con una categoría (ej.: "artes marciales").
*/
export function crearRespuestaCategoria(
  valor: string,
  nombre: string
): RespuestaAsistente {
  return {
    texto: `Tenemos varias opciones dentro de ${nombre.toLowerCase()}. Mirá el catálogo de esa categoría y elegí el deporte que más te tire.`,
    enlaces: [crearEnlaceCategoria(valor, nombre), ENLACE_EXPLORAR],
  };
}

/*
  Intenciones del asistente. El orden importa: ante un empate de puntaje,
  gana la que está declarada primero.
*/
export const INTENCIONES_ASISTENTE: IntencionAsistente[] = [
  /* ---------------------------------------------------------------
     Recomendación: se contestan acá solo si el backend no está. La
     cascada las manda igual al servidor, que tiene memoria de la charla
     y puede recomendar deportes que todavía no están cargados.
     --------------------------------------------------------------- */
  {
    id: "elegir-deporte",
    prioridad: "alta",
    tipo: "recomendacion",
    palabrasClave: [
      "no se que deporte",
      "no se que entrenar",
      "no se que elegir",
      "no se cual elegir",
      "no se por donde empezar",
      "ayudame a elegir",
      "que me recomendas",
      "que me recomiendas",
      "que me conviene",
      "recomendame",
      "recomendacion",
      "quiero empezar a entrenar",
      "quiero arrancar",
      "estoy indeciso",
      "estoy indecisa",
      "no me decido",
    ],
    /*
      "quiero entrenar boxeo" tiene que responder boxeo, no la guía para
      elegir: la frase suelta pide ayuda, dentro de una oración es el
      arranque de un pedido concreto.
    */
    palabrasClaveExactas: ["quiero entrenar", "quiero hacer deporte"],
    respuesta: {
      texto:
        "¡Te ayudo a elegir! Contame un poco más de vos: ¿preferís entrenar solo o en grupo? ¿Buscás algo intenso para descargar o algo más tranquilo?",
      enlaces: [ENLACE_DEPORTES],
      opcionesRapidas: ["Quiero algo social", "Algo tranqui", "Algo intenso"],
    },
  },
  {
    id: "entrenar-solo",
    prioridad: "alta",
    tipo: "recomendacion",
    palabrasClave: [
      "prefiero entrenar solo",
      "prefiero entrenar sola",
      "entrenar solo",
      "entrenar sola",
      "por mi cuenta",
      "a mi ritmo",
      "algo individual",
      "actividades individuales",
    ],
    respuesta: {
      texto:
        "Si te gusta ir a tu ritmo, estas opciones van muy bien: musculación o funcional para armar tu rutina, running para despejarte, natación si te tira el agua, o yoga para cerrar el día.",
      enlaces: [
        crearEnlaceExplorarDeporte("musculacion", "Musculación"),
        crearEnlaceExplorarDeporte("running", "Running"),
        crearEnlaceExplorarDeporte("natacion", "Natación"),
      ],
      opcionesRapidas: ["Algo intenso", "Algo tranqui"],
    },
  },
  {
    id: "entrenar-grupo",
    prioridad: "alta",
    tipo: "recomendacion",
    palabrasClave: [
      "prefiero entrenar en grupo",
      "entrenar en grupo",
      "en grupo",
      "con gente",
      "con amigos",
      "en equipo",
      "deporte de equipo",
      "clases grupales",
      "algo grupal",
    ],
    respuesta: {
      texto:
        "¡Entrenar con gente motiva un montón! Mirá estas opciones: fútbol y vóley para sumarte a un equipo, o funcional y cross training para transpirar en grupo.",
      enlaces: [
        crearEnlaceExplorarDeporte("futbol", "Fútbol"),
        crearEnlaceExplorarDeporte("voley", "Vóley"),
        crearEnlaceExplorarDeporte("funcional", "Funcional"),
      ],
      opcionesRapidas: ["Algo intenso", "Algo tranqui"],
    },
  },
  {
    id: "intensidad-alta",
    prioridad: "alta",
    tipo: "recomendacion",
    palabrasClave: [
      "quiero algo intenso",
      "algo intenso",
      "alta intensidad",
      "bien exigente",
      "algo exigente",
      "descargar energia",
      "descargarme",
      "transpirar",
      "quemar calorias",
      "ponerme en forma rapido",
      "ponerme en forma",
      /*
        Objetivo muy pedido y sin intención propia: sin estas frases,
        "quiero bajar de peso" resolvía calistenia por el alias
        "peso corporal".
      */
      "bajar de peso",
      "perder peso",
      "adelgazar",
    ],
    respuesta: {
      texto:
        "Si querés transpirar en serio, estas actividades no fallan: boxeo o kickboxing para descargar todo, cross training para exigirte al máximo, o running para sumar resistencia.",
      enlaces: [
        crearEnlaceExplorarDeporte("boxeo", "Boxeo"),
        crearEnlaceExplorarDeporte("cross-training", "Cross Training"),
        crearEnlaceExplorarDeporte("running", "Running"),
      ],
      opcionesRapidas: ["Quiero algo social", "Sin deportes de pelea"],
    },
  },
  {
    id: "intensidad-baja",
    prioridad: "alta",
    tipo: "recomendacion",
    palabrasClave: [
      "quiero algo tranquilo",
      "algo tranquilo",
      "algo relajado",
      "algo suave",
      "bajo impacto",
      "sin impacto",
      "relajarme",
      "desestresarme",
      "tranqui",
    ],
    respuesta: {
      texto:
        "Para moverte sin exigirte de más, estas opciones son ideales: yoga o pilates para trabajar cuerpo y cabeza, stretching para ganar movilidad, o natación que es suave con las articulaciones.",
      enlaces: [
        crearEnlaceExplorarDeporte("yoga", "Yoga"),
        crearEnlaceExplorarDeporte("pilates", "Pilates"),
        crearEnlaceExplorarDeporte("natacion", "Natación"),
      ],
      opcionesRapidas: ["Quiero algo social", "Algo intenso"],
    },
  },

  /* ---------------------------------------------------------------
     Ayuda de la app: siempre se responden en el navegador. La respuesta
     es la misma con o sin contexto, así que no tiene sentido pagar una
     llamada al backend ni esperar la red.
     --------------------------------------------------------------- */
  {
    id: "ver-deportes",
    prioridad: "alta",
    tipo: "ayuda-app",
    palabrasClave: [
      "que deportes hay",
      "que actividades hay",
      "que deportes tienen",
      "lista de deportes",
      "todos los deportes",
      "ver deportes",
      "catalogo de deportes",
      "deportes disponibles",
    ],
    /*
      Sueltas son "mostrame el catálogo"; dentro de una frase pertenecen a
      otra cosa: "deportes de combate" es una categoría y antes caía acá.
    */
    palabrasClaveExactas: ["deportes", "deporte", "actividades"],
    respuesta: {
      texto:
        "Hay de todo: fútbol, boxeo, yoga, natación, gimnasios, artes marciales y mucho más. Podés recorrer el catálogo completo por categoría o ir directo a Explorar y filtrar ahí.",
      enlaces: [ENLACE_DEPORTES, ENLACE_EXPLORAR],
    },
  },
  {
    id: "publicar",
    prioridad: "alta",
    tipo: "ayuda-app",
    palabrasClave: [
      "como publico",
      "quiero publicar",
      "publicar una actividad",
      "publicar mi actividad",
      "publicar",
      "soy profesor",
      "soy profesora",
      "soy profe",
      "soy entrenador",
      "soy entrenadora",
      "soy instructor",
      "tengo un club",
      "tengo un gimnasio",
      "tengo una escuela",
      "quiero ofrecer clases",
      "sumar mi actividad",
      "anunciar mi actividad",
    ],
    respuesta: {
      texto:
        "¿Tenés un club, gimnasio o das clases? Publicar es simple: completás el formulario con los datos de tu actividad (deporte, barrio, horarios, precios y contacto) y el equipo la revisa antes de aprobarla. Si te registrás como publicador, además gestionás todo desde Mi perfil.",
      enlaces: [
        { href: "/publicar", etiqueta: "Publicar actividad" },
        { href: "/registro", etiqueta: "Crear cuenta" },
      ],
      opcionesRapidas: ["¿Dónde veo mis solicitudes?"],
    },
  },
  {
    id: "mis-imagenes",
    prioridad: "alta",
    tipo: "ayuda-app",
    palabrasClave: [
      "donde veo mis imagenes",
      "donde estan mis imagenes",
      "mis imagenes",
      "mis fotos",
      "subir imagenes",
      "subir fotos",
      "cargar fotos",
      "cargar imagenes",
      "agregar fotos",
      "gestor de imagenes",
      "cambiar la foto",
      "editar las fotos",
    ],
    respuesta: {
      texto:
        "Si sos publicador: entrá a Mi perfil → Mis actividades y elegí la actividad. Ahí abajo está el gestor de imágenes, donde subís, ordenás y borrás fotos.\n\nLas fotos no se publican al instante: el equipo las revisa antes de que se vean. Para el logo y la portada de tu perfil, en cambio, andá a Mi perfil → editar perfil.",
      enlaces: [
        { href: "/publicador/actividades", etiqueta: "Mis actividades" },
        { href: "/publicador/perfil", etiqueta: "Mi perfil de publicador" },
      ],
      opcionesRapidas: ["¿Qué es la imagen principal?"],
    },
  },
  {
    id: "imagen-principal",
    prioridad: "alta",
    tipo: "ayuda-app",
    palabrasClave: [
      "imagen principal",
      "foto principal",
      "que es la imagen principal",
      "imagen de portada",
      "foto de portada",
      "que es la galeria",
      "galeria de fotos",
      "para que sirve la galeria",
      "diferencia entre principal y galeria",
    ],
    respuesta: {
      texto:
        "La imagen principal es la portada de la actividad: es la que se ve en las tarjetas del listado y arriba de todo en el detalle. Hay una sola por actividad.\n\nLa galería son las demás fotos: el lugar, la clase, el ambiente. Se ven en el carrusel del detalle. Las dos pasan por revisión antes de publicarse.",
      enlaces: [{ href: "/publicador/actividades", etiqueta: "Mis actividades" }],
    },
  },
  {
    id: "mis-solicitudes",
    prioridad: "alta",
    tipo: "ayuda-app",
    palabrasClave: [
      "mis solicitudes",
      "donde veo mis solicitudes",
      "estado de mi solicitud",
      "estado de mi actividad",
      "ya mandé mi actividad",
      "cuando aprueban",
      "cuanto tarda la aprobacion",
      "esta aprobada",
      "solicitud de cambio",
      "pedir un cambio",
      "editar mi actividad",
    ],
    respuesta: {
      texto:
        "Todo lo que enviaste y su estado están en Mi perfil → Solicitudes. Ahí ves si una actividad está pendiente, aprobada o rechazada, con el motivo cuando corresponde.\n\nSi querés cambiar algo de una actividad ya publicada, se pide desde la actividad, con «solicitar cambios»: también pasa por revisión.",
      enlaces: [
        { href: "/publicador/solicitudes", etiqueta: "Mis solicitudes" },
        { href: "/publicador/actividades", etiqueta: "Mis actividades" },
      ],
    },
  },
  {
    id: "guardados",
    prioridad: "alta",
    tipo: "ayuda-app",
    palabrasClave: [
      "guardar una actividad",
      "guardar actividades",
      "como guardo",
      "mis guardados",
      "donde veo lo que guarde",
      "favoritos",
      "mis favoritos",
      "marcar favorito",
    ],
    respuesta: {
      texto:
        "En cada actividad tenés el botón de guardar: al tocarlo se suma a tus Guardados y la encontrás después en Mi perfil, en la solapa Guardados. Para eso sí hace falta tener cuenta.",
      enlaces: [
        { href: "/favoritos", etiqueta: "Ver mis guardados" },
        { href: "/mi-cuenta", etiqueta: "Ir a Mi perfil" },
      ],
    },
  },
  {
    id: "seguir-publicadores",
    prioridad: "alta",
    tipo: "ayuda-app",
    palabrasClave: [
      "seguir un club",
      "seguir a un profe",
      "como sigo",
      "seguir publicador",
      "dejar de seguir",
      "a quien sigo",
      "mis seguidos",
      "novedades",
      "feed",
    ],
    respuesta: {
      texto:
        "Entrá al perfil del club o profe (desde cualquier actividad suya, tocando el nombre) y tocá «Seguir». Lo que publiquen después te aparece en Mi perfil, en la solapa Novedades, y en Siguiendo ves a todos los que seguís.",
      enlaces: [{ href: "/mi-cuenta", etiqueta: "Ir a Mi perfil" }],
    },
  },
  {
    id: "contacto",
    prioridad: "alta",
    tipo: "ayuda-app",
    palabrasClave: [
      "como contacto",
      "como los contacto",
      "como me contacto",
      "contactar",
      "como me comunico",
      "hablar con el club",
      "hablar con el profesor",
      "hablar con el profe",
      "escribirle al club",
      "whatsapp",
      "instagram",
      "telefono",
      "numero de telefono",
      "mandar mensaje",
    ],
    respuesta: {
      texto:
        "Cada actividad tiene su botón de contacto en la página de detalle: puede ser WhatsApp, Instagram o email, según lo que haya cargado el club o profe. Entrá desde Explorar a la actividad que te interesa y tocá el botón de contacto para escribirles directo.",
      enlaces: [ENLACE_EXPLORAR],
    },
  },
  {
    id: "ciudades",
    prioridad: "alta",
    tipo: "ayuda-app",
    palabrasClave: [
      "que ciudades",
      "en que ciudades",
      "ciudades",
      "cambiar ciudad",
      "cambiar de ciudad",
      "otra ciudad",
      "mi ciudad",
      "mar del plata",
      "mardel",
      "mdq",
      "mdp",
      "donde funciona",
    ],
    respuesta: {
      texto:
        "Por ahora el foco está puesto en Mar del Plata, y la idea es ir sumando más ciudades. Desde la página de ciudades podés ver las disponibles y cambiar la tuya cuando quieras; arriba de todo también tenés el selector de ciudad.",
      enlaces: [{ href: "/ciudades", etiqueta: "Ver ciudades" }],
    },
  },
  {
    id: "filtros",
    prioridad: "alta",
    tipo: "ayuda-app",
    palabrasClave: [
      "como filtro",
      "filtrar",
      "filtros",
      "por barrio",
      "que barrios",
      "barrios",
      "barrio",
      "por nivel",
      "nivel de la actividad",
      "modalidad",
      "presencial u online",
      "como ordeno",
      "ordenar resultados",
      "por precio",
      "como busco",
      "buscar actividades",
    ],
    respuesta: {
      texto:
        "En Explorar tenés filtros por deporte, barrio, nivel (por ejemplo, principiante) y modalidad, y también podés ordenar los resultados, por ejemplo por precio. Elegí lo que quieras combinar, tocá «Aplicar filtros» y listo.",
      enlaces: [ENLACE_EXPLORAR],
    },
  },
  {
    id: "precios-horarios",
    prioridad: "alta",
    tipo: "ayuda-app",
    palabrasClave: [
      "precio",
      "precios",
      "cuanto sale",
      "cuanto cuesta",
      "cuanto esta",
      "arancel",
      "cuota",
      "mensualidad",
      "horario",
      "horarios",
      "que dias",
      "a que hora",
      "es gratis",
    ],
    respuesta: {
      texto:
        "Los precios y horarios se ven en el detalle de cada actividad: entrá desde Explorar a la que te interese y ahí tenés toda la info junta, incluido el contacto por si querés confirmar algo directamente con el club o profe.",
      enlaces: [ENLACE_EXPLORAR],
    },
  },
  {
    id: "registro-login",
    prioridad: "alta",
    tipo: "ayuda-app",
    palabrasClave: [
      "registro",
      "registrarme",
      "registrarse",
      "crear cuenta",
      "crear una cuenta",
      "cuenta",
      "login",
      "iniciar sesion",
      "loguearme",
      "ingresar a mi cuenta",
      "mi cuenta",
      "para que sirve la cuenta",
    ],
    respuesta: {
      texto:
        "Con una cuenta podés guardar actividades y seguir a clubes y profes para ver sus novedades. Si además querés publicar, la cuenta de publicador te deja cargar y gestionar tus actividades desde Mi perfil. Para buscar y contactar no hace falta cuenta: eso es libre.",
      enlaces: [
        { href: "/registro", etiqueta: "Crear cuenta" },
        { href: "/login", etiqueta: "Iniciar sesión" },
      ],
    },
  },
  {
    id: "ayuda",
    prioridad: "alta",
    tipo: "ayuda-app",
    palabrasClave: [
      "que es dondeentreno",
      "que es donde entreno",
      "como funciona",
      "en que me podes ayudar",
      "que podes hacer",
      "que sabes hacer",
      "ayuda",
      "necesito ayuda",
    ],
    respuesta: {
      texto:
        "DondeEntreno es una guía deportiva local: buscás un deporte y ves clubes, profes y gimnasios de tu ciudad con precios, horarios, barrio y contacto directo. Yo te puedo ayudar a elegir qué entrenar, encontrar actividades y explicarte cómo usar la app.",
      enlaces: [ENLACE_EXPLORAR, ENLACE_DEPORTES],
      opcionesRapidas: ["No sé qué entrenar", "Cómo publico una actividad"],
    },
  },

  /* ---------------------------------------------------------------
     Conversación suelta: solo se responden acá cuando son TODA la
     entrada. "hola" se contesta en el navegador; "hola, algún deporte
     que recomiendes?" va al backend, que puede recomendar en serio.
     --------------------------------------------------------------- */
  {
    id: "saludo",
    prioridad: "baja",
    tipo: "conversacion",
    palabrasClave: [
      "hola",
      "holis",
      "buenas",
      "buen dia",
      "buenas tardes",
      "buenas noches",
      "que tal",
      "como estas",
      "como andas",
      "hey",
    ],
    respuesta: {
      texto:
        "¡Hola! ¿Cómo andás? Contame qué estás buscando: te puedo ayudar a elegir qué entrenar, encontrar actividades cerca tuyo o explicarte cómo usar la app.",
      opcionesRapidas: [
        "No sé qué entrenar",
        "Quiero algo social",
        "Cómo publico una actividad",
      ],
    },
  },
  {
    id: "agradecimiento",
    prioridad: "baja",
    tipo: "conversacion",
    palabrasClave: [
      "gracias",
      "muchas gracias",
      "mil gracias",
      "genial",
      "buenisimo",
      "perfecto",
      "excelente",
    ],
    respuesta: {
      texto: "¡De nada, para eso estoy! ¿Te doy una mano con algo más?",
      opcionesRapidas: ["No sé qué entrenar", "Ver todos los deportes"],
    },
  },
  {
    id: "despedida",
    prioridad: "baja",
    tipo: "conversacion",
    palabrasClave: [
      "chau",
      "chao",
      "adios",
      "hasta luego",
      "hasta pronto",
      "nos vemos",
      "me voy",
      "hasta la proxima",
    ],
    respuesta: {
      texto:
        "¡Nos vemos! Cuando quieras volver a buscar dónde entrenar, acá voy a estar. ¡Que entrenes bien!",
    },
  },
];
