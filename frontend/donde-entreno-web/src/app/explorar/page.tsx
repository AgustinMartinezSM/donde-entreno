import type { Metadata } from "next";
import type { Actividad } from "../../types/actividad";
import type { FiltrosOpciones } from "../../types/filtros";

import { Header } from "../../components/layout/Header";
import { ActivityList } from "../../components/explorar/ActivityList";
import { buscarActividades } from "../../services/actividadService";
import { SearchBar } from "../../components/home/SearchBar";
import { Pagination } from "../../components/explorar/Pagination";
import { SortSelect } from "../../components/explorar/SortSelect";
import { FiltersPanel } from "../../components/explorar/FiltersPanel";
import { obtenerOpcionesFiltros } from "../../services/filtrosService";
import { ErrorState } from "../../components/feedback/ErrorState";

export const metadata: Metadata = {
  /*
    Metadata específica para la página de exploración.
    En el navegador se va a ver:
    "Explorar actividades | DondeEntreno"
  */
  title: "Explorar actividades",
  description:
    "Buscá y compará actividades deportivas, clubes, gimnasios y profesores disponibles en tu ciudad.",
  openGraph: {
    title: "Explorar actividades - DondeEntreno",
    description:
      "Encontrá deportes, clubes, gimnasios, profesores y actividades deportivas cerca tuyo.",
  },
};

type ExplorarPageProps = {
  searchParams: Promise<{
    texto?: string;
    page?: string;
    orden?: string;

    ciudadId?: string;
    barrioId?: string;
    deporteSlug?: string;
    nivel?: string;
    modalidad?: string;
  }>;
};

export default async function ExplorarPage({ searchParams }: ExplorarPageProps) {
  /*
    En Next.js 16, searchParams puede venir como Promise.
    Por eso usamos await para leer los parámetros de la URL.
  */
  const params = await searchParams;

  /*
    Leemos los parámetros que vengan en la URL.
    Ejemplos:
    /explorar?texto=boxeo
    /explorar?deporteSlug=jiu-jitsu&page=0
    /explorar?orden=precio_asc&page=0
  */
  const textoBuscado = params.texto || "";
  const paginaActual = params.page ? Number(params.page) : 0;
  const ordenActual = params.orden || "";

  const ciudadIdActual = params.ciudadId || "";
  const barrioIdActual = params.barrioId || "";
  const deporteSlugActual = params.deporteSlug || "";
  const nivelActual = params.nivel || "";
  const modalidadActual = params.modalidad || "";

  let actividades: Actividad[] = [];
  let totalElementos = 0;
  let totalPaginas = 0;
  let huboError = false;

  /*
    Opciones iniciales vacías para evitar que la página explote
    si todavía no llegaron los filtros o si el backend falla.
  */
  let filtros: FiltrosOpciones = {
    categorias: [],
    deportes: [],
    ciudades: [],
    barrios: [],
    niveles: [],
    modalidades: [],
    ordenes: [],
  };

  try {
    /*
      Pedimos actividades y opciones de filtros al backend al mismo tiempo.
      Si textoBuscado está vacío, trae actividades sin filtrar.
    */
    const [respuestaActividades, respuestaFiltros] = await Promise.all([
      buscarActividades({
        texto: textoBuscado,
        page: paginaActual,
        size: 6,
        orden: ordenActual,

        ciudadId: ciudadIdActual ? Number(ciudadIdActual) : undefined,
        barrioId: barrioIdActual ? Number(barrioIdActual) : undefined,
        deporteSlug: deporteSlugActual || undefined,
        nivel: nivelActual || undefined,
        modalidad: modalidadActual || undefined,
      }),
      obtenerOpcionesFiltros(),
    ]);

    actividades = respuestaActividades.contenido;
    totalElementos = respuestaActividades.totalElementos;
    totalPaginas = respuestaActividades.totalPaginas;
    filtros = respuestaFiltros;
  } catch (error) {
    /*
      Si el backend está apagado o falla alguna petición,
      mostramos un estado de error prolijo.
    */
    huboError = true;
    console.error("Error al cargar actividades o filtros:", error);
  }

  const descripcionResultados = textoBuscado
    ? totalElementos === 1
      ? `1 actividad encontrada relacionada con "${textoBuscado}".`
      : `${totalElementos} actividades encontradas relacionadas con "${textoBuscado}".`
    : totalElementos === 1
      ? "1 actividad encontrada en DondeEntreno."
      : `${totalElementos} actividades encontradas en DondeEntreno.`;

  return (
    <main className="min-h-screen bg-gradient-to-br from-[#F8FAFC] via-white to-[#E8F6FB] text-[var(--color-text)]">
      <section className="mx-auto w-full max-w-6xl px-4 py-6">
        <Header />

        <div className="py-8 sm:py-10">
          <div className="mb-10 border-b border-[#DDEAF3] pb-8">
            <p className="mb-3 text-sm font-bold uppercase tracking-[0.2em] text-[var(--color-secondary)]">
              Explorar actividades
            </p>

            <h1 className="text-3xl font-extrabold leading-tight text-[var(--color-primary)] sm:text-4xl">
              {textoBuscado
                ? `Resultados para "${textoBuscado}"`
                : "Todas las actividades"}
            </h1>

            <p className="mt-3 max-w-2xl text-base leading-7 text-[var(--color-muted)]">
              Buscá y compará actividades deportivas disponibles en tu ciudad.
            </p>

            {/* Dejamos el buscador visible aunque haya error */}
            <div className="max-w-2xl transition duration-200 ease-out">
              <SearchBar valorInicial={textoBuscado} />
            </div>

            {huboError ? (
              <div className="mt-8">
                <ErrorState
                  titulo="No pudimos cargar las actividades"
                  descripcion="No pudimos conectarnos con el servidor. Intentá nuevamente en unos minutos."
                  mostrarBotonInicio
                  mostrarBotonExplorar={false}
                />
              </div>
            ) : (
              <>
                <SortSelect
                  textoBuscado={textoBuscado}
                  ordenActual={ordenActual}
                  ciudadIdActual={ciudadIdActual}
                  barrioIdActual={barrioIdActual}
                  deporteSlugActual={deporteSlugActual}
                  nivelActual={nivelActual}
                  modalidadActual={modalidadActual}
                />

                <FiltersPanel
                  filtros={filtros}
                  textoBuscado={textoBuscado}
                  ciudadIdActual={ciudadIdActual}
                  barrioIdActual={barrioIdActual}
                  deporteSlugActual={deporteSlugActual}
                  nivelActual={nivelActual}
                  modalidadActual={modalidadActual}
                  ordenActual={ordenActual}
                />
              </>
            )}
          </div>

          {!huboError && (
            <>
              <ActivityList
                actividades={actividades}
                titulo="Resultados encontrados"
                descripcion={descripcionResultados}
              />

              <Pagination
                paginaActual={paginaActual}
                totalPaginas={totalPaginas}
                textoBuscado={textoBuscado}
                ordenActual={ordenActual}
                ciudadIdActual={ciudadIdActual}
                barrioIdActual={barrioIdActual}
                deporteSlugActual={deporteSlugActual}
                nivelActual={nivelActual}
                modalidadActual={modalidadActual}
              />
            </>
          )}
        </div>
      </section>
    </main>
  );
}
