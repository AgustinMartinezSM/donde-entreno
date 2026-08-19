"use client";

import Link from "next/link";
import { useEffect, useId, useRef, useState } from "react";
import type { ReactNode } from "react";

type MenuDesplegableProps = {
  /* Lo que se ve dentro del botón que abre el menú. */
  disparador: ReactNode;
  /* Nombre accesible del botón (el contenido visible suele ser un ícono). */
  etiqueta: string;
  /*
    El contenido recibe `cerrar` para que cada opción pueda cerrar el
    menú al elegirla: un link que navega dentro de la misma página (una
    solapa, por ejemplo) no desmonta nada y el panel quedaría abierto.
  */
  children: (cerrar: () => void) => ReactNode;
  alineacion?: "izquierda" | "derecha";
  className?: string;
};

/*
  Menú desplegable simple: un botón que muestra un panel con acciones.

  A propósito NO usa role="menu"/"menuitem": esa semántica promete
  navegación por flechas y foco gestionado, que acá no implementamos. Con
  links y botones reales dentro de un panel, el Tab recorre las opciones
  como en cualquier página y no le mentimos al lector de pantalla. Es el
  mismo criterio que usan las solapas del perfil.

  Sí resuelve lo que un menú necesita para no ser una trampa: cierra con
  Escape y con un click afuera, y devuelve el foco al disparador.
*/
export function MenuDesplegable({
  disparador,
  etiqueta,
  children,
  alineacion = "derecha",
  className = "",
}: MenuDesplegableProps) {
  const [abierto, setAbierto] = useState(false);
  const contenedorRef = useRef<HTMLDivElement | null>(null);
  const disparadorRef = useRef<HTMLButtonElement | null>(null);
  const idPanel = useId();

  useEffect(() => {
    if (!abierto) {
      return;
    }

    function alApuntarAfuera(evento: PointerEvent) {
      if (
        evento.target instanceof Node &&
        !contenedorRef.current?.contains(evento.target)
      ) {
        setAbierto(false);
      }
    }

    /*
      pointerdown y no click: si el usuario abre el menú y toca un link
      de la página de atrás, queremos que el menú se cierre antes de que
      el link navegue, no después.
    */
    document.addEventListener("pointerdown", alApuntarAfuera);

    return () => {
      document.removeEventListener("pointerdown", alApuntarAfuera);
    };
  }, [abierto]);

  function cerrarYDevolverFoco() {
    setAbierto(false);
    disparadorRef.current?.focus();
  }

  return (
    <div
      ref={contenedorRef}
      /*
        min-w-0: el contenedor se usa dentro de barras flex que pueden
        apretarlo. Sin esto, el disparador conserva su ancho de contenido
        y se desborda del hueco que le queda.
      */
      className="relative min-w-0"
      onKeyDown={(evento) => {
        if (evento.key === "Escape" && abierto) {
          evento.stopPropagation();
          cerrarYDevolverFoco();
        }
      }}
    >
      <button
        ref={disparadorRef}
        type="button"
        onClick={() => setAbierto((valor) => !valor)}
        aria-label={etiqueta}
        aria-expanded={abierto}
        aria-controls={abierto ? idPanel : undefined}
        className={className}
      >
        {disparador}
      </button>

      {abierto ? (
        <div
          id={idPanel}
          className={`absolute top-[calc(100%+0.5rem)] z-50 min-w-56 overflow-hidden rounded-[18px] border border-[var(--color-border-soft)] bg-white p-2 shadow-[0_22px_55px_rgba(12,52,80,0.18)] ${
            alineacion === "derecha" ? "right-0" : "left-0"
          }`}
        >
          {children(() => setAbierto(false))}
        </div>
      ) : null}
    </div>
  );
}

/*
  Fila del menú. Con `href` es un link real y con `onClick` un botón
  real: nunca un div con handler encima.
*/
export function OpcionMenu({
  children,
  href,
  onClick,
  destacada = false,
}: {
  children: ReactNode;
  href?: string;
  onClick?: () => void;
  destacada?: boolean;
}) {
  const clase = `flex w-full min-h-11 items-center gap-2.5 rounded-[12px] px-3 py-2.5 text-left text-sm font-bold transition duration-200 ease-out ${
    destacada
      ? "text-[var(--color-primary)] hover:bg-[var(--color-surface-soft)]"
      : "text-[var(--color-muted)] hover:bg-[var(--color-bg)] hover:text-[var(--color-primary)]"
  }`;

  if (href) {
    return (
      <Link href={href} onClick={onClick} className={clase}>
        {children}
      </Link>
    );
  }

  return (
    <button type="button" onClick={onClick} className={clase}>
      {children}
    </button>
  );
}

export function SeparadorMenu() {
  return <hr className="my-1.5 border-t border-[#EDF3F8]" />;
}
