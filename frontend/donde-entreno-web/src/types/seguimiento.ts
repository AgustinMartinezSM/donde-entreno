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
};

/*
  Item del feed de novedades (GET /api/usuario/feed/actividades).
  El backend devuelve el ActividadDTO público completo (mismo shape que
  los listados): lo tipamos como Actividad para renderizar el feed con
  las mismas cards sociales que el resto del descubrimiento.
*/
export type ActividadFeed = Actividad;
