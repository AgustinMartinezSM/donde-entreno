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
    /* Solo aparece si hay alguna: con cero seria ruido permanente. */
    ...((metricas.actividadesPausadas ?? 0) > 0
      ? [
          {
            etiqueta: "Actividades pausadas",
            valor: metricas.actividadesPausadas ?? 0,
            href: "/publicador/actividades",
            resaltarSiHay: true,
          },
        ]
      : []),
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
    /*
      Tracking anónimo (Fase 2/3 social). Opcionales: un backend viejo
      no manda los campos y las tarjetas no aparecen.
    */
    ...(metricas.vistas30Dias !== undefined
      ? [
          {
            etiqueta: "Vistas (30 días)",
            valor: metricas.vistas30Dias,
          },
        ]
      : []),
    ...(metricas.contactosWhatsapp30Dias !== undefined
      ? [
          {
            etiqueta: "Contactos por WhatsApp (30 días)",
            valor: metricas.contactosWhatsapp30Dias,
          },
        ]
      : []),
    ...((metricas.quierenProbar ?? 0) > 0
      ? [
          {
            etiqueta: "Personas que quieren probar",
            valor: metricas.quierenProbar ?? 0,
          },
        ]
      : []),
    /*
      Fase 5: los contactos desde el perfil van aparte de los de cada
      actividad. Se muestra recién con el primero — hasta entonces no
      dice nada y el panel ya tiene números suficientes.
    */
    ...((metricas.contactosDesdePerfil30Dias ?? 0) > 0
      ? [
          {
            etiqueta: "Contactos desde tu perfil (30 días)",
            valor: metricas.contactosDesdePerfil30Dias ?? 0,
          },
        ]
      : []),
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
