"use client";

import { useEffect, useRef, useState } from "react";
import { usePathname, useRouter } from "next/navigation";
import {
  cerrarSesionAdmin,
  obtenerSesionAdmin,
} from "../../services/authService";
import { AppButton } from "../ui/AppButton";
import { esRolAdmin } from "../../lib/authRedirects";
import type { AdminSesion } from "../../types/auth";
import type { ReactNode } from "react";

type AdminGuardProps = {
  children: ReactNode;
};

export function AdminGuard({ children }: AdminGuardProps) {
  const router = useRouter();
  const pathname = usePathname();
  const [verificando, setVerificando] = useState(true);
  const [sesion, setSesion] = useState<AdminSesion | null>(null);
  const redireccionEnCursoRef = useRef(false);
  const rutaLogin = `/login?returnTo=${encodeURIComponent(
    pathname ?? "/admin/solicitudes"
  )}`;

  useEffect(() => {
    let componenteActivo = true;

    void obtenerSesionAdminCliente().then((sesionActual) => {
      if (!componenteActivo) {
        return;
      }

      if (!sesionActual && !redireccionEnCursoRef.current) {
        redireccionEnCursoRef.current = true;
        router.replace(rutaLogin);
      }

      setSesion(sesionActual);
      setVerificando(false);
    });

    return () => {
      componenteActivo = false;
    };
  }, [router, rutaLogin]);

  function cerrarSesion() {
    cerrarSesionAdmin();
    setSesion(null);
    window.location.replace("/login?logout=1");
  }

  if (verificando) {
    return (
      <main className="min-h-screen bg-gradient-to-br from-[#F8FAFC] via-white to-[#E8F6FB] px-4 py-8 text-[var(--color-text)]">
        <section className="mx-auto flex min-h-[calc(100vh-4rem)] w-full max-w-lg items-center justify-center">
          <div
            role="status"
            className="w-full rounded-[28px] border border-[#DDEAF3] bg-white/95 p-7 text-center shadow-[0_24px_65px_rgba(12,52,80,0.14)]"
          >
            <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-full bg-[#E8F6FB]">
              <div className="h-8 w-8 animate-spin rounded-full border-4 border-[#BFDDEA] border-t-[var(--color-secondary)]" />
            </div>
            <p className="mt-5 text-xs font-extrabold uppercase tracking-[0.18em] text-[var(--color-secondary)]">
              Panel administrador
            </p>
            <h1 className="mt-2 text-2xl font-extrabold text-[var(--color-primary)]">
              Verificando sesión
            </h1>
            <p className="mt-3 text-sm leading-6 text-[var(--color-muted)]">
              Estamos confirmando tu acceso para que puedas continuar con la
              revisión de solicitudes.
            </p>
          </div>
        </section>
      </main>
    );
  }

  if (!sesion) {
    return null;
  }

  if (!esRolAdmin(sesion.usuario.rol)) {
    return (
      <main className="min-h-screen bg-gradient-to-br from-[#F8FAFC] via-white to-[#E8F6FB] px-4 py-8 text-[var(--color-text)]">
        <section className="mx-auto flex min-h-[calc(100vh-4rem)] w-full max-w-lg items-center justify-center">
          <div
            role="alert"
            className="w-full rounded-[28px] border border-[#F6C56D] bg-white/95 p-7 text-center shadow-[0_24px_65px_rgba(12,52,80,0.14)]"
          >
            <p className="text-xs font-extrabold uppercase tracking-[0.18em] text-[var(--color-secondary)]">
              Acceso restringido
            </p>
            <h1 className="mt-3 text-2xl font-extrabold text-[var(--color-primary)]">
              No tenés acceso a esta sección
            </h1>
            <p className="mt-3 text-sm leading-6 text-[var(--color-muted)]">
              Tu cuenta no tiene permisos suficientes para revisar solicitudes
              en el panel administrador.
            </p>
            <AppButton
              type="button"
              onClick={cerrarSesion}
              fullWidth
              className="mt-6"
            >
              Cerrar sesión
            </AppButton>
          </div>
        </section>
      </main>
    );
  }

  return <>{children}</>;
}

function obtenerSesionAdminCliente(): Promise<AdminSesion | null> {
  return Promise.resolve(obtenerSesionAdmin());
}
