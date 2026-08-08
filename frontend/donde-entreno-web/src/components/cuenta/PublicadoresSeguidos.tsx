"use client";

import { useEffect, useState } from "react";

import { useAuthSession } from "../auth/AuthSessionProvider";
import {
  dejarDeSeguirPublicador,
  listarPublicadoresSeguidos,
  seguirPublicador,
} from "../../services/seguimientoService";
import type { PublicadorSeguido } from "../../types/seguimiento";
import { PublisherIdentity } from "../social/PublisherIdentity";
import { AppButton } from "../ui/AppButton";
import { AppLinkButton } from "../ui/AppLinkButton";
import { SectionHeader } from "../ui/SectionHeader";
import { StatusMessage } from "../ui/StatusMessage";
import { SurfaceCard } from "../ui/SurfaceCard";

/*
  Sección de mi-cuenta: publicadores que el usuario sigue (Bloque 8).
  Además de listar, permite dejar de seguir (y deshacer) sin salir de la
  página, con el mismo patrón optimista del botón Seguir del detalle.
*/
export function PublicadoresSeguidos() {
  const { accessToken } = useAuthSession();
  const [seguidos, setSeguidos] = useState<PublicadorSeguido[] | null>(null);
  const [error, setError] = useState(false);
  /* Ids con "dejar de seguir" aplicado en esta visita (permite deshacer). */
  const [idsNoSeguidos, setIdsNoSeguidos] = useState<number[]>([]);
  const [idProcesando, setIdProcesando] = useState<number | null>(null);

  useEffect(() => {
    let activo = true;

    if (!accessToken) {
      return () => {
        activo = false;
      };
    }

    listarPublicadoresSeguidos(accessToken)
      .then((lista) => {
        if (activo) {
          setSeguidos(lista);
          setError(false);
        }
      })
      .catch(() => {
        if (activo) {
          setError(true);
        }
      });

    return () => {
      activo = false;
    };
  }, [accessToken]);

  async function alternarSeguimiento(publicador: PublicadorSeguido) {
    if (!accessToken || idProcesando !== null) {
      return;
    }

    const dejaba = !idsNoSeguidos.includes(publicador.perfilPublicadorId);
    setIdProcesando(publicador.perfilPublicadorId);
    setIdsNoSeguidos((ids) =>
      dejaba
        ? [...ids, publicador.perfilPublicadorId]
        : ids.filter((id) => id !== publicador.perfilPublicadorId)
    );

    try {
      if (dejaba) {
        await dejarDeSeguirPublicador(publicador.perfilPublicadorId, accessToken);
      } else {
        await seguirPublicador(publicador.perfilPublicadorId, accessToken);
      }
    } catch {
      /* Revertimos el cambio optimista si la API falla. */
      setIdsNoSeguidos((ids) =>
        dejaba
          ? ids.filter((id) => id !== publicador.perfilPublicadorId)
          : [...ids, publicador.perfilPublicadorId]
      );
    } finally {
      setIdProcesando(null);
    }
  }

  const cargando = !error && seguidos === null;

  return (
    <SurfaceCard className="p-6 sm:p-8">
      <SectionHeader
        eyebrow="Social"
        title="Publicadores que sigo"
        description="Los clubes, gimnasios y profes que elegiste seguir para descubrir sus actividades."
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

      {!error && seguidos && seguidos.length === 0 ? (
        <StatusMessage variant="info" className="mt-5">
          Todavía no seguís a ningún publicador. Entrá al detalle de una
          actividad y tocá <strong>Seguir</strong> para no perderte sus
          novedades.
        </StatusMessage>
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
                className="rounded-[18px] border border-[#DDEAF3] bg-white/85 p-4 shadow-[0_10px_24px_rgba(12,52,80,0.05)]"
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
                      onClick={() => alternarSeguimiento(publicador)}
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
    </SurfaceCard>
  );
}

function EsqueletoFila() {
  return (
    <div
      aria-hidden="true"
      className="flex animate-pulse items-center gap-3 rounded-[18px] border border-[#DDEAF3] bg-white/85 p-4"
    >
      <span className="h-11 w-11 rounded-full bg-[#E8F6FB]" />
      <div className="flex-1">
        <div className="h-3 w-1/3 rounded-full bg-[#E8F6FB]" />
        <div className="mt-2 h-2.5 w-1/4 rounded-full bg-[#F8FAFC]" />
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

