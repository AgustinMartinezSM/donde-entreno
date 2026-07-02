import Image from "next/image";
import Link from "next/link";

import type { Actividad } from "../../types/actividad";
import { API_BASE_URL } from "../../lib/apiConfig";
import { ActivityImage } from "../actividad/ActivityImage";
import { obtenerImagenActividad } from "../../lib/activityImages";

type ActivityCardProps = {
  actividad: Actividad;
};

export function ActivityCard({ actividad }: ActivityCardProps) {
  /*
    Si la actividad trae imagen propia desde el backend,
    armamos la URL completa usando API_BASE_URL.

    Si no trae imagen propia, dejamos null para que después
    obtenerImagenActividad use la imagen default según el deporte.
  */
  const imagenBackend = actividad.imagenPrincipalUrl
    ? `${API_BASE_URL}${actividad.imagenPrincipalUrl}`
    : null;

  /*
    Definimos qué imagen mostrar en la card.

    Prioridad:
    1. Imagen propia desde backend.
    2. Imagen default por deporteSlug.
    3. Placeholder general.
  */
  const imagenUrl = obtenerImagenActividad({
    imagenBackend,
    deporteSlug: actividad.deporteSlug,
  });

  return (
    <article className="group overflow-hidden rounded-[var(--radius-lg)] border border-[#DDEAF3] bg-[var(--color-surface)] p-3 shadow-[var(--shadow-card)] transition duration-200 ease-out hover:-translate-y-1 hover:border-[#BFDDEA] hover:shadow-[0_22px_55px_rgba(12,52,80,0.14)] active:scale-[0.995]">
      <ActivityImage
        src={imagenUrl}
        alt={actividad.titulo || actividad.deporteNombre || "Actividad deportiva"}
        fallbackText={actividad.deporteNombre || "Actividad"}
        heightClassName="h-44"
      />

      <div className="p-2 pt-4">
        <h3 className="line-clamp-2 text-lg font-extrabold text-[var(--color-primary)]">
          {actividad.titulo}
        </h3>

        <p className="mt-2 text-sm font-bold text-[var(--color-muted)]">
          {actividad.perfilPublicadorNombre || "Publicador no informado"}
        </p>

        <div className="mt-3 flex items-start gap-2 rounded-[var(--radius-md)] bg-[#F8FAFC] px-3 py-2 text-sm font-medium text-[var(--color-text)]">
          <Image
            src="/icons/icon-location.png"
            alt=""
            width={16}
            height={16}
            aria-hidden="true"
            className="mt-0.5 h-4 w-4 shrink-0"
          />

          <p>
            {actividad.barrioNombre || "Barrio sin cargar"}
            {actividad.ciudadNombre ? `, ${actividad.ciudadNombre}` : ""}
          </p>
        </div>

        <div className="mt-3 flex flex-wrap gap-2">
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
          className="mt-5 block w-full rounded-[var(--radius-md)] bg-[var(--color-primary)] px-4 py-3 text-center text-sm font-bold text-white shadow-[var(--shadow-button)] transition duration-200 ease-out hover:-translate-y-0.5 hover:bg-[#0B314D] active:scale-[0.98] group-hover:bg-[#0B314D]"
        >
          Ver detalle
        </Link>
      </div>
    </article>
  );
}
