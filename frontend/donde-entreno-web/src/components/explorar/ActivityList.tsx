import type { Actividad } from "../../types/actividad";
import { ActivityCard } from "./ActivityCard";

type ActivityListProps = {
  actividades: Actividad[];

  /*
    Título y descripción opcionales.
    Si no los mandamos, usa textos pensados para la home.
  */
  titulo?: string;
  descripcion?: string;
};

export function ActivityList({
  actividades,
  titulo = "Actividades destacadas",
  descripcion = "Primeras actividades disponibles en DondeEntreno.",
}: ActivityListProps) {
  if (actividades.length === 0) {
    return (
      <div className="mt-10 rounded-[var(--radius-lg)] border border-[var(--color-border)] bg-[var(--color-surface)] p-6 text-center shadow-[var(--shadow-card)]">
        <h2 className="text-lg font-bold text-[var(--color-primary)]">
          No encontramos actividades
        </h2>

        <p className="mt-2 text-sm text-[var(--color-muted)]">
          Probá con otra búsqueda o revisá los filtros aplicados.
        </p>
      </div>
    );
  }

  return (
    <section className="mt-10 sm:mt-12">
      <div className="mb-5">
        <h2 className="text-2xl font-extrabold text-[var(--color-primary)]">
          {titulo}
        </h2>

        <p className="mt-2 text-sm text-[var(--color-muted)]">
          {descripcion}
        </p>
      </div>

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {actividades.map((actividad) => (
          <ActivityCard key={actividad.id} actividad={actividad} />
        ))}
      </div>
    </section>
  );
}