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
