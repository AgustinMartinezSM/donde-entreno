import { API_BASE_URL } from "../lib/apiConfig";
import type { PerfilPublicadorPublico } from "../types/publicadorPublico";

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

function esPerfilPublicadorPublico(
  valor: unknown
): valor is PerfilPublicadorPublico {
  if (typeof valor !== "object" || valor === null) {
    return false;
  }

  const objeto = valor as Record<string, unknown>;

  return typeof objeto.id === "number" && typeof objeto.nombre === "string";
}
