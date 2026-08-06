import { API_BASE_URL } from "../lib/apiConfig";
import {
  construirAuthorization,
  construirQueryListado,
  ejecutarRequestJson,
  esErrorResponseApi,
  esNumberONull,
  esObjeto,
  esStringONull,
  esValorDeLista,
  validarIdPositivo,
} from "./apiHelpers";
import {
  DIAS_SEMANA_SOLICITUD_ADMIN,
  ESTADOS_SOLICITUD_ADMIN,
  type AdminErrorResponse,
  type CambiarEstadoSolicitudAdminRequest,
  type CambiarEstadoSolicitudAdminResponse,
  type DiaSemanaSolicitudAdmin,
  type EstadoSolicitudAdmin,
  type OrdenSolicitudesAdmin,
  type SolicitudPublicacionAprobacionResponse,
  type SolicitudPublicacionAdminDetalle,
  type SolicitudPublicacionAdminHorario,
  type SolicitudPublicacionAdminResumen,
  type SolicitudPublicacionAdminRevisor,
  type SolicitudesPublicacionAdminPage,
} from "../types/adminSolicitudes";
import {
  esSolicitudCambioDetalle,
  esSolicitudesCambioPage,
} from "./publicadorService";
import type {
  ListarSolicitudesCambioParams,
  SolicitudCambioDetalle,
  SolicitudesCambioPage,
} from "../types/publicador";

export type ListarSolicitudesAdminParams = {
  estado?: EstadoSolicitudAdmin | "";
  page?: number;
  size?: number;
  orden?: OrdenSolicitudesAdmin;
};

type AdminApiErrorOpciones = {
  status?: number | null;
  respuesta?: AdminErrorResponse | null;
};

type ValidadorAdmin<T> = (valor: unknown) => valor is T;

export class AdminApiError extends Error {
  status: number | null;
  respuesta: AdminErrorResponse | null;

  constructor(message: string, opciones: AdminApiErrorOpciones = {}) {
    super(message);
    this.name = "AdminApiError";
    this.status = opciones.status ?? null;
    this.respuesta = opciones.respuesta ?? null;
  }
}

export async function listarSolicitudesAdmin(
  params: ListarSolicitudesAdminParams,
  accessToken: string
): Promise<SolicitudesPublicacionAdminPage> {
  const authorization = construirAuthorizationAdmin(accessToken);
  const url = `${API_BASE_URL}/api/admin/solicitudes-publicacion${construirQueryListado(
    params
  )}`;

  return ejecutarAdminRequest(
    url,
    {
      method: "GET",
      headers: {
        "Accept": "application/json",
        "Authorization": authorization,
      },
      cache: "no-store",
    },
    esSolicitudesPublicacionAdminPage
  );
}

export async function obtenerSolicitudAdmin(
  id: number,
  accessToken: string
): Promise<SolicitudPublicacionAdminDetalle> {
  const authorization = construirAuthorizationAdmin(accessToken);
  const idSeguro = validarIdSolicitud(id);

  return ejecutarAdminRequest(
    `${API_BASE_URL}/api/admin/solicitudes-publicacion/${idSeguro}`,
    {
      method: "GET",
      headers: {
        "Accept": "application/json",
        "Authorization": authorization,
      },
      cache: "no-store",
    },
    esSolicitudPublicacionAdminDetalle
  );
}

export async function cambiarEstadoSolicitudAdmin(
  id: number,
  body: CambiarEstadoSolicitudAdminRequest,
  accessToken: string
): Promise<CambiarEstadoSolicitudAdminResponse> {
  const authorization = construirAuthorizationAdmin(accessToken);
  const idSeguro = validarIdSolicitud(id);

  return ejecutarAdminRequest(
    `${API_BASE_URL}/api/admin/solicitudes-publicacion/${idSeguro}/estado`,
    {
      method: "PATCH",
      headers: {
        "Content-Type": "application/json",
        "Accept": "application/json",
        "Authorization": authorization,
      },
      body: JSON.stringify(body),
    },
    esSolicitudPublicacionAdminDetalle
  );
}

export async function aprobarSolicitudAdmin(
  id: number,
  accessToken: string
): Promise<SolicitudPublicacionAprobacionResponse> {
  const authorization = construirAuthorizationAdmin(accessToken);
  const idSeguro = validarIdSolicitud(id);

  return ejecutarAdminRequest(
    `${API_BASE_URL}/api/admin/solicitudes-publicacion/${idSeguro}/aprobar`,
    {
      method: "POST",
      headers: {
        "Accept": "application/json",
        "Authorization": authorization,
      },
    },
    esSolicitudPublicacionAprobacionResponse
  );
}

// ============================================================
// Solicitudes de cambio sobre actividades publicadas (admin)
// ============================================================

export type CambiarEstadoSolicitudCambioAdminRequest = {
  estado: "EN_REVISION" | "RECHAZADA";
  motivoRechazo?: string;
};

export async function listarSolicitudesCambioAdmin(
  params: ListarSolicitudesCambioParams,
  accessToken: string
): Promise<SolicitudesCambioPage> {
  const authorization = construirAuthorizationAdmin(accessToken);
  const url = `${API_BASE_URL}/api/admin/solicitudes-cambio${construirQueryListado(
    params
  )}`;

  return ejecutarAdminRequest(
    url,
    {
      method: "GET",
      headers: {
        "Accept": "application/json",
        "Authorization": authorization,
      },
      cache: "no-store",
    },
    esSolicitudesCambioPage
  );
}

export async function obtenerSolicitudCambioAdmin(
  id: number,
  accessToken: string
): Promise<SolicitudCambioDetalle> {
  const authorization = construirAuthorizationAdmin(accessToken);
  const idSeguro = validarIdSolicitud(id);

  return ejecutarAdminRequest(
    `${API_BASE_URL}/api/admin/solicitudes-cambio/${idSeguro}`,
    {
      method: "GET",
      headers: {
        "Accept": "application/json",
        "Authorization": authorization,
      },
      cache: "no-store",
    },
    esSolicitudCambioDetalle
  );
}

export async function cambiarEstadoSolicitudCambioAdmin(
  id: number,
  body: CambiarEstadoSolicitudCambioAdminRequest,
  accessToken: string
): Promise<SolicitudCambioDetalle> {
  const authorization = construirAuthorizationAdmin(accessToken);
  const idSeguro = validarIdSolicitud(id);

  return ejecutarAdminRequest(
    `${API_BASE_URL}/api/admin/solicitudes-cambio/${idSeguro}/estado`,
    {
      method: "PATCH",
      headers: {
        "Content-Type": "application/json",
        "Accept": "application/json",
        "Authorization": authorization,
      },
      body: JSON.stringify(body),
    },
    esSolicitudCambioDetalle
  );
}

export async function aprobarSolicitudCambioAdmin(
  id: number,
  accessToken: string
): Promise<SolicitudCambioDetalle> {
  const authorization = construirAuthorizationAdmin(accessToken);
  const idSeguro = validarIdSolicitud(id);

  return ejecutarAdminRequest(
    `${API_BASE_URL}/api/admin/solicitudes-cambio/${idSeguro}/aprobar`,
    {
      method: "POST",
      headers: {
        "Accept": "application/json",
        "Authorization": authorization,
      },
    },
    esSolicitudCambioDetalle
  );
}

// ============================================================
// Moderación de imágenes (admin)
// ============================================================

export type ImagenAdmin = {
  id: number;
  url: string;
  tipoImagen: string;
  estadoModeracion: string;
  motivoRechazo: string | null;
  activa: boolean;
  createdAt: string | null;
  actividadId: number | null;
  actividadTitulo: string | null;
  actividadSlug: string | null;
};

export type ImagenesAdminPage = {
  contenido: ImagenAdmin[];
  paginaActual: number;
  tamanioPagina: number;
  totalElementos: number;
  totalPaginas: number;
  ultima: boolean;
};

export type ListarImagenesAdminParams = {
  estado?: string;
  page?: number;
  size?: number;
};

export async function listarImagenesAdmin(
  params: ListarImagenesAdminParams,
  accessToken: string
): Promise<ImagenesAdminPage> {
  const authorization = construirAuthorizationAdmin(accessToken);
  const url = `${API_BASE_URL}/api/admin/imagenes${construirQueryListado(params)}`;

  return ejecutarAdminRequest(
    url,
    {
      method: "GET",
      headers: {
        "Accept": "application/json",
        "Authorization": authorization,
      },
      cache: "no-store",
    },
    esImagenesAdminPage
  );
}

export async function aprobarImagenAdmin(
  id: number,
  accessToken: string
): Promise<ImagenAdmin> {
  const authorization = construirAuthorizationAdmin(accessToken);
  const idSeguro = validarIdSolicitud(id);

  return ejecutarAdminRequest(
    `${API_BASE_URL}/api/admin/imagenes/${idSeguro}/aprobar`,
    {
      method: "POST",
      headers: {
        "Accept": "application/json",
        "Authorization": authorization,
      },
    },
    esImagenAdmin
  );
}

export async function rechazarImagenAdmin(
  id: number,
  motivo: string,
  accessToken: string
): Promise<ImagenAdmin> {
  const authorization = construirAuthorizationAdmin(accessToken);
  const idSeguro = validarIdSolicitud(id);

  return ejecutarAdminRequest(
    `${API_BASE_URL}/api/admin/imagenes/${idSeguro}/rechazar`,
    {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "Accept": "application/json",
        "Authorization": authorization,
      },
      body: JSON.stringify({ motivo }),
    },
    esImagenAdmin
  );
}

function esImagenAdmin(valor: unknown): valor is ImagenAdmin {
  return (
    esObjeto(valor) &&
    typeof valor.id === "number" &&
    typeof valor.url === "string" &&
    typeof valor.tipoImagen === "string" &&
    typeof valor.estadoModeracion === "string" &&
    esStringONull(valor.motivoRechazo) &&
    typeof valor.activa === "boolean" &&
    esStringONull(valor.createdAt) &&
    esNumberONull(valor.actividadId) &&
    esStringONull(valor.actividadTitulo) &&
    esStringONull(valor.actividadSlug)
  );
}

function esImagenesAdminPage(valor: unknown): valor is ImagenesAdminPage {
  return (
    esObjeto(valor) &&
    Array.isArray(valor.contenido) &&
    valor.contenido.every(esImagenAdmin) &&
    typeof valor.paginaActual === "number" &&
    typeof valor.tamanioPagina === "number" &&
    typeof valor.totalElementos === "number" &&
    typeof valor.totalPaginas === "number" &&
    typeof valor.ultima === "boolean"
  );
}

async function ejecutarAdminRequest<T>(
  url: string,
  opciones: RequestInit,
  validador: ValidadorAdmin<T>
): Promise<T> {
  return ejecutarRequestJson(url, opciones, validador, {
    crearErrorConexion: (error) =>
      error instanceof AdminApiError
        ? error
        : new AdminApiError(
            "No fue posible conectar con el servidor del panel admin."
          ),
    crearErrorHttp: (status, cuerpo) => {
      if (esErrorResponseApi(cuerpo)) {
        return new AdminApiError(
          obtenerMensajeErrorAdmin(status, cuerpo.mensaje),
          {
            status,
            respuesta: cuerpo,
          }
        );
      }

      return new AdminApiError(obtenerMensajeErrorAdmin(status, null), {
        status,
      });
    },
    crearErrorFormatoInvalido: (status) =>
      new AdminApiError(
        "La respuesta del servidor no tiene el formato esperado.",
        { status }
      ),
  });
}

function construirAuthorizationAdmin(accessToken: string): string {
  return construirAuthorization(
    accessToken,
    () =>
      new AdminApiError("Necesitas iniciar sesion para usar el panel admin.")
  );
}

function validarIdSolicitud(id: number): number {
  return validarIdPositivo(
    id,
    () => new AdminApiError("El ID de la solicitud no es valido.")
  );
}

// Mensajes propios del panel admin segun el status HTTP.
// Se mantiene local (y no en apiHelpers) porque los textos son especificos
// de este panel.
function obtenerMensajeErrorAdmin(
  status: number,
  mensajeBackend: string | null
): string {
  const mensajeLimpio = mensajeBackend?.trim();

  if (mensajeLimpio) {
    return mensajeLimpio;
  }

  if (status === 401) {
    return "Tu sesion expiro o no es valida.";
  }

  if (status === 403) {
    return "No tenes permisos para acceder al panel admin.";
  }

  if (status === 404) {
    return "No encontramos la solicitud solicitada.";
  }

  return "No se pudo completar la operacion del panel admin.";
}

function esDiaSemanaSolicitudAdmin(
  valor: unknown
): valor is DiaSemanaSolicitudAdmin {
  return esValorDeLista(valor, DIAS_SEMANA_SOLICITUD_ADMIN);
}

function esEstadoSolicitudAdmin(valor: unknown): valor is EstadoSolicitudAdmin {
  return esValorDeLista(valor, ESTADOS_SOLICITUD_ADMIN);
}

function tieneCamposResumenAdmin(valor: Record<string, unknown>): boolean {
  return (
    typeof valor.id === "number" &&
    typeof valor.codigoSeguimiento === "string" &&
    esEstadoSolicitudAdmin(valor.estado) &&
    typeof valor.origen === "string" &&
    typeof valor.tipoPublicador === "string" &&
    typeof valor.nombrePublicador === "string" &&
    typeof valor.nombreActividad === "string" &&
    esNumberONull(valor.deporteId) &&
    esStringONull(valor.deporteNombre) &&
    esStringONull(valor.deporteOtro) &&
    esNumberONull(valor.ciudadId) &&
    esStringONull(valor.ciudadNombre) &&
    esStringONull(valor.ciudadOtra) &&
    esNumberONull(valor.barrioId) &&
    esStringONull(valor.barrioNombre) &&
    esStringONull(valor.barrioOtro) &&
    esStringONull(valor.email) &&
    esStringONull(valor.whatsapp) &&
    typeof valor.createdAt === "string" &&
    typeof valor.updatedAt === "string" &&
    esStringONull(valor.revisionIniciadaAt) &&
    esStringONull(valor.revisionFinalizadaAt)
  );
}

function esSolicitudPublicacionAdminResumen(
  valor: unknown
): valor is SolicitudPublicacionAdminResumen {
  return esObjeto(valor) && tieneCamposResumenAdmin(valor);
}

function esSolicitudPublicacionAdminRevisor(
  valor: unknown
): valor is SolicitudPublicacionAdminRevisor {
  return (
    esObjeto(valor) &&
    typeof valor.id === "number" &&
    typeof valor.nombre === "string" &&
    typeof valor.apellido === "string" &&
    typeof valor.email === "string" &&
    esStringONull(valor.rol)
  );
}

function esSolicitudPublicacionAdminHorario(
  valor: unknown
): valor is SolicitudPublicacionAdminHorario {
  return (
    esObjeto(valor) &&
    typeof valor.id === "number" &&
    esDiaSemanaSolicitudAdmin(valor.diaSemana) &&
    typeof valor.horaInicio === "string" &&
    typeof valor.horaFin === "string" &&
    esStringONull(valor.observacion)
  );
}

function esSolicitudPublicacionAdminDetalle(
  valor: unknown
): valor is SolicitudPublicacionAdminDetalle {
  return (
    esObjeto(valor) &&
    tieneCamposResumenAdmin(valor) &&
    typeof valor.descripcion === "string" &&
    typeof valor.nivel === "string" &&
    typeof valor.enfoque === "string" &&
    typeof valor.modalidad === "string" &&
    esNumberONull(valor.edadMinima) &&
    esNumberONull(valor.edadMaxima) &&
    esNumberONull(valor.precioReferencia) &&
    typeof valor.mostrarPrecio === "boolean" &&
    esStringONull(valor.nombreLugar) &&
    esStringONull(valor.direccion) &&
    esStringONull(valor.referenciaUbicacion) &&
    esStringONull(valor.instagram) &&
    esStringONull(valor.observacionesSolicitante) &&
    esStringONull(valor.motivoRechazo) &&
    esStringONull(valor.observacionesRevision) &&
    (valor.revisor === null || esSolicitudPublicacionAdminRevisor(valor.revisor)) &&
    esNumberONull(valor.actividadGeneradaId) &&
    Array.isArray(valor.horarios) &&
    valor.horarios.every(esSolicitudPublicacionAdminHorario)
  );
}

function esSolicitudPublicacionAprobacionResponse(
  valor: unknown
): valor is SolicitudPublicacionAprobacionResponse {
  return (
    esObjeto(valor) &&
    typeof valor.solicitudId === "number" &&
    valor.estado === "APROBADA" &&
    typeof valor.actividadId === "number" &&
    typeof valor.actividadSlug === "string" &&
    typeof valor.actividadTitulo === "string" &&
    typeof valor.mensaje === "string"
  );
}

function esSolicitudesPublicacionAdminPage(
  valor: unknown
): valor is SolicitudesPublicacionAdminPage {
  return (
    esObjeto(valor) &&
    Array.isArray(valor.contenido) &&
    valor.contenido.every(esSolicitudPublicacionAdminResumen) &&
    typeof valor.paginaActual === "number" &&
    typeof valor.tamanioPagina === "number" &&
    typeof valor.totalElementos === "number" &&
    typeof valor.totalPaginas === "number" &&
    typeof valor.ultima === "boolean"
  );
}
