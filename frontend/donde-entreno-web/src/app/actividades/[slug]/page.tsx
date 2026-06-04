import Link from "next/link";
import { Header } from "../../../components/layout/Header";
import { obtenerDetalleActividad } from "../../../services/actividadService";
    import { ContactButton } from "../../../components/actividad/ContactButton";
import { API_BASE_URL } from "../../../lib/apiConfig";
import { ActivityImage } from "../../../components/actividad/ActivityImage";

type ActividadDetallePageProps = {
  params: Promise<{
    slug: string;
  }>;
};

export default async function ActividadDetallePage({
  params,
}: ActividadDetallePageProps) {
  const { slug } = await params;

  try {
    const actividad = await obtenerDetalleActividad(slug);

    const imagenPrincipal = actividad.imagenes?.find(
      (imagen) => imagen.tipoImagen === "PRINCIPAL"
    );

    const imagenPrincipalUrl = null;

    return (
      <main className="min-h-screen bg-[var(--color-bg)] text-[var(--color-text)]">
        <section className="mx-auto w-full max-w-6xl px-4 py-6">
          <Header />

          <div className="py-8 sm:py-10">
            <Link
              href="/explorar"
              className="text-sm font-bold text-[var(--color-primary)]"
            >
              ← Volver a explorar
            </Link>

            <div className="mt-6 grid gap-5 lg:grid-cols-[1.4fr_0.8fr] lg:gap-6">
              {/* Columna principal */}
                <article className="rounded-[var(--radius-xl)] border border-[var(--color-border)] bg-[var(--color-surface)] p-4 shadow-[var(--shadow-card)] sm:p-5">
                <ActivityImage
                  src={imagenPrincipalUrl}
                  alt={imagenPrincipal?.descripcion || actividad.titulo}
                  fallbackText={actividad.deporteNombre || "Actividad"}
                  heightClassName="h-40 sm:h-56"
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
                            className="rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-bg)] p-4"
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
                <aside className="h-fit rounded-[var(--radius-xl)] border border-[var(--color-border)] bg-[var(--color-surface)] p-4 shadow-[var(--shadow-card)] sm:p-5">                <h2 className="text-xl font-extrabold text-[var(--color-primary)]">
                  Información
                </h2>

                <div className="mt-5 space-y-4 text-sm">
                  <div>
                    <p className="font-bold text-[var(--color-text)]">
                      Lugar
                    </p>
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
  } catch (error) {
    console.error("Error al cargar detalle:", error);

    return (
      <main className="min-h-screen bg-[var(--color-bg)] text-[var(--color-text)]">
        <section className="mx-auto w-full max-w-6xl px-4 py-6">
          <Header />

          <div className="py-16">
            <h1 className="text-3xl font-extrabold text-[var(--color-primary)]">
              No pudimos cargar esta actividad
            </h1>

            <p className="mt-3 text-[var(--color-muted)]">
              Puede que el backend esté apagado o que la actividad no exista.
            </p>

            <Link
              href="/explorar"
              className="mt-6 inline-block rounded-[var(--radius-md)] bg-[var(--color-primary)] px-5 py-3 text-sm font-bold text-white"
            >
              Volver a explorar
            </Link>
          </div>
        </section>
      </main>
    );
  }
}

