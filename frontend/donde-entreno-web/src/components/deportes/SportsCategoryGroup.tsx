import type { Deporte } from "../../types/deporte";
import { SportCatalogCard } from "./SportCatalogCard";

type SportsCategoryGroupProps = {
  titulo: string;
  deportes: Deporte[];
  totalDeportes: number;
  idGrilla: string;
  mostrarControlExpansion: boolean;
  estaExpandida: boolean;
  cantidadRestante: number;
  onAlternarExpansion: () => void;
};

export function SportsCategoryGroup({
  titulo,
  deportes,
  totalDeportes,
  idGrilla,
  mostrarControlExpansion,
  estaExpandida,
  cantidadRestante,
  onAlternarExpansion,
}: SportsCategoryGroupProps) {
  return (
    <section>
      <div className="mb-4 flex flex-col gap-2 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <h2 className="text-2xl font-extrabold text-[var(--color-primary)]">
            {titulo}
          </h2>
          <p className="mt-1 text-sm font-bold text-[var(--color-muted)]">
            {totalDeportes === 1
              ? "1 deporte disponible"
              : `${totalDeportes} deportes disponibles`}
          </p>
        </div>
      </div>

      <div id={idGrilla} className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {deportes.map((deporte) => (
          <SportCatalogCard key={deporte.id} deporte={deporte} />
        ))}
      </div>

      {mostrarControlExpansion ? (
        <div className="mt-5 flex justify-start sm:justify-end">
          <button
            type="button"
            aria-expanded={estaExpandida}
            aria-controls={idGrilla}
            onClick={onAlternarExpansion}
            className="rounded-[var(--radius-md)] border border-[#BFDDEA] bg-white px-4 py-3 text-sm font-bold text-[var(--color-primary)] shadow-sm transition duration-200 ease-out hover:-translate-y-0.5 hover:border-[var(--color-primary)] hover:bg-[#F8FCFE] active:scale-[0.98]"
          >
            {estaExpandida ? "Ver menos" : `Ver ${cantidadRestante} más`}
          </button>
        </div>
      ) : null}
    </section>
  );
}
