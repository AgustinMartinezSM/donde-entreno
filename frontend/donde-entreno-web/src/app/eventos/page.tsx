import type { Metadata } from "next";
import Link from "next/link";

import { Header } from "../../components/layout/Header";
import { EventoCard } from "../../components/eventos/EventoCard";
import { SectionHeader } from "../../components/ui/SectionHeader";
import { StatusMessage } from "../../components/ui/StatusMessage";
import { SurfaceCard } from "../../components/ui/SurfaceCard";
import {
  RANGOS_EVENTOS,
  obtenerCalendario,
  type Evento,
  type RangoEventos,
} from "../../services/eventosService";

/*
  El calendario de eventos (Fase 9).

  Server component: el contenido es público, cambia solo con la URL y
  tiene que ser indexable. El rango lo resuelve el BACKEND en zona
  argentina — "este finde" es una pregunta sobre el calendario del
  lugar, no sobre el reloj de quien mira.
*/

export const metadata: Metadata = {
  title: "Eventos deportivos en Mar del Plata",
  description:
    "Torneos, clases abiertas y seminarios con fecha confirmada. Enterate de lo que pasa esta semana y sumate.",
};

type EventosPageProps = {
  searchParams?: Promise<{ [clave: string]: string | string[] | undefined }>;
};

export default async function EventosPage({ searchParams }: EventosPageProps) {
  const parametros = (await searchParams) ?? {};
  const rangoPedido = Array.isArray(parametros.rango)
    ? parametros.rango[0]
    : parametros.rango;

  const rango: RangoEventos = RANGOS_EVENTOS.some(
    (opcion) => opcion.valor === rangoPedido
  )
    ? (rangoPedido as RangoEventos)
    : "proximos";

  let eventos: Evento[] = [];
  let huboError = false;

  try {
    const pagina = await obtenerCalendario({ rango, size: 24 });
    eventos = pagina.contenido;
  } catch {
    huboError = true;
  }

  return (
    <main className="min-h-screen text-[var(--color-text)]">
      <Header />

      <section className="mx-auto w-full max-w-6xl px-4 py-6">
        <SectionHeader
          eyebrow="Agenda"
          title="Lo que se viene"
          description="Torneos, clases abiertas y seminarios con fecha confirmada. Lo organizan los mismos clubes y profes que ya publican en DondeEntreno."
        />

        {/* Los rangos son links: cada uno es una URL compartible. */}
        <nav
          className="mt-5 flex flex-wrap gap-2"
          aria-label="Cuándo querés ir"
        >
          {RANGOS_EVENTOS.map((opcion) => {
            const activo = opcion.valor === rango;

            return (
              <Link
                key={opcion.valor}
                href={`/eventos?rango=${opcion.valor}`}
                aria-current={activo ? "page" : undefined}
                className={`inline-flex min-h-10 items-center rounded-full px-4 text-sm font-bold transition duration-200 ease-out ${
                  activo
                    ? "bg-[var(--color-brand)] text-white"
                    : "border border-[var(--color-border-accent)] bg-[var(--color-surface)] text-[var(--color-primary)] hover:border-[var(--color-primary)]"
                }`}
              >
                {opcion.etiqueta}
              </Link>
            );
          })}
        </nav>

        {huboError ? (
          <StatusMessage variant="warning" className="mt-6">
            No pudimos cargar la agenda. Probá de nuevo en unos minutos.
          </StatusMessage>
        ) : eventos.length === 0 ? (
          <SurfaceCard className="mt-6 p-6">
            <p className="text-sm text-[var(--color-muted)]">
              {rango === "proximos"
                ? "Todavía no hay eventos publicados. Cuando un club o profe organice algo, va a aparecer acá."
                : "No hay eventos en ese rango."}
            </p>

            {rango !== "proximos" ? (
              <Link
                href="/eventos?rango=proximos"
                className="mt-3 inline-flex text-sm font-bold text-[var(--color-primary)] underline underline-offset-4"
              >
                Ver todos los próximos
              </Link>
            ) : (
              <Link
                href="/explorar"
                className="mt-3 inline-flex text-sm font-bold text-[var(--color-primary)] underline underline-offset-4"
              >
                Mientras tanto, mirá las actividades
              </Link>
            )}
          </SurfaceCard>
        ) : (
          <ul className="mt-6 grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {eventos.map((evento) => (
              <li key={evento.id}>
                <EventoCard evento={evento} />
              </li>
            ))}
          </ul>
        )}
      </section>
    </main>
  );
}
