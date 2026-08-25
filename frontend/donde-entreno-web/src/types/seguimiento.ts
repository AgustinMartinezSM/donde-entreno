import type { Actividad } from "./actividad";

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
  /* Logo aprobado (identidad única, fix UX 2026-08-22). Aditivo. */
  perfilLogoUrl?: string | null;
  /* Slug del perfil (script 27). Aditivo. */
  perfilSlug?: string | null;
};

/*
  Item del feed de novedades (GET /api/usuario/feed/actividades).
  El backend devuelve el ActividadDTO público completo (mismo shape que
  los listados): lo tipamos como Actividad para renderizar el feed con
  las mismas cards sociales que el resto del descubrimiento.
*/
export type ActividadFeed = Actividad;

/*
  Feed V2 (Fase 6): un HECHO de un publicador, no una actividad. El
  backend lo manda listo para pintar (identidad + actividad + foto),
  así la card no dispara ninguna llamada extra.
*/
export type FeedEvento = {
  id: number;
  tipo: string;
  resumen?: string | null;
  createdAt?: string | null;

  perfilPublicadorId?: number | null;
  perfilNombre?: string | null;
  perfilSlug?: string | null;
  perfilLogoUrl?: string | null;

  actividadId?: number | null;
  actividadTitulo?: string | null;
  actividadSlug?: string | null;
  actividadImagenUrl?: string | null;

  imagenId?: number | null;
  imagenUrl?: string | null;

  /* Novedad del canal (Fase 8): el texto completo, no el resumen. */
  novedadId?: number | null;
  novedadTexto?: string | null;
};

export type PaginaFeedEventos = {
  contenido: FeedEvento[];
  paginaActual: number;
  tamanioPagina: number;
  totalElementos: number;
  totalPaginas: number;
  ultima: boolean;
};
