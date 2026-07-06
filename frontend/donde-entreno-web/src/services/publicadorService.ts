import { API_BASE_URL } from "../lib/apiConfig";
import type {
  AuthErrorResponse,
  AuthErroresPorCampo,
} from "../types/auth";
import {
  DIAS_SEMANA_SOLICITUD,
  ESTADOS_SOLICITUD_PUBLICACION,
  type DiaSemanaSolicitudPublicacion,
  type EstadoSolicitudPublicacion,
} from "../types/solicitudPublicacion";
import type {
  CrearSolicitudPublicadorRequest,
  CrearSolicitudPublicadorResponse,
  EstadoPerfilPublicador,
  ListarSolicitudesPublicadorParams,
  PerfilPublicadorActual,
  SolicitudPublicadorDetalle,
  SolicitudPublicadorHorario,
  SolicitudPublicadorResumen,
  SolicitudesPublicadorPage,
} from "../types/publicador";
import { ESTADOS_PERFIL_PUBLICADOR } from "../types/publicador";

type PublicadorApiErrorOpciones = {
  status?: number | null;
  respuesta?: AuthErrorResponse | null;
  erroresPorCampo?: AuthErroresPorCampo | null;
};

type ValidadorPublicador<T> = (valor: unknown) => valor is T;

export class PublicadorApiError extends Error {
  status: number | null;
  respuesta: AuthErrorResponse | null;
  erroresPorCampo: AuthErroresPorCampo | null;

  constructor(message: string, opciones: PublicadorApiErrorOpciones = {}) {
    super(message);
    this.name = "PublicadorApiError";
    this.status = opciones.status ?? null;
    this.respuesta = opciones.respuesta ?? null;
    this.erroresPorCampo = opciones.erroresPorCampo ?? null;
  }
}

export async function obtenerPerfilPublicador(
  accessToken: string
): Promise<PerfilPublicadorActual> {
  return ejecutarPublicadorRequest(
    `${API_BASE_URL}/api/publicador/me`,
    {
      method: "GET",
      headers: {
        "Accept": "application/json",
        "Authorization": construirAuthorization(accessToken),
      },
      cache: "no-store",
    },
    esPerfilPublicadorActual
  );
}

export async function listarSolicitudesPublicador(
  params: ListarSolicitudesPublicadorParams,
  accessToken: string
): Promise<SolicitudesPublicadorPage> {
  return ejecutarPublicadorRequest(
    construirUrlListado(params),
    {
      method: "GET",
      headers: {
        "Accept": "application/json",
        "Authorization": construirAuthorization(accessToken),
      },
      cache: "no-store",
    },
    esSolicitudesPublicadorPage
  );
}

export async function obtenerSolicitudPublicador(
  id: number,
  accessToken: string
): Promise<SolicitudPublicadorDetalle> {
  const idSeguro = validarIdSolicitud(id);

  return ejecutarPublicadorRequest(
    `${API_BASE_URL}/api/publicador/solicitudes/${encodeURIComponent(
      String(idSeguro)
    )}`,
    {
      method: "GET",
      headers: {
        "Accept": "application/json",
        "Authorization": construirAuthorization(accessToken),
      },
      cache: "no-store",
    },
    esSolicitudPublicadorDetalle
  );
}

export async function crearSolicitudPublicador(
  request: CrearSolicitudPublicadorRequest,
  accessToken: string
): Promise<CrearSolicitudPublicadorResponse> {
  return ejecutarPublicadorRequest(
    `${API_BASE_URL}/api/publicador/solicitudes`,
    {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "Accept": "application/json",
        "Authorization": construirAuthorization(accessToken),
      },
      body: JSON.stringify(request),
    },
    esCrearSolicitudPublicadorResponse
  );
}

async function ejecutarPublicadorRequest<T>(
  url: string,
  opciones: RequestInit,
  validador: ValidadorPublicador<T>
): Promise<T> {
  let respuestaHttp: Response;

  try {
    respuestaHttp = await fetch(url, opciones);
  } catch (error: unknown) {
    if (error instanceof PublicadorApiError) {
      throw error;
    }

    throw new PublicadorApiError("No fue posible conectar con el servidor.");
  }

  const cuerpo: unknown = await leerJsonSeguro(respuestaHttp);

  if (!respuestaHttp.ok) {
    if (esAuthErrorResponse(cuerpo)) {
      throw new PublicadorApiError(
        obtenerMensajeErrorPublicador(respuestaHttp.status, cuerpo.mensaje),
        {
          status: respuestaHttp.status,
          respuesta: cuerpo,
          erroresPorCampo: cuerpo.errores,
        }
      );
    }

    throw new PublicadorApiError(
      obtenerMensajeErrorPublicador(respuestaHttp.status, null),
      {
        status: respuestaHttp.status,
      }
    );
  }

  if (!validador(cuerpo)) {
    throw new PublicadorApiError(
      "La respuesta del servidor no tiene el formato esperado.",
      {
        status: respuestaHttp.status,
      }
    );
  }

  return cuerpo;
}

function construirUrlListado(
  params: ListarSolicitudesPublicadorParams
): string {
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

  return `${API_BASE_URL}/api/publicador/solicitudes${
    queryString ? `?${queryString}` : ""
  }`;
}

function construirAuthorization(accessToken: string): string {
  const token = accessToken.trim();

  if (!token) {
    throw new PublicadorApiError(
      "Necesitas iniciar sesion para usar el panel publicador."
    );
  }

  return `Bearer ${token}`;
}

function validarIdSolicitud(id: number): number {
  if (!Number.isInteger(id) || id <= 0) {
    throw new PublicadorApiError("El ID de la solicitud no es valido.");
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

function obtenerMensajeErrorPublicador(
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
    return "No tenes permisos para acceder a esta seccion.";
  }

  if (status === 404) {
    return "No encontramos la solicitud solicitada.";
  }

  return "No se pudo completar la operacion del panel publicador.";
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

function esEstadoPerfilPublicador(
  valor: unknown
): valor is EstadoPerfilPublicador {
  return (
    typeof valor === "string" &&
    ESTADOS_PERFIL_PUBLICADOR.some((estado) => estado === valor)
  );
}

function esEstadoSolicitudPublicacion(
  valor: unknown
): valor is EstadoSolicitudPublicacion {
  return (
    typeof valor === "string" &&
    ESTADOS_SOLICITUD_PUBLICACION.some((estado) => estado === valor)
  );
}

function esDiaSemanaSolicitudPublicacion(
  valor: unknown
): valor is DiaSemanaSolicitudPublicacion {
  return (
    typeof valor === "string" &&
    DIAS_SEMANA_SOLICITUD.some((diaSemana) => diaSemana === valor)
  );
}

function esPerfilPublicadorActual(
  valor: unknown
): valor is PerfilPublicadorActual {
  return (
    esObjeto(valor) &&
    typeof valor.id === "number" &&
    typeof valor.nombre === "string" &&
    typeof valor.tipoPublicador === "string" &&
    esEstadoPerfilPublicador(valor.estado) &&
    typeof valor.ciudadPrincipalId === "number" &&
    typeof valor.ciudadPrincipalNombre === "string" &&
    typeof valor.whatsapp === "string" &&
    esStringONull(valor.instagram) &&
    esStringONull(valor.emailContacto) &&
    esStringONull(valor.telefonoContacto) &&
    esStringONull(valor.descripcion) &&
    typeof valor.activo === "boolean" &&
    typeof valor.verificado === "boolean"
  );
}

function tieneCamposResumenPublicador(valor: Record<string, unknown>): boolean {
  return (
    typeof valor.id === "number" &&
    typeof valor.codigoSeguimiento === "string" &&
    esEstadoSolicitudPublicacion(valor.estado) &&
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
    typeof valor.createdAt === "string" &&
    typeof valor.updatedAt === "string" &&
    esStringONull(valor.revisionIniciadaAt) &&
    esStringONull(valor.revisionFinalizadaAt) &&
    esStringONull(valor.motivoRechazo)
  );
}

function esSolicitudPublicadorResumen(
  valor: unknown
): valor is SolicitudPublicadorResumen {
  return esObjeto(valor) && tieneCamposResumenPublicador(valor);
}

function esSolicitudPublicadorHorario(
  valor: unknown
): valor is SolicitudPublicadorHorario {
  return (
    esObjeto(valor) &&
    typeof valor.id === "number" &&
    esDiaSemanaSolicitudPublicacion(valor.diaSemana) &&
    typeof valor.horaInicio === "string" &&
    typeof valor.horaFin === "string" &&
    esStringONull(valor.observacion)
  );
}

function esSolicitudPublicadorDetalle(
  valor: unknown
): valor is SolicitudPublicadorDetalle {
  return (
    esObjeto(valor) &&
    tieneCamposResumenPublicador(valor) &&
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
    esStringONull(valor.whatsapp) &&
    esStringONull(valor.instagram) &&
    esStringONull(valor.email) &&
    esStringONull(valor.observacionesSolicitante) &&
    esNumberONull(valor.actividadGeneradaId) &&
    Array.isArray(valor.horarios) &&
    valor.horarios.every(esSolicitudPublicadorHorario)
  );
}

function esSolicitudesPublicadorPage(
  valor: unknown
): valor is SolicitudesPublicadorPage {
  return (
    esObjeto(valor) &&
    Array.isArray(valor.contenido) &&
    valor.contenido.every(esSolicitudPublicadorResumen) &&
    typeof valor.paginaActual === "number" &&
    typeof valor.tamanioPagina === "number" &&
    typeof valor.totalElementos === "number" &&
    typeof valor.totalPaginas === "number" &&
    typeof valor.ultima === "boolean"
  );
}

function esCrearSolicitudPublicadorResponse(
  valor: unknown
): valor is CrearSolicitudPublicadorResponse {
  return (
    esObjeto(valor) &&
    typeof valor.id === "number" &&
    typeof valor.codigoSeguimiento === "string" &&
    esEstadoSolicitudPublicacion(valor.estado) &&
    typeof valor.createdAt === "string" &&
    typeof valor.mensaje === "string"
  );
}

function esAuthErroresPorCampo(
  valor: unknown
): valor is AuthErroresPorCampo {
  if (!esObjeto(valor)) {
    return false;
  }

  return Object.values(valor).every((mensaje) => typeof mensaje === "string");
}

function esAuthErrorResponse(valor: unknown): valor is AuthErrorResponse {
  return (
    esObjeto(valor) &&
    typeof valor.status === "number" &&
    typeof valor.error === "string" &&
    typeof valor.mensaje === "string" &&
    (valor.errores === null || esAuthErroresPorCampo(valor.errores)) &&
    typeof valor.path === "string" &&
    typeof valor.timestamp === "string"
  );
}
