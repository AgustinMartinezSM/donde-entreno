"use client";

import { useEffect, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { AuthApiError, login } from "../../services/authService";
import { obtenerRutaInicialPorRol } from "../../lib/authRedirects";
import { useAuthSession } from "./AuthSessionProvider";
import { AppButton } from "../ui/AppButton";
import { StatusMessage } from "../ui/StatusMessage";
import type { AuthErroresPorCampo } from "../../types/auth";
import type { FormEvent } from "react";

export function LoginForm() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const returnToSeguro = obtenerReturnToSeguro(searchParams.get("returnTo"));
  const {
    status,
    sesion,
    usuario,
    iniciarSesionDesdeRespuesta,
  } = useAuthSession();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [cargando, setCargando] = useState(false);
  const [errorGeneral, setErrorGeneral] = useState<string | null>(null);
  const [erroresPorCampo, setErroresPorCampo] =
    useState<AuthErroresPorCampo | null>(null);

  useEffect(() => {
    if (status !== "authenticated") {
      return;
    }

    const rolActual = usuario?.rol ?? sesion?.usuario.rol;

    if (!rolActual) {
      return;
    }

    router.replace(returnToSeguro ?? obtenerRutaInicialPorRol(rolActual));
  }, [returnToSeguro, router, sesion, status, usuario]);

  async function manejarEnvio(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault();

    if (cargando) {
      return;
    }

    const emailLimpio = email.trim();

    if (!emailLimpio) {
      setErroresPorCampo({
        email: "Ingresá tu email.",
      });
      setErrorGeneral(null);
      return;
    }

    if (!password) {
      setErroresPorCampo({
        password: "Ingresá tu contraseña.",
      });
      setErrorGeneral(null);
      return;
    }

    setCargando(true);
    setErrorGeneral(null);
    setErroresPorCampo(null);

    try {
      const respuesta = await login({
        email: emailLimpio,
        password,
      });

      await iniciarSesionDesdeRespuesta(respuesta);
      setPassword("");
      router.replace(
        returnToSeguro ?? obtenerRutaInicialPorRol(respuesta.usuario.rol)
      );
    } catch (errorLogin: unknown) {
      if (errorLogin instanceof AuthApiError) {
        setErrorGeneral(errorLogin.message);
        setErroresPorCampo(errorLogin.erroresPorCampo);
        return;
      }

      setErrorGeneral("No pudimos iniciar sesión. Intentá nuevamente.");
      setErroresPorCampo(null);
    } finally {
      setCargando(false);
    }
  }

  const errorEmail = erroresPorCampo?.email ?? null;
  const errorPassword = erroresPorCampo?.password ?? null;
  const erroresRestantes = Object.entries(erroresPorCampo ?? {}).filter(
    ([campo]) => campo !== "email" && campo !== "password"
  );

  return (
    <form className="mt-8 flex flex-col gap-5" onSubmit={manejarEnvio}>
      <div>
        <label
          htmlFor="login-email"
          className="text-sm font-bold text-[var(--color-primary)]"
        >
          Email
        </label>
        <input
          id="login-email"
          name="email"
          type="email"
          autoComplete="email"
          value={email}
          onChange={(evento) => {
            setEmail(evento.target.value);
            setErroresPorCampo(null);
            setErrorGeneral(null);
          }}
          disabled={cargando}
          aria-invalid={Boolean(errorEmail)}
          aria-describedby={errorEmail ? "login-email-error" : undefined}
          className="mt-2 min-h-12 w-full rounded-[18px] border border-[#BFDDEA] bg-[#F8FAFC] px-4 text-base text-[var(--color-text)] outline-none transition duration-200 ease-out hover:border-[var(--color-accent)] focus:border-[var(--color-accent)] focus:ring-4 focus:ring-[#DDEAF3] disabled:cursor-not-allowed disabled:opacity-70"
        />
        {errorEmail ? (
          <p id="login-email-error" className="mt-2 text-sm font-bold text-red-700">
            {errorEmail}
          </p>
        ) : null}
      </div>

      <div>
        <label
          htmlFor="login-password"
          className="text-sm font-bold text-[var(--color-primary)]"
        >
          Contraseña
        </label>
        <input
          id="login-password"
          name="password"
          type="password"
          autoComplete="current-password"
          value={password}
          onChange={(evento) => {
            setPassword(evento.target.value);
            setErroresPorCampo(null);
            setErrorGeneral(null);
          }}
          disabled={cargando}
          aria-invalid={Boolean(errorPassword)}
          aria-describedby={errorPassword ? "login-password-error" : undefined}
          className="mt-2 min-h-12 w-full rounded-[18px] border border-[#BFDDEA] bg-[#F8FAFC] px-4 text-base text-[var(--color-text)] outline-none transition duration-200 ease-out hover:border-[var(--color-accent)] focus:border-[var(--color-accent)] focus:ring-4 focus:ring-[#DDEAF3] disabled:cursor-not-allowed disabled:opacity-70"
        />
        {errorPassword ? (
          <p
            id="login-password-error"
            className="mt-2 text-sm font-bold text-red-700"
          >
            {errorPassword}
          </p>
        ) : null}
      </div>

      {errorGeneral ? (
        <StatusMessage variant="error" role="alert" className="font-bold">
          {errorGeneral}
        </StatusMessage>
      ) : null}

      {erroresRestantes.length > 0 ? (
        <StatusMessage variant="error" role="alert">
          <ul className="list-inside list-disc">
            {erroresRestantes.map(([campo, mensaje]) => (
              <li key={campo}>{mensaje}</li>
            ))}
          </ul>
        </StatusMessage>
      ) : null}

      <AppButton type="submit" disabled={cargando} fullWidth>
        {cargando ? "Ingresando..." : "Ingresar"}
      </AppButton>
    </form>
  );
}

function obtenerReturnToSeguro(returnTo: string | null): string | null {
  if (!returnTo) {
    return null;
  }

  if (!returnTo.startsWith("/") || returnTo.startsWith("//")) {
    return null;
  }

  if (returnTo.startsWith("/login") || returnTo.startsWith("/admin/login")) {
    return null;
  }

  return returnTo;
}
