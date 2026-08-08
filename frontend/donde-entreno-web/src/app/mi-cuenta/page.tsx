"use client";

import { useRouter } from "next/navigation";
import { AuthGuard } from "../../components/auth/AuthGuard";
import { useAuthSession } from "../../components/auth/AuthSessionProvider";
import { Header } from "../../components/layout/Header";
import { esRolAdmin, esRolPublicador } from "../../lib/authRedirects";
import { AppButton } from "../../components/ui/AppButton";
import { AppLinkButton } from "../../components/ui/AppLinkButton";
import { StatusMessage } from "../../components/ui/StatusMessage";
import { SurfaceCard } from "../../components/ui/SurfaceCard";
import { FeedNovedades } from "../../components/cuenta/FeedNovedades";
import { PreferenciasDeportivas } from "../../components/cuenta/PreferenciasDeportivas";
import { PublicadoresSeguidos } from "../../components/cuenta/PublicadoresSeguidos";
import { ResumenGuardados } from "../../components/cuenta/ResumenGuardados";

export default function MiCuentaPage() {
  return (
    <AuthGuard>
      <MiCuentaContenido />
    </AuthGuard>
  );
}

/*
  "Mi espacio deportivo": la página del usuario común prioriza lo vivo
  (novedades de seguidos, guardados, preferencias) y deja los datos
  administrativos de la cuenta en una sección secundaria colapsable.
*/
function MiCuentaContenido() {
  const router = useRouter();
  const { sesion, usuario, cerrarSesion } = useAuthSession();
  const usuarioVisible = usuario ?? sesion?.usuario ?? null;
  const rolActual = usuarioVisible?.rol ?? null;
  const nombre = usuarioVisible?.nombre?.trim() || "";

  function manejarCerrarSesion() {
    cerrarSesion();
    router.replace("/");
  }

  return (
    <main className="min-h-screen bg-[var(--color-bg)] text-[var(--color-text)]">
      <section className="mx-auto w-full max-w-6xl px-4 py-6">
        <Header />

        <div className="py-7 sm:py-9">
          {/* Saludo + accesos principales */}
          <SurfaceCard className="border-[#BDE8D0] bg-gradient-to-br from-white via-[#F8FCFE] to-[#E6F7EF] p-6 sm:p-8">
            <div className="flex flex-col gap-5 sm:flex-row sm:items-center sm:justify-between">
              <div className="min-w-0">
                <p className="text-sm font-bold uppercase tracking-[0.2em] text-[var(--color-secondary)]">
                  Mi perfil deportivo
                </p>
                <h1 className="mt-2 text-3xl font-extrabold leading-tight text-[var(--color-primary)] sm:text-4xl">
                  {nombre ? `Hola, ${nombre}` : "Hola"}
                </h1>
                <p className="mt-2 max-w-xl text-sm leading-6 text-[var(--color-muted)] sm:text-base">
                  Tus guardados, tus deportes y las novedades de quienes seguís,
                  todo en un solo lugar.
                </p>
              </div>

              <div className="flex flex-wrap gap-3 sm:shrink-0 sm:flex-col">
                <AppLinkButton href="/explorar">
                  Explorar actividades
                </AppLinkButton>
                <AppLinkButton href="/favoritos" variant="secondary">
                  Ver mis guardados
                </AppLinkButton>
              </div>
            </div>
          </SurfaceCard>

          {/* Lo social primero: novedades de los publicadores seguidos */}
          <div className="mt-8 sm:mt-10">
            <FeedNovedades />
          </div>

          <div className="mt-8 grid gap-5 lg:grid-cols-[1.1fr_0.9fr]">
            <PublicadoresSeguidos />
            <ResumenGuardados />
          </div>

          <div className="mt-5">
            <PreferenciasDeportivas />
          </div>

          {/* Datos administrativos, al final y colapsados */}
          <SurfaceCard className="mt-8" as="section">
            <details className="group">
              <summary className="flex cursor-pointer list-none items-center justify-between gap-3 rounded-[var(--radius-xl)] p-6 text-lg font-extrabold text-[var(--color-primary)] transition hover:bg-[#F8FCFE] sm:p-7 [&::-webkit-details-marker]:hidden">
                Datos de mi cuenta
                <span
                  aria-hidden="true"
                  className="text-sm text-[var(--color-muted)] transition group-open:rotate-180"
                >
                  ▼
                </span>
              </summary>

              <div className="px-6 pb-6 sm:px-7 sm:pb-7">
                {usuarioVisible ? (
                  <dl className="grid gap-4 sm:grid-cols-2">
                    <DatoCuenta etiqueta="Nombre" valor={usuarioVisible.nombre} />
                    <DatoCuenta etiqueta="Apellido" valor={usuarioVisible.apellido} />
                    <DatoCuenta etiqueta="Email" valor={usuarioVisible.email} />
                    <DatoCuenta etiqueta="Rol" valor={formatearRol(usuarioVisible.rol)} />
                  </dl>
                ) : (
                  <StatusMessage variant="info">
                    Estamos preparando los datos de tu cuenta.
                  </StatusMessage>
                )}

                <div className="mt-6 flex flex-col gap-3 sm:flex-row">
                  <AppLinkButton
                    href={obtenerHrefPrincipal(rolActual)}
                    variant="success"
                  >
                    {obtenerTextoAccionPrincipal(rolActual)}
                  </AppLinkButton>
                  <AppButton
                    type="button"
                    variant="secondary"
                    onClick={manejarCerrarSesion}
                  >
                    Cerrar sesión
                  </AppButton>
                </div>
              </div>
            </details>
          </SurfaceCard>
        </div>
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
    return "Ir a mi perfil de publicador";
  }

  if (rol && esRolAdmin(rol)) {
    return "Ir a administración";
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
