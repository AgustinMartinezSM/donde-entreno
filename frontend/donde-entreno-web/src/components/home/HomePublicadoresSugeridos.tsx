import type { PerfilPublicadorPublico } from "../../types/publicadorPublico";
import { SeguirPublicadorButton } from "../actividad/SeguirPublicadorButton";
import { PublisherIdentity } from "../social/PublisherIdentity";
import { AppLinkButton } from "../ui/AppLinkButton";
import { SectionHeader } from "../ui/SectionHeader";

type HomePublicadoresSugeridosProps = {
  publicadores: PerfilPublicadorPublico[];
};

/*
  Sección social de la home: clubes, gimnasios y profes reales de la
  plataforma para empezar a seguir. Los datos vienen por SSR desde
  /api/perfiles-publicadores; si no hay perfiles, la sección no se
  renderiza (nada de conteos ni perfiles inventados).
*/
export function HomePublicadoresSugeridos({
  publicadores,
}: HomePublicadoresSugeridosProps) {
  if (publicadores.length === 0) {
    return null;
  }

  return (
    <section
      className="mt-14 sm:mt-16"
      aria-labelledby="publicadores-sugeridos-titulo"
    >
      <SectionHeader
        eyebrow="Comunidad"
        title="Clubes y profes para seguir"
        description="Seguí a quienes publican actividades y enterate de sus novedades en tu espacio."
        titleId="publicadores-sugeridos-titulo"
      />

      <div className="mt-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {publicadores.map((publicador) => (
          <article
            key={publicador.id}
            className="flex h-full flex-col rounded-[20px] border border-[var(--color-border)] bg-[var(--color-surface)] p-5 shadow-[0_8px_24px_rgba(15,61,94,0.07)] transition duration-200 ease-out hover:-translate-y-1 hover:border-[var(--color-border-accent)] hover:shadow-[0_16px_40px_rgba(15,61,94,0.12)]"
          >
            <PublisherIdentity
              nombre={publicador.nombre}
              tipo={publicador.tipoPublicador}
              verificado={publicador.verificado === true}
              avatarUrl={publicador.logoUrl}
              href={`/publicadores/${publicador.slug ?? publicador.id}`}
            />

            {publicador.descripcion ? (
              <p className="mt-3 line-clamp-2 text-sm leading-6 text-[var(--color-muted)]">
                {publicador.descripcion}
              </p>
            ) : null}

            <div className="mt-auto flex flex-wrap items-center gap-2 pt-4">
              <SeguirPublicadorButton
                perfilPublicadorId={publicador.id}
                perfilPublicadorNombre={publicador.nombre}
              />
              <AppLinkButton
                href={`/publicadores/${publicador.slug ?? publicador.id}`}
                variant="outline"
                size="sm"
              >
                Ver perfil
              </AppLinkButton>
            </div>
          </article>
        ))}
      </div>
    </section>
  );
}
