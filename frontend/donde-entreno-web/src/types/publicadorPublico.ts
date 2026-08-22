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
  /*
    Campo aditivo: tolera respuestas viejas del backend, así el frontend
    no depende del orden de los deploys.
  */
  cantidadSeguidores?: number | null;
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
};
