"use client";

import { SocialActivityCard } from "../social/SocialActivityCard";
import { SectionHeader } from "../ui/SectionHeader";
import { StatusMessage } from "../ui/StatusMessage";
import { BloqueAsistente } from "./BloqueAsistente";
import { ComunidadSugerida } from "./ComunidadSugerida";
import { EspacioDeRol } from "./EspacioDeRol";
import { ProgresoPerfil } from "./ProgresoPerfil";
import { RecomendadosParaVos } from "./RecomendadosParaVos";
import type { FeedNovedades } from "./useFeedNovedades";
import type { PerfilDeportivo, TabPerfil } from "./usePerfilDeportivo";
import type { Seguimientos } from "./useSeguimientos";

type ParaVosProps = {
  perfil: PerfilDeportivo;
  feed: FeedNovedades;
  seguimientos: Seguimientos;
  rol: string | null;
  onIrATab: (tab: TabPerfil) => void;
};

/*
  "Para vos": la portada del espacio deportivo de la persona.

  Antes esta solapa era solo el feed de quienes seguís, así que para
  alguien que todavía no seguía a nadie era una pantalla con un cartel y
  nada más. Ahora siempre tiene algo: si sigue gente abre por sus
  novedades, y si no, abre por a quién seguir. Debajo, en los dos casos,
  van recomendaciones armadas con su ciudad y sus deportes.

  Nada de esto usa datos inventados: novedades y seguidos salen del
  backend, las recomendaciones son la búsqueda pública real, y ciudad y
  deportes salen de lo que la persona eligió en este dispositivo.
*/
export function ParaVos({
  perfil,
  feed,
  seguimientos,
  rol,
  onIrATab,
}: ParaVosProps) {
  const idsSeguidos = (seguimientos.seguidos ?? [])
    .map((publicador) => publicador.perfilPublicadorId)
    .filter((id) => !seguimientos.idsNoSeguidos.includes(id));

  const hayNovedades = (feed.novedades?.length ?? 0) > 0;
  /*
    Solo proponemos a quién seguir cuando sabemos que no sigue a nadie:
    mientras el dato viaja no adelantamos un estado vacío que quizás no
    sea cierto.
  */
  const sinSeguidos =
    !feed.error && feed.novedades !== null && feed.novedades.length === 0;

  /*
    En pantallas grandes el contenido va a la izquierda y lo de apoyo
    —progreso y asistente— a una columna lateral: apilado a lo ancho, el
    feed quedaba en una franja arriba y el resto de la pantalla vacío.

    El aside va primero en el DOM y se manda a la derecha con order
    porque en mobile el progreso tiene que estar arriba de todo: es lo
    que le dice a alguien recién llegado qué hacer.
  */
  return (
    <div className="grid items-start gap-10 sm:gap-12 xl:grid-cols-[minmax(0,1fr)_19rem] xl:gap-8">
      {/*
        En xl esta columna mide 304px, así que su card del asistente sabe
        que ahí tiene que apilar: con el layout ancho el texto se quedaba
        sin lugar y caía en vertical.
      */}
      <aside className="grid gap-6 xl:sticky xl:top-6 xl:order-2">
        {/*
          El puente al espacio de publicador/administración va primero:
          para quien tiene dos mundos, saber cómo cruzar al otro es lo
          más valioso de esta columna.
        */}
        <EspacioDeRol rol={rol} />
        <ProgresoPerfil perfil={perfil} onIrATab={onIrATab} />
        <BloqueAsistente disposicion="lateral" />
      </aside>

      <div className="grid min-w-0 gap-10 sm:gap-12 xl:order-1">
        {feed.error ? (
          <StatusMessage variant="warning">
            No pudimos cargar lo nuevo de quienes seguís. Probá de nuevo en unos
            minutos.
          </StatusMessage>
        ) : null}

        {feed.cargando ? (
          <div
            role="status"
            aria-label="Cargando lo nuevo de quienes seguís"
            className="grid gap-6 lg:grid-cols-2"
          >
            <EsqueletoCard />
            <EsqueletoCard className="hidden lg:block" />
          </div>
        ) : null}

        {hayNovedades ? (
          <section aria-labelledby="novedades-titulo">
            <SectionHeader
              eyebrow="Tu feed"
              title="Lo nuevo de quienes seguís"
              description="Las últimas actividades publicadas por los clubes, gimnasios y profes que seguís."
              titleId="novedades-titulo"
            />

            <div className="mt-5 grid gap-6 lg:grid-cols-2">
              {feed.novedades?.map((actividad) => (
                <SocialActivityCard key={actividad.id} actividad={actividad} />
              ))}
            </div>
          </section>
        ) : null}

        {sinSeguidos ? (
          <ComunidadSugerida
            idsSeguidos={idsSeguidos}
            titulo="Empezá a armar tu comunidad deportiva"
            descripcion="Cuando sigas clubes, gimnasios o profes, sus nuevas actividades van a aparecer acá arriba."
          />
        ) : null}

        <RecomendadosParaVos
          deportesSlugs={perfil.deportesSlugs}
          ciudadSlug={perfil.ciudadSlug}
          ciudadNombre={perfil.ciudadNombre}
          slugsGuardados={perfil.favoritos.map((favorito) => favorito.slug)}
        />
      </div>
    </div>
  );
}

function EsqueletoCard({ className = "" }: { className?: string }) {
  return (
    <div
      aria-hidden="true"
      className={`animate-pulse overflow-hidden rounded-[24px] border border-[var(--color-border)] bg-white p-4 ${className}`}
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
