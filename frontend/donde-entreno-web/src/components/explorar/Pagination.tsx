import Link from "next/link";

type PaginationProps = {
  paginaActual: number;
  totalPaginas: number;
  textoBuscado?: string;
  ordenActual?: string;

  ciudadIdActual?: string;
  barrioIdActual?: string;
  deporteSlugActual?: string;
  nivelActual?: string;
  modalidadActual?: string;
};

export function Pagination({
  paginaActual,
  totalPaginas,
  textoBuscado = "",
  ordenActual = "",
  ciudadIdActual = "",
  barrioIdActual = "",
  deporteSlugActual = "",
  nivelActual = "",
  modalidadActual = "",
}: PaginationProps) {
  /*
    Si hay una sola página o ninguna, no mostramos paginación.
  */
  if (totalPaginas <= 1) {
    return null;
  }

  const tienePaginaAnterior = paginaActual > 0;
  const tienePaginaSiguiente = paginaActual < totalPaginas - 1;

  /*
    Armamos una función para generar links manteniendo el texto buscado.
    Si el usuario buscó "boxeo", los links conservan texto=boxeo.
  */
  function crearHref(numeroPagina: number) {
    const params = new URLSearchParams();

    if (textoBuscado) {
      params.set("texto", textoBuscado);
    }

    if (ordenActual) {
      params.set("orden", ordenActual);
    }

    if (ciudadIdActual) {
      params.set("ciudadId", ciudadIdActual);
    }

    if (barrioIdActual) {
      params.set("barrioId", barrioIdActual);
    }

    if (deporteSlugActual) {
      params.set("deporteSlug", deporteSlugActual);
    }

    if (nivelActual) {
      params.set("nivel", nivelActual);
    }

    if (modalidadActual) {
      params.set("modalidad", modalidadActual);
    }

    params.set("page", String(numeroPagina));

    return `/explorar?${params.toString()}`;
  }

  return (
    <div className="mt-8 flex flex-col items-center justify-between gap-3 rounded-[var(--radius-lg)] border border-[var(--color-border)] bg-[var(--color-surface)] p-4 shadow-[var(--shadow-card)] sm:flex-row">
      {tienePaginaAnterior ? (
        <Link
          href={crearHref(paginaActual - 1)}
          className="w-full rounded-[var(--radius-md)] border border-[var(--color-border)] px-4 py-3 text-center text-sm font-bold text-[var(--color-primary)] sm:w-auto"
        >
          Anterior
        </Link>
      ) : (
        <button
          disabled
          className="w-full cursor-not-allowed rounded-[var(--radius-md)] border border-[var(--color-border)] px-4 py-3 text-sm font-bold text-[var(--color-muted)] opacity-60 sm:w-auto"
        >
          Anterior
        </button>
      )}

      <p className="text-sm font-bold text-[var(--color-muted)]">
        Página {paginaActual + 1} de {totalPaginas}
      </p>

      {tienePaginaSiguiente ? (
        <Link
          href={crearHref(paginaActual + 1)}
          className="w-full rounded-[var(--radius-md)] bg-[var(--color-primary)] px-4 py-3 text-center text-sm font-bold text-white shadow-[var(--shadow-button)] sm:w-auto"
        >
          Siguiente
        </Link>
      ) : (
        <button
          disabled
          className="w-full cursor-not-allowed rounded-[var(--radius-md)] bg-slate-300 px-4 py-3 text-sm font-bold text-white sm:w-auto"
        >
          Siguiente
        </button>
      )}
    </div>
  );
}