import Link from "next/link";

import { SectionHeader } from "../ui/SectionHeader";

export type Zona = {
  barrioId: number;
  barrioNombre: string;
  cantidadActividades: number;
};

/*
  Zonas con actividad real (Fase 7).

  Es el valor territorial que se puede dar HOY, sin una sola coordenada
  cargada: el barrio ya viaja en cada actividad y el filtro por barrio
  ya funciona. Además convierte al barrio en algo NAVEGABLE — hasta
  ahora era un texto muerto en las cards y en el detalle.

  Si no hay zonas con actividad, la sección no se dibuja.
*/
export function HomeZonas({ zonas }: { zonas: Zona[] }) {
  if (zonas.length === 0) {
    return null;
  }

  return (
    <section className="mt-14 sm:mt-18" aria-labelledby="zonas-titulo">
      <SectionHeader
        eyebrow="Por zona"
        title="Dónde hay más para entrenar"
        description="Los barrios con más actividades publicadas."
        titleId="zonas-titulo"
      />

      <ul className="mt-5 flex flex-wrap gap-2">
        {zonas.map((zona) => (
          <li key={zona.barrioId}>
            <Link
              href={`/explorar?barrioId=${zona.barrioId}`}
              className="inline-flex min-h-10 items-center gap-2 rounded-full border border-[var(--color-border-accent)] bg-[var(--color-surface)] px-4 text-sm font-extrabold text-[var(--color-primary)] transition duration-200 ease-out hover:-translate-y-0.5 hover:border-[var(--color-primary)]"
            >
              {zona.barrioNombre}
              <span className="rounded-full bg-[var(--color-info-soft)] px-2 py-0.5 text-xs font-bold text-[var(--color-info-deep)]">
                {zona.cantidadActividades}
              </span>
            </Link>
          </li>
        ))}
      </ul>
    </section>
  );
}
