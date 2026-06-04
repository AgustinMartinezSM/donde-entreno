"use client";

import { useRouter } from "next/navigation";

type SortSelectProps = {
  textoBuscado?: string;
  ordenActual?: string;

  ciudadIdActual?: string;
  barrioIdActual?: string;
  deporteSlugActual?: string;
  nivelActual?: string;
  modalidadActual?: string;
};

export function SortSelect({
  textoBuscado = "",
  ordenActual = "",
  ciudadIdActual = "",
  barrioIdActual = "",
  deporteSlugActual = "",
  nivelActual = "",
  modalidadActual = "",
}: SortSelectProps) {
  const router = useRouter();

  function manejarCambioOrden(evento: React.ChangeEvent<HTMLSelectElement>) {
    const nuevoOrden = evento.target.value;

    const params = new URLSearchParams();

    // Conservamos búsqueda
    if (textoBuscado) {
      params.set("texto", textoBuscado);
    }

    // Conservamos filtros activos
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

    // Agregamos orden solo si tiene valor
    if (nuevoOrden) {
      params.set("orden", nuevoOrden);
    }

    // Al cambiar orden, volvemos a la primera página
    params.set("page", "0");

    router.push(`/explorar?${params.toString()}`);
  }

  return (
    <div className="mt-6 flex w-full flex-col gap-2 sm:max-w-xs">
      <label
        htmlFor="orden"
        className="text-sm font-bold text-[var(--color-primary)]"
      >
        Ordenar por
      </label>

      <select
        id="orden"
        value={ordenActual}
        onChange={manejarCambioOrden}
        className="min-h-12 w-full rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-surface)] px-4 text-sm font-bold text-[var(--color-text)] outline-none focus:border-[var(--color-accent)]"      >
        <option value="">Más relevantes</option>
        <option value="precio_asc">Menor precio</option>
        <option value="precio_desc">Mayor precio</option>
      </select>
    </div>
  );
}