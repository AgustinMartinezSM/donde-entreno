import type { Metadata } from "next";
import Link from "next/link";

import type { ActividadDetalle } from "../../../types/actividad";
import { Header } from "../../../components/layout/Header";
import { obtenerDetalleActividad } from "../../../services/actividadService";
import { ContactButton } from "../../../components/actividad/ContactButton";
import { ActivityImage } from "../../../components/actividad/ActivityImage";
import { ErrorState } from "../../../components/feedback/ErrorState";
import { API_BASE_URL } from "../../../lib/apiConfig";
import {
  obtenerImagenActividad,
  obtenerImagenFallbackActividad,
} from "../../../lib/activityImages";

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
      },
    };
  } catch (error) {
    /*
      Si falla la metadata, no devolvemos JSX.
      generateMetadata siempre tiene que devolver un objeto de metadata.
    */
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

  /*
    No renderizamos JSX dentro del try/catch.
    Primero intentamos obtener la actividad.
    Después, fuera del try/catch, decidimos qué pantalla mostrar.
  */
  let actividad: ActividadDetalle | null = null;
  let huboError = false;

  try {
    actividad = await obtenerDetalleActividad(slug);
  } catch (error) {
    huboError = true;
    console.error("Error al cargar detalle de actividad:", error);
  }

  if (huboError || !actividad) {
    return (
      <main className="min-h-screen bg-gradient-to-br from-[#F8FAFC] via-white to-[#E8F6FB] text-[var(--color-text)]">
        <section className="mx-auto w-full max-w-6xl px-4 py-6">
          <Header />

          <div className="py-10">
            <ErrorState
              titulo="No pudimos cargar esta actividad"
              descripcion="No encontramos esta actividad o no pudimos conectarnos con el servidor. Intentá nuevamente en unos minutos."
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

  return (
    <main className="min-h-screen bg-gradient-to-br from-[#F8FAFC] via-white to-[#E8F6FB] text-[var(--color-text)]">
      <section className="mx-auto w-full max-w-6xl px-4 py-6">
        <Header />

        <div className="py-8 sm:py-10">
          <Link
            href="/explorar"
            className="inline-flex rounded-full border border-[#BFDDEA] bg-white/90 px-4 py-2 text-sm font-bold text-[var(--color-primary)] shadow-sm transition duration-200 ease-out hover:-translate-y-0.5 hover:border-[var(--color-primary)] hover:bg-[#F8FCFE] active:scale-[0.98]"
          >
            ← Volver a explorar
          </Link>

          <div className="mt-6 grid gap-6 lg:grid-cols-[1.45fr_0.75fr] lg:gap-7">
            {/* Columna principal */}
            <article className="overflow-hidden rounded-[var(--radius-xl)] border border-[#DDEAF3] bg-[var(--color-surface)] p-3 shadow-[0_24px_60px_rgba(12,52,80,0.12)] transition duration-200 ease-out sm:p-4">
              <ActivityImage
                src={imagenUrl}
                fallbackSrc={imagenFallbackUrl}
                alt={imagenPrincipal?.descripcion || actividad.titulo}
                fallbackText={actividad.deporteNombre || "Actividad"}
                heightClassName="h-56 sm:h-80"
              />

              <div className="p-2 pt-6 sm:p-3 sm:pt-7">
                <p className="text-sm font-bold uppercase tracking-[0.2em] text-[var(--color-secondary)]">
                  {actividad.categoriaDeportivaNombre || "Deporte"}
                </p>

                <h1 className="mt-2 max-w-3xl text-[1.9rem] font-extrabold leading-tight text-[var(--color-primary)] sm:text-5xl">
                  {actividad.titulo}
                </h1>

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
                    <span className="rounded-full bg-[#E6F7EF] px-3 py-2 text-sm font-bold text-[#167A4A]">
                      {actividad.nivel}
                    </span>
                  )}

                  {actividad.modalidad && (
                    <span className="rounded-full bg-[#E8F6FB] px-3 py-2 text-sm font-bold text-[#0F6F8F]">
                      {actividad.modalidad}
                    </span>
                  )}

                  {actividad.enfoque && (
                    <span className="rounded-full bg-[#E8F6FB] px-3 py-2 text-sm font-bold text-[#0F6F8F]">
                      {actividad.enfoque}
                    </span>
                  )}
                </div>

                <div className="mt-7 rounded-[var(--radius-lg)] border border-[#DDEAF3] bg-white p-5 shadow-[0_12px_30px_rgba(12,52,80,0.06)] sm:mt-8">
                  <h2 className="text-xl font-extrabold text-[var(--color-primary)]">
                    Sobre la actividad
                  </h2>

                  <p className="mt-3 text-sm leading-7 text-[var(--color-muted)] sm:text-base">
                    {actividad.descripcion ||
                      "Esta actividad todavía no tiene una descripción cargada."}
                  </p>
                </div>

                {actividad.horarios && actividad.horarios.length > 0 && (
                  <div className="mt-7 rounded-[var(--radius-lg)] border border-[#DDEAF3] bg-white p-5 shadow-[0_12px_30px_rgba(12,52,80,0.06)] sm:mt-8">
                    <h2 className="text-xl font-extrabold text-[var(--color-primary)]">
                      Horarios
                    </h2>

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
                  </div>
                )}
              </div>
            </article>

            {/* Columna lateral */}
            <aside className="h-fit rounded-[var(--radius-xl)] border border-[#DDEAF3] bg-white/95 p-5 shadow-[0_22px_55px_rgba(12,52,80,0.12)] transition duration-200 ease-out sm:p-6 lg:sticky lg:top-8">
              <p className="text-xs font-extrabold uppercase tracking-[0.18em] text-[var(--color-secondary)]">
                Información clave
              </p>
              <h2 className="mt-2 text-2xl font-extrabold text-[var(--color-primary)]">
                Datos para entrenar
              </h2>

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

                {actividad.precioReferencia !== undefined &&
                  actividad.precioReferencia !== null &&
                  actividad.mostrarPrecio && (
                    <div className="rounded-[var(--radius-md)] border border-[#DDEAF3] bg-[#F8FAFC] p-4">
                      <p className="font-bold text-[var(--color-text)]">
                        Precio de referencia
                      </p>

                      <p className="mt-1 text-lg font-extrabold text-[var(--color-primary)]">
                        ${actividad.precioReferencia}
                      </p>
                    </div>
                  )}

                {actividad.edadMinima !== undefined &&
                  actividad.edadMinima !== null && (
                    <div className="rounded-[var(--radius-md)] border border-[#DDEAF3] bg-[#F8FAFC] p-4">
                      <p className="font-bold text-[var(--color-text)]">
                        Edad mínima
                      </p>

                      <p className="mt-1 text-[var(--color-muted)]">
                        Desde {actividad.edadMinima} años
                      </p>
                    </div>
                  )}
              </div>

              <ContactButton
                whatsapp={actividad.whatsappContacto}
                instagram={actividad.instagramContacto}
                email={actividad.emailContacto}
              />
            </aside>
          </div>
        </div>
      </section>
    </main>
  );
}

function construirUrlImagenBackend(url?: string | null) {
  const urlLimpia = url?.trim();

  if (!urlLimpia) {
    return null;
  }

  if (urlLimpia.startsWith("http://") || urlLimpia.startsWith("https://")) {
    return urlLimpia;
  }

  const separador = urlLimpia.startsWith("/") ? "" : "/";

  return `${API_BASE_URL}${separador}${urlLimpia}`;
}
