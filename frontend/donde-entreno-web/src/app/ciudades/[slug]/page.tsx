import Link from "next/link";
import { notFound } from "next/navigation";

import { Header } from "../../../components/layout/Header";
import { AppLinkButton } from "../../../components/ui/AppLinkButton";
import { SectionHeader } from "../../../components/ui/SectionHeader";
import { StatusMessage } from "../../../components/ui/StatusMessage";
import { SurfaceCard } from "../../../components/ui/SurfaceCard";
import { buscarActividades } from "../../../services/actividadService";
import { obtenerCiudadPorSlug } from "../../../services/ciudadService";
import type { Actividad } from "../../../types/actividad";

type CiudadDetallePageProps = {
  params: Promise<{
    slug: string;
  }>;
};

export default async function CiudadDetallePage({
  params,
}: CiudadDetallePageProps) {
  const { slug } = await params;

  const ciudad = await obtenerCiudadPorSlug(slug).catch(() => null);

  if (!ciudad) {
    notFound();
  }

  let actividades: Actividad[] = [];
  let huboErrorActividades = false;

  try {
    const respuestaActividades = await buscarActividades({
      ciudadSlug: ciudad.slug,
      page: 0,
      size: 6,
    });

    actividades = respuestaActividades.contenido;
  } catch {
    huboErrorActividades = true;
  }

  return (
    <main className="min-h-screen bg-gradient-to-br from-[#F8FAFC] via-white to-[#E8F6FB] text-[var(--color-text)]">
      <section className="mx-auto w-full max-w-6xl px-4 py-6">
        <Header />

        <div className="py-8 sm:py-10">
          <AppLinkButton
            href="/ciudades"
            variant="secondary"
            size="sm"
            className="w-fit rounded-full"
          >
            ← Ver todas las ciudades
          </AppLinkButton>

          <SurfaceCard
            as="section"
            variant="info"
            className="mt-6 overflow-hidden bg-gradient-to-br from-white via-[#F8FCFE] to-[#E8F6FB] p-5 sm:p-8 lg:p-10"
          >
            <p className="text-sm font-bold uppercase tracking-[0.2em] text-[var(--color-secondary)]">
              CIUDAD
            </p>
            <h1 className="mt-3 max-w-3xl text-4xl font-extrabold leading-tight text-[var(--color-primary)] sm:text-5xl">
              Actividades deportivas en {ciudad.nombre}
            </h1>
            <p className="mt-4 max-w-2xl text-base leading-7 text-[var(--color-muted)] sm:text-lg">
              Encontrá clases, clubes, profes y espacios para entrenar en esta
              ciudad, con opciones pensadas para moverte cerca.
            </p>

            <div className="mt-7 flex flex-col gap-3 sm:flex-row">
              <AppLinkButton
                href={`/explorar?ciudadSlug=${encodeURIComponent(ciudad.slug)}`}
                variant="primary"
                size="lg"
                fullWidth
                className="sm:w-auto"
              >
                Ver actividades en {ciudad.nombre}
              </AppLinkButton>

              <AppLinkButton
                href="/ciudades"
                variant="secondary"
                size="lg"
                fullWidth
                className="sm:w-auto"
              >
                Ver todas las ciudades
              </AppLinkButton>
            </div>
          </SurfaceCard>

          <SurfaceCard as="section" variant="soft" className="mt-8 p-4 sm:p-6">
            <SectionHeader
              eyebrow="Actividades"
              title={`Opciones para entrenar en ${ciudad.nombre}`}
              description="Un punto de partida para descubrir propuestas disponibles y seguir filtrando por deporte, barrio o nivel."
              action={
                <AppLinkButton
                  href={`/explorar?ciudadSlug=${encodeURIComponent(
                    ciudad.slug
                  )}`}
                  variant="secondary"
                  size="md"
                  className="w-fit"
                >
                  Ver todas
                </AppLinkButton>
              }
              className="mb-6"
            />

            {huboErrorActividades ? (
              <StatusMessage
                variant="warning"
                title="No pudimos cargar las actividades"
                className="p-5"
              >
                <p>
                  La ciudad está disponible, pero no pudimos traer sus
                  actividades ahora. También podés buscar desde Explorar.
                </p>
                <AppLinkButton
                  href={`/explorar?ciudadSlug=${encodeURIComponent(
                    ciudad.slug
                  )}`}
                  variant="secondary"
                  size="sm"
                  className="mt-4 w-fit"
                >
                  Ir a Explorar
                </AppLinkButton>
              </StatusMessage>
            ) : actividades.length === 0 ? (
              <StatusMessage
                variant="info"
                title="Todavía no hay actividades cargadas en esta ciudad"
                className="p-5"
              >
                <p>
                  Estamos preparando propuestas en {ciudad.nombre}. Podés volver
                  más tarde, mirar otras ciudades o enviar una actividad para
                  revisión.
                </p>
                <div className="mt-4 flex flex-col gap-3 sm:flex-row">
                  <AppLinkButton href="/publicar" size="sm">
                    Publicar actividad
                  </AppLinkButton>
                  <AppLinkButton
                    href="/ciudades"
                    variant="secondary"
                    size="sm"
                  >
                    Ver otras ciudades
                  </AppLinkButton>
                </div>
              </StatusMessage>
            ) : (
              <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
                {actividades.map((actividad) => (
                  <Link
                    key={actividad.id}
                    href={`/actividades/${actividad.slug}`}
                    className="group flex min-h-full flex-col rounded-[var(--radius-lg)] border border-[#DDEAF3] bg-white p-5 shadow-[0_14px_34px_rgba(12,52,80,0.08)] transition duration-200 ease-out hover:-translate-y-0.5 hover:border-[#BFDDEA] hover:shadow-[0_18px_45px_rgba(12,52,80,0.12)]"
                  >
                    <p className="text-sm font-bold uppercase tracking-[0.14em] text-[var(--color-secondary)]">
                      {actividad.deporteNombre || "Actividad"}
                    </p>

                    <h2 className="mt-3 text-xl font-extrabold leading-tight text-[var(--color-primary)] transition duration-200 ease-out group-hover:text-[#0B314D]">
                      {actividad.titulo}
                    </h2>

                    <p className="mt-3 flex-1 text-sm leading-6 text-[var(--color-muted)]">
                      {actividad.ubicacionNombre || "Lugar a confirmar"}
                      {actividad.barrioNombre
                        ? ` · ${actividad.barrioNombre}`
                        : ""}
                    </p>

                    <span className="mt-5 w-fit rounded-full bg-[#E6F7EF] px-3 py-1 text-xs font-extrabold text-[#167A4A] transition duration-200 ease-out group-hover:bg-[var(--color-primary)] group-hover:text-white">
                      Ver detalle
                    </span>
                  </Link>
                ))}
              </div>
            )}
          </SurfaceCard>
        </div>
      </section>
    </main>
  );
}
