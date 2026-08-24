import type { ReactNode } from "react";

import { Header } from "../layout/Header";
import { SectionHeader } from "../ui/SectionHeader";
import { SurfaceCard } from "../ui/SurfaceCard";

/*
  Marco compartido de las páginas de normas/términos/privacidad
  (Fase 1 de la etapa social): mismo shell visual de la app, prosa
  legible, y las tres páginas se linkean entre sí desde el pie.
*/
export function PaginaLegal({
  eyebrow,
  titulo,
  descripcion,
  children,
}: {
  eyebrow: string;
  titulo: string;
  descripcion: string;
  children: ReactNode;
}) {
  return (
    <main className="min-h-screen text-[var(--color-text)]">
      <section className="mx-auto w-full max-w-4xl px-4 py-6">
        <Header />

        <SurfaceCard as="article" className="mt-6 p-6 sm:p-8">
          <SectionHeader
            eyebrow={eyebrow}
            title={titulo}
            description={descripcion}
          />

          <div className="mt-6 space-y-6">{children}</div>
        </SurfaceCard>
      </section>
    </main>
  );
}

export function SeccionLegal({
  titulo,
  children,
}: {
  titulo: string;
  children: ReactNode;
}) {
  return (
    <section>
      <h2 className="text-lg font-extrabold text-[var(--color-primary)]">
        {titulo}
      </h2>
      <div className="mt-2 space-y-3 text-sm leading-7 text-[var(--color-muted)]">
        {children}
      </div>
    </section>
  );
}
