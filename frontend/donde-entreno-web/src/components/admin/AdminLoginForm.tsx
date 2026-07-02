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
      setError("Ingresá el password del administrador.");
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
    <div className="grid w-full max-w-5xl overflow-hidden rounded-[28px] border border-[#DDEAF3] bg-white shadow-[0_30px_80px_rgba(12,52,80,0.16)] lg:grid-cols-[0.95fr_1.05fr]">
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
            Usá tus credenciales para revisar y administrar las publicaciones.
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
              Password
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
            <p
              role="alert"
              className="rounded-[18px] border border-red-200 bg-red-50 px-4 py-3 text-sm font-bold leading-6 text-red-700 shadow-[0_10px_25px_rgba(127,29,29,0.08)]"
            >
              {error}
            </p>
          )}

          {cargando && (
            <p
              role="status"
              className="rounded-[18px] border border-[#DDEAF3] bg-[#F8FCFE] px-4 py-3 text-sm font-bold text-[var(--color-primary)]"
            >
              Verificando credenciales...
            </p>
          )}

          <button
            type="submit"
            disabled={cargando}
            className="min-h-12 rounded-[18px] bg-[var(--color-primary)] px-5 py-3 text-sm font-extrabold text-white shadow-[0_16px_38px_rgba(15,61,94,0.22)] transition duration-200 ease-out hover:-translate-y-0.5 hover:bg-[#0B314D] active:scale-[0.98] disabled:cursor-not-allowed disabled:opacity-70 disabled:hover:translate-y-0"
          >
            {cargando ? "Ingresando..." : "Ingresar al panel"}
          </button>
        </form>
      </section>
    </div>
  );
}

function obtenerSesionAdminCliente(): Promise<AdminSesion | null> {
  return Promise.resolve(obtenerSesionAdmin());
}
