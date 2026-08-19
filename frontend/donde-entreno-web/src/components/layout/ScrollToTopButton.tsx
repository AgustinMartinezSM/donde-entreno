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
    /*
      A la DERECHA en mobile, no a la izquierda.

      Estaba en `left-4`, y el launcher de Dondi se puso también en
      `left-4` dando por hecho lo contrario: que la izquierda estaba
      libre porque la derecha la ocupaba este botón. Medido a 390×844,
      los dos caían en x16,y704 — con Dondi en z-50 y 56px tapando por
      completo a este de z-40 y 48px, o sea que volver arriba era
      inalcanzable en mobile. Con esto la premisa de aquella decisión
      pasa a ser cierta: Dondi a la izquierda, volver arriba a la
      derecha. En desktop los dos van a la derecha, separados por el
      bottom (este a 20px, Dondi a 84px).
    */
    <button
      type="button"
      onClick={volverArriba}
      aria-label="Volver arriba"
      className="fixed bottom-[calc(5.75rem+env(safe-area-inset-bottom))] right-4 z-40 flex h-12 w-12 items-center justify-center rounded-full border-2 border-[var(--color-secondary)] bg-[var(--color-brand)] text-xl font-extrabold text-[var(--color-secondary)] shadow-[0_12px_30px_rgba(15,61,94,0.28)] ring-4 ring-[#2EB872]/15 transition hover:-translate-y-1 hover:scale-105 active:scale-95 lg:bottom-5 lg:right-5 lg:z-50"
    >
      ↑
    </button>
  );
}
