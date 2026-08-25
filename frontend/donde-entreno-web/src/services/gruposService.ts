import { API_BASE_URL } from "../lib/apiConfig";

/*
  Grupos por actividad (script 38).

  El grupo es el espacio de una actividad para quienes van: el
  publicador avisa, los miembros comentan y reaccionan. No hay chat
  libre entre miembros — es V2 del roadmap y el canal más difícil de
  moderar del producto.
*/

export type ComentarioAviso = {
  id: number;
  texto: string;
  autorNombre: string | null;
  esPropio: boolean | null;
  createdAt: string | null;
};

export type AvisoGrupo = {
  id: number;
  texto: string;
  createdAt: string | null;

  imagenId: number | null;
  imagenUrl: string | null;

  cantidadMeGusta: number | null;
  meGusta: boolean | null;

  cantidadComentarios: number | null;
  /** Solo al abrir el aviso. */
  comentarios?: ComentarioAviso[];
};

export type GrupoActividad = {
  actividadId: number;
  actividadTitulo: string | null;
  actividadSlug: string | null;

  esMiembro: boolean;
  cantidadMiembros: number | null;

  /** Vacío si no es miembro: el contenido no sale del backend. */
  avisos: AvisoGrupo[];
};

export const MAX_TEXTO_AVISO = 1000;
export const MAX_TEXTO_COMENTARIO = 500;

export class GruposApiError extends Error {
  status: number | null;

  constructor(message: string, status: number | null = null) {
    super(message);
    this.name = "GruposApiError";
    this.status = status;
  }
}

/* ========================== Miembro ========================== */

export async function obtenerGrupo(
  accessToken: string,
  actividadId: number
): Promise<GrupoActividad> {
  return (await ejecutar(
    `${API_BASE_URL}/api/usuario/grupos/${actividadId}`,
    "GET",
    accessToken
  )) as GrupoActividad;
}

export async function unirseAlGrupo(
  accessToken: string,
  actividadId: number
): Promise<GrupoActividad> {
  return (await ejecutar(
    `${API_BASE_URL}/api/usuario/grupos/${actividadId}/miembros`,
    "PUT",
    accessToken
  )) as GrupoActividad;
}

export async function salirDelGrupo(
  accessToken: string,
  actividadId: number
): Promise<void> {
  await ejecutar(
    `${API_BASE_URL}/api/usuario/grupos/${actividadId}/miembros`,
    "DELETE",
    accessToken
  );
}

export async function obtenerAviso(
  accessToken: string,
  avisoId: number
): Promise<AvisoGrupo> {
  return (await ejecutar(
    `${API_BASE_URL}/api/usuario/grupos/avisos/${avisoId}`,
    "GET",
    accessToken
  )) as AvisoGrupo;
}

export async function comentarAviso(
  accessToken: string,
  avisoId: number,
  texto: string
): Promise<ComentarioAviso> {
  return (await ejecutar(
    `${API_BASE_URL}/api/usuario/grupos/avisos/${avisoId}/comentarios`,
    "POST",
    accessToken,
    { texto: texto.trim() }
  )) as ComentarioAviso;
}

export async function eliminarComentarioPropio(
  accessToken: string,
  comentarioId: number
): Promise<void> {
  await ejecutar(
    `${API_BASE_URL}/api/usuario/grupos/comentarios/${comentarioId}`,
    "DELETE",
    accessToken
  );
}

export type RespuestaMeGustaAviso = {
  cantidadMeGusta: number;
  meGusta: boolean;
};

export async function darMeGustaAviso(
  accessToken: string,
  avisoId: number
): Promise<RespuestaMeGustaAviso> {
  return (await ejecutar(
    `${API_BASE_URL}/api/usuario/grupos/avisos/${avisoId}/me-gusta`,
    "PUT",
    accessToken
  )) as RespuestaMeGustaAviso;
}

export async function quitarMeGustaAviso(
  accessToken: string,
  avisoId: number
): Promise<RespuestaMeGustaAviso> {
  return (await ejecutar(
    `${API_BASE_URL}/api/usuario/grupos/avisos/${avisoId}/me-gusta`,
    "DELETE",
    accessToken
  )) as RespuestaMeGustaAviso;
}

/* ========================= Publicador ========================= */

export async function avisarAlGrupo(
  accessToken: string,
  actividadId: number,
  texto: string,
  imagenId?: number | null
): Promise<AvisoGrupo> {
  return (await ejecutar(
    `${API_BASE_URL}/api/publicador/grupos/${actividadId}/avisos`,
    "POST",
    accessToken,
    {
      texto: texto.trim(),
      ...(imagenId ? { imagenId: String(imagenId) } : {}),
    }
  )) as AvisoGrupo;
}

export async function eliminarAviso(
  accessToken: string,
  avisoId: number
): Promise<void> {
  await ejecutar(
    `${API_BASE_URL}/api/publicador/grupos/avisos/${avisoId}`,
    "DELETE",
    accessToken
  );
}

/** El publicador modera su propio grupo. */
export async function ocultarComentarioComoPublicador(
  accessToken: string,
  comentarioId: number
): Promise<void> {
  await ejecutar(
    `${API_BASE_URL}/api/publicador/grupos/comentarios/${comentarioId}/ocultar`,
    "PATCH",
    accessToken
  );
}

/* =========================== Admin =========================== */

export async function ocultarAvisoAdmin(
  accessToken: string,
  avisoId: number
): Promise<void> {
  await ejecutar(
    `${API_BASE_URL}/api/admin/avisos-grupo/${avisoId}/ocultar`,
    "PATCH",
    accessToken
  );
}

export async function ocultarComentarioGrupoAdmin(
  accessToken: string,
  comentarioId: number
): Promise<void> {
  await ejecutar(
    `${API_BASE_URL}/api/admin/comentarios-grupo/${comentarioId}/ocultar`,
    "PATCH",
    accessToken
  );
}

/* ============================== Común ============================== */

async function ejecutar(
  url: string,
  method: "GET" | "POST" | "PUT" | "PATCH" | "DELETE",
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
    throw new GruposApiError("No fue posible conectar con el servidor.");
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

    throw new GruposApiError(mensaje, respuesta.status);
  }

  if (respuesta.status === 204) {
    return null;
  }

  return respuesta.json();
}
