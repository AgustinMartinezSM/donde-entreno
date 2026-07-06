import type {
  DiaSemanaSolicitudPublicacion,
  EstadoSolicitudPublicacion,
  SolicitudPublicacionRequest,
  SolicitudPublicacionResponse,
} from "./solicitudPublicacion";

export const ESTADOS_PERFIL_PUBLICADOR = [
  "INCOMPLETO",
  "PENDIENTE_REVISION",
  "ACTIVO",
  "SUSPENDIDO",
] as const;

export type EstadoPerfilPublicador =
  (typeof ESTADOS_PERFIL_PUBLICADOR)[number];

export const ORDENES_SOLICITUDES_PUBLICADOR = [
  "recientes",
  "antiguos",
] as const;

export type OrdenSolicitudesPublicador =
  (typeof ORDENES_SOLICITUDES_PUBLICADOR)[number];

export type PerfilPublicadorActual = {
  id: number;
  nombre: string;
  tipoPublicador: string;
  estado: EstadoPerfilPublicador | string;
  ciudadPrincipalId: number;
  ciudadPrincipalNombre: string;
  whatsapp: string;
  instagram: string | null;
  emailContacto: string | null;
  telefonoContacto: string | null;
  descripcion: string | null;
  activo: boolean;
  verificado: boolean;
};

export type SolicitudPublicadorResumen = {
  id: number;
  codigoSeguimiento: string;
  estado: EstadoSolicitudPublicacion;
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
  createdAt: string;
  updatedAt: string;
  revisionIniciadaAt: string | null;
  revisionFinalizadaAt: string | null;
  motivoRechazo: string | null;
};

export type SolicitudPublicadorHorario = {
  id: number;
  diaSemana: DiaSemanaSolicitudPublicacion;

  // LocalTime serializado por el backend.
  horaInicio: string;
  horaFin: string;

  observacion: string | null;
};

export type SolicitudPublicadorDetalle = SolicitudPublicadorResumen & {
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
  whatsapp: string | null;
  instagram: string | null;
  email: string | null;
  observacionesSolicitante: string | null;
  actividadGeneradaId: number | null;
  horarios: SolicitudPublicadorHorario[];
};

export type SolicitudesPublicadorPage = {
  contenido: SolicitudPublicadorResumen[];
  paginaActual: number;
  tamanioPagina: number;
  totalElementos: number;
  totalPaginas: number;
  ultima: boolean;
};

export type ListarSolicitudesPublicadorParams = {
  estado?: EstadoSolicitudPublicacion | "";
  page?: number;
  size?: number;
  orden?: OrdenSolicitudesPublicador;
};

export type CrearSolicitudPublicadorRequest = Omit<
  SolicitudPublicacionRequest,
  "tipoPublicador" | "nombrePublicador"
>;

export type CrearSolicitudPublicadorResponse = SolicitudPublicacionResponse;
