/*
  URL pública del sitio (frontend).

  Se usa para metadata absoluta (OpenGraph, canónicas), robots.txt y sitemap.
  En local no hace falta configurarla: cae en http://localhost:3000.
  En producción se define NEXT_PUBLIC_SITE_URL con la URL real del frontend.
*/
export const SITE_URL =
  process.env.NEXT_PUBLIC_SITE_URL ?? "http://localhost:3000";
