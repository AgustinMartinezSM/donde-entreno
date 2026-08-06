type PublisherIdentityProps = {
  nombre?: string | null;
  tipo?: string | null;
  verificado?: boolean;
};

export function PublisherIdentity({
  nombre,
  tipo,
  verificado = false,
}: PublisherIdentityProps) {
  const nombreVisible = nombre?.trim() || "Comunidad DondeEntreno";
  const iniciales = obtenerIniciales(nombreVisible);

  return (
    <div className="flex min-w-0 items-center gap-3">
      <span
        aria-hidden="true"
        className="flex h-11 w-11 shrink-0 items-center justify-center rounded-full bg-[var(--color-primary)] text-xs font-extrabold tracking-[0.08em] text-white ring-4 ring-[#E8F6FB]"
      >
        {iniciales}
      </span>

      <div className="min-w-0">
        <div className="flex min-w-0 items-center gap-1.5">
          <p className="truncate text-sm font-extrabold text-[var(--color-primary)]">
            {nombreVisible}
          </p>
          {verificado ? (
            <span
              className="inline-flex h-5 w-5 shrink-0 items-center justify-center rounded-full bg-[var(--color-secondary)] text-white"
              aria-label="Publicador verificado"
              title="Publicador verificado"
            >
              <svg
                viewBox="0 0 20 20"
                fill="none"
                stroke="currentColor"
                strokeWidth="2.2"
                strokeLinecap="round"
                strokeLinejoin="round"
                className="h-3 w-3"
                aria-hidden="true"
              >
                <path d="m5.5 10 3 3 6-6" />
              </svg>
            </span>
          ) : null}
        </div>
        <p className="mt-0.5 truncate text-xs font-semibold text-[var(--color-muted)]">
          {tipo ? formatearTipo(tipo) : "Actividad de la comunidad local"}
        </p>
      </div>
    </div>
  );
}

function obtenerIniciales(nombre: string) {
  return nombre
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((parte) => parte.charAt(0).toLocaleUpperCase("es"))
    .join("");
}

function formatearTipo(tipo: string) {
  return tipo
    .trim()
    .toLocaleLowerCase("es")
    .split("_")
    .map((parte) => parte.charAt(0).toLocaleUpperCase("es") + parte.slice(1))
    .join(" ");
}
