"use client";

import { PublicadorGuard } from "../../components/auth/PublicadorGuard";
import { AppLinkButton } from "../../components/ui/AppLinkButton";
import { SurfaceCard } from "../../components/ui/SurfaceCard";
import { StatusMessage } from "../../components/ui/StatusMessage";

export default function PublicadorPage() {
  return (
    <PublicadorGuard>
      <main className="min-h-screen bg-gradient-to-br from-[#F8FAFC] via-white to-[#E8F6FB] px-4 py-8 text-[var(--color-text)] sm:py-12">
        <section className="mx-auto w-full max-w-4xl">
          <SurfaceCard className="p-6 sm:p-8">
            <p className="text-xs font-extrabold uppercase tracking-[0.18em] text-[var(--color-secondary)]">
              Publicador
            </p>
            <h1 className="mt-3 text-3xl font-extrabold text-[var(--color-primary)] sm:text-4xl">
              Panel publicador
            </h1>
            <p className="mt-3 max-w-2xl text-sm leading-6 text-[var(--color-muted)] sm:text-base">
              Gestioná tus solicitudes y actividades publicadas.
            </p>

            <StatusMessage variant="info" className="mt-8">
              Pronto vas a poder revisar tus solicitudes y crear nuevas
              publicaciones desde este panel.
            </StatusMessage>

            <div className="mt-8 grid gap-3 sm:grid-cols-2">
              <AppLinkButton
                href="/publicador/solicitudes"
                variant="secondary"
                fullWidth
              >
                Mis solicitudes
              </AppLinkButton>
              <AppLinkButton
                href="/publicador/solicitudes/nueva"
                variant="success"
                fullWidth
              >
                Nueva solicitud
              </AppLinkButton>
            </div>
          </SurfaceCard>
        </section>
      </main>
    </PublicadorGuard>
  );
}
