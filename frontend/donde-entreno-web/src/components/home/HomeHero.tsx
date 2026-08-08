import Image from "next/image";
import Link from "next/link";
import { AsistenteHomeButton } from "./AsistenteHomeButton";
import { SearchBar } from "./SearchBar";

const busquedasRapidas = [
  { nombre: "Fútbol", texto: "futbol" },
  { nombre: "Gimnasio", texto: "gimnasio" },
  { nombre: "Boxeo", texto: "boxeo" },
  { nombre: "Yoga", texto: "yoga" },
  { nombre: "Jiu Jitsu", texto: "jiu jitsu" },
  { nombre: "Running", texto: "running" },
];

type HomeHeroProps = {
  ciudadNombreInicial: string;
  ciudadSlugInicial: string;
};

function crearHrefExplorarCiudad(ciudadSlug: string, texto?: string) {
  const params = new URLSearchParams();

  params.set("ciudadSlug", ciudadSlug);

  if (texto) {
    params.set("texto", texto);
  }

  return "/explorar?" + params.toString();
}

export function HomeHero({
  ciudadNombreInicial,
  ciudadSlugInicial,
}: HomeHeroProps) {
  return (
    <section className="relative w-full min-w-0 overflow-hidden rounded-[26px] border border-[#D9E2EC] bg-white px-5 py-7 shadow-[0_18px_50px_rgba(15,61,94,0.10)] sm:px-8 sm:py-9 lg:px-10">
      <div
        aria-hidden="true"
        className="absolute -right-24 -top-28 h-64 w-64 rounded-full bg-[#E8F6FB] blur-2xl"
      />

      <div className="relative grid min-w-0 gap-8 lg:grid-cols-[minmax(0,1fr)_360px] lg:items-center">
        <div className="min-w-0">
          <div className="inline-flex items-center gap-2 rounded-full bg-[#E6F7EF] px-3 py-2 text-xs font-extrabold uppercase tracking-[0.14em] text-[#1D7B4A]">
            <IconoUbicacion />
            Descubriendo en {ciudadNombreInicial}
          </div>

          <h1 className="mt-5 max-w-3xl text-[2.35rem] font-extrabold leading-[1.08] text-[var(--color-primary)] sm:text-5xl lg:text-[3.35rem]">
            Tu próxima actividad puede estar{" "}
            <span className="text-[var(--color-secondary)]">cerca tuyo</span>
          </h1>

          <p className="mt-4 max-w-2xl text-base leading-7 text-[var(--color-muted)] sm:text-lg">
            Descubrí clubes, profes y espacios para entrenar, guardá lo que te
            interesa y seguí la actividad deportiva de tu ciudad.
          </p>

          <SearchBar ciudadSlugActual={ciudadSlugInicial} />

          <div className="mt-4 flex flex-col gap-3 sm:flex-row sm:items-center">
            <Link
              href={crearHrefExplorarCiudad(ciudadSlugInicial)}
              className="inline-flex min-h-11 items-center justify-center rounded-[16px] bg-[var(--color-primary)] px-5 py-2.5 text-sm font-extrabold text-white shadow-[var(--shadow-button)] transition duration-200 ease-out hover:-translate-y-0.5 hover:bg-[#0B314D] active:scale-[0.98]"
            >
              Explorar en {ciudadNombreInicial}
            </Link>
            <AsistenteHomeButton />
          </div>

          <div className="mt-6 flex min-w-0 gap-2 overflow-x-auto pb-2 sm:flex-wrap sm:overflow-visible sm:pb-0">
            {busquedasRapidas.map((busqueda) => (
              <Link
                key={busqueda.texto}
                href={crearHrefExplorarCiudad(
                  ciudadSlugInicial,
                  busqueda.texto
                )}
                className="shrink-0 rounded-full border border-[#D9E2EC] bg-[#F8FAFC] px-3.5 py-2 text-sm font-bold text-[var(--color-primary)] transition duration-200 ease-out hover:-translate-y-0.5 hover:border-[#BFDDEA] hover:bg-white active:scale-[0.98]"
              >
                {busqueda.nombre}
              </Link>
            ))}
          </div>
        </div>

        <div className="relative hidden overflow-hidden rounded-[24px] border border-[#BFDDEA] bg-[#E8F6FB] p-3 shadow-[0_18px_45px_rgba(15,61,94,0.12)] lg:block">
          <div className="relative h-[390px] overflow-hidden rounded-[19px]">
            {/* Sin priority: el contenedor es hidden hasta lg y el preload competía con el LCP real en mobile. */}
            <Image
              src="/sports/sport-running.png"
              alt="Ilustración de running y deporte local"
              fill
              sizes="360px"
              className="object-cover object-center"
            />
            <div className="absolute inset-0 bg-gradient-to-t from-[#0F3D5E]/80 via-transparent to-transparent" />
            <div className="absolute inset-x-0 bottom-0 p-5 text-white">
              <p className="text-xs font-extrabold uppercase tracking-[0.16em] text-[#7FDCA8]">
                Movete a tu manera
              </p>
              <p className="mt-2 text-2xl font-extrabold leading-tight">
                Una comunidad local para encontrar tu lugar
              </p>
              <div className="mt-4 flex flex-wrap gap-2 text-xs font-bold">
                <span className="rounded-full bg-white/15 px-3 py-1.5 backdrop-blur">
                  Descubrí
                </span>
                <span className="rounded-full bg-white/15 px-3 py-1.5 backdrop-blur">
                  Guardá
                </span>
                <span className="rounded-full bg-white/15 px-3 py-1.5 backdrop-blur">
                  Seguí
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
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
      className="h-4 w-4"
      aria-hidden="true"
    >
      <path d="M20 10c0 5-8 11-8 11S4 15 4 10a8 8 0 1 1 16 0z" />
      <circle cx="12" cy="10" r="2.5" />
    </svg>
  );
}
