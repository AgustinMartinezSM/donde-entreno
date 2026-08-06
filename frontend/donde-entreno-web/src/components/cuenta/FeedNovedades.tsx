"use client";

import Link from "next/link";
import { useEffect, useState } from "react";

import { useAuthSession } from "../auth/AuthSessionProvider";
import { obtenerFeedActividades } from "../../services/seguimientoService";
import type { ActividadFeed } from "../../types/seguimiento";
import { SectionHeader } from "../ui/SectionHeader";
import { StatusMessage } from "../ui/StatusMessage";
import { SurfaceCard } from "../ui/SurfaceCard";

/*
  Sección de mi-cuenta: novedades de los publicadores seguidos
  (Bloque 8). Muestra las últimas actividades publicadas por los
  publicadores que el usuario sigue, con link al detalle.
*/
export function FeedNovedades() {
  const { accessToken } = useAuthSession();
  const [novedades, setNovedades] = useState<ActividadFeed[] | null>(null);
  const [error, setError] = useState(false);

  useEffect(() => {
    let activo = true;

    if (!accessToken) {
      return () => {
        activo = false;
      };
    }

    obtenerFeedActividades(accessToken)
      .then((lista) => {
        if (activo) {
          setNovedades(lista);
          setError(false);
        }
      })
      .catch(() => {
        if (activo) {
          setError(true);
        }
      });

    return () => {
      activo = false;
    };
  }, [accessToken]);

  return (
    <SurfaceCard className="p-6 sm:p-8">
      <SectionHeader
        eyebrow="Novedades"
        title="Lo nuevo de quienes seguís"
        description="Las últimas actividades publicadas por los publicadores que seguís."
      />

      {error ? (
        <StatusMessage variant="info" className="mt-5">
          No pudimos cargar las novedades en este momento.
        </StatusMessage>
      ) : null}

      {!error && novedades && novedades.length === 0 ? (
        <StatusMessage variant="info" className="mt-5">
          Cuando sigas publicadores, sus nuevas actividades van a aparecer
          acá. Entrá al detalle de una actividad y tocá{" "}
          <strong>Seguir</strong>.
        </StatusMessage>
      ) : null}

      {!error && novedades && novedades.length > 0 ? (
        <ul className="mt-5 grid gap-3">
          {novedades.map((actividad) => (
            <li key={actividad.id}>
              <Link
                href={`/actividades/${encodeURIComponent(actividad.slug)}`}
                className="flex flex-wrap items-center justify-between gap-3 rounded-[18px] border border-[#DDEAF3] bg-white/85 p-4 shadow-[0_10px_24px_rgba(12,52,80,0.05)] transition duration-200 ease-out hover:-translate-y-0.5 hover:border-[var(--color-accent)]"
              >
                <span>
                  <span className="block text-sm font-extrabold text-[var(--color-primary)]">
                    {actividad.titulo}
                  </span>
                  <span className="mt-1 block text-xs font-bold uppercase tracking-[0.1em] text-[var(--color-secondary)]">
                    {[
                      actividad.perfilPublicadorNombre,
                      actividad.deporteNombre,
                      [actividad.barrioNombre, actividad.ciudadNombre]
                        .filter(Boolean)
                        .join(", "),
                    ]
                      .filter(Boolean)
                      .join(" · ")}
                  </span>
                </span>
                <span className="text-sm font-bold text-[var(--color-accent)]">
                  Ver detalle →
                </span>
              </Link>
            </li>
          ))}
        </ul>
      ) : null}
    </SurfaceCard>
  );
}
