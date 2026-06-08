import { Header } from "../components/layout/Header";
import { LoadingState } from "../components/feedback/LoadingState";

export default function Loading() {
  return (
    <main className="min-h-screen bg-[var(--color-bg)] text-[var(--color-text)]">
      <section className="mx-auto w-full max-w-6xl px-4 py-6">
        <Header />

        <div className="py-16">
          <LoadingState
            titulo="Cargando DondeEntreno"
            descripcion="Estamos buscando actividades deportivas disponibles cerca tuyo."
          />
        </div>
      </section>
    </main>
  );
}