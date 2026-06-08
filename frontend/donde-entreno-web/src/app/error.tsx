"use client";

import { useEffect } from "react";
import { Header } from "../components/layout/Header";
import { ErrorState } from "../components/feedback/ErrorState";

type GlobalErrorProps = {
  error: Error & {
    digest?: string;
  };
  reset: () => void;
};

export default function GlobalError({ error, reset }: GlobalErrorProps) {
  useEffect(() => {
    /*
      Dejamos el error en consola para poder revisarlo durante desarrollo.
      En producción, más adelante podríamos enviarlo a un sistema de logs.
    */
    console.error("Error inesperado en DondeEntreno:", error);
  }, [error]);

  return (
    <main className="min-h-screen bg-[var(--color-bg)] text-[var(--color-text)]">
      <section className="mx-auto w-full max-w-6xl px-4 py-6">
        <Header />

        <div className="py-10">
          <ErrorState
            titulo="Ocurrió un error inesperado"
            descripcion="Algo falló mientras cargábamos esta parte de DondeEntreno. Podés intentar nuevamente o volver al inicio."
            mostrarBotonInicio
            mostrarBotonExplorar
          />

          <div className="mt-4 flex justify-center">
            <button
              type="button"
              onClick={reset}
              className="rounded-[var(--radius-md)] bg-[var(--color-primary)] px-5 py-3 text-sm font-bold text-white shadow-[var(--shadow-button)]"
            >
              Intentar nuevamente
            </button>
          </div>
        </div>
      </section>
    </main>
  );
}