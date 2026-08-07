"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import {
  esRolAdmin,
  esRolPublicador,
  obtenerRutaInicialPorRol,
} from "../../lib/authRedirects";
import { useAuthSession } from "../auth/AuthSessionProvider";

type IconoNavegacion = "inicio" | "explorar" | "guardados" | "cuenta";

type ItemNavegacion = {
  label: string;
  href: string;
  icono: IconoNavegacion;
  activo: (pathname: string) => boolean;
};

export function MobileNavigation() {
  const pathname = usePathname() ?? "/";
  const { status, sesion, usuario } = useAuthSession();
  const sesionCargando = status === "loading";
  const autenticado = status === "authenticated" && Boolean(sesion);
  const rol = usuario?.rol ?? sesion?.usuario.rol;
  const usaPanel = Boolean(
    autenticado && rol && (esRolAdmin(rol) || esRolPublicador(rol))
  );
  const destinoCuenta = autenticado && rol
    ? obtenerRutaInicialPorRol(rol)
    : `/login?returnTo=${encodeURIComponent("/mi-cuenta")}`;
  const destinoGuardados = autenticado
    ? "/favoritos"
    : `/login?motivo=cuenta&returnTo=${encodeURIComponent("/favoritos")}`;

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
      label: "Guardados",
      href: destinoGuardados,
      icono: "guardados",
      activo: (ruta) => ruta === "/favoritos",
    },
    {
      /*
        Mientras la sesión se resuelve mostramos "Cuenta" neutro para que un
        usuario logueado no vea (ni toque) "Ingresar" en cada carga.
      */
      label: sesionCargando
        ? "Cuenta"
        : usaPanel
          ? "Panel"
          : autenticado
            ? "Mi espacio"
            : "Ingresar",
      href: destinoCuenta,
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
      className="fixed inset-x-0 bottom-0 z-50 border-t border-[#D9E2EC] bg-white/95 shadow-[0_-10px_30px_rgba(15,61,94,0.10)] backdrop-blur-lg lg:hidden"
      style={{ paddingBottom: "max(0.5rem, env(safe-area-inset-bottom))" }}
    >
      <div className="mx-auto grid min-h-16 max-w-lg grid-cols-4 px-2 pt-1.5">
        {items.map((item) => {
          const seleccionado = item.activo(pathname);
          const clase = `group flex min-h-14 flex-col items-center justify-center gap-1 rounded-[16px] px-1 text-[11px] font-extrabold transition duration-200 ease-out ${
            seleccionado
              ? "bg-[#E6F7EF] text-[#1D7B4A]"
              : "text-[var(--color-muted)] hover:bg-[#F8FCFE] hover:text-[var(--color-primary)]"
          }`;

          if (item.icono === "cuenta" && sesionCargando) {
            return (
              <span
                key={item.icono}
                role="status"
                aria-label="Cargando cuenta"
                className={`${clase} animate-pulse`}
              >
                <Icono tipo={item.icono} seleccionado={false} />
                <span>{item.label}</span>
              </span>
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
    return (
      <svg {...comun}>
        <path d="M6 3h12a1 1 0 0 1 1 1v17l-7-4.2L5 21V4a1 1 0 0 1 1-1z" />
      </svg>
    );
  }

  return (
    <svg {...comun} fill="none">
      <circle cx="12" cy="8" r="4" />
      <path d="M4 21c0-4 3.6-7 8-7s8 3 8 7" />
    </svg>
  );
}
