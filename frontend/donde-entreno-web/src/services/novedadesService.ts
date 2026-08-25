import { API_BASE_URL } from "../lib/apiConfig";

/*
  Canal de novedades del publicador (script 34, Fase 8). Mismo patrón
  que galeriaSocialService: fetch directo y errores humanos. El tope
  diario lo impone el backend y llega como 400 con su mensaje, que se
  muestra tal cual (dice cuántas quedan).
*/

export type Novedad = {
  id: number;
  texto: string;
  createdAt: string | null;

  perfilPublicadorId: number | null;
  perfilNombre: string | null;
  perfilSlug: string | null;
  perfilLogoUrl: string | null;

  imagenId: number | null;
  imagenUrl: string | null;

  /* Reacciones (script 37). */
  cantidadMeGusta: number | null;
  meGusta: boolean | null;
};

export type RespuestaMeGusta = {
  cantidadMeGusta: number;
  meGusta: boolean;
};

/** Reaccionar es idempotente: PUT/DELETE, no un POST que alterna. */
export async function darMeGustaNovedad(
  accessToken: string,
  novedadId: number
): Promise<RespuestaMeGusta> {
  return (await ejecutar(
    `${API_BASE_URL}/api/usuario/novedades/${novedadId}/me-gusta`,
    "PUT",
    accessToken
  )) as RespuestaMeGusta;
}

export async function quitarMeGustaNovedad(
  accessToken: string,
  novedadId: number
): Promise<RespuestaMeGusta> {
  return (await ejecutar(
    `${API_BASE_URL}/api/usuario/novedades/${novedadId}/me-gusta`,
    "DELETE",
    accessToken
  )) as RespuestaMeGusta;
}

export const MAX_TEXTO_NOVEDAD = 1000;

export class NovedadesApiError extends Error {
  status: number | null;

  constructor(message: string, status: number | null = null) {
    super(message);
    this.name = "NovedadesApiError";
    this.status = status;
  }
}

/** Las visibles de un publicador (público, sin sesión). */
export async function obtenerNovedadesDePerfil(
  perfilPublicadorId: number,
  limite = 10
): Promise<Novedad[]> {
  return (await ejecutar(
    `${API_BASE_URL}/api/perfiles-publicadores/${perfilPublicadorId}/novedades?limite=${limite}`,
    "GET"
  )) as Novedad[];
}

/** Las del publicador logueado, para su panel. */
export async function obtenerMisNovedades(
  accessToken: string
): Promise<Novedad[]> {
  return (await ejecutar(
    `${API_BASE_URL}/api/publicador/novedades`,
    "GET",
    accessToken
  )) as Novedad[];
}

export async function publicarNovedad(
  accessToken: string,
  texto: string,
  imagenId?: number | null
): Promise<Novedad> {
  return (await ejecutar(
    `${API_BASE_URL}/api/publicador/novedades`,
    "POST",
    accessToken,
    {
      texto: texto.trim(),
      ...(imagenId ? { imagenId: String(imagenId) } : {}),
    }
  )) as Novedad;
}

export async function eliminarNovedadPropia(
  accessToken: string,
  novedadId: number
): Promise<void> {
  await ejecutar(
    `${API_BASE_URL}/api/publicador/novedades/${novedadId}`,
    "DELETE",
    accessToken
  );
}

/** El admin la oculta (moderación reactiva desde la cola de reportes). */
export async function ocultarNovedadAdmin(
  accessToken: string,
  novedadId: number
): Promise<void> {
  await ejecutar(
    `${API_BASE_URL}/api/admin/novedades/${novedadId}/ocultar`,
    "PATCH",
    accessToken
  );
}

/* ============================== Común ============================== */

async function ejecutar(
  url: string,
  method: "GET" | "POST" | "PUT" | "DELETE" | "PATCH",
  accessToken?: string,
  cuerpo?: unknown
): Promise<unknown> {
  let respuesta: Response;

  try {
    respuesta = await fetch(url, {
      method,
      headers: {
        "Accept": "application/json",
        ...(accessToken ? { "Authorization": `Bearer ${accessToken}` } : {}),
        ...(cuerpo !== undefined ? { "Content-Type": "application/json" } : {}),
      },
      ...(cuerpo !== undefined ? { body: JSON.stringify(cuerpo) } : {}),
      cache: "no-store",
    });
  } catch {
    throw new NovedadesApiError("No fue posible conectar con el servidor.");
  }

  if (!respuesta.ok) {
    let mensaje = "Algo salió mal. Probá nuevamente.";

    try {
      const cuerpoError: unknown = await respuesta.json();
      if (
        typeof cuerpoError === "object" &&
        cuerpoError !== null &&
        typeof (cuerpoError as { mensaje?: unknown }).mensaje === "string"
      ) {
        mensaje = (cuerpoError as { mensaje: string }).mensaje;
      }
    } catch {
      /* Cuerpo ilegible: queda el genérico. */
    }

    throw new NovedadesApiError(mensaje, respuesta.status);
  }

  if (respuesta.status === 204) {
    return null;
  }

  return respuesta.json();
}
