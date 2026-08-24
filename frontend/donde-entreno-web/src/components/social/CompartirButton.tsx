"use client";

import { useEffect, useRef, useState } from "react";

import { registrarInteraccion } from "../../lib/interacciones";

type CompartirButtonProps = {
  /* Ruta relativa a compartir; se resuelve contra el origin actual. */
  ruta: string;
  titulo: string;
  /*
    Variante cuadrada de solo ícono, para las cards del feed: ahí el
    ancho lo necesita el CTA principal y el texto no aporta.
  */
  soloIcono?: boolean;
  /*
    Para la barra de acciones del detalle: en mobile queda solo el ícono
    (los tres botones con texto no entran en una fila a 375px) y el texto
    vuelve desde sm. El perfil del publicador lo deja siempre visible.
  */
  ocultarTextoEnMobile?: boolean;
  /*
    Tracking anónimo (Fase 2 social): con actividadId presente, el
    compartir se cuenta. Best-effort.
  */
  actividadId?: number;
};

/*
  Botón de compartir: usa el share nativo del dispositivo si existe
  (mobile) y cae a copiar el link al portapapeles en desktop.
*/
export function CompartirButton({
  ruta,
  titulo,
  soloIcono = false,
  ocultarTextoEnMobile = false,
  actividadId,
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

    if (actividadId !== undefined) {
      registrarInteraccion(actividadId, "CLICK_COMPARTIR");
    }

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
      /*
        Mismas medidas que "Me gusta" y "Guardar": los tres conviven en la
        barra de acciones del detalle y antes este quedaba más bajo.
      */
      className={`inline-flex min-h-11 shrink-0 items-center justify-center rounded-[18px] border border-[var(--color-border-accent)] bg-[var(--color-surface)] font-extrabold text-[var(--color-primary)] shadow-sm transition duration-200 ease-out hover:-translate-y-0.5 hover:border-[var(--color-primary)] hover:bg-[var(--color-surface-soft)] active:scale-[0.98] ${
        soloIcono
          ? "h-11 w-11 gap-0 px-0 text-xs"
          : ocultarTextoEnMobile
            ? "gap-0 px-3 py-2.5 text-sm sm:gap-2 sm:px-4"
            : "gap-2 px-4 py-2.5 text-sm"
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
      {soloIcono ? null : (
        <span className={ocultarTextoEnMobile ? "hidden sm:inline" : undefined}>
          {copiado ? "¡Link copiado!" : "Compartir"}
        </span>
      )}
    </button>
  );
}
