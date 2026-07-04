"use client";

import { useEffect } from "react";
import { Header } from "../components/layout/Header";
import { ErrorState } from "../components/feedback/ErrorState";
import { BrandName } from "../components/brand/BrandName";
import { AppButton } from "../components/ui/AppButton";

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
            descripcion={
              <>
                No pudimos mostrar esta parte de{" "}
                <BrandName className="inline font-bold" />. Podés intentar de
                nuevo o seguir explorando actividades.
              </>
            }
            mostrarBotonInicio
            mostrarBotonExplorar
          />

          <div className="mt-4 flex justify-center">
            <AppButton type="button" onClick={reset}>
              Intentar nuevamente
            </AppButton>
          </div>
        </div>
      </section>
    </main>
  );
}
