import { AppLinkButton } from "../ui/AppLinkButton";
import { SurfaceCard } from "../ui/SurfaceCard";

export function HomePublishCta() {
  return (
    <SurfaceCard
      as="section"
      variant="success"
      className="mt-16 overflow-hidden bg-gradient-to-br from-[#E6F7EF] via-white to-[#E8F6FB] p-5 sm:mt-20 sm:p-8"
    >
      <div className="grid gap-6 lg:grid-cols-[1.2fr_0.8fr] lg:items-center">
        <div>
          <p className="text-sm font-bold uppercase tracking-[0.2em] text-[#167A4A]">
            Para publicadores
          </p>
          <h2 className="mt-2 text-2xl font-extrabold text-[var(--color-primary)] sm:text-3xl">
            ¿Tenés una actividad deportiva?
          </h2>
          <p className="mt-3 max-w-2xl text-base leading-7 text-[var(--color-muted)]">
            Sumá tu club, gimnasio o clase para que más personas puedan
            encontrarte.
          </p>
          <p className="mt-2 text-sm font-bold text-[#167A4A]">
            Las solicitudes se revisan antes de publicarse.
          </p>
        </div>

        <div className="flex flex-col gap-3 sm:flex-row lg:justify-end">
          <AppLinkButton href="/publicar" variant="primary">
            Publicar actividad
          </AppLinkButton>
          <AppLinkButton href="/explorar" variant="secondary">
            Ver actividades
          </AppLinkButton>
        </div>
      </div>
    </SurfaceCard>
  );
}
