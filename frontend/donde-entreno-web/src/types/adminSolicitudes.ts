export const ESTADOS_SOLICITUD_ADMIN = [
  "PENDIENTE",
  "EN_REVISION",
  "APROBADA",
  "RECHAZADA",
] as const;

export type EstadoSolicitudAdmin = (typeof ESTADOS_SOLICITUD_ADMIN)[number];

export type EstadoCambioSolicitudAdmin = "EN_REVISION" | "RECHAZADA";

export const ORDENES_SOLICITUDES_ADMIN = ["recientes"] as const;

export type OrdenSolicitudesAdmin =
  (typeof ORDENES_SOLICITUDES_ADMIN)[number];

export const DIAS_SEMANA_SOLICITUD_ADMIN = [
  "LUNES",
  "MARTES",
  "MIERCOLES",
  "JUEVES",
  "VIERNES",
  "SABADO",
  "DOMINGO",
] as const;

export type DiaSemanaSolicitudAdmin =
  (typeof DIAS_SEMANA_SOLICITUD_ADMIN)[number];

export type SolicitudPublicacionAdminResumen = {
  id: number;
  codigoSeguimiento: string;
  estado: EstadoSolicitudAdmin;
  origen: string;
  tipoPublicador: string;
  nombrePublicador: string;
  nombreActividad: string;
  deporteId: number | null;
  deporteNombre: string | null;
  deporteOtro: string | null;
  ciudadId: number | null;
  ciudadNombre: string | null;
  ciudadOtra: string | null;
  barrioId: number | null;
  barrioNombre: string | null;
  barrioOtro: string | null;
  email: string | null;
  whatsapp: string | null;
  createdAt: string;
  updatedAt: string;
  revisionIniciadaAt: string | null;
  revisionFinalizadaAt: string | null;
};

export type SolicitudesPublicacionAdminPage = {
  contenido: SolicitudPublicacionAdminResumen[];
  paginaActual: number;
  tamanioPagina: number;
  totalElementos: number;
  totalPaginas: number;
  ultima: boolean;
};

export type SolicitudPublicacionAdminRevisor = {
  id: number;
  nombre: string;
  apellido: string;
  email: string;
  rol: string | null;
};

export type SolicitudPublicacionAdminHorario = {
  id: number;
  diaSemana: DiaSemanaSolicitudAdmin;

  // LocalTime serializado por el backend.
  horaInicio: string;
  horaFin: string;

  observacion: string | null;
};

export type SolicitudPublicacionAdminDetalle =
  SolicitudPublicacionAdminResumen & {
    descripcion: string;
    nivel: string;
    enfoque: string;
    modalidad: string;
    edadMinima: number | null;
    edadMaxima: number | null;
    precioReferencia: number | null;
    mostrarPrecio: boolean;
    nombreLugar: string | null;
    direccion: string | null;
    referenciaUbicacion: string | null;
    instagram: string | null;
    observacionesSolicitante: string | null;
    motivoRechazo: string | null;
    observacionesRevision: string | null;
    revisor: SolicitudPublicacionAdminRevisor | null;
    actividadGeneradaId: number | null;
    horarios: SolicitudPublicacionAdminHorario[];
  };

export type CambiarEstadoSolicitudAdminRequest =
  | {
      estado: "EN_REVISION";
      motivoRechazo: null;
    }
  | {
      estado: "RECHAZADA";
      motivoRechazo: string;
    };

// El PATCH actual devuelve el detalle completo actualizado.
export type CambiarEstadoSolicitudAdminResponse =
  SolicitudPublicacionAdminDetalle;

export type SolicitudPublicacionAprobacionResponse = {
  solicitudId: number;
  estado: "APROBADA";
  actividadId: number;
  actividadSlug: string;
  actividadTitulo: string;
  mensaje: string;
};

export type AdminErroresPorCampo = Record<string, string>;

export type AdminErrorResponse = {
  status: number;
  error: string;
  mensaje: string;
  errores: AdminErroresPorCampo | null;
  path: string;

  // OffsetDateTime serializado.
  timestamp: string;
};
