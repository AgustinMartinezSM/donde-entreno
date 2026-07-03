import type { Metadata } from "next";

import { Header } from "../../components/layout/Header";
import { AppLinkButton } from "../../components/ui/AppLinkButton";
import { SectionHeader } from "../../components/ui/SectionHeader";
import { StatusMessage } from "../../components/ui/StatusMessage";
import { SurfaceCard } from "../../components/ui/SurfaceCard";
import { obtenerCiudades } from "../../services/ciudadService";
import type { Ciudad } from "../../types/ciudad";

export const metadata: Metadata = {
  title: "Ciudades",
  description:
    "Elegí tu ciudad para ver actividades, clubes, profes y espacios cerca tuyo.",
};

function ordenarCiudades(ciudadA: Ciudad, ciudadB: Ciudad) {
  const ordenA = ciudadA.orden;
  const ordenB = ciudadB.orden;

  if (ordenA !== null && ordenB !== null && ordenA !== ordenB) {
    return ordenA - ordenB;
  }

  if (ordenA !== null && ordenB === null) {
    return -1;
  }

  if (ordenA === null && ordenB !== null) {
    return 1;
  }

  return ciudadA.nombre.localeCompare(ciudadB.nombre, "es");
}

export default async function CiudadesPage() {
  let ciudades: Ciudad[] = [];
  let huboError = false;

  try {
    ciudades = (await obtenerCiudades())
      .filter((ciudad) => ciudad.activa)
      .sort(ordenarCiudades);
  } catch {
    huboError = true;
  }

  return (
    <main className="min-h-screen bg-gradient-to-br from-[#F8FAFC] via-white to-[#E8F6FB] text-[var(--color-text)]">
      <section className="mx-auto w-full max-w-6xl px-4 py-6">
        <Header />

        <div className="py-8 sm:py-10">
          <SurfaceCard
            as="section"
            variant="info"
            className="overflow-hidden bg-gradient-to-br from-white via-[#F8FCFE] to-[#E8F6FB] p-5 sm:p-8 lg:p-10"
          >
            <p className="text-sm font-bold uppercase tracking-[0.2em] text-[var(--color-secondary)]">
              CIUDADES
            </p>
            <h1 className="mt-3 max-w-3xl text-4xl font-extrabold leading-tight text-[var(--color-primary)] sm:text-5xl">
              Explorá actividades por ciudad
            </h1>
            <p className="mt-4 max-w-2xl text-base leading-7 text-[var(--color-muted)] sm:text-lg">
              Elegí tu ciudad para ver actividades, clubes, profes y espacios
              cerca tuyo.
            </p>
          </SurfaceCard>

          <SurfaceCard as="section" variant="soft" className="mt-8 p-4 sm:p-6">
            <SectionHeader
              eyebrow="Disponibles"
              title="Ciudades para explorar"
              description="Cada ciudad reúne actividades y espacios deportivos disponibles para buscar por zona."
              className="mb-6"
            />

            {huboError ? (
              <StatusMessage
                variant="error"
                title="No pudimos cargar las ciudades"
                className="p-5"
              >
                <p>
                  Puede haber un problema temporal de conexión. Intentá
                  nuevamente en unos minutos.
                </p>
              </StatusMessage>
            ) : ciudades.length === 0 ? (
              <StatusMessage
                variant="info"
                title="No hay ciudades disponibles para mostrar"
                className="p-5"
              >
                <p>
                  Muy pronto vas a encontrar nuevas ciudades para explorar
                  actividades deportivas.
                </p>
              </StatusMessage>
            ) : (
              <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
                {ciudades.map((ciudad) => (
                  <article
                    key={ciudad.id}
                    className="flex min-h-full flex-col rounded-[var(--radius-lg)] border border-[#DDEAF3] bg-white p-5 shadow-[0_14px_34px_rgba(12,52,80,0.08)] transition duration-200 ease-out hover:-translate-y-0.5 hover:border-[#BFDDEA] hover:shadow-[0_18px_45px_rgba(12,52,80,0.12)]"
                  >
                    <span className="w-fit rounded-full bg-[#E6F7EF] px-3 py-1 text-xs font-extrabold uppercase tracking-[0.12em] text-[#167A4A]">
                      Ciudad disponible
                    </span>

                    <h2 className="mt-4 text-2xl font-extrabold text-[var(--color-primary)]">
                      {ciudad.nombre}
                    </h2>

                    <p className="mt-2 flex-1 text-sm leading-6 text-[var(--color-muted)]">
                      Encontrá actividades, clubes, profes y espacios para
                      entrenar en {ciudad.nombre}.
                    </p>

                    <div className="mt-5 flex flex-col gap-3">
                      <AppLinkButton
                        href={`/explorar?ciudadSlug=${encodeURIComponent(
                          ciudad.slug
                        )}`}
                        variant="primary"
                        size="md"
                        fullWidth
                      >
                        Ver actividades
                      </AppLinkButton>

                      <AppLinkButton
                        href={`/ciudades/${encodeURIComponent(ciudad.slug)}`}
                        variant="secondary"
                        size="md"
                        fullWidth
                      >
                        Ver página de ciudad
                      </AppLinkButton>
                    </div>
                  </article>
                ))}
              </div>
            )}
          </SurfaceCard>
        </div>
      </section>
    </main>
  );
}
