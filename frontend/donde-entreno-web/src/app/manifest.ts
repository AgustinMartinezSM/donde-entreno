import type { MetadataRoute } from "next";

/*
  Manifest PWA (Fase 1 de la etapa social): con esto DondeEntreno se
  puede instalar como app en Android/desktop (Chrome ya no exige
  service worker para instalar; el SW llegará con el offline básico).
  Next lo sirve en /manifest.webmanifest y lo linkea solo.
*/
export default function manifest(): MetadataRoute.Manifest {
  return {
    name: "DondeEntreno",
    short_name: "DondeEntreno",
    description:
      "Encontrá dónde entrenar en tu ciudad: deportes, clubes, profesores y actividades reales.",
    start_url: "/",
    display: "standalone",
    background_color: "#F8FAFC",
    theme_color: "#0F3D5E",
    orientation: "portrait",
    icons: [
      {
        src: "/brand/icon-192.png",
        sizes: "192x192",
        type: "image/png",
        purpose: "any",
      },
      {
        src: "/brand/icon-512.png",
        sizes: "512x512",
        type: "image/png",
        purpose: "any",
      },
      {
        src: "/brand/icon-512.png",
        sizes: "512x512",
        type: "image/png",
        purpose: "maskable",
      },
    ],
  };
}
