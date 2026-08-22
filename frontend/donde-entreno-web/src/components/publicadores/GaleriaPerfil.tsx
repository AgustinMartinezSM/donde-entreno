"use client";

import Image from "next/image";
import { useState } from "react";

import { LightboxFotos } from "../imagenes/LightboxFotos";

export type FotoDeGaleriaPerfil = {
  clave: string;
  url: string;
  alt: string;
  /* Link a la actividad de la que salió la foto, si se conoce. */
  href?: string;
  /* Para el corazón del visor (bloque 14). */
  imagenId?: number;
  cantidadLikes?: number | null;
};

/*
  Grilla de fotos del perfil público, con visor a pantalla completa
  (fase 4). Antes cada foto linkeaba a su actividad: en una solapa de
  FOTOS, tocar una foto tiene que mostrar LA FOTO — el camino a la
  actividad sigue existiendo, como acción dentro del visor.
*/
export function GaleriaPerfil({ fotos }: { fotos: FotoDeGaleriaPerfil[] }) {
  const [indiceLightbox, setIndiceLightbox] = useState<number | null>(null);

  return (
    <>
      <ul className="mt-5 grid grid-cols-2 gap-2 sm:grid-cols-3 sm:gap-3 lg:grid-cols-4">
        {fotos.map((foto, posicion) => (
          <li key={foto.clave}>
            <button
              type="button"
              onClick={() => setIndiceLightbox(posicion)}
              aria-label={`Ver la foto ${posicion + 1} de ${fotos.length} en pantalla completa`}
              aria-haspopup="dialog"
              className="group relative block aspect-square w-full cursor-zoom-in overflow-hidden rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-bg)] focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-[#4FB3D9]/40"
            >
              <Image
                src={foto.url}
                alt={foto.alt}
                fill
                sizes="(max-width: 640px) 50vw, (max-width: 1024px) 33vw, 260px"
                className="object-cover transition duration-200 ease-out group-hover:scale-105"
              />
            </button>
          </li>
        ))}
      </ul>

      <LightboxFotos
        fotos={fotos.map((foto) => ({
          clave: foto.clave,
          url: foto.url,
          alt: foto.alt,
          href: foto.href,
          hrefTexto: "Ver la actividad",
          imagenId: foto.imagenId,
          cantidadLikes: foto.cantidadLikes,
        }))}
        indice={indiceLightbox}
        onCerrar={() => setIndiceLightbox(null)}
        onNavegar={setIndiceLightbox}
      />
    </>
  );
}
