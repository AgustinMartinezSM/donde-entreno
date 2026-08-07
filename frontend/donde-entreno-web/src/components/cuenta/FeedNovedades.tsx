"use client";

import { useEffect, useState } from "react";

import { useAuthSession } from "../auth/AuthSessionProvider";
import { obtenerFeedActividades } from "../../services/seguimientoService";
import type { ActividadFeed } from "../../types/seguimiento";
import { SocialActivityCard } from "../social/SocialActivityCard";
import { SectionHeader } from "../ui/SectionHeader";
import { StatusMessage } from "../ui/StatusMessage";

/*
  Feed de novedades: las últimas actividades publicadas por los
  publicadores que el usuario sigue (Bloque 8). Es la pieza social
  central de "mi espacio", así que se renderiza con las mismas cards
  del descubrimiento (imagen, publicador, guardar), no como listado.
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

  const cargando = !error && novedades === null;

  return (
    <section aria-labelledby="feed-novedades-titulo">
      <SectionHeader
        eyebrow="Novedades"
        title="Lo nuevo de quienes seguís"
        description="Las últimas actividades publicadas por los clubes, gimnasios y profes que seguís."
        titleId="feed-novedades-titulo"
      />

      {error ? (
        <StatusMessage variant="warning" className="mt-5">
          No pudimos cargar las novedades en este momento. Probá de nuevo en
          unos minutos.
        </StatusMessage>
      ) : null}

      {cargando ? (
        <div
          role="status"
          aria-label="Cargando novedades"
          className="mt-5 grid gap-6 lg:grid-cols-2"
        >
          <EsqueletoCard />
          <EsqueletoCard className="hidden lg:block" />
        </div>
      ) : null}

      {!error && novedades && novedades.length === 0 ? (
        <StatusMessage variant="info" className="mt-5">
          Cuando sigas publicadores, sus nuevas actividades van a aparecer
          acá. Entrá al detalle de una actividad y tocá <strong>Seguir</strong>.
        </StatusMessage>
      ) : null}

      {!error && novedades && novedades.length > 0 ? (
        <div className="mt-5 grid gap-6 lg:grid-cols-2">
          {novedades.map((actividad) => (
            <SocialActivityCard key={actividad.id} actividad={actividad} />
          ))}
        </div>
      ) : null}
    </section>
  );
}

function EsqueletoCard({ className = "" }: { className?: string }) {
  return (
    <div
      aria-hidden="true"
      className={`animate-pulse overflow-hidden rounded-[24px] border border-[#D9E2EC] bg-white p-4 ${className}`}
    >
      <div className="flex items-center gap-3">
        <span className="h-11 w-11 rounded-full bg-[#E8F6FB]" />
        <div className="flex-1">
          <div className="h-3 w-1/3 rounded-full bg-[#E8F6FB]" />
          <div className="mt-2 h-2.5 w-1/4 rounded-full bg-[#F8FAFC]" />
        </div>
      </div>
      <div className="mt-4 h-48 rounded-[20px] bg-[#E8F6FB]" />
      <div className="mt-4 h-4 w-2/3 rounded-full bg-[#E8F6FB]" />
      <div className="mt-3 h-3 w-1/2 rounded-full bg-[#F8FAFC]" />
    </div>
  );
}
