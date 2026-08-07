import Image from "next/image";
import Link from "next/link";
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
};

/*
  Linkeamos a las landings territoriales (/ciudades/[ciudad]/[deporte]),
  que son indexables y tienen contenido SEO propio, en lugar de URLs de
  /explorar con query params.
*/
function crearHrefDeporte(ciudadSlug: string, deporteSlug: string) {
  return `/ciudades/${encodeURIComponent(ciudadSlug)}/${encodeURIComponent(deporteSlug)}`;
}

export function HomePopularSports({ ciudadSlug }: HomePopularSportsProps) {
  return (
    <section className="relative mt-14 sm:mt-18">
      <SectionHeader
        eyebrow="Explorá por deporte"
        title="Encontrá una actividad que se parezca a vos"
        description="Elegí un interés y conectá con clubes, profes y espacios de tu ciudad."
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
        {deportesPopulares.map((deporte) => (
          <Link
            key={deporte.deporteSlug}
            href={crearHrefDeporte(ciudadSlug, deporte.deporteSlug)}
            className="group overflow-hidden rounded-[var(--radius-lg)] border border-[var(--color-border)] bg-[var(--color-surface)] shadow-[var(--shadow-card)] transition duration-200 ease-out hover:-translate-y-1 hover:border-[#BFDDEA] hover:shadow-[0_18px_45px_rgba(12,52,80,0.13)] active:scale-[0.98]"
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
              <span className="rounded-full bg-[#E6F7EF] px-3 py-1 text-xs font-bold text-[#167A4A] transition duration-200 ease-out group-hover:bg-[var(--color-primary)] group-hover:text-white">
                Ver actividades
              </span>
            </div>
          </Link>
        ))}
      </div>
    </section>
  );
}
