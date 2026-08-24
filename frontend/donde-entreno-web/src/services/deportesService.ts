import { cache } from "react";

import { API_BASE_URL } from "../lib/apiConfig";
import {
  esObjeto,
  leerNumeroOpcional,
  leerNumeroRequerido,
  leerTextoOpcional,
  leerTextoRequerido,
} from "./apiHelpers";
import type { Deporte } from "../types/deporte";

const MENSAJE_ERROR_DEPORTES = "No pudimos cargar los deportes.";

function parsearDeporte(valor: unknown): Deporte | null {
  if (!esObjeto(valor)) {
    return null;
  }

  const id = leerNumeroRequerido(valor.id);
  const nombre = leerTextoRequerido(valor.nombre);
  const slug = leerTextoRequerido(valor.slug);

  if (id === null || nombre === null || slug === null) {
    return null;
  }

  return {
    id,
    nombre,
    slug,
    descripcion: leerTextoOpcional(valor.descripcion),
    iconoUrl: leerTextoOpcional(valor.iconoUrl),
    orden: leerNumeroOpcional(valor.orden),
    categoriaId: leerNumeroOpcional(valor.categoriaId),
    categoriaNombre: leerTextoOpcional(valor.categoriaNombre),
    categoriaSlug: leerTextoOpcional(valor.categoriaSlug),
  };
}

/*
  cache() de React deduplica llamadas dentro del mismo request SSR:
  las landings piden el catálogo en generateMetadata y de nuevo en la
  página, y antes eso eran fetches repetidos al backend.
*/
export const obtenerDeportes = cache(async (): Promise<Deporte[]> => {
  try {
    const respuesta = await fetch(`${API_BASE_URL}/api/deportes`, {
      headers: {
        Accept: "application/json",
      },
      cache: "no-store",
    });

    if (!respuesta.ok) {
      throw new Error(MENSAJE_ERROR_DEPORTES);
    }

    const datos: unknown = await respuesta.json();

    if (!Array.isArray(datos)) {
      throw new Error(MENSAJE_ERROR_DEPORTES);
    }

    const deportes: Deporte[] = [];

    for (const item of datos) {
      const deporte = parsearDeporte(item);

      if (!deporte) {
        throw new Error(MENSAJE_ERROR_DEPORTES);
      }

      deportes.push(deporte);
    }

    return deportes;
  } catch {
    throw new Error(MENSAJE_ERROR_DEPORTES);
  }
});

/*
  Deportes más vistos (Fase 6), derivados del tracking anónimo.

  Devuelve [] ante cualquier problema y también cuando el backend
  decide que no hay señal suficiente: la sección de la home entonces
  cae a su selección curada, en vez de presentar como "lo más visto"
  un ranking armado con dos clicks.
*/
export const obtenerDeportesPopulares = cache(
  async (): Promise<{ slug: string; nombre: string }[]> => {
    try {
      const respuesta = await fetch(
        `${API_BASE_URL}/api/deportes/populares?dias=30&limite=6`,
        {
          headers: { Accept: "application/json" },
          cache: "no-store",
        }
      );

      if (!respuesta.ok) {
        return [];
      }

      const datos: unknown = await respuesta.json();

      if (!Array.isArray(datos)) {
        return [];
      }

      return datos.flatMap((item) => {
        if (!esObjeto(item)) {
          return [];
        }

        const slug = leerTextoRequerido(item.slug);
        const nombre = leerTextoRequerido(item.nombre);

        return slug && nombre ? [{ slug, nombre }] : [];
      });
    } catch {
      return [];
    }
  }
);
