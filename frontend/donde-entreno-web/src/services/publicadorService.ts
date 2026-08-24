import { API_BASE_URL } from "../lib/apiConfig";
import {
  construirAuthorization,
  construirQueryListado,
  ejecutarRequestJson,
  esBooleanONull,
  esErrorResponseApi,
  esNumberONull,
  esObjeto,
  esStringONull,
  esValorDeLista,
  validarIdPositivo,
} from "./apiHelpers";
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
  ActividadPublicadorDetalle,
  ActividadPublicadorHorario,
  ActividadPublicadorImagen,
  ImagenActividadPublicador,
  ActividadPublicadorResumen,
  ActividadesPublicadorPage,
  CrearSolicitudPublicadorRequest,
  CrearSolicitudPublicadorResponse,
  EstadoPerfilPublicador,
  ListarActividadesPublicadorParams,
  ListarSolicitudesPublicadorParams,
  MetricasPublicador,
  PerfilPublicadorActual,
  SolicitudPublicadorDetalle,
  SolicitudPublicadorHorario,
  SolicitudPublicadorResumen,
  SolicitudesPublicadorPage,
  CampoCambio,
  ListarSolicitudesCambioParams,
  SolicitudCambioDetalle,
  SolicitudCambioRequest,
  SolicitudCambioResumen,
  SolicitudesCambioPage,
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
        "Authorization": construirAuthorizationPublicador(accessToken),
      },
      cache: "no-store",
    },
    esPerfilPublicadorActual
  );
}

export async function obtenerMetricasPublicador(
  accessToken: string
): Promise<MetricasPublicador> {
  return ejecutarPublicadorRequest(
    `${API_BASE_URL}/api/publicador/metricas`,
    {
      method: "GET",
      headers: {
        "Accept": "application/json",
        "Authorization": construirAuthorizationPublicador(accessToken),
      },
      cache: "no-store",
    },
    esMetricasPublicador
  );
}

/*
  Campos de edición directa del perfil (los sensibles van por revisión).
  Semántica PATCH del backend: si un campo no viaja no se toca; si viaja
  vacío, se limpia.
*/
export type ActualizarPerfilPublicadorRequest = {
  /* Fase 5e: edición directa. Obligatorio en el schema — vacío = 400. */
  nombre?: string;
  descripcion?: string;
  instagram?: string;
  emailContacto?: string;
};

export async function actualizarPerfilPublicador(
  datos: ActualizarPerfilPublicadorRequest,
  accessToken: string
): Promise<PerfilPublicadorActual> {
  return ejecutarPublicadorRequest(
    `${API_BASE_URL}/api/publicador/me`,
    {
      method: "PATCH",
      headers: {
        "Content-Type": "application/json",
        "Accept": "application/json",
        "Authorization": construirAuthorizationPublicador(accessToken),
      },
      body: JSON.stringify(datos),
    },
    esPerfilPublicadorActual
  );
}

export async function listarSolicitudesPublicador(
  params: ListarSolicitudesPublicadorParams,
  accessToken: string
): Promise<SolicitudesPublicadorPage> {
  return ejecutarPublicadorRequest(
    `${API_BASE_URL}/api/publicador/solicitudes${construirQueryListado(params)}`,
    {
      method: "GET",
      headers: {
        "Accept": "application/json",
        "Authorization": construirAuthorizationPublicador(accessToken),
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
        "Authorization": construirAuthorizationPublicador(accessToken),
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
        "Authorization": construirAuthorizationPublicador(accessToken),
      },
      body: JSON.stringify(request),
    },
    esCrearSolicitudPublicadorResponse
  );
}

export async function listarActividadesPublicador(
  params: ListarActividadesPublicadorParams,
  accessToken: string
): Promise<ActividadesPublicadorPage> {
  return ejecutarPublicadorRequest(
    `${API_BASE_URL}/api/publicador/actividades${construirQueryListado(params)}`,
    {
      method: "GET",
      headers: {
        "Accept": "application/json",
        "Authorization": construirAuthorizationPublicador(accessToken),
      },
      cache: "no-store",
    },
    esActividadesPublicadorPage
  );
}

export async function obtenerActividadPublicador(
  id: number,
  accessToken: string
): Promise<ActividadPublicadorDetalle> {
  const idSeguro = validarIdActividad(id);

  return ejecutarPublicadorRequest(
    `${API_BASE_URL}/api/publicador/actividades/${encodeURIComponent(
      String(idSeguro)
    )}`,
    {
      method: "GET",
      headers: {
        "Accept": "application/json",
        "Authorization": construirAuthorizationPublicador(accessToken),
      },
      cache: "no-store",
    },
    esActividadPublicadorDetalle
  );
}

/**
 * Pausa (visible=false) o reanuda (visible=true) una actividad propia
 * (fase 6). El backend devuelve el detalle actualizado, con
 * estadoPublicacion PAUSADA o PUBLICADA.
 */
export async function cambiarVisibilidadActividad(
  id: number,
  visible: boolean,
  accessToken: string
): Promise<ActividadPublicadorDetalle> {
  const idSeguro = validarIdActividad(id);

  return ejecutarPublicadorRequest(
    `${API_BASE_URL}/api/publicador/actividades/${encodeURIComponent(
      String(idSeguro)
    )}/visibilidad`,
    {
      method: "PATCH",
      headers: {
        "Content-Type": "application/json",
        "Accept": "application/json",
        "Authorization": construirAuthorizationPublicador(accessToken),
      },
      body: JSON.stringify({ visible }),
    },
    esActividadPublicadorDetalle
  );
}

/*
  Actividades destacadas del publicador (Fase 5): hasta 3, ORDENADAS.
  El PUT reemplaza la selección completa; una lista vacía la limpia.
  El backend valida el tope y que cada actividad sea suya y publicada.
*/
export async function listarDestacadasPublicador(
  accessToken: string
): Promise<ActividadPublicadorResumen[]> {
  return ejecutarPublicadorRequest(
    `${API_BASE_URL}/api/publicador/actividades/destacadas`,
    {
      method: "GET",
      headers: {
        "Accept": "application/json",
        "Authorization": construirAuthorizationPublicador(accessToken),
      },
    },
    esListaDeActividadesPublicador
  );
}

export async function definirDestacadasPublicador(
  actividadIds: number[],
  accessToken: string
): Promise<ActividadPublicadorResumen[]> {
  return ejecutarPublicadorRequest(
    `${API_BASE_URL}/api/publicador/actividades/destacadas`,
    {
      method: "PUT",
      headers: {
        "Content-Type": "application/json",
        "Accept": "application/json",
        "Authorization": construirAuthorizationPublicador(accessToken),
      },
      body: JSON.stringify({ actividadIds }),
    },
    esListaDeActividadesPublicador
  );
}

function esListaDeActividadesPublicador(
  valor: unknown
): valor is ActividadPublicadorResumen[] {
  return (
    Array.isArray(valor) &&
    valor.every(
      (item) =>
        typeof item === "object" &&
        item !== null &&
        typeof (item as ActividadPublicadorResumen).id === "number"
    )
  );
}

// ============================================================
// Solicitudes de cambio sobre actividades publicadas
// ============================================================

/*
  Crea una solicitud de cambio sobre una actividad publicada propia.
  El backend valida ownership, dominios y que haya una sola solicitud
  abierta por actividad (409).
*/
export async function crearSolicitudCambio(
  actividadId: number,
  datos: SolicitudCambioRequest,
  accessToken: string
): Promise<SolicitudCambioDetalle> {
  const idSeguro = validarIdActividad(actividadId);

  return ejecutarPublicadorRequest(
    `${API_BASE_URL}/api/publicador/actividades/${encodeURIComponent(
      String(idSeguro)
    )}/solicitudes-cambio`,
    {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "Accept": "application/json",
        "Authorization": construirAuthorizationPublicador(accessToken),
      },
      body: JSON.stringify(datos),
    },
    esSolicitudCambioDetalle
  );
}

export async function listarSolicitudesCambio(
  params: ListarSolicitudesCambioParams,
  accessToken: string
): Promise<SolicitudesCambioPage> {
  return ejecutarPublicadorRequest(
    `${API_BASE_URL}/api/publicador/solicitudes-cambio${construirQueryListado(params)}`,
    {
      method: "GET",
      headers: {
        "Accept": "application/json",
        "Authorization": construirAuthorizationPublicador(accessToken),
      },
      cache: "no-store",
    },
    esSolicitudesCambioPage
  );
}

export async function obtenerSolicitudCambio(
  id: number,
  accessToken: string
): Promise<SolicitudCambioDetalle> {
  const idSeguro = validarIdSolicitud(id);

  return ejecutarPublicadorRequest(
    `${API_BASE_URL}/api/publicador/solicitudes-cambio/${encodeURIComponent(
      String(idSeguro)
    )}`,
    {
      method: "GET",
      headers: {
        "Accept": "application/json",
        "Authorization": construirAuthorizationPublicador(accessToken),
      },
      cache: "no-store",
    },
    esSolicitudCambioDetalle
  );
}

function esCampoCambio(valor: unknown): valor is CampoCambio {
  return (
    esObjeto(valor) &&
    typeof valor.campo === "string" &&
    esStringONull(valor.valorActual) &&
    typeof valor.valorPropuesto === "string"
  );
}

function esSolicitudCambioResumen(valor: unknown): valor is SolicitudCambioResumen {
  return (
    esObjeto(valor) &&
    typeof valor.id === "number" &&
    esNumberONull(valor.actividadId) &&
    esStringONull(valor.actividadTitulo) &&
    typeof valor.estado === "string" &&
    Array.isArray(valor.camposPropuestos) &&
    valor.camposPropuestos.every((campo) => typeof campo === "string") &&
    esStringONull(valor.createdAt)
  );
}

/*
  Exportados para reutilizarse desde el service del panel admin
  (mismos DTOs de solicitudes de cambio en ambos flujos).
*/
export function esSolicitudCambioDetalle(valor: unknown): valor is SolicitudCambioDetalle {
  return (
    esObjeto(valor) &&
    typeof valor.id === "number" &&
    esNumberONull(valor.actividadId) &&
    esStringONull(valor.actividadTitulo) &&
    esStringONull(valor.actividadSlug) &&
    esNumberONull(valor.perfilPublicadorId) &&
    esStringONull(valor.perfilPublicadorNombre) &&
    typeof valor.estado === "string" &&
    esStringONull(valor.motivoRechazo) &&
    esStringONull(valor.resueltoAt) &&
    esStringONull(valor.createdAt) &&
    Array.isArray(valor.cambios) &&
    valor.cambios.every(esCampoCambio)
  );
}

export function esSolicitudesCambioPage(valor: unknown): valor is SolicitudesCambioPage {
  return (
    esObjeto(valor) &&
    Array.isArray(valor.contenido) &&
    valor.contenido.every(esSolicitudCambioResumen) &&
    typeof valor.paginaActual === "number" &&
    typeof valor.tamanioPagina === "number" &&
    typeof valor.totalElementos === "number" &&
    typeof valor.totalPaginas === "number" &&
    typeof valor.ultima === "boolean"
  );
}

async function ejecutarPublicadorRequest<T>(
  url: string,
  opciones: RequestInit,
  validador: ValidadorPublicador<T>
): Promise<T> {
  return ejecutarRequestJson(url, opciones, validador, {
    crearErrorConexion: (error) =>
      error instanceof PublicadorApiError
        ? error
        : new PublicadorApiError("No fue posible conectar con el servidor."),
    crearErrorHttp: (status, cuerpo) => {
      if (esErrorResponseApi(cuerpo)) {
        return new PublicadorApiError(
          obtenerMensajeErrorPublicador(status, cuerpo.mensaje),
          {
            status,
            respuesta: cuerpo,
            erroresPorCampo: cuerpo.errores,
          }
        );
      }

      return new PublicadorApiError(
        obtenerMensajeErrorPublicador(status, null),
        { status }
      );
    },
    crearErrorFormatoInvalido: (status) =>
      new PublicadorApiError(
        "La respuesta del servidor no tiene el formato esperado.",
        { status }
      ),
  });
}

function construirAuthorizationPublicador(accessToken: string): string {
  return construirAuthorization(
    accessToken,
    () =>
      new PublicadorApiError(
        "Necesitas iniciar sesion para usar el panel publicador."
      )
  );
}

function validarIdSolicitud(id: number): number {
  return validarIdPositivo(
    id,
    () => new PublicadorApiError("El ID de la solicitud no es valido.")
  );
}

function validarIdActividad(id: number): number {
  return validarIdPositivo(
    id,
    () => new PublicadorApiError("El ID de la actividad no es valido.")
  );
}

// Mensajes propios del panel publicador segun el status HTTP.
// Se mantiene local (y no en apiHelpers) porque los textos son especificos
// de este panel.
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

function esEstadoPerfilPublicador(
  valor: unknown
): valor is EstadoPerfilPublicador {
  return esValorDeLista(valor, ESTADOS_PERFIL_PUBLICADOR);
}

function esEstadoSolicitudPublicacion(
  valor: unknown
): valor is EstadoSolicitudPublicacion {
  return esValorDeLista(valor, ESTADOS_SOLICITUD_PUBLICACION);
}

function esDiaSemanaSolicitudPublicacion(
  valor: unknown
): valor is DiaSemanaSolicitudPublicacion {
  return esValorDeLista(valor, DIAS_SEMANA_SOLICITUD);
}

function esMetricasPublicador(valor: unknown): valor is MetricasPublicador {
  return (
    esObjeto(valor) &&
    typeof valor.actividadesPublicadas === "number" &&
    (valor.actividadesPausadas === undefined ||
      typeof valor.actividadesPausadas === "number") &&
    typeof valor.solicitudesPublicacionPendientes === "number" &&
    typeof valor.solicitudesCambioPendientes === "number" &&
    typeof valor.imagenesPendientesModeracion === "number" &&
    typeof valor.seguidores === "number"
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

function tieneCamposResumenActividadPublicador(
  valor: Record<string, unknown>
): boolean {
  return (
    typeof valor.id === "number" &&
    typeof valor.titulo === "string" &&
    typeof valor.slug === "string" &&
    esStringONull(valor.deporteNombre) &&
    esStringONull(valor.deporteSlug) &&
    esStringONull(valor.categoriaDeportivaNombre) &&
    esStringONull(valor.ciudadNombre) &&
    esStringONull(valor.ciudadSlug) &&
    esStringONull(valor.barrioNombre) &&
    typeof valor.estadoPublicacion === "string" &&
    typeof valor.activa === "boolean" &&
    esStringONull(valor.modalidad) &&
    esStringONull(valor.nivel) &&
    esNumberONull(valor.edadMinima) &&
    esNumberONull(valor.edadMaxima) &&
    esNumberONull(valor.precioReferencia) &&
    esBooleanONull(valor.mostrarPrecio) &&
    esStringONull(valor.imagenPrincipalUrl) &&
    esStringONull(valor.createdAt) &&
    esStringONull(valor.slugPublico)
  );
}

function esActividadPublicadorResumen(
  valor: unknown
): valor is ActividadPublicadorResumen {
  return esObjeto(valor) && tieneCamposResumenActividadPublicador(valor);
}

function esActividadPublicadorHorario(
  valor: unknown
): valor is ActividadPublicadorHorario {
  return (
    esObjeto(valor) &&
    typeof valor.id === "number" &&
    typeof valor.diaSemana === "string" &&
    typeof valor.horaInicio === "string" &&
    typeof valor.horaFin === "string" &&
    esStringONull(valor.observacion)
  );
}

function esActividadPublicadorImagen(
  valor: unknown
): valor is ActividadPublicadorImagen {
  return (
    esObjeto(valor) &&
    typeof valor.id === "number" &&
    typeof valor.url === "string" &&
    esStringONull(valor.tipoImagen) &&
    esStringONull(valor.titulo) &&
    esStringONull(valor.descripcion) &&
    esNumberONull(valor.orden)
  );
}

function esActividadPublicadorDetalle(
  valor: unknown
): valor is ActividadPublicadorDetalle {
  return (
    esObjeto(valor) &&
    tieneCamposResumenActividadPublicador(valor) &&
    esStringONull(valor.descripcion) &&
    esStringONull(valor.enfoque) &&
    esBooleanONull(valor.requiereInscripcion) &&
    esBooleanONull(valor.cuposLimitados) &&
    esStringONull(valor.nombreLugar) &&
    esStringONull(valor.direccion) &&
    esStringONull(valor.referenciaUbicacion) &&
    esStringONull(valor.whatsapp) &&
    esStringONull(valor.instagram) &&
    esStringONull(valor.email) &&
    esNumberONull(valor.perfilPublicadorId) &&
    esStringONull(valor.perfilPublicadorNombre) &&
    esStringONull(valor.perfilPublicadorTipo) &&
    esNumberONull(valor.solicitudOrigenId) &&
    esStringONull(valor.solicitudCodigoSeguimiento) &&
    Array.isArray(valor.horarios) &&
    valor.horarios.every(esActividadPublicadorHorario) &&
    Array.isArray(valor.imagenes) &&
    valor.imagenes.every(esActividadPublicadorImagen)
  );
}

function esActividadesPublicadorPage(
  valor: unknown
): valor is ActividadesPublicadorPage {
  return (
    esObjeto(valor) &&
    Array.isArray(valor.contenido) &&
    valor.contenido.every(esActividadPublicadorResumen) &&
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

// ============================================================
// Imágenes de actividades (subida con moderación, Supabase Storage)
// ============================================================

/*
  Sube una imagen para una actividad publicada propia. La imagen nace
  PENDIENTE (archivo en el bucket privado): no se ve en público hasta
  que el equipo la apruebe.
  Multipart: NO se setea Content-Type (el navegador arma el boundary).
*/
export async function subirImagenActividad(
  actividadId: number,
  archivo: File,
  tipo: "PRINCIPAL" | "GALERIA",
  accessToken: string
): Promise<ImagenActividadPublicador> {
  const idSeguro = validarIdPositivo(
    actividadId,
    () => new PublicadorApiError("El id de la actividad es invalido.")
  );

  const formData = new FormData();
  formData.append("archivo", archivo);
  formData.append("tipo", tipo);

  return ejecutarPublicadorRequest(
    `${API_BASE_URL}/api/publicador/actividades/${encodeURIComponent(
      String(idSeguro)
    )}/imagenes`,
    {
      method: "POST",
      headers: {
        "Accept": "application/json",
        "Authorization": construirAuthorizationPublicador(accessToken),
      },
      body: formData,
    },
    esImagenActividadPublicador
  );
}

export async function listarImagenesActividad(
  actividadId: number,
  accessToken: string
): Promise<ImagenActividadPublicador[]> {
  const idSeguro = validarIdPositivo(
    actividadId,
    () => new PublicadorApiError("El id de la actividad es invalido.")
  );

  return ejecutarPublicadorRequest(
    `${API_BASE_URL}/api/publicador/actividades/${encodeURIComponent(
      String(idSeguro)
    )}/imagenes`,
    {
      method: "GET",
      headers: {
        "Accept": "application/json",
        "Authorization": construirAuthorizationPublicador(accessToken),
      },
      cache: "no-store",
    },
    esListaImagenesActividadPublicador
  );
}

/*
  Retira una imagen propia PENDIENTE (el archivo se elimina del bucket
  privado). El backend responde 204 sin cuerpo, por eso el validador
  acepta null.
*/
export async function eliminarImagenActividad(
  actividadId: number,
  imagenId: number,
  accessToken: string
): Promise<void> {
  const idSeguro = validarIdPositivo(
    actividadId,
    () => new PublicadorApiError("El id de la actividad es invalido.")
  );
  const imagenIdSeguro = validarIdPositivo(
    imagenId,
    () => new PublicadorApiError("El id de la imagen es invalido.")
  );

  await ejecutarPublicadorRequest(
    `${API_BASE_URL}/api/publicador/actividades/${encodeURIComponent(
      String(idSeguro)
    )}/imagenes/${encodeURIComponent(String(imagenIdSeguro))}`,
    {
      method: "DELETE",
      headers: {
        "Accept": "application/json",
        "Authorization": construirAuthorizationPublicador(accessToken),
      },
    },
    esCuerpoVacioImagen
  );
}

/**
 * Orden manual de la galería (fase 2): la lista trae TODAS las fotos
 * GALERIA activas de la actividad en el orden deseado.
 */
export async function ordenarImagenesActividad(
  actividadId: number,
  imagenIds: number[],
  accessToken: string
): Promise<void> {
  const idSeguro = validarIdPositivo(
    actividadId,
    () => new PublicadorApiError("El id de la actividad es invalido.")
  );

  await ejecutarPublicadorRequest(
    `${API_BASE_URL}/api/publicador/actividades/${encodeURIComponent(
      String(idSeguro)
    )}/imagenes/orden`,
    {
      method: "PUT",
      headers: {
        "Content-Type": "application/json",
        "Accept": "application/json",
        "Authorization": construirAuthorizationPublicador(accessToken),
      },
      body: JSON.stringify({ imagenIds }),
    },
    esCuerpoVacioImagen
  );
}

/**
 * Promueve una foto aprobada de la galería a imagen principal (fase 2),
 * sin re-moderación: es un swap con la principal vigente.
 */
export async function elegirImagenPrincipal(
  actividadId: number,
  imagenId: number,
  accessToken: string
): Promise<void> {
  const idSeguro = validarIdPositivo(
    actividadId,
    () => new PublicadorApiError("El id de la actividad es invalido.")
  );
  const imagenIdSeguro = validarIdPositivo(
    imagenId,
    () => new PublicadorApiError("El id de la imagen es invalido.")
  );

  await ejecutarPublicadorRequest(
    `${API_BASE_URL}/api/publicador/actividades/${encodeURIComponent(
      String(idSeguro)
    )}/imagenes/${encodeURIComponent(String(imagenIdSeguro))}/principal`,
    {
      method: "PUT",
      headers: {
        "Accept": "application/json",
        "Authorization": construirAuthorizationPublicador(accessToken),
      },
    },
    esCuerpoVacioImagen
  );
}

/**
 * Título y descripción de una imagen (fase 2): alimentan el texto
 * alternativo/epígrafe públicos. Semántica PATCH: null no toca, vacío
 * limpia.
 */
export async function actualizarTextoImagen(
  actividadId: number,
  imagenId: number,
  cambios: {
    titulo?: string;
    descripcion?: string;
    /* Fase 4 (galería social): sección y toggle de comentarios. */
    seccion?: string;
    comentariosActivados?: boolean;
  },
  accessToken: string
): Promise<ImagenActividadPublicador> {
  const idSeguro = validarIdPositivo(
    actividadId,
    () => new PublicadorApiError("El id de la actividad es invalido.")
  );
  const imagenIdSeguro = validarIdPositivo(
    imagenId,
    () => new PublicadorApiError("El id de la imagen es invalido.")
  );

  return ejecutarPublicadorRequest(
    `${API_BASE_URL}/api/publicador/actividades/${encodeURIComponent(
      String(idSeguro)
    )}/imagenes/${encodeURIComponent(String(imagenIdSeguro))}`,
    {
      method: "PATCH",
      headers: {
        "Content-Type": "application/json",
        "Accept": "application/json",
        "Authorization": construirAuthorizationPublicador(accessToken),
      },
      body: JSON.stringify(cambios),
    },
    esImagenActividadPublicador
  );
}

/*
  Logo y portada del perfil. Mismo circuito de moderación que las
  imágenes de actividad, pero cuelgan del perfil: el backend las resuelve
  con el publicador del token, así que acá no viaja ningún id.
*/
export async function subirImagenPerfil(
  archivo: File,
  tipo: "LOGO" | "PORTADA",
  accessToken: string
): Promise<ImagenActividadPublicador> {
  const formData = new FormData();
  formData.append("archivo", archivo);
  formData.append("tipo", tipo);

  return ejecutarPublicadorRequest(
    `${API_BASE_URL}/api/publicador/perfil/imagenes`,
    {
      method: "POST",
      headers: {
        "Accept": "application/json",
        "Authorization": construirAuthorizationPublicador(accessToken),
      },
      body: formData,
    },
    esImagenActividadPublicador
  );
}

export async function listarImagenesPerfil(
  accessToken: string
): Promise<ImagenActividadPublicador[]> {
  return ejecutarPublicadorRequest(
    `${API_BASE_URL}/api/publicador/perfil/imagenes`,
    {
      method: "GET",
      headers: {
        "Accept": "application/json",
        "Authorization": construirAuthorizationPublicador(accessToken),
      },
      cache: "no-store",
    },
    esListaImagenesActividadPublicador
  );
}

export async function eliminarImagenPerfil(
  imagenId: number,
  accessToken: string
): Promise<void> {
  const imagenIdSeguro = validarIdPositivo(
    imagenId,
    () => new PublicadorApiError("El id de la imagen es invalido.")
  );

  await ejecutarPublicadorRequest(
    `${API_BASE_URL}/api/publicador/perfil/imagenes/${encodeURIComponent(
      String(imagenIdSeguro)
    )}`,
    {
      method: "DELETE",
      headers: {
        "Accept": "application/json",
        "Authorization": construirAuthorizationPublicador(accessToken),
      },
    },
    esCuerpoVacioImagen
  );
}

export function esImagenActividadPublicador(
  valor: unknown
): valor is ImagenActividadPublicador {
  return (
    esObjeto(valor) &&
    typeof valor.id === "number" &&
    esStringONull(valor.url) &&
    typeof valor.tipoImagen === "string" &&
    typeof valor.estadoModeracion === "string" &&
    esStringONull(valor.motivoRechazo) &&
    typeof valor.activa === "boolean" &&
    /* Campos de fase 2: se validan solo si el backend ya los manda. */
    (valor.orden === undefined ||
      valor.orden === null ||
      typeof valor.orden === "number") &&
    (valor.titulo === undefined || esStringONull(valor.titulo)) &&
    (valor.descripcion === undefined || esStringONull(valor.descripcion)) &&
    esStringONull(valor.createdAt)
  );
}

function esListaImagenesActividadPublicador(
  valor: unknown
): valor is ImagenActividadPublicador[] {
  return Array.isArray(valor) && valor.every(esImagenActividadPublicador);
}

function esCuerpoVacioImagen(valor: unknown): valor is null {
  return valor === null;
}
