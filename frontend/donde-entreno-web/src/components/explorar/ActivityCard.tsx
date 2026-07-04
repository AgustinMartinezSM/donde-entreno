import Image from "next/image";

import type { Actividad } from "../../types/actividad";
import { API_BASE_URL } from "../../lib/apiConfig";
import { ActivityImage } from "../actividad/ActivityImage";
import {
  obtenerImagenActividad,
  obtenerImagenFallbackActividad,
} from "../../lib/activityImages";
import { AppLinkButton } from "../ui/AppLinkButton";
import { SurfaceCard } from "../ui/SurfaceCard";

type ActivityCardProps = {
  actividad: Actividad;
};

export function ActivityCard({ actividad }: ActivityCardProps) {
  /*
    Si la actividad trae imagen propia desde el backend,
    armamos la URL completa usando API_BASE_URL.

    Si no trae imagen propia, dejamos null para que después
    obtenerImagenActividad use la imagen default según el deporte.
  */
  const imagenBackend = construirUrlImagenBackend(actividad.imagenPrincipalUrl);

  /*
    Definimos qué imagen mostrar en la card.

    Prioridad:
    1. Imagen propia desde backend.
    2. Imagen default por deporteSlug.
    3. Placeholder general.
  */
  const imagenUrl = obtenerImagenActividad({
    imagenBackend,
    deporteSlug: actividad.deporteSlug,
  });
  const imagenFallbackUrl = obtenerImagenFallbackActividad({
    deporteSlug: actividad.deporteSlug,
  });
  const tieneContacto = Boolean(
    actividad.whatsappContacto || actividad.instagramContacto || actividad.emailContacto
  );

  return (
    <SurfaceCard
      as="article"
      className="group overflow-hidden p-3 transition duration-200 ease-out hover:-translate-y-1 hover:border-[#BFDDEA] hover:shadow-[0_22px_55px_rgba(12,52,80,0.14)] active:scale-[0.995]"
    >
      <ActivityImage
        src={imagenUrl}
        fallbackSrc={imagenFallbackUrl}
        alt={actividad.titulo || actividad.deporteNombre || "Actividad deportiva"}
        fallbackText={actividad.deporteNombre || "Actividad"}
        heightClassName="h-48"
      />

      <div className="p-2 pt-4">
        <div className="mb-3 flex flex-wrap gap-2">
          {actividad.deporteNombre && (
            <span className="rounded-full bg-[#E6F7EF] px-3 py-1 text-xs font-extrabold text-[#167A4A]">
              {actividad.deporteNombre}
            </span>
          )}

          {actividad.mostrarPrecio &&
            actividad.precioReferencia !== undefined &&
            actividad.precioReferencia !== null && (
            <span className="rounded-full bg-white px-3 py-1 text-xs font-extrabold text-[var(--color-primary)] ring-1 ring-[#DDEAF3]">
              Desde ${actividad.precioReferencia}
            </span>
          )}
        </div>

        <h3 className="line-clamp-2 text-lg font-extrabold text-[var(--color-primary)]">
          {actividad.titulo}
        </h3>

        <p className="mt-2 line-clamp-1 text-sm font-bold text-[var(--color-muted)]">
          {actividad.perfilPublicadorNombre || "Publicado por la comunidad"}
        </p>

        <div className="mt-3 flex items-start gap-2 rounded-[var(--radius-md)] bg-[#F8FAFC] px-3 py-2 text-sm font-medium text-[var(--color-text)]">
          <Image
            src="/icons/icon-location.png"
            alt=""
            width={16}
            height={16}
            aria-hidden="true"
            className="mt-0.5 h-4 w-4 shrink-0"
          />

          <p>
            {actividad.barrioNombre || "Zona a confirmar"}
            {actividad.ciudadNombre ? `, ${actividad.ciudadNombre}` : ""}
          </p>
        </div>

        <div className="mt-3 flex flex-wrap gap-2">
          {actividad.nivel && (
            <span className="rounded-full bg-[#E6F7EF] px-3 py-1 text-xs font-bold text-[#167A4A]">
              {actividad.nivel}
            </span>
          )}

          {actividad.modalidad && (
            <span className="rounded-full bg-[#E8F6FB] px-3 py-1 text-xs font-bold text-[#0F6F8F]">
              {actividad.modalidad}
            </span>
          )}

          <span className="rounded-full bg-[#F8FAFC] px-3 py-1 text-xs font-bold text-[var(--color-muted)] ring-1 ring-[#DDEAF3]">
            {tieneContacto ? "Contacto disponible" : "Contacto a confirmar"}
          </span>
        </div>

        <AppLinkButton
          href={`/actividades/${actividad.slug}`}
          fullWidth
          className="mt-5 group-hover:bg-[#0B314D]"
        >
          Ver detalle
        </AppLinkButton>
      </div>
    </SurfaceCard>
  );
}

function construirUrlImagenBackend(url?: string | null) {
  const urlLimpia = url?.trim();

  if (!urlLimpia) {
    return null;
  }

  if (urlLimpia.startsWith("http://") || urlLimpia.startsWith("https://")) {
    return urlLimpia;
  }

  const separador = urlLimpia.startsWith("/") ? "" : "/";

  return `${API_BASE_URL}${separador}${urlLimpia}`;
}
