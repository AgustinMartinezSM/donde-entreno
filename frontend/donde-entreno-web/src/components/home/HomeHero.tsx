import Image from "next/image";
import Link from "next/link";
import { SearchBar } from "./SearchBar";

const busquedasRapidas = [
  {
    nombre: "Fútbol",
    href: "/explorar?texto=futbol",
  },
  {
    nombre: "Gimnasio",
    href: "/explorar?texto=gimnasio",
  },
  {
    nombre: "Boxeo",
    href: "/explorar?texto=boxeo",
  },
  {
    nombre: "Yoga",
    href: "/explorar?texto=yoga",
  },
  {
    nombre: "Jiu Jitsu",
    href: "/explorar?texto=jiu%20jitsu",
  },
  {
    nombre: "Running",
    href: "/explorar?texto=running",
  },
];

const beneficios = [
  "Clubes, profes y gimnasios",
  "Búsqueda por deporte o zona",
  "Contacto directo con cada actividad",
];

export function HomeHero() {
  return (
    <section className="relative overflow-hidden rounded-[var(--radius-xl)] border border-[#DDEAF3] bg-gradient-to-br from-white via-[#F8FCFE] to-[#E8F6FB] p-5 shadow-[0_24px_60px_rgba(12,52,80,0.14)] sm:p-8 lg:p-10">
      <div className="grid gap-8 lg:grid-cols-[1.25fr_0.75fr] lg:items-center">
        <div className="relative z-10">
          <p className="mb-3 text-sm font-bold uppercase tracking-[0.2em] text-[var(--color-secondary)]">
            Guía deportiva local
          </p>

          <h1 className="max-w-3xl text-4xl font-extrabold leading-tight text-[var(--color-primary)] sm:text-5xl lg:text-6xl">
            Encontrá dónde entrenar cerca tuyo
          </h1>

          <p className="mt-5 max-w-2xl text-base leading-7 text-[var(--color-muted)] sm:text-lg">
            Descubrí clubes, profes, gimnasios y actividades deportivas en tu
            ciudad.
          </p>

          <SearchBar />

          <div className="mt-5 flex flex-col gap-3 sm:flex-row">
            <Link
              href="/explorar"
              className="rounded-[var(--radius-md)] bg-[var(--color-primary)] px-5 py-3 text-center text-sm font-bold text-white shadow-[var(--shadow-button)] transition duration-200 ease-out hover:-translate-y-0.5 hover:bg-[#0B314D] active:scale-[0.98]"
            >
              Explorar actividades
            </Link>
            <Link
              href="/publicar"
              className="rounded-[var(--radius-md)] border border-[#BFDDEA] bg-white px-5 py-3 text-center text-sm font-bold text-[var(--color-primary)] transition duration-200 ease-out hover:-translate-y-0.5 hover:border-[var(--color-primary)] active:scale-[0.98]"
            >
              Publicar actividad
            </Link>
          </div>

          <div className="mt-6 flex flex-wrap gap-2">
            {busquedasRapidas.map((busqueda) => (
              <Link
                key={busqueda.href}
                href={busqueda.href}
                className="rounded-full border border-[#DDEAF3] bg-white px-3 py-2 text-sm font-bold text-[var(--color-primary)] shadow-sm transition duration-200 ease-out hover:-translate-y-0.5 hover:border-[#BFDDEA] hover:bg-[#F8FCFE] active:scale-[0.98]"
              >
                {busqueda.nombre}
              </Link>
            ))}
          </div>
        </div>

        <aside className="relative z-10 rounded-[var(--radius-xl)] border border-[#BDE8D0] bg-white/85 p-3 shadow-[0_20px_50px_rgba(12,52,80,0.14)] backdrop-blur sm:p-4">
          <div className="relative overflow-hidden rounded-[22px]">
            <div className="relative aspect-[4/3] min-h-64">
              <Image
                src="/sports/sport-boxeo.png"
                alt=""
                fill
                priority
                sizes="(max-width: 1024px) 100vw, 420px"
                className="object-cover object-[center_42%]"
              />
              <div className="absolute inset-0 bg-gradient-to-t from-[#0F3D5E]/85 via-[#0F3D5E]/20 to-transparent" />
              <div className="absolute bottom-0 left-0 right-0 p-4 text-white">
                <p className="inline-flex rounded-full bg-white/95 px-3 py-1 text-xs font-extrabold uppercase tracking-[0.14em] text-[var(--color-primary)] shadow-sm">
                  Actividades cerca tuyo
                </p>
                <p className="mt-2 text-2xl font-extrabold leading-tight">
                  Entrená donde mejor te quede
                </p>
              </div>
            </div>
          </div>

          <div className="mt-3 grid grid-cols-2 gap-3">
            <div className="relative overflow-hidden rounded-[18px] border border-[#DDEAF3]">
              <div className="relative h-24">
                <Image
                  src="/sports/sport-yoga.png"
                  alt=""
                  fill
                  sizes="(max-width: 1024px) 50vw, 200px"
                  className="object-cover object-[center_45%]"
                />
                <div className="absolute inset-0 bg-gradient-to-t from-[#0F3D5E]/70 to-transparent" />
                <p className="absolute bottom-3 left-3 text-sm font-extrabold text-white">
                  Yoga
                </p>
              </div>
            </div>
            <div className="relative overflow-hidden rounded-[18px] border border-[#DDEAF3]">
              <div className="relative h-24">
                <Image
                  src="/sports/sport-running.png"
                  alt=""
                  fill
                  sizes="(max-width: 1024px) 50vw, 200px"
                  className="object-cover object-[center_45%]"
                />
                <div className="absolute inset-0 bg-gradient-to-t from-[#0F3D5E]/70 to-transparent" />
                <p className="absolute bottom-3 left-3 text-sm font-extrabold text-white">
                  Running
                </p>
              </div>
            </div>
          </div>

          <div className="mt-3 grid gap-3">
            {beneficios.map((beneficio, indice) => (
              <div
                key={beneficio}
                className="flex items-center gap-3 rounded-[var(--radius-md)] border border-[#DDEAF3] bg-[#F8FCFE] p-3"
              >
                <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-[#E6F7EF] text-sm font-extrabold text-[#167A4A]">
                  {indice + 1}
                </span>
                <p className="text-sm font-bold leading-5 text-[var(--color-primary)]">
                  {beneficio}
                </p>
              </div>
            ))}
          </div>
        </aside>
      </div>
    </section>
  );
}
