"use client";

import Link from "next/link";
import { obtenerRutaInicialPorRol } from "../../lib/authRedirects";
import { useAuthSession } from "../auth/AuthSessionProvider";

export function MobileAccountShortcut() {
  const { status, sesion, usuario } = useAuthSession();

  if (status === "loading") {
    return (
      <div
        role="status"
        aria-label="Cargando cuenta"
        className="h-11 w-11 animate-pulse rounded-full border border-[#DDEAF3] bg-white"
      />
    );
  }

  if (status === "guest" || !sesion) {
    return (
      <Link
        href="/login"
        aria-label="Iniciar sesión"
        className="flex h-11 w-11 items-center justify-center rounded-full border border-[#BFDDEA] bg-white text-[var(--color-primary)] shadow-sm transition hover:border-[var(--color-primary)] hover:bg-[#F8FCFE]"
      >
        <IconoCuenta />
      </Link>
    );
  }

  const nombre = (usuario?.nombre ?? sesion.usuario.nombre).trim();
  const rol = usuario?.rol ?? sesion.usuario.rol;
  const inicial = nombre.charAt(0).toLocaleUpperCase("es") || "D";

  return (
    <Link
      href={obtenerRutaInicialPorRol(rol)}
      aria-label={`Abrir mi espacio. Sesión de ${nombre || "usuario"}`}
      className="flex h-11 w-11 items-center justify-center rounded-full bg-[var(--color-primary)] text-sm font-extrabold text-white shadow-[var(--shadow-button)] transition hover:bg-[#0B314D]"
    >
      {inicial}
    </Link>
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
