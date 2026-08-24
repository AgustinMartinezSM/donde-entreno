"use client";

import { useEffect, useState } from "react";
import { usePathname, useRouter } from "next/navigation";

import { useAuthSession } from "../auth/AuthSessionProvider";
import {
  CheckinApiError,
  obtenerEstadoCheckinHoy,
  quitarCheckinDeHoy,
  registrarCheckin,
} from "../../services/checkinService";

type CheckinButtonProps = {
  actividadId: number;
  titulo: string;
};

function IconoPesa() {
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
      <path d="M6.5 6.5v11M17.5 6.5v11M3 9v6M21 9v6M6.5 12h11" />
    </svg>
  );
}

function IconoTilde() {
  return (
    <svg
      viewBox="0 0 24 24"
      aria-hidden="true"
      className="h-5 w-5"
      fill="none"
      stroke="currentColor"
      strokeWidth={2.5}
      strokeLinecap="round"
      strokeLinejoin="round"
    >
      <path d="M4.5 12.5l5 5 10-11" />
    </svg>
  );
}

/*
  Check-in "Entrené acá" (script 26): registra el entrenamiento de hoy
  sobre la actividad (una vez por día; el backend valida contra la
  base). Es un TOGGLE — hallazgo del smoke: la elección tiene que
  poder revertirse, así que el segundo click deshace el registro de
  hoy. Anónimo va al login con aviso y returnTo, como las demás
  acciones con cuenta. El contador público NO se muestra acá: vive en
  la fila de social proof del detalle, que es server-rendered.
*/
export function CheckinButton({ actividadId, titulo }: CheckinButtonProps) {
  const router = useRouter();
  const pathname = usePathname();
  const { status, accessToken } = useAuthSession();

  const [yaEntrenoHoy, setYaEntrenoHoy] = useState(false);
  const [enviando, setEnviando] = useState(false);
  const [animando, setAnimando] = useState(false);

  /* Al cargar logueado, el estado real del día pinta el botón. */
  useEffect(() => {
    let componenteActivo = true;

    if (status !== "authenticated" || !accessToken) {
      return () => {
        componenteActivo = false;
      };
    }

    obtenerEstadoCheckinHoy(actividadId, accessToken)
      .then((estado) => {
        if (componenteActivo) {
          setYaEntrenoHoy(estado.yaRegistradoHoy);
        }
      })
      .catch(() => {
        /* Sin estado no se rompe nada: el botón arranca sin tilde. */
      });

    return () => {
      componenteActivo = false;
    };
  }, [status, accessToken, actividadId]);

  const manejarClick = async () => {
    if (status !== "authenticated" || !accessToken) {
      router.push(
        `/login?motivo=cuenta&returnTo=${encodeURIComponent(pathname ?? "/")}`
      );
      return;
    }

    if (enviando) {
      return;
    }

    setEnviando(true);

    try {
      const respuesta = yaEntrenoHoy
        ? await quitarCheckinDeHoy(actividadId, accessToken)
        : await registrarCheckin(actividadId, accessToken);
      setYaEntrenoHoy(respuesta.yaRegistradoHoy);
      if (respuesta.registradoAhora) {
        setAnimando(true);
      }
    } catch (error: unknown) {
      if (error instanceof CheckinApiError && error.status === 401) {
        router.push(
          `/login?motivo=cuenta&returnTo=${encodeURIComponent(pathname ?? "/")}`
        );
      }
      /* Otros errores no rompen el detalle: el botón queda como estaba. */
    } finally {
      setEnviando(false);
    }
  };

  return (
    <button
      type="button"
      onClick={manejarClick}
      disabled={enviando}
      aria-label={
        yaEntrenoHoy
          ? `Quitar tu registro de hoy en ${titulo}`
          : `Registrar que hoy entrenaste en ${titulo}`
      }
      title={yaEntrenoHoy ? "Tocá para deshacer el registro de hoy" : undefined}
      /* Mismo lenguaje que MeGustaButton: solo ícono en mobile. */
      className={`inline-flex min-h-11 items-center justify-center gap-0 rounded-[18px] px-3 py-2.5 text-sm font-extrabold shadow-sm transition duration-200 ease-out hover:-translate-y-0.5 active:scale-[0.98] disabled:cursor-not-allowed disabled:opacity-70 sm:gap-2 sm:px-4 ${
        yaEntrenoHoy
          ? "border border-[var(--color-success-border)] bg-[var(--color-success-wash)] text-[var(--color-success)] hover:border-[var(--color-secondary)]"
          : "border border-[var(--color-border-accent)] bg-[var(--color-surface)] text-[var(--color-primary)] hover:border-[var(--color-primary)] hover:bg-[var(--color-surface-soft)]"
      }`}
    >
      <span
        onAnimationEnd={() => setAnimando(false)}
        className={`inline-flex ${animando ? "animate-[de-pop_0.35s_ease-out]" : ""} ${
          yaEntrenoHoy ? "text-[var(--color-secondary)]" : ""
        }`}
      >
        {yaEntrenoHoy ? <IconoTilde /> : <IconoPesa />}
      </span>
      {/*
        Copy del estado activo (hallazgo del smoke: "Entrenaste hoy" no
        se entendía): dice qué quedó hecho — el registro — y el title +
        aria-label avisan que el mismo botón lo deshace.
      */}
      <span className="hidden sm:inline">
        {yaEntrenoHoy ? "Registrado hoy" : "Entrené acá"}
      </span>
    </button>
  );
}
