import Image from "next/image";
import Link from "next/link";
import { obtenerImagenFallbackActividad } from "../../lib/activityImages";
import { AppLinkButton } from "../ui/AppLinkButton";
import { SectionHeader } from "../ui/SectionHeader";

const deportesPopulares = [
  {
    nombre: "Boxeo",
    deporteSlug: "boxeo",
    imagen: "/sports/sport-boxeo.png",
    encuadre: "center 42%",
  },
  {
    nombre: "Jiu Jitsu",
    deporteSlug: "jiu-jitsu",
    imagen: "/sports/sport-jiu-jitsu.png",
    encuadre: "center 46%",
  },
  {
    nombre: "Fútbol",
    deporteSlug: "futbol",
    imagen: "/sports/sport-futbol.png",
    encuadre: "center 50%",
  },
  {
    nombre: "Yoga",
    deporteSlug: "yoga",
    imagen: "/sports/sport-yoga.png",
    encuadre: "center 45%",
  },
  {
    /*
      El deporte del seed es "musculacion" (no existe slug "gimnasio":
      la card anterior llevaba a un listado vacío).
    */
    nombre: "Musculación",
    deporteSlug: "musculacion",
    imagen: "/sports/sport-gimnasio.png",
    encuadre: "center 44%",
  },
  {
    nombre: "Natación",
    deporteSlug: "natacion",
    imagen: "/sports/sport-natacion.png",
    encuadre: "center 48%",
  },
];

type HomePopularSportsProps = {
  ciudadSlug: string;
  /*
    Fase 6: los deportes más vistos, derivados del tracking anónimo.
    Vacío = no hay señal suficiente (o el backend todavía no expone el
    endpoint), y entonces se muestra la selección curada de abajo.
  */
  populares?: DeportePopular[];
};

export type DeportePopular = {
  slug: string;
  nombre: string;
};

/*
  Encuadres afinados a mano para las seis imágenes propias. Un deporte
  del ranking sin imagen propia cae al fallback por deporte, que ya
  resuelve activityImages.
*/
const ENCUADRE_POR_DEFECTO = "center 45%";

/*
  Linkeamos a las landings territoriales (/ciudades/[ciudad]/[deporte]),
  que son indexables y tienen contenido SEO propio, en lugar de URLs de
  /explorar con query params.
*/
function crearHrefDeporte(ciudadSlug: string, deporteSlug: string) {
  return `/ciudades/${encodeURIComponent(ciudadSlug)}/${encodeURIComponent(deporteSlug)}`;
}

/* Las seis imágenes propias, reusadas cuando el ranking las incluye. */
function imagenPropiaPorSlug(slug: string): string | null {
  return (
    deportesPopulares.find((deporte) => deporte.deporteSlug === slug)?.imagen ??
    null
  );
}

function encuadrePorSlug(slug: string): string | null {
  return (
    deportesPopulares.find((deporte) => deporte.deporteSlug === slug)
      ?.encuadre ?? null
  );
}

export function HomePopularSports({
  ciudadSlug,
  populares = [],
}: HomePopularSportsProps) {
  /*
    Con señal real la sección habla de lo que la gente MIRA; sin ella
    cae a la selección curada de siempre. Nunca se presenta como
    "populares" algo que no salió de datos.
  */
  const hayRanking = populares.length > 0;
  const deportesAMostrar = hayRanking
    ? populares.map((deporte) => ({
        nombre: deporte.nombre,
        deporteSlug: deporte.slug,
        imagen:
          imagenPropiaPorSlug(deporte.slug) ??
          obtenerImagenFallbackActividad({ deporteSlug: deporte.slug }),
        encuadre: encuadrePorSlug(deporte.slug) ?? ENCUADRE_POR_DEFECTO,
      }))
    : deportesPopulares;

  return (
    <section className="relative mt-14 sm:mt-18">
      <SectionHeader
        eyebrow={hayRanking ? "Lo más visto" : "Explorá por deporte"}
        title={
          hayRanking
            ? "Lo que más está mirando la gente"
            : "Encontrá una actividad que se parezca a vos"
        }
        description={
          hayRanking
            ? "Los deportes más consultados en DondeEntreno durante el último mes."
            : "Elegí un interés y conectá con clubes, profes y espacios de tu ciudad."
        }
        action={
          <AppLinkButton
            href="/deportes"
            variant="secondary"
            size="md"
            className="w-fit"
          >
            Ver deportes
          </AppLinkButton>
        }
      />

      <div className="mt-6 grid auto-cols-[82%] grid-flow-col gap-4 overflow-x-auto pb-3 sm:auto-cols-auto sm:grid-flow-row sm:grid-cols-2 sm:overflow-visible sm:pb-0 lg:grid-cols-3">
        {deportesAMostrar.map((deporte) => (
          <Link
            key={deporte.deporteSlug}
            href={crearHrefDeporte(ciudadSlug, deporte.deporteSlug)}
            className="group overflow-hidden rounded-[var(--radius-lg)] border border-[var(--color-border)] bg-[var(--color-surface)] shadow-[var(--shadow-card)] transition duration-200 ease-out hover:-translate-y-1 hover:border-[var(--color-border-accent)] hover:shadow-[0_18px_45px_rgba(12,52,80,0.13)] active:scale-[0.98]"
          >
            <div className="relative h-44 overflow-hidden sm:h-48">
              <Image
                src={deporte.imagen}
                alt=""
                fill
                sizes="(max-width: 640px) 100vw, (max-width: 1024px) 50vw, 360px"
                className="object-cover transition duration-300 ease-out group-hover:scale-105"
                style={{ objectPosition: deporte.encuadre }}
              />
              <div className="absolute inset-0 bg-gradient-to-t from-[#0F3D5E]/30 via-transparent to-transparent" />
            </div>
            <div className="flex items-center justify-between gap-3 p-4">
              <span className="text-xl font-extrabold text-[var(--color-primary)]">
                {deporte.nombre}
              </span>
              <span className="rounded-full bg-[var(--color-success-soft)] px-3 py-1 text-xs font-bold text-[var(--color-success)] transition duration-200 ease-out group-hover:bg-[var(--color-brand)] group-hover:text-white">
                Ver actividades
              </span>
            </div>
          </Link>
        ))}
      </div>
    </section>
  );
}
