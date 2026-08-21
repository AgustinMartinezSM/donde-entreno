"use client";

import { useEffect, useId, useRef, useState } from "react";

import { AuthApiError, cambiarPassword } from "../../services/authService";
import { useAuthSession } from "../auth/AuthSessionProvider";
import { AppButton } from "../ui/AppButton";
import { StatusMessage } from "../ui/StatusMessage";

/*
  Cambio de contraseña con sesión activa (fase 5a).

  Mismo patrón que el resto de los modales: <dialog> nativo con
  showModal(). El backend revoca todas las sesiones del usuario y
  devuelve una sesión nueva; acá se persiste con
  iniciarSesionDesdeRespuesta — el único camino que ya persiste sesiones
  — así este dispositivo queda adentro y los demás afuera.
*/
export function DialogoCambiarPassword({
  abierto,
  onCerrar,
}: {
  abierto: boolean;
  onCerrar: () => void;
}) {
  const dialogoRef = useRef<HTMLDialogElement | null>(null);
  const { accessToken, iniciarSesionDesdeRespuesta } = useAuthSession();

  const [passwordActual, setPasswordActual] = useState("");
  const [passwordNueva, setPasswordNueva] = useState("");
  const [confirmarPassword, setConfirmarPassword] = useState("");
  const [cargando, setCargando] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [exito, setExito] = useState(false);
  /*
    Cerrar remonta el form (via key): sin esto, un ojito dejado en
    "mostrar" reabria el campo con la contraseña visible.
  */
  const [generacion, setGeneracion] = useState(0);

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

  /* Cerrar siempre limpia: las contraseñas no quedan en memoria del form. */
  function cerrarYLimpiar() {
    setPasswordActual("");
    setPasswordNueva("");
    setConfirmarPassword("");
    setError(null);
    setExito(false);
    setCargando(false);
    setGeneracion((actual) => actual + 1);
    onCerrar();
  }

  async function enviar(evento: React.FormEvent<HTMLFormElement>) {
    evento.preventDefault();

    if (!accessToken || cargando) {
      return;
    }

    /* Chequeos rápidos en el cliente; la política real vive en el backend. */
    if (passwordNueva !== confirmarPassword) {
      setError("La contraseña nueva y su confirmación no coinciden.");
      return;
    }

    if (
      passwordNueva.length < 8 ||
      !/[a-zA-Z]/.test(passwordNueva) ||
      !/[0-9]/.test(passwordNueva)
    ) {
      setError(
        "La contraseña nueva necesita al menos 8 caracteres, con una letra y un número."
      );
      return;
    }

    setCargando(true);
    setError(null);

    try {
      const respuesta = await cambiarPassword(accessToken, {
        passwordActual,
        passwordNueva,
        confirmarPassword,
      });

      await iniciarSesionDesdeRespuesta(respuesta);
      setPasswordActual("");
      setPasswordNueva("");
      setConfirmarPassword("");
      setExito(true);
    } catch (excepcion) {
      setError(humanizarError(excepcion));
    } finally {
      setCargando(false);
    }
  }

  return (
    <dialog
      ref={dialogoRef}
      onClose={cerrarYLimpiar}
      aria-labelledby="cambiar-password-titulo"
      className="w-[min(28rem,calc(100vw-2rem))] rounded-[var(--radius-xl)] border border-[var(--color-border-soft)] bg-[var(--color-surface)] p-0 text-[var(--color-text)] shadow-[0_24px_60px_rgba(12,52,80,0.28)] backdrop:bg-[#0F3D5E]/40 backdrop:backdrop-blur-sm"
    >
      <div className="flex items-start justify-between gap-4 border-b border-[var(--color-divisor)] px-5 py-4">
        <h2
          id="cambiar-password-titulo"
          className="text-lg font-extrabold text-[var(--color-primary)]"
        >
          Cambiar contraseña
        </h2>
        <button
          type="button"
          onClick={cerrarYLimpiar}
          aria-label="Cerrar cambiar contraseña"
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
        {exito ? (
          <>
            <StatusMessage variant="success" role="status" className="font-bold">
              Contraseña actualizada. Por seguridad, cerramos tu sesión en los
              demás dispositivos — acá seguís adentro.
            </StatusMessage>

            <AppButton
              variant="secondary"
              fullWidth
              className="mt-5"
              onClick={cerrarYLimpiar}
            >
              Listo
            </AppButton>
          </>
        ) : (
          <form key={generacion} onSubmit={enviar} className="space-y-4">
            <CampoPassword
              etiqueta="Contraseña actual"
              valor={passwordActual}
              onCambio={(valor) => {
                setPasswordActual(valor);
                setError(null);
              }}
              autoComplete="current-password"
              deshabilitado={cargando}
            />

            <CampoPassword
              etiqueta="Contraseña nueva"
              valor={passwordNueva}
              onCambio={(valor) => {
                setPasswordNueva(valor);
                setError(null);
              }}
              autoComplete="new-password"
              deshabilitado={cargando}
              ayuda="Mínimo 8 caracteres, con al menos una letra y un número."
            />

            <CampoPassword
              etiqueta="Confirmar contraseña nueva"
              valor={confirmarPassword}
              onCambio={(valor) => {
                setConfirmarPassword(valor);
                setError(null);
              }}
              autoComplete="new-password"
              deshabilitado={cargando}
            />

            {error ? (
              <StatusMessage variant="error" role="alert" className="font-bold">
                {error}
              </StatusMessage>
            ) : null}

            <AppButton
              type="submit"
              fullWidth
              disabled={
                cargando ||
                !passwordActual ||
                !passwordNueva ||
                !confirmarPassword
              }
            >
              {cargando ? "Cambiando..." : "Cambiar contraseña"}
            </AppButton>
          </form>
        )}
      </div>
    </dialog>
  );
}

function humanizarError(excepcion: unknown): string {
  if (excepcion instanceof AuthApiError) {
    if (excepcion.status === 429) {
      return "Demasiados intentos. Esperá unos minutos y probá de nuevo.";
    }

    if (excepcion.status === 401) {
      return "Tu sesión expiró. Recargá la página y volvé a intentar.";
    }

    /* Los 400 del backend ya vienen con un mensaje concreto y humano. */
    if (excepcion.message) {
      return humanizarMensajeBackend(excepcion.message);
    }
  }

  return "No pudimos cambiar la contraseña. Revisá tu conexión y probá de nuevo.";
}

/* El backend escribe "password" sin acentos (ASCII); en pantalla no. */
function humanizarMensajeBackend(mensaje: string): string {
  return mensaje
    .replace(/\bLa password\b/g, "La contraseña")
    .replace(/\bde password\b/g, "de contraseña")
    .replace(/\bpassword\b/g, "contraseña");
}

function CampoPassword({
  etiqueta,
  valor,
  onCambio,
  autoComplete,
  deshabilitado,
  ayuda,
}: {
  etiqueta: string;
  valor: string;
  onCambio: (valor: string) => void;
  autoComplete: string;
  deshabilitado: boolean;
  ayuda?: string;
}) {
  const id = useId();
  const [mostrar, setMostrar] = useState(false);

  return (
    <div>
      <label
        htmlFor={id}
        className="mb-1.5 block text-sm font-extrabold text-[var(--color-primary)]"
      >
        {etiqueta}
      </label>
      <div className="relative">
        <input
          id={id}
          type={mostrar ? "text" : "password"}
          autoComplete={autoComplete}
          value={valor}
          onChange={(evento) => onCambio(evento.target.value)}
          disabled={deshabilitado}
          aria-describedby={ayuda ? `${id}-ayuda` : undefined}
          className="min-h-12 w-full rounded-[18px] border border-[var(--color-border-accent)] bg-[var(--color-bg)] py-3 pl-4 pr-14 text-base text-[var(--color-text)] outline-none transition duration-200 ease-out hover:border-[var(--color-accent)] focus:border-[var(--color-accent)] focus:ring-4 focus:ring-[var(--color-border-soft)] disabled:cursor-not-allowed disabled:opacity-70"
        />
        <button
          type="button"
          onClick={() => setMostrar((actual) => !actual)}
          aria-label={mostrar ? "Ocultar contraseña" : "Mostrar contraseña"}
          aria-pressed={mostrar}
          className="absolute right-2 top-1/2 flex h-10 w-10 -translate-y-1/2 items-center justify-center rounded-full text-[var(--color-muted)] transition duration-200 ease-out hover:bg-[var(--color-surface)] hover:text-[var(--color-primary)] focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-[#4FB3D9]/30"
        >
          <IconoOjo tachado={!mostrar} />
        </button>
      </div>
      {ayuda ? (
        <p
          id={`${id}-ayuda`}
          className="mt-1.5 text-xs leading-5 text-[var(--color-muted)]"
        >
          {ayuda}
        </p>
      ) : null}
    </div>
  );
}

/* Ojo abierto = mostrando; tachado = oculta. Mismo dibujo que el login. */
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
      <path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7-10-7-10-7Z" />
      <circle cx="12" cy="12" r="3" />
      {tachado ? <path d="m4 4 16 16" /> : null}
    </svg>
  );
}
