/*
  Tipos de la capa social (Bloque 8): seguir publicadores.
  Reflejan /api/usuario/seguimientos/publicadores.
*/

export type EstadoSeguimiento = {
  siguiendo: boolean;
};

export type PublicadorSeguido = {
  perfilPublicadorId: number;
  perfilPublicadorNombre: string;
  tipoPublicador: string | null;
  ciudadPrincipalNombre: string | null;
  seguidoDesde: string | null;
};

/*
  Item del feed de novedades (GET /api/usuario/feed/actividades).
  Es un subconjunto del ActividadDTO público: solo lo que la UI del
  feed necesita, para no acoplarse al DTO completo.
*/
export type ActividadFeed = {
  id: number;
  titulo: string;
  slug: string;
  deporteNombre: string | null;
  ciudadNombre: string | null;
  barrioNombre: string | null;
  perfilPublicadorNombre: string | null;
};
