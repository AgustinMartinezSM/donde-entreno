import { API_BASE_URL } from "../lib/apiConfig";
import type { Ciudad } from "../types/ciudad";

const MENSAJE_ERROR_CIUDADES = "No pudimos cargar las ciudades.";
const MENSAJE_ERROR_CIUDAD = "No pudimos cargar la ciudad solicitada.";

function esObjeto(valor: unknown): valor is Record<string, unknown> {
  return typeof valor === "object" && valor !== null && !Array.isArray(valor);
}

function leerTextoRequerido(valor: unknown) {
  if (typeof valor !== "string") {
    return null;
  }

  const textoLimpio = valor.trim();

  return textoLimpio.length > 0 ? textoLimpio : null;
}

function leerNumeroRequerido(valor: unknown) {
  return typeof valor === "number" && Number.isFinite(valor) ? valor : null;
}

function leerNumeroOpcional(valor: unknown) {
  if (valor === undefined || valor === null) {
    return null;
  }

  return typeof valor === "number" && Number.isFinite(valor) ? valor : null;
}

function leerBooleanoRequerido(valor: unknown) {
  return typeof valor === "boolean" ? valor : null;
}

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

export async function obtenerCiudades(): Promise<Ciudad[]> {
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
}

export async function obtenerCiudadPorSlug(slug: string): Promise<Ciudad> {
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
}
