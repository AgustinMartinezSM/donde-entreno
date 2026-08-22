"use client";

import Link from "next/link";
import { useState } from "react";
import { useAuthSession } from "../auth/AuthSessionProvider";
import { AvatarUsuario } from "../cuenta/AvatarUsuario";
import { MenuCuentaMobile } from "./MenuCuentaMobile";

/*
  Avatar del header en mobile. Con sesión abre el panel de cuenta — el
  mismo que "Mi perfil" en la barra inferior: las dos entradas tienen
  que significar LO MISMO. Antes navegaba directo a un destino por rol,
  así que el publicador caía en /publicador sin forma de llegar a su
  lado persona.
*/
export function MobileAccountShortcut() {
  const { status, sesion, usuario } = useAuthSession();
  const [menuAbierto, setMenuAbierto] = useState(false);

  if (status === "loading") {
    return (
      <div
        role="status"
        aria-label="Cargando cuenta"
        className="h-11 w-11 animate-pulse rounded-full border border-[var(--color-border-soft)] bg-[var(--color-surface)]"
      />
    );
  }

  if (status === "guest" || !sesion) {
    return (
      <Link
        href="/login"
        aria-label="Iniciar sesión"
        className="flex h-11 w-11 items-center justify-center rounded-full border border-[var(--color-border-accent)] bg-[var(--color-surface)] text-[var(--color-primary)] shadow-sm transition hover:border-[var(--color-primary)] hover:bg-[var(--color-surface-soft)]"
      >
        <IconoCuenta />
      </Link>
    );
  }

  const nombre = (usuario?.nombre ?? sesion.usuario.nombre).trim();

  return (
    <>
      <button
        type="button"
        onClick={() => setMenuAbierto(true)}
        aria-haspopup="dialog"
        aria-expanded={menuAbierto}
        aria-label={`Abrir el menú de tu cuenta. Sesión de ${nombre || "usuario"}`}
        className="flex h-11 w-11 items-center justify-center rounded-full shadow-[var(--shadow-button)] transition focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-[#4FB3D9]/40 active:scale-95"
      >
        <AvatarUsuario
          usuario={usuario ?? sesion.usuario}
          className="h-11 w-11"
          claseTexto="text-sm"
        />
      </button>

      <MenuCuentaMobile
        abierto={menuAbierto}
        onCerrar={() => setMenuAbierto(false)}
      />
    </>
  );
}

function IconoCuenta() {
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
      <circle cx="12" cy="8" r="4" />
      <path d="M4 21c0-4 3.6-7 8-7s8 3 8 7" />
    </svg>
  );
}
