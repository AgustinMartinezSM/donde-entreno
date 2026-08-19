"use client";

import { MisFavoritos } from "../favoritos/MisFavoritos";
import { AppButton } from "../ui/AppButton";
import { AppLinkButton } from "../ui/AppLinkButton";
import type { PerfilDeportivo, TabPerfil } from "./usePerfilDeportivo";

type GuardadosPerfilProps = {
  perfil: PerfilDeportivo;
  onIrATab: (tab: TabPerfil) => void;
};

/*
  Solapa "Guardados". El listado es el mismo componente que usa
  /favoritos —una sola colección, no dos vistas que se contradigan—, y
  desde acá le sumamos al estado vacío lo que el perfil sí sabe: los
  deportes que la persona eligió y el asistente.
*/
export function GuardadosPerfil({ perfil, onIrATab }: GuardadosPerfilProps) {
  return (
    <MisFavoritos
      accionesVacio={
        <div className="mt-1 w-full border-t border-[#EDF3F8] pt-5">
          {perfil.deportesSlugs.length > 0 ? (
            <>
              <p className="text-sm font-bold text-[var(--color-primary)]">
                O empezá por tus deportes:
              </p>
              <div className="mt-3 flex flex-wrap justify-center gap-2">
                {perfil.deportesSlugs.map((slug, indice) => (
                  <AppLinkButton
                    key={slug}
                    href={construirHrefDeporte(slug, perfil.ciudadSlug)}
                    variant="secondary"
                    size="sm"
                    className="rounded-full"
                  >
                    {perfil.deportesNombres[indice]}
                  </AppLinkButton>
                ))}
              </div>
            </>
          ) : (
            <>
              <p className="text-sm text-[var(--color-muted)]">
                Si elegís tus deportes, te vamos a mostrar actividades más
                parecidas a lo que buscás.
              </p>
              <AppButton
                variant="secondary"
                size="sm"
                className="mt-3"
                onClick={() => onIrATab("deportes")}
              >
                Elegir mis deportes
              </AppButton>
            </>
          )}

          <p className="mt-5 text-sm text-[var(--color-muted)]">
            ¿No sabés qué buscar?{" "}
            <button
              type="button"
              aria-haspopup="dialog"
              onClick={() =>
                window.dispatchEvent(new Event("donde-entreno:abrir-asistente"))
              }
              className="rounded-sm font-bold text-[var(--color-primary)] underline decoration-[var(--color-border-accent)] decoration-2 underline-offset-2 transition hover:decoration-[var(--color-primary)] focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-[#4FB3D9]/30"
            >
              Pedile una recomendación al asistente
            </button>
            .
          </p>
        </div>
      }
    />
  );
}

function construirHrefDeporte(
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
