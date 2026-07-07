import type { EstadoSolicitudPublicacion } from "../../types/solicitudPublicacion";

type PublicadorSolicitudEstadoBadgeProps = {
  estado: EstadoSolicitudPublicacion;
  size?: "sm" | "md";
};

const etiquetasPorEstado: Record<EstadoSolicitudPublicacion, string> = {
  PENDIENTE: "Pendiente",
  EN_REVISION: "En revisión",
  APROBADA: "Aprobada",
  RECHAZADA: "Rechazada",
};

const clasesPorEstado: Record<EstadoSolicitudPublicacion, string> = {
  PENDIENTE: "border-[#F2C94C] bg-[#FFF8E1] text-[#684A00]",
  EN_REVISION: "border-[#8CCCE6] bg-[#E8F6FB] text-[#0F3D5E]",
  APROBADA: "border-[#7FD4A7] bg-[#E6F7EF] text-[#167A4A]",
  RECHAZADA: "border-[#F3B6B6] bg-[#FFF1F1] text-[#A53030]",
};

const puntoPorEstado: Record<EstadoSolicitudPublicacion, string> = {
  PENDIENTE: "bg-[#D99B00]",
  EN_REVISION: "bg-[#4FB3D9]",
  APROBADA: "bg-[#2EB872]",
  RECHAZADA: "bg-red-500",
};

const clasesPorTamanio: Record<
  NonNullable<PublicadorSolicitudEstadoBadgeProps["size"]>,
  string
> = {
  sm: "gap-1.5 px-3 py-1.5 text-xs",
  md: "gap-2 px-4 py-2 text-sm",
};

export function PublicadorSolicitudEstadoBadge({
  estado,
  size = "md",
}: PublicadorSolicitudEstadoBadgeProps) {
  return (
    <span
      className={`inline-flex w-fit shrink-0 items-center whitespace-nowrap rounded-full border font-extrabold shadow-sm ${clasesPorTamanio[size]} ${clasesPorEstado[estado]}`}
    >
      <span className={`h-2 w-2 rounded-full ${puntoPorEstado[estado]}`} />
      {formatearEstadoSolicitudPublicador(estado)}
    </span>
  );
}

export function formatearEstadoSolicitudPublicador(
  estado: EstadoSolicitudPublicacion
): string {
  return etiquetasPorEstado[estado];
}
