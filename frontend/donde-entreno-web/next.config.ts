import type { NextConfig } from "next";

/*
  Sin hosts remotos habilitados para next/image: las imágenes que
  muestra el sitio son estáticas del propio proyecto.

  Cuando se incorpore un almacenamiento de imágenes externo (por
  ejemplo Supabase Storage), agregar acá su host en images.remotePatterns.
*/
const nextConfig: NextConfig = {};

export default nextConfig;
