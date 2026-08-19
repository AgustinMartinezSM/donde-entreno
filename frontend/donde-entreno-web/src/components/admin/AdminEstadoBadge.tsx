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
  PENDIENTE: "border-[#F2C94C] bg-[#FFF8E1] text-[#684A00]",
  EN_REVISION: "border-[#9CCFE4] bg-[#EEF8FC] text-[var(--color-primary)]",
  APROBADA: "border-[#9FDCBC] bg-[var(--color-success-wash)] text-[#176B3F]",
  RECHAZADA: "border-[#F3B6B6] bg-[#FFF1F1] text-[#A53030]",
};

const puntoPorEstado: Record<EstadoSolicitudAdmin, string> = {
  PENDIENTE: "bg-[#D99B00]",
  EN_REVISION: "bg-[var(--color-accent)]",
  APROBADA: "bg-[var(--color-secondary)]",
  RECHAZADA: "bg-red-500",
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
