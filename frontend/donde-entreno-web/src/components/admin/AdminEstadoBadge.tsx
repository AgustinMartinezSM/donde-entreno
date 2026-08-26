import type { EstadoSolicitudAdmin } from "../../types/adminSolicitudes";

type AdminEstadoBadgeProps = {
  estado: EstadoSolicitudAdmin;
  size?: "sm" | "md";
};

const etiquetasPorEstado: Record<EstadoSolicitudAdmin, string> = {
  PENDIENTE: "Pendiente",
  EN_REVISION: "En revisión",
  APROBADA: "Aprobada",
  RECHAZADA: "Rechazada",
};

const clasesPorEstado: Record<EstadoSolicitudAdmin, string> = {
  PENDIENTE: "border-[var(--color-warning-border)] bg-[var(--color-warning-surface)] text-[var(--color-warning)]",
  EN_REVISION: "border-[var(--color-border-accent)] bg-[var(--color-info-soft)] text-[var(--color-primary)]",
  APROBADA: "border-[var(--color-success-border)] bg-[var(--color-success-wash)] text-[var(--color-success)]",
  RECHAZADA: "border-[var(--color-danger-border)] bg-[var(--color-danger-surface)] text-[var(--color-danger)]",
};

const puntoPorEstado: Record<EstadoSolicitudAdmin, string> = {
  PENDIENTE: "bg-[var(--color-warning)]",
  EN_REVISION: "bg-[var(--color-accent)]",
  APROBADA: "bg-[var(--color-secondary)]",
  RECHAZADA: "bg-[var(--color-danger-surface)]0",
};

const clasesPorTamanio: Record<
  NonNullable<AdminEstadoBadgeProps["size"]>,
  string
> = {
  sm: "gap-1.5 px-3 py-1.5 text-xs",
  md: "gap-2 px-4 py-2 text-sm",
};

export function AdminEstadoBadge({
  estado,
  size = "md",
}: AdminEstadoBadgeProps) {
  return (
    <span
      className={`inline-flex w-fit shrink-0 items-center whitespace-nowrap rounded-full border font-extrabold shadow-sm ${clasesPorTamanio[size]} ${clasesPorEstado[estado]}`}
    >
      <span className={`h-2 w-2 rounded-full ${puntoPorEstado[estado]}`} />
      {etiquetasPorEstado[estado]}
    </span>
  );
}
