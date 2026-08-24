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

export const ORDENES_ACTIVIDADES_PUBLICADOR = [
  "recientes",
  "antiguos",
  "titulo_asc",
] as const;

export type OrdenActividadesPublicador =
  (typeof ORDENES_ACTIVIDADES_PUBLICADOR)[number];

/*
  Métricas de resumen del panel del publicador (solo lectura).
  Reflejan GET /api/publicador/metricas.
*/
export type MetricasPublicador = {
  actividadesPublicadas: number;
  /*
    Pausadas (fase 6). Opcional para tolerar el orden de los deploys:
    un backend sin el campo simplemente no muestra el conteo.
  */
  actividadesPausadas?: number;
  solicitudesPublicacionPendientes: number;
  solicitudesCambioPendientes: number;
  imagenesPendientesModeracion: number;
  seguidores: number;
  /*
    Fase 2/3 social (aditivos, opcionales por el orden de deploys):
    tracking anónimo de 30 días e interés en probar.
  */
  vistas30Dias?: number;
  contactosWhatsapp30Dias?: number;
  quierenProbar?: number;
  /*
    Fase 5: contactos que salieron del PERFIL, separados de los de cada
    actividad — así se ve si convierte la vidriera o la propuesta.
  */
  contactosDesdePerfil30Dias?: number;
};

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

export type ActividadPublicadorResumen = {
  id: number;
  titulo: string;
  slug: string;
  deporteNombre: string | null;
  deporteSlug: string | null;
  categoriaDeportivaNombre: string | null;
  ciudadNombre: string | null;
  ciudadSlug: string | null;
  barrioNombre: string | null;
  estadoPublicacion: string;
  activa: boolean;
  modalidad: string | null;
  nivel: string | null;
  edadMinima: number | null;
  edadMaxima: number | null;
  precioReferencia: number | null;
  mostrarPrecio: boolean | null;
  imagenPrincipalUrl: string | null;
  createdAt: string | null;
  slugPublico: string | null;
};

export type ActividadPublicadorHorario = {
  id: number;
  diaSemana: string;
  horaInicio: string;
  horaFin: string;
  observacion: string | null;
};

/*
  Sede del publicador (Fase 7). `latitud`/`longitud` llegan como string
  desde el backend (BigDecimal) y son null mientras nadie cargó el
  punto — que es el caso de la mayoría hoy.
*/
export type UbicacionPublicador = {
  id: number;
  nombre?: string | null;
  direccion?: string | null;
  barrioNombre?: string | null;
  ciudadNombre?: string | null;
  latitud?: number | string | null;
  longitud?: number | string | null;
  googleMapsUrl?: string | null;
};

export type ActividadPublicadorImagen = {
  id: number;
  url: string;
  tipoImagen: string | null;
  titulo: string | null;
  descripcion: string | null;
  orden: number | null;
};

export type ActividadPublicadorDetalle = ActividadPublicadorResumen & {
  descripcion: string | null;
  enfoque: string | null;
  requiereInscripcion: boolean | null;
  cuposLimitados: boolean | null;
  nombreLugar: string | null;
  direccion: string | null;
  referenciaUbicacion: string | null;
  whatsapp: string | null;
  instagram: string | null;
  email: string | null;
  perfilPublicadorId: number | null;
  perfilPublicadorNombre: string | null;
  perfilPublicadorTipo: string | null;
  solicitudOrigenId: number | null;
  solicitudCodigoSeguimiento: string | null;
  horarios: ActividadPublicadorHorario[];
  imagenes: ActividadPublicadorImagen[];
};

export type ActividadesPublicadorPage = {
  contenido: ActividadPublicadorResumen[];
  paginaActual: number;
  tamanioPagina: number;
  totalElementos: number;
  totalPaginas: number;
  ultima: boolean;
};

export type ListarActividadesPublicadorParams = {
  page?: number;
  size?: number;
  orden?: OrdenActividadesPublicador;
};

// ============================================================
// Solicitudes de cambio sobre actividades publicadas
// ============================================================

/*
  Campos que se pueden proponer cambiar (V1). Un campo ausente
  significa "sin cambio propuesto".
*/
export type SolicitudCambioHorarioRequest = {
  diaSemana: string;
  horaInicio: string;
  horaFin: string;
  observacion?: string;
};

export type SolicitudCambioRequest = {
  titulo?: string;
  descripcion?: string;
  precioReferencia?: number;
  mostrarPrecio?: boolean;
  whatsappContacto?: string;
  instagramContacto?: string;
  emailContacto?: string;
  nivel?: string;
  modalidad?: string;
  deporteId?: number;
  edadMinima?: number;
  edadMaxima?: number;
  enfoque?: string;
  ubicacionNombre?: string;
  ubicacionDireccion?: string;
  ubicacionReferencia?: string;
  ubicacionBarrioId?: number;
  /* true = reemplazar el conjunto de horarios por `horarios` (>= 1). */
  cambiaHorarios?: boolean;
  horarios?: SolicitudCambioHorarioRequest[];
};

export type CampoCambio = {
  campo: string;
  valorActual: string | null;
  valorPropuesto: string;
};

export type SolicitudCambioResumen = {
  id: number;
  actividadId: number | null;
  actividadTitulo: string | null;
  estado: string;
  camposPropuestos: string[];
  createdAt: string | null;
};

export type SolicitudCambioDetalle = {
  id: number;
  actividadId: number | null;
  actividadTitulo: string | null;
  actividadSlug: string | null;
  perfilPublicadorId: number | null;
  perfilPublicadorNombre: string | null;
  estado: string;
  motivoRechazo: string | null;
  resueltoAt: string | null;
  createdAt: string | null;
  cambios: CampoCambio[];
};

export type SolicitudesCambioPage = {
  contenido: SolicitudCambioResumen[];
  paginaActual: number;
  tamanioPagina: number;
  totalElementos: number;
  totalPaginas: number;
  ultima: boolean;
};

export type ListarSolicitudesCambioParams = {
  estado?: string;
  page?: number;
  size?: number;
  orden?: "recientes" | "antiguos";
};

/*
  Imagen de una actividad vista desde el panel del publicador
  (subida con moderación). La url es la visualizable según estado:
  pública si está aprobada, firmada temporal si está pendiente,
  null si fue rechazada (el archivo ya no existe en el storage).
*/
export type ImagenActividadPublicador = {
  id: number;
  url: string | null;
  tipoImagen: string;
  estadoModeracion: "PENDIENTE" | "APROBADA" | "RECHAZADA" | string;
  motivoRechazo: string | null;
  activa: boolean;
  /*
    Fase 2 (controles del publicador): orden manual y alt/epígrafe.
    Opcionales para tolerar el orden de los deploys: un backend viejo
    simplemente no los manda.
  */
  orden?: number | null;
  titulo?: string | null;
  descripcion?: string | null;
  /*
    Fase 4 (galería social): sección de galería y toggle de comentarios.
    Opcionales por la misma razón que arriba.
  */
  seccion?: string | null;
  comentariosActivados?: boolean | null;
  createdAt: string | null;
};
