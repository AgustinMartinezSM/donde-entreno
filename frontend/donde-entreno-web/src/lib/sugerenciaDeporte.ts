/*
  Sugerencia inteligente de deporte para la búsqueda por texto.

  Resuelve texto libre ("jiujitsu", "bjj", "gym") contra el catálogo
  espejo del seed usando el mismo scoring por aliases del buscador de
  deportes. Es código puro: se usa desde componentes server (Explorar)
  sin costo de cliente.
*/

import { CATALOGO_DEPORTES_ASISTENTE } from "./asistente/conocimiento";
import { obtenerPuntajeBusquedaDeporte } from "./deporteSearch";

/*
  750 = "el nombre/alias contiene la búsqueda" o mejor (empieza/exacto).
  Debajo de eso las coincidencias son demasiado débiles para sugerir.
*/
const PUNTAJE_MINIMO_SUGERENCIA = 750;

export type SugerenciaDeporte = {
  nombre: string;
  slug: string;
};

export function obtenerSugerenciaDeporte(texto: string): SugerenciaDeporte | null {
  const textoLimpio = texto.trim();

  if (!textoLimpio) {
    return null;
  }

  let mejorNombre: string | null = null;
  let mejorSlug: string | null = null;
  let mejorPuntaje = 0;

  for (const deporte of CATALOGO_DEPORTES_ASISTENTE) {
    const puntaje = obtenerPuntajeBusquedaDeporte(deporte, textoLimpio);

    if (puntaje > mejorPuntaje) {
      mejorPuntaje = puntaje;
      mejorNombre = deporte.nombre;
      mejorSlug = deporte.slug;
    }
  }

  if (mejorPuntaje < PUNTAJE_MINIMO_SUGERENCIA || !mejorNombre || !mejorSlug) {
    return null;
  }

  return { nombre: mejorNombre, slug: mejorSlug };
}
