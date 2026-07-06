import type { Metadata } from "next";
import { BrandName } from "../../../components/brand/BrandName";
import { RegisterPublisherForm } from "../../../components/auth/RegisterPublisherForm";
import { SurfaceCard } from "../../../components/ui/SurfaceCard";

export const metadata: Metadata = {
  title: "Crear cuenta de publicador",
  description:
    "Creá una cuenta de publicador para enviar actividades deportivas a revisión en DondeEntreno.",
};

export default function RegistroPublicadorPage() {
  return (
    <main className="min-h-screen bg-gradient-to-br from-[#F8FAFC] via-white to-[#E8F6FB] px-4 py-8 text-[var(--color-text)] sm:py-12">
      <section className="mx-auto flex min-h-[calc(100vh-4rem)] w-full max-w-7xl items-center justify-center">
        <SurfaceCard className="grid w-full overflow-hidden rounded-[28px] shadow-[0_30px_80px_rgba(12,52,80,0.16)] xl:grid-cols-[0.82fr_1.18fr]">
          <aside className="bg-gradient-to-br from-[#0F3D5E] via-[#145276] to-[#2EB872] p-6 text-white sm:p-8 lg:p-10">
            <p className="text-xs font-extrabold uppercase tracking-[0.22em] text-[#BDE8D0]">
              Publicador
            </p>
            <h1 className="mt-4 text-3xl font-extrabold leading-tight sm:text-4xl">
              Publicá actividades en <BrandName className="inline" onDark />
            </h1>
            <p className="mt-4 max-w-sm text-sm leading-6 text-white/82 sm:text-base">
              Creá tu perfil para enviar actividades a revisión y gestionarlas
              desde tu panel.
            </p>
            <div className="mt-8 grid gap-3">
              {[
                "Perfil público para tu actividad",
                "Solicitudes enviadas a revisión",
                "Panel preparado para gestionar publicaciones",
              ].map((item) => (
                <div
                  key={item}
                  className="rounded-[18px] border border-white/18 bg-white/12 px-4 py-3 text-sm font-bold backdrop-blur"
                >
                  {item}
                </div>
              ))}
            </div>
          </aside>

          <section className="p-6 sm:p-8 lg:p-10">
            <p className="text-xs font-extrabold uppercase tracking-[0.18em] text-[var(--color-secondary)]">
              Crear perfil publicador
            </p>
            <h2 className="mt-3 text-2xl font-extrabold text-[var(--color-primary)] sm:text-3xl">
              Datos de acceso y publicación
            </h2>
            <p className="mt-3 text-sm leading-6 text-[var(--color-muted)]">
              Usamos estos datos para crear tu cuenta y asociarla a un perfil
              publicador.
            </p>
            <RegisterPublisherForm />
          </section>
        </SurfaceCard>
      </section>
    </main>
  );
}
