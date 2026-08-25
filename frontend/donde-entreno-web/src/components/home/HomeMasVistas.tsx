import { SocialActivityCard } from "../social/SocialActivityCard";
import type { Actividad } from "../../types/actividad";

type HomeMasVistasProps = {
  actividades: Actividad[];
};

/*
  "Lo más visto esta semana" (Fase 10).

  Es la ventana SEMANAL, distinta de la fila de deportes populares, que
  mira 30 días: son dos preguntas diferentes —qué se mira siempre y qué
  se está mirando ahora—.

  Si el backend no devuelve nada, esta sección NO EXISTE. El umbral lo
  aplica él (menos de tres actividades con señal = lista vacía), así
  que acá no hay nada que decidir: sin datos, no se dibuja. Un "lo más
  visto" armado con tres clicks enseña a desconfiar de los números del
  sitio, y es más caro que no mostrarlo.
*/
export function HomeMasVistas({ actividades }: HomeMasVistasProps) {
  if (actividades.length === 0) {
    return null;
  }

  return (
    <section className="mt-14 sm:mt-16">
      <h2 className="text-2xl font-extrabold text-[var(--color-primary)] sm:text-3xl">
        Lo más visto esta semana
      </h2>
      <p className="mt-1 text-sm text-[var(--color-muted)]">
        Las actividades que más se están mirando en los últimos días.
      </p>

      <div className="mt-5 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {actividades.map((actividad) => (
          <SocialActivityCard
            key={actividad.id}
            actividad={actividad}
            variante="compacta"
          />
        ))}
      </div>
    </section>
  );
}
