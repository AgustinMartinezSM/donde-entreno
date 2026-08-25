"use client";

import Link from "next/link";

import { AuthGuard } from "../../../components/auth/AuthGuard";
import { Header } from "../../../components/layout/Header";
import { BandejaConsultas } from "../../../components/inbox/BandejaConsultas";
import { SectionHeader } from "../../../components/ui/SectionHeader";

/*
  Página propia y no una quinta solapa de /mi-cuenta: las solapas son
  una grilla de cuatro justamente porque a 375px no entran más, y la
  quinta habría empujado a una de ellas fuera de la vista. Se llega
  desde el menú de cuenta, que es la fuente única de esas entradas.
*/
export default function MisConsultasPage() {
  return (
    <AuthGuard>
      <main className="min-h-screen text-[var(--color-text)]">
        <section className="mx-auto w-full max-w-5xl px-4 py-6">
          <Header />

          <div className="mt-6">
            <Link
              href="/mi-cuenta"
              className="text-sm font-bold text-[var(--color-muted)] transition hover:text-[var(--color-primary)]"
            >
              ← Volver a mi perfil
            </Link>
          </div>

          <div className="mt-4">
            <SectionHeader
              eyebrow="Mis consultas"
              title="Lo que preguntaste"
              description="Tus conversaciones con clubes y profes, sin dar tu teléfono. Podés cerrar una consulta cuando quieras: dejan de escribirte."
            />
          </div>

          <BandejaConsultas lado="usuario" />
        </section>
      </main>
    </AuthGuard>
  );
}
