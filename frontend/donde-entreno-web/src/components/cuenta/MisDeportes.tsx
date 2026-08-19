"use client";

import { useMemo } from "react";

import { CATALOGO_DEPORTES_ASISTENTE } from "../../lib/asistente/conocimiento";
import { alternarDeporteFavorito } from "../../lib/preferenciasDeportivas";
import type { Deporte } from "../../types/deporte";
import { AppLinkButton } from "../ui/AppLinkButton";
import { SectionHeader } from "../ui/SectionHeader";
import { SurfaceCard } from "../ui/SurfaceCard";
import { BloqueAsistente } from "./BloqueAsistente";
import type { PerfilDeportivo } from "./usePerfilDeportivo";

type MisDeportesProps = {
  perfil: PerfilDeportivo;
};

/*
  Solapa "Mis deportes": la pieza que personaliza el resto del espacio.

  Lo que se marca acá alimenta las recomendaciones de "Para vos", los
  accesos rápidos de la home y el estado vacío de guardados, así que no
  es una preferencia decorativa: es lo que hace que la app se parezca a
  quien la usa.

  Los deportes se agrupan por categoría porque son 27 y en una sola
  pared de chips no se encuentra nada en mobile. El catálogo es el
  espejo del seed real, así que cada acceso rápido lleva a resultados
  que existen.
*/
export function MisDeportes({ perfil }: MisDeportesProps) {
  const elegidos = perfil.deportesSlugs;

  const porCategoria = useMemo(() => agruparPorCategoria(), []);

  return (
    <div className="grid gap-8">
      <section aria-labelledby="mis-deportes-titulo">
        <SectionHeader
          eyebrow="Personalización"
          title="Mis deportes"
          description="Marcá los deportes que te interesan para personalizar tus recomendaciones."
          titleId="mis-deportes-titulo"
        />

        {elegidos.length > 0 ? (
          <SurfaceCard variant="success" className="mt-5 p-5 sm:p-6">
            <p className="text-xs font-extrabold uppercase tracking-[0.14em] text-[var(--color-success)]">
              Accesos rápidos a tus deportes
            </p>
            <div className="mt-3 flex flex-wrap gap-2">
              {elegidos.map((slug, indice) => (
                <AppLinkButton
                  key={slug}
                  href={construirHrefExplorarDeporte(slug, perfil.ciudadSlug)}
                  variant="secondary"
                  size="sm"
                  className="rounded-full"
                >
                  Ver {perfil.deportesNombres[indice]}
                </AppLinkButton>
              ))}
            </div>
          </SurfaceCard>
        ) : (
          <SurfaceCard variant="info" className="mt-5 p-5 sm:p-6">
            <p className="text-sm leading-6 text-[var(--color-primary)]">
              Elegí algunos deportes para que DondeEntreno te muestre opciones
              más relevantes en tu ciudad. Podés cambiarlos cuando quieras.
            </p>
          </SurfaceCard>
        )}

        <div className="mt-7 grid gap-6">
          {porCategoria.map(([categoria, deportes]) => (
            <div key={categoria}>
              <h3 className="text-sm font-extrabold text-[var(--color-primary)]">
                {categoria}
              </h3>

              <div
                role="group"
                aria-label={`Deportes de ${categoria}`}
                className="mt-3 flex flex-wrap gap-2"
              >
                {deportes.map((deporte) => {
                  const seleccionado = elegidos.includes(deporte.slug);

                  return (
                    <button
                      key={deporte.slug}
                      type="button"
                      onClick={() => alternarDeporteFavorito(deporte.slug)}
                      aria-pressed={seleccionado}
                      className={`inline-flex min-h-11 items-center gap-1.5 rounded-full px-4 py-2 text-sm font-bold shadow-sm transition duration-200 ease-out hover:-translate-y-0.5 focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-[#4FB3D9]/30 active:scale-[0.98] ${
                        seleccionado
                          ? "bg-[var(--color-secondary)] text-white"
                          : "border border-[var(--color-border-accent)] bg-[var(--color-surface)] text-[var(--color-primary)] hover:border-[var(--color-primary)] hover:bg-[var(--color-surface-soft)]"
                      }`}
                    >
                      {/*
                        El signo cambia con el estado: "+ Pádel" invita a
                        sumarlo y "✓ Pádel" confirma que ya está. Es
                        decorativo — quien usa lector de pantalla lo sabe
                        por aria-pressed.
                      */}
                      <span aria-hidden="true" className="text-xs">
                        {seleccionado ? "✓" : "+"}
                      </span>
                      {deporte.nombre}
                    </button>
                  );
                })}
              </div>
            </div>
          ))}
        </div>

        {/*
          Esta nota decía "se guardan en este dispositivo... más adelante
          van a viajar con tu cuenta": quedó vieja con el bloque de sync.
        */}
        <p className="mt-6 text-xs leading-5 text-[var(--color-muted)]">
          Tus deportes se sincronizan con tu cuenta: los vas a ver iguales
          desde cualquier dispositivo.
        </p>
      </section>

      <BloqueAsistente
        titulo="¿No sabés qué deporte probar?"
        descripcion="Contame qué buscás —moverte, competir, despejarte— y te sugiero por dónde empezar."
        textoBoton="Ayudame a elegir"
      />
    </div>
  );
}

/*
  Agrupa manteniendo el orden del catálogo: las categorías salen en el
  orden en que aparece su primer deporte, que es el del seed.
*/
function agruparPorCategoria(): Array<[string, Deporte[]]> {
  const grupos = new Map<string, Deporte[]>();

  for (const deporte of CATALOGO_DEPORTES_ASISTENTE) {
    const categoria = deporte.categoriaNombre ?? "Otros";
    const actuales = grupos.get(categoria);

    if (actuales) {
      actuales.push(deporte);
    } else {
      grupos.set(categoria, [deporte]);
    }
  }

  return Array.from(grupos.entries());
}

function construirHrefExplorarDeporte(
  deporteSlug: string,
  ciudadSlug: string | null
): string {
  const params = new URLSearchParams();
  params.set("deporteSlug", deporteSlug);

  if (ciudadSlug) {
    params.set("ciudadSlug", ciudadSlug);
  }

  params.set("page", "0");

  return `/explorar?${params.toString()}`;
}
