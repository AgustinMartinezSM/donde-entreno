"use client";

import { useEffect, useRef, useState } from "react";
import { usePathname, useRouter } from "next/navigation";
import type { FormEvent } from "react";

import { useAuthSession } from "../auth/AuthSessionProvider";
import { AppButton } from "../ui/AppButton";
import { StatusMessage } from "../ui/StatusMessage";
import {
  InboxApiError,
  MAX_TEXTO_MENSAJE,
  consultar,
} from "../../services/inboxService";

/*
  "Consultar sin dar tu teléfono" (inbox).

  Va AL LADO del botón de WhatsApp, no en su lugar: WhatsApp sigue
  siendo el CTA principal porque es lo único que hoy convierte. Este
  botón existe para quien no quiere entregar su número para preguntar
  cuánto sale una clase.
*/
export function BotonConsultar({
  perfilPublicadorId,
  actividadId,
  nombrePublicador,
  className = "",
}: {
  perfilPublicadorId: number;
  actividadId?: number | null;
  nombrePublicador?: string | null;
  className?: string;
}) {
  const router = useRouter();
  const pathname = usePathname();
  const { status, accessToken } = useAuthSession();

  const [abierto, setAbierto] = useState(false);
  const [texto, setTexto] = useState("");
  const [enviando, setEnviando] = useState(false);
  const [enviado, setEnviado] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const dialogoRef = useRef<HTMLDialogElement | null>(null);

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

  function manejarClick() {
    if (status !== "authenticated") {
      router.push(`/login?returnTo=${encodeURIComponent(pathname)}`);
      return;
    }

    setAbierto(true);
  }

  async function manejarEnvio(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault();

    if (!accessToken || enviando || !texto.trim()) {
      return;
    }

    setEnviando(true);
    setError(null);

    try {
      await consultar(accessToken, {
        perfilPublicadorId,
        actividadId,
        texto,
      });

      setEnviado(true);
      setTexto("");
    } catch (errorEnviar: unknown) {
      setError(
        errorEnviar instanceof InboxApiError
          ? errorEnviar.message
          : "No pudimos enviar tu consulta. Probá de nuevo."
      );
    } finally {
      setEnviando(false);
    }
  }

  return (
    <>
      <button
        type="button"
        onClick={manejarClick}
        className={`inline-flex min-h-11 items-center gap-2 rounded-[14px] border border-[var(--color-border-accent)] bg-[var(--color-surface)] px-4 text-sm font-bold text-[var(--color-primary)] transition duration-200 ease-out hover:border-[var(--color-primary)] ${className}`}
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
          <path d="M21 15a2 2 0 0 1-2 2H8l-4 3V5a2 2 0 0 1 2-2h13a2 2 0 0 1 2 2z" />
        </svg>
        Consultar sin dar tu teléfono
      </button>

      <dialog
        ref={dialogoRef}
        onClose={() => setAbierto(false)}
        className="w-[min(30rem,92vw)] rounded-[20px] border border-[var(--color-border)] bg-[var(--color-surface)] p-0 text-[var(--color-text)] backdrop:bg-black/40"
      >
        <form className="p-5" onSubmit={(evento) => void manejarEnvio(evento)}>
          <h2 className="text-base font-extrabold text-[var(--color-primary)]">
            Consultar {nombrePublicador ? `a ${nombrePublicador}` : ""}
          </h2>
          <p className="mt-1 text-sm text-[var(--color-muted)]">
            Te responden acá mismo. Vas a ver la respuesta en Mis consultas y te
            avisamos con la campanita.
          </p>

          {enviado ? (
            <>
              <StatusMessage variant="success" role="status" className="mt-4">
                Consulta enviada. Cuando te respondan, te avisamos.
              </StatusMessage>

              <div className="mt-4 flex flex-wrap gap-2">
                <AppButton
                  type="button"
                  variant="secondary"
                  onClick={() => {
                    setAbierto(false);
                    setEnviado(false);
                  }}
                >
                  Cerrar
                </AppButton>
                <AppButton
                  type="button"
                  onClick={() => router.push("/mi-cuenta/consultas")}
                >
                  Ver mis consultas
                </AppButton>
              </div>
            </>
          ) : (
            <>
              <label htmlFor="texto-consulta" className="sr-only">
                Tu consulta
              </label>
              <textarea
                id="texto-consulta"
                value={texto}
                onChange={(evento) => setTexto(evento.target.value)}
                rows={4}
                maxLength={MAX_TEXTO_MENSAJE}
                placeholder="Hola, ¿hay clases los sábados? ¿Cuánto sale?"
                className="mt-4 w-full rounded-[16px] border border-[var(--color-border-accent)] bg-[var(--color-surface)] p-3 text-sm leading-6 outline-none transition focus:border-[var(--color-primary)]"
              />

              {error ? (
                <StatusMessage variant="error" role="alert" className="mt-3">
                  {error}
                </StatusMessage>
              ) : null}

              <div className="mt-4 flex flex-wrap justify-end gap-2">
                <AppButton
                  type="button"
                  variant="secondary"
                  onClick={() => setAbierto(false)}
                >
                  Cancelar
                </AppButton>
                <AppButton type="submit" disabled={enviando || !texto.trim()}>
                  {enviando ? "Enviando..." : "Enviar consulta"}
                </AppButton>
              </div>
            </>
          )}
        </form>
      </dialog>
    </>
  );
}
