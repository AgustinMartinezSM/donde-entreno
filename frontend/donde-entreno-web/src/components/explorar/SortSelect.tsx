"use client";

import { useRouter } from "next/navigation";

type SortSelectProps = {
  textoBuscado?: string;
  ordenActual?: string;

  ciudadIdActual?: string;
  ciudadSlugActual?: string;
  barrioIdActual?: string;
  deporteSlugActual?: string;
  nivelActual?: string;
  modalidadActual?: string;
};

export function SortSelect({
  textoBuscado = "",
  ordenActual = "",
  ciudadIdActual = "",
  ciudadSlugActual = "",
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

    // Agregamos orden solo si tiene valor
    if (nuevoOrden) {
      params.set("orden", nuevoOrden);
    }

    // Al cambiar orden, volvemos a la primera página
    params.set("page", "0");

    router.push(`/explorar?${params.toString()}`);
  }

  return (
    <div className="flex w-full flex-col gap-2 sm:max-w-xs lg:w-72">
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
        className="min-h-12 w-full rounded-[var(--radius-md)] border border-[#BFDDEA] bg-white px-4 text-sm font-bold text-[var(--color-text)] shadow-sm outline-none transition duration-200 ease-out hover:border-[var(--color-accent)] focus:border-[var(--color-accent)] focus:ring-2 focus:ring-[#DDEAF3]"
      >
        <option value="">Más relevantes</option>
        <option value="precio_asc">Menor precio</option>
        <option value="precio_desc">Mayor precio</option>
      </select>
    </div>
  );
}
