import type { Metadata } from "next";
import Image from "next/image";
import Link from "next/link";
import { notFound } from "next/navigation";

import type { Actividad } from "../../../types/actividad";
import type {
  ImagenPerfilPublicador,
  PerfilPublicadorPublico,
} from "../../../types/publicadorPublico";
import { Header } from "../../../components/layout/Header";
import { SeguirPublicadorButton } from "../../../components/actividad/SeguirPublicadorButton";
import { ContactButton } from "../../../components/actividad/ContactButton";
import { ErrorState } from "../../../components/feedback/ErrorState";
import { CompartirButton } from "../../../components/social/CompartirButton";
import { SocialActivityCard } from "../../../components/social/SocialActivityCard";
import { AppLinkButton } from "../../../components/ui/AppLinkButton";
import { SectionHeader } from "../../../components/ui/SectionHeader";
import { StatusMessage } from "../../../components/ui/StatusMessage";
import { construirUrlImagenBackend } from "../../../lib/backendUrl";
import { formatearTipoPublicador } from "../../../lib/formatoCatalogo";
import {
  buscarActividades,
  obtenerImagenesActividad,
} from "../../../services/actividadService";
import {
  obtenerImagenesPerfilPublicador,
  obtenerPerfilPublicadorPorId,
} from "../../../services/perfilPublicadorService";

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
  { clave: "fotos", etiqueta: "Fotos" },
  { clave: "info", etiqueta: "Info" },
] as const;

type ClaveTab = (typeof TABS)[number]["clave"];

type FotoDelPerfil = {
  clave: string;
  url: string;
  alt: string;
  href?: string;
};

function parsearId(idCrudo: string): number | null {
  const id = Number(idCrudo);

  return Number.isInteger(id) && id > 0 ? id : null;
}

export async function generateMetadata({
  params,
}: PerfilPublicadorPageProps): Promise<Metadata> {
  const { id: idCrudo } = await params;
  const id = parsearId(idCrudo);

  if (id === null) {
    return { title: "Publicador no encontrado", robots: { index: false } };
  }

  try {
    const perfil = await obtenerPerfilPublicadorPorId(id);

    if (!perfil) {
      return { title: "Publicador no encontrado", robots: { index: false } };
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
        canonical: `/publicadores/${perfil.id}`,
      },
      openGraph: {
        title: `${perfil.nombre} | DondeEntreno`,
        description: descripcion,
        type: "profile",
      },
    };
  } catch (error) {
    console.error("Error al generar metadata del perfil publicador:", error);

    return { title: "Perfil de publicador" };
  }
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
  const id = parsearId(idCrudo);

  if (id === null) {
    notFound();
  }

  let perfil: PerfilPublicadorPublico | null = null;
  let huboError = false;

  try {
    perfil = await obtenerPerfilPublicadorPorId(id);
  } catch (error) {
    huboError = true;
    console.error("Error al cargar el perfil publicador:", error);
  }

  if (huboError) {
    return (
      <main className="min-h-screen bg-[var(--color-bg)] text-[var(--color-text)]">
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

  /* Imágenes y actividades: best-effort, el perfil se muestra igual. */
  const [imagenes, respuestaActividades] = await Promise.all([
    obtenerImagenesPerfilPublicador(perfil.id).catch(
      () => [] as ImagenPerfilPublicador[]
    ),
    buscarActividades({
      perfilPublicadorId: perfil.id,
      page: 0,
      size: 6,
    }).catch(() => null),
  ]);

  const logo = imagenes.find((imagen) => imagen.tipoImagen === "LOGO");
  const portada = imagenes.find((imagen) => imagen.tipoImagen === "PORTADA");
  const logoUrl = construirUrlImagenBackend(logo?.url);
  const portadaUrl = construirUrlImagenBackend(portada?.url);

  const actividades: Actividad[] = respuestaActividades?.contenido ?? [];
  const totalActividades = respuestaActividades?.totalElementos ?? 0;
  const huboErrorActividades = respuestaActividades === null;

  const fotos = await reunirFotosDelPerfil(perfil.nombre, actividades, imagenes);
  const tabActiva: ClaveTab = resolverTab(tabPedida, fotos.length > 0);

  const tipoVisible = perfil.tipoPublicador
    ? formatearTipoPublicador(perfil.tipoPublicador)
    : "Publicador de la comunidad";
  const iniciales = obtenerIniciales(perfil.nombre);
  /* Campo aditivo: un backend viejo no lo manda y queda en cero. */
  const seguidores = Math.max(0, perfil.cantidadSeguidores ?? 0);
  const hrefExplorar = `/explorar?perfilPublicadorId=${perfil.id}`;
  const sitioWebUrl = normalizarSitioWeb(perfil.sitioWeb);

  return (
    <main className="min-h-screen bg-[var(--color-bg)] text-[var(--color-text)]">
      <section className="mx-auto w-full max-w-6xl px-4 py-6">
        <Header />

        <div className="py-7 sm:py-9">
          {/* Encabezado del perfil: portada + identidad */}
          <article className="overflow-hidden rounded-[24px] border border-[#D9E2EC] bg-white shadow-[0_12px_35px_rgba(15,61,94,0.08)]">
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
                  className="absolute inset-0 bg-gradient-to-br from-[#0F3D5E] via-[#145276] to-[#2EB872]"
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
                    <span className="relative -mt-12 h-20 w-20 shrink-0 overflow-hidden rounded-full bg-white ring-4 ring-white shadow-[0_10px_24px_rgba(15,61,94,0.18)] sm:-mt-16 sm:h-28 sm:w-28">
                      <Image
                        src={logoUrl}
                        alt={`Logo de ${perfil.nombre}`}
                        fill
                        sizes="112px"
                        className="object-cover"
                      />
                    </span>
                  ) : (
                    <span
                      aria-hidden="true"
                      className="-mt-12 flex h-20 w-20 shrink-0 items-center justify-center rounded-full bg-[var(--color-primary)] text-xl font-extrabold tracking-[0.08em] text-white ring-4 ring-white shadow-[0_10px_24px_rgba(15,61,94,0.18)] sm:-mt-16 sm:h-28 sm:w-28 sm:text-2xl"
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
                    ruta={`/publicadores/${perfil.id}`}
                    titulo={perfil.nombre}
                  />
                </div>
              </div>

              {perfil.descripcion ? (
                <p className="mt-5 max-w-3xl text-sm leading-7 text-[var(--color-muted)] sm:text-base">
                  {perfil.descripcion}
                </p>
              ) : null}

              {/*
                Datos de un vistazo: el perfil sin descripción ni portada
                quedaba prácticamente vacío entre el nombre y el contacto.
              */}
              <div className="mt-5 flex flex-wrap items-center gap-2">
                {!huboErrorActividades ? (
                  <span className="rounded-full bg-[#E8F6FB] px-3 py-1.5 text-xs font-extrabold text-[#0F6F8F]">
                    {totalActividades === 1
                      ? "1 actividad publicada"
                      : `${totalActividades} actividades publicadas`}
                  </span>
                ) : null}

                {/*
                  El contador se muestra recién a partir del primer
                  seguidor: con la plataforma recién arrancando, un
                  "0 seguidores" en todos los perfiles los hace ver
                  abandonados sin aportar información.
                */}
                {seguidores > 0 ? (
                  <span className="rounded-full bg-[#F1F8FC] px-3 py-1.5 text-xs font-extrabold text-[var(--color-primary)]">
                    {seguidores === 1 ? "1 seguidor" : `${seguidores} seguidores`}
                  </span>
                ) : null}

                {perfil.verificado === true ? (
                  <span className="rounded-full bg-[#E6F7EF] px-3 py-1.5 text-xs font-extrabold text-[#1D7B4A]">
                    Perfil verificado
                  </span>
                ) : null}
              </div>

              <div className="mt-5 flex flex-col gap-3 sm:flex-row sm:items-center">
                <div className="sm:w-64">
                  <ContactButton
                    whatsapp={perfil.whatsapp}
                    instagram={perfil.instagram}
                    email={perfil.emailContacto}
                    className=""
                  />
                </div>
                {sitioWebUrl ? (
                  <a
                    href={sitioWebUrl}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="inline-flex min-h-11 items-center justify-center rounded-[18px] border border-[#BFDDEA] bg-white px-4 py-2 text-sm font-bold text-[var(--color-primary)] shadow-sm transition duration-200 ease-out hover:-translate-y-0.5 hover:border-[var(--color-primary)] hover:bg-[#F8FCFE]"
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
            className="mt-8 flex gap-2 overflow-x-auto border-b border-[#D9E2EC] pb-px"
            aria-label="Secciones del perfil"
          >
            {TABS.filter(
              (tab) => tab.clave !== "fotos" || fotos.length > 0
            ).map((tab) => {
              const activa = tab.clave === tabActiva;

              return (
                <Link
                  key={tab.clave}
                  href={`/publicadores/${perfil.id}?tab=${tab.clave}`}
                  scroll={false}
                  aria-current={activa ? "page" : undefined}
                  className={`-mb-px shrink-0 border-b-2 px-4 py-3 text-sm font-extrabold transition duration-200 ease-out ${
                    activa
                      ? "border-[var(--color-secondary)] text-[var(--color-primary)]"
                      : "border-transparent text-[var(--color-muted)] hover:border-[#BFDDEA] hover:text-[var(--color-primary)]"
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
                <div className="mt-5 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
                  {actividades.map((actividad) => (
                    <SocialActivityCard
                      key={actividad.id}
                      actividad={actividad}
                      variante="compacta"
                    />
                  ))}
                </div>
              )}
            </section>
          ) : null}

          {tabActiva === "fotos" ? (
            <section className="mt-7" aria-labelledby="fotos-perfil-titulo">
              <SectionHeader
                eyebrow="Fotos"
                title="Fotos reales"
                description="Imágenes de las actividades que publica, ya revisadas por el equipo."
                titleId="fotos-perfil-titulo"
              />

              <ul className="mt-5 grid grid-cols-2 gap-2 sm:grid-cols-3 sm:gap-3 lg:grid-cols-4">
                {fotos.map((foto) => (
                  <li key={foto.clave}>
                    {foto.href ? (
                      <Link
                        href={foto.href}
                        className="group relative block aspect-square overflow-hidden rounded-[var(--radius-md)] border border-[#D9E2EC] bg-[#F8FAFC]"
                      >
                        <Image
                          src={foto.url}
                          alt={foto.alt}
                          fill
                          sizes="(max-width: 640px) 50vw, (max-width: 1024px) 33vw, 260px"
                          className="object-cover transition duration-200 ease-out group-hover:scale-105"
                        />
                      </Link>
                    ) : (
                      <div className="relative block aspect-square overflow-hidden rounded-[var(--radius-md)] border border-[#D9E2EC] bg-[#F8FAFC]">
                        <Image
                          src={foto.url}
                          alt={foto.alt}
                          fill
                          sizes="(max-width: 640px) 50vw, (max-width: 1024px) 33vw, 260px"
                          className="object-cover"
                        />
                      </div>
                    )}
                  </li>
                ))}
              </ul>
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
                <div className="mt-5 rounded-[var(--radius-lg)] border border-[#DDEAF3] bg-white p-5">
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

function resolverTab(pedida: string | undefined, hayFotos: boolean): ClaveTab {
  const valida = TABS.some((tab) => tab.clave === pedida);

  if (!valida) {
    return "actividades";
  }

  /* Sin fotos la solapa no se muestra: entrar por URL cae en actividades. */
  if (pedida === "fotos" && !hayFotos) {
    return "actividades";
  }

  return pedida as ClaveTab;
}

/*
  Junta las fotos visibles del publicador: la galería propia del perfil
  más las imágenes aprobadas de cada una de sus actividades.

  Hoy no existe un endpoint que devuelva las imágenes de todas las
  actividades de un publicador, así que se pide una por actividad (como
  máximo las de la primera página). Es best-effort: si alguna falla, el
  perfil se muestra igual con las que sí respondieron.
*/
async function reunirFotosDelPerfil(
  nombrePerfil: string,
  actividades: Actividad[],
  imagenesDelPerfil: ImagenPerfilPublicador[]
): Promise<FotoDelPerfil[]> {
  const fotos: FotoDelPerfil[] = [];

  for (const imagen of imagenesDelPerfil) {
    const url = construirUrlImagenBackend(imagen.url);

    /* LOGO y PORTADA ya se ven en el encabezado: acá va la galería. */
    if (url && imagen.tipoImagen !== "LOGO" && imagen.tipoImagen !== "PORTADA") {
      fotos.push({
        clave: `perfil-${imagen.id}`,
        url,
        alt: imagen.titulo?.trim() || `Foto de ${nombrePerfil}`,
      });
    }
  }

  const porActividad = await Promise.all(
    actividades.map((actividad) =>
      obtenerImagenesActividad(actividad.slug).catch(() => [])
    )
  );

  porActividad.forEach((imagenesActividad, indice) => {
    const actividad = actividades[indice];

    for (const imagen of imagenesActividad) {
      const url = construirUrlImagenBackend(imagen.url);

      if (url) {
        fotos.push({
          clave: `actividad-${imagen.id}`,
          url,
          alt: imagen.descripcion?.trim() || `Foto de ${actividad.titulo}`,
          href: `/actividades/${actividad.slug}`,
        });
      }
    }
  });

  return fotos;
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
    <div className="rounded-[var(--radius-md)] border border-[#DDEAF3] bg-white p-4">
      <dt className="text-xs font-extrabold uppercase tracking-[0.12em] text-[var(--color-muted)]">
        {termino}
      </dt>
      <dd className="mt-1 text-sm font-bold text-[var(--color-primary)]">
        {href ? (
          <a
            href={href}
            target="_blank"
            rel="noopener noreferrer"
            className="underline decoration-[#BFDDEA] underline-offset-4 hover:decoration-[var(--color-primary)]"
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
