import Image from "next/image";
import Link from "next/link";

import { construirUrlImagenBackend } from "../../lib/backendUrl";
import { formatearFechaRelativa } from "../../lib/formatoFecha";
import type { FeedEvento } from "../../types/seguimiento";
import { PublisherIdentity } from "./PublisherIdentity";

/*
  Un hecho del feed (Fase 6). Cada tipo se cuenta distinto, pero todos
  terminan en la misma acción útil: entrar a la actividad. La regla de
  la etapa es que nada social sea decorativo.
*/
const FRASES: Record<string, string> = {
  ACTIVIDAD_NUEVA: "publicó una actividad nueva",
  FOTOS_NUEVAS: "subió una foto nueva",
  ACTIVIDAD_ACTUALIZADA: "actualizó una actividad",
};

export function FeedEventoCard({ evento }: { evento: FeedEvento }) {
  const frase = FRASES[evento.tipo] ?? "publicó algo nuevo";

  /*
    La foto del hecho manda sobre la portada de la actividad: si el
    publicador subió una foto, ESA es la novedad.
  */
  const imagenUrl =
    construirUrlImagenBackend(evento.imagenUrl) ??
    construirUrlImagenBackend(evento.actividadImagenUrl);

  const hrefActividad = evento.actividadSlug
    ? `/actividades/${evento.actividadSlug}`
    : null;
  const hrefPerfil = evento.perfilSlug
    ? `/publicadores/${evento.perfilSlug}`
    : evento.perfilPublicadorId
      ? `/publicadores/${evento.perfilPublicadorId}`
      : undefined;

  return (
    <article className="overflow-hidden rounded-[var(--radius-lg)] border border-[var(--color-border)] bg-[var(--color-surface)] shadow-[var(--shadow-soft)]">
      <div className="flex items-center gap-3 px-4 pt-4">
        <PublisherIdentity
          nombre={evento.perfilNombre}
          href={hrefPerfil}
          avatarUrl={construirUrlImagenBackend(evento.perfilLogoUrl)}
          tamanio="normal"
          nota={
            evento.createdAt ? formatearFechaRelativa(evento.createdAt) : null
          }
        />
      </div>

      <p className="px-4 pt-2 text-sm leading-6 text-[var(--color-muted)]">
        <span className="font-bold text-[var(--color-primary)]">
          {evento.perfilNombre}
        </span>{" "}
        {frase}
        {evento.resumen ? `: ${evento.resumen}` : ""}
      </p>

      {imagenUrl && hrefActividad ? (
        <Link href={hrefActividad} className="mt-3 block">
          <div className="relative h-56 w-full sm:h-64">
            <Image
              src={imagenUrl}
              alt={evento.actividadTitulo ?? "Novedad del publicador"}
              fill
              sizes="(max-width: 640px) 100vw, 50vw"
              className="object-cover transition duration-200 ease-out hover:scale-[1.02]"
            />
          </div>
        </Link>
      ) : null}

      {hrefActividad && evento.actividadTitulo ? (
        <div className="px-4 py-4">
          <Link
            href={hrefActividad}
            className="text-base font-extrabold leading-6 text-[var(--color-primary)] underline decoration-[var(--color-border-accent)] underline-offset-4 transition hover:decoration-[var(--color-primary)]"
          >
            {evento.actividadTitulo}
          </Link>
        </div>
      ) : (
        <div className="pb-4" aria-hidden="true" />
      )}
    </article>
  );
}
