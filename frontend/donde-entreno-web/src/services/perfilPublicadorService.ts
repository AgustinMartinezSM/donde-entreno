import { API_BASE_URL } from "../lib/apiConfig";
import type {
  PreguntaActividad,
  ResumenValoraciones,
} from "./confianzaService";
import type {
  ImagenPerfilPublicador,
  PerfilPublicadorPublico,
} from "../types/publicadorPublico";

/*
  Service del listado público de perfiles publicadores.

  El endpoint GET /api/perfiles-publicadores existe en el backend desde
  el inicio pero el frontend nunca lo consumió: acá lo usamos para las
  secciones sociales ("publicadores para seguir"). Validamos la forma de
  cada item y descartamos los malformados en lugar de romper la página.
*/
export async function obtenerPerfilesPublicadores(): Promise<
  PerfilPublicadorPublico[]
> {
  const respuesta = await fetch(`${API_BASE_URL}/api/perfiles-publicadores`, {
    cache: "no-store",
  });

  if (!respuesta.ok) {
    throw new Error("No se pudieron obtener los perfiles publicadores");
  }

  const data: unknown = await respuesta.json();

  if (!Array.isArray(data)) {
    throw new Error(
      "La respuesta de perfiles publicadores no tiene el formato esperado"
    );
  }

  return data.filter(esPerfilPublicadorPublico);
}

/*
  Perfil individual contra GET /api/perfiles-publicadores/{id}.

  Antes se resolvía filtrando el listado público completo, y como la
  página lo pide dos veces por vista (generateMetadata y el render, los
  dos con no-store), cada perfil visitado descargaba la lista entera dos
  veces.

  404 devuelve null para que la página haga notFound(); cualquier otro
  error se propaga y la página muestra su estado de error.
*/
export async function obtenerPerfilPublicadorPorId(
  idOSlug: number | string
): Promise<PerfilPublicadorPublico | null> {
  const respuesta = await fetch(
    `${API_BASE_URL}/api/perfiles-publicadores/${encodeURIComponent(String(idOSlug))}`,
    { cache: "no-store" }
  );

  if (respuesta.status === 404) {
    return null;
  }

  if (!respuesta.ok) {
    throw new Error("No se pudo obtener el perfil publicador");
  }

  const data: unknown = await respuesta.json();

  return esPerfilPublicadorPublico(data) ? data : null;
}

/*
  Imágenes públicas del perfil (LOGO/PORTADA/GALERIA). El backend ya
  filtra APROBADA + activa, así que acá solo validamos la forma.
*/
export async function obtenerImagenesPerfilPublicador(
  id: number
): Promise<ImagenPerfilPublicador[]> {
  const respuesta = await fetch(
    `${API_BASE_URL}/api/perfiles-publicadores/${encodeURIComponent(String(id))}/imagenes`,
    { cache: "no-store" }
  );

  if (!respuesta.ok) {
    throw new Error("No se pudieron obtener las imágenes del perfil");
  }

  const data: unknown = await respuesta.json();

  if (!Array.isArray(data)) {
    return [];
  }

  return data.filter(esImagenPerfilPublicador);
}

/*
  TODAS las fotos visibles del publicador (Fase 5): las del perfil y
  las de sus actividades, en un solo request. Antes esta misma grilla
  costaba una llamada por actividad.
*/
export async function obtenerFotosDelPublicador(
  id: number
): Promise<ImagenPerfilPublicador[]> {
  const respuesta = await fetch(
    `${API_BASE_URL}/api/perfiles-publicadores/${encodeURIComponent(String(id))}/fotos`,
    { cache: "no-store" }
  );

  if (!respuesta.ok) {
    throw new Error("No se pudieron obtener las fotos del publicador");
  }

  const data: unknown = await respuesta.json();

  if (!Array.isArray(data)) {
    return [];
  }

  return data.filter(esImagenPerfilPublicador);
}

/* Opiniones agregadas del publicador (Fase 5). */
export async function obtenerValoracionesDelPublicador(
  idOSlug: number | string
): Promise<ResumenValoraciones | null> {
  const respuesta = await fetch(
    `${API_BASE_URL}/api/perfiles-publicadores/${encodeURIComponent(String(idOSlug))}/valoraciones`,
    { cache: "no-store" }
  );

  if (!respuesta.ok) {
    return null;
  }

  return (await respuesta.json()) as ResumenValoraciones;
}

/* Preguntas YA RESPONDIDAS del publicador (Fase 5). */
export async function obtenerPreguntasDelPublicador(
  idOSlug: number | string
): Promise<PreguntaActividad[]> {
  const respuesta = await fetch(
    `${API_BASE_URL}/api/perfiles-publicadores/${encodeURIComponent(String(idOSlug))}/preguntas`,
    { cache: "no-store" }
  );

  if (!respuesta.ok) {
    return [];
  }

  const data: unknown = await respuesta.json();

  return Array.isArray(data) ? (data as PreguntaActividad[]) : [];
}

function esPerfilPublicadorPublico(
  valor: unknown
): valor is PerfilPublicadorPublico {
  if (typeof valor !== "object" || valor === null) {
    return false;
  }

  const objeto = valor as Record<string, unknown>;

  return typeof objeto.id === "number" && typeof objeto.nombre === "string";
}

function esImagenPerfilPublicador(
  valor: unknown
): valor is ImagenPerfilPublicador {
  if (typeof valor !== "object" || valor === null) {
    return false;
  }

  const objeto = valor as Record<string, unknown>;

  return typeof objeto.id === "number" && typeof objeto.url === "string";
}
