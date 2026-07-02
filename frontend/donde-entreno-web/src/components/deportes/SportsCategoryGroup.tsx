import type { Deporte } from "../../types/deporte";
import { AppButton } from "../ui/AppButton";
import { SectionHeader } from "../ui/SectionHeader";
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
      <SectionHeader
        title={titulo}
        description={
          totalDeportes === 1
            ? "1 deporte disponible"
            : `${totalDeportes} deportes disponibles`
        }
        className="mb-4"
      />

      <div id={idGrilla} className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {deportes.map((deporte) => (
          <SportCatalogCard key={deporte.id} deporte={deporte} />
        ))}
      </div>

      {mostrarControlExpansion ? (
        <div className="mt-5 flex justify-start sm:justify-end">
          <AppButton
            aria-expanded={estaExpandida}
            aria-controls={idGrilla}
            onClick={onAlternarExpansion}
            variant="secondary"
            size="md"
          >
            {estaExpandida ? "Ver menos" : `Ver ${cantidadRestante} más`}
          </AppButton>
        </div>
      ) : null}
    </section>
  );
}
