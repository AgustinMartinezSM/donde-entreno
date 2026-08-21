"use client";

import Image from "next/image";
import { useEffect, useRef, useState } from "react";

import {
  AuthApiError,
  actualizarAvatarUsuario,
  eliminarAvatarUsuario,
} from "../../services/authService";
import type { UsuarioActual } from "../../types/auth";
import { useAuthSession } from "../auth/AuthSessionProvider";
import { EditorRecorteImagen } from "../imagenes/EditorRecorteImagen";
import { AppButton } from "../ui/AppButton";
import { StatusMessage } from "../ui/StatusMessage";

/*
  Foto de perfil del usuario (fase 5d).

  Mismo patrón que el resto de los modales: <dialog> nativo. La foto se
  recorta 1:1 en el cliente con el editor de encuadre de fase 2 (preset
  LOGO, 400×400) y sube ya recortada. Sin moderación a propósito: el
  avatar no tiene superficie pública — solo lo ve su dueño.
*/
export function DialogoAvatar({
  abierto,
  avatarUrl,
  iniciales,
  onCerrar,
  onUsuarioActualizado,
}: {
  abierto: boolean;
  avatarUrl: string | null;
  iniciales: string;
  onCerrar: () => void;
  onUsuarioActualizado: (usuario: UsuarioActual) => void;
}) {
  const dialogoRef = useRef<HTMLDialogElement | null>(null);
  const { accessToken } = useAuthSession();

  const [archivo, setArchivo] = useState<File | null>(null);
  const [urlArchivo, setUrlArchivo] = useState<string | null>(null);
  const [cargando, setCargando] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [exito, setExito] = useState<string | null>(null);

  useEffect(() => {
    const dialogo = dialogoRef.current;

    if (!dialogo) {
      return;
    }

    if (abierto && !dialogo.open) {
      dialogo.showModal();
    } else if (!abierto && dialogo.open) {
      dialogo.close();
    }
  }, [abierto]);

  /* El object URL se libera siempre: es memoria del navegador. */
  useEffect(() => {
    return () => {
      if (urlArchivo) {
        URL.revokeObjectURL(urlArchivo);
      }
    };
  }, [urlArchivo]);

  function limpiarSeleccion() {
    if (urlArchivo) {
      URL.revokeObjectURL(urlArchivo);
    }

    setArchivo(null);
    setUrlArchivo(null);
  }

  function cerrarYLimpiar() {
    limpiarSeleccion();
    setError(null);
    setExito(null);
    setCargando(false);
    onCerrar();
  }

  function elegirArchivo(evento: React.ChangeEvent<HTMLInputElement>) {
    const elegido = evento.target.files?.[0] ?? null;
    evento.target.value = "";

    if (!elegido) {
      return;
    }

    limpiarSeleccion();
    setError(null);
    setExito(null);
    setArchivo(elegido);
    setUrlArchivo(URL.createObjectURL(elegido));
  }

  async function subirRecortada(recortada: File) {
    if (!accessToken || cargando) {
      return;
    }

    setCargando(true);
    setError(null);

    try {
      const usuario = await actualizarAvatarUsuario(accessToken, recortada);
      onUsuarioActualizado(usuario);
      limpiarSeleccion();
      setExito("Foto actualizada.");
    } catch (excepcion) {
      /*
        Volver a la vista inicial: el mensaje de error vive ahí — si el
        editor quedara montado, el fallo seria invisible (cazado en la
        verificacion local con el backend apagado).
      */
      limpiarSeleccion();
      setError(humanizarError(excepcion, "No pudimos subir la foto."));
    } finally {
      setCargando(false);
    }
  }

  async function quitarFoto() {
    if (!accessToken || cargando) {
      return;
    }

    setCargando(true);
    setError(null);

    try {
      const usuario = await eliminarAvatarUsuario(accessToken);
      onUsuarioActualizado(usuario);
      setExito("Foto quitada: volvés a tus iniciales.");
    } catch (excepcion) {
      setError(humanizarError(excepcion, "No pudimos quitar la foto."));
    } finally {
      setCargando(false);
    }
  }

  return (
    <dialog
      ref={dialogoRef}
      onClose={cerrarYLimpiar}
      aria-labelledby="avatar-titulo"
      className="w-[min(30rem,calc(100vw-2rem))] rounded-[var(--radius-xl)] border border-[var(--color-border-soft)] bg-[var(--color-surface)] p-0 text-[var(--color-text)] shadow-[0_24px_60px_rgba(12,52,80,0.28)] backdrop:bg-[#0F3D5E]/40 backdrop:backdrop-blur-sm"
    >
      <div className="flex items-start justify-between gap-4 border-b border-[var(--color-divisor)] px-5 py-4">
        <h2
          id="avatar-titulo"
          className="text-lg font-extrabold text-[var(--color-primary)]"
        >
          Foto de perfil
        </h2>
        <button
          type="button"
          onClick={cerrarYLimpiar}
          aria-label="Cerrar foto de perfil"
          className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full text-[var(--color-muted)] transition duration-200 ease-out hover:bg-[var(--color-bg)] hover:text-[var(--color-primary)] active:scale-95"
        >
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
            <path d="M18 6 6 18" />
            <path d="m6 6 12 12" />
          </svg>
        </button>
      </div>

      <div className="px-5 py-5">
        {archivo && urlArchivo ? (
          /* Encuadre 1:1: lo que se ve en el marco es lo que se publica. */
          <EditorRecorteImagen
            archivo={archivo}
            url={urlArchivo}
            tipo="LOGO"
            onConfirmar={subirRecortada}
            onCancelar={limpiarSeleccion}
          />
        ) : (
          <>
            <div className="flex items-center gap-4">
              <span className="relative flex h-20 w-20 shrink-0 items-center justify-center overflow-hidden rounded-full bg-[var(--color-brand)] text-xl font-extrabold tracking-[0.08em] text-white">
                {avatarUrl ? (
                  <Image
                    src={avatarUrl}
                    alt="Tu foto de perfil actual"
                    fill
                    sizes="80px"
                    className="object-cover"
                  />
                ) : (
                  iniciales
                )}
              </span>

              <p className="text-sm leading-6 text-[var(--color-muted)]">
                Tu foto solo se muestra en tu propio espacio. Formatos JPG,
                PNG o WebP, hasta 2 MB; la encuadrás vos antes de subirla.
              </p>
            </div>

            <div className="mt-5 grid gap-3 sm:grid-cols-2">
              {/* El input real es invisible: el label hace de botón. */}
              <label className="inline-flex min-h-12 cursor-pointer items-center justify-center rounded-[18px] bg-[var(--color-cta)] px-5 text-base font-extrabold text-white transition duration-200 ease-out hover:brightness-110 active:scale-[0.98]">
                {avatarUrl ? "Cambiar foto" : "Elegir foto"}
                <input
                  type="file"
                  accept="image/jpeg,image/png,image/webp"
                  className="sr-only"
                  onChange={elegirArchivo}
                  disabled={cargando}
                />
              </label>

              {avatarUrl ? (
                <AppButton
                  variant="outline"
                  onClick={quitarFoto}
                  disabled={cargando}
                >
                  {cargando ? "Quitando..." : "Quitar foto"}
                </AppButton>
              ) : null}
            </div>

            {exito ? (
              <StatusMessage
                variant="success"
                role="status"
                className="mt-4 font-bold"
              >
                {exito}
              </StatusMessage>
            ) : null}

            {error ? (
              <StatusMessage
                variant="error"
                role="alert"
                className="mt-4 font-bold"
              >
                {error}
              </StatusMessage>
            ) : null}
          </>
        )}
      </div>
    </dialog>
  );
}

function humanizarError(excepcion: unknown, fallback: string): string {
  if (excepcion instanceof AuthApiError) {
    if (excepcion.status === 401) {
      return "Tu sesión expiró. Recargá la página y volvé a intentar.";
    }

    if (excepcion.message) {
      return excepcion.message;
    }
  }

  return fallback;
}
