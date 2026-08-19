"use client";

import { useEffect, useState } from "react";

import { obtenerPerfilesPublicadores } from "../../services/perfilPublicadorService";
import type { PerfilPublicadorPublico } from "../../types/publicadorPublico";
import { SeguirPublicadorButton } from "../actividad/SeguirPublicadorButton";
import { PublisherIdentity } from "../social/PublisherIdentity";
import { AppLinkButton } from "../ui/AppLinkButton";
import { SectionHeader } from "../ui/SectionHeader";

type ComunidadSugeridaProps = {
  /* Ids que ya sigue: no se sugiere a quien ya está siguiendo. */
  idsSeguidos: number[];
  titulo: string;
  descripcion: string;
  maximo?: number;
};

/*
  Clubes, gimnasios y profes reales para seguir.

  Sale del listado público de perfiles, así que no hay perfiles ni
  conteos inventados. Si no queda ninguno para sugerir —porque ya los
  sigue a todos— la sección directamente no se dibuja.
*/
export function ComunidadSugerida({
  idsSeguidos,
  titulo,
  descripcion,
  maximo = 3,
}: ComunidadSugeridaProps) {
  const [publicadores, setPublicadores] = useState<
    PerfilPublicadorPublico[] | null
  >(null);

  useEffect(() => {
    let activo = true;

    obtenerPerfilesPublicadores()
      .then((lista) => {
        if (activo) {
          setPublicadores(lista);
        }
      })
      .catch(() => {
        /* Best-effort: sin sugerencias la sección no aparece. */
        if (activo) {
          setPublicadores([]);
        }
      });

    return () => {
      activo = false;
    };
  }, []);

  if (publicadores === null) {
    return (
      <div
        role="status"
        aria-label="Cargando publicadores sugeridos"
        className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3"
      >
        <EsqueletoFila />
        <EsqueletoFila className="hidden sm:block" />
        <EsqueletoFila className="hidden lg:block" />
      </div>
    );
  }

  const sugeridos = publicadores
    .filter((publicador) => !idsSeguidos.includes(publicador.id))
    .slice(0, maximo);

  if (sugeridos.length === 0) {
    return null;
  }

  return (
    <section aria-labelledby="comunidad-sugerida-titulo">
      <SectionHeader
        eyebrow="Comunidad"
        title={titulo}
        description={descripcion}
        titleId="comunidad-sugerida-titulo"
      />

      <ul className="mt-5 grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
        {sugeridos.map((publicador) => (
          <li
            key={publicador.id}
            className="flex h-full flex-col rounded-[18px] border border-[var(--color-border-soft)] bg-white p-4 shadow-[0_10px_24px_rgba(12,52,80,0.05)]"
          >
            <PublisherIdentity
              nombre={publicador.nombre}
              tipo={publicador.tipoPublicador}
              verificado={publicador.verificado === true}
              href={`/publicadores/${publicador.id}`}
            />

            {publicador.descripcion ? (
              <p className="mt-3 line-clamp-2 text-sm leading-6 text-[var(--color-muted)]">
                {publicador.descripcion}
              </p>
            ) : null}

            <div className="mt-auto flex flex-wrap items-center gap-2 pt-4">
              <SeguirPublicadorButton
                perfilPublicadorId={publicador.id}
                perfilPublicadorNombre={publicador.nombre}
              />
              <AppLinkButton
                href={`/publicadores/${publicador.id}`}
                variant="outline"
                size="sm"
              >
                Ver perfil
              </AppLinkButton>
            </div>
          </li>
        ))}
      </ul>
    </section>
  );
}

function EsqueletoFila({ className = "" }: { className?: string }) {
  return (
    <div
      aria-hidden="true"
      className={`animate-pulse rounded-[18px] border border-[var(--color-border-soft)] bg-white p-4 ${className}`}
    >
      <div className="flex items-center gap-3">
        <span className="h-11 w-11 rounded-full bg-[var(--color-info-soft)]" />
        <div className="flex-1">
          <div className="h-3 w-2/3 rounded-full bg-[var(--color-info-soft)]" />
          <div className="mt-2 h-2.5 w-1/3 rounded-full bg-[var(--color-bg)]" />
        </div>
      </div>
      <div className="mt-4 h-9 w-28 rounded-[14px] bg-[var(--color-bg)]" />
    </div>
  );
}
