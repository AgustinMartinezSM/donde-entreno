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
  descripcion = "Primeras actividades disponibles para empezar a moverte.",
}: ActivityListProps) {
  if (actividades.length === 0) {
    return (
      <div className="mt-10 rounded-[var(--radius-xl)] border border-[#DDEAF3] bg-white/90 p-7 text-center shadow-[0_18px_45px_rgba(12,52,80,0.09)]">
        <h2 className="text-2xl font-extrabold text-[var(--color-primary)]">
          No encontramos actividades con esos filtros
        </h2>

        <p className="mx-auto mt-3 max-w-xl text-sm leading-6 text-[var(--color-muted)]">
          Probá con otra búsqueda, cambiá la zona o revisá los filtros
          aplicados.
        </p>
      </div>
    );
  }

  return (
    <section className="mt-10 rounded-[var(--radius-xl)] border border-[#DDEAF3] bg-white/70 p-4 shadow-[0_16px_40px_rgba(12,52,80,0.08)] sm:mt-12 sm:p-6">
      <div className="mb-6">
        <p className="text-sm font-bold uppercase tracking-[0.2em] text-[var(--color-secondary)]">
          Actividades
        </p>
        <h2 className="mt-2 text-2xl font-extrabold text-[var(--color-primary)] sm:text-3xl">
          {titulo}
        </h2>

        <p className="mt-2 max-w-2xl text-sm leading-6 text-[var(--color-muted)]">
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
