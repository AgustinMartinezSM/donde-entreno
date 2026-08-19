"use client";

import { useEffect, useState } from "react";
import { usePathname, useRouter } from "next/navigation";

import { useAuthSession } from "../auth/AuthSessionProvider";
import {
  SeguimientoApiError,
  dejarDeSeguirPublicador,
  obtenerEstadoSeguimiento,
  seguirPublicador,
} from "../../services/seguimientoService";

type SeguirPublicadorButtonProps = {
  perfilPublicadorId: number;
  perfilPublicadorNombre?: string | null;
};

/*
  Botón "Seguir / Siguiendo" para un publicador (capa social, Bloque 8).

  Regla de producto: seguir es exclusivo de usuarios con cuenta. El
  anónimo va al login con aviso y vuelta (igual que favoritos). El estado
  real vive en el backend; acá se resuelve al montar y se actualiza de
  forma optimista, revirtiendo si la API falla.
*/
export function SeguirPublicadorButton({
  perfilPublicadorId,
  perfilPublicadorNombre,
}: SeguirPublicadorButtonProps) {
  const router = useRouter();
  const pathname = usePathname();
  const { status, accessToken } = useAuthSession();
  const [siguiendo, setSiguiendo] = useState<boolean | null>(null);
  const [procesando, setProcesando] = useState(false);

  useEffect(() => {
    let activo = true;

    if (status !== "authenticated" || !accessToken) {
      return () => {
        activo = false;
      };
    }

    obtenerEstadoSeguimiento(perfilPublicadorId, accessToken)
      .then((estado) => {
        if (activo) {
          setSiguiendo(estado.siguiendo);
        }
      })
      .catch(() => {
        if (activo) {
          setSiguiendo(null);
        }
      });

    return () => {
      activo = false;
    };
  }, [status, accessToken, perfilPublicadorId]);

  const irAlLogin = () => {
    router.push(
      `/login?motivo=cuenta&returnTo=${encodeURIComponent(pathname ?? "/")}`
    );
  };

  const manejarClick = async () => {
    if (status !== "authenticated" || !accessToken) {
      irAlLogin();
      return;
    }

    if (procesando) {
      return;
    }

    const queriaSeguir = siguiendo !== true;
    setProcesando(true);
    setSiguiendo(queriaSeguir); // optimista

    try {
      if (queriaSeguir) {
        await seguirPublicador(perfilPublicadorId, accessToken);
      } else {
        await dejarDeSeguirPublicador(perfilPublicadorId, accessToken);
      }
    } catch (error) {
      setSiguiendo(!queriaSeguir); // revertir
      if (error instanceof SeguimientoApiError && error.status === 401) {
        irAlLogin();
      }
    } finally {
      setProcesando(false);
    }
  };

  const activo = status === "authenticated" && siguiendo === true;
  const sufijoNombre = perfilPublicadorNombre ? ` a ${perfilPublicadorNombre}` : "";

  return (
    <button
      type="button"
      onClick={manejarClick}
      aria-pressed={activo}
      aria-label={activo ? `Dejar de seguir${sufijoNombre}` : `Seguir${sufijoNombre}`}
      disabled={procesando}
      className={`inline-flex min-h-11 items-center justify-center gap-2 rounded-[18px] px-5 py-3 text-sm font-extrabold shadow-sm transition duration-200 ease-out hover:-translate-y-0.5 active:scale-[0.98] disabled:opacity-70 ${
        activo
          ? "border border-[var(--color-success-border)] bg-[var(--color-success-wash)] text-[var(--color-success)] hover:border-[var(--color-secondary)]"
          : "border border-[var(--color-border-accent)] bg-white text-[var(--color-primary)] hover:border-[var(--color-primary)] hover:bg-[var(--color-surface-soft)]"
      }`}
    >
      {activo ? "Siguiendo" : "Seguir"}
    </button>
  );
}
