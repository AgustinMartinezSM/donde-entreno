import { API_BASE_URL } from "../lib/apiConfig";
import {
  ejecutarRequestJson,
  esErrorResponseApi,
  esObjeto,
  esValorDeLista,
} from "./apiHelpers";
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
  return ejecutarRequestJson(
    `${API_BASE_URL}/api/solicitudes-publicacion`,
    {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "Accept": "application/json",
      },
      body: JSON.stringify(solicitud),
    },
    esSolicitudPublicacionResponse,
    {
      crearErrorConexion: (error) =>
        error instanceof SolicitudPublicacionApiError
          ? error
          : new SolicitudPublicacionApiError(
              "No fue posible conectar con el servidor para enviar la solicitud de publicación."
            ),
      crearErrorHttp: (status, cuerpo) => {
        if (esErrorResponseApi(cuerpo)) {
          return new SolicitudPublicacionApiError(cuerpo.mensaje, {
            status,
            respuesta: cuerpo,
            erroresPorCampo: cuerpo.errores,
          });
        }

        return new SolicitudPublicacionApiError(
          "No se pudo enviar la solicitud de publicación.",
          { status }
        );
      },
      crearErrorFormatoInvalido: (status) =>
        new SolicitudPublicacionApiError(
          "La respuesta del servidor no tiene el formato esperado.",
          { status }
        ),
    }
  );
}

function esEstadoSolicitudPublicacion(
  valor: unknown
): valor is EstadoSolicitudPublicacion {
  return esValorDeLista(valor, ESTADOS_SOLICITUD_PUBLICACION);
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
