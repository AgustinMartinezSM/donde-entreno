"use client";

import { useEffect, useState } from "react";

import { useAuthSession } from "../auth/AuthSessionProvider";
import { AppButton } from "../ui/AppButton";
import { StatusMessage } from "../ui/StatusMessage";
import { SurfaceCard } from "../ui/SurfaceCard";
import {
  PublicadorApiError,
  guardarCoordenadasUbicacion,
  listarMisUbicaciones,
} from "../../services/publicadorService";
import { coordenadasDe } from "../../lib/mapas";
import type { UbicacionPublicador } from "../../types/publicador";

/*
  Carga del punto exacto de cada sede (Fase 7).

  Sin mapa para pinchar y sin geocoding: la persona pega el link de
  Google Maps de su lugar —algo que ya sabe hacer— y el backend extrae
  las coordenadas. Es exacto, no necesita API keys y no suma ninguna
  dependencia al frontend.

  Por qué importa: hoy la mayoría de las sedes no tiene coordenadas, y
  sin ellas la actividad queda fuera de "cerca mío".
*/
export function UbicacionesDelPublicador() {
  const { accessToken } = useAuthSession();
  const [ubicaciones, setUbicaciones] = useState<UbicacionPublicador[] | null>(
    null
  );
  const [errorCarga, setErrorCarga] = useState(false);

  useEffect(() => {
    if (!accessToken) {
      return;
    }

    let vigente = true;

    listarMisUbicaciones(accessToken)
      .then((lista) => {
        if (vigente) {
          setUbicaciones(lista);
        }
      })
      .catch(() => {
        if (vigente) {
          setErrorCarga(true);
        }
      });

    return () => {
      vigente = false;
    };
  }, [accessToken]);

  function reemplazar(actualizada: UbicacionPublicador) {
    setUbicaciones((actuales) =>
      (actuales ?? []).map((ubicacion) =>
        ubicacion.id === actualizada.id ? actualizada : ubicacion
      )
    );
  }

  if (errorCarga) {
    return (
      <StatusMessage variant="warning" className="mt-6">
        No pudimos cargar tus sedes. Probá de nuevo en unos minutos.
      </StatusMessage>
    );
  }

  if (ubicaciones === null) {
    return (
      <StatusMessage variant="info" role="status" className="mt-6">
        Cargando tus sedes...
      </StatusMessage>
    );
  }

  if (ubicaciones.length === 0) {
    return null;
  }

  const sinPunto = ubicaciones.filter(
    (ubicacion) => coordenadasDe(ubicacion) === null
  ).length;

  return (
    <SurfaceCard variant="info" className="mt-6 p-5 sm:p-6">
      <h2 className="text-lg font-extrabold text-[var(--color-primary)]">
        Dónde quedan tus sedes
      </h2>
      <p className="mt-1.5 text-sm leading-6 text-[var(--color-muted)]">
        Cargá el punto exacto de cada lugar para que aparezca cuando
        alguien busca actividades cerca suyo.
        {sinPunto > 0 ? (
          <>
            {" "}
            <span className="font-bold text-[var(--color-primary)]">
              {sinPunto === 1
                ? "Te falta 1 sede."
                : `Te faltan ${sinPunto} sedes.`}
            </span>
          </>
        ) : null}
      </p>

      <ul className="mt-4 grid gap-3">
        {ubicaciones.map((ubicacion) => (
          <FilaUbicacion
            key={ubicacion.id}
            ubicacion={ubicacion}
            accessToken={accessToken}
            onGuardada={reemplazar}
          />
        ))}
      </ul>
    </SurfaceCard>
  );
}

function FilaUbicacion({
  ubicacion,
  accessToken,
  onGuardada,
}: {
  ubicacion: UbicacionPublicador;
  accessToken: string | null;
  onGuardada: (actualizada: UbicacionPublicador) => void;
}) {
  const [abierto, setAbierto] = useState(false);
  const [pegado, setPegado] = useState("");
  const [guardando, setGuardando] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [mensaje, setMensaje] = useState<string | null>(null);

  const coordenadas = coordenadasDe(ubicacion);

  async function guardar() {
    if (!accessToken || guardando) {
      return;
    }

    setGuardando(true);
    setError(null);
    setMensaje(null);

    try {
      const actualizada = await guardarCoordenadasUbicacion(
        ubicacion.id,
        pegado,
        accessToken
      );
      onGuardada(actualizada);
      setMensaje("Listo: esta sede ya tiene su punto cargado.");
      setAbierto(false);
      setPegado("");
    } catch (errorGuardar: unknown) {
      /* El backend explica qué hacer (link corto, texto ilegible...). */
      setError(
        errorGuardar instanceof PublicadorApiError
          ? errorGuardar.message
          : "No pudimos guardar el punto. Probá nuevamente."
      );
    } finally {
      setGuardando(false);
    }
  }

  return (
    <li className="rounded-[14px] border border-[var(--color-border-soft)] bg-[var(--color-bg)] p-4">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div className="min-w-0">
          <p className="text-sm font-extrabold text-[var(--color-primary)]">
            {ubicacion.nombre || "Sede sin nombre"}
          </p>
          {ubicacion.direccion ? (
            <p className="mt-0.5 text-xs text-[var(--color-muted)]">
              {ubicacion.direccion}
            </p>
          ) : null}
        </div>

        {coordenadas ? (
          <span className="rounded-full bg-[var(--color-success-soft)] px-3 py-1 text-xs font-extrabold text-[var(--color-success)]">
            Ubicación cargada
          </span>
        ) : (
          <span className="rounded-full bg-[var(--color-warning-surface)] px-3 py-1 text-xs font-extrabold text-[var(--color-warning)]">
            Falta el punto
          </span>
        )}
      </div>

      {abierto ? (
        <div className="mt-3">
          <label
            htmlFor={`punto-${ubicacion.id}`}
            className="text-xs font-bold text-[var(--color-primary)]"
          >
            Pegá el link de Google Maps de esta sede
          </label>
          <input
            id={`punto-${ubicacion.id}`}
            type="text"
            value={pegado}
            onChange={(evento) => setPegado(evento.target.value)}
            disabled={guardando}
            placeholder="https://www.google.com/maps/place/..."
            className="mt-1.5 min-h-11 w-full rounded-[12px] border border-[var(--color-border-accent)] bg-[var(--color-surface)] px-3 text-sm text-[var(--color-text)] outline-none transition focus:border-[var(--color-accent)] disabled:opacity-60"
          />
          <p className="mt-1.5 text-xs leading-5 text-[var(--color-muted)]">
            Buscá tu lugar en Google Maps y copiá el link de la barra del
            navegador. También podés pegar las coordenadas separadas por coma.
          </p>

          <div className="mt-3 flex flex-wrap gap-2">
            <AppButton size="sm" onClick={guardar} disabled={guardando || !pegado.trim()}>
              {guardando ? "Guardando..." : "Guardar punto"}
            </AppButton>
            <AppButton
              size="sm"
              variant="secondary"
              onClick={() => {
                setAbierto(false);
                setError(null);
              }}
              disabled={guardando}
            >
              Cancelar
            </AppButton>
          </div>
        </div>
      ) : (
        <button
          type="button"
          onClick={() => {
            setAbierto(true);
            setMensaje(null);
          }}
          className="mt-2 text-xs font-extrabold text-[var(--color-primary)] underline decoration-[var(--color-border-accent)] underline-offset-4 transition hover:decoration-[var(--color-primary)]"
        >
          {coordenadas ? "Corregir el punto" : "Cargar el punto"}
        </button>
      )}

      {mensaje ? (
        <StatusMessage variant="success" role="status" className="mt-3">
          {mensaje}
        </StatusMessage>
      ) : null}

      {error ? (
        <StatusMessage variant="error" role="alert" className="mt-3">
          {error}
        </StatusMessage>
      ) : null}
    </li>
  );
}
