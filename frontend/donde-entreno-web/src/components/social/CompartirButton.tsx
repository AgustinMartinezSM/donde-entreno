"use client";

import { useEffect, useRef, useState } from "react";

type CompartirButtonProps = {
  /* Ruta relativa a compartir; se resuelve contra el origin actual. */
  ruta: string;
  titulo: string;
  /*
    Variante cuadrada de solo ícono, para las cards del feed: ahí el
    ancho lo necesita el CTA principal y el texto no aporta.
  */
  soloIcono?: boolean;
};

/*
  Botón de compartir: usa el share nativo del dispositivo si existe
  (mobile) y cae a copiar el link al portapapeles en desktop.
*/
export function CompartirButton({
  ruta,
  titulo,
  soloIcono = false,
}: CompartirButtonProps) {
  const [copiado, setCopiado] = useState(false);
  const temporizador = useRef<ReturnType<typeof setTimeout> | null>(null);

  useEffect(() => {
    return () => {
      if (temporizador.current) {
        clearTimeout(temporizador.current);
      }
    };
  }, []);

  async function compartir() {
    const url = `${window.location.origin}${ruta}`;

    if (navigator.share) {
      try {
        await navigator.share({ title: titulo, url });
        return;
      } catch {
        /* Share cancelado o no disponible: probamos con el portapapeles. */
      }
    }

    try {
      await navigator.clipboard.writeText(url);
      setCopiado(true);
      temporizador.current = setTimeout(() => setCopiado(false), 2000);
    } catch {
      /* Sin portapapeles disponible no hay fallback razonable. */
    }
  }

  return (
    <button
      type="button"
      onClick={compartir}
      aria-label={`Compartir ${titulo}`}
      title={soloIcono ? (copiado ? "¡Link copiado!" : "Compartir") : undefined}
      className={`inline-flex min-h-10 shrink-0 items-center justify-center gap-2 rounded-[18px] border border-[#BFDDEA] bg-white text-xs font-extrabold text-[var(--color-primary)] shadow-sm transition duration-200 ease-out hover:-translate-y-0.5 hover:border-[var(--color-primary)] hover:bg-[#F8FCFE] active:scale-[0.98] ${
        soloIcono ? "h-11 w-11 px-0" : "px-4 py-2"
      }`}
    >
      <svg
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        strokeWidth="2"
        strokeLinecap="round"
        strokeLinejoin="round"
        className="h-4 w-4"
        aria-hidden="true"
      >
        <circle cx="18" cy="5" r="3" />
        <circle cx="6" cy="12" r="3" />
        <circle cx="18" cy="19" r="3" />
        <path d="m8.6 13.5 6.8 4M15.4 6.5l-6.8 4" />
      </svg>
      {soloIcono ? null : copiado ? "¡Link copiado!" : "Compartir"}
    </button>
  );
}
