type PublicadorActividadEstadoBadgeProps = {
  estado: string;
  activa?: boolean | null;
  size?: "sm" | "md";
};

const clasesPorTamanio: Record<
  NonNullable<PublicadorActividadEstadoBadgeProps["size"]>,
  string
> = {
  sm: "gap-1.5 px-3 py-1.5 text-xs",
  md: "gap-2 px-4 py-2 text-sm",
};

export function PublicadorActividadEstadoBadge({
  estado,
  activa,
  size = "md",
}: PublicadorActividadEstadoBadgeProps) {
  const estadoNormalizado = estado.trim().toUpperCase();
  const clasesEstado = obtenerClasesEstado(estadoNormalizado, activa);
  const clasesPunto = obtenerClasesPunto(estadoNormalizado, activa);

  return (
    <span
      className={`inline-flex w-fit shrink-0 items-center whitespace-nowrap rounded-full border font-extrabold shadow-sm ${clasesPorTamanio[size]} ${clasesEstado}`}
    >
      <span className={`h-2 w-2 rounded-full ${clasesPunto}`} />
      {activa === false ? "Inactiva" : formatearEstadoActividad(estado)}
    </span>
  );
}

export function formatearEstadoActividad(estado: string): string {
  const estadoNormalizado = estado.trim().toUpperCase();

  if (estadoNormalizado === "PUBLICADA") {
    return "Publicada";
  }

  if (estadoNormalizado === "INACTIVA") {
    return "Inactiva";
  }

  if (!estadoNormalizado) {
    return "Estado no informado";
  }

  return estadoNormalizado
    .toLowerCase()
    .split("_")
    .map((parte) => parte.charAt(0).toUpperCase() + parte.slice(1))
    .join(" ");
}

function obtenerClasesEstado(estado: string, activa?: boolean | null): string {
  if (activa === false) {
    return "border-[var(--color-border-accent)] bg-[var(--color-surface-soft)] text-[var(--color-muted)]";
  }

  if (estado === "PUBLICADA") {
    return "border-[var(--color-success-border)] bg-[var(--color-success-surface)] text-[var(--color-success)]";
  }

  if (estado === "INACTIVA") {
    return "border-[var(--color-border-accent)] bg-[var(--color-surface-soft)] text-[var(--color-muted)]";
  }

  /* Pausa voluntaria (fase 6): tono de aviso, no de error. */
  if (estado === "PAUSADA") {
    return "border-[var(--color-warning-border)] bg-[var(--color-warning-surface)] text-[var(--color-warning)]";
  }

  return "border-[#8CCCE6] bg-[var(--color-info-soft)] text-[var(--color-primary)]";
}

function obtenerClasesPunto(estado: string, activa?: boolean | null): string {
  if (activa === false || estado === "INACTIVA") {
    return "bg-[var(--color-muted)]";
  }

  if (estado === "PUBLICADA") {
    return "bg-[var(--color-secondary)]";
  }

  if (estado === "PAUSADA") {
    return "bg-[var(--color-warning)]";
  }

  return "bg-[var(--color-accent)]";
}
