"use client";

import { useEffect, useRef, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import {
  AuthApiError,
  hayLogoutRecienteAuth,
  login,
  obtenerSesionAuth,
} from "../../services/authService";
import { obtenerRutaPostLogin } from "../../lib/authRedirects";
import { useAuthSession } from "./AuthSessionProvider";
import { AppButton } from "../ui/AppButton";
import { StatusMessage } from "../ui/StatusMessage";
import type { AuthErroresPorCampo } from "../../types/auth";
import type { FormEvent } from "react";

export function LoginForm() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const {
    status,
    sesion,
    usuario,
    iniciarSesionDesdeRespuesta,
    cerrarSesion,
  } = useAuthSession();
  const esLogout = searchParams.get("logout") === "1";
  const motivoCuenta = searchParams.get("motivo") === "cuenta";
  const logoutReciente = hayLogoutRecienteAuth();
  const sesionPersistida =
    status === "authenticated" ? obtenerSesionAuth() : null;
  const estadoAutenticadoStale =
    status === "authenticated" && sesionPersistida === null;
  const debeEstabilizarLogout =
    esLogout || logoutReciente || estadoAutenticadoStale;
  const returnToSeguro = debeEstabilizarLogout
    ? null
    : obtenerReturnToSeguro(searchParams.get("returnTo"));
  const redireccionAutenticadoRef = useRef(false);
  const limpiezaLogoutRef = useRef(false);
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [mostrarPassword, setMostrarPassword] = useState(false);
  const [cargando, setCargando] = useState(false);
  const [errorGeneral, setErrorGeneral] = useState<string | null>(null);
  const [erroresPorCampo, setErroresPorCampo] =
    useState<AuthErroresPorCampo | null>(null);

  useEffect(() => {
    if (!debeEstabilizarLogout || limpiezaLogoutRef.current) {
      return;
    }

    limpiezaLogoutRef.current = true;
    cerrarSesion();
  }, [cerrarSesion, debeEstabilizarLogout]);

  useEffect(() => {
    if (
      debeEstabilizarLogout ||
      status !== "authenticated" ||
      redireccionAutenticadoRef.current
    ) {
      return;
    }

    const sesionActual = obtenerSesionAuth();

    if (!sesionActual) {
      if (!limpiezaLogoutRef.current) {
        limpiezaLogoutRef.current = true;
        cerrarSesion();
      }
      return;
    }

    const rolActual =
      usuario?.rol ?? sesionActual.usuario.rol ?? sesion?.usuario.rol;

    if (!rolActual) {
      return;
    }

    redireccionAutenticadoRef.current = true;
    router.replace(returnToSeguro ?? obtenerRutaPostLogin(rolActual));
  }, [
    cerrarSesion,
    debeEstabilizarLogout,
    returnToSeguro,
    router,
    sesion,
    status,
    usuario,
  ]);

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
      /*
        Sin returnTo, entrar deja a usuario y publicador en el inicio de
        la app (el espacio de publicador queda a un toque en el menú de
        cuenta); el admin va a su panel. Antes el publicador caía
        SIEMPRE en /publicador, como si loguearse fuera fichar.
      */
      router.replace(
        returnToSeguro ?? obtenerRutaPostLogin(respuesta.usuario.rol)
      );
    } catch (errorLogin: unknown) {
      if (errorLogin instanceof AuthApiError) {
        /*
          Con credenciales rechazadas (401) el mensaje del backend es
          técnico y seco; acá lo decimos como se lo diría una persona,
          sin revelar cuál de los dos datos falló.
        */
        setErrorGeneral(
          errorLogin.status === 401
            ? "Email o contraseña incorrectos. Revisá los datos e intentá de nuevo."
            : errorLogin.message
        );
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
      {motivoCuenta && !esLogout ? (
        <StatusMessage variant="info" role="status">
          <p className="font-bold">
            Para guardar favoritos y marcar Me gusta necesitás tu cuenta.
          </p>
          <p className="mt-1">
            Iniciá sesión o{" "}
            <a
              href="/registro"
              className="font-bold text-[var(--color-primary)] underline underline-offset-2"
            >
              creá tu cuenta gratis
            </a>{" "}
            y seguí donde estabas.
          </p>
        </StatusMessage>
      ) : null}

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
          placeholder="tu@email.com"
          value={email}
          onChange={(evento) => {
            setEmail(evento.target.value);
            setErroresPorCampo(null);
            setErrorGeneral(null);
          }}
          disabled={cargando}
          aria-invalid={Boolean(errorEmail)}
          aria-describedby={errorEmail ? "login-email-error" : undefined}
          className="mt-2 min-h-12 w-full rounded-[18px] border border-[var(--color-border-accent)] bg-[var(--color-bg)] px-4 text-base text-[var(--color-text)] outline-none transition duration-200 ease-out hover:border-[var(--color-accent)] focus:border-[var(--color-accent)] focus:ring-4 focus:ring-[var(--color-border-soft)] disabled:cursor-not-allowed disabled:opacity-70"
        />
        {errorEmail ? (
          <p id="login-email-error" className="mt-2 text-sm font-bold text-[var(--color-danger)]">
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
        {/*
          El ojito vive DENTRO del campo (wrapper relativo + botón
          absoluto) y es un botón real con su nombre accesible. El campo
          reserva pr-14 para que el texto nunca pase por debajo.
        */}
        <div className="relative mt-2">
          <input
            id="login-password"
            name="password"
            type={mostrarPassword ? "text" : "password"}
            autoComplete="current-password"
            placeholder="Tu contraseña"
            value={password}
            onChange={(evento) => {
              setPassword(evento.target.value);
              setErroresPorCampo(null);
              setErrorGeneral(null);
            }}
            disabled={cargando}
            aria-invalid={Boolean(errorPassword)}
            aria-describedby={
              errorPassword ? "login-password-error" : undefined
            }
            className="min-h-12 w-full rounded-[18px] border border-[var(--color-border-accent)] bg-[var(--color-bg)] py-3 pl-4 pr-14 text-base text-[var(--color-text)] outline-none transition duration-200 ease-out hover:border-[var(--color-accent)] focus:border-[var(--color-accent)] focus:ring-4 focus:ring-[var(--color-border-soft)] disabled:cursor-not-allowed disabled:opacity-70"
          />
          <button
            type="button"
            onClick={() => setMostrarPassword((valor) => !valor)}
            aria-label={
              mostrarPassword ? "Ocultar contraseña" : "Mostrar contraseña"
            }
            aria-pressed={mostrarPassword}
            className="absolute right-2 top-1/2 flex h-10 w-10 -translate-y-1/2 items-center justify-center rounded-full text-[var(--color-muted)] transition duration-200 ease-out hover:bg-[var(--color-surface)] hover:text-[var(--color-primary)] focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-[#4FB3D9]/30"
          >
            <IconoOjo tachado={!mostrarPassword} />
          </button>
        </div>
        {errorPassword ? (
          <p
            id="login-password-error"
            className="mt-2 text-sm font-bold text-[var(--color-danger)]"
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

      {(esLogout || logoutReciente || estadoAutenticadoStale) &&
      !errorGeneral ? (
        <StatusMessage variant="success" role="status" className="font-bold">
          Sesión cerrada correctamente.
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

/*
  Ojo abierto = la contraseña se está mostrando; ojo tachado = oculta.
  El estado real lo anuncia el aria-label/aria-pressed del botón.
*/
function IconoOjo({ tachado }: { tachado: boolean }) {
  return (
    <svg
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
      className="h-5 w-5"
      aria-hidden="true"
    >
      <path d="M2.5 12s3.5-6.5 9.5-6.5S21.5 12 21.5 12s-3.5 6.5-9.5 6.5S2.5 12 2.5 12z" />
      <circle cx="12" cy="12" r="2.75" />
      {tachado ? <path d="m4 4 16 16" /> : null}
    </svg>
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
