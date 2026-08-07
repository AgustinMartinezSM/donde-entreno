import Link from "next/link";
import type { Actividad } from "../../types/actividad";
import { construirUrlImagenBackend } from "../../lib/backendUrl";
import {
  obtenerImagenActividad,
  obtenerImagenFallbackActividad,
} from "../../lib/activityImages";
import {
  formatearEtiquetaCatalogo,
  formatearPrecio,
} from "../../lib/formatoCatalogo";
import { ActivityImage } from "../actividad/ActivityImage";
import { FavoritoButton } from "../actividad/FavoritoButton";
import { AppLinkButton } from "../ui/AppLinkButton";
import { PublisherIdentity } from "./PublisherIdentity";

type SocialActivityCardProps = {
  actividad: Actividad;
  /*
    - "feed": card grande de descubrimiento (home, feed de seguidos):
      imagen alta, descripción y publicador con tipo visible.
    - "compacta": card de grilla (explorar, landings de deporte/ciudad):
      imagen media, sin descripción, identidad del publicador reducida.
  */
  variante?: "feed" | "compacta";
};

/*
  Card única de actividad para todas las superficies públicas.
  La actividad se presenta como contenido de la comunidad: identidad del
  publicador arriba, imagen protagonista clickeable y acciones claras.
*/
export function SocialActivityCard({
  actividad,
  variante = "feed",
}: SocialActivityCardProps) {
  const esFeed = variante === "feed";
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
  const precioVisible =
    actividad.mostrarPrecio === true
      ? formatearPrecio(actividad.precioReferencia)
      : null;
  const hrefActividad = `/actividades/${actividad.slug}`;

  return (
    <article
      className={`group flex h-full flex-col overflow-hidden border border-[#D9E2EC] bg-white transition duration-200 ease-out hover:-translate-y-1 hover:border-[#BFDDEA] ${
        esFeed
          ? "rounded-[24px] shadow-[0_12px_35px_rgba(15,61,94,0.08)] hover:shadow-[0_22px_50px_rgba(15,61,94,0.13)]"
          : "rounded-[20px] shadow-[0_8px_24px_rgba(15,61,94,0.07)] hover:shadow-[0_16px_40px_rgba(15,61,94,0.12)]"
      }`}
    >
      <div
        className={`flex items-center justify-between gap-3 ${
          esFeed ? "px-4 py-4 sm:px-5" : "px-3.5 py-3"
        }`}
      >
        <PublisherIdentity
          nombre={actividad.perfilPublicadorNombre}
          tipo={actividad.tipoPublicador}
          verificado={actividad.perfilVerificado}
          tamanio={esFeed ? "normal" : "compacta"}
        />

        {actividad.deporteNombre ? (
          <span className="shrink-0 rounded-full bg-[#E6F7EF] px-3 py-1.5 text-xs font-extrabold text-[#1D7B4A]">
            {actividad.deporteNombre}
          </span>
        ) : null}
      </div>

      <div className={`relative ${esFeed ? "mx-3 sm:mx-4" : "mx-3"}`}>
        {/* La imagen linkea al detalle; el link accesible es el del título. */}
        <Link href={hrefActividad} tabIndex={-1} aria-hidden="true">
          <ActivityImage
            src={imagenUrl}
            fallbackSrc={imagenFallbackUrl}
            alt={actividad.titulo || actividad.deporteNombre || "Actividad deportiva"}
            fallbackText={actividad.deporteNombre || "Actividad"}
            heightClassName={esFeed ? "h-60 sm:h-72" : "h-48"}
            sizes={
              esFeed
                ? "(max-width: 1023px) 100vw, 600px"
                : "(max-width: 640px) 100vw, (max-width: 1024px) 50vw, 400px"
            }
          />
        </Link>

        <FavoritoButton actividad={actividad} />
      </div>

      <div
        className={`flex flex-1 flex-col ${
          esFeed ? "px-4 pb-5 pt-4 sm:px-5" : "px-3.5 pb-4 pt-3"
        }`}
      >
        <div className="flex items-start justify-between gap-4">
          <div className="min-w-0">
            <h3
              className={`line-clamp-2 font-extrabold leading-tight ${
                esFeed ? "text-xl" : "text-base"
              }`}
            >
              <Link
                href={hrefActividad}
                className="text-[var(--color-primary)] transition hover:text-[#0B314D] focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-[#4FB3D9]/30"
              >
                {actividad.titulo}
              </Link>
            </h3>
            <p className="mt-2 flex items-start gap-2 text-sm font-semibold text-[var(--color-muted)]">
              <IconoUbicacion />
              <span className="line-clamp-1">
                {ubicacion || "Ubicación a confirmar"}
              </span>
            </p>
          </div>

          {precioVisible ? (
            <span className="shrink-0 rounded-[14px] bg-[#F8FAFC] px-3 py-2 text-right text-xs font-extrabold text-[var(--color-primary)] ring-1 ring-[#D9E2EC]">
              Desde
              <strong className="block text-sm">{precioVisible}</strong>
            </span>
          ) : null}
        </div>

        {esFeed && actividad.descripcion ? (
          <p className="mt-3 line-clamp-2 text-sm leading-6 text-[var(--color-muted)]">
            {actividad.descripcion}
          </p>
        ) : null}

        <div className={`flex flex-wrap gap-2 ${esFeed ? "mt-4" : "mt-3"}`}>
          {actividad.modalidad ? (
            <Etiqueta>{formatearEtiquetaCatalogo(actividad.modalidad)}</Etiqueta>
          ) : null}
          {actividad.nivel ? (
            <Etiqueta>{formatearEtiquetaCatalogo(actividad.nivel)}</Etiqueta>
          ) : null}
          {actividad.cuposLimitados ? <Etiqueta>Cupos limitados</Etiqueta> : null}
        </div>

        {/* mt-auto ancla el CTA al fondo para que las cards vecinas alineen sus botones. */}
        <div className={`mt-auto ${esFeed ? "pt-5" : "pt-4"}`}>
          <AppLinkButton
            href={hrefActividad}
            fullWidth
            size={esFeed ? "md" : "sm"}
            className="gap-2"
          >
            Ver actividad
            <span aria-hidden="true">→</span>
          </AppLinkButton>
        </div>
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
