import Link from "next/link";

export function Footer() {
  /*
    Año actual para que el copyright no quede viejo.
    Esto se calcula automáticamente cuando se renderiza el componente.
  */
  const anioActual = new Date().getFullYear();

  return (
    <footer className="mt-auto border-t border-[#2A5B78] bg-[#08263B] text-white">
      <div className="mx-auto w-full max-w-6xl px-4 py-10 sm:py-12">
        <div className="grid gap-8 md:grid-cols-[1.4fr_1fr_1fr] md:gap-12">
          <div>
            <Link
              href="/"
              className="inline-flex items-center text-2xl font-extrabold tracking-tight transition duration-200 ease-out hover:-translate-y-0.5"
              aria-label="Ir al inicio de DondeEntreno"
            >
              <span className="text-white">Donde</span>
              <span className="text-[var(--color-secondary)]">Entreno</span>
            </Link>

            <p className="mt-4 max-w-md text-sm leading-6 text-[#C9E4EF]">
              La guía deportiva local para descubrir clubes, profes, gimnasios
              y actividades cerca tuyo.
            </p>

            <p className="mt-4 inline-flex rounded-full border border-[#2A5B78] bg-white/5 px-3 py-2 text-xs font-bold uppercase tracking-[0.16em] text-[#A7F3CF]">
              Entrená cerca, elegí mejor
            </p>
          </div>

          <div>
            <h2 className="text-sm font-extrabold uppercase tracking-[0.16em] text-white">
              Navegación
            </h2>

            <nav className="mt-4 flex flex-col gap-3 text-sm font-bold">
              <Link
                href="/"
                className="text-[#C9E4EF] transition duration-200 ease-out hover:text-white"
              >
                Inicio
              </Link>

              <Link
                href="/deportes"
                className="text-[#C9E4EF] transition duration-200 ease-out hover:text-white"
              >
                Deportes
              </Link>

              <Link
                href="/explorar"
                className="text-[#C9E4EF] transition duration-200 ease-out hover:text-white"
              >
                Explorar
              </Link>

              <Link
                href="/publicar"
                className="text-[#C9E4EF] transition duration-200 ease-out hover:text-white"
              >
                Publicar
              </Link>
            </nav>
          </div>

          <div>
            <h2 className="text-sm font-extrabold uppercase tracking-[0.16em] text-white">
              Contacto
            </h2>

            <div className="mt-4 flex flex-col gap-3 text-sm font-bold">
              <a
                href="https://instagram.com/dondeentreno"
                target="_blank"
                rel="noopener noreferrer"
                className="text-[#C9E4EF] transition duration-200 ease-out hover:text-[var(--color-secondary)]"
              >
                Instagram
              </a>

              <a
                href="mailto:contacto@dondeentreno.com"
                className="text-[#C9E4EF] transition duration-200 ease-out hover:text-[var(--color-secondary)]"
              >
                Email
              </a>

              <a
                href="https://wa.me/5492230000000"
                target="_blank"
                rel="noopener noreferrer"
                className="text-[#C9E4EF] transition duration-200 ease-out hover:text-[var(--color-secondary)]"
              >
                WhatsApp
              </a>
            </div>
          </div>
        </div>

        <div className="mt-8 border-t border-[#2A5B78] pt-5 sm:mt-10">
          <p className="text-xs leading-5 text-[#C9E4EF]">
            © {anioActual} DondeEntreno. Todos los derechos reservados.
          </p>
        </div>
      </div>
    </footer>
  );
}
