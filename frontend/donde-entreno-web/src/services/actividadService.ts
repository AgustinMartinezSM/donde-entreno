import { API_BASE_URL } from "../lib/apiConfig";
import type {
  ActividadDetalle,
  HorarioActividad,
  ImagenActividad,
  PaginaActividades,
} from "../types/actividad";


// Parámetros que acepta el endpoint GET /api/actividades.
// Los dejamos opcionales porque el usuario puede buscar con uno, varios o ninguno.
export type BuscarActividadesParams = {
  texto?: string;
  deporteId?: number;
  deporteSlug?: string;
  ciudadId?: number;
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
  actividad: Omit<ActividadDetalle, "horarios" | "imagenes">;
  horarios?: HorarioActividad[];
  imagenes?: ImagenActividad[];
};

// Función para obtener el detalle completo de una actividad por su slug.
// Acá normalizamos la respuesta para que la página pueda usar todo junto.
export async function obtenerDetalleActividad(
  slug: string
): Promise<ActividadDetalle> {
  const url = `${API_BASE_URL}/api/actividades/${slug}/detalle`;

  const respuesta = await fetch(url, {
    cache: "no-store",
  });

  if (!respuesta.ok) {
    throw new Error("No se pudo obtener el detalle de la actividad");
  }

  const data: ActividadDetalleBackendResponse = await respuesta.json();

  // Unificamos los datos principales con horarios e imágenes.
  return {
    ...data.actividad,
    horarios: data.horarios || [],
    imagenes: data.imagenes || [],
  };
}