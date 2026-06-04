import { SearchBar } from "./SearchBar";
import { PopularCategories } from "./PopularCategories";

export function HomeHero() {
  return (
    <div className="max-w-2xl">
      {/* Etiqueta superior del hero */}
      <p className="mb-3 text-sm font-bold uppercase tracking-[0.2em] text-[var(--color-secondary)]">
        Guía deportiva local
      </p>

      {/* Título principal de la home */}
      <h1 className="text-4xl font-extrabold leading-tight text-[var(--color-primary)] sm:text-5xl">
        Encontrá dónde entrenar cerca tuyo
      </h1>

      {/* Descripción breve de la plataforma */}
      <p className="mt-5 text-base leading-7 text-[var(--color-muted)] sm:text-lg">
        Buscá deportes, clubes, profesores, gimnasios y actividades deportivas
        dentro de tu ciudad de forma simple y rápida.
      </p>

      {/* Buscador visual. Más adelante lo conectamos con búsqueda real */}
      <SearchBar />

      {/* Categorías populares visuales. Más adelante pueden venir del backend */}
      <PopularCategories />
    </div>
  );
}