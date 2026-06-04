"use client";

import { useEffect, useState } from "react";

export function ScrollToTopButton() {
  const [mostrarBoton, setMostrarBoton] = useState(false);

  useEffect(() => {
    function controlarScroll() {
      /*
        Si el usuario bajó más de 500px, mostramos el botón.
        Si está cerca de arriba, lo ocultamos.
      */
      setMostrarBoton(window.scrollY > 500);
    }

    window.addEventListener("scroll", controlarScroll);

    /*
      Limpieza del evento cuando el componente se desmonta.
    */
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

  if (!mostrarBoton) {
    return null;
  }

  return (
    <button
      type="button"
      onClick={volverArriba}
      aria-label="Volver arriba"
      className="fixed bottom-5 right-5 z-50 flex h-12 w-12 items-center justify-center rounded-full border-2 border-[var(--color-secondary)] bg-[var(--color-primary)] text-xl font-extrabold text-[var(--color-secondary)] shadow-[0_12px_30px_rgba(0,47,73,0.28)] ring-4 ring-[#00B86B]/10 transition hover:-translate-y-1 hover:scale-105 active:scale-95"
    >
      ↑
    </button>
  );
}