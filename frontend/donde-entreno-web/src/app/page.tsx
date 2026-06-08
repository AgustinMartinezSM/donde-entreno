import type { Metadata } from "next";
import type { Actividad } from "../types/actividad";
import { Header } from "../components/layout/Header";
import { HomeHero } from "../components/home/HomeHero";
import { ActivityList } from "../components/explorar/ActivityList";
import { ErrorState } from "../components/feedback/ErrorState";
import { buscarActividades } from "../services/actividadService";

export const dynamic = "force-dynamic";

export const metadata: Metadata = {
  /*
    Metadata específica de la Home.
    Como en layout.tsx usamos template "%s | DondeEntreno",
    este title queda como:
    "Guía deportiva local | DondeEntreno"
  */
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

  try {
    // Pedimos las primeras 6 actividades al backend.
    const respuesta = await buscarActividades({
      page: 0,
      size: 6,
    });

    actividades = respuesta.contenido;
  } catch (error) {
    /*
      Si el backend está apagado o falla la petición,
      marcamos que hubo error para mostrar un mensaje prolijo.
    */
    huboError = true;
    console.error("Error al cargar actividades:", error);
  }

  return (
    <main className="min-h-screen bg-[var(--color-bg)] text-[var(--color-text)]">
      <section className="mx-auto w-full max-w-6xl px-4 py-6">
        <Header />

        <div className="py-16">
          <HomeHero />

          {huboError ? (
            <div className="mt-12">
              <ErrorState
                titulo="No pudimos cargar las actividades"
                descripcion="Puede que el backend esté apagado o que haya un problema temporal con la conexión."
                mostrarBotonInicio={false}
                mostrarBotonExplorar
              />
            </div>
          ) : (
            <ActivityList actividades={actividades} />
          )}
        </div>
      </section>
    </main>
  );
}