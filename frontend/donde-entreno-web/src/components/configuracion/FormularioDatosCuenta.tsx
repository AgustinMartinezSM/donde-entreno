"use client";

import { useState } from "react";
import type { FormEvent } from "react";

import { useAuthSession } from "../auth/AuthSessionProvider";
import {
  UsuarioPerfilApiError,
  actualizarDatosUsuario,
} from "../../services/usuarioPerfilService";
import { AppButton } from "../ui/AppButton";
import { StatusMessage } from "../ui/StatusMessage";

const CLASE_INPUT =
  "mt-1.5 min-h-11 w-full rounded-[14px] border border-[var(--color-border-accent)] bg-[var(--color-bg)] px-3 text-sm text-[var(--color-text)] outline-none transition duration-200 ease-out focus:border-[var(--color-accent)] focus:ring-4 focus:ring-[var(--color-border-soft)] disabled:cursor-not-allowed disabled:opacity-70";

/*
  Edición de datos INLINE en Configuración (Fase 2 social, regla de
  producto del smoke de Fase 1: los ajustes se manejan acá, sin
  mandarte a otra sección). Nombre y apellido editables; el email es
  la credencial de login y queda visible con la explicación.
*/
export function FormularioDatosCuenta() {
  const { usuario } = useAuthSession();

  /*
    Bajo AuthGuard el usuario ya está resuelto cuando esto monta; si
    aún no llegó, se monta al llegar (el key remonta con los valores).
  */
  if (!usuario) {
    return null;
  }

  return <FormularioConDatos key={usuario.id ?? usuario.email} />;
}

function FormularioConDatos() {
  const { usuario, accessToken, actualizarUsuario, cerrarSesion } =
    useAuthSession();

  const [nombre, setNombre] = useState(() => usuario?.nombre ?? "");
  const [apellido, setApellido] = useState(() => usuario?.apellido ?? "");
  const [guardando, setGuardando] = useState(false);
  const [guardado, setGuardado] = useState(false);
  const [error, setError] = useState<string | null>(null);

  if (!usuario) {
    return null;
  }

  const sinCambios =
    nombre.trim() === (usuario.nombre ?? "") &&
    apellido.trim() === (usuario.apellido ?? "");

  async function manejarEnvio(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault();

    if (guardando || !accessToken) {
      return;
    }

    const nombreLimpio = nombre.trim();
    const apellidoLimpio = apellido.trim();

    if (!nombreLimpio || !apellidoLimpio) {
      setError("El nombre y el apellido no pueden quedar vacíos.");
      return;
    }

    setGuardando(true);
    setError(null);
    setGuardado(false);

    try {
      const actualizado = await actualizarDatosUsuario(
        accessToken,
        nombreLimpio,
        apellidoLimpio
      );

      /* El provider propaga el cambio a toda la app (patrón avatar). */
      actualizarUsuario(actualizado);
      setGuardado(true);
    } catch (excepcion: unknown) {
      if (
        excepcion instanceof UsuarioPerfilApiError &&
        excepcion.status === 401
      ) {
        cerrarSesion();
        return;
      }

      setError(
        excepcion instanceof UsuarioPerfilApiError
          ? excepcion.message
          : "No pudimos guardar tus datos. Probá nuevamente."
      );
    } finally {
      setGuardando(false);
    }
  }

  return (
    <form onSubmit={manejarEnvio} className="py-3">
      <div className="grid gap-3 sm:grid-cols-2">
        <label className="block">
          <span className="text-xs font-extrabold uppercase tracking-[0.12em] text-[var(--color-secondary)]">
            Nombre
          </span>
          <input
            type="text"
            value={nombre}
            maxLength={100}
            onChange={(evento) => setNombre(evento.target.value)}
            disabled={guardando}
            className={CLASE_INPUT}
          />
        </label>

        <label className="block">
          <span className="text-xs font-extrabold uppercase tracking-[0.12em] text-[var(--color-secondary)]">
            Apellido
          </span>
          <input
            type="text"
            value={apellido}
            maxLength={100}
            onChange={(evento) => setApellido(evento.target.value)}
            disabled={guardando}
            className={CLASE_INPUT}
          />
        </label>

        <label className="block sm:col-span-2">
          <span className="text-xs font-extrabold uppercase tracking-[0.12em] text-[var(--color-secondary)]">
            Email
          </span>
          <input
            type="email"
            value={usuario.email ?? ""}
            readOnly
            disabled
            className={CLASE_INPUT}
          />
          <span className="mt-1 block text-xs leading-5 text-[var(--color-muted)]">
            El email es tu acceso a la cuenta: por ahora no se puede cambiar
            desde acá. Si lo necesitás, escribinos.
          </span>
        </label>
      </div>

      {error ? (
        <StatusMessage variant="error" role="alert" className="mt-3">
          {error}
        </StatusMessage>
      ) : null}

      {guardado && !error ? (
        <StatusMessage variant="success" role="status" className="mt-3">
          Tus datos quedaron guardados.
        </StatusMessage>
      ) : null}

      <AppButton
        type="submit"
        size="sm"
        className="mt-3"
        disabled={guardando || sinCambios}
      >
        {guardando ? "Guardando..." : "Guardar cambios"}
      </AppButton>
    </form>
  );
}
