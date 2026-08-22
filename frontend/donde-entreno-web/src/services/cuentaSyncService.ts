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
  metodo: "GET" | "PUT" | "DELETE" | "POST" | "PATCH",
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

/* ================= Colecciones de guardados (bloque 13) ================= */

export type ColeccionGuardados = {
  id: number;
  nombre: string;
  cantidad: number;
};

/** Un guardado con su organización: la card pública más colección y nota. */
export type FavoritoOrganizado = {
  actividad: Actividad;
  coleccionId: number | null;
  nota: string | null;
};

function esColeccion(valor: unknown): valor is ColeccionGuardados {
  return (
    typeof valor === "object" &&
    valor !== null &&
    typeof (valor as ColeccionGuardados).id === "number" &&
    typeof (valor as ColeccionGuardados).nombre === "string" &&
    typeof (valor as ColeccionGuardados).cantidad === "number"
  );
}

export async function obtenerColeccionesCuenta(
  accessToken: string
): Promise<ColeccionGuardados[]> {
  const respuesta = await pedir("/api/usuario/colecciones", "GET", accessToken);
  const datos: unknown = await respuesta.json();

  if (!Array.isArray(datos) || !datos.every(esColeccion)) {
    throw new Error("Sync de cuenta: el listado de colecciones no es valido.");
  }

  return datos;
}

export async function crearColeccionCuenta(
  accessToken: string,
  nombre: string
): Promise<ColeccionGuardados> {
  const respuesta = await pedir("/api/usuario/colecciones", "POST", accessToken, {
    nombre,
  });
  const datos: unknown = await respuesta.json();

  if (!esColeccion(datos)) {
    throw new Error("Sync de cuenta: la coleccion creada no es valida.");
  }

  return datos;
}

export async function renombrarColeccionCuenta(
  accessToken: string,
  id: number,
  nombre: string
): Promise<void> {
  await pedir(`/api/usuario/colecciones/${id}`, "PATCH", accessToken, { nombre });
}

export async function eliminarColeccionCuenta(
  accessToken: string,
  id: number
): Promise<void> {
  await pedir(`/api/usuario/colecciones/${id}`, "DELETE", accessToken);
}

export async function obtenerFavoritosOrganizados(
  accessToken: string
): Promise<FavoritoOrganizado[]> {
  const respuesta = await pedir(
    "/api/usuario/favoritos/organizados",
    "GET",
    accessToken
  );
  const datos: unknown = await respuesta.json();

  if (!Array.isArray(datos)) {
    throw new Error("Sync de cuenta: el listado organizado no es una lista.");
  }

  return datos as FavoritoOrganizado[];
}

/** Reemplazo total de la organización del guardado (colección + nota). */
export async function organizarFavoritoCuenta(
  accessToken: string,
  slug: string,
  coleccionId: number | null,
  nota: string | null
): Promise<void> {
  await pedir(
    `/api/usuario/favoritos/${encodeURIComponent(slug)}`,
    "PATCH",
    accessToken,
    { coleccionId, nota }
  );
}

/* ================= Likes en fotos (bloque 14) ================= */

export async function obtenerLikesFotosCuenta(
  accessToken: string
): Promise<number[]> {
  const respuesta = await pedir("/api/usuario/likes-fotos", "GET", accessToken);
  const datos: unknown = await respuesta.json();

  if (
    !Array.isArray(datos) ||
    !datos.every((valor) => typeof valor === "number")
  ) {
    throw new Error("Sync de cuenta: el listado de likes no es una lista de ids.");
  }

  return datos;
}

export async function darLikeFotoCuenta(
  accessToken: string,
  imagenId: number
): Promise<void> {
  await pedir(`/api/usuario/likes-fotos/${imagenId}`, "PUT", accessToken);
}

export async function quitarLikeFotoCuenta(
  accessToken: string,
  imagenId: number
): Promise<void> {
  await pedir(`/api/usuario/likes-fotos/${imagenId}`, "DELETE", accessToken);
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
