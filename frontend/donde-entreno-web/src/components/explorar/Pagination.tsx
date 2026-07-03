import { AppButton } from "../ui/AppButton";
import { AppLinkButton } from "../ui/AppLinkButton";
import { SurfaceCard } from "../ui/SurfaceCard";

type PaginationProps = {
  paginaActual: number;
  totalPaginas: number;
  textoBuscado?: string;
  ordenActual?: string;

  ciudadIdActual?: string;
  ciudadSlugActual?: string;
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
  ciudadSlugActual = "",
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

    if (ciudadSlugActual) {
      params.set("ciudadSlug", ciudadSlugActual);
    } else if (ciudadIdActual) {
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
    <SurfaceCard className="mt-8 flex flex-col items-center justify-between gap-3 p-4 sm:flex-row">
      {tienePaginaAnterior ? (
        <AppLinkButton
          href={crearHref(paginaActual - 1)}
          variant="secondary"
          fullWidth
          className="sm:w-auto"
        >
          Anterior
        </AppLinkButton>
      ) : (
        <AppButton
          disabled
          variant="secondary"
          fullWidth
          className="sm:w-auto"
        >
          Anterior
        </AppButton>
      )}

      <p className="text-sm font-bold text-[var(--color-muted)]">
        Página {paginaActual + 1} de {totalPaginas}
      </p>

      {tienePaginaSiguiente ? (
        <AppLinkButton
          href={crearHref(paginaActual + 1)}
          variant="primary"
          fullWidth
          className="sm:w-auto"
        >
          Siguiente
        </AppLinkButton>
      ) : (
        <AppButton
          disabled
          variant="primary"
          fullWidth
          className="sm:w-auto"
        >
          Siguiente
        </AppButton>
      )}
    </SurfaceCard>
  );
}
