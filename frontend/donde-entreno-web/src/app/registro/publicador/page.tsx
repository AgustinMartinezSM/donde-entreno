import type { Metadata } from "next";
import { AuthHero } from "../../../components/auth/AuthHero";
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
    <main className="min-h-screen px-4 py-8 text-[var(--color-text)] sm:py-12">
      <section className="mx-auto flex min-h-[calc(100vh-4rem)] w-full max-w-7xl items-center justify-center">
        <SurfaceCard className="grid w-full overflow-hidden rounded-[28px] shadow-[0_30px_80px_rgba(12,52,80,0.16)] xl:grid-cols-[0.82fr_1.18fr]">
          <AuthHero
            eyebrow="Publicador"
            titulo={
              <>
                Publicá actividades en <BrandName className="inline" onDark />
              </>
            }
            descripcion="Creá tu perfil para enviar actividades a revisión y gestionarlas desde tu panel."
            puntos={[
              "Perfil público para tu actividad",
              "Solicitudes enviadas a revisión",
              "Panel preparado para gestionar publicaciones",
            ]}
          />

          <section className="decorative-dots relative overflow-hidden p-6 sm:p-8 lg:p-10">
            <div className="relative z-10">
              <p className="text-xs font-extrabold uppercase tracking-[0.18em] text-[var(--color-secondary)]">
                Crear perfil publicador
              </p>
              <h2 className="mt-3 text-2xl font-extrabold text-[var(--color-primary)] sm:text-3xl">
                Datos de acceso y publicación
              </h2>
              <span aria-hidden="true" className="rule-accent mt-3" />
              <p className="mt-3 text-sm leading-6 text-[var(--color-muted)]">
                Usamos estos datos para crear tu cuenta y asociarla a un perfil
                publicador.
              </p>
              <RegisterPublisherForm />
            </div>
          </section>
        </SurfaceCard>
      </section>
    </main>
  );
}
