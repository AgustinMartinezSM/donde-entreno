import type { Metadata } from "next";
import Image from "next/image";
import { notFound } from "next/navigation";

import type { Actividad } from "../../../../types/actividad";
import type { Ciudad } from "../../../../types/ciudad";
import type { Deporte } from "../../../../types/deporte";
import { Header } from "../../../../components/layout/Header";
import { SocialActivityCard } from "../../../../components/social/SocialActivityCard";
import { ErrorState } from "../../../../components/feedback/ErrorState";
import { AppLinkButton } from "../../../../components/ui/AppLinkButton";
import { SectionHeader } from "../../../../components/ui/SectionHeader";
import { StatusMessage } from "../../../../components/ui/StatusMessage";
import { SurfaceCard } from "../../../../components/ui/SurfaceCard";
import { obtenerImagenFallbackActividad } from "../../../../lib/activityImages";
import { buscarActividades } from "../../../../services/actividadService";
import { obtenerCiudadPorSlug } from "../../../../services/ciudadService";
import { obtenerDeportes } from "../../../../services/deportesService";

/*
  Landing pública por ciudad + deporte (SEO local combinado):
  /ciudades/mar-del-plata/jiu-jitsu, /ciudades/mar-del-plata/yoga, etc.

  Es la página más específica del interlinking: cada combinación
  ciudad×deporte tiene su propia URL indexable con las actividades
  reales de ese deporte en esa ciudad.
*/

type CiudadDeporteProps = {
  params: Promise<{
    slug: string;
    deporte: string;
  }>;
};

async function cargarCiudadYDeporte(
  slug: string,
  deporteSlug: string
): Promise<{ ciudad: Ciudad; deporte: Deporte } | null> {
  const [ciudad, deportes] = await Promise.all([
    obtenerCiudadPorSlug(slug).catch(() => null),
    obtenerDeportes(),
  ]);

  const deporte = deportes.find((item) => item.slug === deporteSlug) ?? null;

  if (!ciudad || !deporte) {
    return null;
  }

  return { ciudad, deporte };
}

export async function generateMetadata({
  params,
}: CiudadDeporteProps): Promise<Metadata> {
  const { slug, deporte } = await params;

  try {
    const datos = await cargarCiudadYDeporte(slug, deporte);

    if (!datos) {
      return { title: "Deporte no encontrado" };
    }

    const titulo = `${datos.deporte.nombre} en ${datos.ciudad.nombre}`;
    const descripcion = `Encontrá clubes, profes y clases de ${datos.deporte.nombre} en ${datos.ciudad.nombre}, con horarios, precios y contacto directo.`;

    return {
      title: titulo,
      description: descripcion,
      alternates: {
        canonical: `/ciudades/${datos.ciudad.slug}/${datos.deporte.slug}`,
      },
      openGraph: {
        title: `${titulo} | DondeEntreno`,
        description: descripcion,
        type: "website",
      },
    };
  } catch (error) {
    console.error("Error al generar metadata de ciudad + deporte:", error);
    return { title: "Actividades por ciudad" };
  }
}

export default async function CiudadDeporteLandingPage({
  params,
}: CiudadDeporteProps) {
  const { slug, deporte: deporteSlug } = await params;

  let datos: { ciudad: Ciudad; deporte: Deporte } | null = null;
  let deportesCatalogo: Deporte[] = [];
  let huboErrorCatalogo = false;

  try {
    const [resuelto, deportes] = await Promise.all([
      cargarCiudadYDeporte(slug, deporteSlug),
      obtenerDeportes(),
    ]);
    datos = resuelto;
    deportesCatalogo = deportes;
  } catch (error) {
    huboErrorCatalogo = true;
    console.error("Error al cargar ciudad + deporte:", error);
  }

  if (huboErrorCatalogo) {
    return (
      <main className="min-h-screen bg-gradient-to-br from-[#F8FAFC] via-white to-[#E8F6FB] text-[var(--color-text)]">
        <section className="mx-auto w-full max-w-6xl px-4 py-6">
          <Header />
          <div className="py-10">
            <ErrorState
              titulo="No pudimos cargar esta página"
              descripcion="No pudimos cargar esta información. Probá nuevamente en unos segundos o volvé al inicio."
              mostrarBotonInicio
              mostrarBotonExplorar
            />
          </div>
        </section>
      </main>
    );
  }

  if (!datos) {
    notFound();
  }

  const { ciudad, deporte } = datos;

  let actividades: Actividad[] = [];
  let totalActividades = 0;
  let huboErrorActividades = false;

  try {
    const respuesta = await buscarActividades({
      deporteSlug: deporte.slug,
      ciudadSlug: ciudad.slug,
      page: 0,
      size: 6,
    });

    actividades = respuesta.contenido;
    totalActividades = respuesta.totalElementos;
  } catch (error) {
    huboErrorActividades = true;
    console.error("Error al cargar actividades de ciudad + deporte:", error);
  }

  const relacionados = deporte.categoriaSlug
    ? deportesCatalogo
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
  )}&ciudadSlug=${encodeURIComponent(ciudad.slug)}`;

  return (
    <main className="min-h-screen bg-gradient-to-br from-[#F8FAFC] via-white to-[#E8F6FB] text-[var(--color-text)]">
      <section className="mx-auto w-full max-w-6xl px-4 py-6">
        <Header />

        <div className="py-8 sm:py-10">
          <div className="flex flex-wrap gap-2 text-sm font-bold text-[var(--color-muted)]">
            <AppLinkButton
              href={`/ciudades/${encodeURIComponent(ciudad.slug)}`}
              variant="secondary"
              size="sm"
              className="rounded-full"
            >
              ← {ciudad.nombre}
            </AppLinkButton>
            <AppLinkButton
              href={`/deportes/${encodeURIComponent(deporte.slug)}`}
              variant="secondary"
              size="sm"
              className="rounded-full"
            >
              {deporte.nombre} en todas las ciudades
            </AppLinkButton>
          </div>

          <SurfaceCard as="section" className="mt-6 overflow-hidden">
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
                  <span className="text-[#7FDCA8]">en {ciudad.nombre}</span>
                </h1>
              </div>
            </div>

            <div className="p-5 sm:p-7">
              <p className="max-w-3xl text-base leading-7 text-[var(--color-muted)]">
                {deporte.descripcion ||
                  `Clubes, profes y espacios para practicar ${deporte.nombre} en ${ciudad.nombre}, con horarios, precios de referencia y contacto directo.`}
              </p>

              <div className="mt-5 flex flex-wrap gap-3">
                <AppLinkButton href={hrefExplorar}>
                  Ver todas las actividades
                </AppLinkButton>
                <AppLinkButton
                  href={`/ciudades/${encodeURIComponent(ciudad.slug)}`}
                  variant="secondary"
                >
                  Otros deportes en {ciudad.nombre}
                </AppLinkButton>
              </div>
            </div>
          </SurfaceCard>

          <SurfaceCard as="section" variant="soft" className="mt-8 p-4 sm:p-6">
            <SectionHeader
              eyebrow="ACTIVIDADES"
              title={`${deporte.nombre} en ${ciudad.nombre}`}
              description={
                totalActividades > 0
                  ? `${totalActividades} ${
                      totalActividades === 1
                        ? "actividad disponible"
                        : "actividades disponibles"
                    }.`
                  : `Opciones disponibles en ${ciudad.nombre}.`
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
                title={`Todavía no hay actividades de ${deporte.nombre} publicadas en ${ciudad.nombre}`}
                className="p-7 text-center"
              >
                <p className="mx-auto max-w-xl">
                  ¿Das clases de {deporte.nombre} en {ciudad.nombre}? Publicá tu
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
                title={`Otros ${
                  deporte.categoriaNombre
                    ? deporte.categoriaNombre.toLowerCase()
                    : "deportes"
                } en ${ciudad.nombre}`}
                description="Disciplinas de la misma categoría en esta ciudad."
                className="mb-5"
              />
              <div className="flex flex-wrap gap-2">
                {relacionados.map((relacionado) => (
                  <AppLinkButton
                    key={relacionado.slug}
                    href={`/ciudades/${encodeURIComponent(
                      ciudad.slug
                    )}/${encodeURIComponent(relacionado.slug)}`}
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
        </div>
      </section>
    </main>
  );
}
