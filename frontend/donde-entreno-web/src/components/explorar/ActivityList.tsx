import type { Actividad } from "../../types/actividad";
import { AppLinkButton } from "../ui/AppLinkButton";
import { SectionHeader } from "../ui/SectionHeader";
import { StatusMessage } from "../ui/StatusMessage";
import { SurfaceCard } from "../ui/SurfaceCard";
import { ActivityCard } from "./ActivityCard";

type ActivityListProps = {
  actividades: Actividad[];

  /*
    Título y descripción opcionales.
    Si no los mandamos, usa textos pensados para la home.
  */
  titulo?: string;
  descripcion?: string;
};

export function ActivityList({
  actividades,
  titulo = "Actividades destacadas",
  descripcion = "Primeras actividades disponibles para empezar a moverte.",
}: ActivityListProps) {
  if (actividades.length === 0) {
    return (
      <StatusMessage
        variant="info"
        title="Todavía no encontramos actividades para mostrar"
        className="mt-10 p-7 text-center"
      >
        <p className="mx-auto max-w-xl">
          Podés explorar el listado completo o cambiar de ciudad para descubrir
          otras opciones.
        </p>
        <div className="mt-5 flex flex-col gap-3 sm:flex-row sm:justify-center">
          <AppLinkButton href="/explorar" size="sm">
            Explorar actividades
          </AppLinkButton>
          <AppLinkButton href="/ciudades" variant="secondary" size="sm">
            Ver ciudades
          </AppLinkButton>
        </div>
      </StatusMessage>
    );
  }

  return (
    <SurfaceCard
      as="section"
      variant="soft"
      className="mt-10 p-4 sm:mt-12 sm:p-6"
    >
      <SectionHeader
        eyebrow="Actividades"
        title={titulo}
        description={descripcion}
        className="mb-6"
      />

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {actividades.map((actividad) => (
          <ActivityCard key={actividad.id} actividad={actividad} />
        ))}
      </div>
    </SurfaceCard>
  );
}
