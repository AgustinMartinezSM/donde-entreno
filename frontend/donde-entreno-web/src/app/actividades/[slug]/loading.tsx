import { Header } from "../../../components/layout/Header";
import { LoadingState } from "../../../components/feedback/LoadingState";

export default function ActividadDetalleLoading() {
  return (
    <main className="min-h-screen bg-[var(--color-bg)] text-[var(--color-text)]">
      <section className="mx-auto w-full max-w-6xl px-4 py-6">
        <Header />

        <div className="py-8 sm:py-10">
          <LoadingState
            titulo="Cargando actividad"
            descripcion="Estamos buscando el detalle, horarios e información de contacto de esta actividad."
          />
        </div>
      </section>
    </main>
  );
}