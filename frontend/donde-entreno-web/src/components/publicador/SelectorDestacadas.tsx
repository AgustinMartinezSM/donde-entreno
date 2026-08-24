"use client";

import { useEffect, useState } from "react";

import { useAuthSession } from "../auth/AuthSessionProvider";
import { AppButton } from "../ui/AppButton";
import { StatusMessage } from "../ui/StatusMessage";
import { SurfaceCard } from "../ui/SurfaceCard";
import {
  PublicadorApiError,
  definirDestacadasPublicador,
  listarDestacadasPublicador,
} from "../../services/publicadorService";
import type { ActividadPublicadorResumen } from "../../types/publicador";

const MAX_DESTACADAS = 3;

/*
  Elegir hasta 3 actividades destacadas (Fase 5): son las que van
  primero en el perfil público. El orden de selección es el orden en
  que se muestran, por eso cada elegida lleva su número.

  El backend valida el tope y la pertenencia; acá el tope solo evita
  que la persona seleccione de más antes de guardar.
*/
export function SelectorDestacadas({
  actividades,
}: {
  actividades: ActividadPublicadorResumen[];
}) {
  const { accessToken } = useAuthSession();
  const [elegidas, setElegidas] = useState<number[]>([]);
  const [cargando, setCargando] = useState(true);
  const [guardando, setGuardando] = useState(false);
  const [mensaje, setMensaje] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!accessToken) {
      return;
    }

    let vigente = true;

    listarDestacadasPublicador(accessToken)
      .then((lista) => {
        if (vigente) {
          setElegidas(lista.map((actividad) => actividad.id));
        }
      })
      .catch(() => {
        /* Backend viejo o sin red: se arranca sin destacadas. */
      })
      .finally(() => {
        if (vigente) {
          setCargando(false);
        }
      });

    return () => {
      vigente = false;
    };
  }, [accessToken]);

  function alternar(actividadId: number) {
    setMensaje(null);
    setError(null);

    setElegidas((actuales) => {
      if (actuales.includes(actividadId)) {
        return actuales.filter((id) => id !== actividadId);
      }

      if (actuales.length >= MAX_DESTACADAS) {
        setError(
          `Podés destacar hasta ${MAX_DESTACADAS}. Sacá una para elegir otra.`
        );
        return actuales;
      }

      return [...actuales, actividadId];
    });
  }

  async function guardar() {
    if (!accessToken || guardando) {
      return;
    }

    setGuardando(true);
    setMensaje(null);
    setError(null);

    try {
      await definirDestacadasPublicador(elegidas, accessToken);
      setMensaje(
        elegidas.length === 0
          ? "Listo: tu perfil ya no muestra destacadas."
          : "Listo: eso es lo primero que se ve en tu perfil."
      );
    } catch (errorGuardar: unknown) {
      setError(
        errorGuardar instanceof PublicadorApiError
          ? errorGuardar.message
          : "No pudimos guardar las destacadas. Probá nuevamente."
      );
    } finally {
      setGuardando(false);
    }
  }

  if (actividades.length === 0) {
    return null;
  }

  return (
    <SurfaceCard variant="info" className="mt-6 p-5 sm:p-6">
      <h2 className="text-lg font-extrabold text-[var(--color-primary)]">
        Destacadas en tu perfil
      </h2>
      <p className="mt-1.5 text-sm leading-6 text-[var(--color-muted)]">
        Elegí hasta {MAX_DESTACADAS}: son las primeras que ve alguien que
        entra a tu perfil. El orden en que las elegís es el orden en que se
        muestran.
      </p>

      {cargando ? (
        <StatusMessage variant="info" role="status" className="mt-4">
          Cargando tu selección...
        </StatusMessage>
      ) : (
        <>
          <ul className="mt-4 grid gap-2">
            {actividades.map((actividad) => {
              const posicion = elegidas.indexOf(actividad.id);
              const elegida = posicion >= 0;

              return (
                <li key={actividad.id}>
                  <button
                    type="button"
                    onClick={() => alternar(actividad.id)}
                    aria-pressed={elegida}
                    disabled={guardando}
                    className={`flex w-full items-center gap-3 rounded-[14px] border px-4 py-3 text-left transition duration-200 ease-out disabled:opacity-60 ${
                      elegida
                        ? "border-[var(--color-secondary)] bg-[var(--color-surface)]"
                        : "border-[var(--color-border-soft)] bg-[var(--color-bg)] hover:border-[var(--color-border-accent)]"
                    }`}
                  >
                    <span
                      className={`flex h-7 w-7 shrink-0 items-center justify-center rounded-full text-xs font-extrabold ${
                        elegida
                          ? "bg-[var(--color-secondary)] text-white"
                          : "bg-[var(--color-surface-soft)] text-[var(--color-muted)]"
                      }`}
                      aria-hidden="true"
                    >
                      {elegida ? posicion + 1 : "+"}
                    </span>
                    <span className="min-w-0 flex-1">
                      <span className="block truncate text-sm font-bold text-[var(--color-primary)]">
                        {actividad.titulo}
                      </span>
                      {actividad.deporteNombre ? (
                        <span className="block truncate text-xs text-[var(--color-muted)]">
                          {actividad.deporteNombre}
                        </span>
                      ) : null}
                    </span>
                  </button>
                </li>
              );
            })}
          </ul>

          <div className="mt-4 flex flex-wrap items-center gap-3">
            <AppButton size="sm" onClick={guardar} disabled={guardando}>
              {guardando ? "Guardando..." : "Guardar destacadas"}
            </AppButton>
            <span className="text-xs font-bold text-[var(--color-muted)]">
              {elegidas.length} de {MAX_DESTACADAS} elegidas
            </span>
          </div>
        </>
      )}

      {mensaje ? (
        <StatusMessage variant="success" role="status" className="mt-4">
          {mensaje}
        </StatusMessage>
      ) : null}

      {error ? (
        <StatusMessage variant="error" role="alert" className="mt-4">
          {error}
        </StatusMessage>
      ) : null}
    </SurfaceCard>
  );
}
