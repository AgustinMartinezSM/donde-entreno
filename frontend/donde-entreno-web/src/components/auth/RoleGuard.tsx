"use client";

import { useEffect, useRef } from "react";
import { usePathname, useRouter } from "next/navigation";
import { hayLogoutRecienteAuth } from "../../services/authService";
import { useAuthSession } from "./AuthSessionProvider";
import { AppButton } from "../ui/AppButton";
import { AppLinkButton } from "../ui/AppLinkButton";
import { SurfaceCard } from "../ui/SurfaceCard";
import { StatusMessage } from "../ui/StatusMessage";
import type { RolAuth } from "../../types/auth";
import type { ReactNode } from "react";

type RoleGuardProps = {
  children: ReactNode;
  rolesPermitidos: ReadonlyArray<RolAuth | string>;
  returnTo?: string;
};

export function RoleGuard({
  children,
  rolesPermitidos,
  returnTo,
}: RoleGuardProps) {
  const router = useRouter();
  const pathname = usePathname();
  const { status, sesion, usuario, cerrarSesion } = useAuthSession();
  const rutaRetorno = returnTo ?? pathname ?? "/";
  const redireccionEnCursoRef = useRef(false);
  const rolActual = usuario?.rol ?? sesion?.usuario.rol ?? null;
  const tieneRolPermitido =
    rolActual !== null && rolesPermitidos.includes(rolActual);

  useEffect(() => {
    if (status !== "guest" || redireccionEnCursoRef.current) {
      return;
    }

    redireccionEnCursoRef.current = true;
    router.replace(obtenerRutaLoginProtegida(rutaRetorno));
  }, [router, rutaRetorno, status]);

  function cerrarSesionYRedirigir() {
    cerrarSesion();
    window.location.replace("/login?logout=1");
  }

  if (status === "loading") {
    return (
      <RoleGuardShell>
        <StatusMessage variant="info" role="status" title="Verificando acceso">
          Estamos confirmando tu sesión.
        </StatusMessage>
      </RoleGuardShell>
    );
  }

  if (status === "guest") {
    return (
      <RoleGuardShell>
        <StatusMessage variant="info" role="status" title="Redirigiendo">
          Te estamos llevando al inicio de sesión.
        </StatusMessage>
      </RoleGuardShell>
    );
  }

  if (!tieneRolPermitido) {
    return (
      <RoleGuardShell>
        <StatusMessage
          variant="warning"
          role="alert"
          title="Permisos insuficientes"
        >
          Tu cuenta no tiene permisos para acceder a esta sección.
        </StatusMessage>
        <div className="mt-5 grid gap-3 sm:grid-cols-2">
          <AppLinkButton href="/" variant="secondary" fullWidth>
            Volver al inicio
          </AppLinkButton>
          <AppButton
            type="button"
            variant="outline"
            fullWidth
            onClick={cerrarSesionYRedirigir}
          >
            Cerrar sesión
          </AppButton>
        </div>
      </RoleGuardShell>
    );
  }

  return <>{children}</>;
}

function obtenerRutaLoginProtegida(rutaRetorno: string): string {
  if (hayLogoutRecienteAuth()) {
    return "/login?logout=1";
  }

  return `/login?returnTo=${encodeURIComponent(rutaRetorno)}`;
}

function RoleGuardShell({ children }: { children: ReactNode }) {
  return (
    <main className="min-h-screen bg-gradient-to-br from-[#F8FAFC] via-white to-[#E8F6FB] px-4 py-8 text-[var(--color-text)]">
      <section className="mx-auto flex min-h-[calc(100vh-4rem)] w-full max-w-lg items-center justify-center">
        <SurfaceCard className="w-full p-6 sm:p-7">{children}</SurfaceCard>
      </section>
    </main>
  );
}
