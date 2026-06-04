import { API_BASE_URL } from "../lib/apiConfig";
import type { FiltrosOpciones } from "../types/filtros";

// Obtiene todas las opciones disponibles para filtros.
// Ejemplo:
// GET http://localhost:8080/api/filtros/opciones
export async function obtenerOpcionesFiltros(): Promise<FiltrosOpciones> {
  const url = `${API_BASE_URL}/api/filtros/opciones`;

  const respuesta = await fetch(url, {
    cache: "no-store",
  });

  if (!respuesta.ok) {
    throw new Error("No se pudieron obtener las opciones de filtros");
  }

  return respuesta.json();
}