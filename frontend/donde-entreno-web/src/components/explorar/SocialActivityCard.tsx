import Link from "next/link";
import type { Actividad } from "../../types/actividad";
import { construirUrlImagenBackend } from "../../lib/backendUrl";
import {
  obtenerImagenActividad,
  obtenerImagenFallbackActividad,
} from "../../lib/activityImages";
import { ActivityImage } from "../actividad/ActivityImage";
import { FavoritoButton } from "../actividad/FavoritoButton";
import { PublisherIdentity } from "../social/PublisherIdentity";

export function SocialActivityCard({ actividad }: { actividad: Actividad }) {
  const imagenBackend = construirUrlImagenBackend(actividad.imagenPrincipalUrl);
  const imagenUrl = obtenerImagenActividad({
    imagenBackend,
    deporteSlug: actividad.deporteSlug,
  });
  const imagenFallbackUrl = obtenerImagenFallbackActividad({
    deporteSlug: actividad.deporteSlug,
  });
  const ubicacion = [actividad.barrioNombre, actividad.ciudadNombre]
    .filter(Boolean)
    .join(", ");

  return (
    <article className="group overflow-hidden rounded-[24px] border border-[#D9E2EC] bg-white shadow-[0_12px_35px_rgba(15,61,94,0.08)] transition duration-200 ease-out hover:-translate-y-1 hover:border-[#BFDDEA] hover:shadow-[0_22px_50px_rgba(15,61,94,0.13)]">
      <div className="flex items-center justify-between gap-3 px-4 py-4 sm:px-5">
        <PublisherIdentity
          nombre={actividad.perfilPublicadorNombre}
          tipo={actividad.tipoPublicador}
          verificado={actividad.perfilVerificado}
        />

        {actividad.deporteNombre ? (
          <span className="shrink-0 rounded-full bg-[#E6F7EF] px-3 py-1.5 text-xs font-extrabold text-[#167A4A]">
            {actividad.deporteNombre}
          </span>
        ) : null}
      </div>

      <div className="relative mx-3 sm:mx-4">
        <ActivityImage
          src={imagenUrl}
          fallbackSrc={imagenFallbackUrl}
          alt={actividad.titulo || actividad.deporteNombre || "Actividad deportiva"}
          fallbackText={actividad.deporteNombre || "Actividad"}
          heightClassName="h-60 sm:h-72"
        />

        <FavoritoButton
          actividad={{
            slug: actividad.slug,
            titulo: actividad.titulo,
            deporteNombre: actividad.deporteNombre,
            deporteSlug: actividad.deporteSlug,
            ciudadNombre: actividad.ciudadNombre,
            barrioNombre: actividad.barrioNombre,
            imagenPrincipalUrl: actividad.imagenPrincipalUrl,
            nivel: actividad.nivel,
            modalidad: actividad.modalidad,
            precioReferencia: actividad.precioReferencia,
            mostrarPrecio: actividad.mostrarPrecio,
          }}
        />
      </div>

      <div className="px-4 pb-5 pt-4 sm:px-5">
        <div className="flex items-start justify-between gap-4">
          <div className="min-w-0">
            <h3 className="line-clamp-2 text-xl font-extrabold leading-tight text-[var(--color-primary)]">
              {actividad.titulo}
            </h3>
            <p className="mt-2 flex items-start gap-2 text-sm font-semibold text-[var(--color-muted)]">
              <IconoUbicacion />
              <span>{ubicacion || "Ubicación a confirmar"}</span>
            </p>
          </div>

          {actividad.mostrarPrecio && actividad.precioReferencia != null ? (
            <span className="shrink-0 rounded-[14px] bg-[#F8FAFC] px-3 py-2 text-right text-xs font-extrabold text-[var(--color-primary)] ring-1 ring-[#D9E2EC]">
              Desde
              <strong className="block text-sm">
                ${actividad.precioReferencia}
              </strong>
            </span>
          ) : null}
        </div>

        {actividad.descripcion ? (
          <p className="mt-3 line-clamp-2 text-sm leading-6 text-[var(--color-muted)]">
            {actividad.descripcion}
          </p>
        ) : null}

        <div className="mt-4 flex flex-wrap gap-2">
          {actividad.modalidad ? (
            <Etiqueta>{actividad.modalidad}</Etiqueta>
          ) : null}
          {actividad.nivel ? <Etiqueta>{actividad.nivel}</Etiqueta> : null}
          {actividad.cuposLimitados ? <Etiqueta>Cupos limitados</Etiqueta> : null}
        </div>

        <Link
          href={`/actividades/${actividad.slug}`}
          className="mt-5 inline-flex min-h-11 w-full items-center justify-center gap-2 rounded-[16px] bg-[var(--color-primary)] px-5 py-3 text-sm font-extrabold text-white shadow-[var(--shadow-button)] transition duration-200 ease-out hover:-translate-y-0.5 hover:bg-[#0B314D] active:scale-[0.98]"
        >
          Ver actividad
          <span aria-hidden="true">→</span>
        </Link>
      </div>
    </article>
  );
}

function Etiqueta({ children }: { children: string }) {
  return (
    <span className="rounded-full bg-[#E8F6FB] px-3 py-1.5 text-xs font-bold text-[#0F6F8F]">
      {children}
    </span>
  );
}

function IconoUbicacion() {
  return (
    <svg
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
      className="mt-0.5 h-4 w-4 shrink-0 text-[var(--color-accent)]"
      aria-hidden="true"
    >
      <path d="M20 10c0 5-8 11-8 11S4 15 4 10a8 8 0 1 1 16 0z" />
      <circle cx="12" cy="10" r="2.5" />
    </svg>
  );
}
