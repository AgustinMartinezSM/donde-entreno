/*
  Centralizamos aca las imagenes visuales de actividades.

  La prioridad que usamos es:
  1. Imagen propia de la actividad desde backend.
  2. Imagen default segun el deporte.
  3. Placeholder general si no conocemos el deporte.
*/

/*
  Mapa alineado a los deportes reales del seed (02_seed_data.sql).
  Los deportes sin ilustración propia reutilizan la más afín para no
  caer al placeholder genérico. Cuando se generen assets nuevos,
  reemplazar los alias marcados.

  Sin imagen afín (usan el placeholder general hasta tener asset propio):
  hockey, ciclismo.
*/
const imagenesPorDeporteSlug: Record<string, string> = {
  // Deportes con ilustración propia
  basquet: "/sports/sport-basquet.png",
  boxeo: "/sports/sport-boxeo.png",
  "cross-training": "/sports/sport-cross-training.png",
  futbol: "/sports/sport-futbol.png",
  "jiu-jitsu": "/sports/sport-jiu-jitsu.png",
  karate: "/sports/sport-karate.png",
  musculacion: "/sports/sport-musculacion.png",
  natacion: "/sports/sport-natacion.png",
  pilates: "/sports/sport-pilates.png",
  running: "/sports/sport-running.png",
  taekwondo: "/sports/sport-taekwondo.png",
  tenis: "/sports/sport-tenis.png",
  voley: "/sports/sport-voley.png",
  yoga: "/sports/sport-yoga.png",

  // Alias: reutilizan la ilustración más afín hasta tener asset propio
  kickboxing: "/sports/sport-boxeo.png",
  "muay-thai": "/sports/sport-boxeo.png",
  mma: "/sports/sport-boxeo.png",
  judo: "/sports/sport-jiu-jitsu.png",
  funcional: "/sports/sport-cross-training.png",
  calistenia: "/sports/sport-cross-training.png",
  "entrenamiento-personalizado": "/sports/sport-gimnasio.png",
  "aqua-gym": "/sports/sport-natacion.png",
  stretching: "/sports/sport-yoga.png",
  padel: "/sports/sport-tenis.png",
  squash: "/sports/sport-tenis.png",
};

const imagenPlaceholderGeneral = "/placeholders/placeholder-general.png";

/*
  Badges circulares para la fila de historias de la home (public/stories,
  512x512 con fondo transparente y anillo propio). Son un set aparte de
  las ilustraciones de card: aquellas son anchas y para usarlas en un
  círculo había que hacer zoom al centro; estas se diseñaron redondas.

  Cubren 18 de los 27 deportes del catálogo. "GYM" se asignó a
  entrenamiento-personalizado (musculación ya tiene badge propio). Los
  deportes sin badge caen a la ilustración de card con el zoom de
  siempre, así la fila nunca queda con huecos.
*/
const badgesStoryPorDeporteSlug: Record<string, string> = {
  boxeo: "/stories/story-boxeo.png",
  "cross-training": "/stories/story-cross-training.png",
  "entrenamiento-personalizado":
    "/stories/story-entrenamiento-personalizado.png",
  funcional: "/stories/story-funcional.png",
  futbol: "/stories/story-futbol.png",
  "jiu-jitsu": "/stories/story-jiu-jitsu.png",
  judo: "/stories/story-judo.png",
  karate: "/stories/story-karate.png",
  kickboxing: "/stories/story-kickboxing.png",
  mma: "/stories/story-mma.png",
  musculacion: "/stories/story-musculacion.png",
  natacion: "/stories/story-natacion.png",
  padel: "/stories/story-padel.png",
  pilates: "/stories/story-pilates.png",
  running: "/stories/story-running.png",
  taekwondo: "/stories/story-taekwondo.png",
  tenis: "/stories/story-tenis.png",
  yoga: "/stories/story-yoga.png",
};

/* Badge de historia del deporte, o null si todavía no tiene arte propio. */
export function obtenerBadgeStoryDeporte(deporteSlug?: string | null) {
  if (!deporteSlug) {
    return null;
  }

  return badgesStoryPorDeporteSlug[deporteSlug] ?? null;
}

type ObtenerImagenActividadParams = {
  imagenBackend?: string | null;
  deporteSlug?: string | null;
};

type ObtenerImagenFallbackActividadParams = {
  deporteSlug?: string | null;
};

export function obtenerImagenActividad({
  imagenBackend,
  deporteSlug,
}: ObtenerImagenActividadParams) {
  if (imagenBackend && imagenBackend.trim().length > 0) {
    return imagenBackend;
  }

  return obtenerImagenFallbackActividad({ deporteSlug });
}

export function obtenerImagenFallbackActividad({
  deporteSlug,
}: ObtenerImagenFallbackActividadParams) {
  if (deporteSlug) {
    const imagenPorDeporte = imagenesPorDeporteSlug[deporteSlug];

    if (imagenPorDeporte) {
      return imagenPorDeporte;
    }
  }

  return imagenPlaceholderGeneral;
}
