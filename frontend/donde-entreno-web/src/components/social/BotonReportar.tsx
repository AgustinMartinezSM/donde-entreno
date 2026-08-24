"use client";

import { useEffect, useRef, useState } from "react";
import { usePathname, useRouter } from "next/navigation";
import type { FormEvent } from "react";

import { useAuthSession } from "../auth/AuthSessionProvider";
import {
  MOTIVOS_REPORTE,
  ReportesApiError,
  enviarReporte,
  type TipoObjetoReporte,
} from "../../services/reportesService";
import { AppButton } from "../ui/AppButton";
import { StatusMessage } from "../ui/StatusMessage";

/*
  Botón "Reportar" (script 28, Fase 2 social): la válvula de la
  moderación flexible. Anónimo va al login; el envío es idempotente
  (repetir no duplica). Diálogo nativo con motivos del catálogo.
*/
export function BotonReportar({
  tipoObjeto,
  objetoId,
  etiquetaObjeto,
  compacto = false,
}: {
  tipoObjeto: TipoObjetoReporte;
  objetoId: number;
  /** Cómo llamar al objeto en los textos ("esta foto", "este perfil"). */
  etiquetaObjeto: string;
  /** Solo ícono + texto chico (para barras con poco lugar). */
  compacto?: boolean;
}) {
  const router = useRouter();
  const pathname = usePathname();
  const { status, accessToken } = useAuthSession();

  const [abierto, setAbierto] = useState(false);
  const [motivo, setMotivo] = useState<string>(MOTIVOS_REPORTE[0].valor);
  const [detalle, setDetalle] = useState("");
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
      router.push(
        `/login?motivo=cuenta&returnTo=${encodeURIComponent(pathname ?? "/")}`
      );
      return;
    }

    setEnviado(false);
    setError(null);
    setDetalle("");
    setMotivo(MOTIVOS_REPORTE[0].valor);
    setAbierto(true);
  }

  async function manejarEnvio(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault();

    if (enviando || !accessToken) {
      return;
    }

    setEnviando(true);
    setError(null);

    try {
      await enviarReporte(accessToken, tipoObjeto, objetoId, motivo, detalle);
      setEnviado(true);
    } catch (excepcion: unknown) {
      if (excepcion instanceof ReportesApiError && excepcion.status === 401) {
        router.push(
          `/login?motivo=cuenta&returnTo=${encodeURIComponent(pathname ?? "/")}`
        );
        return;
      }

      setError("No pudimos enviar el reporte. Probá nuevamente.");
    } finally {
      setEnviando(false);
    }
  }

  return (
    <>
      <button
        type="button"
        onClick={manejarClick}
        aria-label={`Reportar ${etiquetaObjeto}`}
        className={
          compacto
            ? "inline-flex min-h-9 items-center gap-1.5 rounded-full px-2.5 text-xs font-bold text-[var(--color-muted)] transition duration-200 ease-out hover:text-[var(--color-primary)]"
            : "inline-flex min-h-11 items-center gap-2 rounded-[18px] border border-[var(--color-border-accent)] bg-[var(--color-surface)] px-4 py-2.5 text-sm font-bold text-[var(--color-muted)] transition duration-200 ease-out hover:-translate-y-0.5 hover:border-[var(--color-primary)] hover:text-[var(--color-primary)]"
        }
      >
        <IconoBandera />
        Reportar
      </button>

      <dialog
        ref={dialogoRef}
        onClose={() => setAbierto(false)}
        aria-labelledby="reportar-titulo"
        className="w-[min(26rem,calc(100vw-2rem))] rounded-[var(--radius-xl)] border border-[var(--color-border-soft)] bg-[var(--color-surface)] p-0 text-[var(--color-text)] shadow-[0_24px_60px_rgba(12,52,80,0.28)] backdrop:bg-[#0F3D5E]/40 backdrop:backdrop-blur-sm"
      >
        <div className="border-b border-[var(--color-divisor)] px-5 py-4">
          <h2
            id="reportar-titulo"
            className="text-lg font-extrabold text-[var(--color-primary)]"
          >
            Reportar {etiquetaObjeto}
          </h2>
        </div>

        <div className="px-5 py-5">
          {enviado ? (
            <>
              <StatusMessage variant="success" role="status">
                Recibimos tu reporte. El equipo lo va a revisar — gracias por
                cuidar la comunidad.
              </StatusMessage>
              <AppButton
                variant="secondary"
                fullWidth
                className="mt-4"
                onClick={() => setAbierto(false)}
              >
                Cerrar
              </AppButton>
            </>
          ) : (
            <form onSubmit={manejarEnvio} className="space-y-4">
              <fieldset>
                <legend className="text-sm font-bold text-[var(--color-primary)]">
                  ¿Cuál es el problema?
                </legend>
                <div className="mt-2 space-y-1.5">
                  {MOTIVOS_REPORTE.map((opcion) => (
                    <label
                      key={opcion.valor}
                      className="flex min-h-10 cursor-pointer items-center gap-3 rounded-[12px] px-2 text-sm text-[var(--color-text)] transition duration-200 ease-out hover:bg-[var(--color-surface-soft)]"
                    >
                      <input
                        type="radio"
                        name="motivo-reporte"
                        value={opcion.valor}
                        checked={motivo === opcion.valor}
                        onChange={() => setMotivo(opcion.valor)}
                        className="h-4 w-4 accent-[var(--color-secondary)]"
                      />
                      {opcion.etiqueta}
                    </label>
                  ))}
                </div>
              </fieldset>

              <label className="block">
                <span className="text-sm font-bold text-[var(--color-primary)]">
                  Contanos más (opcional)
                </span>
                <textarea
                  value={detalle}
                  onChange={(evento) => setDetalle(evento.target.value)}
                  maxLength={280}
                  rows={3}
                  disabled={enviando}
                  className="mt-2 w-full rounded-[14px] border border-[var(--color-border-accent)] bg-[var(--color-bg)] px-3 py-2 text-sm text-[var(--color-text)] outline-none transition duration-200 ease-out focus:border-[var(--color-accent)] focus:ring-4 focus:ring-[var(--color-border-soft)]"
                />
              </label>

              {error ? (
                <StatusMessage variant="error" role="alert">
                  {error}
                </StatusMessage>
              ) : null}

              <div className="grid gap-2 sm:grid-cols-2">
                <AppButton type="submit" disabled={enviando} fullWidth>
                  {enviando ? "Enviando..." : "Enviar reporte"}
                </AppButton>
                <AppButton
                  type="button"
                  variant="secondary"
                  fullWidth
                  onClick={() => setAbierto(false)}
                  disabled={enviando}
                >
                  Cancelar
                </AppButton>
              </div>
            </form>
          )}
        </div>
      </dialog>
    </>
  );
}

function IconoBandera() {
  return (
    <svg
      viewBox="0 0 24 24"
      aria-hidden="true"
      className="h-4 w-4"
      fill="none"
      stroke="currentColor"
      strokeWidth={2}
      strokeLinecap="round"
      strokeLinejoin="round"
    >
      <path d="M4 21V4a1 1 0 0 1 1-1h9.5l-1 4h6.5l-1.5 6H12l1-4H6" />
    </svg>
  );
}
