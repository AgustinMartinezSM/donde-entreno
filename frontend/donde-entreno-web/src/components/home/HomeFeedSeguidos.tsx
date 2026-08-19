"use client";

import { useEffect, useState } from "react";

import { useAuthSession } from "../auth/AuthSessionProvider";
import { obtenerFeedActividades } from "../../services/seguimientoService";
import type { ActividadFeed } from "../../types/seguimiento";
import type { PerfilPublicadorPublico } from "../../types/publicadorPublico";
import { SeguirPublicadorButton } from "../actividad/SeguirPublicadorButton";
import { PublisherIdentity } from "../social/PublisherIdentity";
import { SocialActivityCard } from "../social/SocialActivityCard";
import { SectionHeader } from "../ui/SectionHeader";
import { StatusMessage } from "../ui/StatusMessage";

type HomeFeedSeguidosProps = {
  publicadoresSugeridos: PerfilPublicadorPublico[];
};

/*
  Feed de quienes seguís, arriba de todo en la home.

  Sigue la lógica de una app social: para quien tiene cuenta, lo primero
  es lo nuevo de la gente que sigue. Si todavía no sigue a nadie no se
  muestra un hueco vacío sino a quién seguir, con el botón al lado, que
  es lo que convierte el vacío en el primer paso.

  Para el visitante anónimo no se dibuja nada: la home pública sigue
  arrancando por el descubrimiento general.
*/
export function HomeFeedSeguidos({
  publicadoresSugeridos,
}: HomeFeedSeguidosProps) {
  const { status, accessToken } = useAuthSession();
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

  /*
    Mientras la sesión se resuelve tampoco dibujamos: si no, el visitante
    anónimo vería aparecer y desaparecer una sección que no le
    corresponde.
  */
  if (status !== "authenticated") {
    return null;
  }

  const cargando = !error && novedades === null;
  const sinSeguidos = !error && novedades !== null && novedades.length === 0;

  /*
    Un error del feed no puede tapar el resto de la home: se avisa en
    chico y el descubrimiento general sigue abajo.
  */
  if (error) {
    return (
      <section className="mt-6" aria-labelledby="feed-seguidos-titulo">
        <h2 id="feed-seguidos-titulo" className="sr-only">
          De quienes seguís
        </h2>
        <StatusMessage variant="warning">
          No pudimos cargar lo nuevo de quienes seguís. Probá de nuevo en unos
          minutos.
        </StatusMessage>
      </section>
    );
  }

  if (cargando) {
    return (
      <section className="mt-6" aria-labelledby="feed-seguidos-titulo">
        <h2 id="feed-seguidos-titulo" className="sr-only">
          De quienes seguís
        </h2>
        <div
          role="status"
          aria-label="Cargando lo nuevo de quienes seguís"
          className="grid gap-5 lg:grid-cols-2"
        >
          <EsqueletoCard />
          <EsqueletoCard className="hidden lg:block" />
        </div>
      </section>
    );
  }

  if (sinSeguidos) {
    if (publicadoresSugeridos.length === 0) {
      return null;
    }

    return (
      <section className="mt-8" aria-labelledby="feed-seguidos-titulo">
        <SectionHeader
          eyebrow="Para empezar"
          title="Seguí a quienes te interesan"
          description="Cuando sigas clubes, gimnasios o profes, sus nuevas actividades van a aparecer acá arriba."
          titleId="feed-seguidos-titulo"
        />

        <ul className="mt-5 grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
          {publicadoresSugeridos.map((publicador) => (
            <li
              key={publicador.id}
              className="flex items-center justify-between gap-3 rounded-[var(--radius-lg)] border border-[var(--color-border)] bg-[var(--color-surface)] p-4"
            >
              <PublisherIdentity
                nombre={publicador.nombre}
                tipo={publicador.tipoPublicador}
                verificado={publicador.verificado === true}
                href={`/publicadores/${publicador.id}`}
                tamanio="compacta"
              />
              <SeguirPublicadorButton
                perfilPublicadorId={publicador.id}
                perfilPublicadorNombre={publicador.nombre}
              />
            </li>
          ))}
        </ul>
      </section>
    );
  }

  return (
    <section className="mt-8" aria-labelledby="feed-seguidos-titulo">
      <SectionHeader
        eyebrow="Tu feed"
        title="Lo nuevo de quienes seguís"
        titleId="feed-seguidos-titulo"
      />

      <div className="mt-5 grid gap-5 lg:grid-cols-2">
        {novedades?.map((actividad) => (
          <SocialActivityCard key={actividad.id} actividad={actividad} />
        ))}
      </div>
    </section>
  );
}

function EsqueletoCard({ className = "" }: { className?: string }) {
  return (
    <div
      aria-hidden="true"
      className={`animate-pulse overflow-hidden rounded-[24px] border border-[var(--color-border)] bg-[var(--color-surface)] p-4 ${className}`}
    >
      <div className="flex items-center gap-3">
        <span className="h-11 w-11 rounded-full bg-[var(--color-info-soft)]" />
        <div className="flex-1">
          <div className="h-3 w-1/3 rounded-full bg-[var(--color-info-soft)]" />
          <div className="mt-2 h-2.5 w-1/4 rounded-full bg-[var(--color-bg)]" />
        </div>
      </div>
      <div className="mt-4 h-48 rounded-[20px] bg-[var(--color-info-soft)]" />
      <div className="mt-4 h-4 w-2/3 rounded-full bg-[var(--color-info-soft)]" />
      <div className="mt-3 h-3 w-1/2 rounded-full bg-[var(--color-bg)]" />
    </div>
  );
}
