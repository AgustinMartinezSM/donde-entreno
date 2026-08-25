import type { Metadata } from "next";
import Image from "next/image";
import Link from "next/link";
import { notFound } from "next/navigation";

import { Header } from "../../../components/layout/Header";
import { BotonMeInteresa } from "../../../components/eventos/BotonMeInteresa";
import { ContactButton } from "../../../components/actividad/ContactButton";
import { BotonReportar } from "../../../components/social/BotonReportar";
import { CompartirButton } from "../../../components/social/CompartirButton";
import { PublisherIdentity } from "../../../components/social/PublisherIdentity";
import { StatusMessage } from "../../../components/ui/StatusMessage";
import { SurfaceCard } from "../../../components/ui/SurfaceCard";
import { construirUrlImagenBackend } from "../../../lib/backendUrl";
import { formatearFechaEvento } from "../../../lib/formatoFecha";
import { construirHrefComoLlegar } from "../../../lib/mapas";
import {
  obtenerEventoPorSlug,
  type Evento,
} from "../../../services/eventosService";

/*
  El detalle de un evento (Fase 9).

  Server component: es la página que se comparte por WhatsApp, así que
  el título y la descripción tienen que viajar en el HTML para que la
  previsualización del link diga algo. La imagen OG generada por
  evento quedó fuera del alcance a propósito.
*/

type EventoPageProps = {
  params: Promise<{ slug: string }>;
};

async function cargarEvento(slug: string): Promise<Evento | null> {
  try {
    return await obtenerEventoPorSlug(slug);
  } catch {
    return null;
  }
}

export async function generateMetadata({
  params,
}: EventoPageProps): Promise<Metadata> {
  const { slug } = await params;
  const evento = await cargarEvento(slug);

  if (!evento) {
    return { title: "Evento no encontrado" };
  }

  const cuando = formatearFechaEvento(evento.iniciaAt);
  const lugar = [evento.sedeNombre, evento.ciudadNombre]
    .filter(Boolean)
    .join(", ");

  return {
    title: `${evento.titulo}${cuando ? ` · ${cuando}` : ""}`,
    description: lugar
      ? `${evento.descripcion.slice(0, 140)} — ${lugar}.`
      : evento.descripcion.slice(0, 160),
  };
}

export default async function EventoPage({ params }: EventoPageProps) {
  const { slug } = await params;
  const evento = await cargarEvento(slug);

  if (!evento) {
    notFound();
  }

  const imagenUrl = construirUrlImagenBackend(evento.imagenUrl);
  const cuando = formatearFechaEvento(evento.iniciaAt);
  const termina = formatearFechaEvento(evento.terminaAt);
  const cancelado = evento.estado === "CANCELADO";

  const hrefPerfil = evento.perfilSlug
    ? `/publicadores/${evento.perfilSlug}`
    : evento.perfilPublicadorId
      ? `/publicadores/${evento.perfilPublicadorId}`
      : undefined;

  const hrefComoLlegar = construirHrefComoLlegar({
    latitud: evento.latitud,
    longitud: evento.longitud,
    direccion: evento.direccion,
    ubicacionNombre: evento.sedeNombre,
    barrioNombre: evento.barrioNombre,
    ciudadNombre: evento.ciudadNombre,
  });

  return (
    <main className="min-h-screen text-[var(--color-text)]">
      <Header />

      <section className="mx-auto w-full max-w-3xl px-4 py-6">
        <Link
          href="/eventos"
          className="text-sm font-bold text-[var(--color-muted)] transition hover:text-[var(--color-primary)]"
        >
          ← Volver a la agenda
        </Link>

        {/*
          El aviso de cancelado va ARRIBA de todo: quien llega por un
          link compartido tiene que enterarse antes de leer nada más.
        */}
        {cancelado ? (
          <StatusMessage variant="warning" role="alert" className="mt-4">
            Este evento fue cancelado por quien lo organizaba.
          </StatusMessage>
        ) : null}

        <SurfaceCard as="article" className="mt-4 overflow-hidden">
          {imagenUrl ? (
            <div className="relative h-56 w-full sm:h-72">
              <Image
                src={imagenUrl}
                alt=""
                fill
                sizes="(max-width: 768px) 100vw, 768px"
                className={`object-cover ${cancelado ? "opacity-60 grayscale" : ""}`}
                priority
              />
            </div>
          ) : null}

          <div className="p-5 sm:p-6">
            <div className="flex flex-wrap items-center gap-2">
              {evento.esGratis ? (
                <span className="rounded-full bg-[var(--color-success-soft)] px-3 py-1 text-xs font-extrabold text-[var(--color-success)]">
                  Gratis
                </span>
              ) : null}
              {evento.deporteSlug && evento.deporteNombre ? (
                <Link
                  href={`/deportes/${evento.deporteSlug}`}
                  className="rounded-full border border-[var(--color-border-accent)] px-3 py-1 text-xs font-bold text-[var(--color-primary)]"
                >
                  {evento.deporteNombre}
                </Link>
              ) : null}
            </div>

            {cuando ? (
              <p className="mt-3 text-sm font-extrabold capitalize text-[var(--color-primary)]">
                {cuando}
                {termina ? ` — termina ${termina}` : ""}
              </p>
            ) : null}

            <h1 className="mt-1 text-2xl font-extrabold leading-8 text-[var(--color-text)]">
              {evento.titulo}
            </h1>

            <div className="mt-4">
              <PublisherIdentity
                nombre={evento.perfilNombre ?? "Publicador"}
                href={hrefPerfil}
                avatarUrl={construirUrlImagenBackend(evento.perfilLogoUrl)}
                tamanio="normal"
              />
            </div>

            <p className="mt-4 whitespace-pre-line text-sm leading-6 text-[var(--color-text)]">
              {evento.descripcion}
            </p>

            <dl className="mt-5 grid grid-cols-1 gap-3 sm:grid-cols-2">
              {evento.sedeNombre || evento.direccion ? (
                <div>
                  <dt className="text-xs font-bold uppercase tracking-wide text-[var(--color-muted)]">
                    Dónde
                  </dt>
                  <dd className="mt-1 text-sm text-[var(--color-text)]">
                    {evento.sedeNombre}
                    {evento.direccion ? ` · ${evento.direccion}` : ""}
                    {evento.barrioNombre ? ` (${evento.barrioNombre})` : ""}
                  </dd>
                </div>
              ) : null}

              {/* El cupo es informativo: no hay reserva en V1. */}
              {evento.cupo ? (
                <div>
                  <dt className="text-xs font-bold uppercase tracking-wide text-[var(--color-muted)]">
                    Cupo
                  </dt>
                  <dd className="mt-1 text-sm text-[var(--color-text)]">
                    {evento.cupo} lugares · se reserva por WhatsApp
                  </dd>
                </div>
              ) : null}

              {!evento.esGratis && evento.precioReferencia ? (
                <div>
                  <dt className="text-xs font-bold uppercase tracking-wide text-[var(--color-muted)]">
                    Precio de referencia
                  </dt>
                  <dd className="mt-1 text-sm text-[var(--color-text)]">
                    ${evento.precioReferencia}
                  </dd>
                </div>
              ) : null}

              {evento.actividadSlug && evento.actividadTitulo ? (
                <div>
                  <dt className="text-xs font-bold uppercase tracking-wide text-[var(--color-muted)]">
                    Es parte de
                  </dt>
                  <dd className="mt-1 text-sm">
                    <Link
                      href={`/actividades/${evento.actividadSlug}`}
                      className="font-bold text-[var(--color-primary)] underline underline-offset-4"
                    >
                      {evento.actividadTitulo}
                    </Link>
                  </dd>
                </div>
              ) : null}
            </dl>

            {!cancelado ? (
              <div className="mt-6 flex flex-wrap items-center gap-3">
                <BotonMeInteresa
                  eventoId={evento.id}
                  interesaInicial={Boolean(evento.meInteresa)}
                  cantidadInicial={evento.cantidadInteresados ?? 0}
                />

                {hrefComoLlegar ? (
                  <a
                    href={hrefComoLlegar}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="inline-flex min-h-11 items-center rounded-[14px] border border-[var(--color-border-accent)] bg-[var(--color-surface)] px-4 text-sm font-bold text-[var(--color-primary)] transition hover:border-[var(--color-primary)]"
                  >
                    Cómo llegar
                  </a>
                ) : null}

                <CompartirButton
                  ruta={`/eventos/${evento.slug}`}
                  titulo={evento.titulo}
                />
              </div>
            ) : null}

            {!cancelado && evento.whatsappContacto ? (
              <ContactButton
                whatsapp={evento.whatsappContacto}
                tituloActividad={evento.titulo}
                perfilPublicadorId={evento.perfilPublicadorId ?? undefined}
                nombrePublicador={evento.perfilNombre ?? undefined}
              />
            ) : null}

            <div className="mt-6 border-t border-[var(--color-border-soft)] pt-4">
              <BotonReportar
                tipoObjeto="EVENTO"
                objetoId={evento.id}
                etiquetaObjeto="este evento"
                compacto
              />
            </div>
          </div>
        </SurfaceCard>
      </section>
    </main>
  );
}
