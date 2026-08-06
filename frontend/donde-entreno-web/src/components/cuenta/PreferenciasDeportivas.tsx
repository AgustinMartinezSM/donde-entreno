"use client";

import { CATALOGO_DEPORTES_ASISTENTE } from "../../lib/asistente/conocimiento";
import {
  alternarDeporteFavorito,
  useDeportesFavoritos,
} from "../../lib/preferenciasDeportivas";
import { AppLinkButton } from "../ui/AppLinkButton";
import { SectionHeader } from "../ui/SectionHeader";
import { SurfaceCard } from "../ui/SurfaceCard";

/*
  Preferencias deportivas del usuario (V1 local).

  El catálogo de chips es el espejo del seed real (el mismo que usa el
  asistente), así cada acceso rápido lleva a resultados reales.
*/
export function PreferenciasDeportivas() {
  const favoritos = useDeportesFavoritos();

  const deportesFavoritos = CATALOGO_DEPORTES_ASISTENTE.filter((deporte) =>
    favoritos.includes(deporte.slug)
  );

  return (
    <SurfaceCard className="p-6 sm:p-8">
      <SectionHeader
        eyebrow="Preferencias"
        title="Tus deportes"
        description="Marcá los deportes que te interesan para tener accesos directos a sus actividades."
      />

      <div className="mt-6 flex flex-wrap gap-2" role="group" aria-label="Elegir deportes favoritos">
        {CATALOGO_DEPORTES_ASISTENTE.map((deporte) => {
          const seleccionado = favoritos.includes(deporte.slug);

          return (
            <button
              key={deporte.slug}
              type="button"
              onClick={() => alternarDeporteFavorito(deporte.slug)}
              aria-pressed={seleccionado}
              className={`rounded-full px-4 py-2 text-sm font-bold shadow-sm transition duration-200 ease-out hover:-translate-y-0.5 active:scale-[0.98] ${
                seleccionado
                  ? "bg-[var(--color-secondary)] text-white"
                  : "border border-[#BFDDEA] bg-white text-[var(--color-primary)] hover:border-[var(--color-primary)] hover:bg-[#F8FCFE]"
              }`}
            >
              {deporte.nombre}
            </button>
          );
        })}
      </div>

      {deportesFavoritos.length > 0 ? (
        <div className="mt-6 border-t border-[#DDEAF3] pt-5">
          <p className="text-sm font-bold text-[var(--color-primary)]">
            Accesos rápidos a tus deportes:
          </p>
          <div className="mt-3 flex flex-wrap gap-2">
            {deportesFavoritos.map((deporte) => (
              <AppLinkButton
                key={deporte.slug}
                href={`/explorar?deporteSlug=${encodeURIComponent(deporte.slug)}&page=0`}
                variant="secondary"
                size="sm"
                className="rounded-full"
              >
                Ver {deporte.nombre}
              </AppLinkButton>
            ))}
          </div>
        </div>
      ) : (
        <p className="mt-6 text-sm text-[var(--color-muted)]">
          Todavía no marcaste ninguno. Tus elecciones se guardan en este
          dispositivo.
        </p>
      )}
    </SurfaceCard>
  );
}
