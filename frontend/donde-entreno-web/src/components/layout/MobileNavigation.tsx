"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { obtenerRutaInicialPorRol } from "../../lib/authRedirects";
import { useAuthSession } from "../auth/AuthSessionProvider";

type IconoNavegacion = "inicio" | "explorar" | "asistente" | "cuenta";

/*
  Un item puede navegar (href) o disparar una acción en la página, como
  abrir el asistente, que no es una ruta.
*/
type ItemNavegacion = {
  label: string;
  icono: IconoNavegacion;
  activo: (pathname: string) => boolean;
  href?: string;
  alPresionar?: () => void;
  ariaLabel?: string;
};

export function MobileNavigation() {
  const pathname = usePathname() ?? "/";
  const { status, sesion, usuario } = useAuthSession();
  const sesionCargando = status === "loading";
  const autenticado = status === "authenticated" && Boolean(sesion);
  const rol = usuario?.rol ?? sesion?.usuario.rol;
  /*
    Cada rol sigue llegando a su propio destino: el nombre del ítem es el
    mismo para todos, el lugar no.
  */
  const destinoCuenta = autenticado && rol
    ? obtenerRutaInicialPorRol(rol)
    : `/login?returnTo=${encodeURIComponent("/mi-cuenta")}`;

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
        El asistente no es una ruta: abre el panel con el mismo evento
        que usa el botón de la home. Ocupa el lugar que antes tenía
        "Guardados", que ahora vive dentro de Mi perfil (como en las
        apps sociales: lo guardado es un destino al que vas a propósito,
        no algo que tocás mientras navegás).
      */
      label: "Asistente",
      ariaLabel: "Abrir el asistente de DondeEntreno",
      icono: "asistente",
      alPresionar: () =>
        window.dispatchEvent(new Event("donde-entreno:abrir-asistente")),
      activo: () => false,
    },
    {
      /*
        Mientras la sesión se resuelve mostramos "Mi perfil" neutro para
        que un usuario logueado no vea (ni toque) "Ingresar" en cada
        carga.

        "Perfil" en vez de "Panel": panel es lenguaje de sistema, no de
        persona. El publicador y el admin llegan al mismo lugar de
        siempre, solo cambia cómo se llama.
      */
      label: autenticado || sesionCargando ? "Mi perfil" : "Ingresar",
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
                aria-label="Cargando tu perfil"
                className={`${clase} animate-pulse`}
              >
                <Icono tipo={item.icono} seleccionado={false} />
                <span>{item.label}</span>
              </span>
            );
          }

          if (item.alPresionar) {
            return (
              <button
                key={item.icono}
                type="button"
                onClick={item.alPresionar}
                aria-label={item.ariaLabel}
                aria-haspopup="dialog"
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
              href={item.href ?? "/"}
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

  if (tipo === "asistente") {
    return (
      <svg {...comun} fill="none">
        <path d="M21 11.5a8.5 8.5 0 0 1-12.3 7.6L3 21l1.9-5.7A8.5 8.5 0 1 1 21 11.5z" />
        <path d="M8.5 11.5h.01M12 11.5h.01M15.5 11.5h.01" />
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
