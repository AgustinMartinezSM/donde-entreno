import Link from "next/link";

export function HomePublishCta() {
  return (
    <section className="mt-16 overflow-hidden rounded-[var(--radius-xl)] border border-[#BDE8D0] bg-gradient-to-br from-[#E6F7EF] via-white to-[#E8F6FB] p-5 shadow-[0_20px_50px_rgba(12,52,80,0.12)] sm:mt-20 sm:p-8">
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
          <Link
            href="/publicar"
            className="rounded-[var(--radius-md)] bg-[var(--color-primary)] px-5 py-3 text-center text-sm font-bold text-white shadow-[var(--shadow-button)] transition duration-200 ease-out hover:-translate-y-0.5 hover:bg-[#0B314D] active:scale-[0.98]"
          >
            Publicar actividad
          </Link>
          <Link
            href="/explorar"
            className="rounded-[var(--radius-md)] border border-[#BDE8D0] bg-white px-5 py-3 text-center text-sm font-bold text-[var(--color-primary)] transition duration-200 ease-out hover:-translate-y-0.5 hover:border-[var(--color-primary)] active:scale-[0.98]"
          >
            Ver actividades
          </Link>
        </div>
      </div>
    </section>
  );
}
