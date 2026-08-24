import { API_BASE_URL } from "../lib/apiConfig";

/*
  Notificaciones internas (script 28, Fase 2 social): campanita con
  contador por polling suave + panel con marcar leídas.
*/

export type Notificacion = {
  id: number;
  tipo: string;
  titulo: string;
  ruta: string | null;
  leida: boolean;
  createdAt: string | null;
};

export type PaginaNotificaciones = {
  contenido: Notificacion[];
  paginaActual: number;
  totalPaginas: number;
  ultima: boolean;
};

export class NotificacionesApiError extends Error {
  status: number | null;

  constructor(message: string, status: number | null = null) {
    super(message);
    this.name = "NotificacionesApiError";
    this.status = status;
  }
}

export async function obtenerNotificaciones(
  accessToken: string,
  page = 0
): Promise<PaginaNotificaciones> {
  const data = await ejecutar(
    `${API_BASE_URL}/api/usuario/notificaciones?page=${page}&size=20`,
    "GET",
    accessToken
  );

  return data as PaginaNotificaciones;
}

export async function obtenerContadorNoLeidas(
  accessToken: string
): Promise<number> {
  const data = (await ejecutar(
    `${API_BASE_URL}/api/usuario/notificaciones/contador`,
    "GET",
    accessToken
  )) as { noLeidas?: number };

  return typeof data.noLeidas === "number" ? data.noLeidas : 0;
}

export async function marcarNotificacionLeida(
  accessToken: string,
  id: number
): Promise<void> {
  await ejecutar(
    `${API_BASE_URL}/api/usuario/notificaciones/${id}/leida`,
    "PATCH",
    accessToken,
    true
  );
}

export async function marcarTodasLeidas(accessToken: string): Promise<void> {
  await ejecutar(
    `${API_BASE_URL}/api/usuario/notificaciones/todas-leidas`,
    "PATCH",
    accessToken,
    true
  );
}

async function ejecutar(
  url: string,
  method: "GET" | "PATCH",
  accessToken: string,
  sinCuerpo = false
): Promise<unknown> {
  let respuesta: Response;

  try {
    respuesta = await fetch(url, {
      method,
      headers: {
        "Authorization": `Bearer ${accessToken}`,
        "Accept": "application/json",
      },
      cache: "no-store",
    });
  } catch {
    throw new NotificacionesApiError("No fue posible conectar con el servidor.");
  }

  if (!respuesta.ok) {
    throw new NotificacionesApiError(
      "No pudimos cargar tus notificaciones.",
      respuesta.status
    );
  }

  if (sinCuerpo || respuesta.status === 204) {
    return null;
  }

  return respuesta.json();
}
