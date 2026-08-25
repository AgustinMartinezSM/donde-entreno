import type { Metadata } from "next";
import Image from "next/image";
import Link from "next/link";
import { notFound, permanentRedirect } from "next/navigation";

import type { Actividad } from "../../../types/actividad";
import type {
  ImagenPerfilPublicador,
  PerfilPublicadorPublico,
} from "../../../types/publicadorPublico";
import { Header } from "../../../components/layout/Header";
import { SeguirPublicadorButton } from "../../../components/actividad/SeguirPublicadorButton";
import { ContactButton } from "../../../components/actividad/ContactButton";
import { ErrorState } from "../../../components/feedback/ErrorState";
import { GaleriaPerfil } from "../../../components/publicadores/GaleriaPerfil";
import { NovedadesDelPublicador } from "../../../components/publicadores/NovedadesDelPublicador";
import { BotonReportar } from "../../../components/social/BotonReportar";
import { CompartirButton } from "../../../components/social/CompartirButton";
import { SocialActivityCard } from "../../../components/social/SocialActivityCard";
import { AppLinkButton } from "../../../components/ui/AppLinkButton";
import { SectionHeader } from "../../../components/ui/SectionHeader";
import { StatusMessage } from "../../../components/ui/StatusMessage";
import { construirUrlImagenBackend } from "../../../lib/backendUrl";
import { formatearTipoPublicador } from "../../../lib/formatoCatalogo";
import {
  buscarActividades,
  obtenerDestacadasDelPublicador,
} from "../../../services/actividadService";
import type {
  PreguntaActividad,
  ResumenValoraciones,
} from "../../../services/confianzaService";
import {
  obtenerFotosDelPublicador,
  obtenerImagenesPerfilPublicador,
  obtenerPerfilPublicadorPorId,
  obtenerPreguntasDelPublicador,
  obtenerValoracionesDelPublicador,
} from "../../../services/perfilPublicadorService";
import {
  obtenerNovedadesDePerfil,
  type Novedad,
} from "../../../services/novedadesService";
import {
  obtenerEventosDePerfil,
  type Evento,
} from "../../../services/eventosService";
import { EventoCard } from "../../../components/eventos/EventoCard";

/*
  Perfil público de publicador: /publicadores/[id]

  El nodo visible del grafo social: hasta ahora "seguir" apuntaba a una
  entidad sin página propia. V1 construida 100% sobre contratos que ya
  existían en el backend (listado público de perfiles, imágenes públicas
  del perfil y actividades filtradas por publicador).

  Deuda documentada en docs/social-sports-experience.md: endpoint de
  detalle individual, slug amigable y contador público de seguidores.
*/

type PerfilPublicadorPageProps = {
  params: Promise<{
    id: string;
  }>;
  /* La tab activa viaja en la URL: el perfil sigue siendo server component. */
  searchParams?: Promise<{ [clave: string]: string | string[] | undefined }>;
};

const TABS = [
  { clave: "actividades", etiqueta: "Actividades" },
  /* Fase 9: lo que organiza con fecha. Va primero: caduca. */
  { clave: "eventos", etiqueta: "Eventos" },
  /* Fase 8: el canal del publicador. Solo aparece si contó algo. */
  { clave: "novedades", etiqueta: "Novedades" },
  { clave: "fotos", etiqueta: "Fotos" },
  /* Fase 5: las valoraciones existían pero solo dentro de cada actividad. */
  { clave: "opiniones", etiqueta: "Opiniones" },
  { clave: "info", etiqueta: "Info" },
] as const;

type ClaveTab = (typeof TABS)[number]["clave"];

type FotoDelPerfil = {
  clave: string;
  url: string;
  alt: string;
  href?: string;
  imagenId?: number;
  cantidadLikes?: number | null;
  /* Fase 4 (galería social): comentarios y sección. */
  cantidadComentarios?: number | null;
  comentariosActivados?: boolean | null;
  seccion?: string | null;
};

/*
  El param acepta id numérico (links viejos) o slug (script 27).
  Devuelve null solo para basura que no puede ser ninguno de los dos.
*/
function parsearParametroPerfil(
  crudo: string
): { esId: boolean; valor: string } | null {
  const texto = crudo.trim();

  if (/^\d+$/.test(texto)) {
    const id = Number(texto);
    return Number.isInteger(id) && id > 0 ? { esId: true, valor: texto } : null;
  }

  return /^[a-z0-9-]{1,150}$/.test(texto) ? { esId: false, valor: texto } : null;
}

export async function generateMetadata({
  params,
  searchParams,
}: PerfilPublicadorPageProps): Promise<Metadata> {
  const { id: idCrudo } = await params;
  const parametro = parsearParametroPerfil(idCrudo);

  if (parametro === null) {
    return { title: "Publicador no encontrado", robots: { index: false } };
  }

  let perfil: PerfilPublicadorPublico | null = null;

  try {
    perfil = await obtenerPerfilPublicadorPorId(parametro.valor);
  } catch (error) {
    console.error("Error al generar metadata del perfil publicador:", error);

    return { title: "Perfil de publicador" };
  }

  if (!perfil) {
    return { title: "Publicador no encontrado", robots: { index: false } };
  }

  /*
    El redirect canónico va ACÁ y no solo en el page component: el
    shell se streamea antes de que el page corra, así que un redirect
    del page sale como meta refresh dentro de un 200. generateMetadata
    corre antes del primer flush y produce el 308 HTTP real. Fuera del
    try a propósito: un catch se tragaría el NEXT_REDIRECT.
  */
  if (parametro.esId && perfil.slug) {
    const parametros = (await searchParams) ?? {};
    const tab = Array.isArray(parametros.tab)
      ? parametros.tab[0]
      : parametros.tab;
    permanentRedirect(
      `/publicadores/${perfil.slug}${tab ? `?tab=${encodeURIComponent(tab)}` : ""}`
    );
  }

  const tipo = perfil.tipoPublicador
    ? formatearTipoPublicador(perfil.tipoPublicador)
    : "Publicador";
  const descripcion =
    perfil.descripcion ||
    `${tipo} en DondeEntreno. Mirá sus actividades, horarios y datos de contacto, y seguilo para no perderte sus novedades.`;

  return {
    title: `${perfil.nombre}: actividades y contacto`,
    description: descripcion,
    alternates: {
      /* La URL canónica es la del slug cuando existe (script 27). */
      canonical: `/publicadores/${perfil.slug ?? perfil.id}`,
    },
    openGraph: {
      title: `${perfil.nombre} | DondeEntreno`,
      description: descripcion,
      type: "profile",
    },
  };
}

export default async function PerfilPublicadorPage({
  params,
  searchParams,
}: PerfilPublicadorPageProps) {
  const { id: idCrudo } = await params;
  const parametros = (await searchParams) ?? {};
  const tabPedida = Array.isArray(parametros.tab)
    ? parametros.tab[0]
    : parametros.tab;
  const parametro = parsearParametroPerfil(idCrudo);

  if (parametro === null) {
    notFound();
  }

  let perfil: PerfilPublicadorPublico | null = null;
  let huboError = false;

  try {
    perfil = await obtenerPerfilPublicadorPorId(parametro.valor);
  } catch (error) {
    huboError = true;
    console.error("Error al cargar el perfil publicador:", error);
  }

  if (huboError) {
    return (
      <main className="min-h-screen text-[var(--color-text)]">
        <section className="mx-auto w-full max-w-6xl px-4 py-6">
          <Header />
          <div className="py-10">
            <ErrorState
              titulo="No pudimos cargar este perfil"
              descripcion="No pudimos conectarnos con el servidor. Probá nuevamente en unos minutos."
              mostrarBotonInicio
              mostrarBotonExplorar
            />
          </div>
        </section>
      </main>
    );
  }

  if (!perfil) {
    notFound();
  }

  /*
    Redirect canónico (script 27): entrar por id numérico con slug
    disponible manda a la URL amigable — una sola URL por perfil para
    los buscadores. Se conserva la tab pedida.
  */
  if (parametro.esId && perfil.slug) {
    permanentRedirect(
      `/publicadores/${perfil.slug}${tabPedida ? `?tab=${encodeURIComponent(tabPedida)}` : ""}`
    );
  }

  /* Imágenes y actividades: best-effort, el perfil se muestra igual. */
  const [imagenes, respuestaActividades, fotosDelPublicador, destacadas] =
    await Promise.all([
      obtenerImagenesPerfilPublicador(perfil.id).catch(
        () => [] as ImagenPerfilPublicador[]
      ),
      buscarActividades({
        perfilPublicadorId: perfil.id,
        page: 0,
        size: 6,
      }).catch(() => null),
      /* Fase 5: todas sus fotos en UN request (antes, una por actividad). */
      obtenerFotosDelPublicador(perfil.id).catch(
        () => [] as ImagenPerfilPublicador[]
      ),
      obtenerDestacadasDelPublicador(perfil.id).catch(() => [] as Actividad[]),
    ]);

  const logo = imagenes.find((imagen) => imagen.tipoImagen === "LOGO");
  const portada = imagenes.find((imagen) => imagen.tipoImagen === "PORTADA");
  const logoUrl = construirUrlImagenBackend(logo?.url);
  const portadaUrl = construirUrlImagenBackend(portada?.url);

  const actividades: Actividad[] = respuestaActividades?.contenido ?? [];
  const totalActividades = respuestaActividades?.totalElementos ?? 0;
  const huboErrorActividades = respuestaActividades === null;

  const fotos = reunirFotosDelPerfil(perfil.nombre, fotosDelPublicador);

  /*
    Opiniones y preguntas del publicador (Fase 5): hasta ahora las
    valoraciones vivían solo dentro de cada actividad, así que quien
    miraba el perfil no veía ninguna.
  */
  const [opiniones, preguntas, novedades, eventos] = await Promise.all([
    obtenerValoracionesDelPublicador(perfil.id).catch(() => null),
    obtenerPreguntasDelPublicador(perfil.id).catch(() => []),
    /* Fase 8: el canal. Si falla, el perfil se muestra igual sin él. */
    obtenerNovedadesDePerfil(perfil.id).catch(() => [] as Novedad[]),
    /* Fase 9: lo que organiza. Ídem: best-effort. */
    obtenerEventosDePerfil(perfil.id).catch(() => [] as Evento[]),
  ]);
  const hayOpiniones =
    (opiniones?.contenido?.length ?? 0) > 0 || preguntas.length > 0;

  const tabActiva: ClaveTab = resolverTab(
    tabPedida,
    fotos.length > 0,
    hayOpiniones,
    novedades.length > 0,
    eventos.length > 0
  );

  const tipoVisible = perfil.tipoPublicador
    ? formatearTipoPublicador(perfil.tipoPublicador)
    : "Publicador de la comunidad";
  const iniciales = obtenerIniciales(perfil.nombre);
  /* Campo aditivo: un backend viejo no lo manda y queda en cero. */
  const seguidores = Math.max(0, perfil.cantidadSeguidores ?? 0);
  const hrefExplorar = `/explorar?perfilPublicadorId=${perfil.id}`;
  const sitioWebUrl = normalizarSitioWeb(perfil.sitioWeb);

  return (
    <main className="min-h-screen text-[var(--color-text)]">
      <section className="mx-auto w-full max-w-6xl px-4 py-6">
        <Header />

        <div className="py-7 sm:py-9">
          {/* Encabezado del perfil: portada + identidad */}
          <article className="overflow-hidden rounded-[24px] border border-[var(--color-border)] bg-[var(--color-surface)] shadow-[0_12px_35px_rgba(15,61,94,0.08)]">
            {/*
              La portada solo reserva altura cuando hay una imagen que
              mostrar. Sin imagen alcanza una banda fina de color: una
              franja alta de degradado dejaba el perfil arrancando con
              200px vacíos.
            */}
            <div
              className={`relative ${
                portadaUrl ? "h-40 sm:h-56" : "h-20 sm:h-24"
              }`}
            >
              {portadaUrl ? (
                <>
                  <Image
                    src={portadaUrl}
                    alt=""
                    fill
                    priority
                    sizes="(max-width: 1024px) 100vw, 1100px"
                    className="object-cover"
                  />
                  <div
                    aria-hidden="true"
                    className="absolute inset-0 bg-gradient-to-t from-[#0F3D5E]/55 via-transparent to-transparent"
                  />
                </>
              ) : (
                <div
                  aria-hidden="true"
                  className="absolute inset-0 bg-gradient-to-br from-[var(--color-primary)] via-[#145276] to-[var(--color-secondary)]"
                />
              )}
            </div>

            <div className="px-5 pb-6 sm:px-8 sm:pb-8">
              {/*
                Identidad y acciones. En desktop comparten una fila, con el
                avatar montado sobre la portada y el nombre a su lado; en
                mobile las acciones bajan a su propia fila, porque cuando
                convivían con el avatar se superponían (a 320px "Seguir"
                quedaba flotando sobre la portada).
              */}
              <div className="flex flex-col gap-5 sm:flex-row sm:items-end sm:justify-between sm:gap-6">
                <div className="flex min-w-0 items-end gap-4">
                  {logoUrl ? (
                    <span className="relative z-10 -mt-12 h-20 w-20 shrink-0 overflow-hidden rounded-full bg-[var(--color-surface)] ring-4 ring-white shadow-[0_10px_24px_rgba(15,61,94,0.18)] sm:-mt-16 sm:h-28 sm:w-28">
                      <Image
                        src={logoUrl}
                        alt={`Logo de ${perfil.nombre}`}
                        fill
                        sizes="112px"
                        className="object-cover"
                      />
                    </span>
                  ) : (
                    /*
                      relative + z-10: la portada de arriba es un
                      elemento posicionado, así que pintaba por encima
                      del avatar estático y le cortaba la mitad de
                      arriba.
                    */
                    <span
                      aria-hidden="true"
                      className="relative z-10 -mt-12 flex h-20 w-20 shrink-0 items-center justify-center rounded-full bg-[var(--color-brand)] text-xl font-extrabold tracking-[0.08em] text-white ring-4 ring-white shadow-[0_10px_24px_rgba(15,61,94,0.18)] sm:-mt-16 sm:h-28 sm:w-28 sm:text-2xl"
                    >
                      {iniciales}
                    </span>
                  )}

                  <div className="min-w-0 pb-1">
                    <div className="flex flex-wrap items-center gap-2">
                      <h1 className="text-2xl font-extrabold leading-tight text-[var(--color-primary)] sm:text-3xl">
                        {perfil.nombre}
                      </h1>
                      {perfil.verificado === true ? (
                        <span
                          role="img"
                          aria-label="Publicador verificado"
                          title="Publicador verificado"
                          className="inline-flex h-6 w-6 shrink-0 items-center justify-center rounded-full bg-[var(--color-secondary)] text-white"
                        >
                          <svg
                            viewBox="0 0 20 20"
                            fill="none"
                            stroke="currentColor"
                            strokeWidth="2.2"
                            strokeLinecap="round"
                            strokeLinejoin="round"
                            className="h-3.5 w-3.5"
                            aria-hidden="true"
                          >
                            <path d="m5.5 10 3 3 6-6" />
                          </svg>
                        </span>
                      ) : null}
                    </div>

                    <p className="mt-1 text-sm font-bold uppercase tracking-[0.14em] text-[var(--color-secondary)]">
                      {tipoVisible}
                    </p>
                  </div>
                </div>

                {/*
                  Seguir es un botón "md" y Compartir uno "sm": quedaban
                  escalonados (46px contra 40px). items-stretch los lleva a
                  los dos a la altura del más alto; min-height solo no
                  alcanzaba porque el contenido de Seguir la supera.
                */}
                <div className="grid grid-cols-2 items-stretch gap-2 sm:flex sm:shrink-0 sm:items-stretch sm:gap-3 [&>button]:min-h-11 [&>button]:w-full sm:[&>button]:w-auto">
                  <SeguirPublicadorButton
                    perfilPublicadorId={perfil.id}
                    perfilPublicadorNombre={perfil.nombre}
                  />
                  <CompartirButton
                    ruta={`/publicadores/${perfil.slug ?? perfil.id}`}
                    titulo={perfil.nombre}
                  />
                  {/* Reportar (Fase 2 social): moderación flexible. */}
                  <BotonReportar
                    tipoObjeto="PERFIL_PUBLICADOR"
                    objetoId={perfil.id}
                    etiquetaObjeto="este perfil"
                    compacto
                  />
                </div>
              </div>

              {perfil.descripcion ? (
                <p className="mt-5 max-w-3xl text-sm leading-7 text-[var(--color-muted)] sm:text-base">
                  {perfil.descripcion}
                </p>
              ) : null}

              {/*
                Fila de stats (Fase 5): los números que hacen decidir,
                cada uno navegando a donde se ven. Un stat sin dato no
                se dibuja — nunca un cero falso, la misma regla que ya
                regía para seguidores.
              */}
              <StatsDelPerfil
                slugOId={perfil.slug ?? String(perfil.id)}
                actividades={huboErrorActividades ? null : totalActividades}
                seguidores={seguidores}
                fotos={fotos.length}
                promedio={perfil.valoracionPromedio ?? null}
                cantidadOpiniones={perfil.cantidadValoraciones ?? 0}
              />

              {perfil.verificado === true ? (
                <div className="mt-4">
                  <span className="rounded-full bg-[var(--color-success-soft)] px-3 py-1.5 text-xs font-extrabold text-[var(--color-success)]">
                    Perfil verificado
                  </span>
                </div>
              ) : null}

              <div className="mt-5 flex flex-col gap-3 sm:flex-row sm:items-center">
                <div className="sm:w-64">
                  <ContactButton
                    whatsapp={perfil.whatsapp}
                    instagram={perfil.instagram}
                    email={perfil.emailContacto}
                    /* Fase 5: hasta acá el botón del perfil no medía nada. */
                    perfilPublicadorId={perfil.id}
                    nombrePublicador={perfil.nombre}
                    className=""
                  />
                </div>
                {sitioWebUrl ? (
                  <a
                    href={sitioWebUrl}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="inline-flex min-h-11 items-center justify-center rounded-[18px] border border-[var(--color-border-accent)] bg-[var(--color-surface)] px-4 py-2 text-sm font-bold text-[var(--color-primary)] shadow-sm transition duration-200 ease-out hover:-translate-y-0.5 hover:border-[var(--color-primary)] hover:bg-[var(--color-surface-soft)]"
                  >
                    Sitio web ↗
                  </a>
                ) : null}
              </div>
            </div>
          </article>

          {/*
            Tabs por URL: mantienen el perfil como server component y hacen
            que cada solapa sea enlazable y compartible.
          */}
          <nav
            className="mt-8 flex gap-2 overflow-x-auto border-b border-[var(--color-border)] pb-px"
            aria-label="Secciones del perfil"
          >
            {TABS.filter(
              (tab) =>
                (tab.clave !== "fotos" || fotos.length > 0) &&
                (tab.clave !== "opiniones" || hayOpiniones) &&
                (tab.clave !== "novedades" || novedades.length > 0) &&
                (tab.clave !== "eventos" || eventos.length > 0)
            ).map((tab) => {
              const activa = tab.clave === tabActiva;

              return (
                <Link
                  key={tab.clave}
                  href={`/publicadores/${perfil.slug ?? perfil.id}?tab=${tab.clave}`}
                  scroll={false}
                  aria-current={activa ? "page" : undefined}
                  className={`-mb-px shrink-0 border-b-2 px-4 py-3 text-sm font-extrabold transition duration-200 ease-out ${
                    activa
                      ? "border-[var(--color-secondary)] text-[var(--color-primary)]"
                      : "border-transparent text-[var(--color-muted)] hover:border-[var(--color-border-accent)] hover:text-[var(--color-primary)]"
                  }`}
                >
                  {tab.etiqueta}
                  {tab.clave === "fotos" ? (
                    <span className="ml-1.5 font-bold text-[var(--color-muted)]">
                      {fotos.length}
                    </span>
                  ) : null}
                </Link>
              );
            })}
          </nav>

          {tabActiva === "actividades" ? (
            <section className="mt-7" aria-labelledby="actividades-perfil-titulo">
              <SectionHeader
                eyebrow="Actividades"
                title={`Lo que publica ${perfil.nombre}`}
                description={
                  totalActividades > 0
                    ? `${totalActividades} ${
                        totalActividades === 1
                          ? "actividad publicada"
                          : "actividades publicadas"
                      } en la plataforma.`
                    : undefined
                }
                titleId="actividades-perfil-titulo"
                action={
                  totalActividades > 6 ? (
                    <AppLinkButton
                      href={hrefExplorar}
                      variant="secondary"
                      size="sm"
                    >
                      Ver todas
                    </AppLinkButton>
                  ) : undefined
                }
              />

              {huboErrorActividades ? (
                <StatusMessage variant="warning" className="mt-5">
                  No pudimos cargar las actividades de este publicador. Probá de
                  nuevo en unos minutos.
                </StatusMessage>
              ) : actividades.length === 0 ? (
                <StatusMessage variant="info" className="mt-5">
                  Este publicador todavía no tiene actividades publicadas.
                  Seguilo para enterarte cuando publique.
                </StatusMessage>
              ) : (
                <>
                  {/*
                    Destacadas (Fase 5): lo que el publicador eligió
                    mostrar primero. Van ARRIBA del listado normal, no
                    en su lugar.
                  */}
                  {destacadas.length > 0 ? (
                    <div className="mt-5">
                      <p className="text-xs font-extrabold uppercase tracking-[0.16em] text-[var(--color-secondary)]">
                        Destacadas por {perfil.nombre}
                      </p>
                      <div className="mt-3 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
                        {destacadas.map((actividad) => (
                          <SocialActivityCard
                            key={`destacada-${actividad.id}`}
                            actividad={actividad}
                            variante="compacta"
                          />
                        ))}
                      </div>
                    </div>
                  ) : null}

                  <div className="mt-5 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
                    {actividades
                      .filter(
                        (actividad) =>
                          !destacadas.some(
                            (destacada) => destacada.id === actividad.id
                          )
                      )
                      .map((actividad) => (
                        <SocialActivityCard
                          key={actividad.id}
                          actividad={actividad}
                          variante="compacta"
                        />
                      ))}
                  </div>
                </>
              )}
            </section>
          ) : null}

          {tabActiva === "eventos" ? (
            <section className="mt-7" aria-labelledby="eventos-perfil-titulo">
              <SectionHeader
                eyebrow="Eventos"
                title={`Lo que organiza ${perfil.nombre}`}
                description="Torneos, clases abiertas y seminarios con fecha confirmada."
                titleId="eventos-perfil-titulo"
              />

              <ul className="mt-5 grid grid-cols-1 gap-4 sm:grid-cols-2">
                {eventos.map((evento) => (
                  <li key={evento.id}>
                    <EventoCard evento={evento} />
                  </li>
                ))}
              </ul>
            </section>
          ) : null}

          {tabActiva === "novedades" ? (
            <section className="mt-7" aria-labelledby="novedades-perfil-titulo">
              <SectionHeader
                eyebrow="Novedades"
                title={`Lo último de ${perfil.nombre}`}
                description="Cambios de horario, cupos que se liberan, cómo salió el torneo. Lo cuenta acá, sin esperar a publicar una actividad nueva."
                titleId="novedades-perfil-titulo"
              />

              <NovedadesDelPublicador novedades={novedades} />
            </section>
          ) : null}

          {tabActiva === "opiniones" ? (
            <section className="mt-7" aria-labelledby="opiniones-perfil-titulo">
              <SectionHeader
                eyebrow="Opiniones"
                title={`Lo que dicen de ${perfil.nombre}`}
                description="Valoraciones de todas sus actividades y las preguntas que ya respondió."
                titleId="opiniones-perfil-titulo"
              />

              <OpinionesDelPublicador
                resumen={opiniones}
                preguntas={preguntas}
                promedioVisible={perfil.valoracionPromedio ?? null}
              />
            </section>
          ) : null}

          {tabActiva === "fotos" ? (
            <section className="mt-7" aria-labelledby="fotos-perfil-titulo">
              <SectionHeader
                eyebrow="Fotos"
                title="Fotos reales"
                description="Imágenes reales de sus actividades y su espacio."
                titleId="fotos-perfil-titulo"
              />

              {/*
                Client component con visor a pantalla completa: tocar
                una foto la muestra; el link a la actividad vive dentro
                del visor (fase 4).
              */}
              <GaleriaPerfil fotos={fotos} />
            </section>
          ) : null}

          {tabActiva === "info" ? (
            <section className="mt-7" aria-labelledby="info-perfil-titulo">
              <SectionHeader
                eyebrow="Info"
                title="Sobre este publicador"
                titleId="info-perfil-titulo"
              />

              <dl className="mt-5 grid gap-3 sm:grid-cols-2">
                <DatoDelPerfil termino="Tipo" valor={tipoVisible} />
                <DatoDelPerfil
                  termino="Actividades publicadas"
                  valor={
                    huboErrorActividades ? null : String(totalActividades)
                  }
                />
                <DatoDelPerfil
                  termino="Seguidores"
                  valor={seguidores > 0 ? String(seguidores) : "Todavía ninguno"}
                />
                <DatoDelPerfil
                  termino="Verificado"
                  valor={perfil.verificado === true ? "Sí" : "Todavía no"}
                />
                <DatoDelPerfil termino="WhatsApp" valor={perfil.whatsapp} />
                <DatoDelPerfil termino="Instagram" valor={perfil.instagram} />
                <DatoDelPerfil termino="Email" valor={perfil.emailContacto} />
                <DatoDelPerfil
                  termino="Sitio web"
                  valor={perfil.sitioWeb}
                  href={sitioWebUrl}
                />
              </dl>

              {perfil.descripcion ? (
                <div className="mt-5 rounded-[var(--radius-lg)] border border-[var(--color-border-soft)] bg-[var(--color-surface)] p-5">
                  <p className="text-sm font-extrabold text-[var(--color-primary)]">
                    Descripción
                  </p>
                  <p className="mt-2 text-sm leading-7 text-[var(--color-muted)] sm:text-base">
                    {perfil.descripcion}
                  </p>
                </div>
              ) : null}
            </section>
          ) : null}
        </div>
      </section>
    </main>
  );
}

function resolverTab(
  pedida: string | undefined,
  hayFotos: boolean,
  hayOpiniones: boolean,
  hayNovedades: boolean,
  hayEventos: boolean
): ClaveTab {
  const valida = TABS.some((tab) => tab.clave === pedida);

  if (!valida) {
    return "actividades";
  }

  /* Sin fotos la solapa no se muestra: entrar por URL cae en actividades. */
  if (pedida === "fotos" && !hayFotos) {
    return "actividades";
  }

  /* Ídem el canal: una solapa vacía es una promesa rota. */
  if (pedida === "novedades" && !hayNovedades) {
    return "actividades";
  }

  if (pedida === "eventos" && !hayEventos) {
    return "actividades";
  }

  /* Mismo criterio para Opiniones: una solapa vacía es una promesa rota. */
  if (pedida === "opiniones" && !hayOpiniones) {
    return "actividades";
  }

  return pedida as ClaveTab;
}

/*
  Junta las fotos visibles del publicador: la galería propia del perfil
  más las imágenes aprobadas de cada una de sus actividades.

  Desde la Fase 5 vienen TODAS en un solo request
  (GET /api/perfiles-publicadores/{id}/fotos): antes se pedía una
  llamada por actividad, hasta 6 por vista. El backend ya filtra LOGO
  y PORTADA, que se ven en la cabecera.
*/
function reunirFotosDelPerfil(
  nombrePerfil: string,
  fotosDelBackend: ImagenPerfilPublicador[]
): FotoDelPerfil[] {
  const fotos: FotoDelPerfil[] = [];

  for (const imagen of fotosDelBackend) {
    const url = construirUrlImagenBackend(imagen.url);

    if (!url) {
      continue;
    }

    fotos.push({
      clave: `foto-${imagen.id}`,
      url,
      alt:
        imagen.descripcion?.trim() ||
        imagen.titulo?.trim() ||
        `Foto de ${nombrePerfil}`,
      /* La foto de una actividad linkea a su actividad desde el visor. */
      href: imagen.actividadSlug
        ? `/actividades/${imagen.actividadSlug}`
        : undefined,
      imagenId: imagen.id,
      cantidadLikes: imagen.cantidadLikes ?? null,
      cantidadComentarios: imagen.cantidadComentarios ?? null,
      comentariosActivados: imagen.comentariosActivados ?? null,
      seccion: imagen.seccion ?? null,
    });
  }

  return fotos;
}

/*
  Fila de stats de la cabecera (Fase 5). Cada número navega a donde se
  ve lo que cuenta. Los que no tienen dato NO se dibujan: la regla que
  ya regía para seguidores ("nunca un cero falso") vale para todos —
  con la plataforma recién arrancando, cuatro ceros en fila hacen ver
  cada perfil abandonado.
*/
function StatsDelPerfil({
  slugOId,
  actividades,
  seguidores,
  fotos,
  promedio,
  cantidadOpiniones,
}: {
  slugOId: string;
  actividades: number | null;
  seguidores: number;
  fotos: number;
  promedio: number | null;
  cantidadOpiniones: number;
}) {
  const items: { clave: string; valor: string; etiqueta: string; href?: string }[] =
    [];

  if (actividades !== null && actividades > 0) {
    items.push({
      clave: "actividades",
      valor: String(actividades),
      etiqueta: actividades === 1 ? "actividad" : "actividades",
      href: `/publicadores/${slugOId}?tab=actividades`,
    });
  }

  if (seguidores > 0) {
    items.push({
      clave: "seguidores",
      valor: String(seguidores),
      etiqueta: seguidores === 1 ? "seguidor" : "seguidores",
    });
  }

  if (fotos > 0) {
    items.push({
      clave: "fotos",
      valor: String(fotos),
      etiqueta: fotos === 1 ? "foto" : "fotos",
      href: `/publicadores/${slugOId}?tab=fotos`,
    });
  }

  /*
    El promedio viaja null hasta las 3 valoraciones (regla del backend).
    Mientras tanto se muestra la CANTIDAD, que sí es un dato honesto.
  */
  if (promedio !== null) {
    items.push({
      clave: "promedio",
      valor: `★ ${promedio.toFixed(1)}`,
      etiqueta: cantidadOpiniones === 1 ? "1 opinión" : `${cantidadOpiniones} opiniones`,
      href: `/publicadores/${slugOId}?tab=opiniones`,
    });
  } else if (cantidadOpiniones > 0) {
    items.push({
      clave: "opiniones",
      valor: String(cantidadOpiniones),
      etiqueta: cantidadOpiniones === 1 ? "opinión" : "opiniones",
      href: `/publicadores/${slugOId}?tab=opiniones`,
    });
  }

  if (items.length === 0) {
    return null;
  }

  return (
    <dl className="mt-5 flex flex-wrap items-center gap-x-6 gap-y-3">
      {items.map((item) => {
        const contenido = (
          <>
            <dt className="sr-only">{item.etiqueta}</dt>
            <dd className="text-lg font-extrabold leading-tight text-[var(--color-primary)]">
              {item.valor}{" "}
              <span className="text-xs font-bold text-[var(--color-muted)]">
                {item.etiqueta}
              </span>
            </dd>
          </>
        );

        return item.href ? (
          <Link
            key={item.clave}
            href={item.href}
            scroll={false}
            className="rounded-[10px] transition duration-200 ease-out hover:opacity-80 focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-[var(--color-border-soft)]"
          >
            {contenido}
          </Link>
        ) : (
          <div key={item.clave}>{contenido}</div>
        );
      })}
    </dl>
  );
}

/*
  Tab Opiniones (Fase 5): las valoraciones de TODAS sus actividades y
  las preguntas que ya respondió. Cada una linkea a su actividad,
  porque acá se mezclan varias.
*/
function OpinionesDelPublicador({
  resumen,
  preguntas,
  promedioVisible,
}: {
  resumen: ResumenValoraciones | null;
  preguntas: PreguntaActividad[];
  promedioVisible: number | null;
}) {
  const valoraciones = resumen?.contenido ?? [];

  return (
    <div className="mt-5 grid gap-6">
      {valoraciones.length > 0 ? (
        <div>
          {promedioVisible !== null ? (
            <p className="text-sm font-bold text-[var(--color-primary)]">
              ★ {promedioVisible.toFixed(1)} de 5 ·{" "}
              {resumen?.cantidad === 1
                ? "1 opinión"
                : `${resumen?.cantidad ?? 0} opiniones`}
            </p>
          ) : (
            <p className="text-sm text-[var(--color-muted)]">
              Todavía no hay suficientes opiniones para un promedio.
            </p>
          )}

          <ul className="mt-4 grid gap-3">
            {valoraciones.map((valoracion) => (
              <li
                key={valoracion.id}
                className="rounded-[var(--radius-md)] border border-[var(--color-border-soft)] bg-[var(--color-surface)] p-4"
              >
                <div className="flex flex-wrap items-center gap-x-3 gap-y-1">
                  <span className="text-sm font-extrabold text-[#F0B429]">
                    {"★".repeat(valoracion.puntaje)}
                    <span className="text-[var(--color-border-accent)]">
                      {"★".repeat(5 - valoracion.puntaje)}
                    </span>
                  </span>
                  <span className="text-sm font-bold text-[var(--color-primary)]">
                    {valoracion.autorNombre}
                  </span>
                  {valoracion.verificada ? (
                    <span className="rounded-full bg-[var(--color-success-soft)] px-2 py-0.5 text-[11px] font-extrabold text-[var(--color-success)]">
                      Entrenó acá
                    </span>
                  ) : null}
                </div>

                {valoracion.comentario ? (
                  <p className="mt-2 text-sm leading-6 text-[var(--color-text)]">
                    {valoracion.comentario}
                  </p>
                ) : null}

                {valoracion.actividadSlug && valoracion.actividadTitulo ? (
                  <Link
                    href={`/actividades/${valoracion.actividadSlug}`}
                    className="mt-2 inline-block text-xs font-extrabold text-[var(--color-primary)] underline decoration-[var(--color-border-accent)] underline-offset-4 transition hover:decoration-[var(--color-primary)]"
                  >
                    Sobre {valoracion.actividadTitulo}
                  </Link>
                ) : null}
              </li>
            ))}
          </ul>
        </div>
      ) : null}

      {preguntas.length > 0 ? (
        <div>
          <h3 className="text-sm font-extrabold uppercase tracking-[0.16em] text-[var(--color-secondary)]">
            Preguntas que ya respondió
          </h3>

          <ul className="mt-3 grid gap-3">
            {preguntas.map((pregunta) => (
              <li
                key={pregunta.id}
                className="rounded-[var(--radius-md)] border border-[var(--color-border-soft)] bg-[var(--color-surface)] p-4"
              >
                <p className="text-sm font-bold leading-6 text-[var(--color-primary)]">
                  {pregunta.pregunta}
                </p>
                <p className="mt-1.5 text-sm leading-6 text-[var(--color-text)]">
                  {pregunta.respuesta}
                </p>

                {pregunta.actividadSlug && pregunta.actividadTitulo ? (
                  <Link
                    href={`/actividades/${pregunta.actividadSlug}`}
                    className="mt-2 inline-block text-xs font-extrabold text-[var(--color-primary)] underline decoration-[var(--color-border-accent)] underline-offset-4 transition hover:decoration-[var(--color-primary)]"
                  >
                    Sobre {pregunta.actividadTitulo}
                  </Link>
                ) : null}
              </li>
            ))}
          </ul>
        </div>
      ) : null}
    </div>
  );
}

function DatoDelPerfil({
  termino,
  valor,
  href,
}: {
  termino: string;
  valor?: string | null;
  href?: string | null;
}) {
  const limpio = valor?.trim();

  if (!limpio) {
    return null;
  }

  return (
    <div className="rounded-[var(--radius-md)] border border-[var(--color-border-soft)] bg-[var(--color-surface)] p-4">
      <dt className="text-xs font-extrabold uppercase tracking-[0.12em] text-[var(--color-muted)]">
        {termino}
      </dt>
      <dd className="mt-1 text-sm font-bold text-[var(--color-primary)]">
        {href ? (
          <a
            href={href}
            target="_blank"
            rel="noopener noreferrer"
            className="underline decoration-[var(--color-border-accent)] underline-offset-4 hover:decoration-[var(--color-primary)]"
          >
            {limpio}
          </a>
        ) : (
          limpio
        )}
      </dd>
    </div>
  );
}

function obtenerIniciales(nombre: string) {
  return nombre
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((parte) => parte.charAt(0).toLocaleUpperCase("es"))
    .join("");
}

function normalizarSitioWeb(valor?: string | null): string | null {
  const limpio = valor?.trim();

  if (!limpio) {
    return null;
  }

  return limpio.startsWith("http") ? limpio : `https://${limpio}`;
}
