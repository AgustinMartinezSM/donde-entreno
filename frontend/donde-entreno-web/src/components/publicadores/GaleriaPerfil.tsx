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
  /* Fase 4 (galería social): comentarios y sección. */
  cantidadComentarios?: number | null;
  comentariosActivados?: boolean | null;
  seccion?: string | null;
};

const ETIQUETAS_SECCION: Record<string, string> = {
  INSTALACIONES: "Instalaciones",
  ENTRENAMIENTOS: "Entrenamientos",
  EVENTOS: "Eventos",
  EQUIPO: "Equipo",
};

/*
  Grilla de fotos del perfil público, con visor a pantalla completa
  (fase 4). Antes cada foto linkeaba a su actividad: en una solapa de
  FOTOS, tocar una foto tiene que mostrar LA FOTO — el camino a la
  actividad sigue existiendo, como acción dentro del visor.

  Chips de sección (fase 4 social): aparecen recién cuando el publicador
  usa dos o más secciones — con una sola (o ninguna) no filtran nada.
*/
export function GaleriaPerfil({ fotos }: { fotos: FotoDeGaleriaPerfil[] }) {
  const [indiceLightbox, setIndiceLightbox] = useState<number | null>(null);
  const [seccionActiva, setSeccionActiva] = useState<string | null>(null);

  const seccionesEnUso = Object.keys(ETIQUETAS_SECCION).filter((seccion) =>
    fotos.some((foto) => foto.seccion === seccion)
  );
  const hayGenerales = fotos.some((foto) => !foto.seccion);
  const mostrarChips = seccionesEnUso.length >= 2;

  const visibles =
    mostrarChips && seccionActiva !== null
      ? fotos.filter((foto) =>
          seccionActiva === "GENERAL"
            ? !foto.seccion
            : foto.seccion === seccionActiva
        )
      : fotos;

  function elegirSeccion(seccion: string | null) {
    setSeccionActiva(seccion);
    /* La lista cambia: un visor abierto sobre la lista vieja no vale. */
    setIndiceLightbox(null);
  }

  return (
    <>
      {mostrarChips ? (
        <div
          role="group"
          aria-label="Filtrar las fotos por sección"
          className="mt-5 flex flex-wrap gap-2"
        >
          <ChipSeccion
            etiqueta="Todas"
            activo={seccionActiva === null}
            onClick={() => elegirSeccion(null)}
          />
          {seccionesEnUso.map((seccion) => (
            <ChipSeccion
              key={seccion}
              etiqueta={ETIQUETAS_SECCION[seccion]}
              activo={seccionActiva === seccion}
              onClick={() => elegirSeccion(seccion)}
            />
          ))}
          {hayGenerales ? (
            <ChipSeccion
              etiqueta="General"
              activo={seccionActiva === "GENERAL"}
              onClick={() => elegirSeccion("GENERAL")}
            />
          ) : null}
        </div>
      ) : null}

      <ul className="mt-5 grid grid-cols-2 gap-2 sm:grid-cols-3 sm:gap-3 lg:grid-cols-4">
        {visibles.map((foto, posicion) => (
          <li key={foto.clave}>
            <button
              type="button"
              onClick={() => setIndiceLightbox(posicion)}
              aria-label={`Ver la foto ${posicion + 1} de ${visibles.length} en pantalla completa`}
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
        fotos={visibles.map((foto) => ({
          clave: foto.clave,
          url: foto.url,
          alt: foto.alt,
          href: foto.href,
          hrefTexto: "Ver la actividad",
          imagenId: foto.imagenId,
          cantidadLikes: foto.cantidadLikes,
          cantidadComentarios: foto.cantidadComentarios,
          comentariosActivados: foto.comentariosActivados,
        }))}
        indice={indiceLightbox}
        onCerrar={() => setIndiceLightbox(null)}
        onNavegar={setIndiceLightbox}
      />
    </>
  );
}

function ChipSeccion({
  etiqueta,
  activo,
  onClick,
}: {
  etiqueta: string;
  activo: boolean;
  onClick: () => void;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      aria-pressed={activo}
      className={`min-h-9 rounded-full border px-4 text-sm font-extrabold transition duration-200 ease-out active:scale-95 ${
        activo
          ? "border-transparent bg-[var(--color-primary)] text-[var(--color-surface)]"
          : "border-[var(--color-border-accent)] bg-[var(--color-surface)] text-[var(--color-primary)] hover:border-[var(--color-primary)]"
      }`}
    >
      {etiqueta}
    </button>
  );
}
