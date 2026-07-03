import type { Metadata } from "next";
import type { Actividad } from "../../types/actividad";
import type { FiltrosOpciones } from "../../types/filtros";

import { Header } from "../../components/layout/Header";
import { ActivityCard } from "../../components/explorar/ActivityCard";
import { buscarActividades } from "../../services/actividadService";
import { obtenerCiudadPorSlug } from "../../services/ciudadService";
import { SearchBar } from "../../components/home/SearchBar";
import { Pagination } from "../../components/explorar/Pagination";
import { SortSelect } from "../../components/explorar/SortSelect";
import { FiltersPanel } from "../../components/explorar/FiltersPanel";
import { obtenerOpcionesFiltros } from "../../services/filtrosService";
import { AppLinkButton } from "../../components/ui/AppLinkButton";
import { ErrorState } from "../../components/feedback/ErrorState";
import { SectionHeader } from "../../components/ui/SectionHeader";
import { StatusMessage } from "../../components/ui/StatusMessage";
import { SurfaceCard } from "../../components/ui/SurfaceCard";

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
    ciudadSlug?: string;
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
  const ciudadSlugActual = params.ciudadSlug?.trim() || "";
  const barrioIdActual = params.barrioId || "";
  const deporteSlugActual = params.deporteSlug || "";
  const nivelActual = params.nivel || "";
  const modalidadActual = params.modalidad || "";
  const ciudadIdParaBusqueda = ciudadSlugActual
    ? undefined
    : ciudadIdActual
      ? Number(ciudadIdActual)
      : undefined;

  let actividades: Actividad[] = [];
  let totalPaginas = 0;
  let huboError = false;
  let huboErrorCiudad = false;
  let nombreCiudadActiva: string | null = null;

  if (ciudadSlugActual) {
    try {
      const ciudadActiva = await obtenerCiudadPorSlug(ciudadSlugActual);
      nombreCiudadActiva = ciudadActiva.nombre;
    } catch {
      huboErrorCiudad = true;
    }
  }

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
    if (!huboErrorCiudad) {
      const [respuestaActividades, respuestaFiltros] = await Promise.all([
        buscarActividades({
          texto: textoBuscado,
          page: paginaActual,
          size: 6,
          orden: ordenActual,

          ciudadId: ciudadIdParaBusqueda,
          ciudadSlug: ciudadSlugActual || undefined,
          barrioId: barrioIdActual ? Number(barrioIdActual) : undefined,
          deporteSlug: deporteSlugActual || undefined,
          nivel: nivelActual || undefined,
          modalidad: modalidadActual || undefined,
        }),
        obtenerOpcionesFiltros(),
      ]);

      actividades = respuestaActividades.contenido;
      totalPaginas = respuestaActividades.totalPaginas;
      filtros = respuestaFiltros;
    }
  } catch (error) {
    /*
      Si falla alguna petición,
      mostramos un estado de error prolijo.
    */
    huboError = true;
    console.error("Error al cargar actividades o filtros:", error);
  }

  return (
    <main className="min-h-screen bg-gradient-to-br from-[#F8FAFC] via-white to-[#E8F6FB] text-[var(--color-text)]">
      <section className="mx-auto w-full max-w-6xl px-4 py-6">
        <Header />

        <div className="py-8 sm:py-10">
          <SurfaceCard
            as="section"
            variant="info"
            className="overflow-hidden bg-gradient-to-br from-white via-[#F8FCFE] to-[#E8F6FB] p-5 sm:p-6"
          >
            <p className="mb-3 text-sm font-bold uppercase tracking-[0.2em] text-[var(--color-secondary)]">
              EXPLORAR
            </p>

            <h1 className="max-w-3xl text-3xl font-extrabold leading-tight text-[var(--color-primary)] sm:text-4xl">
              Explorá{" "}
              <span className="text-[var(--color-secondary)]">
                actividades
              </span>
              {nombreCiudadActiva
                ? ` en ${nombreCiudadActiva}`
                : " cerca tuyo"}
            </h1>

            <p className="mt-3 max-w-2xl text-base leading-7 text-[var(--color-muted)]">
              {nombreCiudadActiva
                ? `Opciones disponibles para entrenar en ${nombreCiudadActiva}. Podés combinar deporte, barrio, nivel y modalidad.`
                : "Buscá por deporte, zona, nivel o modalidad y descubrí opciones cerca tuyo."}
            </p>

            <div className="mt-4 flex flex-wrap items-center gap-3">
              {nombreCiudadActiva ? (
                <p className="inline-flex rounded-full bg-[#E6F7EF] px-3 py-2 text-sm font-bold text-[#167A4A]">
                  Explorando actividades en {nombreCiudadActiva}
                </p>
              ) : null}

              {textoBuscado ? (
                <p className="inline-flex rounded-full bg-[#E6F7EF] px-3 py-2 text-sm font-bold text-[#167A4A]">
                  Resultados para &quot;{textoBuscado}&quot;
                </p>
              ) : null}

              {ciudadSlugActual ? (
                <AppLinkButton
                  href="/ciudades"
                  variant="secondary"
                  size="sm"
                  className="rounded-full"
                >
                  Cambiar ciudad
                </AppLinkButton>
              ) : null}
            </div>

            {/* Dejamos el buscador visible aunque haya error */}
            <div className="max-w-3xl transition duration-200 ease-out">
              {ciudadSlugActual ? (
                <form
                  action="/explorar"
                  method="get"
                  className="mt-8 w-full min-w-0 rounded-[24px] border border-[#BFDDEA] bg-white/95 p-2.5 shadow-[0_18px_45px_rgba(12,52,80,0.12)] transition duration-200 ease-out focus-within:border-[var(--color-accent)] focus-within:ring-4 focus-within:ring-[#DDEAF3] sm:p-3"
                >
                  <div className="flex min-w-0 flex-col gap-3 sm:flex-row">
                    <input
                      type="text"
                      name="texto"
                      defaultValue={textoBuscado}
                      placeholder="Buscar deporte, actividad o club"
                      className="min-h-12 w-full min-w-0 flex-1 rounded-[18px] border border-transparent bg-[#F8FAFC] px-4 text-sm font-medium text-[var(--color-text)] outline-none transition duration-200 ease-out placeholder:text-[var(--color-muted)] hover:border-[#BFDDEA] focus:border-[var(--color-accent)] sm:min-h-14"
                    />
                    <input
                      type="hidden"
                      name="ciudadSlug"
                      value={ciudadSlugActual}
                    />
                    <input type="hidden" name="page" value="0" />

                    <button
                      type="submit"
                      className="min-h-12 w-full rounded-[18px] bg-[var(--color-primary)] px-6 font-bold text-white shadow-[var(--shadow-button)] transition duration-200 ease-out hover:-translate-y-0.5 hover:bg-[#0B314D] active:scale-[0.98] sm:min-h-14 sm:w-auto sm:min-w-32"
                    >
                      Buscar
                    </button>
                  </div>
                </form>
              ) : (
                <SearchBar valorInicial={textoBuscado} />
              )}
            </div>
          </SurfaceCard>

          <div className="mt-6">
            {huboErrorCiudad ? (
              <StatusMessage
                variant="warning"
                title="No pudimos cargar la ciudad seleccionada"
                className="mt-8 p-5"
              >
                <p>
                  Revisá la ciudad elegida o volvé al listado de ciudades para
                  explorar opciones disponibles.
                </p>
                <AppLinkButton
                  href="/ciudades"
                  variant="secondary"
                  size="sm"
                  className="mt-4 w-fit"
                >
                  Ver ciudades
                </AppLinkButton>
              </StatusMessage>
            ) : huboError ? (
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
                <FiltersPanel
                  filtros={filtros}
                  textoBuscado={textoBuscado}
                  ciudadIdActual={ciudadIdActual}
                  ciudadSlugActual={ciudadSlugActual}
                  barrioIdActual={barrioIdActual}
                  deporteSlugActual={deporteSlugActual}
                  nivelActual={nivelActual}
                  modalidadActual={modalidadActual}
                  ordenActual={ordenActual}
                />
              </>
            )}
          </div>

          {!huboError && !huboErrorCiudad && (
            <>
              <SurfaceCard
                as="section"
                variant="soft"
                className="mt-8 p-4 sm:p-6"
              >
                <SectionHeader
                  eyebrow="ACTIVIDADES"
                  title="Todas las actividades"
                  description={
                    nombreCiudadActiva
                      ? `Opciones disponibles según tu búsqueda y filtros en ${nombreCiudadActiva}.`
                      : "Opciones disponibles según tu búsqueda y filtros."
                  }
                  action={
                    <SortSelect
                      textoBuscado={textoBuscado}
                      ordenActual={ordenActual}
                      ciudadIdActual={ciudadIdActual}
                      ciudadSlugActual={ciudadSlugActual}
                      barrioIdActual={barrioIdActual}
                      deporteSlugActual={deporteSlugActual}
                      nivelActual={nivelActual}
                      modalidadActual={modalidadActual}
                    />
                  }
                  className="mb-6"
                />

                {actividades.length === 0 ? (
                  <StatusMessage
                    variant="info"
                    title="No encontramos actividades con esos filtros"
                    className="p-7 text-center"
                  >
                    <p className="mx-auto max-w-xl">
                      Probá con otra búsqueda, cambiá la zona o revisá los
                      filtros aplicados.
                    </p>
                  </StatusMessage>
                ) : (
                  <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
                    {actividades.map((actividad) => (
                      <ActivityCard key={actividad.id} actividad={actividad} />
                    ))}
                  </div>
                )}
              </SurfaceCard>

              <Pagination
                paginaActual={paginaActual}
                totalPaginas={totalPaginas}
                textoBuscado={textoBuscado}
                ordenActual={ordenActual}
                ciudadIdActual={ciudadIdActual}
                ciudadSlugActual={ciudadSlugActual}
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
