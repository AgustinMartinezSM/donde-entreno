"use client";

import { esRolAdmin, esRolPublicador } from "../../lib/authRedirects";
import { useAuthSession } from "./AuthSessionProvider";
import { AppLinkButton } from "../ui/AppLinkButton";
import {
  MenuDesplegable,
  OpcionMenu,
  SeparadorMenu,
} from "../ui/MenuDesplegable";

type AccesoSesion = {
  href: string;
  label: string;
};

export function HeaderSessionMenu() {
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
  const accesoDeRol = obtenerAccesoDeRol(rol);
  const inicial = nombre.charAt(0).toLocaleUpperCase("es") || "D";

  function manejarCerrarSesion() {
    cerrarSesion();
    window.location.replace("/login?logout=1");
  }

  /*
    Antes el header mostraba tres cosas del mismo tamaño —"Hola, X", el
    acceso al perfil y Cerrar sesión— así que salir de la sesión pesaba
    igual que entrar a ella. Ahora hay una sola entrada (el avatar) y
    adentro un menú donde cerrar sesión ocupa el lugar que le
    corresponde: el último, en gris, después de un separador.
  */
  return (
    <MenuDesplegable
      etiqueta={`Abrir el menú de tu cuenta. Sesión de ${nombre || "usuario"}`}
      className="flex min-h-11 min-w-0 items-center gap-2.5 rounded-full border border-[#DDEAF3] bg-white py-1.5 pl-1.5 pr-3.5 text-sm font-extrabold text-[var(--color-primary)] shadow-sm transition duration-200 ease-out hover:border-[#BFDDEA] hover:bg-[#F8FCFE] focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-[#4FB3D9]/30"
      disparador={
        <>
          <span
            aria-hidden="true"
            className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-[var(--color-primary)] text-xs font-extrabold text-white"
          >
            {inicial}
          </span>
          <span className="min-w-0 max-w-28 truncate">
            {nombre || "Mi cuenta"}
          </span>
          <IconoChevron />
        </>
      }
    >
      {(cerrar) => (
        <>
          <OpcionMenu href="/mi-cuenta" onClick={cerrar} destacada>
            Mi perfil deportivo
          </OpcionMenu>

          <OpcionMenu href="/favoritos" onClick={cerrar} destacada>
            Actividades guardadas
          </OpcionMenu>

          {accesoDeRol ? (
            <OpcionMenu href={accesoDeRol.href} onClick={cerrar} destacada>
              {accesoDeRol.label}
            </OpcionMenu>
          ) : null}

          <SeparadorMenu />

          <OpcionMenu onClick={manejarCerrarSesion}>Cerrar sesión</OpcionMenu>
        </>
      )}
    </MenuDesplegable>
  );
}

/*
  El acceso por rol es una opción más del menú, no un botón aparte: el
  usuario común no tiene ninguno y no ve nada, y quien publica o
  administra llega a su lugar sin que eso ocupe el header de todos.

  La administración se nombra como tal: quien la usa es del equipo y le
  sirve saber que está entrando a otra cosa.
*/
function obtenerAccesoDeRol(rol: string): AccesoSesion | null {
  if (esRolAdmin(rol)) {
    return {
      href: "/admin/solicitudes",
      label: "Administración",
    };
  }

  if (esRolPublicador(rol)) {
    return {
      href: "/publicador",
      label: "Mi espacio de publicador",
    };
  }

  return null;
}

function IconoChevron() {
  return (
    <svg
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="2.5"
      strokeLinecap="round"
      strokeLinejoin="round"
      className="h-3.5 w-3.5 shrink-0 text-[var(--color-muted)]"
      aria-hidden="true"
    >
      <path d="m6 9 6 6 6-6" />
    </svg>
  );
}
