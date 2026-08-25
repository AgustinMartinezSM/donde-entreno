import { API_BASE_URL } from "../lib/apiConfig";

/*
  Eventos y calendario (script 35, Fase 9). El rango lo resuelve el
  BACKEND: "este finde" es una pregunta sobre el calendario de Mar del
  Plata, no sobre el reloj del dispositivo de quien mira.
*/

export type RangoEventos = "hoy" | "finde" | "semana" | "proximos";

export type Evento = {
  id: number;
  slug: string;
  titulo: string;
  descripcion: string;

  iniciaAt: string;
  terminaAt: string | null;

  cupo: number | null;
  esGratis: boolean | null;
  precioReferencia: number | null;
  mostrarPrecio: boolean | null;

  /** PUBLICADO o CANCELADO. */
  estado: string;

  perfilPublicadorId: number | null;
  perfilNombre: string | null;
  perfilSlug: string | null;
  perfilLogoUrl: string | null;
  whatsappContacto: string | null;

  deporteId: number | null;
  deporteNombre: string | null;
  deporteSlug: string | null;

  sedeNombre: string | null;
  direccion: string | null;
  ciudadNombre: string | null;
  ciudadSlug: string | null;
  barrioNombre: string | null;
  latitud: number | null;
  longitud: number | null;

  actividadId: number | null;
  actividadTitulo: string | null;
  actividadSlug: string | null;

  imagenId: number | null;
  imagenUrl: string | null;

  cantidadInteresados: number | null;
  meInteresa: boolean | null;
};

export type PaginaEventos = {
  contenido: Evento[];
  paginaActual: number;
  tamanioPagina: number;
  totalElementos: number;
  totalPaginas: number;
  ultima: boolean;
};

export const RANGOS_EVENTOS: Array<{ valor: RangoEventos; etiqueta: string }> = [
  { valor: "hoy", etiqueta: "Hoy" },
  { valor: "finde", etiqueta: "Este finde" },
  { valor: "semana", etiqueta: "Esta semana" },
  { valor: "proximos", etiqueta: "Todos los próximos" },
];

export const MAX_TITULO_EVENTO = 150;
export const MAX_DESCRIPCION_EVENTO = 2000;

export class EventosApiError extends Error {
  status: number | null;

  constructor(message: string, status: number | null = null) {
    super(message);
    this.name = "EventosApiError";
    this.status = status;
  }
}

export async function obtenerCalendario(
  filtros: {
    rango?: RangoEventos;
    ciudadSlug?: string | null;
    deporteId?: number | null;
    page?: number;
    size?: number;
  } = {},
  accessToken?: string | null
): Promise<PaginaEventos> {
  const parametros = new URLSearchParams();
  parametros.set("rango", filtros.rango ?? "proximos");
  if (filtros.ciudadSlug) {
    parametros.set("ciudadSlug", filtros.ciudadSlug);
  }
  if (filtros.deporteId) {
    parametros.set("deporteId", String(filtros.deporteId));
  }
  parametros.set("page", String(filtros.page ?? 0));
  parametros.set("size", String(filtros.size ?? 12));

  return (await ejecutar(
    `${API_BASE_URL}/api/eventos?${parametros.toString()}`,
    "GET",
    accessToken ?? undefined
  )) as PaginaEventos;
}

export async function obtenerEventoPorSlug(
  slug: string,
  accessToken?: string | null
): Promise<Evento> {
  return (await ejecutar(
    `${API_BASE_URL}/api/eventos/${encodeURIComponent(slug)}`,
    "GET",
    accessToken ?? undefined
  )) as Evento;
}

/** Los próximos de una actividad, para el aviso en su detalle. */
export async function obtenerEventosDeActividad(
  actividadId: number,
  limite = 3
): Promise<Evento[]> {
  return (await ejecutar(
    `${API_BASE_URL}/api/eventos/de-actividad/${actividadId}?limite=${limite}`,
    "GET"
  )) as Evento[];
}

/** Los próximos de un publicador (solapa de su perfil público). */
export async function obtenerEventosDePerfil(
  perfilPublicadorId: number,
  limite = 10
): Promise<Evento[]> {
  return (await ejecutar(
    `${API_BASE_URL}/api/perfiles-publicadores/${perfilPublicadorId}/eventos?limite=${limite}`,
    "GET"
  )) as Evento[];
}

/* ========================= Publicador ========================= */

export async function obtenerMisEventos(accessToken: string): Promise<Evento[]> {
  return (await ejecutar(
    `${API_BASE_URL}/api/publicador/eventos`,
    "GET",
    accessToken
  )) as Evento[];
}

export type DatosNuevoEvento = {
  titulo: string;
  descripcion: string;
  /** ISO con offset: el backend rechaza lo que no parsea. */
  iniciaAt: string;
  terminaAt?: string | null;
  ubicacionId?: number | null;
  deporteId?: number | null;
  actividadId?: number | null;
  imagenId?: number | null;
  cupo?: number | null;
  esGratis?: boolean;
  precioReferencia?: number | null;
  mostrarPrecio?: boolean;
};

export async function publicarEvento(
  accessToken: string,
  datos: DatosNuevoEvento
): Promise<Evento> {
  const cuerpo: Record<string, string> = {
    titulo: datos.titulo.trim(),
    descripcion: datos.descripcion.trim(),
    iniciaAt: datos.iniciaAt,
  };

  if (datos.terminaAt) {
    cuerpo.terminaAt = datos.terminaAt;
  }
  if (datos.ubicacionId) {
    cuerpo.ubicacionId = String(datos.ubicacionId);
  }
  if (datos.deporteId) {
    cuerpo.deporteId = String(datos.deporteId);
  }
  if (datos.actividadId) {
    cuerpo.actividadId = String(datos.actividadId);
  }
  if (datos.imagenId) {
    cuerpo.imagenId = String(datos.imagenId);
  }
  if (datos.cupo) {
    cuerpo.cupo = String(datos.cupo);
  }
  if (datos.esGratis !== undefined) {
    cuerpo.esGratis = String(datos.esGratis);
  }
  if (datos.precioReferencia) {
    cuerpo.precioReferencia = String(datos.precioReferencia);
  }
  if (datos.mostrarPrecio !== undefined) {
    cuerpo.mostrarPrecio = String(datos.mostrarPrecio);
  }

  return (await ejecutar(
    `${API_BASE_URL}/api/publicador/eventos`,
    "POST",
    accessToken,
    cuerpo
  )) as Evento;
}

/** Cancelar NO es borrar: el detalle sigue vivo diciendo que se canceló. */
export async function cancelarEvento(
  accessToken: string,
  eventoId: number
): Promise<void> {
  await ejecutar(
    `${API_BASE_URL}/api/publicador/eventos/${eventoId}/cancelar`,
    "PATCH",
    accessToken
  );
}

export async function eliminarEvento(
  accessToken: string,
  eventoId: number
): Promise<void> {
  await ejecutar(
    `${API_BASE_URL}/api/publicador/eventos/${eventoId}`,
    "DELETE",
    accessToken
  );
}

/* ========================= Me interesa ========================= */

export type RespuestaInteres = {
  cantidadInteresados: number;
  meInteresa: boolean;
};

export async function marcarInteres(
  accessToken: string,
  eventoId: number
): Promise<RespuestaInteres> {
  return (await ejecutar(
    `${API_BASE_URL}/api/usuario/eventos/${eventoId}/interes`,
    "PUT",
    accessToken
  )) as RespuestaInteres;
}

export async function quitarInteres(
  accessToken: string,
  eventoId: number
): Promise<RespuestaInteres> {
  return (await ejecutar(
    `${API_BASE_URL}/api/usuario/eventos/${eventoId}/interes`,
    "DELETE",
    accessToken
  )) as RespuestaInteres;
}

/** El admin lo oculta (moderación reactiva desde la cola de reportes). */
export async function ocultarEventoAdmin(
  accessToken: string,
  eventoId: number
): Promise<void> {
  await ejecutar(
    `${API_BASE_URL}/api/admin/eventos/${eventoId}/ocultar`,
    "PATCH",
    accessToken
  );
}

/* ============================== Común ============================== */

async function ejecutar(
  url: string,
  method: "GET" | "POST" | "PUT" | "PATCH" | "DELETE",
  accessToken?: string,
  cuerpo?: unknown
): Promise<unknown> {
  let respuesta: Response;

  try {
    respuesta = await fetch(url, {
      method,
      headers: {
        "Accept": "application/json",
        ...(accessToken ? { "Authorization": `Bearer ${accessToken}` } : {}),
        ...(cuerpo !== undefined ? { "Content-Type": "application/json" } : {}),
      },
      ...(cuerpo !== undefined ? { body: JSON.stringify(cuerpo) } : {}),
      cache: "no-store",
    });
  } catch {
    throw new EventosApiError("No fue posible conectar con el servidor.");
  }

  if (!respuesta.ok) {
    let mensaje = "Algo salió mal. Probá nuevamente.";

    try {
      const cuerpoError: unknown = await respuesta.json();
      if (
        typeof cuerpoError === "object" &&
        cuerpoError !== null &&
        typeof (cuerpoError as { mensaje?: unknown }).mensaje === "string"
      ) {
        mensaje = (cuerpoError as { mensaje: string }).mensaje;
      }
    } catch {
      /* Cuerpo ilegible: queda el genérico. */
    }

    throw new EventosApiError(mensaje, respuesta.status);
  }

  if (respuesta.status === 204) {
    return null;
  }

  return respuesta.json();
}
