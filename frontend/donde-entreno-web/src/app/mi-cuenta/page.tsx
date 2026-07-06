"use client";

import { useRouter } from "next/navigation";
import { AuthGuard } from "../../components/auth/AuthGuard";
import { useAuthSession } from "../../components/auth/AuthSessionProvider";
import { AppButton } from "../../components/ui/AppButton";
import { AppLinkButton } from "../../components/ui/AppLinkButton";
import { SurfaceCard } from "../../components/ui/SurfaceCard";
import { StatusMessage } from "../../components/ui/StatusMessage";

export default function MiCuentaPage() {
  return (
    <AuthGuard>
      <MiCuentaContenido />
    </AuthGuard>
  );
}

function MiCuentaContenido() {
  const router = useRouter();
  const { sesion, usuario, cerrarSesion } = useAuthSession();
  const usuarioVisible = usuario ?? sesion?.usuario ?? null;

  function manejarCerrarSesion() {
    cerrarSesion();
    router.replace("/");
  }

  return (
    <main className="min-h-screen bg-gradient-to-br from-[#F8FAFC] via-white to-[#E8F6FB] px-4 py-8 text-[var(--color-text)] sm:py-12">
      <section className="mx-auto w-full max-w-4xl">
        <SurfaceCard className="p-6 sm:p-8">
          <p className="text-xs font-extrabold uppercase tracking-[0.18em] text-[var(--color-secondary)]">
            Cuenta
          </p>
          <h1 className="mt-3 text-3xl font-extrabold text-[var(--color-primary)] sm:text-4xl">
            Mi cuenta
          </h1>
          <p className="mt-3 max-w-2xl text-sm leading-6 text-[var(--color-muted)] sm:text-base">
            Acá vas a encontrar la información principal de tu cuenta.
          </p>

          {usuarioVisible ? (
            <dl className="mt-8 grid gap-4 sm:grid-cols-2">
              <DatoCuenta etiqueta="Nombre" valor={usuarioVisible.nombre} />
              <DatoCuenta etiqueta="Apellido" valor={usuarioVisible.apellido} />
              <DatoCuenta etiqueta="Email" valor={usuarioVisible.email} />
              <DatoCuenta etiqueta="Rol" valor={usuarioVisible.rol} />
            </dl>
          ) : (
            <StatusMessage variant="info" className="mt-8">
              Estamos preparando los datos de tu cuenta.
            </StatusMessage>
          )}

          <div className="mt-8 grid gap-3 sm:grid-cols-3">
            <AppLinkButton href="/" variant="secondary" fullWidth>
              Ir al inicio
            </AppLinkButton>
            <AppLinkButton href="/explorar" variant="outline" fullWidth>
              Explorar actividades
            </AppLinkButton>
            <AppButton
              type="button"
              variant="secondary"
              fullWidth
              onClick={manejarCerrarSesion}
            >
              Cerrar sesión
            </AppButton>
          </div>
        </SurfaceCard>
      </section>
    </main>
  );
}

function DatoCuenta({ etiqueta, valor }: { etiqueta: string; valor: string }) {
  return (
    <div className="rounded-[18px] border border-[#DDEAF3] bg-white/75 p-4">
      <dt className="text-xs font-extrabold uppercase tracking-[0.14em] text-[var(--color-secondary)]">
        {etiqueta}
      </dt>
      <dd className="mt-2 break-words text-sm font-bold text-[var(--color-primary)]">
        {valor}
      </dd>
    </div>
  );
}
