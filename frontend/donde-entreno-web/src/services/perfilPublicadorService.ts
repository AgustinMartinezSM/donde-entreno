import { API_BASE_URL } from "../lib/apiConfig";
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
  Perfil individual. El backend todavía no expone GET /{id} (deuda
  documentada): resolvemos contra el listado público, que hoy es corto.
  Cuando exista el endpoint de detalle, solo cambia esta función.
*/
export async function obtenerPerfilPublicadorPorId(
  id: number
): Promise<PerfilPublicadorPublico | null> {
  const perfiles = await obtenerPerfilesPublicadores();

  return perfiles.find((perfil) => perfil.id === id) ?? null;
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
