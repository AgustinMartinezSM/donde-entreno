"use client";

import Image from "next/image";
import Link from "next/link";
import { useEffect, useRef } from "react";

export type FotoLightbox = {
  clave: string;
  /* URL absoluta ya validada como publicable. */
  url: string;
  alt: string;
  /* Epígrafe visible (el título que cargó el publicador), si existe. */
  epigrafe?: string | null;
  /* Link contextual ("Ver actividad") para galerías de perfil. */
  href?: string;
  hrefTexto?: string;
};

type LightboxFotosProps = {
  fotos: FotoLightbox[];
  /* null = cerrado; un índice = abierto en esa foto. */
  indice: number | null;
  onCerrar: () => void;
  onNavegar: (indice: number) => void;
};

/*
  Visor de fotos a pantalla completa (fase 4 del bloque visual).

  Mismo patrón que el resto de los modales de la app: <dialog> nativo
  con showModal() — foco contenido, Escape, top layer sobre cualquier
  flotante — sin librerías. Navegación con flechas (botones y teclado)
  y swipe horizontal en pantallas táctiles.

  El visor es oscuro en los dos temas a propósito: es la convención de
  todo visor de medios (la foto manda, la interfaz desaparece), así que
  acá no hay tokens de tema.
*/
export function LightboxFotos({
  fotos,
  indice,
  onCerrar,
  onNavegar,
}: LightboxFotosProps) {
  const dialogoRef = useRef<HTMLDialogElement | null>(null);
  const inicioSwipeRef = useRef<number | null>(null);
  const abierto = indice !== null && fotos.length > 0;

  useEffect(() => {
    const dialogo = dialogoRef.current;

    if (!dialogo) {
      return;
    }

    if (abierto && !dialogo.open) {
      dialogo.showModal();
    } else if (!abierto && dialogo.open) {
      dialogo.close();
    }
  }, [abierto]);

  /* El fondo queda inerte pero aún scrollea: se congela mientras está abierto. */
  useEffect(() => {
    if (!abierto) {
      return;
    }

    const overflowPrevio = document.body.style.overflow;
    document.body.style.overflow = "hidden";

    return () => {
      document.body.style.overflow = overflowPrevio;
    };
  }, [abierto]);

  if (fotos.length === 0) {
    return null;
  }

  const posicion = Math.min(indice ?? 0, fotos.length - 1);
  const foto = fotos[posicion];
  const hayAnterior = posicion > 0;
  const haySiguiente = posicion < fotos.length - 1;

  function navegar(delta: number) {
    const destino = posicion + delta;

    if (destino >= 0 && destino < fotos.length) {
      onNavegar(destino);
    }
  }

  return (
    <dialog
      ref={dialogoRef}
      onClose={onCerrar}
      /* Click fuera de la foto y sus controles = cerrar. */
      onClick={(evento) => {
        if (evento.target === dialogoRef.current) {
          onCerrar();
        }
      }}
      onKeyDown={(evento) => {
        if (evento.key === "ArrowLeft") {
          evento.preventDefault();
          navegar(-1);
        }

        if (evento.key === "ArrowRight") {
          evento.preventDefault();
          navegar(1);
        }
      }}
      onTouchStart={(evento) => {
        inicioSwipeRef.current = evento.touches[0]?.clientX ?? null;
      }}
      onTouchEnd={(evento) => {
        const inicio = inicioSwipeRef.current;
        inicioSwipeRef.current = null;

        if (inicio === null) {
          return;
        }

        const delta = (evento.changedTouches[0]?.clientX ?? inicio) - inicio;

        /* Umbral de 48px: un tap no navega, un arrastre sí. */
        if (Math.abs(delta) > 48) {
          navegar(delta < 0 ? 1 : -1);
        }
      }}
      aria-label={`Foto ${posicion + 1} de ${fotos.length} en pantalla completa`}
      className="fixed inset-0 m-0 h-[100dvh] max-h-none w-screen max-w-none bg-[#050D15]/95 p-0 backdrop:bg-[#050D15]/80"
    >
      <div className="relative flex h-full w-full flex-col">
        {/* Barra superior: contador y cierre, siempre visibles. */}
        <div className="relative z-10 flex items-center justify-between px-4 pt-4">
          <span className="rounded-full bg-white/10 px-3 py-1 text-xs font-extrabold text-white">
            {posicion + 1} / {fotos.length}
          </span>

          <button
            type="button"
            onClick={onCerrar}
            aria-label="Cerrar la vista de fotos"
            className="flex h-11 w-11 items-center justify-center rounded-full bg-white/10 text-white transition duration-200 ease-out hover:bg-white/20 focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-[#4FB3D9]/50 active:scale-95"
          >
            <svg
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth="2"
              strokeLinecap="round"
              strokeLinejoin="round"
              className="h-5 w-5"
              aria-hidden="true"
            >
              <path d="M18 6 6 18" />
              <path d="m6 6 12 12" />
            </svg>
          </button>
        </div>

        {/* La foto: ocupa todo el medio, entera y sin recortar. */}
        <div className="relative min-h-0 flex-1 px-2 py-3 sm:px-14">
          <Image
            key={foto.clave}
            src={foto.url}
            alt={foto.alt}
            fill
            sizes="100vw"
            className="object-contain"
          />
        </div>

        <FlechaLightbox
          direccion="anterior"
          oculta={!hayAnterior}
          onClick={() => navegar(-1)}
        />
        <FlechaLightbox
          direccion="siguiente"
          oculta={!haySiguiente}
          onClick={() => navegar(1)}
        />

        {/* Pie: epígrafe del publicador y link contextual, si existen. */}
        {foto.epigrafe || foto.href ? (
          <div className="relative z-10 flex flex-wrap items-center justify-center gap-x-4 gap-y-2 px-4 pb-5 text-center">
            {foto.epigrafe ? (
              <p className="max-w-2xl text-sm font-semibold leading-6 text-white/85">
                {foto.epigrafe}
              </p>
            ) : null}

            {foto.href ? (
              <Link
                href={foto.href}
                className="rounded-full bg-white/10 px-4 py-1.5 text-xs font-extrabold text-white underline-offset-4 transition hover:bg-white/20 hover:underline focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-[#4FB3D9]/50"
              >
                {foto.hrefTexto ?? "Ver actividad"}
              </Link>
            ) : null}
          </div>
        ) : (
          <div className="pb-5" aria-hidden="true" />
        )}
      </div>
    </dialog>
  );
}

function FlechaLightbox({
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
      tabIndex={oculta ? -1 : 0}
      aria-hidden={oculta}
      aria-label={esAnterior ? "Foto anterior" : "Foto siguiente"}
      className={`absolute top-1/2 z-10 flex h-12 w-12 -translate-y-1/2 items-center justify-center rounded-full bg-white/10 text-white transition duration-200 ease-out hover:bg-white/20 focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-[#4FB3D9]/50 active:scale-95 ${
        esAnterior ? "left-3" : "right-3"
      } ${oculta ? "pointer-events-none opacity-0" : ""}`}
    >
      <svg
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        strokeWidth="2.5"
        strokeLinecap="round"
        strokeLinejoin="round"
        className="h-6 w-6"
        aria-hidden="true"
      >
        <path d={esAnterior ? "m15 5-7 7 7 7" : "m9 5 7 7-7 7"} />
      </svg>
    </button>
  );
}
