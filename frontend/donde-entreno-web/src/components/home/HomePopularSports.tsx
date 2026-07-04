import Image from "next/image";
import Link from "next/link";
import { AppLinkButton } from "../ui/AppLinkButton";
import { SectionHeader } from "../ui/SectionHeader";
import { SurfaceCard } from "../ui/SurfaceCard";

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
    nombre: "Gimnasio",
    deporteSlug: "gimnasio",
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

function crearHrefDeporte(ciudadSlug: string, deporteSlug: string) {
  const params = new URLSearchParams();

  params.set("ciudadSlug", ciudadSlug);
  params.set("deporteSlug", deporteSlug);
  params.set("page", "0");

  return `/explorar?${params.toString()}`;
}

export function HomePopularSports({ ciudadSlug }: HomePopularSportsProps) {
  return (
    <SurfaceCard
      as="section"
      variant="soft"
      className="relative mt-16 p-4 sm:mt-20 sm:p-6"
    >
      <SectionHeader
        eyebrow="Deportes populares"
        title="Arrancá por lo que te gusta"
        description="Elegí un deporte y mirá opciones reales para moverte en tu ciudad."
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

      <div className="mt-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {deportesPopulares.map((deporte) => (
          <Link
            key={deporte.deporteSlug}
            href={crearHrefDeporte(ciudadSlug, deporte.deporteSlug)}
            className="group overflow-hidden rounded-[var(--radius-lg)] border border-[var(--color-border)] bg-[var(--color-surface)] shadow-[var(--shadow-card)] transition duration-200 ease-out hover:-translate-y-1 hover:border-[#BFDDEA] hover:shadow-[0_18px_45px_rgba(12,52,80,0.13)] active:scale-[0.98]"
          >
            <div className="relative h-52 overflow-hidden sm:h-56">
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
    </SurfaceCard>
  );
}
