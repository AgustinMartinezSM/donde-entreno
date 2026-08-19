import { AsistenteHomeButton } from "./AsistenteHomeButton";
import { SearchBar } from "./SearchBar";

type HomeTopBarProps = {
  ciudadNombre: string;
  ciudadSlug: string;
};

/*
  Encabezado compacto de la home.

  Reemplaza al hero anterior, que abría con un titular de tres líneas,
  un párrafo explicativo y una imagen de 390px: en mobile había que
  scrollear toda una pantalla antes de ver la primera actividad. Acá
  quedan solo las tres cosas que el visitante necesita de entrada —dónde
  está, buscar, y pedir ayuda si no sabe qué entrenar— y el contenido
  arranca enseguida.

  El h1 se mantiene corto pero presente: la página necesita uno para
  accesibilidad y para SEO.
*/
export function HomeTopBar({ ciudadNombre, ciudadSlug }: HomeTopBarProps) {
  return (
    /*
      El encabezado ahora es una superficie propia y no texto suelto
      sobre el fondo: con la página en blanco plano, el buscador —que es
      lo más importante de la home— no se distinguía de lo que venía
      abajo. El orbe y los puntos son decoración de CSS, sin peso extra.
    */
    <section className="decorative-orb decorative-dots relative w-full min-w-0 overflow-hidden rounded-[var(--radius-xl)] border border-[var(--color-border-soft)] bg-[var(--color-surface)]/70 p-5 shadow-soft backdrop-blur-sm sm:p-6">
      <div className="relative z-10">
        <div className="flex flex-wrap items-center gap-2">
          <span className="inline-flex items-center gap-1.5 rounded-full bg-[var(--color-success-soft)] px-3 py-1.5 text-xs font-extrabold uppercase tracking-[0.12em] text-[var(--color-success)]">
            <IconoUbicacion />
            {ciudadNombre}
          </span>
        </div>

        <h1 className="mt-3 text-2xl font-extrabold leading-tight text-[var(--color-primary)] sm:text-3xl">
          Entrená en{" "}
          <span className="text-[var(--color-secondary)]">{ciudadNombre}</span>
        </h1>

        <SearchBar ciudadSlugActual={ciudadSlug} />

        <div className="mt-3">
          <AsistenteHomeButton />
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
      strokeWidth="2.2"
      strokeLinecap="round"
      strokeLinejoin="round"
      className="h-3.5 w-3.5"
      aria-hidden="true"
    >
      <path d="M12 21s7-6.1 7-11a7 7 0 1 0-14 0c0 4.9 7 11 7 11Z" />
      <circle cx="12" cy="10" r="2.6" />
    </svg>
  );
}
