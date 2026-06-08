import type { Metadata } from "next";
import Link from "next/link";
import { Header } from "../../components/layout/Header";
import { PublishForm } from "../../components/publicar/PublishForm";

export const metadata: Metadata = {
  /*
    Metadata específica para la página de publicación.
    Por ahora es una primera versión visual del formulario.
  */
  title: "Publicar actividad",
  description:
    "Publicá tu actividad deportiva en DondeEntreno. Formulario inicial para clubes, profesores, gimnasios y espacios deportivos.",
  openGraph: {
    title: "Publicar actividad - DondeEntreno",
    description:
      "Cargá tu actividad deportiva para que más personas puedan encontrar dónde entrenar.",
  },
};

export default function PublicarPage() {
  return (
    <main className="min-h-screen bg-[var(--color-bg)] text-[var(--color-text)]">
      <section className="mx-auto w-full max-w-6xl px-4 py-6">
        <Header />

        <div className="py-8 sm:py-10">
          <Link
            href="/"
            className="text-sm font-bold text-[var(--color-primary)]"
          >
            ← Volver al inicio
          </Link>

          <div className="mt-6 max-w-3xl">
            <p className="mb-3 text-sm font-bold uppercase tracking-[0.2em] text-[var(--color-secondary)]">
              Publicar actividad
            </p>

            <h1 className="text-3xl font-extrabold leading-tight text-[var(--color-primary)] sm:text-4xl">
              Sumá tu actividad a{" "}
              <span className="whitespace-nowrap">
                <span className="text-[var(--color-primary)]">Donde</span>
                <span className="text-[var(--color-secondary)]">Entreno</span>
              </span>
            </h1>

            <p className="mt-4 text-base leading-7 text-[var(--color-muted)]">
              Completa los datos principales de tu actividad deportiva. Esta
              primera versión del formulario nos ayuda a preparar el flujo de
              publicación del MVP.
            </p>
          </div>

          <PublishForm />
        </div>
      </section>
    </main>
  );
}