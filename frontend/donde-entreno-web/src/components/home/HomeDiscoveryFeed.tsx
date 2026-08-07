import type { Actividad } from "../../types/actividad";
import { SocialActivityCard } from "../social/SocialActivityCard";
import { ErrorState } from "../feedback/ErrorState";
import { AppLinkButton } from "../ui/AppLinkButton";
import { SectionHeader } from "../ui/SectionHeader";
import { StatusMessage } from "../ui/StatusMessage";

type HomeDiscoveryFeedProps = {
  actividades: Actividad[];
  ciudadNombre: string;
  ciudadSlug: string;
  huboError: boolean;
};

export function HomeDiscoveryFeed({
  actividades,
  ciudadNombre,
  ciudadSlug,
  huboError,
}: HomeDiscoveryFeedProps) {
  const hrefExplorar = `/explorar?ciudadSlug=${encodeURIComponent(ciudadSlug)}`;

  return (
    <section className="mt-12 sm:mt-16" aria-labelledby="descubrimiento-titulo">
      {/* SectionHeader compartido: misma jerarquía tipográfica que el
          resto de las secciones de la home. */}
      <SectionHeader
        eyebrow="Tu comunidad deportiva local"
        title={`Descubrí actividades en ${ciudadNombre}`}
        description="Clubes, profes y espacios de tu ciudad, reunidos para que encuentres una actividad que encaje con vos."
        titleId="descubrimiento-titulo"
        action={
          <AppLinkButton
            href={hrefExplorar}
            variant="secondary"
            className="w-fit"
          >
            Ver todas
          </AppLinkButton>
        }
      />

      {huboError ? (
        <div className="mt-7">
          <ErrorState
            titulo="No pudimos cargar las actividades"
            descripcion="No pudimos conectarnos con el servidor. Intentá nuevamente en unos minutos."
            mostrarBotonInicio={false}
            mostrarBotonExplorar
          />
        </div>
      ) : actividades.length === 0 ? (
        <StatusMessage
          variant="info"
          title={`Todavía no hay actividades para mostrar en ${ciudadNombre}`}
          className="mt-7 p-7 text-center"
        >
          <p className="mx-auto max-w-xl">
            Probá otro deporte, revisá el listado completo o elegí una ciudad
            diferente.
          </p>
          <div className="mt-5 flex flex-col justify-center gap-3 sm:flex-row">
            <AppLinkButton href={hrefExplorar} size="sm">
              Explorar actividades
            </AppLinkButton>
            <AppLinkButton href="/ciudades" variant="secondary" size="sm">
              Ver ciudades
            </AppLinkButton>
          </div>
        </StatusMessage>
      ) : (
        <div className="mt-7 grid gap-6 lg:grid-cols-2">
          {actividades.map((actividad) => (
            <SocialActivityCard key={actividad.id} actividad={actividad} />
          ))}
        </div>
      )}
    </section>
  );
}
