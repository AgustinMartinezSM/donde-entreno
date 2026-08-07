import type { Metadata } from "next";
import type { Actividad } from "../../types/actividad";
import type { FiltrosOpciones } from "../../types/filtros";

import { Header } from "../../components/layout/Header";
import { SocialActivityCard } from "../../components/social/SocialActivityCard";
import { buscarActividades } from "../../services/actividadService";
import { obtenerCiudadPorSlug } from "../../services/ciudadService";
import { SearchBar } from "../../components/home/SearchBar";
import { Pagination } from "../../components/explorar/Pagination";
import { SortSelect } from "../../components/explorar/SortSelect";
import { FiltersPanel } from "../../components/explorar/FiltersPanel";
import { obtenerOpcionesFiltros } from "../../services/filtrosService";
import { AppLinkButton } from "../../components/ui/AppLinkButton";
import { ErrorState } from "../../components/feedback/ErrorState";
import { IlustracionSinResultados } from "../../components/illustrations/IlustracionSinResultados";
import { obtenerSugerenciaDeporte } from "../../lib/sugerenciaDeporte";
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
    perfilPublicadorId?: string;
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
  /*
    Saneamos page: un valor no numérico (?page=abc) mandaba NaN al
    backend y tiraba toda la página al estado de error.
  */
  const paginaParseada = Number(params.page);
  const paginaActual =
    Number.isFinite(paginaParseada) && paginaParseada > 0
      ? Math.floor(paginaParseada)
      : 0;
  const ordenActual = params.orden || "";

  const ciudadIdActual = params.ciudadId || "";
  const ciudadSlugActual = params.ciudadSlug?.trim() || "";
  const barrioIdActual = params.barrioId || "";
  const perfilPublicadorIdActual = params.perfilPublicadorId || "";
  const deporteSlugActual = params.deporteSlug || "";
  const nivelActual = params.nivel || "";
  const modalidadActual = params.modalidad || "";
  const ciudadIdParaBusqueda = ciudadSlugActual
    ? undefined
    : ciudadIdActual
      ? Number(ciudadIdActual)
      : undefined;

  /*
    Búsqueda inteligente: si el texto libre matchea un deporte real
    ("jiujitsu", "bjj", "gym"), sugerimos ir directo al filtro por
    deporte. No se sugiere si ya se está filtrando por ese deporte.
  */
  const sugerenciaDeporte =
    textoBuscado && !deporteSlugActual
      ? obtenerSugerenciaDeporte(textoBuscado)
      : null;
  const paramsSugerencia = new URLSearchParams();
  if (sugerenciaDeporte) {
    paramsSugerencia.set("deporteSlug", sugerenciaDeporte.slug);
    if (ciudadSlugActual) {
      paramsSugerencia.set("ciudadSlug", ciudadSlugActual);
    }
  }
  const hrefSugerencia = sugerenciaDeporte
    ? `/explorar?${paramsSugerencia.toString()}`
    : null;

  let actividades: Actividad[] = [];
  let totalPaginas = 0;
  let totalElementos = 0;
  let huboError = false;
  let huboErrorCiudad = false;
  let nombreCiudadActiva: string | null = null;
  let nombrePublicadorFiltrado: string | null = null;

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
          perfilPublicadorId: perfilPublicadorIdActual
            ? Number(perfilPublicadorIdActual)
            : undefined,
          deporteSlug: deporteSlugActual || undefined,
          nivel: nivelActual || undefined,
          modalidad: modalidadActual || undefined,
        }),
        obtenerOpcionesFiltros(),
      ]);

      actividades = respuestaActividades.contenido;
      totalPaginas = respuestaActividades.totalPaginas;
      totalElementos = respuestaActividades.totalElementos;
      filtros = respuestaFiltros;

      /*
        Si se está filtrando por publicador, el nombre sale de los propios
        resultados (el DTO lo trae) para mostrar el chip de contexto.
      */
      if (perfilPublicadorIdActual) {
        nombrePublicadorFiltrado =
          actividades[0]?.perfilPublicadorNombre ?? null;
      }
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
                ? `Encontrá actividades para entrenar en ${nombreCiudadActiva} y ajustá la búsqueda por deporte, barrio, nivel o modalidad.`
                : "Buscá por deporte, zona, nivel o modalidad y descubrí opciones para moverte cerca tuyo."}
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

              {perfilPublicadorIdActual ? (
                <p className="inline-flex items-center gap-2 rounded-full bg-[#E8F6FB] px-3 py-2 text-sm font-bold text-[#0F6F8F]">
                  Actividades de{" "}
                  {nombrePublicadorFiltrado ?? "un publicador que seguís"}
                  <AppLinkButton
                    href={
                      ciudadSlugActual
                        ? `/explorar?ciudadSlug=${encodeURIComponent(ciudadSlugActual)}`
                        : "/explorar"
                    }
                    variant="secondary"
                    size="sm"
                    className="min-h-7 rounded-full !px-2 !py-0.5 text-[11px]"
                  >
                    Quitar ✕
                  </AppLinkButton>
                </p>
              ) : null}

              {sugerenciaDeporte && hrefSugerencia ? (
                <AppLinkButton
                  href={hrefSugerencia}
                  variant="success"
                  size="sm"
                  className="rounded-full"
                >
                  ¿Buscabas {sugerenciaDeporte.nombre}? Ver todas sus actividades
                </AppLinkButton>
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

            {/* Dejamos el buscador visible aunque haya error. Es el mismo
                SearchBar de la home (con sugerencias de deportes) y
                preserva la ciudad activa por sí solo. */}
            <div className="max-w-3xl transition duration-200 ease-out">
              <SearchBar
                valorInicial={textoBuscado}
                ciudadSlugActual={ciudadSlugActual}
              />
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
                  descripcion="No pudimos cargar esta información. Probá nuevamente en unos segundos o volvé al inicio."
                  mostrarBotonInicio
                  mostrarBotonExplorar
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
                  perfilPublicadorIdActual={perfilPublicadorIdActual}
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
                    totalElementos > 0
                      ? `${totalElementos} ${
                          totalElementos === 1
                            ? "actividad encontrada"
                            : "actividades encontradas"
                        }${
                          nombreCiudadActiva ? ` en ${nombreCiudadActiva}` : ""
                        } según tu búsqueda y filtros.`
                      : nombreCiudadActiva
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
                      perfilPublicadorIdActual={perfilPublicadorIdActual}
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
                    title={
                      nombreCiudadActiva
                        ? `No encontramos actividades con esos filtros en ${nombreCiudadActiva}`
                        : "No encontramos actividades con esos filtros"
                    }
                    className="p-7 text-center"
                  >
                    <IlustracionSinResultados />

                    <p className="mx-auto max-w-xl">
                      Probá con otra búsqueda, cambiá la zona o abrí los
                      filtros para ampliar opciones.
                    </p>

                    {sugerenciaDeporte && hrefSugerencia ? (
                      <div className="mt-5">
                        <AppLinkButton href={hrefSugerencia} variant="success">
                          ¿Buscabas {sugerenciaDeporte.nombre}? Ver sus actividades
                        </AppLinkButton>
                      </div>
                    ) : null}

                    {filtros.deportes.length > 0 && (
                      <div className="mt-5">
                        <p className="text-sm font-bold text-[var(--color-primary)]">
                          O explorá directamente por deporte:
                        </p>
                        <div className="mt-3 flex flex-wrap justify-center gap-2">
                          {filtros.deportes.slice(0, 8).map((deporte) => {
                            const paramsDeporte = new URLSearchParams();
                            if (deporte.slug) {
                              paramsDeporte.set("deporteSlug", deporte.slug);
                            }
                            if (ciudadSlugActual) {
                              paramsDeporte.set("ciudadSlug", ciudadSlugActual);
                            }

                            return (
                              <AppLinkButton
                                key={deporte.id}
                                href={`/explorar?${paramsDeporte.toString()}`}
                                variant="secondary"
                                size="sm"
                                className="rounded-full"
                              >
                                {deporte.nombre}
                              </AppLinkButton>
                            );
                          })}
                        </div>
                      </div>
                    )}

                    <div className="mt-5 flex flex-col gap-3 sm:flex-row sm:justify-center">
                      <AppLinkButton
                        href="/ciudades"
                        variant="secondary"
                        size="sm"
                      >
                        Cambiar ciudad
                      </AppLinkButton>
                      <AppLinkButton
                        href={
                          ciudadSlugActual
                            ? `/explorar?ciudadSlug=${encodeURIComponent(
                                ciudadSlugActual
                              )}`
                            : "/explorar"
                        }
                        variant="primary"
                        size="sm"
                      >
                        {nombreCiudadActiva
                          ? `Ver todas en ${nombreCiudadActiva}`
                          : "Limpiar filtros"}
                      </AppLinkButton>
                    </div>
                  </StatusMessage>
                ) : (
                  <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
                    {actividades.map((actividad) => (
                      <SocialActivityCard
                        key={actividad.id}
                        actividad={actividad}
                        variante="compacta"
                      />
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
                perfilPublicadorIdActual={perfilPublicadorIdActual}
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
