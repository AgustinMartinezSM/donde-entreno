import type { Metadata } from "next";
import Image from "next/image";
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
import { buscarActividades } from "../../../services/actividadService";
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
}: PerfilPublicadorPageProps) {
  const { id: idCrudo } = await params;
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

  const tipoVisible = perfil.tipoPublicador
    ? formatearTipoPublicador(perfil.tipoPublicador)
    : "Publicador de la comunidad";
  const iniciales = obtenerIniciales(perfil.nombre);
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
              {!huboErrorActividades ? (
                <div className="mt-5 flex flex-wrap items-center gap-2">
                  <span className="rounded-full bg-[#E8F6FB] px-3 py-1.5 text-xs font-extrabold text-[#0F6F8F]">
                    {totalActividades === 1
                      ? "1 actividad publicada"
                      : `${totalActividades} actividades publicadas`}
                  </span>
                  {perfil.verificado === true ? (
                    <span className="rounded-full bg-[#E6F7EF] px-3 py-1.5 text-xs font-extrabold text-[#1D7B4A]">
                      Perfil verificado
                    </span>
                  ) : null}
                </div>
              ) : null}

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

          {/* Actividades del publicador */}
          <section className="mt-10" aria-labelledby="actividades-perfil-titulo">
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
                Este publicador todavía no tiene actividades publicadas. Seguilo
                para enterarte cuando publique.
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
        </div>
      </section>
    </main>
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
