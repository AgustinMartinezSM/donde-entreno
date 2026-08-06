import type { MetadataRoute } from "next";
import { SITE_URL } from "../lib/siteConfig";

/*
  robots.txt generado por Next.js (App Router).
  Las áreas privadas no aportan nada a los buscadores.
*/
export default function robots(): MetadataRoute.Robots {
  return {
    rules: {
      userAgent: "*",
      allow: "/",
      disallow: ["/admin/", "/publicador/", "/mi-cuenta", "/favoritos"],
    },
    sitemap: `${SITE_URL}/sitemap.xml`,
  };
}
