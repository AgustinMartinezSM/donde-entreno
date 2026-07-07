"use client";

import { useRouter } from "next/navigation";
import { AuthGuard } from "../../components/auth/AuthGuard";
import { useAuthSession } from "../../components/auth/AuthSessionProvider";
import { esRolAdmin, esRolPublicador } from "../../lib/authRedirects";
import { AppButton } from "../../components/ui/AppButton";
import { AppLinkButton } from "../../components/ui/AppLinkButton";
import { SectionHeader } from "../../components/ui/SectionHeader";
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
  const rolActual = usuarioVisible?.rol ?? null;

  function manejarCerrarSesion() {
    cerrarSesion();
    router.replace("/");
  }

  return (
    <main className="min-h-screen bg-gradient-to-br from-[#F8FAFC] via-white to-[#E8F6FB] px-4 py-8 text-[var(--color-text)] sm:py-12">
      <section className="mx-auto w-full max-w-5xl">
        <SurfaceCard className="overflow-hidden border-[#BDE8D0] bg-gradient-to-br from-white via-[#F8FCFE] to-[#E6F7EF] p-6 shadow-[0_24px_65px_rgba(12,52,80,0.12)] sm:p-8">
          <div className="grid gap-6 lg:grid-cols-[1fr_0.72fr] lg:items-start">
            <div>
              <SectionHeader
                eyebrow="Cuenta"
                title="Mi cuenta"
                description="Tu espacio personal para revisar los datos principales y moverte rápido por DondeEntreno."
              />
            </div>

            <div className="rounded-[22px] border border-[#BDE8D0] bg-white/80 p-4 text-sm leading-6 text-[#167A4A]">
              <p className="font-extrabold text-[var(--color-primary)]">
                Cuenta activa
              </p>
              <p className="mt-2">
                Desde acá podés explorar actividades, ver ciudades o continuar
                hacia el espacio que corresponda a tu rol.
              </p>
            </div>
          </div>

          {usuarioVisible ? (
            <dl className="mt-8 grid gap-4 sm:grid-cols-2">
              <DatoCuenta etiqueta="Nombre" valor={usuarioVisible.nombre} />
              <DatoCuenta etiqueta="Apellido" valor={usuarioVisible.apellido} />
              <DatoCuenta etiqueta="Email" valor={usuarioVisible.email} />
              <DatoCuenta etiqueta="Rol" valor={formatearRol(usuarioVisible.rol)} />
              <DatoCuenta etiqueta="Estado" valor="Activa" />
            </dl>
          ) : (
            <StatusMessage variant="info" className="mt-8">
              Estamos preparando los datos de tu cuenta.
            </StatusMessage>
          )}

          <div className="mt-8 grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
            <AppLinkButton href="/explorar" variant="primary" fullWidth>
              Explorar actividades
            </AppLinkButton>
            <AppLinkButton href="/ciudades" variant="outline" fullWidth>
              Ver ciudades
            </AppLinkButton>
            <AppLinkButton href={obtenerHrefPrincipal(rolActual)} variant="success" fullWidth>
              {obtenerTextoAccionPrincipal(rolActual)}
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
    <div className="rounded-[18px] border border-[#DDEAF3] bg-white/85 p-4 shadow-[0_10px_24px_rgba(12,52,80,0.05)]">
      <dt className="text-xs font-extrabold uppercase tracking-[0.14em] text-[var(--color-secondary)]">
        {etiqueta}
      </dt>
      <dd className="mt-2 break-words text-sm font-bold text-[var(--color-primary)]">
        {valor}
      </dd>
    </div>
  );
}

function obtenerHrefPrincipal(rol: string | null): string {
  if (rol && esRolPublicador(rol)) {
    return "/publicador";
  }

  if (rol && esRolAdmin(rol)) {
    return "/admin/solicitudes";
  }

  return "/publicar";
}

function obtenerTextoAccionPrincipal(rol: string | null): string {
  if (rol && esRolPublicador(rol)) {
    return "Ir al panel publicador";
  }

  if (rol && esRolAdmin(rol)) {
    return "Ir al panel admin";
  }

  return "Publicar actividad";
}

function formatearRol(rol: string): string {
  return rol
    .toLowerCase()
    .split("_")
    .map((parte) => parte.charAt(0).toUpperCase() + parte.slice(1))
    .join(" ");
}
