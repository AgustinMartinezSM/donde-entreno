import { formatearTipoPublicador } from "../../lib/formatoCatalogo";

type PublisherIdentityProps = {
  nombre?: string | null;
  tipo?: string | null;
  verificado?: boolean;
  /*
    - "normal": header de card de feed y detalle de actividad.
    - "compacta": listados densos (grillas de explorar, filas del feed).
  */
  tamanio?: "normal" | "compacta";
};

export function PublisherIdentity({
  nombre,
  tipo,
  verificado = false,
  tamanio = "normal",
}: PublisherIdentityProps) {
  const nombreVisible = nombre?.trim() || "Comunidad DondeEntreno";
  const iniciales = obtenerIniciales(nombreVisible);
  const compacta = tamanio === "compacta";

  return (
    <div className={`flex min-w-0 items-center ${compacta ? "gap-2" : "gap-3"}`}>
      <span
        aria-hidden="true"
        className={`flex shrink-0 items-center justify-center rounded-full bg-[var(--color-primary)] font-extrabold tracking-[0.08em] text-white ${
          compacta
            ? "h-8 w-8 text-[10px] ring-2 ring-[#E8F6FB]"
            : "h-11 w-11 text-xs ring-4 ring-[#E8F6FB]"
        }`}
      >
        {iniciales}
      </span>

      <div className="min-w-0">
        <div className="flex min-w-0 items-center gap-1.5">
          <p
            className={`truncate font-extrabold text-[var(--color-primary)] ${
              compacta ? "text-xs" : "text-sm"
            }`}
          >
            {nombreVisible}
          </p>
          {verificado ? (
            <span
              role="img"
              aria-label="Publicador verificado"
              title="Publicador verificado"
              className={`inline-flex shrink-0 items-center justify-center rounded-full bg-[var(--color-secondary)] text-white ${
                compacta ? "h-4 w-4" : "h-5 w-5"
              }`}
            >
              <svg
                viewBox="0 0 20 20"
                fill="none"
                stroke="currentColor"
                strokeWidth="2.2"
                strokeLinecap="round"
                strokeLinejoin="round"
                className={compacta ? "h-2.5 w-2.5" : "h-3 w-3"}
                aria-hidden="true"
              >
                <path d="m5.5 10 3 3 6-6" />
              </svg>
            </span>
          ) : null}
        </div>
        {!compacta ? (
          <p className="mt-0.5 truncate text-xs font-semibold text-[var(--color-muted)]">
            {tipo ? formatearTipoPublicador(tipo) : "Actividad de la comunidad local"}
          </p>
        ) : null}
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
