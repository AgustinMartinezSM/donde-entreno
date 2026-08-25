"use client";

import { useState } from "react";
import { usePathname, useRouter } from "next/navigation";

import { useAuthSession } from "../auth/AuthSessionProvider";
import {
  NovedadesApiError,
  darMeGustaNovedad,
  quitarMeGustaNovedad,
} from "../../services/novedadesService";

/*
  Reacción a una novedad (script 37).

  Una sola reacción, no un set de emojis: un set multiplica tabla,
  contadores, UI y decisiones de producto sin resolver nada que un
  gesto simple no resuelva.

  Anónimo va al login con returnTo, igual que guardar una foto.
*/
export function BotonMeGustaNovedad({
  novedadId,
  meGustaInicial,
  cantidadInicial,
}: {
  novedadId: number;
  meGustaInicial: boolean;
  cantidadInicial: number;
}) {
  const router = useRouter();
  const pathname = usePathname();
  const { status, accessToken } = useAuthSession();

  const [meGusta, setMeGusta] = useState(meGustaInicial);
  const [cantidad, setCantidad] = useState(cantidadInicial);
  const [enviando, setEnviando] = useState(false);

  async function alternar() {
    if (status !== "authenticated" || !accessToken) {
      router.push(`/login?returnTo=${encodeURIComponent(pathname)}`);
      return;
    }

    if (enviando) {
      return;
    }

    setEnviando(true);

    /* Optimista; el backend devuelve el total real y manda. */
    const previoMeGusta = meGusta;
    const previaCantidad = cantidad;
    setMeGusta(!previoMeGusta);
    setCantidad(previaCantidad + (previoMeGusta ? -1 : 1));

    try {
      const respuesta = previoMeGusta
        ? await quitarMeGustaNovedad(accessToken, novedadId)
        : await darMeGustaNovedad(accessToken, novedadId);

      setMeGusta(respuesta.meGusta);
      setCantidad(respuesta.cantidadMeGusta);
    } catch (error: unknown) {
      setMeGusta(previoMeGusta);
      setCantidad(previaCantidad);

      /* Silencioso salvo que el backend diga algo concreto. */
      if (error instanceof NovedadesApiError && error.status === 404) {
        setCantidad(previaCantidad);
      }
    } finally {
      setEnviando(false);
    }
  }

  return (
    <button
      type="button"
      onClick={() => void alternar()}
      aria-pressed={meGusta}
      aria-label={meGusta ? "Quitar me gusta" : "Me gusta"}
      disabled={enviando}
      className={`inline-flex min-h-9 items-center gap-1.5 rounded-full px-3 text-sm font-bold transition duration-200 ease-out ${
        meGusta
          ? "bg-[var(--color-brand)] text-white"
          : "border border-[var(--color-border-accent)] bg-[var(--color-surface)] text-[var(--color-primary)] hover:border-[var(--color-primary)]"
      }`}
    >
      <svg
        aria-hidden="true"
        viewBox="0 0 24 24"
        className="h-4 w-4"
        fill={meGusta ? "currentColor" : "none"}
        stroke="currentColor"
        strokeWidth="2"
        strokeLinecap="round"
        strokeLinejoin="round"
      >
        <path d="M20.8 4.6a5.5 5.5 0 0 0-7.8 0L12 5.7l-1-1.1a5.5 5.5 0 0 0-7.8 7.8l1.1 1L12 21l7.7-7.6 1.1-1a5.5 5.5 0 0 0 0-7.8z" />
      </svg>

      {/* Con 0 no se muestra número: un contador en cero es ruido. */}
      {cantidad > 0 ? cantidad : "Me gusta"}
    </button>
  );
}
