// Catálogos exactos aceptados por el endpoint POST /api/solicitudes-publicacion.
export const TIPOS_PUBLICADOR_SOLICITUD = [
  "CLUB",
  "GIMNASIO",
  "PROFESOR_INDEPENDIENTE",
  "INSTITUCION",
  "ESCUELA_DEPORTIVA",
  "ESPACIO_ENTRENAMIENTO",
] as const;

export type TipoPublicadorSolicitud =
  (typeof TIPOS_PUBLICADOR_SOLICITUD)[number];

export const NIVELES_SOLICITUD = [
  "PRINCIPIANTE",
  "INTERMEDIO",
  "AVANZADO",
  "TODOS",
] as const;

export type NivelSolicitudPublicacion = (typeof NIVELES_SOLICITUD)[number];

export const ENFOQUES_SOLICITUD = [
  "RECREATIVO",
  "COMPETITIVO",
  "MIXTO",
] as const;

export type EnfoqueSolicitudPublicacion = (typeof ENFOQUES_SOLICITUD)[number];

export const MODALIDADES_SOLICITUD = ["PRESENCIAL", "MIXTA"] as const;

export type ModalidadSolicitudPublicacion =
  (typeof MODALIDADES_SOLICITUD)[number];

export const DIAS_SEMANA_SOLICITUD = [
  "LUNES",
  "MARTES",
  "MIERCOLES",
  "JUEVES",
  "VIERNES",
  "SABADO",
  "DOMINGO",
] as const;

export type DiaSemanaSolicitudPublicacion =
  (typeof DIAS_SEMANA_SOLICITUD)[number];

export const ESTADOS_SOLICITUD_PUBLICACION = [
  "PENDIENTE",
  "EN_REVISION",
  "APROBADA",
  "RECHAZADA",
] as const;

export type EstadoSolicitudPublicacion =
  (typeof ESTADOS_SOLICITUD_PUBLICACION)[number];

export type SolicitudPublicacionHorarioRequest = {
  diaSemana: DiaSemanaSolicitudPublicacion;

  // Se envía en formato HH:mm y debe ser anterior a horaFin.
  horaInicio: string;

  // Se envía en formato HH:mm.
  horaFin: string;

  // Admite null. El backend rechaza horarios duplicados por día, hora inicial y hora final.
  observacion: string | null;
};

// Request normalizado: los campos no informados se envían como null cuando corresponde.
export type SolicitudPublicacionRequest = {
  tipoPublicador: TipoPublicadorSolicitud;
  nombrePublicador: string;
  nombreActividad: string;

  // Debe existir exactamente uno entre deporteId y deporteOtro.
  deporteId: number | null;
  deporteOtro: string | null;

  descripcion: string;
  nivel: NivelSolicitudPublicacion;
  enfoque: EnfoqueSolicitudPublicacion;
  modalidad: ModalidadSolicitudPublicacion;
  edadMinima: number | null;
  edadMaxima: number | null;

  // mostrarPrecio true requiere precioReferencia.
  precioReferencia: number | null;
  mostrarPrecio: boolean | null;

  // Debe existir exactamente uno entre ciudadId y ciudadOtra.
  ciudadId: number | null;
  ciudadOtra: string | null;

  /*
    Debe existir exactamente uno entre barrioId y barrioOtro.
    barrioId requiere ciudadId y debe pertenecer a esa ciudad.
    ciudadOtra obliga a usar barrioOtro.
    barrioOtro sí puede combinarse con ciudadId.
  */
  barrioId: number | null;
  barrioOtro: string | null;

  // Debe existir nombreLugar o direccion.
  nombreLugar: string | null;
  direccion: string | null;

  referenciaUbicacion: string | null;

  // Debe existir whatsapp o email.
  whatsapp: string | null;
  instagram: string | null;
  email: string | null;

  observacionesSolicitante: string | null;

  // Debe ser true.
  aceptaCondiciones: boolean;

  // Debe contener al menos un horario.
  horarios: SolicitudPublicacionHorarioRequest[];
};

export type SolicitudPublicacionResponse = {
  id: number;
  codigoSeguimiento: string;

  // En la creación exitosa el estado esperado es PENDIENTE.
  estado: EstadoSolicitudPublicacion;

  // OffsetDateTime serializado en formato ISO.
  createdAt: string;
  mensaje: string;
};

export type SolicitudPublicacionErroresPorCampo = Record<string, string>;

export type SolicitudPublicacionErrorResponse = {
  status: number;
  error: string;
  mensaje: string;

  /*
    En Bean Validation, errores es un objeto por campo.
    Puede ser null en errores generales, JSON inválido, reglas de negocio, 404 o 500.
    Nunca es una lista según el DTO actual.
  */
  errores: SolicitudPublicacionErroresPorCampo | null;

  path: string;

  // OffsetDateTime serializado.
  timestamp: string;
};
