import Image from "next/image";
import Link from "next/link";
import { obtenerImagenFallbackActividad } from "../../lib/activityImages";
import type { Deporte } from "../../types/deporte";

type SportCatalogCardProps = {
  deporte: Deporte;
};

export function SportCatalogCard({ deporte }: SportCatalogCardProps) {
  const imagen = obtenerImagenFallbackActividad({
    deporteSlug: deporte.slug,
  });

  return (
    <Link
      href={`/explorar?deporteSlug=${encodeURIComponent(deporte.slug)}&page=0`}
      className="group overflow-hidden rounded-[var(--radius-lg)] border border-[var(--color-border)] bg-[var(--color-surface)] shadow-[var(--shadow-card)] transition duration-200 ease-out hover:-translate-y-1 hover:border-[#BFDDEA] hover:shadow-[0_18px_45px_rgba(12,52,80,0.13)] active:scale-[0.98]"
    >
      <div className="relative h-44 overflow-hidden bg-[#E8F6FB] sm:h-48">
        <Image
          src={imagen}
          alt=""
          fill
          sizes="(max-width: 640px) 100vw, (max-width: 1024px) 50vw, 340px"
          className="object-cover transition duration-300 ease-out group-hover:scale-105"
        />
        <div className="absolute inset-0 bg-gradient-to-t from-[#0F3D5E]/45 via-[#0F3D5E]/8 to-transparent" />

        {deporte.categoriaNombre ? (
          <span className="absolute left-3 top-3 rounded-full bg-white/95 px-3 py-1 text-xs font-bold text-[var(--color-primary)] shadow-sm">
            {deporte.categoriaNombre}
          </span>
        ) : null}
      </div>

      <div className="p-4">
        <h3 className="text-xl font-extrabold text-[var(--color-primary)]">
          {deporte.nombre}
        </h3>

        {deporte.descripcion ? (
          <p className="mt-2 line-clamp-2 text-sm leading-6 text-[var(--color-muted)]">
            {deporte.descripcion}
          </p>
        ) : null}

        <div className="mt-4 inline-flex rounded-full bg-[#E6F7EF] px-3 py-1 text-xs font-bold text-[#167A4A] transition duration-200 ease-out group-hover:bg-[var(--color-primary)] group-hover:text-white">
          Ver actividades
        </div>
      </div>
    </Link>
  );
}
