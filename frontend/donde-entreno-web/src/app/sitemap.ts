import type { MetadataRoute } from "next";
import { CATALOGO_DEPORTES_ASISTENTE } from "../lib/asistente/conocimiento";
import { DEFAULT_CITY_SLUG } from "../lib/ciudadActiva";
import { SITE_URL } from "../lib/siteConfig";

/*
  Sitemap estático con las páginas públicas principales, las landings
  por deporte y las combinaciones ciudad×deporte de la ciudad activa
  (el catálogo es espejo del seed real).

  Cuando el producto sume más ciudades, las combinaciones de esas
  ciudades se agregan acá (o se genera fetcheando ciudades en build).
  Las actividades individuales quedan para una etapa posterior.
*/
export default function sitemap(): MetadataRoute.Sitemap {
  const rutasPublicas = [
    { ruta: "", prioridad: 1 },
    { ruta: "/explorar", prioridad: 0.9 },
    { ruta: "/deportes", prioridad: 0.8 },
    { ruta: "/ciudades", prioridad: 0.8 },
    { ruta: `/ciudades/${DEFAULT_CITY_SLUG}`, prioridad: 0.7 },
    { ruta: "/publicar", prioridad: 0.7 },
  ];

  const rutasDeportes = CATALOGO_DEPORTES_ASISTENTE.map((deporte) => ({
    ruta: `/deportes/${deporte.slug}`,
    prioridad: 0.7,
  }));

  const rutasCiudadDeporte = CATALOGO_DEPORTES_ASISTENTE.map((deporte) => ({
    ruta: `/ciudades/${DEFAULT_CITY_SLUG}/${deporte.slug}`,
    prioridad: 0.6,
  }));

  return [...rutasPublicas, ...rutasDeportes, ...rutasCiudadDeporte].map(
    ({ ruta, prioridad }) => ({
      url: `${SITE_URL}${ruta}`,
      changeFrequency: "weekly" as const,
      priority: prioridad,
    })
  );
}
