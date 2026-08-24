"use client";

import { useAuthSession } from "../auth/AuthSessionProvider";
import { useFeedEventos } from "../cuenta/useFeedEventos";
import type { PerfilPublicadorPublico } from "../../types/publicadorPublico";
import { SeguirPublicadorButton } from "../actividad/SeguirPublicadorButton";
import { EsqueletoFeedCard } from "../social/EsqueletoFeedCard";
import { FeedEventoCard } from "../social/FeedEventoCard";
import { PublisherIdentity } from "../social/PublisherIdentity";
import { SectionHeader } from "../ui/SectionHeader";
import { AppButton } from "../ui/AppButton";
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

  Fase 6: pasa a leer el feed de EVENTOS paginado (antes: las últimas
  20 actividades, sin forma de pedir más) y usa el hook compartido en
  vez de duplicar el fetch, que era lo que hacía que un usuario
  logueado pidiera el feed dos veces al pasar por /mi-cuenta.
*/
export function HomeFeedSeguidos({
  publicadoresSugeridos,
}: HomeFeedSeguidosProps) {
  const { status, accessToken } = useAuthSession();
  const { eventos, error, cargando, cargandoMas, hayMas, cargarMas } =
    useFeedEventos(accessToken ?? null);

  /*
    Mientras la sesión se resuelve tampoco dibujamos: si no, el visitante
    anónimo vería aparecer y desaparecer una sección que no le
    corresponde.
  */
  if (status !== "authenticated") {
    return null;
  }

  const sinSeguidos = !error && eventos !== null && eventos.length === 0;

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
          <EsqueletoFeedCard />
          <EsqueletoFeedCard className="hidden lg:block" />
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
          description="Cuando sigas clubes, gimnasios o profes, sus novedades van a aparecer acá arriba."
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
                avatarUrl={publicador.logoUrl}
                href={`/publicadores/${publicador.slug ?? publicador.id}`}
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
        {eventos?.map((evento) => (
          <FeedEventoCard key={evento.id} evento={evento} />
        ))}
      </div>

      {/*
        "Ver más" explícito y no scroll infinito: el proyecto no lo usa
        en ningún lado y acá agregaría complejidad sin beneficio.
      */}
      {hayMas ? (
        <div className="mt-6 flex justify-center">
          <AppButton
            type="button"
            variant="secondary"
            onClick={cargarMas}
            disabled={cargandoMas}
          >
            {cargandoMas ? "Cargando..." : "Ver más novedades"}
          </AppButton>
        </div>
      ) : null}
    </section>
  );
}
