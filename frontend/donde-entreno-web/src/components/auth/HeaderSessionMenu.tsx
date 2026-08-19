"use client";

import { Fragment } from "react";

import { obtenerSeccionesCuenta } from "../../lib/menuCuenta";
import { useAuthSession } from "./AuthSessionProvider";
import { AppLinkButton } from "../ui/AppLinkButton";
import { IconoMenuCuenta } from "../cuenta/IconoMenuCuenta";
import {
  MenuDesplegable,
  OpcionMenu,
  SeparadorMenu,
} from "../ui/MenuDesplegable";

export function HeaderSessionMenu() {
  const { status, sesion, usuario, cerrarSesion } = useAuthSession();

  if (status === "loading") {
    return (
      <div
        className="h-10 w-full rounded-full border border-[var(--color-border-soft)] bg-white/60 sm:w-36"
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
  const secciones = obtenerSeccionesCuenta(rol);
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

    Las opciones salen de `obtenerSeccionesCuenta`, la misma fuente que
    el panel de cuenta mobile: el espacio personal primero para todos
    los roles, y el lado publicador o la administración como sección
    aparte — nunca mezclados como si fueran lo mismo.
  */
  return (
    <MenuDesplegable
      etiqueta={`Abrir el menú de tu cuenta. Sesión de ${nombre || "usuario"}`}
      /*
        w-full sobre un contenedor con min-w-0: el botón se adapta al
        hueco que le deja el header en vez de desbordarlo. Con el ancho
        del contenido sobresalía ~20px por la derecha cuando la barra
        quedaba justa de espacio.
      */
      className="flex min-h-11 w-full min-w-0 items-center gap-2 rounded-full border border-[var(--color-border-soft)] bg-white py-1.5 pl-1.5 pr-3 text-sm font-extrabold text-[var(--color-primary)] shadow-sm transition duration-200 ease-out hover:border-[var(--color-border-accent)] hover:bg-[var(--color-surface-soft)] focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-[#4FB3D9]/30"
      disparador={
        <>
          <span
            aria-hidden="true"
            className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-[var(--color-primary)] text-xs font-extrabold text-white"
          >
            {inicial}
          </span>
          {/* El nombre es lo único que puede ceder ancho: se recorta. */}
          <span className="min-w-0 flex-1 truncate text-left">
            {nombre || "Mi cuenta"}
          </span>
          <IconoChevron />
        </>
      }
    >
      {(cerrar) => (
        <>
          {secciones.map((seccion, indice) => (
            <Fragment key={seccion.titulo ?? indice}>
              {indice > 0 ? <SeparadorMenu /> : null}

              {seccion.titulo ? (
                <p className="px-3 pb-1.5 pt-2 text-[11px] font-extrabold uppercase tracking-[0.16em] text-[var(--color-secondary)]">
                  {seccion.titulo}
                </p>
              ) : null}

              {seccion.items.map((item) => (
                <OpcionMenu
                  key={item.href}
                  href={item.href}
                  onClick={cerrar}
                  destacada
                >
                  <IconoMenuCuenta
                    tipo={item.icono}
                    className="h-[18px] w-[18px] shrink-0 text-[var(--color-accent)]"
                  />
                  {item.label}
                </OpcionMenu>
              ))}
            </Fragment>
          ))}

          <SeparadorMenu />

          <OpcionMenu onClick={manejarCerrarSesion}>
            <IconoMenuCuenta tipo="salir" />
            Cerrar sesión
          </OpcionMenu>
        </>
      )}
    </MenuDesplegable>
  );
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
