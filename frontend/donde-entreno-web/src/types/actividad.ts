// Representa una actividad deportiva que viene desde el backend.
// Por ahora dejamos algunos campos como opcionales porque todavía
// estamos validando exactamente qué devuelve el endpoint.
export type Actividad = {
  id: number;
  titulo: string;
  slug: string;

  // Datos descriptivos
  descripcion?: string;
  edadMinima?: number | null;
  edadMaxima?: number | null;
  nivel?: string;
  enfoque?: string;
  modalidad?: string;

  // Datos de ubicación
  ubicacionId?: number;
  ubicacionNombre?: string;
  ciudadId?: number;
  ciudadNombre?: string;
  ciudadSlug?: string;
  barrioId?: number;
  barrioNombre?: string;
  direccion?: string;

  // Datos relacionados al deporte o categoría
  deporteId?: number;
  deporteNombre?: string;
  deporteSlug?: string;
  categoriaDeportivaId?: number;
  categoriaDeportivaNombre?: string;
  categoriaDeportivaSlug?: string;

  // Datos del publicador
  perfilPublicadorId?: number;
  perfilPublicadorNombre?: string;
  tipoPublicador?: string;
  perfilVerificado?: boolean;

  // Datos visuales o económicos
  imagenPrincipalUrl?: string | null;
  /*
    Logo aprobado del publicador (identidad única, fix UX 2026-08-22).
    Opcional: backend viejo no lo trae y el avatar cae a iniciales.
  */
  perfilLogoUrl?: string | null;
  precioReferencia?: number | null;
  mostrarPrecio?: boolean;
  requiereInscripcion?: boolean;
  cuposLimitados?: boolean;

  whatsappContacto?: string | null;
  instagramContacto?: string | null;
  emailContacto?: string | null;

  /*
    Fecha en que la actividad quedó publicada (ISO-8601). Opcional para
    tolerar respuestas de un backend anterior al campo.
  */
  fechaPublicacion?: string | null;
};

// Representa la respuesta paginada del endpoint GET /api/actividades.
export type PaginaActividades = {
  contenido: Actividad[];
  paginaActual: number;
  tamanioPagina: number;
  totalElementos: number;
  totalPaginas: number;
  ultima: boolean;
};

// Representa el detalle completo de una actividad.
// Lo usamos en GET /api/actividades/{slug}/detalle.
// Dejamos varios campos opcionales hasta confirmar la respuesta exacta del backend.
// Representa el detalle completo que devuelve:
// GET /api/actividades/{slug}/detalle
// Representa el detalle completo normalizado para usar en el frontend.
// El backend devuelve actividad + horarios + imagenes separados,
// pero desde el frontend lo vamos a usar todo junto.
export type ActividadDetalle = {
  id: number;
  titulo: string;
  slug: string;
  descripcion: string;

  edadMinima?: number | null;
  edadMaxima?: number | null;

  nivel?: string;
  enfoque?: string;
  modalidad?: string;

  precioReferencia?: number | null;
  mostrarPrecio?: boolean;
  requiereInscripcion?: boolean;
  cuposLimitados?: boolean;

  whatsappContacto?: string | null;
  instagramContacto?: string | null;
  emailContacto?: string | null;

  /*
    El detalle envuelve el mismo ActividadDTO público que los listados:
    los campos reales del publicador son tipoPublicador y perfilVerificado
    (el viejo perfilPublicadorTipo nunca llegaba en este endpoint).
  */
  perfilPublicadorId?: number | null;
  perfilPublicadorNombre?: string | null;
  tipoPublicador?: string | null;
  perfilVerificado?: boolean;
  /* Logo aprobado del publicador (identidad única). Aditivo. */
  perfilLogoUrl?: string | null;

  deporteId?: number;
  deporteNombre?: string;
  deporteSlug?: string;

  categoriaDeportivaId?: number;
  categoriaDeportivaNombre?: string;
  categoriaDeportivaSlug?: string;

  ubicacionId?: number;
  ubicacionNombre?: string;
  direccion?: string;

  ciudadId?: number;
  ciudadNombre?: string;
  ciudadSlug?: string;

  barrioId?: number;
  barrioNombre?: string;

  fechaPublicacion?: string | null;

  horarios?: HorarioActividad[];
  imagenes?: ImagenActividad[];

  /* Señales agregadas de confianza (script 26). Aditivo. */
  socialProof?: SocialProofActividad | null;
};

/*
  Social proof del detalle: números agregados y anónimos. El frontend
  muestra cada señal solo si es mayor que cero.
*/
export type SocialProofActividad = {
  cantidadFavoritos: number;
  cantidadLikesFotos: number;
  cantidadPersonasEntrenaron30Dias: number;
};

export type HorarioActividad = {
  id: number;
  diaSemana: string;
  horaInicio: string;
  horaFin: string;
  observacion?: string | null;

  actividadId?: number;
  actividadTitulo?: string;
  actividadSlug?: string;
};

export type ImagenActividad = {
  id: number;
  url: string;
  tipoImagen?: string;
  titulo?: string;
  descripcion?: string;
  orden?: number;

  actividadId?: number;
  actividadSlug?: string;

  /* Likes públicos de la foto (bloque 14). Opcional: backend viejo no lo trae. */
  cantidadLikes?: number | null;
};
