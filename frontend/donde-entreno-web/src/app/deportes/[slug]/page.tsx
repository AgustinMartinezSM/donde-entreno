import type { Metadata } from "next";
import Image from "next/image";
import { notFound } from "next/navigation";

import type { Actividad } from "../../../types/actividad";
import type { Deporte } from "../../../types/deporte";
import { Header } from "../../../components/layout/Header";
import { SocialActivityCard } from "../../../components/social/SocialActivityCard";
import { ErrorState } from "../../../components/feedback/ErrorState";
import { AppLinkButton } from "../../../components/ui/AppLinkButton";
import { SectionHeader } from "../../../components/ui/SectionHeader";
import { StatusMessage } from "../../../components/ui/StatusMessage";
import { SurfaceCard } from "../../../components/ui/SurfaceCard";
import { obtenerImagenFallbackActividad } from "../../../lib/activityImages";
import { DEFAULT_CITY_SLUG } from "../../../lib/ciudadActiva";
import { buscarActividades } from "../../../services/actividadService";
import { obtenerCiudadPorSlug } from "../../../services/ciudadService";
import { obtenerDeportes } from "../../../services/deportesService";

/*
  Landing pública por deporte (SEO local):
  /deportes/jiu-jitsu, /deportes/yoga, etc.

  Cada deporte del catálogo tiene una página indexable con sus
  actividades reales en la ciudad por defecto, deportes relacionados
  de la misma categoría y CTA para publicadores.
*/

type DeporteLandingProps = {
  params: Promise<{
    slug: string;
  }>;
};

export async function generateMetadata({
  params,
}: DeporteLandingProps): Promise<Metadata> {
  const { slug } = await params;

  try {
    const deporte = await buscarDeportePorSlug(slug);

    if (!deporte) {
      return {
        title: "Deporte no encontrado",
      };
    }

    const descripcion =
      deporte.descripcion ||
      `Encontrá clubes, profes y clases de ${deporte.nombre} cerca tuyo con horarios, precios y contacto directo.`;

    return {
      title: `${deporte.nombre}: clubes, profes y clases`,
      description: descripcion,
      alternates: {
        canonical: `/deportes/${deporte.slug}`,
      },
      openGraph: {
        title: `${deporte.nombre} en DondeEntreno`,
        description: descripcion,
        type: "website",
      },
    };
  } catch (error) {
    console.error("Error al generar metadata del deporte:", error);

    return {
      title: "Deportes",
      description:
        "Encontrá deportes, clubes, profes y gimnasios en tu ciudad con DondeEntreno.",
    };
  }
}

export default async function DeporteLandingPage({ params }: DeporteLandingProps) {
  const { slug } = await params;

  let deporte: Deporte | null = null;
  let deportes: Deporte[] = [];
  let huboErrorCatalogo = false;

  try {
    deportes = await obtenerDeportes();
    deporte = deportes.find((item) => item.slug === slug) ?? null;
  } catch (error) {
    huboErrorCatalogo = true;
    console.error("Error al cargar el catálogo de deportes:", error);
  }

  if (huboErrorCatalogo) {
    return (
      <main className="min-h-screen text-[var(--color-text)]">
        <section className="mx-auto w-full max-w-6xl px-4 py-6">
          <Header />
          <div className="py-10">
            <ErrorState
              titulo="No pudimos cargar este deporte"
              descripcion="No pudimos cargar esta información. Probá nuevamente en unos segundos o volvé al inicio."
              mostrarBotonInicio
              mostrarBotonExplorar
            />
          </div>
        </section>
      </main>
    );
  }

  if (!deporte) {
    notFound();
  }

  let ciudadNombre = "Mar del Plata";
  try {
    const ciudad = await obtenerCiudadPorSlug(DEFAULT_CITY_SLUG);
    ciudadNombre = ciudad.nombre;
  } catch {
    ciudadNombre = "Mar del Plata";
  }

  let actividades: Actividad[] = [];
  let totalActividades = 0;
  let huboErrorActividades = false;

  try {
    const respuesta = await buscarActividades({
      deporteSlug: deporte.slug,
      ciudadSlug: DEFAULT_CITY_SLUG,
      page: 0,
      size: 6,
    });

    actividades = respuesta.contenido;
    totalActividades = respuesta.totalElementos;
  } catch (error) {
    huboErrorActividades = true;
    console.error("Error al cargar actividades del deporte:", error);
  }

  const relacionados = deporte.categoriaSlug
    ? deportes
        .filter(
          (item) =>
            item.categoriaSlug === deporte.categoriaSlug &&
            item.slug !== deporte.slug
        )
        .slice(0, 6)
    : [];

  const imagenDeporte = obtenerImagenFallbackActividad({
    deporteSlug: deporte.slug,
  });
  const hrefExplorar = `/explorar?deporteSlug=${encodeURIComponent(
    deporte.slug
  )}&ciudadSlug=${encodeURIComponent(DEFAULT_CITY_SLUG)}`;

  return (
    <main className="min-h-screen text-[var(--color-text)]">
      <section className="mx-auto w-full max-w-6xl px-4 py-6">
        <Header />

        <div className="py-8 sm:py-10">
          <SurfaceCard as="section" className="overflow-hidden">
            <div className="relative h-52 sm:h-64">
              <Image
                src={imagenDeporte}
                alt=""
                fill
                priority
                sizes="(max-width: 1024px) 100vw, 1100px"
                className="object-cover"
              />
              <div className="absolute inset-0 bg-gradient-to-t from-[#0F3D5E]/80 via-[#0F3D5E]/35 to-transparent" />

              <div className="absolute inset-x-0 bottom-0 p-5 sm:p-7">
                {deporte.categoriaNombre ? (
                  <span className="rounded-full bg-white/95 px-3 py-1 text-xs font-bold text-[var(--color-primary)] shadow-sm">
                    {deporte.categoriaNombre}
                  </span>
                ) : null}
                <h1 className="mt-3 text-3xl font-extrabold leading-tight text-white sm:text-5xl">
                  {deporte.nombre}{" "}
                  <span className="text-[#7FDCA8]">en {ciudadNombre}</span>
                </h1>
              </div>
            </div>

            <div className="p-5 sm:p-7">
              <p className="max-w-3xl text-base leading-7 text-[var(--color-muted)]">
                {deporte.descripcion ||
                  `Clubes, profes y espacios para practicar ${deporte.nombre} cerca tuyo, con horarios, precios de referencia y contacto directo.`}
              </p>

              <div className="mt-5 flex flex-wrap gap-3">
                <AppLinkButton href={hrefExplorar}>
                  Ver todas las actividades
                </AppLinkButton>
                <AppLinkButton href="/deportes" variant="secondary">
                  Ver todos los deportes
                </AppLinkButton>
              </div>
            </div>
          </SurfaceCard>

          <SurfaceCard as="section" variant="soft" className="mt-8 p-4 sm:p-6">
            <SectionHeader
              eyebrow="ACTIVIDADES"
              title={`Dónde practicar ${deporte.nombre}`}
              description={
                totalActividades > 0
                  ? `${totalActividades} ${
                      totalActividades === 1
                        ? "actividad disponible"
                        : "actividades disponibles"
                    } en ${ciudadNombre}.`
                  : `Opciones disponibles en ${ciudadNombre}.`
              }
              className="mb-6"
            />

            {huboErrorActividades ? (
              <StatusMessage variant="warning" title="No pudimos cargar las actividades">
                <p>
                  Probá nuevamente en unos segundos o abrí Explorar para buscar
                  manualmente.
                </p>
              </StatusMessage>
            ) : actividades.length === 0 ? (
              <StatusMessage
                variant="info"
                title={`Todavía no hay actividades de ${deporte.nombre} publicadas en ${ciudadNombre}`}
                className="p-7 text-center"
              >
                <p className="mx-auto max-w-xl">
                  ¿Das clases o tenés un espacio de {deporte.nombre}? Publicá tu
                  actividad y aparecé acá.
                </p>
                <div className="mt-5 flex justify-center">
                  <AppLinkButton href="/publicar" variant="success">
                    Publicar mi actividad
                  </AppLinkButton>
                </div>
              </StatusMessage>
            ) : (
              <>
                <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
                  {actividades.map((actividad) => (
                    <SocialActivityCard
                      key={actividad.id}
                      actividad={actividad}
                      variante="compacta"
                    />
                  ))}
                </div>

                {totalActividades > actividades.length ? (
                  <div className="mt-6 flex justify-center">
                    <AppLinkButton href={hrefExplorar} variant="secondary">
                      Ver las {totalActividades} actividades
                    </AppLinkButton>
                  </div>
                ) : null}
              </>
            )}
          </SurfaceCard>

          {relacionados.length > 0 ? (
            <SurfaceCard as="section" className="mt-8 p-5 sm:p-6">
              <SectionHeader
                eyebrow={deporte.categoriaNombre || "Relacionados"}
                title="Deportes relacionados"
                description="Otras disciplinas de la misma categoría."
                className="mb-5"
              />
              <div className="flex flex-wrap gap-2">
                {relacionados.map((relacionado) => (
                  <AppLinkButton
                    key={relacionado.slug}
                    href={`/deportes/${encodeURIComponent(relacionado.slug)}`}
                    variant="secondary"
                    size="sm"
                    className="rounded-full"
                  >
                    {relacionado.nombre}
                  </AppLinkButton>
                ))}
              </div>
            </SurfaceCard>
          ) : null}

          <SurfaceCard
            as="section"
            className="mt-8 border-[#BDE8D0] bg-gradient-to-br from-white via-[#F8FCFE] to-[#E6F7EF] p-6 text-center sm:p-8"
          >
            <h2 className="text-2xl font-extrabold text-[var(--color-primary)]">
              ¿Enseñás {deporte.nombre} o tenés un espacio?
            </h2>
            <p className="mx-auto mt-2 max-w-xl text-sm leading-6 text-[var(--color-muted)] sm:text-base">
              Publicá tu actividad gratis: el equipo la revisa y aparece para
              todas las personas que buscan {deporte.nombre} en tu ciudad.
            </p>
            <div className="mt-5 flex justify-center">
              <AppLinkButton href="/publicar" variant="success">
                Publicar mi actividad
              </AppLinkButton>
            </div>
          </SurfaceCard>
        </div>
      </section>
    </main>
  );
}

async function buscarDeportePorSlug(slug: string): Promise<Deporte | null> {
  const deportes = await obtenerDeportes();
  return deportes.find((deporte) => deporte.slug === slug) ?? null;
}
