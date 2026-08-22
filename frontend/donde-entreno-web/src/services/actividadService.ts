import { API_BASE_URL } from "../lib/apiConfig";
import type {
  ActividadDetalle,
  HorarioActividad,
  ImagenActividad,
  PaginaActividades,
  SocialProofActividad,
} from "../types/actividad";


// Parámetros que acepta el endpoint GET /api/actividades.
// Los dejamos opcionales porque el usuario puede buscar con uno, varios o ninguno.
export type BuscarActividadesParams = {
  texto?: string;
  deporteId?: number;
  deporteSlug?: string;
  ciudadId?: number;
  ciudadSlug?: string;
  barrioId?: number;
  perfilPublicadorId?: number;
  nivel?: string;
  modalidad?: string;
  page?: number;
  size?: number;
  orden?: string;
};

// Función para buscar actividades en el backend.
// Recibe filtros opcionales y devuelve una página de actividades.
export async function buscarActividades(
  params: BuscarActividadesParams = {}
): Promise<PaginaActividades> {
  // URLSearchParams nos ayuda a armar query params de forma prolija.
  // Ejemplo final: /api/actividades?texto=boxeo&page=0&size=10
  const queryParams = new URLSearchParams();

  // Agregamos cada parámetro solo si tiene valor.
  if (params.texto) queryParams.append("texto", params.texto);
  if (params.deporteId) queryParams.append("deporteId", String(params.deporteId));
  if (params.deporteSlug) queryParams.append("deporteSlug", params.deporteSlug);
  if (params.ciudadId) queryParams.append("ciudadId", String(params.ciudadId));
  if (params.ciudadSlug?.trim()) {
    queryParams.append("ciudadSlug", params.ciudadSlug.trim());
  }
  if (params.barrioId) queryParams.append("barrioId", String(params.barrioId));
  if (params.perfilPublicadorId) {
    queryParams.append("perfilPublicadorId", String(params.perfilPublicadorId));
  }
  if (params.nivel) queryParams.append("nivel", params.nivel);
  if (params.modalidad) queryParams.append("modalidad", params.modalidad);
  if (params.page !== undefined) queryParams.append("page", String(params.page));
  if (params.size !== undefined) queryParams.append("size", String(params.size));
  if (params.orden) queryParams.append("orden", params.orden);

  // Armamos la URL final.
  const url = `${API_BASE_URL}/api/actividades?${queryParams.toString()}`;

  // Hacemos la petición al backend.
  const respuesta = await fetch(url, {
    cache: "no-store",
  });

  // Si el backend responde con error, frenamos y mostramos un mensaje claro.
  if (!respuesta.ok) {
    throw new Error("No se pudieron obtener las actividades");
  }

  // Convertimos la respuesta JSON al tipo PaginaActividades.
  return respuesta.json();
}

// Función para obtener el detalle completo de una actividad por su slug.
// Ejemplo de URL final:
// http://localhost:8080/api/actividades/boxeo-recreativo-adultos-principiantes/detalle
// Tipo interno para representar cómo viene realmente la respuesta del backend.
// El backend devuelve un objeto con:
// - actividad: datos principales
// - horarios: listado de horarios
// - imagenes: listado de imágenes
type ActividadDetalleBackendResponse = {
  actividad: Omit<ActividadDetalle, "horarios" | "imagenes" | "socialProof">;
  horarios?: HorarioActividad[];
  imagenes?: ImagenActividad[];
  socialProof?: SocialProofActividad | null;
};

/*
  Error específico para distinguir "la actividad no existe" (404 real,
  la página debe responder notFound) de un backend caído (error genérico,
  la página muestra el estado de error con reintento).
*/
export class ActividadNoEncontradaError extends Error {
  constructor(slug: string) {
    super(`No existe una actividad con slug "${slug}"`);
    this.name = "ActividadNoEncontradaError";
  }
}

// Función para obtener el detalle completo de una actividad por su slug.
// Acá normalizamos la respuesta para que la página pueda usar todo junto.
export async function obtenerDetalleActividad(
  slug: string
): Promise<ActividadDetalle> {
  const url = `${API_BASE_URL}/api/actividades/${slug}/detalle`;

  const respuesta = await fetch(url, {
    cache: "no-store",
  });

  if (respuesta.status === 404) {
    throw new ActividadNoEncontradaError(slug);
  }

  if (!respuesta.ok) {
    throw new Error("No se pudo obtener el detalle de la actividad");
  }

  const data: ActividadDetalleBackendResponse = await respuesta.json();

  // Unificamos los datos principales con horarios e imágenes.
  return {
    ...data.actividad,
    horarios: data.horarios || [],
    imagenes: data.imagenes || [],
    socialProof: data.socialProof ?? null,
  };
}

/*
  Imágenes públicas de una actividad por slug. El backend ya filtra
  APROBADA + activa, así que acá solo validamos la forma.

  La usa el perfil del publicador para armar su grilla de fotos. Hoy no
  hay un endpoint que devuelva las imágenes de todas las actividades de
  un publicador de una vez, así que el perfil pide una por actividad;
  cuando exista el agregado, solo cambia el caller.
*/
export async function obtenerImagenesActividad(
  slug: string
): Promise<ImagenActividad[]> {
  const respuesta = await fetch(
    `${API_BASE_URL}/api/actividades/${encodeURIComponent(slug)}/imagenes`,
    { cache: "no-store" }
  );

  if (!respuesta.ok) {
    throw new Error("No se pudieron obtener las imágenes de la actividad");
  }

  const data: unknown = await respuesta.json();

  if (!Array.isArray(data)) {
    return [];
  }

  return data.filter(
    (item): item is ImagenActividad =>
      typeof item === "object" &&
      item !== null &&
      typeof (item as ImagenActividad).id === "number" &&
      typeof (item as ImagenActividad).url === "string"
  );
}
