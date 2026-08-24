/*
  Perfil público de publicador (GET /api/perfiles-publicadores).
  Es el DTO público del backend: sin datos privados de la cuenta.
*/
export type PerfilPublicadorPublico = {
  id: number;
  nombre: string;
  tipoPublicador?: string | null;
  descripcion?: string | null;
  emailContacto?: string | null;
  telefonoContacto?: string | null;
  whatsapp?: string | null;
  instagram?: string | null;
  sitioWeb?: string | null;
  verificado?: boolean | null;
  /* Logo aprobado (identidad única, fix UX 2026-08-22). Aditivo. */
  logoUrl?: string | null;
  /* URL amigable (script 27). Aditivo: null en perfiles sin backfill. */
  slug?: string | null;
  /*
    Campo aditivo: tolera respuestas viejas del backend, así el frontend
    no depende del orden de los deploys.
  */
  cantidadSeguidores?: number | null;

  /*
    Stats de cabecera (Fase 5). Aditivos por la misma razón.
    valoracionPromedio es null hasta juntar 3 valoraciones — misma
    regla que en el detalle de actividad, para que los dos números no
    se contradigan en pantalla.
  */
  cantidadActividades?: number | null;
  cantidadFotos?: number | null;
  valoracionPromedio?: number | null;
  cantidadValoraciones?: number | null;
};

/*
  Imagen pública de un perfil publicador
  (GET /api/perfiles-publicadores/{id}/imagenes; el backend ya filtra
  por estado APROBADA + activa). tipoImagen: LOGO | PORTADA | GALERIA.
*/
export type ImagenPerfilPublicador = {
  id: number;
  url: string;
  tipoImagen?: string | null;
  titulo?: string | null;
  descripcion?: string | null;
  orden?: number | null;
  /* Likes públicos (bloque 14). Opcional: backend viejo no lo trae. */
  cantidadLikes?: number | null;
  /* Fase 4 (galería social). Opcionales por el orden de los deploys. */
  cantidadComentarios?: number | null;
  comentariosActivados?: boolean | null;
  seccion?: string | null;

  /*
    Fase 5: el endpoint agregado de fotos mezcla las del perfil con las
    de sus actividades, así que cada foto dice de cuál viene.
  */
  actividadId?: number | null;
  actividadSlug?: string | null;
};
