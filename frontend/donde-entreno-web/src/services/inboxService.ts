import { API_BASE_URL } from "../lib/apiConfig";

/*
  Inbox de consultas usuario ↔ publicador (script 36).

  La conversación la inicia SIEMPRE el usuario: no hay función para que
  el publicador arranque una, porque el backend tampoco expone el
  endpoint. Solo responde.
*/

export type MensajeConsulta = {
  id: number;
  /** null cuando el mensaje fue ocultado por el admin. */
  texto: string | null;
  createdAt: string | null;
  esPropio: boolean | null;
  oculto: boolean | null;
};

export type Conversacion = {
  id: number;
  /** ABIERTA | CERRADA_POR_USUARIO. */
  estado: string;
  ultimoMensajeAt: string | null;
  noLeidos: number | null;

  contraparteNombre: string | null;
  contraparteLogoUrl: string | null;
  perfilPublicadorId: number | null;
  perfilSlug: string | null;

  actividadId: number | null;
  actividadTitulo: string | null;
  actividadSlug: string | null;

  ultimoMensajeTexto: string | null;

  /** Solo al abrir el hilo. */
  mensajes?: MensajeConsulta[];
};

export const MAX_TEXTO_MENSAJE = 2000;

export class InboxApiError extends Error {
  status: number | null;

  constructor(message: string, status: number | null = null) {
    super(message);
    this.name = "InboxApiError";
    this.status = status;
  }
}

/* ========================== Usuario ========================== */

export async function obtenerBandejaUsuario(
  accessToken: string
): Promise<Conversacion[]> {
  return (await ejecutar(
    `${API_BASE_URL}/api/usuario/consultas`,
    "GET",
    accessToken
  )) as Conversacion[];
}

export async function obtenerHiloUsuario(
  accessToken: string,
  conversacionId: number
): Promise<Conversacion> {
  return (await ejecutar(
    `${API_BASE_URL}/api/usuario/consultas/${conversacionId}`,
    "GET",
    accessToken
  )) as Conversacion;
}

export async function consultar(
  accessToken: string,
  datos: {
    perfilPublicadorId: number;
    actividadId?: number | null;
    texto: string;
  }
): Promise<Conversacion> {
  return (await ejecutar(
    `${API_BASE_URL}/api/usuario/consultas`,
    "POST",
    accessToken,
    {
      perfilPublicadorId: String(datos.perfilPublicadorId),
      ...(datos.actividadId ? { actividadId: String(datos.actividadId) } : {}),
      texto: datos.texto.trim(),
    }
  )) as Conversacion;
}

/** Cerrar es solo del usuario: el publicador deja de poder escribir. */
export async function cerrarConsulta(
  accessToken: string,
  conversacionId: number
): Promise<void> {
  await ejecutar(
    `${API_BASE_URL}/api/usuario/consultas/${conversacionId}/cerrar`,
    "PATCH",
    accessToken
  );
}

/* ========================= Publicador ========================= */

export async function obtenerBandejaPublicador(
  accessToken: string
): Promise<Conversacion[]> {
  return (await ejecutar(
    `${API_BASE_URL}/api/publicador/consultas`,
    "GET",
    accessToken
  )) as Conversacion[];
}

export async function obtenerHiloPublicador(
  accessToken: string,
  conversacionId: number
): Promise<Conversacion> {
  return (await ejecutar(
    `${API_BASE_URL}/api/publicador/consultas/${conversacionId}`,
    "GET",
    accessToken
  )) as Conversacion;
}

export async function responderConsulta(
  accessToken: string,
  conversacionId: number,
  texto: string
): Promise<Conversacion> {
  return (await ejecutar(
    `${API_BASE_URL}/api/publicador/consultas/${conversacionId}/respuestas`,
    "POST",
    accessToken,
    { texto: texto.trim() }
  )) as Conversacion;
}

/* =========================== Admin =========================== */

/**
 * El mensaje reportado y a lo sumo los dos anteriores. Es lo ÚNICO
 * que el admin puede ver de una conversación privada: no existe un
 * endpoint que devuelva el hilo completo.
 */
export async function obtenerContextoMensajeAdmin(
  accessToken: string,
  mensajeId: number
): Promise<MensajeConsulta[]> {
  return (await ejecutar(
    `${API_BASE_URL}/api/admin/mensajes/${mensajeId}/contexto`,
    "GET",
    accessToken
  )) as MensajeConsulta[];
}

export async function ocultarMensajeAdmin(
  accessToken: string,
  mensajeId: number
): Promise<void> {
  await ejecutar(
    `${API_BASE_URL}/api/admin/mensajes/${mensajeId}/ocultar`,
    "PATCH",
    accessToken
  );
}

/* ============================== Común ============================== */

async function ejecutar(
  url: string,
  method: "GET" | "POST" | "PATCH" | "DELETE",
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
    throw new InboxApiError("No fue posible conectar con el servidor.");
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

    throw new InboxApiError(mensaje, respuesta.status);
  }

  if (respuesta.status === 204) {
    return null;
  }

  return respuesta.json();
}
