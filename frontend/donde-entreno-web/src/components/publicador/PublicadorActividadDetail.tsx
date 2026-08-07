"use client";

import { useEffect, useMemo, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import { useAuthSession } from "../auth/AuthSessionProvider";
import { ActivityImage } from "../actividad/ActivityImage";
import { AppLinkButton } from "../ui/AppLinkButton";
import { SectionHeader } from "../ui/SectionHeader";
import { StatusMessage } from "../ui/StatusMessage";
import { SurfaceCard } from "../ui/SurfaceCard";
import { API_BASE_URL } from "../../lib/apiConfig";
import {
  obtenerImagenActividad,
  obtenerImagenFallbackActividad,
} from "../../lib/activityImages";
import {
  PublicadorApiError,
  obtenerActividadPublicador,
} from "../../services/publicadorService";
import type {
  ActividadPublicadorDetalle,
  ActividadPublicadorImagen,
} from "../../types/publicador";
import { PublicadorActividadEstadoBadge } from "./PublicadorActividadEstadoBadge";
import { PublicadorPageHeader } from "./PublicadorPageHeader";

export function PublicadorActividadDetail() {
  const router = useRouter();
  const params = useParams<{ id: string }>();
  const { accessToken, cerrarSesion } = useAuthSession();
  const idActividad = useMemo(() => Number(params.id), [params.id]);
  const idActividadInvalido =
    !Number.isInteger(idActividad) || idActividad <= 0;
  const [actividad, setActividad] =
    useState<ActividadPublicadorDetalle | null>(null);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [noEncontrada, setNoEncontrada] = useState(false);

  useEffect(() => {
    let componenteActivo = true;

    if (!accessToken) {
      return () => {
        componenteActivo = false;
      };
    }

    if (idActividadInvalido) {
      return () => {
        componenteActivo = false;
      };
    }

    obtenerActividadPublicador(idActividad, accessToken)
      .then((actividadActual) => {
        if (!componenteActivo) {
          return;
        }

        setActividad(actividadActual);
        setError(null);
        setNoEncontrada(false);
      })
      .catch((errorCarga: unknown) => {
        if (!componenteActivo) {
          return;
        }

        if (errorCarga instanceof PublicadorApiError) {
          if (errorCarga.status === 401) {
            cerrarSesion();
            router.replace(
              `/login?returnTo=${encodeURIComponent(
                `/publicador/actividades/${idActividad}`
              )}`
            );
            return;
          }

          if (errorCarga.status === 403) {
            setError("No tenés permisos para acceder al panel publicador.");
            return;
          }

          if (errorCarga.status === 404) {
            setNoEncontrada(true);
            return;
          }

          setError(errorCarga.message);
          return;
        }

        setError("Ocurrió un problema inesperado al cargar la actividad.");
      })
      .finally(() => {
        if (!componenteActivo) {
          return;
        }

        setCargando(false);
      });

    return () => {
      componenteActivo = false;
    };
  }, [accessToken, cerrarSesion, idActividad, idActividadInvalido, router]);

  const mostrarCargando = cargando && !idActividadInvalido;
  const mostrarNoEncontrada = noEncontrada || idActividadInvalido;
  const slugPublico = actividad ? obtenerSlugPublico(actividad) : null;

  return (
    <main className="min-h-screen bg-gradient-to-br from-[#F8FAFC] via-white to-[#E8F6FB] px-4 py-8 text-[var(--color-text)] sm:py-12">
      <section className="mx-auto w-full max-w-6xl">
        <PublicadorPageHeader
          title="Detalle de actividad"
          description="Consultá cómo se ve y qué datos tiene una actividad ya aprobada en DondeEntreno."
          action={
            <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-1">
              {slugPublico ? (
                <AppLinkButton
                  href={`/actividades/${slugPublico}`}
                  fullWidth
                >
                  Ver pública
                </AppLinkButton>
              ) : null}
              <AppLinkButton
                href="/publicador/actividades"
                variant="secondary"
                fullWidth
              >
                Volver a mis actividades
              </AppLinkButton>
            </div>
          }
        />

        {mostrarCargando ? (
          <StatusMessage variant="info" role="status" className="mt-6">
            Cargando actividad...
          </StatusMessage>
        ) : null}

        {error ? (
          <StatusMessage variant="error" role="alert" className="mt-6">
            {error}
          </StatusMessage>
        ) : null}

        {mostrarNoEncontrada ? (
          <SurfaceCard className="mt-6 p-6 text-center sm:p-8">
            <StatusMessage variant="warning" role="alert">
              No encontramos esta actividad.
            </StatusMessage>
            <div className="mt-5">
              <AppLinkButton href="/publicador/actividades" variant="secondary">
                Volver a mis actividades
              </AppLinkButton>
            </div>
          </SurfaceCard>
        ) : null}

        {actividad ? (
          <ActividadDetalleContenido actividad={actividad} />
        ) : null}
      </section>
    </main>
  );
}

function ActividadDetalleContenido({
  actividad,
}: {
  actividad: ActividadPublicadorDetalle;
}) {
  const slugPublico = obtenerSlugPublico(actividad);
  const imagenBackend = construirUrlImagenBackend(actividad.imagenPrincipalUrl);
  const imagenUrl = obtenerImagenActividad({
    imagenBackend,
    deporteSlug: actividad.deporteSlug,
  });
  const imagenFallbackUrl = obtenerImagenFallbackActividad({
    deporteSlug: actividad.deporteSlug,
  });
  const imagenesConUrl = actividad.imagenes.filter((imagen) =>
    Boolean(imagen.url.trim())
  );

  return (
    <div className="mt-6 grid gap-5 lg:grid-cols-[1.1fr_0.9fr]">
      <SurfaceCard className="overflow-hidden border-[#BFDDEA] bg-gradient-to-br from-white via-white to-[#F8FCFE]">
        <ActivityImage
          src={imagenUrl}
          fallbackSrc={imagenFallbackUrl}
          alt={actividad.titulo}
          fallbackText={actividad.deporteNombre ?? "Actividad"}
          heightClassName="h-56 sm:h-72"
        />

        <div className="p-6 sm:p-8">
          <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
            <div>
              <p className="text-xs font-extrabold uppercase tracking-[0.16em] text-[var(--color-secondary)]">
                {actividad.deporteNombre ?? "Actividad publicada"}
              </p>
              <h2 className="mt-2 text-2xl font-extrabold text-[var(--color-primary)] sm:text-3xl">
                {actividad.titulo}
              </h2>
              <p className="mt-2 text-sm leading-6 text-[var(--color-muted)]">
                {obtenerUbicacion(actividad)}
              </p>
            </div>
            <PublicadorActividadEstadoBadge
              estado={actividad.estadoPublicacion}
              activa={actividad.activa}
            />
          </div>

          <StatusMessage variant="info" className="mt-6">
            Esta actividad ya está publicada. Podés proponer cambios desde{" "}
            <strong>Solicitar cambios</strong>: se publican después de la
            revisión del equipo.
          </StatusMessage>

          <div className="mt-8 grid gap-5">
            <SeccionDetalle titulo="Actividad">
              <CampoDetalle etiqueta="Deporte" valor={actividad.deporteNombre} />
              <CampoDetalle
                etiqueta="Categoría"
                valor={actividad.categoriaDeportivaNombre}
              />
              <CampoDetalle etiqueta="Descripción" valor={actividad.descripcion} />
              <CampoDetalle etiqueta="Enfoque" valor={formatearCatalogoONull(actividad.enfoque)} />
              <CampoDetalle etiqueta="Modalidad" valor={formatearCatalogoONull(actividad.modalidad)} />
              <CampoDetalle etiqueta="Nivel" valor={formatearCatalogoONull(actividad.nivel)} />
              <CampoDetalle etiqueta="Edades" valor={formatearEdades(actividad)} />
              <CampoDetalle etiqueta="Precio" valor={formatearPrecio(actividad)} />
              <CampoDetalle
                etiqueta="Requiere inscripción"
                valor={formatearBooleano(actividad.requiereInscripcion)}
              />
              <CampoDetalle
                etiqueta="Cupos limitados"
                valor={formatearBooleano(actividad.cuposLimitados)}
              />
            </SeccionDetalle>

            <SeccionDetalle titulo="Ubicación">
              <CampoDetalle etiqueta="Ciudad" valor={actividad.ciudadNombre} />
              <CampoDetalle etiqueta="Barrio" valor={actividad.barrioNombre} />
              <CampoDetalle etiqueta="Lugar" valor={actividad.nombreLugar} />
              <CampoDetalle etiqueta="Dirección" valor={actividad.direccion} />
              <CampoDetalle
                etiqueta="Referencia"
                valor={actividad.referenciaUbicacion}
              />
            </SeccionDetalle>

            <SeccionDetalle titulo="Contacto">
              <CampoDetalle etiqueta="WhatsApp" valor={actividad.whatsapp} />
              <CampoDetalle etiqueta="Instagram" valor={actividad.instagram} />
              <CampoDetalle etiqueta="Email" valor={actividad.email} />
            </SeccionDetalle>
          </div>
        </div>
      </SurfaceCard>

      <div className="grid gap-5">
        <SurfaceCard variant="info" className="p-6 sm:p-8">
          <SectionHeader
            eyebrow="Publicación"
            title="Datos internos"
            description="Referencias útiles para conectar esta actividad con su solicitud de origen."
          />
          <dl className="mt-6 grid gap-4">
            <CampoDetalle
              etiqueta="Creada"
              valor={formatearFechaHora(actividad.createdAt)}
            />
            <CampoDetalle
              etiqueta="Perfil publicador"
              valor={actividad.perfilPublicadorNombre}
            />
            <CampoDetalle
              etiqueta="Tipo de publicador"
              valor={formatearCatalogoONull(actividad.perfilPublicadorTipo)}
            />
            <CampoDetalle
              etiqueta="Código de solicitud"
              valor={actividad.solicitudCodigoSeguimiento}
            />
            <CampoDetalle
              etiqueta="Solicitud origen"
              valor={
                actividad.solicitudOrigenId
                  ? `#${actividad.solicitudOrigenId}`
                  : null
              }
            />
          </dl>

          {slugPublico ? (
            <div className="mt-6">
              <AppLinkButton href={`/actividades/${slugPublico}`} fullWidth>
                Abrir vista pública
              </AppLinkButton>
            </div>
          ) : null}

          <div className="mt-3">
            <AppLinkButton
              href={`/publicador/actividades/${actividad.id}/solicitar-cambios`}
              variant="secondary"
              fullWidth
            >
              Solicitar cambios
            </AppLinkButton>
            <p className="mt-2 text-center text-xs text-[var(--color-muted)]">
              Los cambios se publican después de la revisión del equipo.
            </p>
          </div>
        </SurfaceCard>

        <SurfaceCard className="p-6 sm:p-8">
          <SectionHeader
            eyebrow="Horarios"
            title="Horarios publicados"
            description="Días y franjas visibles para quienes buscan actividades."
          />
          {actividad.horarios.length > 0 ? (
            <div className="mt-6 grid gap-3">
              {actividad.horarios.map((horario) => (
                <div
                  key={horario.id}
                  className="rounded-[18px] border border-[#DDEAF3] bg-gradient-to-br from-white to-[#F8FCFE] p-4 transition duration-200 ease-out hover:border-[#BDE8D0]"
                >
                  <p className="text-sm font-extrabold text-[var(--color-primary)]">
                    {formatearCatalogoONull(horario.diaSemana)}
                  </p>
                  <p className="mt-1 text-sm font-bold text-[var(--color-muted)]">
                    {formatearHora(horario.horaInicio)} a{" "}
                    {formatearHora(horario.horaFin)}
                  </p>
                  {horario.observacion ? (
                    <p className="mt-2 text-sm leading-6 text-[var(--color-muted)]">
                      {horario.observacion}
                    </p>
                  ) : null}
                </div>
              ))}
            </div>
          ) : (
            <StatusMessage variant="info" className="mt-6">
              No hay horarios publicados para esta actividad.
            </StatusMessage>
          )}
        </SurfaceCard>

        <SurfaceCard className="p-6 sm:p-8">
          <SectionHeader
            eyebrow="Imágenes"
            title="Imágenes asociadas"
            description="Material visual publicado para esta actividad."
          />
          {imagenesConUrl.length > 0 ? (
            <div className="mt-6 grid gap-3">
              {imagenesConUrl.map((imagen) => (
                <ImagenActividadItem key={imagen.id} imagen={imagen} />
              ))}
            </div>
          ) : (
            <StatusMessage variant="info" className="mt-6">
              Esta actividad no tiene imágenes cargadas.
            </StatusMessage>
          )}
        </SurfaceCard>
      </div>
    </div>
  );
}

function ImagenActividadItem({
  imagen,
}: {
  imagen: ActividadPublicadorImagen;
}) {
  const src = construirUrlImagenBackend(imagen.url);

  return (
    <div className="rounded-[18px] border border-[#DDEAF3] bg-white/80 p-3">
      <ActivityImage
        src={src}
        alt={imagen.descripcion ?? imagen.titulo ?? "Imagen de actividad"}
        fallbackText={imagen.titulo ?? "Imagen"}
        heightClassName="h-36"
      />
      <div className="mt-3">
        <p className="text-sm font-extrabold text-[var(--color-primary)]">
          {imagen.titulo ?? formatearCatalogoONull(imagen.tipoImagen) ?? "Imagen"}
        </p>
        {imagen.descripcion ? (
          <p className="mt-1 text-sm leading-6 text-[var(--color-muted)]">
            {imagen.descripcion}
          </p>
        ) : null}
      </div>
    </div>
  );
}

function SeccionDetalle({
  titulo,
  children,
}: {
  titulo: string;
  children: React.ReactNode;
}) {
  return (
    <section className="rounded-[22px] border border-[#DDEAF3] bg-white/80 p-4 shadow-[0_10px_24px_rgba(12,52,80,0.05)]">
      <h3 className="text-lg font-extrabold text-[var(--color-primary)]">
        {titulo}
      </h3>
      <dl className="mt-4 grid gap-4 sm:grid-cols-2">{children}</dl>
    </section>
  );
}

function CampoDetalle({
  etiqueta,
  valor,
}: {
  etiqueta: string;
  valor: string | number | null;
}) {
  return (
    <div>
      <dt className="text-xs font-extrabold uppercase tracking-[0.14em] text-[var(--color-secondary)]">
        {etiqueta}
      </dt>
      <dd className="mt-1 break-words text-sm font-bold text-[var(--color-primary)]">
        {valor ?? "No informado"}
      </dd>
    </div>
  );
}

function obtenerSlugPublico(actividad: ActividadPublicadorDetalle): string | null {
  return actividad.slugPublico?.trim() || actividad.slug.trim() || null;
}

function obtenerUbicacion(actividad: ActividadPublicadorDetalle): string {
  return (
    [actividad.ciudadNombre, actividad.barrioNombre].filter(Boolean).join(", ") ||
    "Ubicación no informada"
  );
}

function formatearEdades(actividad: ActividadPublicadorDetalle): string | null {
  if (actividad.edadMinima === null && actividad.edadMaxima === null) {
    return null;
  }

  if (actividad.edadMinima !== null && actividad.edadMaxima !== null) {
    return `${actividad.edadMinima} a ${actividad.edadMaxima} años`;
  }

  if (actividad.edadMinima !== null) {
    return `Desde ${actividad.edadMinima} años`;
  }

  return `Hasta ${actividad.edadMaxima} años`;
}

function formatearPrecio(actividad: ActividadPublicadorDetalle): string | null {
  if (!actividad.mostrarPrecio || actividad.precioReferencia === null) {
    return null;
  }

  return new Intl.NumberFormat("es-AR", {
    style: "currency",
    currency: "ARS",
    maximumFractionDigits: 0,
  }).format(actividad.precioReferencia);
}

function formatearBooleano(valor: boolean | null): string | null {
  if (valor === null) {
    return null;
  }

  return valor ? "Sí" : "No";
}

function formatearCatalogoONull(valor: string | null): string | null {
  if (!valor) {
    return null;
  }

  return valor
    .toLowerCase()
    .split("_")
    .map((parte) => parte.charAt(0).toUpperCase() + parte.slice(1))
    .join(" ");
}

function formatearFechaHora(valor: string | null): string | null {
  if (!valor) {
    return null;
  }

  const fecha = new Date(valor);

  if (Number.isNaN(fecha.getTime())) {
    return null;
  }

  return new Intl.DateTimeFormat("es-AR", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  }).format(fecha);
}

function formatearHora(valor: string): string {
  const horaLimpia = valor.trim();

  if (/^\d{2}:\d{2}(:\d{2})?$/.test(horaLimpia)) {
    return horaLimpia.slice(0, 5);
  }

  return horaLimpia;
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
