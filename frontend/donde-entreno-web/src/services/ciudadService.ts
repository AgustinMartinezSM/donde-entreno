import { cache } from "react";

import { API_BASE_URL } from "../lib/apiConfig";
import {
  esObjeto,
  leerBooleanoRequerido,
  leerNumeroOpcional,
  leerNumeroRequerido,
  leerTextoRequerido,
} from "./apiHelpers";
import type { Ciudad } from "../types/ciudad";

const MENSAJE_ERROR_CIUDADES = "No pudimos cargar las ciudades.";
const MENSAJE_ERROR_CIUDAD = "No pudimos cargar la ciudad solicitada.";

function parsearCiudad(valor: unknown): Ciudad | null {
  if (!esObjeto(valor)) {
    return null;
  }

  const id = leerNumeroRequerido(valor.id);
  const nombre = leerTextoRequerido(valor.nombre);
  const slug = leerTextoRequerido(valor.slug);
  const activa = leerBooleanoRequerido(valor.activa);

  if (id === null || nombre === null || slug === null || activa === null) {
    return null;
  }

  return {
    id,
    nombre,
    slug,
    activa,
    orden: leerNumeroOpcional(valor.orden),
  };
}

/*
  cache() de React deduplica llamadas dentro del mismo request SSR
  (varias páginas resuelven la ciudad en generateMetadata y de nuevo
  en la página). En cliente actúa como passthrough.
*/
export const obtenerCiudades = cache(async (): Promise<Ciudad[]> => {
  try {
    const respuesta = await fetch(`${API_BASE_URL}/api/ciudades`, {
      headers: {
        Accept: "application/json",
      },
      cache: "no-store",
    });

    if (!respuesta.ok) {
      throw new Error(MENSAJE_ERROR_CIUDADES);
    }

    const datos: unknown = await respuesta.json();

    if (!Array.isArray(datos)) {
      throw new Error(MENSAJE_ERROR_CIUDADES);
    }

    const ciudades: Ciudad[] = [];

    for (const item of datos) {
      const ciudad = parsearCiudad(item);

      if (!ciudad) {
        throw new Error(MENSAJE_ERROR_CIUDADES);
      }

      ciudades.push(ciudad);
    }

    return ciudades;
  } catch {
    throw new Error(MENSAJE_ERROR_CIUDADES);
  }
});

export const obtenerCiudadPorSlug = cache(async (slug: string): Promise<Ciudad> => {
  const slugLimpio = slug.trim();

  if (!slugLimpio) {
    throw new Error(MENSAJE_ERROR_CIUDAD);
  }

  try {
    const respuesta = await fetch(
      `${API_BASE_URL}/api/ciudades/${encodeURIComponent(slugLimpio)}`,
      {
        headers: {
          Accept: "application/json",
        },
        cache: "no-store",
      }
    );

    if (!respuesta.ok) {
      throw new Error(MENSAJE_ERROR_CIUDAD);
    }

    const datos: unknown = await respuesta.json();
    const ciudad = parsearCiudad(datos);

    if (!ciudad) {
      throw new Error(MENSAJE_ERROR_CIUDAD);
    }

    return ciudad;
  } catch {
    throw new Error(MENSAJE_ERROR_CIUDAD);
  }
});
