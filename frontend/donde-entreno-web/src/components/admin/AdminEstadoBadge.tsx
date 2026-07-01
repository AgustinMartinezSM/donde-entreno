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
  PENDIENTE: "border-[#F7D87A] bg-[#FFF8E1] text-[#7A5A00]",
  EN_REVISION: "border-[#A9D8EA] bg-[#EEF8FC] text-[var(--color-primary)]",
  APROBADA: "border-[#BDE8D0] bg-[#ECF9F2] text-[#1D7B4A]",
  RECHAZADA: "border-red-200 bg-red-50 text-red-700",
};

const clasesPorTamanio: Record<
  NonNullable<AdminEstadoBadgeProps["size"]>,
  string
> = {
  sm: "px-3 py-1 text-xs",
  md: "px-4 py-1.5 text-sm",
};

export function AdminEstadoBadge({
  estado,
  size = "md",
}: AdminEstadoBadgeProps) {
  return (
    <span
      className={`inline-flex w-fit items-center whitespace-nowrap rounded-full border font-bold ${clasesPorTamanio[size]} ${clasesPorEstado[estado]}`}
    >
      {etiquetasPorEstado[estado]}
    </span>
  );
}
