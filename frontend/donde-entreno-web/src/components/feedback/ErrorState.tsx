import Link from "next/link";

type ErrorStateProps = {
  titulo?: string;
  descripcion?: string;
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
    <div className="rounded-[var(--radius-xl)] border border-[var(--color-border)] bg-[var(--color-surface)] p-6 text-center shadow-[var(--shadow-card)]">
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
          <Link
            href="/explorar"
            className="rounded-[var(--radius-md)] bg-[var(--color-primary)] px-5 py-3 text-sm font-bold text-white shadow-[var(--shadow-button)]"
          >
            Explorar actividades
          </Link>
        )}

        {mostrarBotonInicio && (
          <Link
            href="/"
            className="rounded-[var(--radius-md)] border border-[var(--color-border)] px-5 py-3 text-sm font-bold text-[var(--color-primary)]"
          >
            Volver al inicio
          </Link>
        )}
      </div>
    </div>
  );
}