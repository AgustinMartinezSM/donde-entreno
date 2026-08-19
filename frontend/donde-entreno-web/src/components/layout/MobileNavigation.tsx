"use client";

import Link from "next/link";
import { useState } from "react";
import { usePathname } from "next/navigation";
import { useAuthSession } from "../auth/AuthSessionProvider";
import { IconoGuardar } from "../ui/IconoGuardar";
import { MenuCuentaMobile } from "./MenuCuentaMobile";

type IconoNavegacion = "inicio" | "explorar" | "guardados" | "cuenta";

type ItemNavegacion = {
  label: string;
  icono: IconoNavegacion;
  activo: (pathname: string) => boolean;
  href: string;
};

export function MobileNavigation() {
  const pathname = usePathname() ?? "/";
  const { status, sesion } = useAuthSession();
  const [menuCuentaAbierto, setMenuCuentaAbierto] = useState(false);
  const sesionCargando = status === "loading";
  const autenticado = status === "authenticated" && Boolean(sesion);

  const items: ItemNavegacion[] = [
    {
      label: "Inicio",
      href: "/",
      icono: "inicio",
      activo: (ruta) => ruta === "/",
    },
    {
      label: "Explorar",
      href: "/explorar",
      icono: "explorar",
      activo: (ruta) =>
        ["/explorar", "/actividades", "/deportes", "/ciudades"].some(
          (prefijo) => ruta === prefijo || ruta.startsWith(`${prefijo}/`)
        ),
    },
    {
      /*
        "Guardados" vuelve a la barra: el asistente ya no necesita este
        lugar porque Dondi vive como launcher flotante en toda la app, y
        dos entradas al mismo panel confundían cuál era la principal.
        Para el visitante el destino es el login con aviso y returnTo,
        igual que el acceso de Guardados del header.
      */
      label: "Guardados",
      href: autenticado
        ? "/favoritos"
        : `/login?motivo=cuenta&returnTo=${encodeURIComponent("/favoritos")}`,
      icono: "guardados",
      activo: (ruta) => ruta === "/favoritos",
    },
    {
      /*
        Mientras la sesión se resuelve mostramos "Mi perfil" neutro para
        que un usuario logueado no vea (ni toque) "Ingresar" en cada
        carga.

        Con sesión, este ítem ya no navega: abre el panel de cuenta. El
        destino directo por rol mandaba al publicador SIEMPRE a
        /publicador, sin ningún camino a su perfil deportivo ni a sus
        guardadas — "Mi perfil" significaba otra cosa según quién lo
        tocara. El visitante sí navega: /login, con "Iniciar sesión" y
        "Crear cuenta" a la vista, ya es su menú.
      */
      label: autenticado || sesionCargando ? "Mi perfil" : "Ingresar",
      href: `/login?returnTo=${encodeURIComponent("/mi-cuenta")}`,
      icono: "cuenta",
      activo: (ruta) =>
        (!autenticado &&
          ["/login", "/registro"].some(
            (prefijo) => ruta === prefijo || ruta.startsWith(`${prefijo}/`)
          )) ||
        ["/mi-cuenta", "/publicador", "/admin"].some(
          (prefijo) => ruta === prefijo || ruta.startsWith(`${prefijo}/`)
        ),
    },
  ];

  return (
    <nav
      aria-label="Navegación principal mobile"
      className="surface-glass fixed inset-x-0 bottom-0 z-50 border-t border-white/60 shadow-[0_-10px_30px_rgba(15,61,94,0.12)] backdrop-blur-xl backdrop-saturate-150 lg:hidden"
      style={{ paddingBottom: "max(0.5rem, env(safe-area-inset-bottom))" }}
    >
      <div className="mx-auto grid min-h-16 max-w-lg grid-cols-4 px-2 pt-1.5">
        {items.map((item) => {
          const seleccionado = item.activo(pathname);
          /*
            El tab activo se marca con pastilla verde Y con un punto
            debajo del ícono: en una barra de vidrio, el fondo suave solo
            se pierde sobre contenido claro.
          */
          const clase = `group relative flex min-h-14 flex-col items-center justify-center gap-1 rounded-[18px] px-1 text-[11px] font-extrabold transition duration-200 ease-out ${
            seleccionado
              ? "bg-[var(--color-success-soft)] text-[var(--color-success)] shadow-[inset_0_0_0_1px_rgba(46,184,114,0.22)]"
              : "text-[var(--color-muted)] hover:bg-white/70 hover:text-[var(--color-primary)]"
          }`;

          if (item.icono === "cuenta" && sesionCargando) {
            return (
              <span
                key={item.icono}
                role="status"
                aria-label="Cargando tu perfil"
                className={`${clase} animate-pulse`}
              >
                <Icono tipo={item.icono} seleccionado={false} />
                <span>{item.label}</span>
              </span>
            );
          }

          if (item.icono === "cuenta" && autenticado) {
            return (
              <button
                key={item.icono}
                type="button"
                onClick={() => setMenuCuentaAbierto(true)}
                aria-haspopup="dialog"
                aria-expanded={menuCuentaAbierto}
                aria-current={seleccionado ? "page" : undefined}
                className={clase}
              >
                <Icono tipo={item.icono} seleccionado={seleccionado} />
                <span>{item.label}</span>
              </button>
            );
          }

          return (
            <Link
              key={item.icono}
              href={item.href}
              aria-current={seleccionado ? "page" : undefined}
              className={clase}
            >
              <Icono tipo={item.icono} seleccionado={seleccionado} />
              <span>{item.label}</span>
            </Link>
          );
        })}
      </div>

      {autenticado ? (
        <MenuCuentaMobile
          abierto={menuCuentaAbierto}
          onCerrar={() => setMenuCuentaAbierto(false)}
        />
      ) : null}
    </nav>
  );
}

function Icono({
  tipo,
  seleccionado,
}: {
  tipo: IconoNavegacion;
  seleccionado: boolean;
}) {
  const comun = {
    viewBox: "0 0 24 24",
    className: "h-5 w-5",
    fill: seleccionado ? "currentColor" : "none",
    stroke: "currentColor",
    strokeWidth: 2,
    strokeLinecap: "round" as const,
    strokeLinejoin: "round" as const,
    "aria-hidden": true,
  };

  if (tipo === "inicio") {
    return (
      <svg {...comun}>
        <path d="m3 11 9-8 9 8" />
        <path d="M5 10v10h14V10" />
        <path d="M9 20v-6h6v6" fill="none" />
      </svg>
    );
  }

  if (tipo === "explorar") {
    return (
      <svg {...comun} fill="none">
        <circle cx="12" cy="12" r="9" />
        <path d="m15.5 8.5-2.1 4.9-4.9 2.1 2.1-4.9 4.9-2.1z" />
      </svg>
    );
  }

  if (tipo === "guardados") {
    /* Mismo bookmark que FavoritoButton y el header: relleno = activo. */
    return <IconoGuardar relleno={seleccionado} className="h-5 w-5" />;
  }

  return (
    <svg {...comun} fill="none">
      <circle cx="12" cy="8" r="4" />
      <path d="M4 21c0-4 3.6-7 8-7s8 3 8 7" />
    </svg>
  );
}
