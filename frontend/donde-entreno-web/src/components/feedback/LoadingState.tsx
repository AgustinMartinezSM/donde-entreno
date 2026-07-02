import type { ReactNode } from "react";
import { SurfaceCard } from "../ui/SurfaceCard";

type LoadingStateProps = {
  titulo?: ReactNode;
  descripcion?: ReactNode;
};

export function LoadingState({
  titulo = "Cargando contenido",
  descripcion = "Estamos buscando la información disponible.",
}: LoadingStateProps) {
  return (
    <SurfaceCard className="p-6 text-center">
      {/* Indicador visual simple de carga */}
      <div className="mx-auto h-10 w-10 animate-spin rounded-full border-4 border-[#DDEAF3] border-t-[var(--color-secondary)]" />

      <h2 className="mt-5 text-2xl font-extrabold text-[var(--color-primary)]">
        {titulo}
      </h2>

      <p className="mx-auto mt-3 max-w-xl text-sm leading-6 text-[var(--color-muted)]">
        {descripcion}
      </p>
    </SurfaceCard>
  );
}
