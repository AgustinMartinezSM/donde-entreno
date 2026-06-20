import { API_BASE_URL } from "../lib/apiConfig";
import {
  ESTADOS_SOLICITUD_PUBLICACION,
  type EstadoSolicitudPublicacion,
  type SolicitudPublicacionErrorResponse,
  type SolicitudPublicacionErroresPorCampo,
  type SolicitudPublicacionRequest,
  type SolicitudPublicacionResponse,
} from "../types/solicitudPublicacion";

type SolicitudPublicacionApiErrorOpciones = {
  status?: number | null;
  respuesta?: SolicitudPublicacionErrorResponse | null;
  erroresPorCampo?: SolicitudPublicacionErroresPorCampo | null;
};

export class SolicitudPublicacionApiError extends Error {
  status: number | null;
  respuesta: SolicitudPublicacionErrorResponse | null;
  erroresPorCampo: SolicitudPublicacionErroresPorCampo | null;

  constructor(
    message: string,
    opciones: SolicitudPublicacionApiErrorOpciones = {}
  ) {
    super(message);
    this.name = "SolicitudPublicacionApiError";
    this.status = opciones.status ?? null;
    this.respuesta = opciones.respuesta ?? null;
    this.erroresPorCampo = opciones.erroresPorCampo ?? null;
  }
}

// Envía una solicitud pública de publicación al backend.
export async function enviarSolicitudPublicacion(
  solicitud: SolicitudPublicacionRequest
): Promise<SolicitudPublicacionResponse> {
  let respuestaHttp: Response;

  try {
    respuestaHttp = await fetch(`${API_BASE_URL}/api/solicitudes-publicacion`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "Accept": "application/json",
      },
      body: JSON.stringify(solicitud),
    });
  } catch (error: unknown) {
    if (error instanceof SolicitudPublicacionApiError) {
      throw error;
    }

    throw new SolicitudPublicacionApiError(
      "No fue posible conectar con el servidor para enviar la solicitud de publicación."
    );
  }

  const cuerpo: unknown = await leerJsonSeguro(respuestaHttp);

  if (!respuestaHttp.ok) {
    if (esSolicitudPublicacionErrorResponse(cuerpo)) {
      throw new SolicitudPublicacionApiError(cuerpo.mensaje, {
        status: respuestaHttp.status,
        respuesta: cuerpo,
        erroresPorCampo: cuerpo.errores,
      });
    }

    throw new SolicitudPublicacionApiError(
      "No se pudo enviar la solicitud de publicación.",
      {
        status: respuestaHttp.status,
      }
    );
  }

  if (!esSolicitudPublicacionResponse(cuerpo)) {
    throw new SolicitudPublicacionApiError(
      "La respuesta del servidor no tiene el formato esperado.",
      {
        status: respuestaHttp.status,
      }
    );
  }

  return cuerpo;
}

async function leerJsonSeguro(respuesta: Response): Promise<unknown> {
  try {
    const cuerpo: unknown = await respuesta.json();
    return cuerpo;
  } catch {
    return null;
  }
}

function esObjeto(valor: unknown): valor is Record<string, unknown> {
  return typeof valor === "object" && valor !== null && !Array.isArray(valor);
}

function esEstadoSolicitudPublicacion(
  valor: unknown
): valor is EstadoSolicitudPublicacion {
  return (
    typeof valor === "string" &&
    ESTADOS_SOLICITUD_PUBLICACION.includes(
      valor as EstadoSolicitudPublicacion
    )
  );
}

function esSolicitudPublicacionErroresPorCampo(
  valor: unknown
): valor is SolicitudPublicacionErroresPorCampo {
  if (!esObjeto(valor)) {
    return false;
  }

  return Object.values(valor).every(
    (mensajePorCampo) => typeof mensajePorCampo === "string"
  );
}

function esSolicitudPublicacionResponse(
  valor: unknown
): valor is SolicitudPublicacionResponse {
  return (
    esObjeto(valor) &&
    typeof valor.id === "number" &&
    typeof valor.codigoSeguimiento === "string" &&
    esEstadoSolicitudPublicacion(valor.estado) &&
    typeof valor.createdAt === "string" &&
    typeof valor.mensaje === "string"
  );
}

function esSolicitudPublicacionErrorResponse(
  valor: unknown
): valor is SolicitudPublicacionErrorResponse {
  return (
    esObjeto(valor) &&
    typeof valor.status === "number" &&
    typeof valor.error === "string" &&
    typeof valor.mensaje === "string" &&
    (valor.errores === null ||
      esSolicitudPublicacionErroresPorCampo(valor.errores)) &&
    typeof valor.path === "string" &&
    typeof valor.timestamp === "string"
  );
}
