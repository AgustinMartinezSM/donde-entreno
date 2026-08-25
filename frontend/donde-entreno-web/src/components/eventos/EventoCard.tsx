import Image from "next/image";
import Link from "next/link";

import { construirUrlImagenBackend } from "../../lib/backendUrl";
import {
  formatearCuantoFalta,
  formatearFechaEvento,
} from "../../lib/formatoFecha";
import type { Evento } from "../../services/eventosService";
import { SurfaceCard } from "../ui/SurfaceCard";

/*
  La card de un evento (Fase 9). Lo que la distingue de una actividad
  es la FECHA: va arriba de todo y con su chip de urgencia, porque es
  lo único que hace que alguien entre hoy y no la semana que viene.
*/
export function EventoCard({ evento }: { evento: Evento }) {
  const imagenUrl = construirUrlImagenBackend(evento.imagenUrl);
  const cuando = formatearFechaEvento(evento.iniciaAt);
  const falta = formatearCuantoFalta(evento.iniciaAt);
  const cancelado = evento.estado === "CANCELADO";

  const lugar = [evento.barrioNombre, evento.ciudadNombre]
    .filter(Boolean)
    .join(", ");

  return (
    <SurfaceCard as="article" className="overflow-hidden">
      <Link href={`/eventos/${evento.slug}`} className="block">
        {imagenUrl ? (
          <div className="relative h-40 w-full sm:h-48">
            <Image
              src={imagenUrl}
              alt=""
              fill
              sizes="(max-width: 640px) 100vw, 33vw"
              className={`object-cover ${cancelado ? "opacity-60 grayscale" : ""}`}
            />
          </div>
        ) : null}

        <div className="p-5">
          <div className="flex flex-wrap items-center gap-2">
            {cancelado ? (
              <span className="rounded-full bg-[var(--color-error-soft)] px-3 py-1 text-xs font-extrabold text-[var(--color-error)]">
                Cancelado
              </span>
            ) : falta ? (
              <span className="rounded-full bg-[var(--color-info-soft)] px-3 py-1 text-xs font-extrabold text-[var(--color-info-deep)]">
                {falta}
              </span>
            ) : null}

            {evento.esGratis ? (
              <span className="rounded-full bg-[var(--color-success-soft)] px-3 py-1 text-xs font-extrabold text-[var(--color-success)]">
                Gratis
              </span>
            ) : null}

            {evento.deporteNombre ? (
              <span className="text-xs font-bold text-[var(--color-muted)]">
                {evento.deporteNombre}
              </span>
            ) : null}
          </div>

          {cuando ? (
            <p className="mt-3 text-sm font-extrabold capitalize text-[var(--color-primary)]">
              {cuando}
            </p>
          ) : null}

          <h3 className="mt-1 text-base font-extrabold leading-6 text-[var(--color-text)]">
            {evento.titulo}
          </h3>

          {lugar ? (
            <p className="mt-1 text-sm text-[var(--color-muted)]">
              {evento.sedeNombre ? `${evento.sedeNombre} · ` : ""}
              {lugar}
            </p>
          ) : null}

          <div className="mt-3 flex flex-wrap items-center gap-x-3 gap-y-1 text-xs text-[var(--color-muted)]">
            {evento.perfilNombre ? (
              <span className="font-bold text-[var(--color-primary)]">
                {evento.perfilNombre}
              </span>
            ) : null}

            {/* El cupo es informativo: NO se reserva (decisión de la fase). */}
            {evento.cupo ? <span>{evento.cupo} lugares</span> : null}

            {evento.cantidadInteresados && evento.cantidadInteresados > 0 ? (
              <span>
                {evento.cantidadInteresados}{" "}
                {evento.cantidadInteresados === 1
                  ? "persona interesada"
                  : "personas interesadas"}
              </span>
            ) : null}
          </div>
        </div>
      </Link>
    </SurfaceCard>
  );
}
