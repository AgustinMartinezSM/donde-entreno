import { API_BASE_URL } from "../lib/apiConfig";
import type { ImagenActividad } from "../types/actividad";

/*
  Fase 4 social (script 30): comentarios en fotos y fotos guardadas.
  Mismo patrón que confianzaService: fetch directo, errores humanos.
*/

export type ComentarioImagen = {
  id: number;
  texto: string;
  autorNombre: string;
  esPropio: boolean;
  createdAt: string | null;
};

export class GaleriaSocialApiError extends Error {
  status: number | null;

  constructor(message: string, status: number | null = null) {
    super(message);
    this.name = "GaleriaSocialApiError";
    this.status = status;
  }
}

/* =========================== Comentarios =========================== */

export async function obtenerComentarios(
  imagenId: number,
  accessToken?: string | null
): Promise<ComentarioImagen[]> {
  return (await ejecutar(
    `${API_BASE_URL}/api/imagenes/${imagenId}/comentarios`,
    "GET",
    accessToken ?? undefined
  )) as ComentarioImagen[];
}

export async function comentarFoto(
  accessToken: string,
  imagenId: number,
  texto: string
): Promise<ComentarioImagen> {
  return (await ejecutar(
    `${API_BASE_URL}/api/usuario/comentarios`,
    "POST",
    accessToken,
    { imagenId: String(imagenId), texto: texto.trim() }
  )) as ComentarioImagen;
}

export async function eliminarComentarioPropio(
  accessToken: string,
  comentarioId: number
): Promise<void> {
  await ejecutar(
    `${API_BASE_URL}/api/usuario/comentarios/${comentarioId}`,
    "DELETE",
    accessToken
  );
}

/* El backend valida que la foto sea del publicador del token. */
export async function ocultarComentarioEnMiFoto(
  accessToken: string,
  comentarioId: number
): Promise<void> {
  await ejecutar(
    `${API_BASE_URL}/api/publicador/comentarios/${comentarioId}/ocultar`,
    "PATCH",
    accessToken
  );
}

/* ========================= Fotos guardadas ========================= */

export async function obtenerIdsFotosGuardadas(
  accessToken: string
): Promise<number[]> {
  return (await ejecutar(
    `${API_BASE_URL}/api/usuario/fotos-guardadas`,
    "GET",
    accessToken
  )) as number[];
}

export async function obtenerFotosGuardadas(
  accessToken: string
): Promise<ImagenActividad[]> {
  return (await ejecutar(
    `${API_BASE_URL}/api/usuario/fotos-guardadas/detalle`,
    "GET",
    accessToken
  )) as ImagenActividad[];
}

export async function guardarFoto(
  accessToken: string,
  imagenId: number
): Promise<void> {
  await ejecutar(
    `${API_BASE_URL}/api/usuario/fotos-guardadas/${imagenId}`,
    "PUT",
    accessToken
  );
}

export async function quitarFotoGuardada(
  accessToken: string,
  imagenId: number
): Promise<void> {
  await ejecutar(
    `${API_BASE_URL}/api/usuario/fotos-guardadas/${imagenId}`,
    "DELETE",
    accessToken
  );
}

/* ============================== Común ============================== */

async function ejecutar(
  url: string,
  method: "GET" | "PUT" | "POST" | "DELETE" | "PATCH",
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
    throw new GaleriaSocialApiError("No fue posible conectar con el servidor.");
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

    throw new GaleriaSocialApiError(mensaje, respuesta.status);
  }

  if (respuesta.status === 204) {
    return null;
  }

  return respuesta.json();
}
