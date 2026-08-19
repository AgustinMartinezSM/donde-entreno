import { Suspense } from "react";
import Link from "next/link";
import type { Metadata } from "next";
import type { ReactNode } from "react";
import { AuthHero } from "../../components/auth/AuthHero";
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
    <main className="min-h-screen px-4 py-8 text-[var(--color-text)] sm:py-12">
      <section className="mx-auto flex min-h-[calc(100vh-4rem)] w-full max-w-6xl items-center justify-center">
        <SurfaceCard className="grid w-full max-w-5xl overflow-hidden rounded-[28px] shadow-[0_30px_80px_rgba(12,52,80,0.16)] lg:grid-cols-[0.95fr_1.05fr]">
          <AuthHero
            eyebrow={<BrandName className="inline" onDark />}
            titulo={
              <>
                Ingresá a <BrandName className="inline" onDark />
              </>
            }
            descripcion="Usá tu cuenta para acceder a tu panel, tus solicitudes o tu perfil."
            puntos={[
              "Seguimiento de solicitudes",
              "Gestioná tu cuenta",
              "Acceso seguro al ecosistema",
            ]}
          />

          {/*
            decorative-dots da algo de vida a la columna del formulario,
            que era el rectángulo más blanco y más vacío de la app.
          */}
          <section className="decorative-dots relative overflow-hidden p-6 sm:p-8 lg:p-10">
            <div className="relative z-10">
              <p className="text-xs font-extrabold uppercase tracking-[0.18em] text-[var(--color-secondary)]">
                Acceso
              </p>
              <h2 className="mt-3 text-2xl font-extrabold text-[var(--color-primary)] sm:text-3xl">
                Entrá con tu cuenta
              </h2>
              <span aria-hidden="true" className="rule-accent mt-3" />
              <p className="mt-3 text-sm leading-6 text-[var(--color-muted)]">
                Si ya tenés una cuenta, ingresá con tu email y contraseña.
              </p>

              <Suspense
                fallback={
                  <StatusMessage variant="info" role="status" className="mt-8">
                    Preparando el formulario...
                  </StatusMessage>
                }
              >
                <LoginForm />
              </Suspense>

              <div className="mt-7 grid gap-3 border-t border-[var(--color-border-soft)] pt-5 sm:grid-cols-2">
                <TarjetaCuenta
                  href="/registro"
                  titulo="Crear cuenta"
                  descripcion="Para usar DondeEntreno como persona que busca actividades."
                  tono="info"
                  icono={<IconoPersonaMas />}
                />
                <TarjetaCuenta
                  href="/registro/publicador"
                  titulo="Crear cuenta de publicador"
                  descripcion="Para enviar actividades a revisión y gestionarlas desde tu panel."
                  tono="exito"
                  icono={<IconoMegafono />}
                />
              </div>
            </div>
          </section>
        </SurfaceCard>
      </section>
    </main>
  );
}

type TarjetaCuentaProps = {
  href: string;
  titulo: string;
  descripcion: string;
  tono: "info" | "exito";
  icono: ReactNode;
};

/*
  Los dos accesos a registro eran dos bloques de texto con borde. Con el
  tile de ícono y el chevron se leen como lo que son: dos caminos para
  tocar, no dos notas al pie.
*/
function TarjetaCuenta({
  href,
  titulo,
  descripcion,
  tono,
  icono,
}: TarjetaCuentaProps) {
  const esExito = tono === "exito";

  return (
    <Link
      href={href}
      className={`group flex items-start gap-3 rounded-[20px] border p-4 transition duration-200 ease-out hover:-translate-y-0.5 hover:bg-white hover:shadow-[0_14px_32px_rgba(12,52,80,0.10)] ${
        esExito
          ? "border-[var(--color-success-border)] bg-[var(--color-success-wash)] hover:border-[var(--color-secondary)]"
          : "border-[var(--color-border-accent)] bg-[var(--color-surface-soft)] hover:border-[var(--color-primary)]"
      }`}
    >
      <span
        className={`icon-tile mt-0.5 ${
          esExito ? "text-[var(--color-success)]" : "text-[var(--color-primary)]"
        }`}
      >
        {icono}
      </span>

      <span className="min-w-0 flex-1">
        <span
          className={`block font-extrabold ${
            esExito ? "text-[var(--color-success)]" : "text-[var(--color-primary)]"
          }`}
        >
          {titulo}
        </span>
        <span className="mt-1 block text-xs leading-5 text-[var(--color-muted)]">
          {descripcion}
        </span>
      </span>

      <span
        aria-hidden="true"
        className="mt-0.5 shrink-0 text-lg text-[var(--color-muted)] transition duration-200 ease-out group-hover:translate-x-0.5 group-hover:text-[var(--color-primary)]"
      >
        ›
      </span>
    </Link>
  );
}

const propsIcono = {
  viewBox: "0 0 24 24",
  fill: "none",
  stroke: "currentColor",
  strokeWidth: 2,
  strokeLinecap: "round",
  strokeLinejoin: "round",
  className: "h-5 w-5",
  "aria-hidden": true,
} as const;

function IconoPersonaMas() {
  return (
    <svg {...propsIcono}>
      <circle cx="10" cy="8" r="3.5" />
      <path d="M3.5 20c0-3.3 2.9-5.5 6.5-5.5 1.1 0 2.2.2 3.1.6" />
      <path d="M17.5 14.5v6M14.5 17.5h6" />
    </svg>
  );
}

function IconoMegafono() {
  return (
    <svg {...propsIcono}>
      <path d="M4 10v4a1 1 0 0 0 1 1h2.5l6.5 4V5L7.5 9H5a1 1 0 0 0-1 1Z" />
      <path d="M17.5 9.5a3.5 3.5 0 0 1 0 5" />
    </svg>
  );
}
