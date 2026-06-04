import Link from "next/link";

const categoriasPopulares = [
  {
    nombre: "Fútbol",
    busqueda: "futbol",
    variante: "verde",
  },
  {
    nombre: "Gimnasio",
    busqueda: "gimnasio",
    variante: "verde",
  },
  {
    nombre: "Boxeo",
    busqueda: "boxeo",
    variante: "celeste",
  },
  {
    nombre: "Yoga",
    busqueda: "yoga",
    variante: "celeste",
  },
];

export function PopularCategories() {
  return (
    <div className="mt-6 flex flex-wrap gap-2">
      {/*
        Categorías populares del MVP.
        Por ahora son fijas, pero ya funcionan como accesos rápidos a /explorar.
      */}
      {categoriasPopulares.map((categoria) => {
        const estilos =
          categoria.variante === "verde"
            ? "bg-[#E6F7EF] text-[#167A4A]"
            : "bg-[#E8F6FB] text-[#0F6F8F]";

        return (
          <Link
            key={categoria.busqueda}
            href={`/explorar?texto=${categoria.busqueda}`}
            className={`rounded-full px-3 py-2 text-sm font-bold transition hover:scale-105 ${estilos}`}
          >
            {categoria.nombre}
          </Link>
        );
      })}
    </div>
  );
}