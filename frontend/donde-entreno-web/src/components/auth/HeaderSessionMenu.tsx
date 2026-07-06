"use client";

import { useRouter } from "next/navigation";
import { esRolAdmin, esRolPublicador } from "../../lib/authRedirects";
import { useAuthSession } from "./AuthSessionProvider";
import { AppButton } from "../ui/AppButton";
import { AppLinkButton } from "../ui/AppLinkButton";

type AccesoSesion = {
  href: string;
  label: string;
};

export function HeaderSessionMenu() {
  const router = useRouter();
  const { status, sesion, usuario, cerrarSesion } = useAuthSession();

  if (status === "loading") {
    return (
      <div
        className="h-10 w-full rounded-full border border-[#DDEAF3] bg-white/60 sm:w-36"
        role="status"
        aria-label="Cargando sesión"
      />
    );
  }

  if (status === "guest" || !sesion) {
    return (
      <AppLinkButton
        href="/login"
        variant="outline"
        size="sm"
        className="w-full sm:w-auto"
      >
        Iniciar sesión
      </AppLinkButton>
    );
  }

  const nombre = (usuario?.nombre ?? sesion.usuario.nombre).trim();
  const rol = usuario?.rol ?? sesion.usuario.rol;
  const acceso = obtenerAccesoSesion(rol);

  function manejarCerrarSesion() {
    cerrarSesion();
    router.replace("/");
  }

  return (
    <div className="flex w-full min-w-0 flex-wrap items-center gap-2 sm:w-auto sm:justify-end">
      <span className="min-w-0 flex-1 truncate text-sm font-extrabold text-[var(--color-primary)] sm:flex-none">
        Hola, {nombre || "tu cuenta"}
      </span>
      <AppLinkButton
        href={acceso.href}
        variant="success"
        size="sm"
        className="flex-1 sm:flex-none"
      >
        {acceso.label}
      </AppLinkButton>
      <AppButton
        type="button"
        variant="secondary"
        size="sm"
        onClick={manejarCerrarSesion}
        className="flex-1 sm:flex-none"
      >
        Cerrar sesión
      </AppButton>
    </div>
  );
}

function obtenerAccesoSesion(rol: string): AccesoSesion {
  if (esRolAdmin(rol)) {
    return {
      href: "/admin/solicitudes",
      label: "Panel admin",
    };
  }

  if (esRolPublicador(rol)) {
    return {
      href: "/publicador",
      label: "Panel publicador",
    };
  }

  return {
    href: "/mi-cuenta",
    label: "Mi cuenta",
  };
}
