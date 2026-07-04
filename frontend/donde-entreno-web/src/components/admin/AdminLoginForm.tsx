"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import {
  AuthApiError,
  guardarSesionAdmin,
  loginAdmin,
  obtenerSesionAdmin,
} from "../../services/authService";
import { BrandName } from "../brand/BrandName";
import { AppButton } from "../ui/AppButton";
import { StatusMessage } from "../ui/StatusMessage";
import { SurfaceCard } from "../ui/SurfaceCard";
import type { AdminSesion } from "../../types/auth";
import type { FormEvent } from "react";

export function AdminLoginForm() {
  const router = useRouter();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [cargando, setCargando] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let componenteActivo = true;

    void obtenerSesionAdminCliente().then((sesion) => {
      if (componenteActivo && sesion) {
        router.replace("/admin/solicitudes");
      }
    });

    return () => {
      componenteActivo = false;
    };
  }, [router]);

  async function manejarEnvio(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault();

    if (cargando) {
      return;
    }

    const emailLimpio = email.trim();

    if (!emailLimpio) {
      setError("Ingresá el email del administrador.");
      return;
    }

    if (!password) {
      setError("Ingresá la contraseña del administrador.");
      return;
    }

    setCargando(true);
    setError(null);

    try {
      const respuesta = await loginAdmin({
        email: emailLimpio,
        password,
      });

      guardarSesionAdmin(respuesta);
      setPassword("");
      router.replace("/admin/solicitudes");
    } catch (errorLogin: unknown) {
      if (errorLogin instanceof AuthApiError) {
        setError(errorLogin.message);
      } else {
        setError("No se pudo iniciar sesión en el panel administrador.");
      }
    } finally {
      setCargando(false);
    }
  }

  return (
    <SurfaceCard className="grid w-full max-w-5xl overflow-hidden rounded-[28px] shadow-[0_30px_80px_rgba(12,52,80,0.16)] lg:grid-cols-[0.95fr_1.05fr]">
      <aside className="bg-gradient-to-br from-[#0F3D5E] via-[#145276] to-[#2EB872] p-6 text-white sm:p-8 lg:p-10">
        <p className="text-xs font-extrabold uppercase tracking-[0.22em] text-[#BDE8D0]">
          <BrandName className="inline" onDark />
        </p>
        <h1 className="mt-4 text-3xl font-extrabold leading-tight sm:text-4xl">
          Panel administrador
        </h1>
        <p className="mt-4 max-w-sm text-sm leading-6 text-white/82 sm:text-base">
          Acceso para el equipo de <BrandName className="inline font-bold" onDark />.
        </p>

        <div className="mt-8 grid gap-3">
          {[
            "Revisá solicitudes",
            "Gestioná estados",
            "Publicá actividades aprobadas",
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
            Acceso interno
          </p>
          <h2 className="mt-3 text-2xl font-extrabold text-[var(--color-primary)] sm:text-3xl">
            Ingresá al panel
          </h2>
          <p className="mt-3 text-sm leading-6 text-[var(--color-muted)]">
            Ingresá con tu cuenta administradora para revisar solicitudes y
            cuidar la calidad de las publicaciones.
          </p>
        </div>

        <form className="mt-8 flex flex-col gap-5" onSubmit={manejarEnvio}>
          <div>
            <label
              htmlFor="admin-email"
              className="text-sm font-bold text-[var(--color-primary)]"
            >
              Email
            </label>
            <input
              id="admin-email"
              name="email"
              type="email"
              autoComplete="email"
              value={email}
              onChange={(evento) => setEmail(evento.target.value)}
              disabled={cargando}
              className="mt-2 min-h-12 w-full rounded-[18px] border border-[#BFDDEA] bg-[#F8FAFC] px-4 text-base text-[var(--color-text)] outline-none transition duration-200 ease-out hover:border-[var(--color-accent)] focus:border-[var(--color-accent)] focus:ring-4 focus:ring-[#DDEAF3] disabled:cursor-not-allowed disabled:opacity-70"
            />
          </div>

          <div>
            <label
              htmlFor="admin-password"
              className="text-sm font-bold text-[var(--color-primary)]"
            >
              Contraseña
            </label>
            <input
              id="admin-password"
              name="password"
              type="password"
              autoComplete="current-password"
              value={password}
              onChange={(evento) => setPassword(evento.target.value)}
              disabled={cargando}
              className="mt-2 min-h-12 w-full rounded-[18px] border border-[#BFDDEA] bg-[#F8FAFC] px-4 text-base text-[var(--color-text)] outline-none transition duration-200 ease-out hover:border-[var(--color-accent)] focus:border-[var(--color-accent)] focus:ring-4 focus:ring-[#DDEAF3] disabled:cursor-not-allowed disabled:opacity-70"
            />
          </div>

          {error && (
            <StatusMessage variant="error" className="font-bold">
              {error}
            </StatusMessage>
          )}

          {cargando && (
            <StatusMessage variant="info" className="font-bold">
              Verificando credenciales...
            </StatusMessage>
          )}

          <AppButton
            type="submit"
            disabled={cargando}
            fullWidth
          >
            {cargando ? "Ingresando..." : "Ingresar al panel"}
          </AppButton>
        </form>
      </section>
    </SurfaceCard>
  );
}

function obtenerSesionAdminCliente(): Promise<AdminSesion | null> {
  return Promise.resolve(obtenerSesionAdmin());
}
