import { API_BASE_URL } from "./apiConfig";

/*
  Convierte la URL de una imagen que devuelve el backend en una URL
  usable por next/image:
  - Si ya es absoluta (http/https), se usa tal cual.
  - Si es relativa (por ejemplo /recursos/foto.png), se antepone API_BASE_URL.
  - Si viene vacia o nula, devuelve null para que el caller use su fallback.
*/
export function construirUrlImagenBackend(url?: string | null) {
  const urlLimpia = url?.trim();

  if (!urlLimpia) {
    return null;
  }

  if (urlLimpia.startsWith("http://") || urlLimpia.startsWith("https://")) {
    return urlLimpia;
  }

  const separador = urlLimpia.startsWith("/") ? "" : "/";

  return `${API_BASE_URL}${separador}${urlLimpia}`;
}
