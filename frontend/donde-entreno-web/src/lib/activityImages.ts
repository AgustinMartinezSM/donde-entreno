/*
  Centralizamos acá las imágenes visuales de actividades.

  La prioridad que vamos a usar es:
  1. Imagen propia de la actividad desde backend.
  2. Imagen default según el deporte.
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

export function obtenerImagenActividad({
  imagenBackend,
  deporteSlug,
}: ObtenerImagenActividadParams) {
  /*
    Si el backend ya trae una imagen propia válida,
    usamos esa imagen primero.
  */
  if (imagenBackend && imagenBackend.trim().length > 0) {
    return imagenBackend;
  }

  /*
    Si no hay imagen propia, buscamos una imagen default
    según el slug del deporte.
  */
  if (deporteSlug) {
    const imagenPorDeporte = imagenesPorDeporteSlug[deporteSlug];

    if (imagenPorDeporte) {
      return imagenPorDeporte;
    }
  }

  /*
    Si no conocemos el deporte o no tenemos imagen para ese slug,
    usamos un placeholder general.
  */
  return imagenPlaceholderGeneral;
}