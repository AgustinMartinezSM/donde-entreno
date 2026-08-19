"use client";

import { useState } from "react";
import { usePathname, useRouter } from "next/navigation";

import { useAuthSession } from "../auth/AuthSessionProvider";
import {
  alternarFavorito,
  useEsFavorito,
  type DatosFavorito,
} from "../../lib/favoritos";
import { IconoGuardar } from "../ui/IconoGuardar";

type FavoritoButtonProps = {
  actividad: DatosFavorito;
  /*
    - "card": botón circular flotante para superponer sobre la imagen
      de la card (el contenedor padre debe ser relative).
    - "detalle": botón pill con texto para la página de detalle.
  */
  variante?: "card" | "detalle";
};

export function FavoritoButton({
  actividad,
  variante = "card",
}: FavoritoButtonProps) {
  const router = useRouter();
  const pathname = usePathname();
  const { status } = useAuthSession();
  const guardada = useEsFavorito(actividad.slug);
  const [animando, setAnimando] = useState(false);

  /*
    Variante card (solo ícono): nombre accesible FIJO + aria-pressed, que
    es el patrón WAI-ARIA correcto para un toggle. La variante detalle sí
    tiene texto visible que cambia, así que ahí usamos un nombre que
    contiene ese texto y NO usamos aria-pressed (evita la señal doble).
  */
  const etiquetaFija = `Guardar ${actividad.titulo} en favoritos`;
  const etiquetaDetalle = guardada
    ? `Guardada en favoritos: ${actividad.titulo}`
    : `Guardar en favoritos: ${actividad.titulo}`;

  const manejarClick = () => {
    /*
      Regla de producto: guardar favoritos es exclusivo de usuarios con
      cuenta. El visitante anónimo va al login con aviso y vuelta al
      lugar donde estaba.
    */
    if (status !== "authenticated") {
      router.push(
        `/login?motivo=cuenta&returnTo=${encodeURIComponent(pathname ?? "/")}`
      );
      return;
    }

    const quedoGuardada = alternarFavorito(actividad);

    if (quedoGuardada) {
      setAnimando(true);
    }
  };

  const animacion = animando ? "animate-[de-pop_0.35s_ease-out]" : "";

  if (variante === "detalle") {
    return (
      <button
        type="button"
        onClick={manejarClick}
        aria-label={etiquetaDetalle}
        /* Ver MeGustaButton: en mobile la barra de acciones va sin texto. */
        className={`inline-flex min-h-11 items-center justify-center gap-0 rounded-[18px] px-3 py-2.5 text-sm font-extrabold shadow-sm transition duration-200 ease-out hover:-translate-y-0.5 active:scale-[0.98] sm:gap-2 sm:px-4 ${
          guardada
            ? "border border-[var(--color-success-border)] bg-[var(--color-success-wash)] text-[var(--color-success)] hover:border-[var(--color-secondary)]"
            : "border border-[var(--color-border-accent)] bg-[var(--color-surface)] text-[var(--color-primary)] hover:border-[var(--color-primary)] hover:bg-[var(--color-surface-soft)]"
        }`}
      >
        <span
          onAnimationEnd={() => setAnimando(false)}
          className={`inline-flex ${animacion}`}
        >
          <IconoGuardar relleno={guardada} />
        </span>
        {/*
          Texto corto: en el detalle este botón vive en una barra de
          acciones junto a "Me gusta" y "Compartir", y "Guardar en
          favoritos" empujaba la fila a tres renglones. El aria-label de
          arriba sí mantiene la frase completa.
        */}
        <span className="hidden sm:inline">
          {guardada ? "Guardada" : "Guardar"}
        </span>
      </button>
    );
  }

  return (
    <button
      type="button"
      onClick={manejarClick}
      aria-pressed={guardada}
      aria-label={etiquetaFija}
      title={guardada ? "Quitar de favoritos" : "Guardar en favoritos"}
      className={`absolute right-3 top-3 z-10 inline-flex h-10 w-10 items-center justify-center rounded-full shadow-[0_6px_16px_rgba(15,61,94,0.22)] backdrop-blur transition duration-200 ease-out hover:scale-110 active:scale-95 ${
        guardada
          ? "bg-[var(--color-secondary)] text-white"
          : "bg-white/95 text-[var(--color-brand)] hover:bg-white"
      }`}
    >
      <span
        onAnimationEnd={() => setAnimando(false)}
        className={`inline-flex ${animacion}`}
      >
        <IconoGuardar relleno={guardada} />
      </span>
    </button>
  );
}
