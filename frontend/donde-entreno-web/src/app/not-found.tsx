import { Header } from "../components/layout/Header";
import { BrandName } from "../components/brand/BrandName";
import { AppLinkButton } from "../components/ui/AppLinkButton";
import type { Metadata } from "next";

export const metadata: Metadata = {
  /*
    Metadata para rutas inexistentes.
    Sirve para que la pestaña y los datos básicos no queden genéricos.
  */
  title: "Página no encontrada",
  description:
    "La página que buscás no existe o todavía no está disponible en DondeEntreno.",
};

export default function NotFound() {
  return (
    <main className="min-h-screen bg-[var(--color-bg)] text-[var(--color-text)]">
      <section className="mx-auto w-full max-w-6xl px-4 py-6">
        <Header />

        <div className="flex min-h-[70vh] items-center justify-center py-10">
          <div className="w-full max-w-xl rounded-[var(--radius-xl)] border border-[var(--color-border)] bg-[var(--color-surface)] p-6 text-center shadow-[var(--shadow-card)]">
            <p className="text-sm font-bold uppercase tracking-[0.2em] text-[var(--color-secondary)]">
              Página no encontrada
            </p>

            <h1 className="mt-3 text-3xl font-extrabold text-[var(--color-primary)] sm:text-4xl">
              Esta página no existe
            </h1>

            <p className="mt-4 text-base leading-7 text-[var(--color-muted)]">
              Quizás el enlace cambió o esa página todavía no está disponible
              en{" "}
              <BrandName className="whitespace-nowrap font-bold" />
              .
            </p>

            <div className="mt-6 flex flex-col gap-3 sm:flex-row sm:justify-center">
              <AppLinkButton href="/explorar" variant="primary">
                Explorar actividades
              </AppLinkButton>

              <AppLinkButton href="/ciudades" variant="outline">
                Ver ciudades
              </AppLinkButton>

              <AppLinkButton href="/" variant="secondary">
                Volver al inicio
              </AppLinkButton>
            </div>
          </div>
        </div>
      </section>
    </main>
  );
}
