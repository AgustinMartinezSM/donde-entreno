import Link from "next/link";

export function Footer() {
  /*
    Año actual para que el copyright no quede viejo.
    Esto se calcula automáticamente cuando se renderiza el componente.
  */
  const anioActual = new Date().getFullYear();

  return (
    <footer className="mt-auto border-t border-[var(--color-border)] bg-[var(--color-surface)]">
      <div className="mx-auto w-full max-w-6xl px-4 py-10 sm:py-12">
        <div className="grid gap-8 md:grid-cols-[1.4fr_1fr_1fr] md:gap-12">
          {/* Columna de marca */}
          <div>
            <Link href="/" className="inline-block text-2xl font-extrabold tracking-tight">
              <span className="text-[var(--color-primary)]">Donde</span>
              <span className="text-[var(--color-secondary)]">Entreno</span>
            </Link>

            <p className="mt-3 max-w-md text-sm leading-6 text-[var(--color-muted)]">
              Una guía deportiva local para encontrar clubes, profesores,
              gimnasios y actividades cerca tuyo.
            </p>
          </div>

          {/* Links principales */}
          <div>
            <h2 className="text-sm font-extrabold uppercase tracking-[0.16em] text-[var(--color-primary)]">
              Navegación
            </h2>

            <nav className="mt-4 flex flex-col gap-3 text-sm font-bold">
              <Link
                href="/"
                className="text-[var(--color-muted)] transition hover:text-[var(--color-primary)]"
              >
                Inicio
              </Link>

              <Link
                href="/explorar"
                className="text-[var(--color-muted)] transition hover:text-[var(--color-primary)]"
              >
                Explorar
              </Link>

              <Link
                href="/publicar"
                className="text-[var(--color-muted)] transition hover:text-[var(--color-primary)]"
              >
                Publicar
              </Link>
            </nav>
          </div>

          {/* Contacto provisorio */}
          <div>
            <h2 className="text-sm font-extrabold uppercase tracking-[0.16em] text-[var(--color-primary)]">
              Contacto
            </h2>

            <div className="mt-4 flex flex-col gap-3 text-sm font-bold">
              {/* Links falsos/provisorios hasta tener los reales */}
              <a
                href="https://instagram.com/dondeentreno"
                target="_blank"
                rel="noopener noreferrer"
                className="text-[var(--color-muted)] transition hover:text-[var(--color-secondary)]"
              >
                Instagram
              </a>

              <a
                href="mailto:contacto@dondeentreno.com"
                className="text-[var(--color-muted)] transition hover:text-[var(--color-secondary)]"
              >
                Gmail
              </a>

              <a
                href="https://wa.me/5492230000000"
                target="_blank"
                rel="noopener noreferrer"
                className="text-[var(--color-muted)] transition hover:text-[var(--color-secondary)]"
              >
                WhatsApp
              </a>
            </div>
          </div>
        </div>

        {/* Línea inferior */}
        <div className="mt-8 border-t border-[var(--color-border)] pt-5 sm:mt-10">
          <p className="text-xs leading-5 text-[var(--color-muted)]">
            © {anioActual} DondeEntreno. Todos los derechos reservados.
          </p>
        </div>
      </div>
    </footer>
  );
}