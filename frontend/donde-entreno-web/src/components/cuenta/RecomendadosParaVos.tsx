"use client";

import { useEffect, useState } from "react";

import { CATALOGO_DEPORTES_ASISTENTE } from "../../lib/asistente/conocimiento";
import { buscarActividades } from "../../services/actividadService";
import type { Actividad } from "../../types/actividad";
import { SocialActivityCard } from "../social/SocialActivityCard";
import { AppLinkButton } from "../ui/AppLinkButton";
import { SectionHeader } from "../ui/SectionHeader";

type RecomendadosParaVosProps = {
  deportesSlugs: string[];
  ciudadSlug: string | null;
  ciudadNombre: string | null;
  /* Slugs ya guardados: no tiene sentido recomendar lo que ya se guardó. */
  slugsGuardados: string[];
};

/* Cuántos deportes consultamos: uno por request, así que no pueden ser todos. */
const MAX_DEPORTES_CONSULTADOS = 3;
const MAX_RECOMENDADAS = 6;

/*
  "Recomendado para vos" con los datos que ya tenemos: la ciudad activa
  y los deportes que la persona marcó. No hay modelo de recomendación ni
  métricas de popularidad detrás — es la búsqueda pública real filtrada
  por lo que ya sabemos de esta persona, y el encabezado lo dice.

  Si todavía no eligió deportes, mostramos lo último publicado en su
  ciudad: sigue siendo relevante y no obliga a configurar nada para que
  la pantalla tenga contenido.
*/
export function RecomendadosParaVos({
  deportesSlugs,
  ciudadSlug,
  ciudadNombre,
  slugsGuardados,
}: RecomendadosParaVosProps) {
  const [actividades, setActividades] = useState<Actividad[] | null>(null);
  const [error, setError] = useState(false);

  /*
    Las dependencias son los slugs unidos y no los arrays: dos arrays con
    el mismo contenido son objetos distintos en cada render y el effect
    se dispararía en cada uno.
  */
  const claveDeportes = deportesSlugs.slice(0, MAX_DEPORTES_CONSULTADOS).join(",");

  useEffect(() => {
    let activo = true;
    const deportesConsultados = claveDeportes ? claveDeportes.split(",") : [];

    const consultas =
      deportesConsultados.length > 0
        ? deportesConsultados.map((deporteSlug) =>
            buscarActividades({
              deporteSlug,
              ciudadSlug: ciudadSlug ?? undefined,
              page: 0,
              size: 3,
            })
          )
        : [
            buscarActividades({
              ciudadSlug: ciudadSlug ?? undefined,
              page: 0,
              size: MAX_RECOMENDADAS,
            }),
          ];

    Promise.all(consultas)
      .then((paginas) => {
        if (!activo) {
          return;
        }

        /*
          Intercalamos por deporte en vez de concatenar: con 3 deportes y
          3 resultados cada uno, concatenar dejaba la primera fila entera
          del mismo deporte.
        */
        setActividades(intercalar(paginas.map((pagina) => pagina.contenido)));
        setError(false);
      })
      .catch(() => {
        if (activo) {
          setError(true);
        }
      });

    return () => {
      activo = false;
    };
  }, [claveDeportes, ciudadSlug]);

  /*
    Un error acá no merece un cartel: es una sección de descubrimiento,
    no el contenido principal de la persona.
  */
  if (error) {
    return null;
  }

  const visibles = (actividades ?? [])
    .filter((actividad) => !slugsGuardados.includes(actividad.slug))
    .slice(0, MAX_RECOMENDADAS);

  if (actividades !== null && visibles.length === 0) {
    return null;
  }

  const hayDeportes = claveDeportes.length > 0;
  const nombresDeportes = claveDeportes
    .split(",")
    .map(
      (slug) =>
        CATALOGO_DEPORTES_ASISTENTE.find((deporte) => deporte.slug === slug)
          ?.nombre
    )
    .filter(Boolean)
    .join(", ");

  return (
    <section aria-labelledby="recomendados-titulo">
      <SectionHeader
        eyebrow="Recomendado"
        title={hayDeportes ? "Para vos" : `Lo último en ${ciudadNombre ?? "tu zona"}`}
        description={
          hayDeportes
            ? `Según ${ciudadNombre ? `${ciudadNombre} y ` : ""}tus deportes: ${nombresDeportes}.`
            : "Elegí tus deportes para que estas recomendaciones se parezcan más a vos."
        }
        titleId="recomendados-titulo"
      />

      {actividades === null ? (
        <div
          role="status"
          aria-label="Cargando recomendaciones"
          className="mt-5 grid gap-5 sm:grid-cols-2 lg:grid-cols-3"
        >
          <EsqueletoCard />
          <EsqueletoCard className="hidden sm:block" />
          <EsqueletoCard className="hidden lg:block" />
        </div>
      ) : (
        <>
          <div className="mt-5 grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
            {visibles.map((actividad) => (
              <SocialActivityCard
                key={actividad.id}
                actividad={actividad}
                variante="compacta"
              />
            ))}
          </div>

          <div className="mt-5">
            <AppLinkButton
              href={construirHrefExplorar(ciudadSlug)}
              variant="secondary"
              size="sm"
            >
              Ver más actividades
            </AppLinkButton>
          </div>
        </>
      )}
    </section>
  );
}

/*
  Toma una de cada lista por vuelta hasta agotarlas, sin repetir slugs:
  una misma actividad puede venir en dos deportes distintos si el
  catálogo los agrupa parecido.
*/
function intercalar(listas: Actividad[][]): Actividad[] {
  const resultado: Actividad[] = [];
  const vistos = new Set<string>();
  const maxLargo = Math.max(0, ...listas.map((lista) => lista.length));

  for (let indice = 0; indice < maxLargo; indice += 1) {
    for (const lista of listas) {
      const actividad = lista[indice];

      if (actividad && !vistos.has(actividad.slug)) {
        vistos.add(actividad.slug);
        resultado.push(actividad);
      }
    }
  }

  return resultado;
}

function construirHrefExplorar(ciudadSlug: string | null): string {
  const params = new URLSearchParams();

  if (ciudadSlug) {
    params.set("ciudadSlug", ciudadSlug);
  }

  params.set("page", "0");

  return `/explorar?${params.toString()}`;
}

function EsqueletoCard({ className = "" }: { className?: string }) {
  return (
    <div
      aria-hidden="true"
      className={`animate-pulse overflow-hidden rounded-[20px] border border-[var(--color-border)] bg-white p-3.5 ${className}`}
    >
      <div className="flex items-center gap-2">
        <span className="h-8 w-8 rounded-full bg-[var(--color-info-soft)]" />
        <div className="h-3 w-1/3 rounded-full bg-[var(--color-info-soft)]" />
      </div>
      <div className="mt-3 h-48 rounded-[16px] bg-[var(--color-info-soft)]" />
      <div className="mt-4 h-4 w-2/3 rounded-full bg-[var(--color-info-soft)]" />
      <div className="mt-3 h-3 w-1/2 rounded-full bg-[var(--color-bg)]" />
    </div>
  );
}
