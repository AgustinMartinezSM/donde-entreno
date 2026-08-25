import Image from "next/image";
import Link from "next/link";

import { construirUrlImagenBackend } from "../../lib/backendUrl";
import {
  formatearCuantoFalta,
  formatearFechaEvento,
  formatearFechaRelativa,
} from "../../lib/formatoFecha";
import type { FeedEvento } from "../../types/seguimiento";
import { BotonReportar } from "./BotonReportar";
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
  NOVEDAD: "contó una novedad",
  EVENTO_NUEVO: "organiza un evento",
};

export function FeedEventoCard({ evento }: { evento: FeedEvento }) {
  const frase = FRASES[evento.tipo] ?? "publicó algo nuevo";

  /*
    La novedad del canal (Fase 8) es texto propio: va entera en el
    cuerpo, no recortada detrás de dos puntos como el resumen de los
    otros hechos, y lleva su botón de reporte — es el único tipo de
    evento cuyo contenido lo escribe una persona.
  */
  const esNovedad = evento.tipo === "NOVEDAD" && Boolean(evento.novedadTexto);

  /*
    El evento (Fase 9): lo que lo hace distinto de todo lo demás del
    feed es que TIENE FECHA y se puede perder. Por eso la fecha va
    destacada y el link lleva al evento, no a una actividad.
  */
  const esEvento =
    evento.tipo === "EVENTO_NUEVO" && Boolean(evento.eventoSlug);
  const cuandoEsElEvento = esEvento
    ? formatearFechaEvento(evento.eventoIniciaAt)
    : null;
  const faltaParaElEvento = esEvento
    ? formatearCuantoFalta(evento.eventoIniciaAt)
    : null;

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

  /* Adónde lleva la foto: al evento si es uno, si no a la actividad. */
  const hrefDestino = esEvento
    ? `/eventos/${evento.eventoSlug}`
    : hrefActividad;

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
        {!esNovedad && !esEvento && evento.resumen
          ? `: ${evento.resumen}`
          : ""}
      </p>

      {esEvento ? (
        <div className="px-4 pt-2">
          <div className="flex flex-wrap items-center gap-2">
            {faltaParaElEvento ? (
              <span className="rounded-full bg-[var(--color-info-soft)] px-3 py-1 text-xs font-extrabold text-[var(--color-info-deep)]">
                {faltaParaElEvento}
              </span>
            ) : null}
            {cuandoEsElEvento ? (
              <span className="text-xs font-extrabold capitalize text-[var(--color-primary)]">
                {cuandoEsElEvento}
              </span>
            ) : null}
          </div>

          <p className="mt-1 text-sm font-extrabold leading-6 text-[var(--color-text)]">
            {evento.eventoTitulo}
          </p>
        </div>
      ) : null}

      {esNovedad ? (
        <p className="whitespace-pre-line px-4 pt-2 text-sm leading-6 text-[var(--color-text)]">
          {evento.novedadTexto}
        </p>
      ) : null}

      {imagenUrl ? (
        hrefDestino ? (
          <Link href={hrefDestino} className="mt-3 block">
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
        ) : (
          /* La novedad no cuelga de una actividad: la foto no linkea. */
          <div className="relative mt-3 h-56 w-full sm:h-64">
            <Image
              src={imagenUrl}
              alt=""
              fill
              sizes="(max-width: 640px) 100vw, 50vw"
              className="object-cover"
            />
          </div>
        )
      ) : null}

      {esEvento ? (
        <div className="flex flex-wrap items-center justify-between gap-2 px-4 py-4">
          <Link
            href={`/eventos/${evento.eventoSlug}`}
            className="text-sm font-extrabold text-[var(--color-primary)] underline decoration-[var(--color-border-accent)] underline-offset-4 transition hover:decoration-[var(--color-primary)]"
          >
            Ver el evento
          </Link>

          {evento.eventoDeportivoId ? (
            <BotonReportar
              tipoObjeto="EVENTO"
              objetoId={evento.eventoDeportivoId}
              etiquetaObjeto="este evento"
              compacto
            />
          ) : null}
        </div>
      ) : hrefActividad && evento.actividadTitulo ? (
        <div className="px-4 py-4">
          <Link
            href={hrefActividad}
            className="text-base font-extrabold leading-6 text-[var(--color-primary)] underline decoration-[var(--color-border-accent)] underline-offset-4 transition hover:decoration-[var(--color-primary)]"
          >
            {evento.actividadTitulo}
          </Link>
        </div>
      ) : esNovedad ? (
        /*
          La acción útil de una novedad es entrar al publicador: ahí
          están sus actividades, sus fotos y el contacto.
        */
        <div className="flex flex-wrap items-center justify-between gap-2 px-4 py-4">
          {hrefPerfil ? (
            <Link
              href={hrefPerfil}
              className="text-sm font-extrabold text-[var(--color-primary)] underline decoration-[var(--color-border-accent)] underline-offset-4 transition hover:decoration-[var(--color-primary)]"
            >
              Ver el perfil de {evento.perfilNombre}
            </Link>
          ) : (
            <span />
          )}

          {evento.novedadId ? (
            <BotonReportar
              tipoObjeto="NOVEDAD"
              objetoId={evento.novedadId}
              etiquetaObjeto="esta novedad"
              compacto
            />
          ) : null}
        </div>
      ) : (
        <div className="pb-4" aria-hidden="true" />
      )}
    </article>
  );
}
