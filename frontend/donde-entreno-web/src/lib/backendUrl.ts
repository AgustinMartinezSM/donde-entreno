/*
  Normaliza la URL de una imagen que devuelve el backend para usarla
  con next/image:
  - Si es absoluta (http/https), se usa tal cual: desde el bloque de
    imágenes con moderación las imágenes viven en Supabase Storage y se
    guardan con su URL absoluta.
  - Si es relativa (por ejemplo /uploads/actividades/foto.jpg), devuelve
    null: son rutas legado de cuando los archivos se guardaban en disco.
    El backend no expone ningún recurso estático y el contenedor es
    efímero, así que esas rutas no resuelven en ningún entorno; anteponer
    el host de la API solo genera un pedido fallido y una imagen rota en
    vez del fallback por deporte.
  - Si viene vacía o nula, devuelve null para que el caller use su fallback.
*/
export function construirUrlImagenBackend(url?: string | null) {
  const urlLimpia = url?.trim();

  if (!urlLimpia) {
    return null;
  }

  if (urlLimpia.startsWith("http://") || urlLimpia.startsWith("https://")) {
    return urlLimpia;
  }

  return null;
}
