import Image from "next/image";

const pasos = [
  {
    titulo: "Buscá por deporte o zona",
    texto: "Usá el buscador para encontrar actividades cerca tuyo.",
    icono: "/icons/icon-search.png",
  },
  {
    titulo: "Compará opciones",
    texto: "Revisá horarios, ubicación, nivel y modalidad.",
    icono: "/icons/icon-location.png",
  },
  {
    titulo: "Contactá directo",
    texto: "Hablá con el club, profe o espacio deportivo.",
    icono: "/icons/icon-contact.png",
  },
];

export function HomeHowItWorks() {
  return (
    <section className="mt-16 rounded-[var(--radius-xl)] border border-[#DDEAF3] bg-gradient-to-br from-white via-[#F8FCFE] to-[#E8F6FB] p-5 shadow-[0_18px_45px_rgba(12,52,80,0.10)] sm:mt-20 sm:p-7">
      <div className="max-w-2xl">
        <p className="text-sm font-bold uppercase tracking-[0.2em] text-[var(--color-secondary)]">
          Cómo funciona
        </p>
        <h2 className="mt-2 text-2xl font-extrabold text-[var(--color-primary)] sm:text-3xl">
          Encontrar dónde entrenar es simple
        </h2>
      </div>

      <div className="mt-7 grid gap-4 md:grid-cols-3">
        {pasos.map((paso, indice) => (
          <article
            key={paso.titulo}
            className="rounded-[var(--radius-lg)] border border-[var(--color-border)] bg-[var(--color-surface)] p-5 shadow-[var(--shadow-card)] transition duration-200 ease-out hover:-translate-y-0.5 hover:border-[#BFDDEA] hover:shadow-[0_14px_35px_rgba(12,52,80,0.10)]"
          >
            <div className="flex h-12 w-12 items-center justify-center rounded-full bg-[#E8F6FB]">
              <Image src={paso.icono} alt="" width={24} height={24} />
            </div>
            <p className="mt-5 text-xs font-extrabold uppercase tracking-[0.18em] text-[var(--color-secondary)]">
              Paso {indice + 1}
            </p>
            <h3 className="mt-2 text-lg font-extrabold text-[var(--color-primary)]">
              {paso.titulo}
            </h3>
            <p className="mt-2 text-sm leading-6 text-[var(--color-muted)]">
              {paso.texto}
            </p>
          </article>
        ))}
      </div>
    </section>
  );
}
