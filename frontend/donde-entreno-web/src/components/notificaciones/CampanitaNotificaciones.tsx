"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { useRouter } from "next/navigation";

import { useAuthSession } from "../auth/AuthSessionProvider";
import {
  marcarNotificacionLeida,
  marcarTodasLeidas,
  obtenerContadorNoLeidas,
  obtenerNotificaciones,
  type Notificacion,
} from "../../services/notificacionesService";
import { formatearFechaRelativa } from "../../lib/formatoFecha";
import { AppButton } from "../ui/AppButton";
import { StatusMessage } from "../ui/StatusMessage";

/* Polling suave: cada 60s + al recuperar el foco. Nunca websockets. */
const INTERVALO_POLLING_MS = 60_000;

/*
  Campanita de notificaciones (script 28, Fase 2 social). Solo se
  dibuja con sesión; el panel es un <dialog> nativo (patrón del sheet
  de cuenta: top layer, foco contenido, Escape cierra).
*/
export function CampanitaNotificaciones() {
  const router = useRouter();
  const { status, accessToken } = useAuthSession();

  const [noLeidas, setNoLeidas] = useState(0);
  const [abierto, setAbierto] = useState(false);
  const [notificaciones, setNotificaciones] = useState<Notificacion[] | null>(null);
  const [errorCarga, setErrorCarga] = useState(false);
  const dialogoRef = useRef<HTMLDialogElement | null>(null);

  const actualizarContador = useCallback(() => {
    if (status !== "authenticated" || !accessToken) {
      return;
    }

    obtenerContadorNoLeidas(accessToken)
      .then(setNoLeidas)
      .catch(() => {
        /* Sin contador no se rompe nada. */
      });
  }, [status, accessToken]);

  useEffect(() => {
    if (status !== "authenticated") {
      return;
    }

    actualizarContador();
    const timer = window.setInterval(actualizarContador, INTERVALO_POLLING_MS);
    window.addEventListener("focus", actualizarContador);

    return () => {
      window.clearInterval(timer);
      window.removeEventListener("focus", actualizarContador);
    };
  }, [status, actualizarContador]);

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

  if (status !== "authenticated") {
    return null;
  }

  function abrirPanel() {
    setAbierto(true);
    setErrorCarga(false);

    if (accessToken) {
      obtenerNotificaciones(accessToken)
        .then((pagina) => setNotificaciones(pagina.contenido))
        .catch(() => setErrorCarga(true));
    }
  }

  async function manejarClickNotificacion(notificacion: Notificacion) {
    if (accessToken && !notificacion.leida) {
      try {
        await marcarNotificacionLeida(accessToken, notificacion.id);
        setNoLeidas((valor) => Math.max(0, valor - 1));
      } catch {
        /* Marcar leída es best-effort. */
      }
    }

    setAbierto(false);

    if (notificacion.ruta) {
      router.push(notificacion.ruta);
    }
  }

  async function manejarTodasLeidas() {
    if (!accessToken) {
      return;
    }

    try {
      await marcarTodasLeidas(accessToken);
      setNoLeidas(0);
      setNotificaciones((actuales) =>
        actuales?.map((notificacion) => ({ ...notificacion, leida: true })) ?? null
      );
    } catch {
      /* Best-effort. */
    }
  }

  return (
    <>
      <button
        type="button"
        onClick={abrirPanel}
        aria-label={
          noLeidas > 0
            ? `Notificaciones: ${noLeidas} sin leer`
            : "Notificaciones"
        }
        className="relative flex h-11 w-11 items-center justify-center rounded-full border border-[var(--color-border-accent)] bg-[var(--color-surface)] text-[var(--color-primary)] transition duration-200 ease-out hover:-translate-y-0.5 hover:border-[var(--color-primary)]"
      >
        <IconoCampana />
        {noLeidas > 0 ? (
          <span
            aria-hidden="true"
            className="absolute -right-0.5 -top-0.5 flex h-5 min-w-5 items-center justify-center rounded-full bg-[var(--color-secondary)] px-1 text-[11px] font-extrabold text-white"
          >
            {noLeidas > 99 ? "99+" : noLeidas}
          </span>
        ) : null}
      </button>

      <dialog
        ref={dialogoRef}
        onClose={() => setAbierto(false)}
        aria-labelledby="notificaciones-titulo"
        className="w-[min(26rem,calc(100vw-2rem))] rounded-[var(--radius-xl)] border border-[var(--color-border-soft)] bg-[var(--color-surface)] p-0 text-[var(--color-text)] shadow-[0_24px_60px_rgba(12,52,80,0.28)] backdrop:bg-[#0F3D5E]/40 backdrop:backdrop-blur-sm"
      >
        <div className="flex items-center justify-between gap-3 border-b border-[var(--color-divisor)] px-5 py-4">
          <h2
            id="notificaciones-titulo"
            className="text-lg font-extrabold text-[var(--color-primary)]"
          >
            Notificaciones
          </h2>
          <div className="flex items-center gap-2">
            {noLeidas > 0 ? (
              <AppButton
                type="button"
                variant="secondary"
                size="sm"
                onClick={manejarTodasLeidas}
              >
                Marcar leídas
              </AppButton>
            ) : null}
            <button
              type="button"
              onClick={() => setAbierto(false)}
              aria-label="Cerrar notificaciones"
              className="flex h-9 w-9 items-center justify-center rounded-full text-[var(--color-muted)] transition duration-200 ease-out hover:bg-[var(--color-bg)] hover:text-[var(--color-primary)]"
            >
              <IconoCerrar />
            </button>
          </div>
        </div>

        <div className="max-h-[60vh] overflow-y-auto px-2 py-2">
          {errorCarga ? (
            <div className="p-3">
              <StatusMessage variant="error" role="alert">
                No pudimos cargar tus notificaciones. Probá nuevamente.
              </StatusMessage>
            </div>
          ) : notificaciones === null ? (
            <p className="p-4 text-sm text-[var(--color-muted)]">Cargando...</p>
          ) : notificaciones.length === 0 ? (
            <p className="p-4 text-sm text-[var(--color-muted)]">
              Todavía no tenés notificaciones. Cuando pase algo con tus
              actividades o con quienes seguís, te avisamos acá.
            </p>
          ) : (
            <ul className="divide-y divide-[var(--color-divisor)]">
              {notificaciones.map((notificacion) => (
                <li key={notificacion.id}>
                  <button
                    type="button"
                    onClick={() => void manejarClickNotificacion(notificacion)}
                    className="flex w-full items-start gap-3 rounded-[12px] px-3 py-3 text-left transition duration-200 ease-out hover:bg-[var(--color-surface-soft)]"
                  >
                    <span
                      aria-hidden="true"
                      className={`mt-1.5 h-2.5 w-2.5 shrink-0 rounded-full ${
                        notificacion.leida
                          ? "bg-[var(--color-border-soft)]"
                          : "bg-[var(--color-secondary)]"
                      }`}
                    />
                    <span className="min-w-0 flex-1">
                      <span
                        className={`block text-sm leading-6 ${
                          notificacion.leida
                            ? "text-[var(--color-muted)]"
                            : "font-bold text-[var(--color-primary)]"
                        }`}
                      >
                        {notificacion.titulo}
                      </span>
                      {notificacion.createdAt ? (
                        <span className="mt-0.5 block text-xs text-[var(--color-muted)]">
                          {formatearFechaRelativa(notificacion.createdAt)}
                        </span>
                      ) : null}
                    </span>
                  </button>
                </li>
              ))}
            </ul>
          )}
        </div>
      </dialog>
    </>
  );
}

function IconoCampana() {
  return (
    <svg
      viewBox="0 0 24 24"
      aria-hidden="true"
      className="h-5 w-5"
      fill="none"
      stroke="currentColor"
      strokeWidth={2}
      strokeLinecap="round"
      strokeLinejoin="round"
    >
      <path d="M18 8a6 6 0 0 0-12 0c0 7-3 9-3 9h18s-3-2-3-9" />
      <path d="M13.7 21a2 2 0 0 1-3.4 0" />
    </svg>
  );
}

function IconoCerrar() {
  return (
    <svg
      viewBox="0 0 24 24"
      aria-hidden="true"
      className="h-5 w-5"
      fill="none"
      stroke="currentColor"
      strokeWidth={2}
      strokeLinecap="round"
      strokeLinejoin="round"
    >
      <path d="M18 6 6 18" />
      <path d="m6 6 12 12" />
    </svg>
  );
}
