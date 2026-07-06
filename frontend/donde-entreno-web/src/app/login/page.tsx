import { Suspense } from "react";
import Link from "next/link";
import type { Metadata } from "next";
import { BrandName } from "../../components/brand/BrandName";
import { LoginForm } from "../../components/auth/LoginForm";
import { SurfaceCard } from "../../components/ui/SurfaceCard";
import { StatusMessage } from "../../components/ui/StatusMessage";

export const metadata: Metadata = {
  title: "Ingresar",
  description:
    "Ingresá a DondeEntreno para acceder a tu panel, tus solicitudes o tu perfil.",
};

export default function LoginPage() {
  return (
    <main className="min-h-screen bg-gradient-to-br from-[#F8FAFC] via-white to-[#E8F6FB] px-4 py-8 text-[var(--color-text)] sm:py-12">
      <section className="mx-auto flex min-h-[calc(100vh-4rem)] w-full max-w-6xl items-center justify-center">
        <SurfaceCard className="grid w-full max-w-5xl overflow-hidden rounded-[28px] shadow-[0_30px_80px_rgba(12,52,80,0.16)] lg:grid-cols-[0.95fr_1.05fr]">
          <aside className="bg-gradient-to-br from-[#0F3D5E] via-[#145276] to-[#2EB872] p-6 text-white sm:p-8 lg:p-10">
            <p className="text-xs font-extrabold uppercase tracking-[0.22em] text-[#BDE8D0]">
              <BrandName className="inline" onDark />
            </p>
            <h1 className="mt-4 text-3xl font-extrabold leading-tight sm:text-4xl">
              Ingresá a <BrandName className="inline" onDark />
            </h1>
            <p className="mt-4 max-w-sm text-sm leading-6 text-white/82 sm:text-base">
              Usá tu cuenta para acceder a tu panel, tus solicitudes o tu
              perfil.
            </p>

            <div className="mt-8 grid gap-3">
              {[
                "Seguimiento de solicitudes",
                "Paneles según tu rol",
                "Acceso seguro al ecosistema",
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
            <div>
              <p className="text-xs font-extrabold uppercase tracking-[0.18em] text-[var(--color-secondary)]">
                Acceso
              </p>
              <h2 className="mt-3 text-2xl font-extrabold text-[var(--color-primary)] sm:text-3xl">
                Entrá con tu cuenta
              </h2>
              <p className="mt-3 text-sm leading-6 text-[var(--color-muted)]">
                Si ya tenés una cuenta, ingresá con tu email y contraseña.
              </p>
            </div>

            <Suspense
              fallback={
                <StatusMessage variant="info" role="status" className="mt-8">
                  Preparando el formulario...
                </StatusMessage>
              }
            >
              <LoginForm />
            </Suspense>

            <div className="mt-7 grid gap-3 border-t border-[#DDEAF3] pt-5 text-sm text-[var(--color-muted)] sm:grid-cols-2">
              <Link
                href="/registro"
                className="font-extrabold text-[var(--color-primary)] transition hover:text-[var(--color-secondary)]"
              >
                Crear cuenta
              </Link>
              <Link
                href="/registro/publicador"
                className="font-extrabold text-[var(--color-primary)] transition hover:text-[var(--color-secondary)] sm:text-right"
              >
                Crear cuenta de publicador
              </Link>
            </div>
          </section>
        </SurfaceCard>
      </section>
    </main>
  );
}
