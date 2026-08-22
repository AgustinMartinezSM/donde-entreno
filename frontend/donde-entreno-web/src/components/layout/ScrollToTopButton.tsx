"use client";

import { useEffect, useState } from "react";

/*
  Volver arriba (rediseño 2026-08-22).

  Antes: círculo navy con borde y halo verdes y una flecha de texto
  "↑", que aparecía y desaparecía de golpe (montaje condicional). Ahora
  comparte el lenguaje de los flotantes de la app — el gradiente de
  marca de los botones primarios, sombra `shadow-lifted`, icono SVG — y
  queda SIEMPRE montado: la visibilidad es una transición suave de
  opacidad y desplazamiento, con pointer-events-none mientras está
  oculto para no robar taps invisibles.

  Posiciones intactas a propósito: la coreografía con Dondi (este a la
  derecha en mobile, Dondi a la izquierda; en desktop apilados a la
  derecha) ya está medida y documentada — acá solo cambia la piel.
*/
export function ScrollToTopButton() {
  const [visible, setVisible] = useState(false);

  useEffect(() => {
    function controlarScroll() {
      setVisible(window.scrollY > 500);
    }

    controlarScroll();
    window.addEventListener("scroll", controlarScroll, { passive: true });

    return () => {
      window.removeEventListener("scroll", controlarScroll);
    };
  }, []);

  function volverArriba() {
    window.scrollTo({
      top: 0,
      behavior: "smooth",
    });
  }

  return (
    <button
      type="button"
      onClick={volverArriba}
      aria-label="Volver arriba"
      aria-hidden={!visible}
      tabIndex={visible ? 0 : -1}
      className={`gradient-cta gradient-cta-hover fixed bottom-[calc(5.75rem+env(safe-area-inset-bottom))] right-4 z-40 flex h-12 w-12 items-center justify-center rounded-full bg-[var(--color-brand)] text-white shadow-lifted ring-1 ring-white/25 transition-all duration-300 ease-out focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-[#4FB3D9]/50 active:scale-95 lg:bottom-5 lg:right-5 lg:z-50 ${
        visible
          ? "translate-y-0 opacity-100 hover:-translate-y-0.5"
          : "pointer-events-none translate-y-3 opacity-0"
      }`}
    >
      <svg
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        strokeWidth="2.5"
        strokeLinecap="round"
        strokeLinejoin="round"
        className="h-5 w-5"
        aria-hidden="true"
      >
        <path d="m5 13 7-7 7 7" />
        <path d="M12 6v14" />
      </svg>
    </button>
  );
}
