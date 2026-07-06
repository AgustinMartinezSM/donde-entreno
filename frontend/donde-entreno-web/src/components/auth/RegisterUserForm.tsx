"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { AuthApiError, registrarUsuario } from "../../services/authService";
import { obtenerRutaInicialPorRol } from "../../lib/authRedirects";
import { useAuthSession } from "./AuthSessionProvider";
import { AppButton } from "../ui/AppButton";
import { AppLinkButton } from "../ui/AppLinkButton";
import { StatusMessage } from "../ui/StatusMessage";
import type { AuthErroresPorCampo, RegistroUsuarioRequest } from "../../types/auth";
import type { ChangeEvent, FormEvent } from "react";

type RegistroUsuarioFormState = {
  nombre: string;
  apellido: string;
  email: string;
  telefono: string;
  password: string;
  confirmarPassword: string;
};

const ESTADO_INICIAL: RegistroUsuarioFormState = {
  nombre: "",
  apellido: "",
  email: "",
  telefono: "",
  password: "",
  confirmarPassword: "",
};

const inputClassName =
  "mt-2 min-h-12 w-full rounded-[18px] border border-[#BFDDEA] bg-[#F8FAFC] px-4 text-base text-[var(--color-text)] outline-none transition duration-200 ease-out hover:border-[var(--color-accent)] focus:border-[var(--color-accent)] focus:ring-4 focus:ring-[#DDEAF3] disabled:cursor-not-allowed disabled:opacity-70";

export function RegisterUserForm() {
  const router = useRouter();
  const { status, sesion, usuario, iniciarSesionDesdeRespuesta } =
    useAuthSession();
  const [formulario, setFormulario] =
    useState<RegistroUsuarioFormState>(ESTADO_INICIAL);
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

    router.replace(obtenerRutaInicialPorRol(rolActual));
  }, [router, sesion, status, usuario]);

  function manejarCambio(
    campo: keyof RegistroUsuarioFormState,
    evento: ChangeEvent<HTMLInputElement>
  ) {
    setFormulario((estadoActual) => ({
      ...estadoActual,
      [campo]: evento.target.value,
    }));
    setErroresPorCampo(null);
    setErrorGeneral(null);
  }

  async function manejarEnvio(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault();

    if (cargando) {
      return;
    }

    const erroresLocales = validarFormularioUsuario(formulario);

    if (Object.keys(erroresLocales).length > 0) {
      setErroresPorCampo(erroresLocales);
      setErrorGeneral("Revisá los campos marcados para crear tu cuenta.");
      return;
    }

    setCargando(true);
    setErrorGeneral(null);
    setErroresPorCampo(null);

    try {
      const request: RegistroUsuarioRequest = {
        nombre: formulario.nombre.trim(),
        apellido: formulario.apellido.trim(),
        email: formulario.email.trim().toLowerCase(),
        password: formulario.password,
        confirmarPassword: formulario.confirmarPassword,
        telefono: normalizarTextoOpcional(formulario.telefono),
      };
      const respuesta = await registrarUsuario(request);

      await iniciarSesionDesdeRespuesta(respuesta);
      router.replace(obtenerRutaInicialPorRol(respuesta.usuario.rol));
    } catch (errorRegistro: unknown) {
      if (errorRegistro instanceof AuthApiError) {
        setErrorGeneral(errorRegistro.message);
        setErroresPorCampo(errorRegistro.erroresPorCampo);
        return;
      }

      setErrorGeneral("No pudimos crear tu cuenta. Intentá nuevamente.");
      setErroresPorCampo(null);
    } finally {
      setCargando(false);
    }
  }

  return (
    <form className="mt-8 flex flex-col gap-5" onSubmit={manejarEnvio}>
      <div className="grid gap-5 sm:grid-cols-2">
        <CampoTexto
          id="registro-usuario-nombre"
          label="Nombre"
          value={formulario.nombre}
          onChange={(evento) => manejarCambio("nombre", evento)}
          error={erroresPorCampo?.nombre}
          disabled={cargando}
          autoComplete="given-name"
        />
        <CampoTexto
          id="registro-usuario-apellido"
          label="Apellido"
          value={formulario.apellido}
          onChange={(evento) => manejarCambio("apellido", evento)}
          error={erroresPorCampo?.apellido}
          disabled={cargando}
          autoComplete="family-name"
        />
      </div>

      <CampoTexto
        id="registro-usuario-email"
        label="Email"
        type="email"
        value={formulario.email}
        onChange={(evento) => manejarCambio("email", evento)}
        error={erroresPorCampo?.email}
        disabled={cargando}
        autoComplete="email"
      />

      <CampoTexto
        id="registro-usuario-telefono"
        label="Teléfono opcional"
        type="tel"
        value={formulario.telefono}
        onChange={(evento) => manejarCambio("telefono", evento)}
        error={erroresPorCampo?.telefono}
        disabled={cargando}
        autoComplete="tel"
      />

      <div className="grid gap-5 sm:grid-cols-2">
        <CampoTexto
          id="registro-usuario-password"
          label="Contraseña"
          type="password"
          value={formulario.password}
          onChange={(evento) => manejarCambio("password", evento)}
          error={erroresPorCampo?.password}
          disabled={cargando}
          autoComplete="new-password"
        />
        <CampoTexto
          id="registro-usuario-confirmar-password"
          label="Confirmar contraseña"
          type="password"
          value={formulario.confirmarPassword}
          onChange={(evento) => manejarCambio("confirmarPassword", evento)}
          error={erroresPorCampo?.confirmarPassword}
          disabled={cargando}
          autoComplete="new-password"
        />
      </div>

      {errorGeneral ? (
        <StatusMessage variant="error" role="alert" className="font-bold">
          {errorGeneral}
        </StatusMessage>
      ) : null}

      <AppButton type="submit" disabled={cargando} fullWidth>
        {cargando ? "Creando cuenta..." : "Crear cuenta de usuario"}
      </AppButton>

      <div className="grid gap-3 border-t border-[#DDEAF3] pt-5 text-sm sm:grid-cols-2">
        <AppLinkButton href="/login" variant="secondary" fullWidth>
          Ya tengo cuenta
        </AppLinkButton>
        <AppLinkButton href="/registro/publicador" variant="outline" fullWidth>
          Quiero publicar actividades
        </AppLinkButton>
      </div>
    </form>
  );
}

function CampoTexto({
  id,
  label,
  value,
  onChange,
  error,
  disabled,
  type = "text",
  autoComplete,
}: {
  id: string;
  label: string;
  value: string;
  onChange: (evento: ChangeEvent<HTMLInputElement>) => void;
  error?: string;
  disabled: boolean;
  type?: string;
  autoComplete?: string;
}) {
  const errorId = error ? `${id}-error` : undefined;

  return (
    <div>
      <label htmlFor={id} className="text-sm font-bold text-[var(--color-primary)]">
        {label}
      </label>
      <input
        id={id}
        type={type}
        value={value}
        onChange={onChange}
        disabled={disabled}
        autoComplete={autoComplete}
        aria-invalid={Boolean(error)}
        aria-describedby={errorId}
        className={inputClassName}
      />
      {error ? (
        <p id={errorId} className="mt-2 text-sm font-bold text-red-700">
          {error}
        </p>
      ) : null}
    </div>
  );
}

function validarFormularioUsuario(
  formulario: RegistroUsuarioFormState
): AuthErroresPorCampo {
  const errores: AuthErroresPorCampo = {};
  const email = formulario.email.trim();

  if (!formulario.nombre.trim()) {
    errores.nombre = "Ingresá tu nombre.";
  }

  if (!formulario.apellido.trim()) {
    errores.apellido = "Ingresá tu apellido.";
  }

  if (!email) {
    errores.email = "Ingresá tu email.";
  } else if (!esEmailValido(email)) {
    errores.email = "Ingresá un email válido.";
  }

  if (!formulario.password) {
    errores.password = "Ingresá una contraseña.";
  } else if (formulario.password.length < 8) {
    errores.password = "La contraseña debe tener al menos 8 caracteres.";
  }

  if (!formulario.confirmarPassword) {
    errores.confirmarPassword = "Confirmá tu contraseña.";
  } else if (formulario.password !== formulario.confirmarPassword) {
    errores.confirmarPassword = "Las contraseñas no coinciden.";
  }

  return errores;
}

function esEmailValido(email: string): boolean {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
}

function normalizarTextoOpcional(valor: string): string | null {
  const textoLimpio = valor.trim();

  return textoLimpio ? textoLimpio : null;
}
