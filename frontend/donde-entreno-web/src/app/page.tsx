import type { Metadata } from "next";
import type { Actividad } from "../types/actividad";
import type { PerfilPublicadorPublico } from "../types/publicadorPublico";
import type { Deporte } from "../types/deporte";
import { Header } from "../components/layout/Header";
import { HomeTopBar } from "../components/home/HomeTopBar";
import { HomeStoriesDeportes } from "../components/home/HomeStoriesDeportes";
import { HomeFeedSeguidos } from "../components/home/HomeFeedSeguidos";
import { HomeParaVos } from "../components/home/HomeParaVos";
import { HomeDiscoveryFeed } from "../components/home/HomeDiscoveryFeed";
import { HomeCrearCuentaCta } from "../components/home/HomeCrearCuentaCta";
import { HomeHowItWorks } from "../components/home/HomeHowItWorks";
import { HomePopularSports } from "../components/home/HomePopularSports";
import { HomePreferenciasChips } from "../components/home/HomePreferenciasChips";
import { HomePublicadoresSugeridos } from "../components/home/HomePublicadoresSugeridos";
import { HomePublishCta } from "../components/home/HomePublishCta";
import { DEFAULT_CITY_SLUG } from "../lib/ciudadActiva";
import { buscarActividades } from "../services/actividadService";
import { obtenerCiudadPorSlug } from "../services/ciudadService";
import { obtenerDeportes } from "../services/deportesService";
import { obtenerPerfilesPublicadores } from "../services/perfilPublicadorService";

export const dynamic = "force-dynamic";

export const metadata: Metadata = {
  title: "Descubrí dónde entrenar cerca tuyo",
  description:
    "Descubrí actividades, clubes, profesores y espacios deportivos cerca tuyo. Guardá lo que te interesa y conectate con tu comunidad deportiva local.",
  alternates: {
    canonical: "/",
  },
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

    /*
      Contenido visual primero (bloque 12): dentro de la misma página de
      resultados, las actividades CON foto real encabezan el feed. Es un
      reorden estable de los mismos datos — no inventa ranking, solo
      pone adelante lo que mejor se ve.
    */
    actividades = [...respuesta.contenido].sort(
      (a, b) =>
        Number(Boolean(b.imagenPrincipalUrl)) -
        Number(Boolean(a.imagenPrincipalUrl))
    );
  } catch (error) {
    huboError = true;
    console.error("Error al cargar actividades:", error);
  }

  /*
    Publicadores para la sección "Clubes y profes para seguir".
    Best-effort: si falla, la home sigue completa sin esa sección.
  */
  let publicadoresSugeridos: PerfilPublicadorPublico[] = [];

  try {
    publicadoresSugeridos = (await obtenerPerfilesPublicadores()).slice(0, 3);
  } catch (error) {
    console.error("Error al cargar perfiles publicadores:", error);
  }

  /*
    Catálogo real para la fila de stories. Best-effort: sin deportes la
    fila no se dibuja y la home sigue completa.
  */
  let deportes: Deporte[] = [];

  try {
    deportes = await obtenerDeportes();
  } catch (error) {
    console.error("Error al cargar deportes:", error);
  }

  return (
    /* overflow-x-clip (y no hidden): hidden crea un scroll container y rompe el sticky del Header. */
    <main className="min-h-screen overflow-x-clip text-[var(--color-text)]">
      <section className="mx-auto w-full max-w-6xl min-w-0 px-4 py-6">
        <Header />

        {/*
          Orden de app social: primero dónde estás y qué buscar, después
          contenido real (stories, feed, publicadores) y recién al final
          lo explicativo. Antes la home abría con un hero de una pantalla
          entera y el primer contenido quedaba abajo del pliegue.
        */}
        <div className="py-5 sm:py-7">
          <HomeTopBar ciudadNombre={ciudadNombre} ciudadSlug={ciudadSlug} />

          <HomeStoriesDeportes deportes={deportes} ciudadSlug={ciudadSlug} />

          {/*
            Para quien tiene cuenta, lo primero es lo nuevo de quienes
            sigue; para el visitante anónimo no se dibuja nada y la home
            arranca por el descubrimiento general.
          */}
          <HomeFeedSeguidos publicadoresSugeridos={publicadoresSugeridos} />

          {/*
            "Para vos" (bloque 12): recomendaciones reales por ciudad +
            deportes elegidos, solo para logueados con preferencias. El
            visitante sigue arrancando por el descubrimiento general.
          */}
          <HomeParaVos ciudadSlug={ciudadSlug} ciudadNombre={ciudadNombre} />

          <HomePreferenciasChips ciudadSlug={ciudadSlug} />

          <HomeDiscoveryFeed
            actividades={actividades}
            ciudadNombre={ciudadNombre}
            ciudadSlug={ciudadSlug}
            huboError={huboError}
          />

          <HomePublicadoresSugeridos publicadores={publicadoresSugeridos} />
          <HomePopularSports ciudadSlug={ciudadSlug} />
          <HomeCrearCuentaCta />
          <HomeHowItWorks />
          <HomePublishCta ciudadSlug={ciudadSlug} />
        </div>
      </section>
    </main>
  );
}
