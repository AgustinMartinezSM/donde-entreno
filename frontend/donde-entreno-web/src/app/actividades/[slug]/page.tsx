import type { Metadata } from "next";
import { notFound } from "next/navigation";

import type { Actividad, ActividadDetalle } from "../../../types/actividad";
import { Header } from "../../../components/layout/Header";
import {
  ActividadNoEncontradaError,
  buscarActividades,
  obtenerDetalleActividad,
} from "../../../services/actividadService";
import { obtenerImagenesPerfilPublicador } from "../../../services/perfilPublicadorService";
import { ContactButton } from "../../../components/actividad/ContactButton";
import {
  ActividadGaleria,
  type FotoActividad,
} from "../../../components/actividad/ActividadGaleria";
import { FavoritoButton } from "../../../components/actividad/FavoritoButton";
import { MeGustaButton } from "../../../components/actividad/MeGustaButton";
import { SeguirPublicadorButton } from "../../../components/actividad/SeguirPublicadorButton";
import { ErrorState } from "../../../components/feedback/ErrorState";
import { CompartirButton } from "../../../components/social/CompartirButton";
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
import { formatearFechaRelativa } from "../../../lib/formatoFecha";
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
      <main className="min-h-screen text-[var(--color-text)]">
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
    Medio del post: la PRINCIPAL primero y detrás el resto de la galería
    aprobada, todo en un carrusel. El backend ya filtra APROBADA +
    activa; acá solo descartamos las URLs no publicables (las filas
    legado que guardan rutas de disco).
  */
  const fotos: FotoActividad[] = [
    ...(imagenPrincipal ? [imagenPrincipal] : []),
    ...(actividad.imagenes ?? []).filter(
      (imagen) => imagen.id !== imagenPrincipal?.id
    ),
  ].flatMap((imagen) => {
    const url = construirUrlImagenBackend(imagen.url);

    if (!url) {
      return [];
    }

    return [
      {
        id: imagen.id,
        url,
        alt:
          imagen.descripcion?.trim() ||
          imagen.titulo?.trim() ||
          `Foto de ${actividad.titulo}`,
        titulo: imagen.titulo?.trim() || null,
      },
    ];
  });

  /* Sin fotos propias, el carrusel muestra la ilustración del deporte. */
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
  /*
    Lugar en una línea, como la ubicación de un post. La dirección exacta
    y el resto de los datos siguen en el panel lateral: antes la columna
    principal repetía la misma caja de ubicación que el panel.
  */
  const zona = [actividad.barrioNombre, actividad.ciudadNombre]
    .filter(Boolean)
    .join(", ");
  const lugarVisible =
    [actividad.ubicacionNombre, zona].filter(Boolean).join(" · ") ||
    "Ubicación a confirmar";
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
    Contexto social del post (best-effort: si el backend falla acá, el
    detalle igual se muestra completo).
  */
  const { masDelPublicador, similares, logoPublicadorUrl } =
    await cargarContextoDelPost(actividad);

  return (
    <main className="min-h-screen text-[var(--color-text)]">
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
              {/*
                Encabezado del post: quién publica firma arriba de la foto,
                como en cualquier publicación. Antes esta identidad quedaba
                enterrada debajo del título, en una caja más.
              */}
              <header className="flex flex-wrap items-center justify-between gap-3 px-2 pb-3.5 pt-1.5 sm:px-3">
                <PublisherIdentity
                  nombre={actividad.perfilPublicadorNombre}
                  tipo={actividad.tipoPublicador}
                  verificado={actividad.perfilVerificado}
                  avatarUrl={logoPublicadorUrl}
                  tamanio="destacada"
                  nota={
                    publicadaRelativa ? `Publicada ${publicadaRelativa}` : null
                  }
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
              </header>

              <ActividadGaleria
                fotos={fotos}
                fallbackSrc={imagenFallbackUrl}
                fallbackAlt={actividad.titulo}
                fallbackText={actividad.deporteNombre || "Actividad"}
              />

              {/*
                Barra de acciones del post, pegada al medio: las tres cosas
                que puede hacer alguien que mira esta actividad.
              */}
              <div className="flex flex-wrap items-center gap-2 px-2 pt-4 sm:px-3">
                <MeGustaButton
                  slug={actividad.slug}
                  titulo={actividad.titulo}
                />

                <FavoritoButton variante="detalle" actividad={datosFavorito} />

                <CompartirButton
                  ruta={`/actividades/${actividad.slug}`}
                  titulo={actividad.titulo}
                  ocultarTextoEnMobile
                />
              </div>

              <div className="p-2 pt-6 sm:p-3 sm:pt-7">
                <p className="text-sm font-bold uppercase tracking-[0.2em] text-[var(--color-secondary)]">
                  {actividad.categoriaDeportivaNombre || "Deporte"}
                </p>

                <h1 className="mt-2 max-w-3xl text-2xl font-extrabold leading-tight text-[var(--color-primary)] sm:text-4xl">
                  {actividad.titulo}
                </h1>

                <p className="mt-3 flex items-start gap-2 text-sm font-semibold text-[var(--color-muted)]">
                  <IconoUbicacion />
                  <span>{lugarVisible}</span>
                </p>

                {/*
                  La descripción es el epígrafe del post: va suelta debajo
                  del título, sin caja ni encabezado de sección propios.
                  whitespace-pre-line respeta los renglones que escribió el
                  publicador.
                */}
                <p className="mt-4 max-w-3xl whitespace-pre-line text-sm leading-7 text-[var(--color-muted)] sm:text-base">
                  {actividad.descripcion ||
                    "Esta actividad todavía no tiene una descripción cargada."}
                </p>

                <div className="mt-5 flex flex-wrap gap-2.5">
                  {actividad.nivel && (
                    <span className="rounded-full bg-[var(--color-success-soft)] px-3 py-2 text-sm font-bold text-[var(--color-success)]">
                      {formatearEtiquetaCatalogo(actividad.nivel)}
                    </span>
                  )}

                  {actividad.modalidad && (
                    <span className="rounded-full bg-[var(--color-info-soft)] px-3 py-2 text-sm font-bold text-[var(--color-info-deep)]">
                      {formatearEtiquetaCatalogo(actividad.modalidad)}
                    </span>
                  )}

                  {actividad.enfoque && (
                    <span className="rounded-full bg-[var(--color-info-soft)] px-3 py-2 text-sm font-bold text-[var(--color-info-deep)]">
                      {formatearEtiquetaCatalogo(actividad.enfoque)}
                    </span>
                  )}

                  {actividad.cuposLimitados && (
                    <span className="rounded-full bg-[#FDF3E7] px-3 py-2 text-sm font-bold text-[#9A5B13]">
                      Cupos limitados
                    </span>
                  )}

                  {actividad.requiereInscripcion && (
                    <span className="rounded-full bg-[var(--color-bg)] px-3 py-2 text-sm font-bold text-[var(--color-muted)] ring-1 ring-[var(--color-border-soft)]">
                      Requiere inscripción
                    </span>
                  )}
                </div>

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
                          className="rounded-[var(--radius-md)] border border-[var(--color-border-soft)] bg-[var(--color-bg)] p-4 transition duration-200 ease-out hover:-translate-y-0.5 hover:border-[var(--color-border-accent)] hover:bg-white hover:shadow-[0_12px_30px_rgba(12,52,80,0.08)]"
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
                  <div className="rounded-[var(--radius-md)] border border-[var(--color-border-soft)] bg-[var(--color-bg)] p-4">
                    <p className="font-bold text-[var(--color-text)]">Lugar</p>

                    <p className="mt-1 text-[var(--color-muted)]">
                      {actividad.ubicacionNombre || "Lugar sin cargar"}
                    </p>
                  </div>

                  <div className="rounded-[var(--radius-md)] border border-[var(--color-border-soft)] bg-[var(--color-bg)] p-4">
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
                    <div className="rounded-[var(--radius-md)] border border-[var(--color-border-soft)] bg-[var(--color-bg)] p-4">
                      <p className="font-bold text-[var(--color-text)]">
                        Dirección
                      </p>

                      <p className="mt-1 text-[var(--color-muted)]">
                        {actividad.direccion}
                      </p>
                    </div>
                  )}

                  {precioVisible && (
                    <div className="rounded-[var(--radius-md)] border border-[var(--color-border-soft)] bg-[var(--color-bg)] p-4">
                      <p className="font-bold text-[var(--color-text)]">
                        Precio de referencia
                      </p>

                      <p className="mt-1 text-lg font-extrabold text-[var(--color-primary)]">
                        {precioVisible}
                      </p>
                    </div>
                  )}

                  {rangoEdad && (
                    <div className="rounded-[var(--radius-md)] border border-[var(--color-border-soft)] bg-[var(--color-bg)] p-4">
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
      <div className="fixed inset-x-0 bottom-[calc(5rem+env(safe-area-inset-bottom))] z-40 border-t border-[var(--color-border)] bg-white/95 px-4 py-3 shadow-[0_-8px_24px_rgba(15,61,94,0.10)] backdrop-blur-lg lg:hidden">
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
  Carga en paralelo el contexto social del post: las dos franjas de
  descubrimiento relacionado y el logo del publicador que firma arriba.
  Cualquier fallo devuelve vacío: nunca rompe el detalle.
*/
async function cargarContextoDelPost(actividad: ActividadDetalle): Promise<{
  masDelPublicador: Actividad[];
  similares: Actividad[];
  logoPublicadorUrl: string | null;
}> {
  const [respuestaPublicador, respuestaSimilares, imagenesPerfil] =
    await Promise.all([
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
      /*
        El detalle no trae el logo del publicador (el DTO público de
        actividad no lo expone), así que lo pedimos al endpoint de
        imágenes del perfil. Sin logo, la identidad cae a las iniciales.
      */
      actividad.perfilPublicadorId
        ? obtenerImagenesPerfilPublicador(actividad.perfilPublicadorId).catch(
            () => []
          )
        : Promise.resolve([]),
    ]);

  const masDelPublicador = (respuestaPublicador?.contenido ?? [])
    .filter((otra) => otra.slug !== actividad.slug)
    .slice(0, 3);

  const idsYaMostrados = new Set(masDelPublicador.map((otra) => otra.id));
  const similares = (respuestaSimilares?.contenido ?? [])
    .filter((otra) => otra.slug !== actividad.slug && !idsYaMostrados.has(otra.id))
    .slice(0, 3);

  const logoPublicadorUrl = construirUrlImagenBackend(
    imagenesPerfil.find((imagen) => imagen.tipoImagen === "LOGO")?.url
  );

  return { masDelPublicador, similares, logoPublicadorUrl };
}

function IconoUbicacion() {
  return (
    <svg
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
      className="mt-0.5 h-4 w-4 shrink-0 text-[var(--color-accent)]"
      aria-hidden="true"
    >
      <path d="M20 10c0 5-8 11-8 11S4 15 4 10a8 8 0 1 1 16 0z" />
      <circle cx="12" cy="10" r="2.5" />
    </svg>
  );
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
