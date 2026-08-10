"use client";

import type { TabPerfil } from "./usePerfilDeportivo";

/*
  Cuatro solapas de contenido. "Ajustes" ya no está: la configuración se
  movió al menú de la cabecera, así que las solapas quedaron para lo que
  la persona viene a mirar.

  Las etiquetas son cortas a propósito: a 320px son cuatro columnas de
  ~70px y "Mis deportes" no entra en una línea.
*/
const TABS: Array<{ clave: TabPerfil; etiqueta: string }> = [
  { clave: "para-vos", etiqueta: "Para vos" },
  { clave: "guardados", etiqueta: "Guardados" },
  { clave: "siguiendo", etiqueta: "Siguiendo" },
  { clave: "deportes", etiqueta: "Deportes" },
];

type TabsPerfilProps = {
  tabActiva: TabPerfil;
  onCambiar: (tab: TabPerfil) => void;
};

/*
  Solapas con botones y aria-current, no con roles de tablist: no
  implementamos navegación por flechas, así que prometer la semántica de
  tabs sería peor que no usarla.

  Grilla de cuatro en mobile en vez de fila con scroll: a 375px las
  cuatro no entran en el ancho y la última quedaba fuera de pantalla, sin
  nada que insinuara que había más.
*/
export function TabsPerfil({ tabActiva, onCambiar }: TabsPerfilProps) {
  return (
    <nav
      className="mt-8 grid grid-cols-4 border-b border-[#D9E2EC] pb-px sm:flex sm:gap-2"
      aria-label="Secciones de mi espacio deportivo"
    >
      {TABS.map((tab) => {
        const activa = tab.clave === tabActiva;

        return (
          <button
            key={tab.clave}
            type="button"
            onClick={() => onCambiar(tab.clave)}
            aria-current={activa ? "true" : undefined}
            className={`-mb-px min-h-12 shrink-0 border-b-2 px-1 py-3 text-xs font-extrabold transition duration-200 ease-out focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-[#4FB3D9]/30 sm:px-4 sm:text-sm ${
              activa
                ? "border-[var(--color-secondary)] text-[var(--color-primary)]"
                : "border-transparent text-[var(--color-muted)] hover:border-[#BFDDEA] hover:text-[var(--color-primary)]"
            }`}
          >
            {tab.etiqueta}
          </button>
        );
      })}
    </nav>
  );
}
