"use client";

import { useEffect, useState } from "react";

import { useAuthSession } from "../auth/AuthSessionProvider";
import { listarPublicadoresSeguidos } from "../../services/seguimientoService";
import type { PublicadorSeguido } from "../../types/seguimiento";
import { AppLinkButton } from "../ui/AppLinkButton";
import { SectionHeader } from "../ui/SectionHeader";
import { StatusMessage } from "../ui/StatusMessage";
import { SurfaceCard } from "../ui/SurfaceCard";

/*
  Sección de mi-cuenta: publicadores que el usuario sigue (Bloque 8).
  Carga la lista real del backend con el token de la sesión.
*/
export function PublicadoresSeguidos() {
  const { accessToken } = useAuthSession();
  const [seguidos, setSeguidos] = useState<PublicadorSeguido[] | null>(null);
  const [error, setError] = useState(false);

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

  return (
    <SurfaceCard className="p-6 sm:p-8">
      <SectionHeader
        eyebrow="Social"
        title="Publicadores que sigo"
        description="Los clubes, gimnasios y profes que elegiste seguir para descubrir sus actividades."
      />

      {error ? (
        <StatusMessage variant="info" className="mt-5">
          No pudimos cargar tus seguidos en este momento.
        </StatusMessage>
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
          {seguidos.map((publicador) => (
            <li
              key={publicador.perfilPublicadorId}
              className="flex flex-wrap items-center justify-between gap-3 rounded-[18px] border border-[#DDEAF3] bg-white/85 p-4 shadow-[0_10px_24px_rgba(12,52,80,0.05)]"
            >
              <div>
                <p className="text-sm font-extrabold text-[var(--color-primary)]">
                  {publicador.perfilPublicadorNombre}
                </p>
                <p className="mt-1 text-xs font-bold uppercase tracking-[0.1em] text-[var(--color-secondary)]">
                  {[
                    formatearTipo(publicador.tipoPublicador),
                    publicador.ciudadPrincipalNombre,
                  ]
                    .filter(Boolean)
                    .join(" · ") || "Publicador"}
                </p>
              </div>
              <AppLinkButton
                href={`/explorar?perfilPublicadorId=${publicador.perfilPublicadorId}`}
                variant="secondary"
                size="sm"
              >
                Ver actividades
              </AppLinkButton>
            </li>
          ))}
        </ul>
      ) : null}
    </SurfaceCard>
  );
}

function formatearTipo(tipo: string | null): string {
  if (!tipo) {
    return "";
  }

  return tipo
    .toLowerCase()
    .split("_")
    .map((parte) => parte.charAt(0).toUpperCase() + parte.slice(1))
    .join(" ");
}
