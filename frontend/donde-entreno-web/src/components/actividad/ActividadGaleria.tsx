"use client";

import Image from "next/image";
import { useCallback, useRef, useState } from "react";

import { LightboxFotos } from "../imagenes/LightboxFotos";
import { ActivityImage } from "./ActivityImage";

export type FotoActividad = {
  id: number;
  /* URL ya validada como publicable (absoluta http/https). */
  url: string;
  alt: string;
  titulo?: string | null;
  /* Likes públicos (bloque 14); null/ausente = backend viejo, sin corazón. */
  cantidadLikes?: number | null;
};

type ActividadGaleriaProps = {
  fotos: FotoActividad[];
  /* Ilustración por deporte, para las actividades que todavía no subieron fotos. */
  fallbackSrc: string;
  fallbackAlt: string;
  fallbackText: string;
};

/*
  Medio del "post": la principal y la galería aprobada en un solo
  carrusel. Antes la principal era un hero y el resto una grilla chica al
  final de la página, después de la descripción y los horarios.

  Sin librerías: scroll-snap nativo, que ya da swipe en mobile, rueda
  horizontal en trackpad y scroll por teclado cuando la pista tiene foco.
  Las flechas y los puntos solo empujan ese scroll.
*/
const ALTO_MEDIO = "h-72 sm:h-[26rem]";

export function ActividadGaleria({
  fotos,
  fallbackSrc,
  fallbackAlt,
  fallbackText,
}: ActividadGaleriaProps) {
  const pistaRef = useRef<HTMLUListElement>(null);
  const [indice, setIndice] = useState(0);
  /* null = lightbox cerrado; un índice = abierto en esa foto (fase 4). */
  const [indiceLightbox, setIndiceLightbox] = useState<number | null>(null);

  /*
    El índice se deduce del scroll real, no al revés: así queda
    sincronizado tanto si el usuario arrastra como si toca una flecha.
  */
  const sincronizarIndice = useCallback(() => {
    const pista = pistaRef.current;

    if (!pista || pista.clientWidth === 0) {
      return;
    }

    const visible = Math.round(pista.scrollLeft / pista.clientWidth);

    setIndice((actual) => (actual === visible ? actual : visible));
  }, []);

  const desplazarA = useCallback(
    (destino: number) => {
      const pista = pistaRef.current;

      if (!pista) {
        return;
      }

      const acotado = Math.min(Math.max(destino, 0), fotos.length - 1);

      pista.scrollTo({ left: pista.clientWidth * acotado, behavior: "smooth" });
    },
    [fotos.length]
  );

  /* Sin fotos reales mostramos la ilustración del deporte, como las cards. */
  if (fotos.length === 0) {
    return (
      <ActivityImage
        src={fallbackSrc}
        alt={fallbackAlt}
        fallbackText={fallbackText}
        heightClassName={ALTO_MEDIO}
        sizes="(max-width: 1023px) 100vw, 800px"
      />
    );
  }

  const unaSola = fotos.length === 1;
  const fotoVisible = fotos[Math.min(indice, fotos.length - 1)];

  return (
    <figure className="group/galeria relative">
      <ul
        ref={pistaRef}
        onScroll={sincronizarIndice}
        tabIndex={unaSola ? -1 : 0}
        aria-label={
          unaSola ? undefined : `Fotos de la actividad: ${fotos.length} en total`
        }
        className={`flex overflow-x-auto rounded-[var(--radius-lg)] [scrollbar-width:none] focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-[#4FB3D9]/30 [&::-webkit-scrollbar]:hidden ${
          unaSola ? "" : "snap-x snap-mandatory"
        }`}
      >
        {fotos.map((foto, posicion) => (
          <li
            key={foto.id}
            className={`relative ${ALTO_MEDIO} min-w-full shrink-0 snap-center bg-[var(--color-bg)]`}
          >
            {/*
              La foto es un botón: la abre a pantalla completa. El swipe
              del carrusel no se pierde — un arrastre táctil no dispara
              el click, solo el tap.
            */}
            <button
              type="button"
              onClick={() => setIndiceLightbox(posicion)}
              aria-label={`Ver la foto ${posicion + 1} en pantalla completa`}
              aria-haspopup="dialog"
              className="absolute inset-0 h-full w-full cursor-zoom-in focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-inset focus-visible:ring-[#4FB3D9]/50"
            >
              <Image
                src={foto.url}
                alt={foto.alt}
                fill
                /* La primera es la LCP de la página de detalle. */
                priority={posicion === 0}
                sizes="(max-width: 1023px) 100vw, 800px"
                className="object-cover"
              />
            </button>
          </li>
        ))}
      </ul>

      {/*
        "Ver todas": la entrada explícita al visor. El tap sobre la foto
        también lo abre, pero un botón con el total dice que hay más.
      */}
      {fotos.length > 1 ? (
        /* Arriba a la izquierda, espejando el contador: los bordes de
           abajo se mueven con los puntos y el epígrafe. */
        <button
          type="button"
          onClick={() => setIndiceLightbox(indice)}
          aria-haspopup="dialog"
          className="absolute left-3 top-3 rounded-full bg-[#0F3D5E]/75 px-3.5 py-1.5 text-xs font-extrabold text-white backdrop-blur-sm transition duration-200 ease-out hover:bg-[#0F3D5E]/90 focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-[#4FB3D9]/50 active:scale-95"
        >
          Ver todas las fotos ({fotos.length})
        </button>
      ) : null}

      <LightboxFotos
        fotos={fotos.map((foto) => ({
          clave: String(foto.id),
          url: foto.url,
          alt: foto.alt,
          epigrafe: foto.titulo,
          imagenId: foto.id,
          cantidadLikes: foto.cantidadLikes,
        }))}
        indice={indiceLightbox}
        onCerrar={() => setIndiceLightbox(null)}
        onNavegar={setIndiceLightbox}
      />

      {unaSola ? null : (
        <>
          <span className="pointer-events-none absolute right-3 top-3 rounded-full bg-[#0F3D5E]/75 px-3 py-1 text-xs font-extrabold text-white backdrop-blur-sm">
            {Math.min(indice + 1, fotos.length)}/{fotos.length}
          </span>

          {/*
            Flechas solo desde sm: en mobile el gesto natural es el swipe y
            los botones taparían la foto.
          */}
          <FlechaGaleria
            direccion="anterior"
            oculta={indice === 0}
            onClick={() => desplazarA(indice - 1)}
          />
          <FlechaGaleria
            direccion="siguiente"
            oculta={indice >= fotos.length - 1}
            onClick={() => desplazarA(indice + 1)}
          />

          <div className="mt-3 flex items-center justify-center gap-2">
            {fotos.map((foto, posicion) => {
              const activa = posicion === indice;

              return (
                <button
                  key={foto.id}
                  type="button"
                  onClick={() => desplazarA(posicion)}
                  aria-label={`Ver la foto ${posicion + 1} de ${fotos.length}`}
                  aria-current={activa ? "true" : undefined}
                  className={`h-2.5 rounded-full transition-all duration-200 ease-out ${
                    activa
                      ? "w-6 bg-[var(--color-secondary)]"
                      : "w-2.5 bg-[#C7DCE8] hover:bg-[var(--color-brand)]"
                  }`}
                />
              );
            })}
          </div>
        </>
      )}

      {fotoVisible?.titulo ? (
        <figcaption className="mt-2 px-1 text-center text-xs font-semibold text-[var(--color-muted)]">
          {fotoVisible.titulo}
        </figcaption>
      ) : null}
    </figure>
  );
}

function FlechaGaleria({
  direccion,
  oculta,
  onClick,
}: {
  direccion: "anterior" | "siguiente";
  oculta: boolean;
  onClick: () => void;
}) {
  const esAnterior = direccion === "anterior";

  return (
    <button
      type="button"
      onClick={onClick}
      /*
        En el extremo la flecha se esconde pero no se desmonta: aparecer y
        desaparecer del DOM movía el foco del teclado al vacío.
      */
      tabIndex={oculta ? -1 : 0}
      aria-hidden={oculta}
      aria-label={esAnterior ? "Foto anterior" : "Foto siguiente"}
      className={`absolute top-1/2 hidden h-10 w-10 -translate-y-1/2 items-center justify-center rounded-full bg-white/90 text-[var(--color-brand)] shadow-[0_6px_18px_rgba(15,61,94,0.22)] backdrop-blur transition duration-200 ease-out hover:bg-white active:scale-95 sm:flex ${
        esAnterior ? "left-3" : "right-3"
      } ${
        oculta
          ? "pointer-events-none opacity-0"
          : "opacity-0 group-hover/galeria:opacity-100 focus-visible:opacity-100"
      }`}
    >
      <svg
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        strokeWidth="2.5"
        strokeLinecap="round"
        strokeLinejoin="round"
        className="h-5 w-5"
        aria-hidden="true"
      >
        <path d={esAnterior ? "m15 5-7 7 7 7" : "m9 5 7 7-7 7"} />
      </svg>
    </button>
  );
}
