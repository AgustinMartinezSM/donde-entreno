import type { Metadata } from "next";
import { AuthHero } from "../../../components/auth/AuthHero";
import { BrandName } from "../../../components/brand/BrandName";
import { RegisterUserForm } from "../../../components/auth/RegisterUserForm";
import { SurfaceCard } from "../../../components/ui/SurfaceCard";

export const metadata: Metadata = {
  title: "Crear cuenta de usuario",
  description:
    "Creá una cuenta de usuario en DondeEntreno para preparar tu perfil y futuras funciones.",
};

export default function RegistroUsuarioPage() {
  return (
    <main className="min-h-screen px-4 py-8 text-[var(--color-text)] sm:py-12">
      <section className="mx-auto flex min-h-[calc(100vh-4rem)] w-full max-w-6xl items-center justify-center">
        <SurfaceCard className="grid w-full max-w-5xl overflow-hidden rounded-[28px] shadow-[0_30px_80px_rgba(12,52,80,0.16)] lg:grid-cols-[0.88fr_1.12fr]">
          <AuthHero
            eyebrow="Usuario"
            titulo={
              <>
                Creá tu cuenta en <BrandName className="inline" onDark />
              </>
            }
            descripcion="Ideal si querés encontrar actividades, guardar tu información y prepararte para nuevas funciones de la plataforma."
            puntos={[
              "Acceso a tu perfil",
              "Datos listos para futuras herramientas",
              "Experiencia más personalizada",
            ]}
          />

          <section className="decorative-dots relative overflow-hidden p-6 sm:p-8 lg:p-10">
            <div className="relative z-10">
              <p className="text-xs font-extrabold uppercase tracking-[0.18em] text-[var(--color-secondary)]">
                Crear cuenta
              </p>
              <h2 className="mt-3 text-2xl font-extrabold text-[var(--color-primary)] sm:text-3xl">
                Datos de usuario
              </h2>
              <span aria-hidden="true" className="rule-accent mt-3" />
              <p className="mt-3 text-sm leading-6 text-[var(--color-muted)]">
                Completá tus datos para crear tu cuenta personal.
              </p>
              <RegisterUserForm />
            </div>
          </section>
        </SurfaceCard>
      </section>
    </main>
  );
}
