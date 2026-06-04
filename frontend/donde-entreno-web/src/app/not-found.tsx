import Link from "next/link";
import { Header } from "../components/layout/Header";

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
              Puede que el enlace esté mal escrito o que la página todavía no
              esté disponible en{" "}
              <span className="whitespace-nowrap font-bold">
                <span className="text-[var(--color-primary)]">Donde</span>
                <span className="text-[var(--color-secondary)]">Entreno</span>
              </span>
              .
            </p>

            <div className="mt-6 flex flex-col gap-3 sm:flex-row sm:justify-center">
              <Link
                href="/explorar"
                className="rounded-[var(--radius-md)] bg-[var(--color-primary)] px-5 py-3 text-sm font-bold text-white shadow-[var(--shadow-button)]"
              >
                Explorar actividades
              </Link>

              <Link
                href="/"
                className="rounded-[var(--radius-md)] border border-[var(--color-border)] px-5 py-3 text-sm font-bold text-[var(--color-primary)]"
              >
                Volver al inicio
              </Link>
            </div>
          </div>
        </div>
      </section>
    </main>
  );
}