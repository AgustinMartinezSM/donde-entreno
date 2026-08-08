"use client";

import { useSyncExternalStore } from "react";

import { leerSlugCiudadGuardada } from "../../lib/ciudadActiva";
import { useFavoritos } from "../../lib/favoritos";
import { IconoGuardar } from "../ui/IconoGuardar";
import { AppLinkButton } from "../ui/AppLinkButton";
import { SectionHeader } from "../ui/SectionHeader";
import { SurfaceCard } from "../ui/SurfaceCard";

function formatearSlugCiudad(slug: string | null): string {
  if (!slug) {
    return "Sin elegir";
  }

  return slug
    .split("-")
    .map((parte, indice) =>
      indice === 0 || parte.length > 3
        ? parte.charAt(0).toUpperCase() + parte.slice(1)
        : parte
    )
    .join(" ");
}

/*
  Resumen del espacio personal: actividades guardadas (V1 local)
  y ciudad activa elegida en el selector del header.
*/
export function ResumenGuardados() {
  const favoritos = useFavoritos();
  /*
    La ciudad guardada vive en localStorage: se lee recién después de
    hidratar (snapshot de servidor null) para no desincronizar el HTML
    de SSR con el primer render del cliente.
  */
  const slugCiudad = useSyncExternalStore(
    suscripcionVacia,
    leerSlugCiudadGuardadaSinFallar,
    () => null
  );
  const ciudadActiva = formatearSlugCiudad(slugCiudad);

  return (
    <SurfaceCard className="p-6 sm:p-8">
      <SectionHeader
        eyebrow="Tu actividad"
        title="Guardados y ciudad"
        description="Lo que fuiste marcando mientras explorabas."
      />

      <div className="mt-6 grid gap-3 sm:grid-cols-2">
        <div className="flex items-center gap-4 rounded-[18px] border border-[#DDEAF3] bg-white/85 p-4">
          <span
            aria-hidden="true"
            className="inline-flex h-11 w-11 items-center justify-center rounded-full bg-[#E6F7EF] text-[#1D7B4A]"
          >
            <IconoGuardar relleno={favoritos.length > 0} className="h-5 w-5" />
          </span>
          <div>
            <p className="text-2xl font-extrabold leading-none text-[var(--color-primary)]">
              {favoritos.length}
            </p>
            <p className="mt-1 text-sm font-bold text-[var(--color-muted)]">
              {favoritos.length === 1
                ? "actividad guardada"
                : "actividades guardadas"}
            </p>
          </div>
        </div>

        <div className="flex items-center gap-4 rounded-[18px] border border-[#DDEAF3] bg-white/85 p-4">
          <span
            aria-hidden="true"
            className="inline-flex h-11 w-11 items-center justify-center rounded-full bg-[#E8F6FB] text-[var(--color-primary)]"
          >
            <svg
              viewBox="0 0 24 24"
              className="h-5 w-5"
              fill="none"
              stroke="currentColor"
              strokeWidth={2}
              strokeLinecap="round"
              strokeLinejoin="round"
              aria-hidden="true"
            >
              <path d="M12 21s-7-5.5-7-11a7 7 0 0 1 14 0c0 5.5-7 11-7 11z" />
              <circle cx="12" cy="10" r="2.5" />
            </svg>
          </span>
          <div className="min-w-0">
            <p className="truncate text-lg font-extrabold leading-tight text-[var(--color-primary)]">
              {ciudadActiva}
            </p>
            <p className="mt-1 text-sm font-bold text-[var(--color-muted)]">
              ciudad activa
            </p>
          </div>
        </div>
      </div>

      <div className="mt-5 grid gap-3 sm:grid-cols-2">
        <AppLinkButton href="/favoritos" fullWidth>
          Ver mis favoritos
        </AppLinkButton>
        <AppLinkButton href="/ciudades" variant="outline" fullWidth>
          Cambiar ciudad
        </AppLinkButton>
      </div>
    </SurfaceCard>
  );
}

function suscripcionVacia() {
  return () => {};
}

function leerSlugCiudadGuardadaSinFallar(): string | null {
  try {
    return leerSlugCiudadGuardada();
  } catch {
    return null;
  }
}
