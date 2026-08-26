"use client";

import { useState } from "react";
import { usePathname, useRouter } from "next/navigation";

import { useAuthSession } from "../auth/AuthSessionProvider";
import {
  EventosApiError,
  marcarInteres,
  quitarInteres,
} from "../../services/eventosService";

/*
  "Me interesa" (Fase 9): la prueba social barata de un evento. NO es
  una reserva —el cupo se muestra pero no se guarda— y el contacto
  sigue siendo WhatsApp, que es lo que ya funciona y ya se mide.

  Anónimo va al login con returnTo, igual que guardar una foto.
*/
export function BotonMeInteresa({
  eventoId,
  interesaInicial,
  cantidadInicial,
}: {
  eventoId: number;
  interesaInicial: boolean;
  cantidadInicial: number;
}) {
  const router = useRouter();
  const pathname = usePathname();
  const { status, accessToken } = useAuthSession();

  const [interesa, setInteresa] = useState(interesaInicial);
  const [cantidad, setCantidad] = useState(cantidadInicial);
  const [enviando, setEnviando] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function alternar() {
    if (status !== "authenticated" || !accessToken) {
      router.push(`/login?returnTo=${encodeURIComponent(pathname)}`);
      return;
    }

    if (enviando) {
      return;
    }

    setEnviando(true);
    setError(null);

    /*
      Optimista: el contador se mueve al toque y el backend manda la
      última palabra (devuelve el total real, así que no hay que
      pedir el evento de nuevo).
    */
    const previoInteresa = interesa;
    const previoCantidad = cantidad;
    setInteresa(!previoInteresa);
    setCantidad(previoCantidad + (previoInteresa ? -1 : 1));

    try {
      const respuesta = previoInteresa
        ? await quitarInteres(accessToken, eventoId)
        : await marcarInteres(accessToken, eventoId);

      setInteresa(respuesta.meInteresa);
      setCantidad(respuesta.cantidadInteresados);
    } catch (errorAlternar: unknown) {
      setInteresa(previoInteresa);
      setCantidad(previoCantidad);
      setError(
        errorAlternar instanceof EventosApiError
          ? errorAlternar.message
          : "No pudimos guardar tu interés. Probá de nuevo."
      );
    } finally {
      setEnviando(false);
    }
  }

  return (
    <div>
      <button
        type="button"
        onClick={() => void alternar()}
        aria-pressed={interesa}
        disabled={enviando}
        className={`inline-flex min-h-11 items-center gap-2 rounded-[14px] px-4 text-sm font-extrabold transition duration-200 ease-out ${
          interesa
            ? "bg-[var(--color-brand)] text-white"
            : "border border-[var(--color-border-accent)] bg-[var(--color-surface)] text-[var(--color-primary)] hover:border-[var(--color-primary)]"
        }`}
      >
        <svg
          aria-hidden="true"
          viewBox="0 0 24 24"
          className="h-4 w-4"
          fill="none"
          stroke="currentColor"
          strokeWidth="2"
          strokeLinecap="round"
          strokeLinejoin="round"
        >
          <path d="M9 11l3 3L22 4" />
          <path d="M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11" />
        </svg>
        {interesa ? "Me interesa" : "Me interesa"}
        {cantidad > 0 ? (
          <span className={interesa ? "opacity-90" : "text-[var(--color-muted)]"}>
            · {cantidad}
          </span>
        ) : null}
      </button>

      {error ? (
        <p role="alert" className="mt-2 text-xs text-[var(--color-danger)]">
          {error}
        </p>
      ) : null}
    </div>
  );
}
