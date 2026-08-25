import type { Metadata } from "next";
import Link from "next/link";
import { notFound } from "next/navigation";

import { Header } from "../../../components/layout/Header";
import { SocialActivityCard } from "../../../components/social/SocialActivityCard";
import { AppLinkButton } from "../../../components/ui/AppLinkButton";
import { SectionHeader } from "../../../components/ui/SectionHeader";
import { SurfaceCard } from "../../../components/ui/SurfaceCard";
import { DEFAULT_CITY_SLUG } from "../../../lib/ciudadActiva";
import { GUIAS, obtenerGuia } from "../../../lib/guias";
import { buscarActividades } from "../../../services/actividadService";
import type { Actividad } from "../../../types/actividad";

export const dynamic = "force-dynamic";

/*
  La fecha se arma con los números sueltos y NO con new Date(iso).

  `new Date("2026-08-25")` se parsea como medianoche UTC, que en
  Argentina es el 24 a las 21: la guía decía estar revisada un día
  antes de lo que dice el dato. Misma familia que el ISO con offset del
  formulario de eventos.
*/
const MESES = [
  "enero", "febrero", "marzo", "abril", "mayo", "junio",
  "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre",
];

function formatearFechaLocal(iso: string) {
  const [anio, mes, dia] = iso.split("-").map(Number);

  if (!anio || !mes || !dia) {
    return iso;
  }

  return `${dia} de ${MESES[mes - 1]} de ${anio}`;
}

type Props = { params: Promise<{ slug: string }> };

export async function generateMetadata({ params }: Props): Promise<Metadata> {
  const { slug } = await params;
  const guia = obtenerGuia(slug);

  if (!guia) {
    return { title: "Guía no encontrada" };
  }

  return {
    title: guia.titulo,
    description: guia.resumen,
    alternates: { canonical: `/guias/${guia.slug}` },
    openGraph: {
      title: `${guia.titulo} - DondeEntreno`,
      description: guia.resumen,
      type: "article",
    },
  };
}

/*
  Detalle de guía (Fase 10).

  El texto es editorial y sale de lib/guias.ts; las ACTIVIDADES salen
  del catálogo real. Esa separación es la que hace que la guía envejezca
  bien: si mañana abre un club nuevo de karate, aparece acá sin tocar
  una línea del texto.
*/
export default async function GuiaPage({ params }: Props) {
  const { slug } = await params;
  const guia = obtenerGuia(slug);

  if (!guia) {
    notFound();
  }

  let actividades: Actividad[] = [];
  let totalActividades = 0;

  try {
    const respuesta = await buscarActividades({
      deporteSlug: guia.deporteSlug,
      ciudadSlug: DEFAULT_CITY_SLUG,
      page: 0,
      size: 6,
    });

    actividades = respuesta.contenido;
    totalActividades = respuesta.totalElementos;
  } catch (error) {
    /* Best-effort: la guía se lee igual sin el catálogo. */
    console.error("Error al cargar actividades de la guía:", error);
  }

  const otras = GUIAS.filter((otra) => otra.slug !== guia.slug);

  return (
    <main className="min-h-screen text-[var(--color-text)]">
      <section className="mx-auto w-full max-w-3xl px-4 py-6">
        <Header />

        <nav aria-label="Migas" className="mt-6 text-sm text-[var(--color-muted)]">
          <Link
            href="/guias"
            className="underline-offset-4 hover:text-[var(--color-primary)] hover:underline"
          >
            Guías
          </Link>{" "}
          / {guia.titulo}
        </nav>

        <div className="mt-3">
          <SectionHeader
            eyebrow="Guía"
            title={guia.titulo}
            description={guia.resumen}
          />
        </div>

        <SurfaceCard as="section" variant="info" className="mt-6 p-5">
          <h2 className="text-xs font-extrabold uppercase tracking-[0.16em] text-[var(--color-secondary)]">
            De un vistazo
          </h2>

          <ul className="mt-3 grid gap-2">
            {guia.deUnVistazo.map((punto) => (
              <li
                key={punto}
                className="flex gap-2 text-sm leading-6 text-[var(--color-text)]"
              >
                <span aria-hidden="true" className="text-[var(--color-secondary)]">
                  •
                </span>
                {punto}
              </li>
            ))}
          </ul>
        </SurfaceCard>

        <article className="mt-8 grid gap-8">
          {guia.secciones.map((seccion) => (
            <section key={seccion.titulo}>
              <h2 className="text-xl font-extrabold text-[var(--color-primary)]">
                {seccion.titulo}
              </h2>

              <div className="mt-2 grid gap-3">
                {seccion.parrafos.map((parrafo) => (
                  <p
                    key={parrafo.slice(0, 40)}
                    className="text-base leading-7 text-[var(--color-text)]"
                  >
                    {parrafo}
                  </p>
                ))}
              </div>
            </section>
          ))}
        </article>

        <SurfaceCard as="section" className="mt-8 p-5 sm:p-6">
          <h2 className="text-xl font-extrabold text-[var(--color-primary)]">
            Qué preguntar antes de anotarte
          </h2>
          <p className="mt-1 text-sm text-[var(--color-muted)]">
            Ninguna de estas preguntas es molesta: cualquier club o profe está
            acostumbrado a responderlas.
          </p>

          <ul className="mt-4 grid gap-2">
            {guia.quePreguntar.map((pregunta) => (
              <li
                key={pregunta}
                className="rounded-[var(--radius-md)] border border-[var(--color-border-soft)] px-3 py-2 text-sm leading-6"
              >
                {pregunta}
              </li>
            ))}
          </ul>
        </SurfaceCard>

        {/*
          El catálogo real. Si no hay nada publicado de este deporte, la
          sección no promete lo que no existe: ofrece explorar.
        */}
        <section className="mt-10">
          <div className="flex flex-wrap items-end justify-between gap-3">
            <h2 className="text-xl font-extrabold text-[var(--color-primary)]">
              {actividades.length > 0
                ? `Dónde practicar ${guia.titulo.replace("Empezar ", "")}`
                : "Dónde practicar"}
            </h2>

            {totalActividades > actividades.length ? (
              <AppLinkButton
                href={`/explorar?deporteSlug=${guia.deporteSlug}&page=0`}
                variant="outline"
              >
                Ver las {totalActividades}
              </AppLinkButton>
            ) : null}
          </div>

          {actividades.length === 0 ? (
            <SurfaceCard className="mt-3 p-5">
              <p className="text-sm leading-6 text-[var(--color-muted)]">
                Todavía no hay actividades de este deporte publicadas en tu
                ciudad. Podés{" "}
                <Link
                  href="/explorar"
                  className="font-semibold text-[var(--color-primary)] underline-offset-4 hover:underline"
                >
                  explorar otras actividades
                </Link>{" "}
                mientras tanto.
              </p>
            </SurfaceCard>
          ) : (
            <div className="mt-3 grid gap-4 sm:grid-cols-2">
              {actividades.map((actividad) => (
                <SocialActivityCard
                  key={actividad.id}
                  actividad={actividad}
                  variante="compacta"
                />
              ))}
            </div>
          )}
        </section>

        <p className="mt-10 text-xs text-[var(--color-muted)]">
          Guía revisada el{" "}
          {formatearFechaLocal(guia.actualizada)}
          . Es información general para orientarte: cada escuela tiene su forma
          de trabajar, y lo que diga el club o el profe manda sobre esto.
        </p>

        {otras.length > 0 ? (
          <section className="mt-8">
            <h2 className="text-sm font-extrabold uppercase tracking-[0.16em] text-[var(--color-secondary)]">
              Otras guías
            </h2>

            <ul className="mt-3 grid gap-2">
              {otras.map((otra) => (
                <li key={otra.slug}>
                  <Link
                    href={`/guias/${otra.slug}`}
                    className="text-sm font-semibold text-[var(--color-primary)] underline-offset-4 hover:underline"
                  >
                    {otra.titulo}
                  </Link>
                </li>
              ))}
            </ul>
          </section>
        ) : null}
      </section>
    </main>
  );
}
