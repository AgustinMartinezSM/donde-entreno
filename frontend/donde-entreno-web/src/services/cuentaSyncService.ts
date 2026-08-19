import { API_BASE_URL } from "../lib/apiConfig";
import type { Actividad } from "../types/actividad";

/*
  Cliente de los endpoints de sync por cuenta (script 20):
  /api/usuario/favoritos y /api/usuario/deportes.

  Deliberadamente minimo: lo consumen las libs de favoritos/preferencias
  y el sincronizador de cuenta, nunca los componentes. Los errores se
  lanzan como Error pelado y el caller decide (los toggles revierten, el
  sincronizador aborta en silencio: un corte de red no puede romper la
  UI ni pisar datos).
*/

async function pedir(
  ruta: string,
  metodo: "GET" | "PUT" | "DELETE",
  accessToken: string,
  body?: unknown
): Promise<Response> {
  const respuesta = await fetch(`${API_BASE_URL}${ruta}`, {
    method: metodo,
    headers: {
      "Accept": "application/json",
      "Authorization": `Bearer ${accessToken}`,
      ...(body === undefined ? {} : { "Content-Type": "application/json" }),
    },
    body: body === undefined ? undefined : JSON.stringify(body),
    cache: "no-store",
  });

  if (!respuesta.ok) {
    throw new Error(`Sync de cuenta: ${metodo} ${ruta} devolvio ${respuesta.status}.`);
  }

  return respuesta;
}

export async function obtenerFavoritosCuenta(
  accessToken: string
): Promise<Actividad[]> {
  const respuesta = await pedir("/api/usuario/favoritos", "GET", accessToken);
  const datos: unknown = await respuesta.json();

  if (!Array.isArray(datos)) {
    throw new Error("Sync de cuenta: el listado de favoritos no es una lista.");
  }

  return datos as Actividad[];
}

export async function guardarFavoritoCuenta(
  accessToken: string,
  slug: string
): Promise<void> {
  await pedir(
    `/api/usuario/favoritos/${encodeURIComponent(slug)}`,
    "PUT",
    accessToken
  );
}

export async function quitarFavoritoCuenta(
  accessToken: string,
  slug: string
): Promise<void> {
  await pedir(
    `/api/usuario/favoritos/${encodeURIComponent(slug)}`,
    "DELETE",
    accessToken
  );
}

export async function obtenerDeportesCuenta(
  accessToken: string
): Promise<string[]> {
  const respuesta = await pedir("/api/usuario/deportes", "GET", accessToken);
  const datos: unknown = await respuesta.json();

  if (
    !Array.isArray(datos) ||
    !datos.every((valor) => typeof valor === "string")
  ) {
    throw new Error("Sync de cuenta: el listado de deportes no es una lista de slugs.");
  }

  return datos;
}

export async function reemplazarDeportesCuenta(
  accessToken: string,
  slugs: string[]
): Promise<void> {
  await pedir("/api/usuario/deportes", "PUT", accessToken, { slugs });
}
