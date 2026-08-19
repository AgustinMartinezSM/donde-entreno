"use client";

import Link from "next/link";

import { CATALOGO_DEPORTES_ASISTENTE } from "../../lib/asistente/conocimiento";
import { useDeportesFavoritos } from "../../lib/preferenciasDeportivas";
import { useAuthSession } from "../auth/AuthSessionProvider";

type HomePreferenciasChipsProps = {
  ciudadSlug: string;
};

/*
  Personalización liviana de la home: si la persona ya marcó deportes
  favoritos (en /mi-cuenta), le mostramos accesos directos arriba del
  feed. Sin preferencias guardadas no se renderiza nada — la home queda
  igual para quien llega por primera vez.

  SOLO para sesiones iniciadas: elegir deportes vive en /mi-cuenta
  (detrás del login), así que un visitante no puede armar esta lista —
  pero el scope de invitado conserva la clave histórica de localStorage
  y guardaba elecciones viejas de esta computadora. Un visitante veía
  "TUS DEPORTES" de otra persona (visto en producción, 2026-08-19).
  Mientras la sesión resuelve tampoco se dibuja: mostrar y sacar el
  bloque era peor que aparecer medio segundo después.
*/
export function HomePreferenciasChips({
  ciudadSlug,
}: HomePreferenciasChipsProps) {
  const { status } = useAuthSession();
  const favoritos = useDeportesFavoritos();

  const deportesFavoritos = CATALOGO_DEPORTES_ASISTENTE.filter((deporte) =>
    favoritos.includes(deporte.slug)
  );

  if (status !== "authenticated" || deportesFavoritos.length === 0) {
    return null;
  }

  return (
    <div className="mt-8 rounded-[20px] border border-[var(--color-success-border)] bg-[var(--color-success-surface)] p-4 sm:p-5">
      <p className="text-xs font-extrabold uppercase tracking-[0.16em] text-[var(--color-success)]">
        Tus deportes
      </p>
      <div className="mt-3 flex min-w-0 gap-2 overflow-x-auto pb-1 sm:flex-wrap sm:overflow-visible sm:pb-0">
        {deportesFavoritos.map((deporte) => (
          <Link
            key={deporte.slug}
            href={`/explorar?deporteSlug=${encodeURIComponent(deporte.slug)}&ciudadSlug=${encodeURIComponent(ciudadSlug)}&page=0`}
            className="shrink-0 rounded-full border border-[var(--color-success-border)] bg-white px-3.5 py-2 text-sm font-bold text-[var(--color-success)] transition duration-200 ease-out hover:-translate-y-0.5 hover:border-[var(--color-secondary)] active:scale-[0.98]"
          >
            {deporte.nombre}
          </Link>
        ))}
        <Link
          href="/mi-cuenta"
          className="shrink-0 rounded-full px-3.5 py-2 text-sm font-bold text-[var(--color-muted)] transition hover:text-[var(--color-primary)]"
        >
          Editar preferencias
        </Link>
      </div>
    </div>
  );
}
