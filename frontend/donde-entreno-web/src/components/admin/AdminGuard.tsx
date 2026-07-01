"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import {
  cerrarSesionAdmin,
  obtenerSesionAdmin,
} from "../../services/authService";
import type { AdminSesion } from "../../types/auth";
import type { ReactNode } from "react";

type AdminGuardProps = {
  children: ReactNode;
};

export function AdminGuard({ children }: AdminGuardProps) {
  const router = useRouter();
  const [verificando, setVerificando] = useState(true);
  const [sesion, setSesion] = useState<AdminSesion | null>(null);

  useEffect(() => {
    let componenteActivo = true;

    void obtenerSesionAdminCliente().then((sesionActual) => {
      if (!componenteActivo) {
        return;
      }

      if (!sesionActual) {
        router.replace("/admin/login");
      }

      setSesion(sesionActual);
      setVerificando(false);
    });

    return () => {
      componenteActivo = false;
    };
  }, [router]);

  function cerrarSesion() {
    cerrarSesionAdmin();
    setSesion(null);
    router.replace("/admin/login");
  }

  if (verificando) {
    return (
      <main className="min-h-screen bg-[var(--color-bg)] px-4 py-8 text-[var(--color-text)]">
        <section className="mx-auto flex min-h-[calc(100vh-4rem)] w-full max-w-md items-center justify-center">
          <div
            role="status"
            className="w-full rounded-[var(--radius-xl)] border border-[var(--color-border)] bg-[var(--color-surface)] p-6 text-center shadow-[var(--shadow-card)]"
          >
            <div className="mx-auto h-9 w-9 animate-spin rounded-full border-4 border-[#DDEAF3] border-t-[var(--color-secondary)]" />
            <p className="mt-4 text-sm font-bold text-[var(--color-primary)]">
              Verificando sesión...
            </p>
          </div>
        </section>
      </main>
    );
  }

  if (!sesion) {
    return null;
  }

  if (sesion.usuario.rol !== "SUPER_ADMIN") {
    return (
      <main className="min-h-screen bg-[var(--color-bg)] px-4 py-8 text-[var(--color-text)]">
        <section className="mx-auto flex min-h-[calc(100vh-4rem)] w-full max-w-md items-center justify-center">
          <div
            role="alert"
            className="w-full rounded-[var(--radius-xl)] border border-[var(--color-border)] bg-[var(--color-surface)] p-6 text-center shadow-[var(--shadow-card)]"
          >
            <p className="text-xs font-extrabold uppercase tracking-[0.18em] text-[var(--color-secondary)]">
              Panel admin
            </p>
            <h1 className="mt-3 text-2xl font-extrabold text-[var(--color-primary)]">
              Permisos insuficientes
            </h1>
            <p className="mt-3 text-sm leading-6 text-[var(--color-muted)]">
              Tu usuario no tiene permisos para acceder al panel admin local.
            </p>
            <button
              type="button"
              onClick={cerrarSesion}
              className="mt-6 rounded-[var(--radius-md)] bg-[var(--color-primary)] px-5 py-3 text-sm font-bold text-white shadow-[var(--shadow-button)]"
            >
              Cerrar sesión
            </button>
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
