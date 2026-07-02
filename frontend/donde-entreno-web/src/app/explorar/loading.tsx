import { Header } from "../../components/layout/Header";
import { LoadingState } from "../../components/feedback/LoadingState";
import { BrandName } from "../../components/brand/BrandName";

export default function ExplorarLoading() {
  return (
    <main className="min-h-screen bg-[var(--color-bg)] text-[var(--color-text)]">
      <section className="mx-auto w-full max-w-6xl px-4 py-6">
        <Header />

        <div className="py-8 sm:py-10">
          <div className="mb-8">
            <p className="mb-3 text-sm font-bold uppercase tracking-[0.2em] text-[var(--color-secondary)]">
              Explorar actividades
            </p>

            <h1 className="text-3xl font-extrabold leading-tight text-[var(--color-primary)] sm:text-4xl">
              Cargando resultados
            </h1>

            <p className="mt-3 max-w-2xl text-base leading-7 text-[var(--color-muted)]">
              Estamos preparando las actividades y filtros disponibles.
            </p>
          </div>

          <LoadingState
            titulo="Cargando actividades"
            descripcion={
              <>
                Estamos buscando actividades deportivas, filtros y resultados
                disponibles en <BrandName className="inline font-bold" />.
              </>
            }
          />
        </div>
      </section>
    </main>
  );
}
