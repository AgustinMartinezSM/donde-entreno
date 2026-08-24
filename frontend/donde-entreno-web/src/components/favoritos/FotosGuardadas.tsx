"use client";

import Image from "next/image";
import { useEffect, useState } from "react";

import { construirUrlImagenBackend } from "../../lib/backendUrl";
import { obtenerFotosGuardadas } from "../../services/galeriaSocialService";
import type { ImagenActividad } from "../../types/actividad";
import { useAuthSession } from "../auth/AuthSessionProvider";
import { LightboxFotos } from "../imagenes/LightboxFotos";
import { SectionHeader } from "../ui/SectionHeader";

/*
  Fotos guardadas del usuario (fase 4 social), como bloque de /favoritos
  debajo de las actividades guardadas. Grilla que abre el mismo visor de
  la galería: ahí se puede quitar el guardado, comentar y dar me gusta.

  El backend ya omite las fotos despublicadas sin borrar el guardado
  (snapshot vivo): acá solo se pinta lo que llega.
*/
export function FotosGuardadas() {
  const { status, accessToken } = useAuthSession();
  const [fotos, setFotos] = useState<ImagenActividad[] | null>(null);
  const [errorCarga, setErrorCarga] = useState(false);
  const [indiceLightbox, setIndiceLightbox] = useState<number | null>(null);

  useEffect(() => {
    if (status !== "authenticated" || !accessToken) {
      return;
    }

    let vigente = true;

    obtenerFotosGuardadas(accessToken)
      .then((lista) => {
        if (vigente) {
          setFotos(lista);
        }
      })
      .catch(() => {
        if (vigente) {
          setErrorCarga(true);
        }
      });

    return () => {
      vigente = false;
    };
  }, [status, accessToken]);

  /* Sin nada guardado (o backend viejo), el bloque no aparece. */
  if (errorCarga || fotos === null || fotos.length === 0) {
    return null;
  }

  const visibles = fotos.flatMap((imagen) => {
    const url = construirUrlImagenBackend(imagen.url);

    if (!url) {
      return [];
    }

    return [
      {
        clave: `guardada-${imagen.id}`,
        url,
        alt:
          imagen.descripcion?.trim() ||
          imagen.titulo?.trim() ||
          "Foto guardada",
        epigrafe: imagen.titulo?.trim() || null,
        href: imagen.actividadSlug
          ? `/actividades/${imagen.actividadSlug}`
          : undefined,
        imagenId: imagen.id,
        cantidadLikes: imagen.cantidadLikes ?? null,
        cantidadComentarios: imagen.cantidadComentarios ?? null,
        comentariosActivados: imagen.comentariosActivados ?? null,
      },
    ];
  });

  if (visibles.length === 0) {
    return null;
  }

  return (
    <section className="mt-10" aria-labelledby="fotos-guardadas-titulo">
      <SectionHeader
        eyebrow="Fotos"
        title="Tus fotos guardadas"
        description="Las fotos que marcaste con el bookmark en las galerías."
        titleId="fotos-guardadas-titulo"
      />

      <ul className="mt-5 grid grid-cols-2 gap-2 sm:grid-cols-3 sm:gap-3 lg:grid-cols-4">
        {visibles.map((foto, posicion) => (
          <li key={foto.clave}>
            <button
              type="button"
              onClick={() => setIndiceLightbox(posicion)}
              aria-label={`Ver la foto guardada ${posicion + 1} de ${visibles.length} en pantalla completa`}
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
        fotos={visibles}
        indice={indiceLightbox}
        onCerrar={() => setIndiceLightbox(null)}
        onNavegar={setIndiceLightbox}
      />
    </section>
  );
}
