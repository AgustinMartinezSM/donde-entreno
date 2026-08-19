"use client";

import { useEffect, useRef, useState } from "react";

import { esRolAdmin, esRolPublicador } from "../../lib/authRedirects";
import type { UsuarioActual } from "../../types/auth";
import { AppButton } from "../ui/AppButton";
import {
  MenuDesplegable,
  OpcionMenu,
  SeparadorMenu,
} from "../ui/MenuDesplegable";
import { StatusMessage } from "../ui/StatusMessage";

type MenuAjustesProps = {
  usuario: UsuarioActual | null;
  rol: string | null;
  onIrADeportes: () => void;
  onCerrarSesion: () => void;
};

/*
  Ajustes del perfil: todo lo que es configuración, fuera del contenido.

  Antes era una solapa más —al mismo nivel que las actividades guardadas
  y la gente que seguís— con "Cerrar sesión" como botón visible. Ahora
  vive detrás del engranaje de la cabecera: sigue a un toque de
  distancia, pero deja las cuatro solapas para lo que la persona
  realmente viene a mirar.
*/
export function MenuAjustes({
  usuario,
  rol,
  onIrADeportes,
  onCerrarSesion,
}: MenuAjustesProps) {
  const [datosAbiertos, setDatosAbiertos] = useState(false);
  const accesoDeRol = obtenerAccesoDeRol(rol);

  return (
    <>
      <MenuDesplegable
        etiqueta="Abrir ajustes de mi perfil"
        className="flex h-10 w-10 items-center justify-center rounded-full border border-white/40 bg-white/20 text-white backdrop-blur-sm transition duration-200 ease-out hover:bg-white/30 focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-white/40"
        disparador={<IconoAjustes />}
      >
        {(cerrar) => (
          <>
            <p className="px-3 pb-2 pt-1 text-xs font-extrabold uppercase tracking-[0.14em] text-[var(--color-secondary)]">
              Ajustes
            </p>

            <OpcionMenu
              destacada
              onClick={() => {
                onIrADeportes();
                cerrar();
              }}
            >
              Mis deportes
            </OpcionMenu>

            <OpcionMenu href="/ciudades" onClick={cerrar} destacada>
              Cambiar ciudad
            </OpcionMenu>

            <OpcionMenu
              destacada
              onClick={() => {
                setDatosAbiertos(true);
                cerrar();
              }}
            >
              Datos de mi cuenta
            </OpcionMenu>

            {accesoDeRol ? (
              <>
                <SeparadorMenu />
                <OpcionMenu href={accesoDeRol.href} onClick={cerrar} destacada>
                  {accesoDeRol.label}
                </OpcionMenu>
              </>
            ) : null}

            <SeparadorMenu />

            <OpcionMenu onClick={onCerrarSesion}>Cerrar sesión</OpcionMenu>
          </>
        )}
      </MenuDesplegable>

      <DialogoDatosDeCuenta
        usuario={usuario}
        abierto={datosAbiertos}
        onCerrar={() => setDatosAbiertos(false)}
      />
    </>
  );
}

/*
  Los datos de la cuenta se muestran en un diálogo y no en una sección
  fija de la página: son datos que se consultan de vez en cuando, no
  contenido. Son de solo lectura porque no existe endpoint para
  editarlos — decirlo es más honesto que un formulario que no guarda.
*/
function DialogoDatosDeCuenta({
  usuario,
  abierto,
  onCerrar,
}: {
  usuario: UsuarioActual | null;
  abierto: boolean;
  onCerrar: () => void;
}) {
  const dialogoRef = useRef<HTMLDialogElement | null>(null);

  /*
    <dialog> nativo con showModal(): trae contención de foco, cierre con
    Escape y fondo inerte sin que tengamos que implementarlos.
  */
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

  return (
    <dialog
      ref={dialogoRef}
      onClose={onCerrar}
      aria-labelledby="datos-cuenta-titulo"
      className="w-[min(28rem,calc(100vw-2rem))] rounded-[var(--radius-xl)] border border-[var(--color-border-soft)] bg-white p-0 text-[var(--color-text)] shadow-[0_24px_60px_rgba(12,52,80,0.28)] backdrop:bg-[#0F3D5E]/40 backdrop:backdrop-blur-sm"
    >
      <div className="flex items-start justify-between gap-4 border-b border-[#EDF3F8] px-5 py-4">
        <h2
          id="datos-cuenta-titulo"
          className="text-lg font-extrabold text-[var(--color-primary)]"
        >
          Datos de mi cuenta
        </h2>
        <button
          type="button"
          onClick={onCerrar}
          aria-label="Cerrar datos de mi cuenta"
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
        {usuario ? (
          <>
            <dl className="grid gap-3 sm:grid-cols-2">
              <DatoCuenta etiqueta="Nombre" valor={usuario.nombre} />
              <DatoCuenta etiqueta="Apellido" valor={usuario.apellido} />
              <DatoCuenta etiqueta="Email" valor={usuario.email} />
              <DatoCuenta etiqueta="Rol" valor={formatearRol(usuario.rol)} />
            </dl>

            <p className="mt-4 text-xs leading-5 text-[var(--color-muted)]">
              Por ahora estos datos son de solo lectura. Si necesitás
              cambiarlos, escribinos y lo resolvemos.
            </p>
          </>
        ) : (
          <StatusMessage variant="info">
            Estamos preparando los datos de tu cuenta.
          </StatusMessage>
        )}

        <AppButton
          variant="secondary"
          fullWidth
          className="mt-5"
          onClick={onCerrar}
        >
          Cerrar
        </AppButton>
      </div>
    </dialog>
  );
}

function DatoCuenta({ etiqueta, valor }: { etiqueta: string; valor: string }) {
  return (
    <div className="rounded-[14px] border border-[var(--color-border-soft)] bg-[var(--color-bg)] p-3">
      <dt className="text-[11px] font-extrabold uppercase tracking-[0.14em] text-[var(--color-secondary)]">
        {etiqueta}
      </dt>
      <dd className="mt-1.5 break-words text-sm font-bold text-[var(--color-primary)]">
        {valor}
      </dd>
    </div>
  );
}

function obtenerAccesoDeRol(
  rol: string | null
): { href: string; label: string } | null {
  if (!rol) {
    return null;
  }

  if (esRolAdmin(rol)) {
    return { href: "/admin/solicitudes", label: "Administración" };
  }

  if (esRolPublicador(rol)) {
    return { href: "/publicador", label: "Mi espacio de publicador" };
  }

  return null;
}

function formatearRol(rol: string): string {
  /* Los roles vienen como ROLE_USUARIO y se leían "Role Usuario". */
  return rol
    .replace(/^ROLE_/i, "")
    .toLowerCase()
    .split("_")
    .map((parte) => parte.charAt(0).toUpperCase() + parte.slice(1))
    .join(" ");
}

function IconoAjustes() {
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
      <circle cx="12" cy="12" r="3" />
      <path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 1 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 1 1-2.83-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 1 1 2.83-2.83l.06.06A1.65 1.65 0 0 0 9 4.6h.09A1.65 1.65 0 0 0 10 3.09V3a2 2 0 0 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 1 1 2.83 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z" />
    </svg>
  );
}
