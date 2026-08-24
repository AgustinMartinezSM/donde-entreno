/*
  Links a mapas (Fase 7), sin ninguna dependencia.

  El proyecto no tiene librerías de UI y esta fase no las suma: para
  "Cómo llegar" alcanza con un link bien armado, que además abre la
  app nativa de mapas en el teléfono — mejor que un mapa embebido.
*/

type PuntoActividad = {
  latitud?: number | string | null;
  longitud?: number | string | null;
  googleMapsUrl?: string | null;
  direccion?: string | null;
  ubicacionNombre?: string | null;
  barrioNombre?: string | null;
  ciudadNombre?: string | null;
};

/** Las coordenadas llegan como string desde Jackson (BigDecimal). */
export function coordenadasDe(
  punto: PuntoActividad
): { lat: number; lng: number } | null {
  const lat = Number(punto.latitud);
  const lng = Number(punto.longitud);

  if (
    punto.latitud === null ||
    punto.latitud === undefined ||
    punto.longitud === null ||
    punto.longitud === undefined ||
    Number.isNaN(lat) ||
    Number.isNaN(lng)
  ) {
    return null;
  }

  return { lat, lng };
}

/**
 * Link para llegar. Con coordenadas apunta al punto exacto; sin ellas
 * cae a una búsqueda por dirección, que es lo mejor que se puede
 * hacer con el dato disponible (y nunca una precisión inventada).
 *
 * Devuelve null si no hay ni coordenadas ni dirección: ahí el botón
 * directamente no se dibuja.
 */
export function construirHrefComoLlegar(punto: PuntoActividad): string | null {
  const coordenadas = coordenadasDe(punto);

  if (coordenadas) {
    return `https://www.google.com/maps/search/?api=1&query=${coordenadas.lat},${coordenadas.lng}`;
  }

  /* Un link que el publicador ya cargó vale más que armar una búsqueda. */
  if (punto.googleMapsUrl && punto.googleMapsUrl.startsWith("http")) {
    return punto.googleMapsUrl;
  }

  const consulta = [
    punto.ubicacionNombre,
    punto.direccion,
    punto.barrioNombre,
    punto.ciudadNombre,
  ]
    .filter(Boolean)
    .join(", ");

  if (!consulta) {
    return null;
  }

  return `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(consulta)}`;
}

/** "a 1,2 km" — con coma decimal, como se escribe en español. */
export function formatearDistancia(km?: number | null): string | null {
  if (km === null || km === undefined || km < 0) {
    return null;
  }

  if (km < 1) {
    return `a ${Math.round(km * 1000)} m`;
  }

  return `a ${km.toFixed(1).replace(".", ",")} km`;
}
