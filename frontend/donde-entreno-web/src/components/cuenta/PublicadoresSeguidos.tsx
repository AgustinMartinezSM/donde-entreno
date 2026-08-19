"use client";

import type { Seguimientos } from "./useSeguimientos";
import { ComunidadSugerida } from "./ComunidadSugerida";
import { PublisherIdentity } from "../social/PublisherIdentity";
import { AppButton } from "../ui/AppButton";
import { AppLinkButton } from "../ui/AppLinkButton";
import { SectionHeader } from "../ui/SectionHeader";
import { StatusMessage } from "../ui/StatusMessage";

/*
  Solapa "Siguiendo" de mi perfil: los publicadores que el usuario sigue.
  Además de listar, permite dejar de seguir (y deshacer) sin salir de la
  página, con el mismo patrón optimista del botón Seguir del detalle.

  El estado lo trae useSeguimientos desde la página, porque la cabecera
  del perfil muestra el mismo contador.
*/
export function PublicadoresSeguidos({
  seguimientos,
}: {
  seguimientos: Seguimientos;
}) {
  const { seguidos, error, cargando, idsNoSeguidos, idProcesando, alternar } =
    seguimientos;
  const vacio = !error && seguidos !== null && seguidos.length === 0;
  const idsSeguidos = (seguidos ?? [])
    .map((publicador) => publicador.perfilPublicadorId)
    .filter((id) => !idsNoSeguidos.includes(id));

  /*
    Sin seguidos, la solapa mostraba un cartel azul y nada más. Ahora
    muestra a quién seguir, que es la única forma de que la sección deje
    de estar vacía.
  */
  if (vacio) {
    return (
      <div className="grid gap-8">
        <StatusMessage variant="info">
          Todavía no seguís a ningún club, gimnasio ni profe. Seguilos para
          enterarte cuando publiquen nuevas actividades.
        </StatusMessage>

        <ComunidadSugerida
          idsSeguidos={idsSeguidos}
          titulo="Para empezar a seguir"
          descripcion="Clubes, gimnasios y profes que ya publican actividades en la plataforma."
          maximo={6}
        />
      </div>
    );
  }

  return (
    <section aria-labelledby="seguidos-titulo">
      <SectionHeader
        eyebrow="Social"
        title="Publicadores que sigo"
        description="Los clubes, gimnasios y profes que elegiste seguir para descubrir sus actividades."
        titleId="seguidos-titulo"
      />

      {error ? (
        <StatusMessage variant="warning" className="mt-5">
          No pudimos cargar tus seguidos en este momento.
        </StatusMessage>
      ) : null}

      {cargando ? (
        <div
          role="status"
          aria-label="Cargando publicadores seguidos"
          className="mt-5 grid gap-3"
        >
          <EsqueletoFila />
          <EsqueletoFila />
        </div>
      ) : null}

      {!error && seguidos && seguidos.length > 0 ? (
        <ul className="mt-5 grid gap-3">
          {seguidos.map((publicador) => {
            const dejoDeSeguir = idsNoSeguidos.includes(
              publicador.perfilPublicadorId
            );

            return (
              <li
                key={publicador.perfilPublicadorId}
                className="rounded-[18px] border border-[var(--color-border-soft)] bg-white/85 p-4 shadow-[0_10px_24px_rgba(12,52,80,0.05)]"
              >
                <div className="flex flex-wrap items-center justify-between gap-3">
                  <div className="min-w-0">
                    <PublisherIdentity
                      nombre={publicador.perfilPublicadorNombre}
                      tipo={publicador.tipoPublicador}
                      href={`/publicadores/${publicador.perfilPublicadorId}`}
                    />
                    <p className="mt-2 text-xs font-bold uppercase tracking-[0.1em] text-[var(--color-secondary)]">
                      {[
                        publicador.ciudadPrincipalNombre,
                        formatearSeguidoDesde(publicador.seguidoDesde),
                      ]
                        .filter(Boolean)
                        .join(" · ")}
                    </p>
                  </div>

                  <div className="flex flex-wrap items-center gap-2">
                    <AppButton
                      type="button"
                      variant={dejoDeSeguir ? "primary" : "secondary"}
                      size="sm"
                      onClick={() => alternar(publicador)}
                      disabled={idProcesando === publicador.perfilPublicadorId}
                      aria-label={
                        dejoDeSeguir
                          ? `Volver a seguir a ${publicador.perfilPublicadorNombre}`
                          : `Dejar de seguir a ${publicador.perfilPublicadorNombre}`
                      }
                    >
                      {dejoDeSeguir ? "Seguir" : "Siguiendo"}
                    </AppButton>
                    <AppLinkButton
                      href={`/publicadores/${publicador.perfilPublicadorId}`}
                      variant="outline"
                      size="sm"
                    >
                      Ver perfil
                    </AppLinkButton>
                  </div>
                </div>
              </li>
            );
          })}
        </ul>
      ) : null}
    </section>
  );
}

function EsqueletoFila() {
  return (
    <div
      aria-hidden="true"
      className="flex animate-pulse items-center gap-3 rounded-[18px] border border-[var(--color-border-soft)] bg-white/85 p-4"
    >
      <span className="h-11 w-11 rounded-full bg-[var(--color-info-soft)]" />
      <div className="flex-1">
        <div className="h-3 w-1/3 rounded-full bg-[var(--color-info-soft)]" />
        <div className="mt-2 h-2.5 w-1/4 rounded-full bg-[var(--color-bg)]" />
      </div>
    </div>
  );
}

function formatearSeguidoDesde(fechaIso: string | null): string {
  if (!fechaIso) {
    return "";
  }

  const fecha = new Date(fechaIso);

  if (Number.isNaN(fecha.getTime())) {
    return "";
  }

  return `Seguís desde ${fecha.toLocaleDateString("es-AR", {
    month: "short",
    year: "numeric",
  })}`;
}
