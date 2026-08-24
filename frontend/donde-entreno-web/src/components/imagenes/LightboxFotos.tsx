"use client";

import Image from "next/image";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useEffect, useRef, useState } from "react";

import {
  cargarLikesFotos,
  toggleLikeFoto,
  useLikesFotos,
} from "../../lib/likesFotos";
import { useAuthSession } from "../auth/AuthSessionProvider";
import { BotonReportar } from "../social/BotonReportar";

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
  /*
    Para el corazón (bloque 14): id real de la imagen y su contador
    público. Sin estos dos datos el corazón no se dibuja — un backend
    anterior al bloque simplemente no lo muestra.
  */
  imagenId?: number;
  cantidadLikes?: number | null;
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

  const router = useRouter();
  const pathname = usePathname();
  const { status, accessToken } = useAuthSession();
  const likesPropios = useLikesFotos();
  /*
    Ajustes locales del contador (bloque 14): el cantidadLikes del DTO ya
    incluye mi like previo, así que cada toggle de esta vista suma o
    resta 1 sobre esa base — exacto sin re-consultar.
  */
  const [ajustesLikes, setAjustesLikes] = useState<Map<number, number>>(
    new Map()
  );

  /* Los ids propios se cargan recién cuando el visor se abre con sesión. */
  useEffect(() => {
    if (abierto && status === "authenticated" && accessToken) {
      cargarLikesFotos(accessToken);
    }
  }, [abierto, status, accessToken]);

  function alternarLike(imagenId: number) {
    if (status !== "authenticated" || !accessToken) {
      router.push(`/login?returnTo=${encodeURIComponent(pathname)}`);
      return;
    }

    const quedoConLike = toggleLikeFoto(accessToken, imagenId);
    setAjustesLikes((actual) => {
      const nuevo = new Map(actual);
      nuevo.set(imagenId, (nuevo.get(imagenId) ?? 0) + (quedoConLike ? 1 : -1));
      return nuevo;
    });
  }

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

        {/* Pie: corazón, epígrafe del publicador y link contextual. */}
        {foto.imagenId !== undefined &&
        foto.cantidadLikes !== undefined &&
        foto.cantidadLikes !== null ? (
          <div className="relative z-10 flex items-center justify-center gap-3 px-4 pb-2">
            <BotonMeGustaFoto
              cantidad={
                foto.cantidadLikes + (ajustesLikes.get(foto.imagenId) ?? 0)
              }
              activo={likesPropios.has(foto.imagenId)}
              onClick={() => alternarLike(foto.imagenId as number)}
            />
            {/* Reportar (Fase 2 social): compacto y en tinta clara del visor. */}
            <span className="text-white/70 [&_button]:text-white/70 [&_button:hover]:text-white">
              <BotonReportar
                tipoObjeto="IMAGEN"
                objetoId={foto.imagenId}
                etiquetaObjeto="esta foto"
                compacto
              />
            </span>
          </div>
        ) : null}

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

/*
  Corazón del visor (bloque 14): contador público + like propio.
  Anónimo → login con returnTo (lo resuelve el caller del onClick).
*/
function BotonMeGustaFoto({
  cantidad,
  activo,
  onClick,
}: {
  cantidad: number;
  activo: boolean;
  onClick: () => void;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      aria-pressed={activo}
      aria-label={
        activo
          ? `Quitar tu me gusta (${cantidad} en total)`
          : `Dar me gusta a esta foto (${cantidad} en total)`
      }
      className={`flex min-h-10 items-center gap-2 rounded-full px-4 text-sm font-extrabold transition duration-200 ease-out active:scale-95 ${
        activo
          ? "bg-white/20 text-white"
          : "bg-white/10 text-white/85 hover:bg-white/20"
      }`}
    >
      <svg
        viewBox="0 0 24 24"
        fill={activo ? "currentColor" : "none"}
        stroke="currentColor"
        strokeWidth="2"
        strokeLinecap="round"
        strokeLinejoin="round"
        className={`h-5 w-5 ${activo ? "text-[#FF6B81]" : ""}`}
        aria-hidden="true"
      >
        <path d="M19 14c1.49-1.46 3-3.21 3-5.5A5.5 5.5 0 0 0 16.5 3c-1.76 0-3 .5-4.5 2-1.5-1.5-2.74-2-4.5-2A5.5 5.5 0 0 0 2 8.5c0 2.3 1.5 4.05 3 5.5l7 7Z" />
      </svg>
      {cantidad}
    </button>
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
