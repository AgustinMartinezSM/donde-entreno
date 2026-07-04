import type { Ciudad } from "../types/ciudad";

export const DEFAULT_CITY_SLUG = "mar-del-plata";
export const CIUDAD_ACTIVA_STORAGE_KEY = "dondeEntreno.ciudadActivaSlug";

export function normalizarSlugCiudad(valor?: string | null) {
  const slug = valor?.trim();

  return slug ? slug : null;
}

export function leerSlugCiudadGuardada() {
  if (!puedeUsarLocalStorage()) {
    return null;
  }

  try {
    return normalizarSlugCiudad(
      window.localStorage.getItem(CIUDAD_ACTIVA_STORAGE_KEY)
    );
  } catch {
    return null;
  }
}

export function guardarSlugCiudadActiva(slug: string) {
  const slugNormalizado = normalizarSlugCiudad(slug);

  if (!slugNormalizado || !puedeUsarLocalStorage()) {
    return;
  }

  try {
    window.localStorage.setItem(CIUDAD_ACTIVA_STORAGE_KEY, slugNormalizado);
  } catch {
    // Si el navegador bloquea localStorage, la navegacion sigue funcionando.
  }
}

export function ordenarCiudadesActivas(ciudades: Ciudad[]) {
  return ciudades
    .filter((ciudad) => ciudad.activa)
    .sort((ciudadA, ciudadB) => {
      const ordenA = ciudadA.orden;
      const ordenB = ciudadB.orden;

      if (ordenA !== null && ordenB !== null && ordenA !== ordenB) {
        return ordenA - ordenB;
      }

      if (ordenA !== null && ordenB === null) {
        return -1;
      }

      if (ordenA === null && ordenB !== null) {
        return 1;
      }

      return ciudadA.nombre.localeCompare(ciudadB.nombre, "es");
    });
}

export function buscarCiudadPorSlug(
  ciudades: Ciudad[],
  slug?: string | null
) {
  const slugNormalizado = normalizarSlugCiudad(slug);

  if (!slugNormalizado) {
    return null;
  }

  return (
    ciudades.find((ciudad) => ciudad.slug === slugNormalizado && ciudad.activa) ??
    null
  );
}

export function buscarCiudadPorId(
  ciudades: Ciudad[],
  ciudadId?: string | null
) {
  const id = Number(ciudadId);

  if (!Number.isInteger(id) || id <= 0) {
    return null;
  }

  return ciudades.find((ciudad) => ciudad.id === id && ciudad.activa) ?? null;
}

export function obtenerCiudadFallback(ciudades: Ciudad[]) {
  return (
    buscarCiudadPorSlug(ciudades, DEFAULT_CITY_SLUG) ??
    ciudades.find((ciudad) => ciudad.activa) ??
    null
  );
}

export function obtenerSlugCiudadDesdeRuta(pathname: string | null) {
  const segmentos = pathname?.split("/").filter(Boolean) ?? [];

  if (segmentos[0] !== "ciudades" || !segmentos[1]) {
    return null;
  }

  try {
    return normalizarSlugCiudad(decodeURIComponent(segmentos[1]));
  } catch {
    return normalizarSlugCiudad(segmentos[1]);
  }
}

function puedeUsarLocalStorage() {
  return typeof window !== "undefined" && "localStorage" in window;
}
