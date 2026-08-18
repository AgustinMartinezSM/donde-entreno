import Image from "next/image";
import Link from "next/link";

import type { Deporte } from "../../types/deporte";
import {
  obtenerBadgeStoryDeporte,
  obtenerImagenFallbackActividad,
} from "../../lib/activityImages";

type HomeStoriesDeportesProps = {
  deportes: Deporte[];
  ciudadSlug: string;
};

/*
  Fila de descubrimiento tipo "stories": círculos por deporte que abren
  la landing de ese deporte en la ciudad activa.

  Se arma con el catálogo real de deportes, no con una lista fija: si el
  backend suma uno, aparece solo. La imagen es la ilustración por deporte
  que ya usan las cards; cuando un deporte no tiene ilustración propia,
  cae al placeholder general.
*/
export function HomeStoriesDeportes({
  deportes,
  ciudadSlug,
}: HomeStoriesDeportesProps) {
  if (deportes.length === 0) {
    return null;
  }

  return (
    <section className="mt-5" aria-labelledby="stories-deportes-titulo">
      <h2 id="stories-deportes-titulo" className="sr-only">
        Deportes para descubrir
      </h2>

      {/*
        -mx-4 + px-4: el carrusel sangra hasta el borde en mobile.

        py-2: overflow-x-auto también recorta en vertical, así que sin
        aire arriba y abajo el anillo de los círculos quedaba cortado.
      */}
      <ul className="-mx-4 flex snap-x snap-mandatory gap-4 overflow-x-auto px-4 py-2 [scrollbar-width:none] sm:mx-0 sm:px-0 [&::-webkit-scrollbar]:hidden">
        {deportes.map((deporte) => {
          const badge = obtenerBadgeStoryDeporte(deporte.slug);

          return (
            <li key={deporte.slug} className="shrink-0 snap-start">
              <Link
                href={`/ciudades/${encodeURIComponent(
                  ciudadSlug
                )}/${encodeURIComponent(deporte.slug)}`}
                className="group flex w-[4.75rem] flex-col items-center gap-2"
              >
                <span
                  className={`relative block h-[4.25rem] w-[4.25rem] overflow-hidden rounded-full transition duration-200 ease-out ${
                    badge
                      ? /*
                          El badge trae su propio anillo verde dibujado:
                          sumarle el ring de CSS lo duplicaba. La sombra
                          al hover reemplaza el cambio de color del ring.
                        */
                        "group-hover:-translate-y-0.5 group-hover:drop-shadow-[0_6px_14px_rgba(15,61,94,0.25)]"
                      : "bg-[#E8F6FB] ring-2 ring-[var(--color-secondary)] ring-offset-2 ring-offset-[var(--color-bg)] group-hover:ring-[var(--color-primary)]"
                  }`}
                >
                  {badge ? (
                    <Image
                      src={badge}
                      alt=""
                      fill
                      sizes="68px"
                      className="object-contain"
                    />
                  ) : (
                    /*
                      Sin badge propio: la ilustración de card es ancha,
                      recortada a un círculo de 68px se ve sobre todo el
                      fondo. El zoom al centro deja a la vista el objeto
                      que distingue al deporte (la raqueta, la pelota).
                    */
                    <Image
                      src={obtenerImagenFallbackActividad({
                        deporteSlug: deporte.slug,
                      })}
                      alt=""
                      fill
                      sizes="68px"
                      className="scale-[1.75] object-cover object-[center_45%] transition duration-200 ease-out group-hover:scale-[1.85]"
                    />
                  )}
                </span>
                <span className="w-full truncate text-center text-xs font-bold text-[var(--color-primary)]">
                  {deporte.nombre}
                </span>
              </Link>
            </li>
          );
        })}
      </ul>
    </section>
  );
}
