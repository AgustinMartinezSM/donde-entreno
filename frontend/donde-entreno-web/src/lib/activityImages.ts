/*
  Centralizamos aca las imagenes visuales de actividades.

  La prioridad que usamos es:
  1. Imagen propia de la actividad desde backend.
  2. Imagen default segun el deporte.
  3. Placeholder general si no conocemos el deporte.
*/

const imagenesPorDeporteSlug: Record<string, string> = {
  basquet: "/sports/sport-basquet.png",
  boxeo: "/sports/sport-boxeo.png",
  "cross-training": "/sports/sport-cross-training.png",
  futbol: "/sports/sport-futbol.png",
  gimnasio: "/sports/sport-gimnasio.png",
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
