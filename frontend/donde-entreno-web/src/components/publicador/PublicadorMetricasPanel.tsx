import Link from "next/link";
import type { MetricasPublicador } from "../../types/publicador";
import { SectionHeader } from "../ui/SectionHeader";
import { SurfaceCard } from "../ui/SurfaceCard";

type MetricaItem = {
  etiqueta: string;
  valor: number;
  /*
    Sin href la tarjeta es informativa (no hay una sección a la que ir),
    como el contador de seguidores.
  */
  href?: string;
  /*
    Cuando es una métrica "de acción" (algo que espera al publicador o
    al equipo), la resaltamos solo si hay pendientes.
  */
  resaltarSiHay?: boolean;
};

/*
  Panel de resumen del publicador: conteos de un vistazo con acceso
  directo a cada sección. Es presentacional; el dashboard resuelve la
  carga y le pasa las métricas ya validadas.
*/
export function PublicadorMetricasPanel({
  metricas,
}: {
  metricas: MetricasPublicador;
}) {
  const items: MetricaItem[] = [
    {
      etiqueta: "Actividades publicadas",
      valor: metricas.actividadesPublicadas,
      href: "/publicador/actividades",
    },
    {
      etiqueta: "Solicitudes pendientes",
      valor: metricas.solicitudesPublicacionPendientes,
      href: "/publicador/solicitudes",
      resaltarSiHay: true,
    },
    {
      etiqueta: "Cambios en revisión",
      valor: metricas.solicitudesCambioPendientes,
      href: "/publicador/solicitudes-cambio",
      resaltarSiHay: true,
    },
    {
      etiqueta: "Imágenes en moderación",
      valor: metricas.imagenesPendientesModeracion,
      href: "/publicador/actividades",
      resaltarSiHay: true,
    },
    {
      etiqueta: "Seguidores",
      valor: metricas.seguidores,
    },
  ];

  return (
    <SurfaceCard className="mt-6 p-6 sm:p-8">
      <SectionHeader
        eyebrow="Resumen"
        title="Tu actividad de un vistazo"
        description="Un pantallazo de tus publicaciones, tus seguidores y lo que está esperando revisión."
      />

      <div
        className={`mt-6 grid gap-4 sm:grid-cols-2 ${
          items.length >= 5 ? "lg:grid-cols-5" : "lg:grid-cols-4"
        }`}
      >
        {items.map((item) => (
          <MetricaTile key={item.etiqueta} item={item} />
        ))}
      </div>
    </SurfaceCard>
  );
}

function MetricaTile({ item }: { item: MetricaItem }) {
  const pendiente = Boolean(item.resaltarSiHay) && item.valor > 0;
  const claseCaja = `group flex flex-col justify-between rounded-[20px] border p-5 shadow-[0_10px_24px_rgba(12,52,80,0.05)] transition duration-200 ease-out ${
    pendiente ? "border-[#F4CE9A] bg-[#FDF6EC]" : "border-[var(--color-border-soft)] bg-white/80"
  }`;

  const contenido = (
    <>
      <span
        className={`text-3xl font-extrabold tabular-nums ${
          pendiente ? "text-[#B5730E]" : "text-[var(--color-primary)]"
        }`}
      >
        {item.valor}
      </span>
      <span className="mt-2 text-sm font-bold leading-5 text-[var(--color-muted)] group-hover:text-[var(--color-primary)]">
        {item.etiqueta}
      </span>
    </>
  );

  if (!item.href) {
    return <div className={claseCaja}>{contenido}</div>;
  }

  return (
    <Link href={item.href} className={`${claseCaja} hover:-translate-y-0.5`}>
      {contenido}
    </Link>
  );
}
