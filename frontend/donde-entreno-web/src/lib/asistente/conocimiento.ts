/*
  Base de conocimiento declarativa del Asistente DondeEntreno.

  Todo lo que el asistente "sabe" está acá: intenciones con palabras clave,
  respuestas en español rioplatense, enlaces internos y opciones rápidas de
  seguimiento. El motor (motorLocal.ts) solo normaliza, puntúa y elige; el
  contenido se edita en este archivo sin tocar lógica.

  Las palabras clave se comparan normalizadas (sin tildes ni mayúsculas),
  usando la misma normalización que el buscador de deportes (deporteSearch).
*/

import type { Deporte } from "../../types/deporte";
import type { EnlaceAsistente, RespuestaAsistente } from "./tipos";

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
  // Frases o palabras clave, ya en minúsculas y sin tildes.
  palabrasClave: string[];
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
  Catálogo espejo de los deportes reales del seed (26 deportes).
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
*/
export const RESPUESTA_BIENVENIDA: RespuestaAsistente = {
  texto:
    "¡Hola! Soy el asistente de DondeEntreno. Te ayudo a encontrar deportes, clubes, profes y gimnasios en tu ciudad. Contame qué buscás o elegí una opción.",
  opcionesRapidas: [
    "No sé qué deporte elegir",
    "¿Qué deportes hay?",
    "¿Cómo publico una actividad?",
    "¿Cómo filtro en Explorar?",
  ],
};

/*
  Respuesta amable cuando el motor no entiende la consulta.
*/
export const RESPUESTA_FALLBACK: RespuestaAsistente = {
  texto:
    "Mmm, esa no la tengo del todo clara. ¿Me lo contás con otras palabras? Por ejemplo, decime un deporte («boxeo», «yoga») o probá con alguna de estas opciones.",
  enlaces: [ENLACE_EXPLORAR, ENLACE_DEPORTES],
  opcionesRapidas: [
    "No sé qué deporte elegir",
    "¿Qué deportes hay?",
    "¿Cómo publico una actividad?",
    "¿Cómo contacto a un club?",
  ],
};

/*
  Respuesta cuando el motor resuelve un deporte concreto del catálogo.
*/
export function crearRespuestaDeporte(deporte: Deporte): RespuestaAsistente {
  return {
    texto: `¡${deporte.nombre} es una gran elección! Mirá las actividades de ${deporte.nombre} en tu ciudad: en el detalle de cada una vas a encontrar precios, horarios, barrio y el contacto directo.`,
    enlaces: [crearEnlaceExplorarDeporte(deporte.slug, deporte.nombre), ENLACE_DEPORTES],
    opcionesRapidas: [
      "¿Cómo contacto a un club?",
      "¿Dónde veo precios y horarios?",
      "¿Cómo filtro por barrio?",
    ],
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
    opcionesRapidas: ["No sé qué deporte elegir", "¿Cómo filtro en Explorar?"],
  };
}

/*
  Intenciones del asistente. El orden importa: ante un empate de puntaje,
  gana la que está declarada primero.
*/
export const INTENCIONES_ASISTENTE: IntencionAsistente[] = [
  {
    id: "elegir-deporte",
    prioridad: "alta",
    palabrasClave: [
      "no se que deporte",
      "no se que elegir",
      "no se cual elegir",
      "no se por donde empezar",
      "ayudame a elegir",
      "que me recomendas",
      "que me conviene",
      "recomendame",
      "recomendacion",
      "quiero empezar a entrenar",
      "quiero entrenar",
      "quiero arrancar",
      "estoy indeciso",
      "estoy indecisa",
      "no me decido",
    ],
    respuesta: {
      texto:
        "¡Te ayudo a elegir! Contame un poco más de vos: ¿preferís entrenar solo o en grupo? ¿Buscás algo intenso para descargar o algo más tranquilo?",
      enlaces: [ENLACE_DEPORTES],
      opcionesRapidas: [
        "Prefiero entrenar solo",
        "Prefiero entrenar en grupo",
        "Quiero algo intenso",
        "Quiero algo tranquilo",
      ],
    },
  },
  {
    id: "entrenar-solo",
    prioridad: "alta",
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
        crearEnlaceExplorarDeporte("yoga", "Yoga"),
      ],
      opcionesRapidas: [
        "Quiero algo intenso",
        "Quiero algo tranquilo",
        "Ver todos los deportes",
      ],
    },
  },
  {
    id: "entrenar-grupo",
    prioridad: "alta",
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
        "¡Entrenar con gente motiva un montón! Mirá estas opciones: fútbol y vóley para sumarte a un equipo, básquet si te gusta el ritmo rápido, o cross training para transpirar en grupo.",
      enlaces: [
        crearEnlaceExplorarDeporte("futbol", "Fútbol"),
        crearEnlaceExplorarDeporte("voley", "Vóley"),
        crearEnlaceExplorarDeporte("basquet", "Básquet"),
        crearEnlaceExplorarDeporte("cross-training", "Cross Training"),
      ],
      opcionesRapidas: [
        "Quiero algo intenso",
        "Quiero algo tranquilo",
        "Ver todos los deportes",
      ],
    },
  },
  {
    id: "intensidad-alta",
    prioridad: "alta",
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
    ],
    respuesta: {
      texto:
        "Si querés transpirar en serio, estas actividades no fallan: boxeo o kickboxing para descargar todo, cross training para exigirte al máximo, o running para sumar resistencia.",
      enlaces: [
        crearEnlaceExplorarDeporte("boxeo", "Boxeo"),
        crearEnlaceExplorarDeporte("kickboxing", "Kickboxing"),
        crearEnlaceExplorarDeporte("cross-training", "Cross Training"),
        crearEnlaceExplorarDeporte("running", "Running"),
      ],
      opcionesRapidas: [
        "Prefiero entrenar solo",
        "Prefiero entrenar en grupo",
        "Ver todos los deportes",
      ],
    },
  },
  {
    id: "intensidad-baja",
    prioridad: "alta",
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
        crearEnlaceExplorarDeporte("stretching", "Stretching"),
        crearEnlaceExplorarDeporte("natacion", "Natación"),
      ],
      opcionesRapidas: [
        "Prefiero entrenar solo",
        "Prefiero entrenar en grupo",
        "Ver todos los deportes",
      ],
    },
  },
  {
    id: "ver-deportes",
    prioridad: "alta",
    palabrasClave: [
      "que deportes hay",
      "que actividades hay",
      "que deportes tienen",
      "lista de deportes",
      "todos los deportes",
      "ver deportes",
      "catalogo de deportes",
      "deportes disponibles",
      "deportes",
      "deporte",
    ],
    respuesta: {
      texto:
        "Hay de todo: fútbol, boxeo, yoga, natación, gimnasios, artes marciales y mucho más. Podés recorrer el catálogo completo por categoría o ir directo a Explorar y filtrar ahí.",
      enlaces: [ENLACE_DEPORTES, ENLACE_EXPLORAR],
      opcionesRapidas: ["No sé qué deporte elegir", "¿Cómo filtro en Explorar?"],
    },
  },
  {
    id: "publicar",
    prioridad: "alta",
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
        "¿Tenés un club, gimnasio o das clases? ¡Buenísimo! Publicar es simple: completás el formulario con los datos de tu actividad (deporte, barrio, horarios, precios y contacto) y el equipo la revisa antes de aprobarla. Si te registrás como publicador, además gestionás tus actividades desde tu propio panel.",
      enlaces: [
        { href: "/publicar", etiqueta: "Publicar actividad" },
        { href: "/registro", etiqueta: "Crear cuenta" },
      ],
      opcionesRapidas: [
        "¿Para qué sirve registrarse?",
        "¿Qué ciudades hay?",
      ],
    },
  },
  {
    id: "contacto",
    prioridad: "alta",
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
        "Cada actividad tiene su botón de contacto en la página de detalle: puede ser WhatsApp, Instagram o email, según lo que haya cargado el club o profe. Entrá desde Explorar a la actividad que te interesa y tocá el botón verde de contacto para escribirles directo.",
      enlaces: [ENLACE_EXPLORAR],
      opcionesRapidas: [
        "¿Dónde veo precios y horarios?",
        "¿Cómo filtro en Explorar?",
      ],
    },
  },
  {
    id: "ciudades",
    prioridad: "alta",
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
      opcionesRapidas: ["¿Qué deportes hay?", "¿Cómo filtro por barrio?"],
    },
  },
  {
    id: "filtros",
    prioridad: "alta",
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
      opcionesRapidas: [
        "¿Dónde veo precios y horarios?",
        "¿Qué deportes hay?",
      ],
    },
  },
  {
    id: "precios-horarios",
    prioridad: "alta",
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
      opcionesRapidas: [
        "¿Cómo contacto a un club?",
        "¿Cómo filtro en Explorar?",
      ],
    },
  },
  {
    id: "registro-login",
    prioridad: "alta",
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
        "Registrarte te sirve sobre todo si querés publicar: con una cuenta de publicador cargás tus actividades y las gestionás desde tu panel. Para buscar actividades y contactar clubes no hace falta cuenta, ¡podés usar todo libremente!",
      enlaces: [
        { href: "/registro", etiqueta: "Crear cuenta" },
        { href: "/login", etiqueta: "Iniciar sesión" },
      ],
      opcionesRapidas: [
        "¿Cómo publico una actividad?",
        "¿Qué deportes hay?",
      ],
    },
  },
  {
    id: "ayuda",
    prioridad: "alta",
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
        "DondeEntreno es una guía deportiva local: buscás un deporte y ves clubes, profes y gimnasios de tu ciudad con precios, horarios, barrio y contacto directo. Yo te puedo ayudar a elegir un deporte, explicarte cómo publicar tu actividad y cómo sacarle el jugo a los filtros.",
      enlaces: [ENLACE_EXPLORAR, ENLACE_DEPORTES],
      opcionesRapidas: [
        "No sé qué deporte elegir",
        "¿Cómo publico una actividad?",
        "¿Qué ciudades hay?",
      ],
    },
  },
  {
    id: "saludo",
    prioridad: "baja",
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
        "¡Hola! ¿Cómo andás? Contame qué estás buscando: puedo ayudarte a encontrar un deporte, explicarte cómo publicar tu actividad o cómo usar los filtros de Explorar.",
      opcionesRapidas: [
        "No sé qué deporte elegir",
        "¿Qué deportes hay?",
        "¿Cómo publico una actividad?",
        "¿Cómo filtro en Explorar?",
      ],
    },
  },
  {
    id: "agradecimiento",
    prioridad: "baja",
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
      opcionesRapidas: [
        "No sé qué deporte elegir",
        "¿Cómo filtro en Explorar?",
        "¿Qué ciudades hay?",
      ],
    },
  },
  {
    id: "despedida",
    prioridad: "baja",
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
      opcionesRapidas: ["Ver todos los deportes"],
    },
  },
];
