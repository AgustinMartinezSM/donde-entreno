import type { Metadata } from "next";
import Image from "next/image";
import { notFound } from "next/navigation";

import type { Actividad, ActividadDetalle } from "../../../types/actividad";
import { Header } from "../../../components/layout/Header";
import {
  ActividadNoEncontradaError,
  buscarActividades,
  obtenerDetalleActividad,
} from "../../../services/actividadService";
import { ContactButton } from "../../../components/actividad/ContactButton";
import { ActivityImage } from "../../../components/actividad/ActivityImage";
import { FavoritoButton } from "../../../components/actividad/FavoritoButton";
import { MeGustaButton } from "../../../components/actividad/MeGustaButton";
import { SeguirPublicadorButton } from "../../../components/actividad/SeguirPublicadorButton";
import { ErrorState } from "../../../components/feedback/ErrorState";
import { PublisherIdentity } from "../../../components/social/PublisherIdentity";
import { SocialActivityCard } from "../../../components/social/SocialActivityCard";
import { construirUrlImagenBackend } from "../../../lib/backendUrl";
import {
  obtenerImagenActividad,
  obtenerImagenFallbackActividad,
} from "../../../lib/activityImages";
import {
  formatearEtiquetaCatalogo,
  formatearPrecio,
} from "../../../lib/formatoCatalogo";
import {
  formatearFechaLarga,
  formatearFechaRelativa,
} from "../../../lib/formatoFecha";
import { AppLinkButton } from "../../../components/ui/AppLinkButton";
import { SectionHeader } from "../../../components/ui/SectionHeader";
import { StatusMessage } from "../../../components/ui/StatusMessage";
import { SurfaceCard } from "../../../components/ui/SurfaceCard";

type ActividadDetallePageProps = {
  params: Promise<{
    slug: string;
  }>;
};

export async function generateMetadata({
  params,
}: ActividadDetallePageProps): Promise<Metadata> {
  const { slug } = await params;

  try {
    const actividad = await obtenerDetalleActividad(slug);

    const titulo = actividad.titulo || "Detalle de actividad";

    const descripcion =
      actividad.descripcion ||
      `Conocé más información sobre ${
        actividad.deporteNombre || "esta actividad"
      } en DondeEntreno.`;

    /*
      Imagen para compartir: la real de la actividad si existe; si no, la
      ilustración por deporte (ruta relativa resuelta con metadataBase).
    */
    const imagenPrincipal = actividad.imagenes?.find(
      (imagen) => imagen.tipoImagen === "PRINCIPAL"
    );
    const imagenOg =
      construirUrlImagenBackend(imagenPrincipal?.url) ??
      obtenerImagenActividad({
        imagenBackend: null,
        deporteSlug: actividad.deporteSlug,
      });

    return {
      /*
        Como en layout.tsx usamos template "%s | DondeEntreno",
        este title se va a ver como:
        "Boxeo recreativo para adultos principiantes | DondeEntreno"
      */
      title: titulo,
      description: descripcion,
      openGraph: {
        title: `${titulo} - DondeEntreno`,
        description: descripcion,
        type: "article",
        ...(imagenOg ? { images: [{ url: imagenOg }] } : {}),
      },
    };
  } catch (error) {
    if (error instanceof ActividadNoEncontradaError) {
      /* La página va a responder 404: evitamos indexar el soft-error. */
      return {
        title: "Actividad no encontrada",
        robots: { index: false },
      };
    }

    console.error("Error al generar metadata de actividad:", error);

    return {
      title: "Detalle de actividad",
      description:
        "Conocé más información sobre una actividad deportiva disponible en DondeEntreno.",
    };
  }
}

export default async function ActividadDetallePage({
  params,
}: ActividadDetallePageProps) {
  const { slug } = await params;

  let actividad: ActividadDetalle | null = null;
  let huboError = false;

  try {
    actividad = await obtenerDetalleActividad(slug);
  } catch (error) {
    if (error instanceof ActividadNoEncontradaError) {
      /* 404 real: slug inexistente ≠ backend caído. */
      notFound();
    }

    huboError = true;
    console.error("Error al cargar detalle de actividad:", error);
  }

  if (huboError || !actividad) {
    return (
      <main className="min-h-screen bg-[var(--color-bg)] text-[var(--color-text)]">
        <section className="mx-auto w-full max-w-6xl px-4 py-6">
          <Header />

          <div className="py-10">
            <ErrorState
              titulo="No pudimos cargar esta actividad"
              descripcion="No pudimos encontrar esta actividad ahora. Podés volver a explorar opciones disponibles."
              mostrarBotonInicio
              mostrarBotonExplorar
            />
          </div>
        </section>
      </main>
    );
  }

  const imagenPrincipal = actividad.imagenes?.find(
    (imagen) => imagen.tipoImagen === "PRINCIPAL"
  );

  /*
    Galería: el resto de las imágenes aprobadas que devuelve el detalle
    (el backend ya filtra APROBADA + activa). Hasta ahora el detalle solo
    leía la PRINCIPAL, así que las fotos de galería que subía y aprobaba
    el publicador no se veían en ninguna superficie pública.
  */
  const galeria = (actividad.imagenes ?? [])
    .filter((imagen) => imagen.id !== imagenPrincipal?.id)
    .map((imagen) => ({
      ...imagen,
      urlPublicable: construirUrlImagenBackend(imagen.url),
    }))
    .filter((imagen) => imagen.urlPublicable !== null);

  /*
    Usamos la misma prioridad visual que las cards:
    imagen real, imagen default por deporte y placeholder general.
  */
  const imagenBackend = construirUrlImagenBackend(imagenPrincipal?.url);
  const imagenUrl = obtenerImagenActividad({
    imagenBackend,
    deporteSlug: actividad.deporteSlug,
  });
  const imagenFallbackUrl = obtenerImagenFallbackActividad({
    deporteSlug: actividad.deporteSlug,
  });
  const volverAExplorarHref = actividad.ciudadSlug
    ? `/explorar?ciudadSlug=${encodeURIComponent(actividad.ciudadSlug)}`
    : "/explorar";
  const precioVisible =
    actividad.mostrarPrecio === true
      ? formatearPrecio(actividad.precioReferencia)
      : null;
  const rangoEdad = formatearRangoEdad(
    actividad.edadMinima,
    actividad.edadMaxima
  );
  const publicadaRelativa = formatearFechaRelativa(actividad.fechaPublicacion);
  const publicadaExacta = formatearFechaLarga(actividad.fechaPublicacion);
  const datosFavorito = {
    slug: actividad.slug,
    titulo: actividad.titulo,
    deporteNombre: actividad.deporteNombre,
    deporteSlug: actividad.deporteSlug,
    ciudadNombre: actividad.ciudadNombre,
    barrioNombre: actividad.barrioNombre,
    imagenPrincipalUrl: imagenPrincipal?.url ?? null,
    nivel: actividad.nivel,
    modalidad: actividad.modalidad,
    precioReferencia: actividad.precioReferencia,
    mostrarPrecio: actividad.mostrarPrecio,
  };

  /*
    Secciones de descubrimiento relacionado (best-effort: si el backend
    falla acá, el detalle igual se muestra completo).
  */
  const { masDelPublicador, similares } = await cargarRelacionadas(actividad);

  return (
    <main className="min-h-screen bg-[var(--color-bg)] text-[var(--color-text)]">
      <section className="mx-auto w-full max-w-6xl px-4 py-6">
        <Header />

        {/* pb extra en mobile para que la barra sticky de contacto no tape contenido */}
        <div className="py-8 pb-24 sm:py-10 lg:pb-10">
          <AppLinkButton
            href={volverAExplorarHref}
            variant="secondary"
            size="sm"
            className="w-fit rounded-full"
          >
            ← Volver a explorar
          </AppLinkButton>

          <div className="mt-6 grid gap-6 lg:grid-cols-[1.45fr_0.75fr] lg:gap-7">
            {/* Columna principal */}
            <SurfaceCard
              as="article"
              className="overflow-hidden p-3 transition duration-200 ease-out sm:p-4"
            >
              <ActivityImage
                src={imagenUrl}
                fallbackSrc={imagenFallbackUrl}
                alt={imagenPrincipal?.descripcion || actividad.titulo}
                fallbackText={actividad.deporteNombre || "Actividad"}
                heightClassName="h-56 sm:h-80"
                sizes="(max-width: 1023px) 100vw, 800px"
              />

              <div className="p-2 pt-6 sm:p-3 sm:pt-7">
                <p className="text-sm font-bold uppercase tracking-[0.2em] text-[var(--color-secondary)]">
                  {actividad.categoriaDeportivaNombre || "Deporte"}
                </p>

                <h1 className="mt-2 max-w-3xl text-[1.9rem] font-extrabold leading-tight text-[var(--color-primary)] sm:text-5xl">
                  {actividad.titulo}
                </h1>

                {publicadaRelativa ? (
                  <p
                    className="mt-2 text-sm font-semibold text-[var(--color-muted)]"
                    title={publicadaExacta ?? undefined}
                  >
                    Publicada {publicadaRelativa}
                  </p>
                ) : null}

                {/* Identidad del publicador: quién publica esta actividad */}
                <div className="mt-5 flex flex-wrap items-center justify-between gap-4 rounded-[var(--radius-lg)] border border-[#DDEAF3] bg-[#F8FCFE] p-4">
                  <PublisherIdentity
                    nombre={actividad.perfilPublicadorNombre}
                    tipo={actividad.tipoPublicador}
                    verificado={actividad.perfilVerificado}
                    href={
                      actividad.perfilPublicadorId
                        ? `/publicadores/${actividad.perfilPublicadorId}`
                        : undefined
                    }
                  />

                  {actividad.perfilPublicadorId ? (
                    <div className="flex flex-wrap items-center gap-2">
                      <SeguirPublicadorButton
                        perfilPublicadorId={actividad.perfilPublicadorId}
                        perfilPublicadorNombre={actividad.perfilPublicadorNombre}
                      />
                      <AppLinkButton
                        href={`/publicadores/${actividad.perfilPublicadorId}`}
                        variant="outline"
                        size="sm"
                      >
                        Ver perfil
                      </AppLinkButton>
                    </div>
                  ) : null}
                </div>

                <div className="mt-4 rounded-[var(--radius-lg)] border border-[#DDEAF3] bg-[#F8FAFC] p-4">
                  <p className="text-sm font-bold text-[var(--color-primary)]">
                    {actividad.ubicacionNombre || "Ubicación no informada"}
                  </p>
                  <p className="mt-1 text-sm text-[var(--color-muted)]">
                    {actividad.barrioNombre || "Barrio sin cargar"}
                    {actividad.ciudadNombre
                      ? `, ${actividad.ciudadNombre}`
                      : ""}
                  </p>
                </div>

                <div className="mt-4 flex flex-wrap gap-2.5">
                  {actividad.nivel && (
                    <span className="rounded-full bg-[#E6F7EF] px-3 py-2 text-sm font-bold text-[#1D7B4A]">
                      {formatearEtiquetaCatalogo(actividad.nivel)}
                    </span>
                  )}

                  {actividad.modalidad && (
                    <span className="rounded-full bg-[#E8F6FB] px-3 py-2 text-sm font-bold text-[#0F6F8F]">
                      {formatearEtiquetaCatalogo(actividad.modalidad)}
                    </span>
                  )}

                  {actividad.enfoque && (
                    <span className="rounded-full bg-[#E8F6FB] px-3 py-2 text-sm font-bold text-[#0F6F8F]">
                      {formatearEtiquetaCatalogo(actividad.enfoque)}
                    </span>
                  )}

                  {actividad.cuposLimitados && (
                    <span className="rounded-full bg-[#FDF3E7] px-3 py-2 text-sm font-bold text-[#9A5B13]">
                      Cupos limitados
                    </span>
                  )}

                  {actividad.requiereInscripcion && (
                    <span className="rounded-full bg-[#F8FAFC] px-3 py-2 text-sm font-bold text-[var(--color-muted)] ring-1 ring-[#DDEAF3]">
                      Requiere inscripción
                    </span>
                  )}
                </div>

                <div className="mt-5 flex flex-wrap gap-2.5">
                  <FavoritoButton variante="detalle" actividad={datosFavorito} />

                  <MeGustaButton
                    slug={actividad.slug}
                    titulo={actividad.titulo}
                  />
                </div>

                <SurfaceCard className="mt-7 p-5 sm:mt-8">
                  <SectionHeader title="Sobre la actividad" />

                  <p className="mt-3 text-sm leading-7 text-[var(--color-muted)] sm:text-base">
                    {actividad.descripcion ||
                      "Esta actividad todavía no tiene una descripción cargada."}
                  </p>
                </SurfaceCard>

                {galeria.length > 0 ? (
                  <SurfaceCard className="mt-7 p-5 sm:mt-8">
                    <SectionHeader
                      title="Fotos de la actividad"
                      description="Imágenes que compartió el publicador."
                    />

                    <ul className="mt-4 grid grid-cols-2 gap-3 sm:grid-cols-3">
                      {galeria.map((imagen) => (
                        <li key={imagen.id}>
                          <figure className="overflow-hidden rounded-[var(--radius-md)] border border-[#DDEAF3] bg-[#F8FAFC]">
                            <div className="relative h-32 w-full sm:h-36">
                              <Image
                                src={imagen.urlPublicable as string}
                                alt={
                                  imagen.descripcion ||
                                  imagen.titulo ||
                                  `Foto de ${actividad.titulo}`
                                }
                                fill
                                sizes="(max-width: 640px) 50vw, (max-width: 1023px) 33vw, 260px"
                                className="object-cover"
                              />
                            </div>
                            {imagen.titulo ? (
                              <figcaption className="px-3 py-2 text-xs font-semibold leading-5 text-[var(--color-muted)]">
                                {imagen.titulo}
                              </figcaption>
                            ) : null}
                          </figure>
                        </li>
                      ))}
                    </ul>
                  </SurfaceCard>
                ) : null}

                <SurfaceCard className="mt-7 p-5 sm:mt-8">
                  <SectionHeader
                    title="Horarios"
                    description="Revisá cuándo se dicta antes de contactar."
                  />

                  {actividad.horarios && actividad.horarios.length > 0 ? (
                    <div className="mt-4 grid gap-3 sm:grid-cols-2">
                      {actividad.horarios.map((horario) => (
                        <div
                          key={horario.id}
                          className="rounded-[var(--radius-md)] border border-[#DDEAF3] bg-[#F8FAFC] p-4 transition duration-200 ease-out hover:-translate-y-0.5 hover:border-[#BFDDEA] hover:bg-white hover:shadow-[0_12px_30px_rgba(12,52,80,0.08)]"
                        >
                          <p className="font-bold text-[var(--color-primary)]">
                            {horario.diaSemana}
                          </p>

                          <p className="mt-2 text-lg font-extrabold text-[var(--color-primary)]">
                            {horario.horaInicio} a {horario.horaFin}
                          </p>

                          {horario.observacion && (
                            <p className="mt-2 text-sm text-[var(--color-muted)]">
                              {horario.observacion}
                            </p>
                          )}
                        </div>
                      ))}
                    </div>
                  ) : (
                    <StatusMessage
                      variant="info"
                      title="Horarios a confirmar"
                      className="mt-4"
                    >
                      <p>
                        Esta actividad todavía no cargó horarios visibles.
                        Consultá por el canal de contacto disponible.
                      </p>
                    </StatusMessage>
                  )}
                </SurfaceCard>
              </div>
            </SurfaceCard>

            {/* Columna lateral */}
            <aside className="h-fit lg:sticky lg:top-8">
              <SurfaceCard className="bg-white/95 p-5 transition duration-200 ease-out sm:p-6">
                <SectionHeader
                  eyebrow="Información clave"
                  title="Datos para entrenar"
                />

                <div className="mt-5 space-y-3 text-sm">
                  <div className="rounded-[var(--radius-md)] border border-[#DDEAF3] bg-[#F8FAFC] p-4">
                    <p className="font-bold text-[var(--color-text)]">Lugar</p>

                    <p className="mt-1 text-[var(--color-muted)]">
                      {actividad.ubicacionNombre || "Lugar sin cargar"}
                    </p>
                  </div>

                  <div className="rounded-[var(--radius-md)] border border-[#DDEAF3] bg-[#F8FAFC] p-4">
                    <p className="font-bold text-[var(--color-text)]">
                      Ubicación
                    </p>

                    <p className="mt-1 text-[var(--color-muted)]">
                      {actividad.barrioNombre || "Barrio sin cargar"}
                      {actividad.ciudadNombre
                        ? `, ${actividad.ciudadNombre}`
                        : ""}
                    </p>
                  </div>

                  {actividad.direccion && (
                    <div className="rounded-[var(--radius-md)] border border-[#DDEAF3] bg-[#F8FAFC] p-4">
                      <p className="font-bold text-[var(--color-text)]">
                        Dirección
                      </p>

                      <p className="mt-1 text-[var(--color-muted)]">
                        {actividad.direccion}
                      </p>
                    </div>
                  )}

                  {precioVisible && (
                    <div className="rounded-[var(--radius-md)] border border-[#DDEAF3] bg-[#F8FAFC] p-4">
                      <p className="font-bold text-[var(--color-text)]">
                        Precio de referencia
                      </p>

                      <p className="mt-1 text-lg font-extrabold text-[var(--color-primary)]">
                        {precioVisible}
                      </p>
                    </div>
                  )}

                  {rangoEdad && (
                    <div className="rounded-[var(--radius-md)] border border-[#DDEAF3] bg-[#F8FAFC] p-4">
                      <p className="font-bold text-[var(--color-text)]">
                        Edades
                      </p>

                      <p className="mt-1 text-[var(--color-muted)]">
                        {rangoEdad}
                      </p>
                    </div>
                  )}
                </div>

                {/*
                  Solo desde lg: por debajo, el mismo CTA ya vive en la
                  barra sticky de abajo y se veían dos botones de
                  WhatsApp en la misma pantalla.
                */}
                <div className="hidden lg:block">
                  <ContactButton
                    whatsapp={actividad.whatsappContacto}
                    instagram={actividad.instagramContacto}
                    email={actividad.emailContacto}
                    tituloActividad={actividad.titulo}
                  />
                </div>
              </SurfaceCard>
            </aside>
          </div>

          {/* Descubrimiento relacionado */}
          {masDelPublicador.length > 0 ? (
            <section className="mt-12" aria-labelledby="mas-del-publicador-titulo">
              <SectionHeader
                eyebrow="Del mismo publicador"
                title={`Más de ${actividad.perfilPublicadorNombre ?? "este publicador"}`}
                titleId="mas-del-publicador-titulo"
                action={
                  actividad.perfilPublicadorId ? (
                    <AppLinkButton
                      href={`/explorar?perfilPublicadorId=${actividad.perfilPublicadorId}`}
                      variant="secondary"
                      size="sm"
                    >
                      Ver todas
                    </AppLinkButton>
                  ) : undefined
                }
              />
              <div className="mt-5 grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
                {masDelPublicador.map((relacionada) => (
                  <SocialActivityCard
                    key={relacionada.id}
                    actividad={relacionada}
                    variante="compacta"
                  />
                ))}
              </div>
            </section>
          ) : null}

          {similares.length > 0 ? (
            <section className="mt-12" aria-labelledby="similares-titulo">
              <SectionHeader
                eyebrow="Para seguir descubriendo"
                title={
                  actividad.ciudadNombre
                    ? `Similares en ${actividad.ciudadNombre}`
                    : "Actividades similares"
                }
                titleId="similares-titulo"
                action={
                  <AppLinkButton
                    href={volverAExplorarHref}
                    variant="secondary"
                    size="sm"
                  >
                    Explorar más
                  </AppLinkButton>
                }
              />
              <div className="mt-5 grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
                {similares.map((relacionada) => (
                  <SocialActivityCard
                    key={relacionada.id}
                    actividad={relacionada}
                    variante="compacta"
                  />
                ))}
              </div>
            </section>
          ) : null}
        </div>
      </section>

      {/* Barra sticky de contacto en mobile: el paso de conversión principal
          queda siempre a mano, por encima de la navegación inferior. */}
      <div className="fixed inset-x-0 bottom-[calc(5rem+env(safe-area-inset-bottom))] z-40 border-t border-[#D9E2EC] bg-white/95 px-4 py-3 shadow-[0_-8px_24px_rgba(15,61,94,0.10)] backdrop-blur-lg lg:hidden">
        {/*
          pr deja libre la esquina donde flota el asistente (56px + su
          margen): sin eso el botón del asistente quedaba encima del
          extremo derecho del CTA.
        */}
        <div className="mx-auto flex max-w-lg items-center gap-3 pr-[4.5rem]">
          {precioVisible ? (
            <div className="shrink-0">
              <p className="text-[11px] font-extrabold uppercase tracking-[0.1em] text-[var(--color-muted)]">
                Desde
              </p>
              <p className="text-base font-extrabold leading-tight text-[var(--color-primary)]">
                {precioVisible}
              </p>
            </div>
          ) : null}
          <div className="min-w-0 flex-1">
            <ContactButton
              whatsapp={actividad.whatsappContacto}
              instagram={actividad.instagramContacto}
              email={actividad.emailContacto}
              tituloActividad={actividad.titulo}
              className=""
            />
          </div>
        </div>
      </div>
    </main>
  );
}

/*
  Carga las dos franjas de descubrimiento relacionado en paralelo.
  Cualquier fallo devuelve listas vacías: nunca rompe el detalle.
*/
async function cargarRelacionadas(actividad: ActividadDetalle): Promise<{
  masDelPublicador: Actividad[];
  similares: Actividad[];
}> {
  const [respuestaPublicador, respuestaSimilares] = await Promise.all([
    actividad.perfilPublicadorId
      ? buscarActividades({
          perfilPublicadorId: actividad.perfilPublicadorId,
          page: 0,
          size: 4,
        }).catch(() => null)
      : Promise.resolve(null),
    actividad.deporteSlug
      ? buscarActividades({
          deporteSlug: actividad.deporteSlug,
          ciudadSlug: actividad.ciudadSlug,
          page: 0,
          size: 7,
        }).catch(() => null)
      : Promise.resolve(null),
  ]);

  const masDelPublicador = (respuestaPublicador?.contenido ?? [])
    .filter((otra) => otra.slug !== actividad.slug)
    .slice(0, 3);

  const idsYaMostrados = new Set(masDelPublicador.map((otra) => otra.id));
  const similares = (respuestaSimilares?.contenido ?? [])
    .filter((otra) => otra.slug !== actividad.slug && !idsYaMostrados.has(otra.id))
    .slice(0, 3);

  return { masDelPublicador, similares };
}

function formatearRangoEdad(
  edadMinima?: number | null,
  edadMaxima?: number | null
): string | null {
  if (edadMinima != null && edadMaxima != null) {
    return `De ${edadMinima} a ${edadMaxima} años`;
  }

  if (edadMinima != null) {
    return `Desde ${edadMinima} años`;
  }

  if (edadMaxima != null) {
    return `Hasta ${edadMaxima} años`;
  }

  return null;
}
