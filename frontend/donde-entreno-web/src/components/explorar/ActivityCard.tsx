import type { Actividad } from "../../types/actividad";
import Link from "next/link";
import { API_BASE_URL } from "../../lib/apiConfig";
import { ActivityImage } from "../actividad/ActivityImage";

type ActivityCardProps = {
  actividad: Actividad;
};

export function ActivityCard({ actividad }: ActivityCardProps) {

    const imagenUrl = actividad.imagenPrincipalUrl
      ? `${API_BASE_URL}${actividad.imagenPrincipalUrl}`
      : null;

  return (
    <article className="rounded-[var(--radius-lg)] border border-[var(--color-border)] bg-[var(--color-surface)] p-4 shadow-[var(--shadow-card)]">
      {/* Placeholder visual temporal.
          Más adelante acá podemos mostrar imagen real si el backend devuelve una URL. */}
      <ActivityImage
        src={imagenUrl}
        alt={actividad.nombre}
        fallbackText={actividad.deporteNombre || "Actividad"}
        heightClassName="h-36"
      />

      <div className="space-y-2">
        <h3 className="text-lg font-extrabold text-[var(--color-primary)]">
          {actividad.nombre}
        </h3>

        <p className="text-sm text-[var(--color-muted)]">
          {actividad.perfilPublicadorNombre || "Publicador no informado"}
        </p>

        <p className="text-sm font-medium text-[var(--color-text)]">
          {actividad.barrioNombre || "Barrio sin cargar"}
          {actividad.ciudadNombre ? `, ${actividad.ciudadNombre}` : ""}
        </p>

        <div className="flex flex-wrap gap-2 pt-2">
          {actividad.nivel && (
            <span className="rounded-full bg-[#E6F7EF] px-3 py-1 text-xs font-bold text-[#167A4A]">
              {actividad.nivel}
            </span>
          )}

          {actividad.modalidad && (
            <span className="rounded-full bg-[#E8F6FB] px-3 py-1 text-xs font-bold text-[#0F6F8F]">
              {actividad.modalidad}
            </span>
          )}
        </div>

        <Link
          href={`/actividades/${actividad.slug}`}
          className="mt-4 block w-full rounded-[var(--radius-md)] bg-[var(--color-primary)] px-4 py-3 text-center text-sm font-bold text-white shadow-[var(--shadow-button)]"
        >
          Ver detalle
        </Link>


      </div>
    </article>
  );
}