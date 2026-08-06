import type { Metadata } from "next";
import type { Actividad } from "../types/actividad";
import { Header } from "../components/layout/Header";
import { HomeHero } from "../components/home/HomeHero";
import { HomeDiscoveryFeed } from "../components/home/HomeDiscoveryFeed";
import { HomeCrearCuentaCta } from "../components/home/HomeCrearCuentaCta";
import { HomeHowItWorks } from "../components/home/HomeHowItWorks";
import { HomePopularSports } from "../components/home/HomePopularSports";
import { HomePublishCta } from "../components/home/HomePublishCta";
import { DEFAULT_CITY_SLUG } from "../lib/ciudadActiva";
import { buscarActividades } from "../services/actividadService";
import { obtenerCiudadPorSlug } from "../services/ciudadService";

export const dynamic = "force-dynamic";

export const metadata: Metadata = {
  title: "Descubrí dónde entrenar cerca tuyo",
  description:
    "Descubrí actividades, clubes, profesores y espacios deportivos cerca tuyo. Guardá lo que te interesa y conectate con tu comunidad deportiva local.",
  openGraph: {
    title: "DondeEntreno - Tu comunidad deportiva local",
    description:
      "Descubrí dónde entrenar, guardá actividades y seguí a clubes y profesores de tu ciudad.",
  },
};

type HomeProps = {
  searchParams: Promise<{
    ciudadSlug?: string | string[];
  }>;
};

export default async function Home({ searchParams }: HomeProps) {
  let actividades: Actividad[] = [];
  let huboError = false;
  let ciudadNombre = "Mar del Plata";
  const params = await searchParams;
  const ciudadSlugSolicitada = Array.isArray(params.ciudadSlug)
    ? params.ciudadSlug[0]
    : params.ciudadSlug;
  let ciudadSlug = ciudadSlugSolicitada?.trim() || DEFAULT_CITY_SLUG;

  try {
    const ciudadDefault = await obtenerCiudadPorSlug(ciudadSlug);
    ciudadNombre = ciudadDefault.nombre;
  } catch {
    ciudadSlug = DEFAULT_CITY_SLUG;

    try {
      const ciudadDefault = await obtenerCiudadPorSlug(ciudadSlug);
      ciudadNombre = ciudadDefault.nombre;
    } catch {
      ciudadNombre = "Mar del Plata";
    }
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
    <main className="min-h-screen overflow-x-hidden bg-[var(--color-bg)] text-[var(--color-text)]">
      <section className="mx-auto w-full max-w-6xl min-w-0 px-4 py-6">
        <Header />

        <div className="py-7 sm:py-10">
          <HomeHero
            ciudadNombreInicial={ciudadNombre}
            ciudadSlugInicial={ciudadSlug}
          />

          <HomeDiscoveryFeed
            actividades={actividades}
            ciudadNombre={ciudadNombre}
            ciudadSlug={ciudadSlug}
            huboError={huboError}
          />

          <HomePopularSports ciudadSlug={ciudadSlug} />
          <HomeCrearCuentaCta />
          <HomeHowItWorks />
          <HomePublishCta ciudadSlug={ciudadSlug} />
        </div>
      </section>
    </main>
  );
}
