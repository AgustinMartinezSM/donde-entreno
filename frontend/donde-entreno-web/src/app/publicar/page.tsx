import type { Metadata } from "next";
import { BrandName } from "../../components/brand/BrandName";
import { Header } from "../../components/layout/Header";
import { PublishForm } from "../../components/publicar/PublishForm";
import { AppLinkButton } from "../../components/ui/AppLinkButton";
import { SurfaceCard } from "../../components/ui/SurfaceCard";

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
    <main className="min-h-screen bg-gradient-to-br from-[#F8FAFC] via-white to-[#E8F6FB] text-[var(--color-text)]">
      <section className="mx-auto w-full max-w-6xl px-4 py-6">
        <Header />

        <div className="py-8 sm:py-10">
          <AppLinkButton
            href="/"
            variant="secondary"
            size="sm"
            className="w-fit rounded-full"
          >
            ← Volver al inicio
          </AppLinkButton>

          <SurfaceCard
            as="section"
            variant="success"
            className="mt-6 overflow-hidden bg-gradient-to-br from-white via-[#F8FCFE] to-[#E6F7EF] p-5 sm:p-8"
          >
            <div className="grid gap-6 lg:grid-cols-[1.25fr_0.75fr] lg:items-center">
              <div>
                <p className="mb-3 text-sm font-bold uppercase tracking-[0.2em] text-[var(--color-secondary)]">
                  PUBLICAR ACTIVIDAD
                </p>

                <h1 className="max-w-3xl text-4xl font-extrabold leading-tight text-[var(--color-primary)] sm:text-5xl">
                  Sumá tu actividad a{" "}
                  <BrandName className="whitespace-nowrap" />
                </h1>

                <p className="mt-4 max-w-2xl text-base leading-7 text-[var(--color-muted)] sm:text-lg">
                  Completá los datos de tu club, gimnasio, clase o espacio
                  deportivo. La solicitud será revisada antes de publicarse.
                </p>
              </div>

              <div className="rounded-[var(--radius-lg)] border border-[#BDE8D0] bg-white/90 p-4 shadow-[0_16px_40px_rgba(12,52,80,0.08)]">
                <p className="text-sm font-extrabold uppercase tracking-[0.16em] text-[#167A4A]">
                  ¿Qué pasa después?
                </p>
                <ul className="mt-4 space-y-3 text-sm font-bold leading-6 text-[var(--color-primary)]">
                  <li>Llegá a personas que buscan entrenar en tu ciudad.</li>
                  <li>Mostrá horarios, ubicación y datos de contacto.</li>
                  <li>La publicación se revisa antes de quedar visible.</li>
                </ul>
              </div>
            </div>
          </SurfaceCard>

          <PublishForm />
        </div>
      </section>
    </main>
  );
}
