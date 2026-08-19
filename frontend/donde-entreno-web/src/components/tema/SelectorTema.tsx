"use client";

import { useSyncExternalStore } from "react";

import {
  guardarPreferenciaTema,
  leerPreferenciaTema,
  suscribirseATema,
} from "../../lib/preferenciaTema";
import type { PreferenciaTema } from "../../lib/preferenciaTema";

const OPCIONES: Array<{ valor: PreferenciaTema; etiqueta: string }> = [
  { valor: "system", etiqueta: "Sistema" },
  { valor: "light", etiqueta: "Claro" },
  { valor: "dark", etiqueta: "Oscuro" },
];

/*
  Selector de apariencia: tres pastillas (Sistema / Claro / Oscuro).
  Vive dentro de los menús de cuenta; elegir NO cierra el menú a
  propósito, para poder comparar los temas sin reabrirlo. Botones reales
  con aria-pressed — el efecto es inmediato y visible, no hace falta
  anunciar más.
*/
export function SelectorTema() {
  const preferencia = useSyncExternalStore(
    suscribirseATema,
    leerPreferenciaTema,
    () => "system" as const
  );

  return (
    <div>
      <p className="px-1 pb-1.5 text-[11px] font-extrabold uppercase tracking-[0.16em] text-[var(--color-secondary)]">
        Apariencia
      </p>

      <div
        role="group"
        aria-label="Apariencia de la aplicación"
        className="grid grid-cols-3 gap-1 rounded-[14px] border border-[var(--color-border-soft)] bg-[var(--color-bg)] p-1"
      >
        {OPCIONES.map((opcion) => {
          const activa = opcion.valor === preferencia;

          return (
            <button
              key={opcion.valor}
              type="button"
              onClick={() => guardarPreferenciaTema(opcion.valor)}
              aria-pressed={activa}
              className={`min-h-9 rounded-[10px] px-2 text-xs font-extrabold transition duration-200 ease-out focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-[#4FB3D9]/30 ${
                activa
                  ? "bg-[var(--color-surface)] text-[var(--color-primary)] shadow-sm ring-1 ring-[var(--color-border-accent)]"
                  : "text-[var(--color-muted)] hover:text-[var(--color-primary)]"
              }`}
            >
              {opcion.etiqueta}
            </button>
          );
        })}
      </div>
    </div>
  );
}
