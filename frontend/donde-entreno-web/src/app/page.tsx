import type { Metadata } from "next";
import Link from "next/link";
import type { Actividad } from "../types/actividad";
import { Header } from "../components/layout/Header";
import { HomeHero } from "../components/home/HomeHero";
import { HomeHowItWorks } from "../components/home/HomeHowItWorks";
import { HomePopularSports } from "../components/home/HomePopularSports";
import { HomePublishCta } from "../components/home/HomePublishCta";
import { ActivityList } from "../components/explorar/ActivityList";
import { ErrorState } from "../components/feedback/ErrorState";
import { DEFAULT_CITY_SLUG } from "../lib/ciudadActiva";
import { buscarActividades } from "../services/actividadService";
import { obtenerCiudadPorSlug } from "../services/ciudadService";

export const dynamic = "force-dynamic";

export const metadata: Metadata = {
  title: "Guía deportiva local",
  description:
    "Encontrá dónde entrenar cerca tuyo: deportes, clubes, profesores, gimnasios y actividades deportivas en tu ciudad.",
  openGraph: {
    title: "DondeEntreno - Guía deportiva local",
    description:
      "Buscá deportes, clubes, profesores, gimnasios y actividades deportivas cerca tuyo.",
  },
};

export default async function Home() {
  let actividades: Actividad[] = [];
  let huboError = false;
  let ciudadNombre = "Mar del Plata";
  const ciudadSlug = DEFAULT_CITY_SLUG;

  try {
    const ciudadDefault = await obtenerCiudadPorSlug(ciudadSlug);
    ciudadNombre = ciudadDefault.nombre;
  } catch {
    ciudadNombre = "Mar del Plata";
  }

  try {
    const respuesta = await buscarActividades({
      ciudadSlug,
      page: 0,
      size: 6,
    });

    actividades = respuesta.contenido;
  } catch (error) {
    huboError = true;
    console.error("Error al cargar actividades:", error);
  }

  return (
    <main className="min-h-screen overflow-x-hidden bg-gradient-to-br from-[#F8FAFC] via-white to-[#E8F6FB] text-[var(--color-text)]">
      <section className="mx-auto w-full max-w-6xl min-w-0 px-4 py-6">
        <Header />

        <div className="py-10 sm:py-14">
          <HomeHero
            ciudadNombreInicial={ciudadNombre}
            ciudadSlugInicial={ciudadSlug}
          />
          <HomePopularSports ciudadSlug={ciudadSlug} />

          <section className="mt-14 rounded-[var(--radius-xl)] border border-[#DDEAF3] bg-[var(--color-surface)] p-5 shadow-[0_18px_45px_rgba(12,52,80,0.10)] sm:mt-16 sm:p-7">
            {huboError ? (
              <div className="mt-6">
                <ErrorState
                  titulo="No pudimos cargar las actividades"
                  descripcion="No pudimos conectarnos con el servidor. Intentá nuevamente en unos minutos."
                  mostrarBotonInicio={false}
                  mostrarBotonExplorar
                />
              </div>
            ) : (
              <ActivityList
                actividades={actividades}
                titulo="Actividades destacadas"
                descripcion={`Opciones disponibles para empezar a moverte en ${ciudadNombre}.`}
              />
            )}

            <div className="mt-6 flex justify-end">
              <Link
                href={`/explorar?ciudadSlug=${encodeURIComponent(ciudadSlug)}`}
                className="rounded-[var(--radius-md)] border border-[#BFDDEA] px-4 py-3 text-center text-sm font-bold text-[var(--color-primary)] transition duration-200 ease-out hover:-translate-y-0.5 hover:border-[var(--color-primary)] hover:bg-[#F8FCFE] active:scale-[0.98]"
              >
                Ver todas
              </Link>
            </div>
          </section>

          <HomeHowItWorks />
          <HomePublishCta ciudadSlug={ciudadSlug} />
        </div>
      </section>
    </main>
  );
}
