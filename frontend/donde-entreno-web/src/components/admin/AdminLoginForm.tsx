"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import {
  AuthApiError,
  guardarSesionAdmin,
  loginAdmin,
  obtenerSesionAdmin,
} from "../../services/authService";
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
    <div className="w-full max-w-md rounded-[var(--radius-xl)] border border-[#DDEAF3] bg-gradient-to-br from-white to-[#F8FCFE] p-6 shadow-[0_22px_55px_rgba(12,52,80,0.12)] sm:p-8">
      <div>
        <p className="text-xs font-extrabold uppercase tracking-[0.18em] text-[var(--color-secondary)]">
          DondeEntreno
        </p>
        <h1 className="mt-3 text-3xl font-extrabold text-[var(--color-primary)]">
          Panel administrador
        </h1>
        <p className="mt-3 text-sm leading-6 text-[var(--color-muted)]">
          Acceso para el equipo de DondeEntreno.
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
            className="mt-2 w-full rounded-[var(--radius-md)] border border-[var(--color-border)] bg-white px-4 py-3 text-base text-[var(--color-text)] outline-none transition duration-200 ease-out hover:border-[#BFDDEA] focus:border-[var(--color-accent)] focus:ring-2 focus:ring-[#DDEAF3] disabled:cursor-not-allowed disabled:opacity-70"
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
            className="mt-2 w-full rounded-[var(--radius-md)] border border-[var(--color-border)] bg-white px-4 py-3 text-base text-[var(--color-text)] outline-none transition duration-200 ease-out hover:border-[#BFDDEA] focus:border-[var(--color-accent)] focus:ring-2 focus:ring-[#DDEAF3] disabled:cursor-not-allowed disabled:opacity-70"
          />
        </div>

        {error && (
          <p
            role="alert"
            className="rounded-[var(--radius-md)] border border-red-200 bg-red-50 px-4 py-3 text-sm font-bold text-red-700 shadow-[0_10px_25px_rgba(127,29,29,0.08)]"
          >
            {error}
          </p>
        )}

        {cargando && (
          <p
            role="status"
            className="text-sm font-bold text-[var(--color-muted)]"
          >
            Verificando credenciales...
          </p>
        )}

        <button
          type="submit"
          disabled={cargando}
          className="rounded-[var(--radius-md)] bg-[var(--color-primary)] px-5 py-3 text-sm font-bold text-white shadow-[var(--shadow-button)] transition duration-200 ease-out hover:-translate-y-0.5 hover:bg-[#0B314D] active:scale-[0.98] disabled:cursor-not-allowed disabled:opacity-70 disabled:hover:translate-y-0"
        >
          {cargando ? "Ingresando..." : "Ingresar al panel"}
        </button>
      </form>
    </div>
  );
}

function obtenerSesionAdminCliente(): Promise<AdminSesion | null> {
  return Promise.resolve(obtenerSesionAdmin());
}
