import { API_BASE_URL } from "../lib/apiConfig";

/*
  Check-in "Entrené acá" (script 26). Endpoints autenticados bajo
  /api/usuario/checkins: el POST es idempotente por día (201 con fila
  nueva, 200 si ya había una hoy) y el GET pinta el botón al cargar.
*/

export type CheckinRespuesta = {
  yaRegistradoHoy: boolean;
  registradoAhora: boolean;
  cantidadPersonasEntrenaron30Dias: number;
};

export class CheckinApiError extends Error {
  status: number | null;

  constructor(message: string, status: number | null = null) {
    super(message);
    this.name = "CheckinApiError";
    this.status = status;
  }
}

export async function registrarCheckin(
  actividadId: number,
  accessToken: string
): Promise<CheckinRespuesta> {
  return ejecutar(
    `${API_BASE_URL}/api/usuario/checkins/${actividadId}`,
    "POST",
    accessToken
  );
}

export async function obtenerEstadoCheckinHoy(
  actividadId: number,
  accessToken: string
): Promise<CheckinRespuesta> {
  return ejecutar(
    `${API_BASE_URL}/api/usuario/checkins/${actividadId}/hoy`,
    "GET",
    accessToken
  );
}

async function ejecutar(
  url: string,
  method: "GET" | "POST",
  accessToken: string
): Promise<CheckinRespuesta> {
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
    throw new CheckinApiError(
      "No fue posible conectar con el servidor."
    );
  }

  if (!respuesta.ok) {
    throw new CheckinApiError(
      "No pudimos registrar tu entrenamiento. Probá nuevamente.",
      respuesta.status
    );
  }

  const data: unknown = await respuesta.json();

  if (
    typeof data !== "object" ||
    data === null ||
    typeof (data as CheckinRespuesta).yaRegistradoHoy !== "boolean"
  ) {
    throw new CheckinApiError(
      "La respuesta del servidor no tiene el formato esperado.",
      respuesta.status
    );
  }

  return data as CheckinRespuesta;
}
