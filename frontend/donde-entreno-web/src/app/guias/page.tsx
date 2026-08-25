import type { Metadata } from "next";
import Link from "next/link";

import { Header } from "../../components/layout/Header";
import { SectionHeader } from "../../components/ui/SectionHeader";
import { SurfaceCard } from "../../components/ui/SurfaceCard";
import { GUIAS } from "../../lib/guias";

export const metadata: Metadata = {
  title: "Guías para empezar",
  description:
    "Guías para empezar un deporte: qué es, cómo es la primera clase, qué necesitás y qué preguntar antes de anotarte.",
  alternates: { canonical: "/guias" },
  openGraph: {
    title: "Guías para empezar - DondeEntreno",
    description:
      "Qué esperar de cada deporte antes de dar el primer paso.",
  },
};

/*
  Índice de guías (Fase 10).

  Arranca con una sola guía a propósito: son textos que el sitio
  publica como propios, así que se revisan de a una antes de sumar la
  siguiente. El índice ya está preparado para varias.
*/
export default function GuiasPage() {
  return (
    <main className="min-h-screen text-[var(--color-text)]">
      <section className="mx-auto w-full max-w-5xl px-4 py-6">
        <Header />

        <div className="mt-6">
          <SectionHeader
            eyebrow="Guías"
            title="Guías para empezar"
            description="Qué es cada deporte, cómo es la primera clase y qué conviene preguntar antes de anotarte. Los precios, horarios y lugares los pone el catálogo: acá va lo que no cambia."
          />
        </div>

        <div className="mt-6 grid gap-4 sm:grid-cols-2">
          {GUIAS.map((guia) => (
            <SurfaceCard key={guia.slug} as="article" className="p-5">
              <h2 className="text-lg font-extrabold text-[var(--color-primary)]">
                <Link
                  href={`/guias/${guia.slug}`}
                  className="underline-offset-4 hover:underline"
                >
                  {guia.titulo}
                </Link>
              </h2>

              <p className="mt-2 text-sm leading-6 text-[var(--color-muted)]">
                {guia.resumen}
              </p>

              <Link
                href={`/guias/${guia.slug}`}
                className="mt-3 inline-block text-sm font-bold text-[var(--color-primary)] underline-offset-4 hover:underline"
              >
                Leer la guía
              </Link>
            </SurfaceCard>
          ))}
        </div>

        {/*
          Con una sola guía publicada, decirlo es más honesto que dejar
          una grilla de una tarjeta sin explicación.
        */}
        <SurfaceCard variant="info" className="mt-6 p-5">
          <p className="text-sm leading-6 text-[var(--color-muted)]">
            Estamos escribiendo más guías, de a una. Mientras tanto, si no
            sabés por dónde empezar,{" "}
            <Link
              href="/empezar"
              className="font-semibold text-[var(--color-primary)] underline-offset-4 hover:underline"
            >
              hay una página para eso
            </Link>
            .
          </p>
        </SurfaceCard>
      </section>
    </main>
  );
}
