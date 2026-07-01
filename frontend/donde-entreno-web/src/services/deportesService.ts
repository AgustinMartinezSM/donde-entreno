import { API_BASE_URL } from "../lib/apiConfig";
import type { Deporte } from "../types/deporte";

const MENSAJE_ERROR_DEPORTES = "No pudimos cargar los deportes.";

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

function leerTextoOpcional(valor: unknown) {
  if (valor === undefined || valor === null) {
    return null;
  }

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

export async function obtenerDeportes(): Promise<Deporte[]> {
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
}
