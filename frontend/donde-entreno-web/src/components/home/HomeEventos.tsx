import Link from "next/link";

import { EventoCard } from "../eventos/EventoCard";
import { SectionHeader } from "../ui/SectionHeader";
import { obtenerCalendario, type Evento } from "../../services/eventosService";

/*
  "Lo que se viene" en la home (Fase 9).

  Server component y SIN estado vacío: si no hay eventos esta semana,
  la sección no se dibuja. Una sección que dice "todavía no hay nada"
  en la home ocupa el mismo lugar que una con contenido y no aporta
  nada — la misma regla que ya se aplicó a las solapas del perfil.
*/
export async function HomeEventos() {
  let eventos: Evento[] = [];

  try {
    const pagina = await obtenerCalendario({ rango: "semana", size: 3 });
    eventos = pagina.contenido;
  } catch {
    /* Best-effort: la home carga igual sin la agenda. */
    return null;
  }

  if (eventos.length === 0) {
    return null;
  }

  return (
    <section className="mt-10" aria-labelledby="home-eventos-titulo">
      <SectionHeader
        eyebrow="Esta semana"
        title="Lo que se viene"
        description="Torneos y clases abiertas con fecha confirmada."
        titleId="home-eventos-titulo"
        action={
          <Link
            href="/eventos"
            className="text-sm font-bold text-[var(--color-primary)] underline underline-offset-4"
          >
            Ver la agenda
          </Link>
        }
      />

      <ul className="mt-5 grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {eventos.map((evento) => (
          <li key={evento.id}>
            <EventoCard evento={evento} />
          </li>
        ))}
      </ul>
    </section>
  );
}
