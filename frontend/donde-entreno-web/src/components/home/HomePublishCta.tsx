import { AppLinkButton } from "../ui/AppLinkButton";
import { SurfaceCard } from "../ui/SurfaceCard";

type HomePublishCtaProps = {
  ciudadSlug: string;
};

function crearHrefExplorarCiudad(ciudadSlug: string) {
  const params = new URLSearchParams();

  params.set("ciudadSlug", ciudadSlug);

  return `/explorar?${params.toString()}`;
}

export function HomePublishCta({ ciudadSlug }: HomePublishCtaProps) {
  return (
    /*
      Superficie de marca: la home terminaba en blanco sobre blanco de
      punta a punta y este bloque queda como cierre, alternando con el
      claro de "Cómo funciona" que viene antes.

      Antes pedía variant="success" y además bg-[var(--color-surface)] por className: las
      dos clases competían y la tinta verde nunca llegaba a verse.
    */
    <SurfaceCard
      as="section"
      variant="brand"
      className="mt-14 overflow-hidden p-5 sm:mt-16 sm:p-7"
    >
      <div className="grid gap-6 lg:grid-cols-[1.2fr_0.8fr] lg:items-center">
        <div>
          <p className="text-sm font-bold uppercase tracking-[0.2em] text-[#7FDCA8]">
            Para publicadores
          </p>
          <h2 className="mt-2 text-2xl font-extrabold sm:text-3xl">
            Sumá tu club, profe o actividad
          </h2>
          <p className="mt-3 max-w-2xl text-base leading-7 text-[#BFDDEA]">
            Si sos club, profe, gimnasio o espacio deportivo, cargá tu propuesta
            para que más personas puedan encontrarte.
          </p>
          <p className="mt-2 text-sm font-bold text-[#7FDCA8]">
            Revisamos cada solicitud antes de publicarla.
          </p>
        </div>

        <div className="flex flex-col gap-3 sm:flex-row lg:justify-end">
          <AppLinkButton href="/publicar" variant="success">
            Sumar mi actividad
          </AppLinkButton>
          <AppLinkButton
            href={crearHrefExplorarCiudad(ciudadSlug)}
            variant="secondary"
            className="border-white/40 bg-white/10 text-white hover:border-white hover:bg-white/20"
          >
            Explorar actividades
          </AppLinkButton>
        </div>
      </div>
    </SurfaceCard>
  );
}
