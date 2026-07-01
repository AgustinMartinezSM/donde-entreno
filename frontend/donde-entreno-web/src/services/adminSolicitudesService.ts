import { API_BASE_URL } from "../lib/apiConfig";
import {
  DIAS_SEMANA_SOLICITUD_ADMIN,
  ESTADOS_SOLICITUD_ADMIN,
  type AdminErrorResponse,
  type AdminErroresPorCampo,
  type CambiarEstadoSolicitudAdminRequest,
  type CambiarEstadoSolicitudAdminResponse,
  type DiaSemanaSolicitudAdmin,
  type EstadoSolicitudAdmin,
  type OrdenSolicitudesAdmin,
  type SolicitudPublicacionAdminDetalle,
  type SolicitudPublicacionAdminHorario,
  type SolicitudPublicacionAdminResumen,
  type SolicitudPublicacionAdminRevisor,
  type SolicitudesPublicacionAdminPage,
} from "../types/adminSolicitudes";

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
  const authorization = construirAuthorization(accessToken);
  const url = construirUrlListado(params);

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
  const authorization = construirAuthorization(accessToken);
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
  const authorization = construirAuthorization(accessToken);
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

async function ejecutarAdminRequest<T>(
  url: string,
  opciones: RequestInit,
  validador: ValidadorAdmin<T>
): Promise<T> {
  let respuestaHttp: Response;

  try {
    respuestaHttp = await fetch(url, opciones);
  } catch (error: unknown) {
    if (error instanceof AdminApiError) {
      throw error;
    }

    throw new AdminApiError(
      "No fue posible conectar con el servidor del panel admin."
    );
  }

  const cuerpo: unknown = await leerJsonSeguro(respuestaHttp);

  if (!respuestaHttp.ok) {
    if (esAdminErrorResponse(cuerpo)) {
      throw new AdminApiError(
        obtenerMensajeErrorAdmin(respuestaHttp.status, cuerpo.mensaje),
        {
          status: respuestaHttp.status,
          respuesta: cuerpo,
        }
      );
    }

    throw new AdminApiError(
      obtenerMensajeErrorAdmin(respuestaHttp.status, null),
      {
        status: respuestaHttp.status,
      }
    );
  }

  if (!validador(cuerpo)) {
    throw new AdminApiError(
      "La respuesta del servidor no tiene el formato esperado.",
      {
        status: respuestaHttp.status,
      }
    );
  }

  return cuerpo;
}

function construirUrlListado(params: ListarSolicitudesAdminParams): string {
  const parametros = new URLSearchParams();

  if (params.estado) {
    parametros.set("estado", params.estado);
  }

  if (typeof params.page === "number" && Number.isFinite(params.page)) {
    parametros.set("page", String(params.page));
  }

  if (typeof params.size === "number" && Number.isFinite(params.size)) {
    parametros.set("size", String(params.size));
  }

  if (params.orden) {
    parametros.set("orden", params.orden);
  }

  const queryString = parametros.toString();

  return `${API_BASE_URL}/api/admin/solicitudes-publicacion${
    queryString ? `?${queryString}` : ""
  }`;
}

function construirAuthorization(accessToken: string): string {
  const token = accessToken.trim();

  if (!token) {
    throw new AdminApiError("Necesitas iniciar sesion para usar el panel admin.");
  }

  return `Bearer ${token}`;
}

function validarIdSolicitud(id: number): number {
  if (!Number.isInteger(id) || id <= 0) {
    throw new AdminApiError("El ID de la solicitud no es valido.");
  }

  return id;
}

async function leerJsonSeguro(respuesta: Response): Promise<unknown> {
  try {
    const cuerpo: unknown = await respuesta.json();
    return cuerpo;
  } catch {
    return null;
  }
}

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

function esObjeto(valor: unknown): valor is Record<string, unknown> {
  return typeof valor === "object" && valor !== null && !Array.isArray(valor);
}

function esStringONull(valor: unknown): valor is string | null {
  return typeof valor === "string" || valor === null;
}

function esNumberONull(valor: unknown): valor is number | null {
  return typeof valor === "number" || valor === null;
}

function esDiaSemanaSolicitudAdmin(
  valor: unknown
): valor is DiaSemanaSolicitudAdmin {
  return (
    typeof valor === "string" &&
    DIAS_SEMANA_SOLICITUD_ADMIN.some((dia) => dia === valor)
  );
}

function esEstadoSolicitudAdmin(valor: unknown): valor is EstadoSolicitudAdmin {
  return (
    typeof valor === "string" &&
    ESTADOS_SOLICITUD_ADMIN.some((estado) => estado === valor)
  );
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

function esAdminErroresPorCampo(
  valor: unknown
): valor is AdminErroresPorCampo {
  if (!esObjeto(valor)) {
    return false;
  }

  return Object.values(valor).every((mensaje) => typeof mensaje === "string");
}

function esAdminErrorResponse(valor: unknown): valor is AdminErrorResponse {
  return (
    esObjeto(valor) &&
    typeof valor.status === "number" &&
    typeof valor.error === "string" &&
    typeof valor.mensaje === "string" &&
    (valor.errores === null || esAdminErroresPorCampo(valor.errores)) &&
    typeof valor.path === "string" &&
    typeof valor.timestamp === "string"
  );
}
