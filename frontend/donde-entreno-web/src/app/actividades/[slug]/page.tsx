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
            className="text-sm font-bold text-[var(--color-primary)] transition hover:text-[#0B314D]"
          >
            ← Volver a explorar
          </Link>

          <div className="mt-6 grid gap-5 lg:grid-cols-[1.4fr_0.8fr] lg:gap-6">
            {/* Columna principal */}
            <article className="rounded-[var(--radius-xl)] border border-[var(--color-border)] bg-[var(--color-surface)] p-4 shadow-[0_18px_50px_rgba(12,52,80,0.10)] transition duration-200 ease-out sm:p-5">
              <ActivityImage
                src={imagenUrl}
                fallbackSrc={imagenFallbackUrl}
                alt={imagenPrincipal?.descripcion || actividad.titulo}
                fallbackText={actividad.deporteNombre || "Actividad"}
                heightClassName="h-48 sm:h-72"
              />

              <div className="mt-6">
                <p className="text-sm font-bold uppercase tracking-[0.2em] text-[var(--color-secondary)]">
                  {actividad.categoriaDeportivaNombre || "Deporte"}
                </p>

                <h1 className="mt-2 text-[1.65rem] font-extrabold leading-tight text-[var(--color-primary)] sm:text-4xl">
                  {actividad.titulo}
                </h1>

                <p className="mt-3 text-base text-[var(--color-muted)]">
                  {actividad.ubicacionNombre || "Ubicación no informada"}
                </p>

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

                <div className="mt-7 sm:mt-8">
                  <h2 className="text-xl font-extrabold text-[var(--color-primary)]">
                    Descripción
                  </h2>

                  <p className="mt-3 text-sm leading-7 text-[var(--color-muted)] sm:text-base">
                    {actividad.descripcion ||
                      "Esta actividad todavía no tiene una descripción cargada."}
                  </p>
                </div>

                {actividad.horarios && actividad.horarios.length > 0 && (
                  <div className="mt-7 sm:mt-8">
                    <h2 className="text-xl font-extrabold text-[var(--color-primary)]">
                      Horarios
                    </h2>

                    <div className="mt-4 grid gap-3 sm:grid-cols-2">
                      {actividad.horarios.map((horario) => (
                        <div
                          key={horario.id}
                          className="rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-bg)] p-4 transition duration-200 ease-out hover:-translate-y-0.5 hover:border-[#BFDDEA] hover:shadow-[0_12px_30px_rgba(12,52,80,0.08)]"
                        >
                          <p className="font-bold text-[var(--color-primary)]">
                            {horario.diaSemana}
                          </p>

                          <p className="mt-1 text-sm text-[var(--color-muted)]">
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
            <aside className="h-fit rounded-[var(--radius-xl)] border border-[#DDEAF3] bg-[var(--color-surface)] p-4 shadow-[0_18px_45px_rgba(12,52,80,0.10)] transition duration-200 ease-out sm:p-5 lg:sticky lg:top-8">
              <h2 className="text-xl font-extrabold text-[var(--color-primary)]">
                Información
              </h2>

              <div className="mt-5 space-y-4 text-sm">
                <div>
                  <p className="font-bold text-[var(--color-text)]">Lugar</p>

                  <p className="mt-1 text-[var(--color-muted)]">
                    {actividad.ubicacionNombre || "Lugar sin cargar"}
                  </p>
                </div>

                <div>
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
                  <div>
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
                    <div>
                      <p className="font-bold text-[var(--color-text)]">
                        Precio de referencia
                      </p>

                      <p className="mt-1 text-[var(--color-muted)]">
                        ${actividad.precioReferencia}
                      </p>
                    </div>
                  )}

                {actividad.edadMinima !== undefined &&
                  actividad.edadMinima !== null && (
                    <div>
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
