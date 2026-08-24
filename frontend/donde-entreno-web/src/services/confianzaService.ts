import { API_BASE_URL } from "../lib/apiConfig";

/*
  Fase 3 social (script 29): intereses (quiero probar / ya probé),
  valoraciones y preguntas.
*/

export type EstadoInteres = "QUIERO_PROBAR" | "YA_PROBE" | null;

export type ValoracionPublica = {
  id: number;
  puntaje: number;
  comentario: string | null;
  tags: string[];
  verificada: boolean;
  autorNombre: string;
  esPropia: boolean;
  createdAt: string | null;
  /*
    Fase 5: solo vienen en el listado del PERFIL, donde se mezclan
    reseñas de varias actividades. En el detalle son null (ahí es obvio).
  */
  actividadTitulo?: string | null;
  actividadSlug?: string | null;
};

export type ResumenValoraciones = {
  promedio: number | null;
  cantidad: number;
  distribucion: Record<string, number>;
  contenido: ValoracionPublica[];
};

export type PreguntaActividad = {
  id: number;
  pregunta: string;
  respuesta: string | null;
  respondidaAt: string | null;
  esPropia: boolean;
  createdAt: string | null;
  /* Fase 5: ídem, solo en el listado del perfil. */
  actividadTitulo?: string | null;
  actividadSlug?: string | null;
};

export const TAGS_VALORACION = [
  { valor: "BUEN_AMBIENTE", etiqueta: "Buen ambiente" },
  { valor: "IDEAL_PRINCIPIANTES", etiqueta: "Ideal principiantes" },
  { valor: "PROFES_ATENTOS", etiqueta: "Profes atentos" },
  { valor: "BUENA_UBICACION", etiqueta: "Buena ubicación" },
  { valor: "MUY_INTENSO", etiqueta: "Muy intenso" },
  { valor: "INSTALACIONES_COMODAS", etiqueta: "Instalaciones cómodas" },
] as const;

export class ConfianzaApiError extends Error {
  status: number | null;

  constructor(message: string, status: number | null = null) {
    super(message);
    this.name = "ConfianzaApiError";
    this.status = status;
  }
}

/* ============================ Intereses ============================ */

export async function marcarInteres(
  accessToken: string,
  actividadId: number,
  estado: "QUIERO_PROBAR" | "YA_PROBE"
): Promise<void> {
  await ejecutar(
    `${API_BASE_URL}/api/usuario/intereses/${actividadId}`,
    "PUT",
    accessToken,
    { estado }
  );
}

export async function quitarInteres(
  accessToken: string,
  actividadId: number
): Promise<void> {
  await ejecutar(
    `${API_BASE_URL}/api/usuario/intereses/${actividadId}`,
    "DELETE",
    accessToken
  );
}

export async function obtenerInteres(
  accessToken: string,
  actividadId: number
): Promise<EstadoInteres> {
  const data = (await ejecutar(
    `${API_BASE_URL}/api/usuario/intereses/${actividadId}`,
    "GET",
    accessToken
  )) as { estado?: string | null };

  return (data.estado as EstadoInteres) ?? null;
}

/* =========================== Valoraciones =========================== */

export async function obtenerValoraciones(
  actividadId: number,
  accessToken?: string | null
): Promise<ResumenValoraciones> {
  return (await ejecutar(
    `${API_BASE_URL}/api/actividades/${actividadId}/valoraciones`,
    "GET",
    accessToken ?? undefined
  )) as ResumenValoraciones;
}

export async function enviarValoracion(
  accessToken: string,
  actividadId: number,
  puntaje: number,
  comentario: string,
  tags: string[]
): Promise<ValoracionPublica> {
  return (await ejecutar(
    `${API_BASE_URL}/api/usuario/valoraciones/${actividadId}`,
    "PUT",
    accessToken,
    {
      puntaje,
      ...(comentario.trim() ? { comentario: comentario.trim() } : {}),
      ...(tags.length > 0 ? { tags } : {}),
    }
  )) as ValoracionPublica;
}

export async function eliminarValoracion(
  accessToken: string,
  actividadId: number
): Promise<void> {
  await ejecutar(
    `${API_BASE_URL}/api/usuario/valoraciones/${actividadId}`,
    "DELETE",
    accessToken
  );
}

/* ============================ Preguntas ============================ */

export async function obtenerPreguntas(
  actividadId: number,
  accessToken?: string | null
): Promise<PreguntaActividad[]> {
  return (await ejecutar(
    `${API_BASE_URL}/api/actividades/${actividadId}/preguntas`,
    "GET",
    accessToken ?? undefined
  )) as PreguntaActividad[];
}

export async function enviarPregunta(
  accessToken: string,
  actividadId: number,
  pregunta: string
): Promise<PreguntaActividad> {
  return (await ejecutar(
    `${API_BASE_URL}/api/usuario/preguntas`,
    "POST",
    accessToken,
    { actividadId: String(actividadId), pregunta: pregunta.trim() }
  )) as PreguntaActividad;
}

export async function eliminarPregunta(
  accessToken: string,
  preguntaId: number
): Promise<void> {
  await ejecutar(
    `${API_BASE_URL}/api/usuario/preguntas/${preguntaId}`,
    "DELETE",
    accessToken
  );
}

export async function responderPregunta(
  accessToken: string,
  preguntaId: number,
  respuesta: string
): Promise<PreguntaActividad> {
  return (await ejecutar(
    `${API_BASE_URL}/api/publicador/preguntas/${preguntaId}/respuesta`,
    "POST",
    accessToken,
    { respuesta: respuesta.trim() }
  )) as PreguntaActividad;
}

/* ============================== Común ============================== */

async function ejecutar(
  url: string,
  method: "GET" | "PUT" | "POST" | "DELETE",
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
    throw new ConfianzaApiError("No fue posible conectar con el servidor.");
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

    throw new ConfianzaApiError(mensaje, respuesta.status);
  }

  if (respuesta.status === 204) {
    return null;
  }

  return respuesta.json();
}
