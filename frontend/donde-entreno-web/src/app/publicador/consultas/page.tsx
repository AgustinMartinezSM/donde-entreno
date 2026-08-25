"use client";

import { PublicadorGuard } from "../../../components/auth/PublicadorGuard";
import { BandejaConsultas } from "../../../components/inbox/BandejaConsultas";
import { PublicadorPageHeader } from "../../../components/publicador/PublicadorPageHeader";

export default function ConsultasDelPublicadorPage() {
  return (
    <PublicadorGuard>
      <main className="min-h-screen text-[var(--color-text)]">
        <section className="mx-auto w-full max-w-5xl px-4 py-6">
          <PublicadorPageHeader
            title="Consultas"
            description="Las preguntas que te hacen desde la plataforma, sin que nadie tenga que dar su teléfono. Responder rápido es lo que más ayuda a que alguien se decida."
          />

          <BandejaConsultas lado="publicador" />
        </section>
      </main>
    </PublicadorGuard>
  );
}
