"use client";

import Link from "next/link";
import { useEffect, useRef } from "react";
import { Fragment } from "react";

import { esRolAdmin, esRolPublicador } from "../../lib/authRedirects";
import { obtenerSeccionesCuenta } from "../../lib/menuCuenta";
import { useAuthSession } from "../auth/AuthSessionProvider";
import { IconoMenuCuenta } from "../cuenta/IconoMenuCuenta";
import { SelectorTema } from "../tema/SelectorTema";

type MenuCuentaMobileProps = {
  abierto: boolean;
  onCerrar: () => void;
};

/*
  Panel de cuenta mobile (bottom sheet).

  Es la pieza que le faltaba al teléfono: en desktop el avatar despliega
  un menú con todas las opciones, pero en mobile "Mi perfil" saltaba
  directo a un único destino por rol — un publicador caía SIEMPRE en
  /publicador y no tenía ningún camino a su perfil deportivo ni a sus
  guardadas. Ahora tocar "Mi perfil" (barra inferior o avatar del
  header) abre este panel con las mismas opciones que el menú desktop,
  armadas por `obtenerSeccionesCuenta`.

  Es un <dialog> nativo con showModal(): trae foco contenido, cierre con
  Escape, velo propio y fondo inerte sin reimplementarlos. Además vive
  en el top layer, así que queda por encima de la barra inferior (z-50),
  del launcher de Dondi (z-50) y de su panel (z-60) sin sumar un z-index
  más a esa guerra.

  Solo lo montan los disparadores mobile (<lg); el visitante nunca lo
  ve: sus disparadores navegan directo a /login, que con "Iniciar
  sesión" y "Crear cuenta" a la vista ya es su menú.
*/
export function MenuCuentaMobile({ abierto, onCerrar }: MenuCuentaMobileProps) {
  const { sesion, usuario, cerrarSesion } = useAuthSession();
  const dialogoRef = useRef<HTMLDialogElement | null>(null);

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

  /*
    El fondo de un dialog modal queda inerte pero la página todavía
    puede scrollear con la rueda o el gesto: se congela mientras el
    panel está abierto para que el sheet se sienta anclado.
  */
  useEffect(() => {
    if (!abierto) {
      return;
    }

    const overflowPrevio = document.body.style.overflow;
    document.body.style.overflow = "hidden";

    return () => {
      document.body.style.overflow = overflowPrevio;
    };
  }, [abierto]);

  /*
    Los disparadores viven en contenedores lg:hidden: si el viewport
    cruza a desktop con el panel abierto (rotar una tablet), el dialog
    quedaría invisible pero modal — página inerte y scroll congelado sin
    nada en pantalla. Cruzar el umbral lo cierra.
  */
  useEffect(() => {
    if (!abierto) {
      return;
    }

    const consulta = window.matchMedia("(min-width: 1024px)");

    if (consulta.matches) {
      onCerrar();
      return;
    }

    function alCambiar(evento: MediaQueryListEvent) {
      if (evento.matches) {
        onCerrar();
      }
    }

    consulta.addEventListener("change", alCambiar);

    return () => {
      consulta.removeEventListener("change", alCambiar);
    };
  }, [abierto, onCerrar]);

  const usuarioDeSesion = usuario ?? sesion?.usuario ?? null;
  const rol = usuarioDeSesion?.rol ?? null;
  const nombre = (usuarioDeSesion?.nombre ?? "").trim();
  const inicial = nombre.charAt(0).toLocaleUpperCase("es") || "D";
  const secciones = obtenerSeccionesCuenta(rol);
  const chipRol = obtenerChipRol(rol);

  function manejarCerrarSesion() {
    onCerrar();
    cerrarSesion();
    window.location.replace("/login?logout=1");
  }

  return (
    <dialog
      ref={dialogoRef}
      onClose={onCerrar}
      /* Un click en el velo llega con target = el propio dialog. */
      onClick={(evento) => {
        if (evento.target === dialogoRef.current) {
          onCerrar();
        }
      }}
      aria-label="Menú de mi cuenta"
      className="fixed inset-x-0 bottom-0 top-auto m-0 max-h-[85dvh] w-full max-w-none animate-[de-sheet_0.28s_ease-out] overflow-y-auto rounded-t-[28px] border border-b-0 border-[var(--color-border-soft)] bg-[var(--color-surface)] p-0 text-[var(--color-text)] shadow-[0_-18px_50px_rgba(12,52,80,0.28)] backdrop:bg-[#0B314D]/40 backdrop:backdrop-blur-sm"
    >
      <div
        className="mx-auto w-full max-w-lg px-5 pt-3"
        style={{
          paddingBottom: "max(1.25rem, env(safe-area-inset-bottom))",
        }}
      >
        {/* Manija visual del sheet: señal de "esto se desliza desde abajo". */}
        <div
          aria-hidden="true"
          className="mx-auto h-1.5 w-10 rounded-full bg-[var(--color-border)]"
        />

        <div className="mt-4 flex items-center gap-3">
          <span
            aria-hidden="true"
            className="flex h-12 w-12 shrink-0 items-center justify-center rounded-full bg-[var(--color-brand)] text-base font-extrabold text-white"
          >
            {inicial}
          </span>

          <div className="min-w-0 flex-1">
            <p className="truncate text-base font-extrabold text-[var(--color-primary)]">
              {nombre || "Tu cuenta"}
            </p>
            <p className="mt-0.5 flex flex-wrap items-center gap-x-2 gap-y-1 text-xs font-bold text-[var(--color-muted)]">
              <span className="truncate">
                {usuario?.email ?? "Tu espacio en DondeEntreno"}
              </span>
              {chipRol ? (
                <span className="shrink-0 rounded-full bg-[var(--color-success-soft)] px-2 py-0.5 text-[11px] font-extrabold text-[var(--color-success)]">
                  {chipRol}
                </span>
              ) : null}
            </p>
          </div>

          <button
            type="button"
            onClick={onCerrar}
            aria-label="Cerrar el menú de mi cuenta"
            className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full text-[var(--color-muted)] transition duration-200 ease-out hover:bg-[var(--color-bg)] hover:text-[var(--color-primary)] focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-[#4FB3D9]/30 active:scale-95"
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

        <div className="mt-4">
          {secciones.map((seccion) => (
            <Fragment key={seccion.titulo ?? "principal"}>
              {seccion.titulo ? (
                <p className="mt-4 px-1 pb-1.5 text-[11px] font-extrabold uppercase tracking-[0.16em] text-[var(--color-secondary)]">
                  {seccion.titulo}
                </p>
              ) : null}

              <div className="grid gap-1">
                {seccion.items.map((item) => (
                  <FilaMenuCuenta
                    key={item.href}
                    href={item.href}
                    icono={item.icono}
                    onElegir={onCerrar}
                  >
                    {item.label}
                  </FilaMenuCuenta>
                ))}
              </div>
            </Fragment>
          ))}

          <hr className="my-3 border-t border-[var(--color-divisor)]" />

          <div className="px-1">
            <SelectorTema />
          </div>

          <hr className="my-3 border-t border-[var(--color-divisor)]" />

          <FilaMenuCuenta icono="salir" onElegir={manejarCerrarSesion} apagada>
            Cerrar sesión
          </FilaMenuCuenta>
        </div>
      </div>
    </dialog>
  );
}

/*
  Fila del panel: link real o botón real, nunca un div con handler. El
  ícono va en una ficha suave para que la lista se lea como una app y no
  como un listado gris.
*/
function FilaMenuCuenta({
  children,
  icono,
  href,
  onElegir,
  apagada = false,
}: {
  children: React.ReactNode;
  icono: Parameters<typeof IconoMenuCuenta>[0]["tipo"];
  href?: string;
  onElegir: () => void;
  apagada?: boolean;
}) {
  const clase = `flex min-h-12 w-full items-center gap-3 rounded-[16px] px-2.5 py-2 text-left text-[15px] font-bold transition duration-200 ease-out focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-[#4FB3D9]/30 ${
    apagada
      ? "text-[var(--color-muted)] hover:bg-[var(--color-bg)] hover:text-[var(--color-primary)]"
      : "text-[var(--color-primary)] hover:bg-[var(--color-surface-soft)]"
  }`;

  const ficha = (
    <span
      aria-hidden="true"
      className={`flex h-9 w-9 shrink-0 items-center justify-center rounded-[12px] ${
        apagada
          ? "bg-[var(--color-bg)] text-[var(--color-muted)]"
          : "bg-[var(--color-info-soft)] text-[var(--color-accent)]"
      }`}
    >
      <IconoMenuCuenta tipo={icono} />
    </span>
  );

  if (href) {
    return (
      <Link href={href} onClick={onElegir} className={clase}>
        {ficha}
        <span className="min-w-0 flex-1 truncate">{children}</span>
        <IconoFlecha />
      </Link>
    );
  }

  return (
    <button type="button" onClick={onElegir} className={clase}>
      {ficha}
      <span className="min-w-0 flex-1 truncate">{children}</span>
    </button>
  );
}

function IconoFlecha() {
  return (
    <svg
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="2.5"
      strokeLinecap="round"
      strokeLinejoin="round"
      className="h-4 w-4 shrink-0 text-[var(--color-border-accent)]"
      aria-hidden="true"
    >
      <path d="m9 6 6 6-6 6" />
    </svg>
  );
}

function obtenerChipRol(rol: string | null): string | null {
  if (!rol) {
    return null;
  }

  if (esRolAdmin(rol)) {
    return "Equipo";
  }

  if (esRolPublicador(rol)) {
    return "Publicador";
  }

  return null;
}
