import type { ReactNode } from "react";
import { AppLinkButton } from "../ui/AppLinkButton";
import { SurfaceCard } from "../ui/SurfaceCard";

type ErrorStateProps = {
  titulo?: ReactNode;
  descripcion?: ReactNode;
  mostrarBotonInicio?: boolean;
  mostrarBotonExplorar?: boolean;
};

export function ErrorState({
  titulo = "Algo salió mal",
  descripcion = "No pudimos cargar la información en este momento. Probá nuevamente en unos minutos.",
  mostrarBotonInicio = true,
  mostrarBotonExplorar = false,
}: ErrorStateProps) {
  return (
    <SurfaceCard className="p-6 text-center">
      {/* Título principal del error */}
      <h2 className="text-2xl font-extrabold text-[var(--color-primary)]">
        {titulo}
      </h2>

      {/* Mensaje explicativo */}
      <p className="mx-auto mt-3 max-w-xl text-sm leading-6 text-[var(--color-muted)]">
        {descripcion}
      </p>

      {/* Acciones opcionales */}
      <div className="mt-6 flex flex-col gap-3 sm:flex-row sm:justify-center">
        {mostrarBotonExplorar && (
          <AppLinkButton href="/explorar" variant="primary">
            Explorar actividades
          </AppLinkButton>
        )}

        {mostrarBotonInicio && (
          <AppLinkButton href="/" variant="secondary">
            Volver al inicio
          </AppLinkButton>
        )}
      </div>
    </SurfaceCard>
  );
}
