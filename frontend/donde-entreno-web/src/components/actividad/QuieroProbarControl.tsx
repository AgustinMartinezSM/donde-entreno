"use client";

import { useEffect, useState } from "react";
import { usePathname, useRouter } from "next/navigation";

import { useAuthSession } from "../auth/AuthSessionProvider";
import {
  ConfianzaApiError,
  marcarInteres,
  obtenerInteres,
  quitarInteres,
  type EstadoInteres,
} from "../../services/confianzaService";
import { AppButton } from "../ui/AppButton";

/*
  El flujo propio de DondeEntreno (Fase 3): Quiero probar → Ya probé →
  valorá. Vive junto al contacto (acción PRE-visita, decisión del
  plan). Anónimo va al login.
*/
export function QuieroProbarControl({
  actividadId,
  className = "",
}: {
  actividadId: number;
  className?: string;
}) {
  const router = useRouter();
  const pathname = usePathname();
  const { status, accessToken } = useAuthSession();

  const [estado, setEstado] = useState<EstadoInteres>(null);
  const [procesando, setProcesando] = useState(false);

  useEffect(() => {
    let componenteActivo = true;

    if (status !== "authenticated" || !accessToken) {
      return () => {
        componenteActivo = false;
      };
    }

    obtenerInteres(accessToken, actividadId)
      .then((actual) => {
        if (componenteActivo) {
          setEstado(actual);
        }
      })
      .catch(() => {
        /* Sin estado el control arranca desde cero. */
      });

    return () => {
      componenteActivo = false;
    };
  }, [status, accessToken, actividadId]);

  async function ejecutar(accion: () => Promise<void>, estadoNuevo: EstadoInteres) {
    if (status !== "authenticated" || !accessToken) {
      router.push(
        `/login?motivo=cuenta&returnTo=${encodeURIComponent(pathname ?? "/")}`
      );
      return;
    }

    if (procesando) {
      return;
    }

    setProcesando(true);

    try {
      await accion();
      setEstado(estadoNuevo);
    } catch (error: unknown) {
      if (error instanceof ConfianzaApiError && error.status === 401) {
        router.push(
          `/login?motivo=cuenta&returnTo=${encodeURIComponent(pathname ?? "/")}`
        );
      }
      /* Otros errores no rompen el detalle. */
    } finally {
      setProcesando(false);
    }
  }

  if (estado === "YA_PROBE") {
    return (
      <div
        className={`rounded-[var(--radius-md)] border border-[var(--color-success-border)] bg-[var(--color-success-wash)] p-3 text-center ${className}`}
      >
        <p className="text-sm font-bold text-[var(--color-success)]">
          Ya probaste esta actividad ✓
        </p>
        <a
          href="#valoraciones"
          className="mt-1 block text-xs font-bold text-[var(--color-secondary)] underline-offset-2 hover:underline"
        >
          Contale a la comunidad cómo estuvo
        </a>
        <button
          type="button"
          disabled={procesando}
          onClick={() =>
            void ejecutar(() => quitarInteres(accessToken as string, actividadId), null)
          }
          className="mt-1 text-xs text-[var(--color-muted)] underline-offset-2 hover:underline"
        >
          Deshacer
        </button>
      </div>
    );
  }

  if (estado === "QUIERO_PROBAR") {
    return (
      <div
        className={`rounded-[var(--radius-md)] border border-[var(--color-border-accent)] bg-[var(--color-surface-soft)] p-3 text-center ${className}`}
      >
        <p className="text-sm font-bold text-[var(--color-primary)]">
          Querés probar esta actividad
        </p>
        <div className="mt-2 flex items-center justify-center gap-3">
          <AppButton
            type="button"
            size="sm"
            disabled={procesando}
            onClick={() =>
              void ejecutar(
                () => marcarInteres(accessToken as string, actividadId, "YA_PROBE"),
                "YA_PROBE"
              )
            }
          >
            ¡Ya la probé!
          </AppButton>
          <button
            type="button"
            disabled={procesando}
            onClick={() =>
              void ejecutar(() => quitarInteres(accessToken as string, actividadId), null)
            }
            className="text-xs text-[var(--color-muted)] underline-offset-2 hover:underline"
          >
            Ya no me interesa
          </button>
        </div>
      </div>
    );
  }

  return (
    <AppButton
      type="button"
      variant="outline"
      fullWidth
      disabled={procesando}
      className={className}
      onClick={() =>
        void ejecutar(
          () => marcarInteres(accessToken as string, actividadId, "QUIERO_PROBAR"),
          "QUIERO_PROBAR"
        )
      }
    >
      Quiero probar esta actividad
    </AppButton>
  );
}
