import type { NextConfig } from "next";

/*
  Hosts remotos habilitados para next/image:
  - Supabase Storage (imágenes de actividades y perfiles, bloque 4):
    cubre las URLs públicas del bucket aprobado y las firmadas del
    bucket privado (previews de moderación), que viven en el mismo host
    *.supabase.co del proyecto.
*/
const nextConfig: NextConfig = {
  images: {
    remotePatterns: [
      {
        protocol: "https",
        hostname: "**.supabase.co",
      },
    ],
  },
};

export default nextConfig;
