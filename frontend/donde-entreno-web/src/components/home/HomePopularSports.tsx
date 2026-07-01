import Image from "next/image";
import Link from "next/link";

const deportesPopulares = [
  {
    nombre: "Boxeo",
    href: "/explorar?deporteSlug=boxeo&page=0",
    imagen: "/sports/sport-boxeo.png",
    encuadre: "center 42%",
  },
  {
    nombre: "Jiu Jitsu",
    href: "/explorar?deporteSlug=jiu-jitsu&page=0",
    imagen: "/sports/sport-jiu-jitsu.png",
    encuadre: "center 46%",
  },
  {
    nombre: "Fútbol",
    href: "/explorar?deporteSlug=futbol&page=0",
    imagen: "/sports/sport-futbol.png",
    encuadre: "center 50%",
  },
  {
    nombre: "Yoga",
    href: "/explorar?deporteSlug=yoga&page=0",
    imagen: "/sports/sport-yoga.png",
    encuadre: "center 45%",
  },
  {
    nombre: "Gimnasio",
    href: "/explorar?deporteSlug=gimnasio&page=0",
    imagen: "/sports/sport-gimnasio.png",
    encuadre: "center 44%",
  },
  {
    nombre: "Natación",
    href: "/explorar?deporteSlug=natacion&page=0",
    imagen: "/sports/sport-natacion.png",
    encuadre: "center 48%",
  },
];

export function HomePopularSports() {
  return (
    <section className="relative mt-16 rounded-[var(--radius-xl)] border border-[#DDEAF3] bg-white/75 p-4 shadow-[0_16px_40px_rgba(12,52,80,0.08)] sm:mt-20 sm:p-6">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <p className="text-sm font-bold uppercase tracking-[0.2em] text-[var(--color-secondary)]">
            Deportes populares
          </p>
          <h2 className="mt-2 text-2xl font-extrabold text-[var(--color-primary)] sm:text-3xl">
            Explorá por deporte
          </h2>
          <p className="mt-2 max-w-2xl text-sm leading-6 text-[var(--color-muted)] sm:text-base">
            Encontrá actividades según lo que te gusta hacer.
          </p>
        </div>

        <Link
          href="/deportes"
          className="w-fit rounded-[var(--radius-md)] border border-[#BFDDEA] bg-white px-4 py-3 text-sm font-bold text-[var(--color-primary)] shadow-sm transition duration-200 ease-out hover:-translate-y-0.5 hover:border-[var(--color-primary)] hover:bg-[#F8FCFE] active:scale-[0.98]"
        >
          Ver todos
        </Link>
      </div>

      <div className="mt-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {deportesPopulares.map((deporte) => (
          <Link
            key={deporte.href}
            href={deporte.href}
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
                Ver opciones
              </span>
            </div>
          </Link>
        ))}
      </div>
    </section>
  );
}
