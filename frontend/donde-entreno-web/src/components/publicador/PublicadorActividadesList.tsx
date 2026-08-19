"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { useAuthSession } from "../auth/AuthSessionProvider";
import { ActivityImage } from "../actividad/ActivityImage";
import { AppButton } from "../ui/AppButton";
import { AppLinkButton } from "../ui/AppLinkButton";
import { SectionHeader } from "../ui/SectionHeader";
import { StatusMessage } from "../ui/StatusMessage";
import { SurfaceCard } from "../ui/SurfaceCard";
import { construirUrlImagenBackend } from "../../lib/backendUrl";
import {
  obtenerImagenActividad,
  obtenerImagenFallbackActividad,
} from "../../lib/activityImages";
import {
  PublicadorApiError,
  listarActividadesPublicador,
} from "../../services/publicadorService";
import type {
  ActividadPublicadorResumen,
  ActividadesPublicadorPage,
  OrdenActividadesPublicador,
} from "../../types/publicador";
import { PublicadorActividadEstadoBadge } from "./PublicadorActividadEstadoBadge";
import { PublicadorPageHeader } from "./PublicadorPageHeader";
import type { ChangeEvent } from "react";

const TAMANIO_PAGINA = 10;

const ORDENES: Array<{
  valor: OrdenActividadesPublicador;
  etiqueta: string;
}> = [
  { valor: "recientes", etiqueta: "Más recientes" },
  { valor: "antiguos", etiqueta: "Más antiguas" },
  { valor: "titulo_asc", etiqueta: "Título A-Z" },
];

export function PublicadorActividadesList() {
  const router = useRouter();
  const { accessToken, cerrarSesion } = useAuthSession();
  const [paginaActividades, setPaginaActividades] =
    useState<ActividadesPublicadorPage | null>(null);
  const [paginaActual, setPaginaActual] = useState(0);
  const [orden, setOrden] = useState<OrdenActividadesPublicador>("recientes");
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let componenteActivo = true;

    if (!accessToken) {
      return () => {
        componenteActivo = false;
      };
    }

    listarActividadesPublicador(
      {
        page: paginaActual,
        size: TAMANIO_PAGINA,
        orden,
      },
      accessToken
    )
      .then((pagina) => {
        if (!componenteActivo) {
          return;
        }

        setPaginaActividades(pagina);
        setError(null);
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
                "/publicador/actividades"
              )}`
            );
            return;
          }

          if (errorCarga.status === 403) {
            setError("No tenés permisos para ver esto: es una cuenta de publicador lo que hace falta.");
            return;
          }

          setError(errorCarga.message);
          return;
        }

        setError("Ocurrió un problema inesperado al cargar tus actividades.");
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
  }, [accessToken, cerrarSesion, orden, paginaActual, router]);

  function cambiarOrden(evento: ChangeEvent<HTMLSelectElement>) {
    setCargando(true);
    setError(null);
    setOrden(evento.target.value as OrdenActividadesPublicador);
    setPaginaActual(0);
  }

  function irPaginaAnterior() {
    setCargando(true);
    setError(null);
    setPaginaActual((pagina) => Math.max(pagina - 1, 0));
  }

  function irPaginaSiguiente() {
    if (paginaActividades?.ultima) {
      return;
    }

    setCargando(true);
    setError(null);
    setPaginaActual((pagina) => pagina + 1);
  }

  const actividades = paginaActividades?.contenido ?? [];
  const puedeIrAnterior = !cargando && paginaActual > 0;
  const puedeIrSiguiente =
    !cargando && Boolean(paginaActividades) && !paginaActividades?.ultima;

  return (
    <main className="min-h-screen px-4 py-8 text-[var(--color-text)] sm:py-12">
      <section className="mx-auto w-full max-w-6xl">
        <PublicadorPageHeader
          title="Mis actividades"
          description="Actividades aprobadas y publicadas en DondeEntreno."
          action={
            <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-1">
              <AppLinkButton href="/publicador/solicitudes/nueva" fullWidth>
                Nueva solicitud
              </AppLinkButton>
              <AppLinkButton href="/publicador" variant="secondary" fullWidth>
                Volver al panel
              </AppLinkButton>
            </div>
          }
        />

        <SurfaceCard className="mt-6 border-[var(--color-border-accent)] bg-gradient-to-br from-white via-white to-[var(--color-surface-soft)] p-5 sm:p-6">
          <div className="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
            <SectionHeader
              eyebrow="Actividades"
              title="Aprobadas y visibles"
              description="Revisá tus actividades publicadas, abrí el detalle interno o visitá la vista pública."
            />

            <div className="w-full lg:w-64">
              <label
                htmlFor="orden-actividades-publicador"
                className="text-sm font-bold text-[var(--color-primary)]"
              >
                Ordenar por
              </label>
              <select
                id="orden-actividades-publicador"
                value={orden}
                onChange={cambiarOrden}
                disabled={cargando}
                className="mt-2 min-h-11 w-full rounded-[16px] border border-[var(--color-border-accent)] bg-white px-4 text-sm font-bold text-[var(--color-primary)] outline-none transition focus:border-[var(--color-accent)] focus:ring-4 focus:ring-[var(--color-border-soft)] disabled:cursor-not-allowed disabled:opacity-70"
              >
                {ORDENES.map((opcion) => (
                  <option key={opcion.valor} value={opcion.valor}>
                    {opcion.etiqueta}
                  </option>
                ))}
              </select>
            </div>
          </div>

          {cargando ? (
            <StatusMessage variant="info" role="status" className="mt-6">
              Cargando actividades...
            </StatusMessage>
          ) : null}

          {error ? (
            <StatusMessage variant="error" role="alert" className="mt-6">
              {error}
            </StatusMessage>
          ) : null}

          {!cargando && !error && actividades.length === 0 ? (
            <div className="mt-6 rounded-[var(--radius-lg)] border border-[var(--color-border-accent)] bg-[var(--color-surface-soft)] p-4 text-sm leading-6 text-[var(--color-muted)]">
              <p className="font-extrabold text-[var(--color-primary)]">
                Todavía no tenés actividades publicadas activas.
              </p>
              <p className="mt-2">
                Las solicitudes aprobadas generan actividades, pero solo las
                actividades activas y visibles aparecen en esta sección.
              </p>
              <div className="mt-4 grid gap-3 sm:grid-cols-2">
                <AppLinkButton
                  href="/publicador/solicitudes"
                  size="sm"
                >
                  Ver mis solicitudes
                </AppLinkButton>
                <AppLinkButton
                  href="/publicador/solicitudes/nueva"
                  variant="secondary"
                  size="sm"
                >
                  Crear nueva solicitud
                </AppLinkButton>
              </div>
            </div>
          ) : null}

          {actividades.length > 0 ? (
            <div className="mt-6 grid gap-4">
              {actividades.map((actividad) => (
                <ActividadCardPublicador
                  key={actividad.id}
                  actividad={actividad}
                />
              ))}
            </div>
          ) : null}

          {paginaActividades ? (
            <div className="mt-6 flex flex-col gap-3 border-t border-[var(--color-border-soft)] pt-5 sm:flex-row sm:items-center sm:justify-between">
              <p className="text-sm font-bold text-[var(--color-muted)]">
                Página {paginaActividades.paginaActual + 1} de{" "}
                {Math.max(paginaActividades.totalPaginas, 1)}
              </p>
              <div className="grid gap-3 sm:grid-cols-2">
                <AppButton
                  type="button"
                  variant="secondary"
                  disabled={!puedeIrAnterior}
                  onClick={irPaginaAnterior}
                >
                  Anterior
                </AppButton>
                <AppButton
                  type="button"
                  variant="secondary"
                  disabled={!puedeIrSiguiente}
                  onClick={irPaginaSiguiente}
                >
                  Siguiente
                </AppButton>
              </div>
            </div>
          ) : null}
        </SurfaceCard>
      </section>
    </main>
  );
}

function ActividadCardPublicador({
  actividad,
}: {
  actividad: ActividadPublicadorResumen;
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

  return (
    <article className="overflow-hidden rounded-[22px] border border-[var(--color-border-soft)] bg-gradient-to-br from-white to-[var(--color-surface-soft)] p-4 shadow-sm transition duration-200 ease-out hover:-translate-y-0.5 hover:border-[var(--color-success-border)] hover:shadow-[0_18px_45px_rgba(12,52,80,0.10)] sm:p-5">
      <div className="grid gap-5 lg:grid-cols-[220px_1fr]">
        <ActivityImage
          src={imagenUrl}
          fallbackSrc={imagenFallbackUrl}
          alt={actividad.titulo}
          fallbackText={actividad.deporteNombre ?? "Actividad"}
          heightClassName="h-44 lg:h-full"
        />

        <div className="min-w-0">
          <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
            <div className="min-w-0">
              <p className="text-xs font-extrabold uppercase tracking-[0.16em] text-[var(--color-secondary)]">
                {actividad.deporteNombre ?? "Actividad publicada"}
              </p>
              <h2 className="mt-2 text-xl font-extrabold text-[var(--color-primary)]">
                {actividad.titulo}
              </h2>
              <p className="mt-2 text-sm leading-6 text-[var(--color-muted)]">
                {obtenerUbicacion(actividad)}
              </p>
            </div>
            <PublicadorActividadEstadoBadge
              estado={actividad.estadoPublicacion}
              activa={actividad.activa}
              size="sm"
            />
          </div>

          <dl className="mt-5 grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
            <DatoRapido etiqueta="Modalidad" valor={formatearCatalogoONull(actividad.modalidad)} />
            <DatoRapido etiqueta="Nivel" valor={formatearCatalogoONull(actividad.nivel)} />
            <DatoRapido etiqueta="Edades" valor={formatearEdades(actividad)} />
            <DatoRapido etiqueta="Precio" valor={formatearPrecio(actividad)} />
          </dl>

          <div className="mt-5 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
            <p className="text-sm font-bold text-[var(--color-muted)]">
              {actividad.createdAt
                ? `Publicada el ${formatearFecha(actividad.createdAt)}`
                : "Fecha no disponible"}
            </p>
            <div className="grid gap-3 sm:grid-cols-2">
              <Link
                href={`/publicador/actividades/${actividad.id}`}
                className="gradient-cta gradient-cta-hover inline-flex items-center justify-center rounded-[18px] bg-[var(--color-brand)] px-4 py-2 text-sm font-extrabold text-white shadow-[var(--shadow-button)] transition duration-200 ease-out hover:-translate-y-0.5"
              >
                Ver detalle
              </Link>
              {slugPublico ? (
                <Link
                  href={`/actividades/${slugPublico}`}
                  className="inline-flex items-center justify-center rounded-[18px] border border-[var(--color-border-accent)] bg-white px-4 py-2 text-sm font-extrabold text-[var(--color-primary)] transition duration-200 ease-out hover:-translate-y-0.5 hover:border-[var(--color-primary)] hover:bg-[var(--color-surface-soft)]"
                >
                  Ver pública
                </Link>
              ) : null}
            </div>
          </div>
        </div>
      </div>
    </article>
  );
}

function DatoRapido({
  etiqueta,
  valor,
}: {
  etiqueta: string;
  valor: string | null;
}) {
  return (
    <div className="rounded-[16px] border border-[var(--color-border-soft)] bg-white/80 px-3 py-2">
      <dt className="text-[0.68rem] font-extrabold uppercase tracking-[0.14em] text-[var(--color-secondary)]">
        {etiqueta}
      </dt>
      <dd className="mt-1 text-sm font-bold text-[var(--color-primary)]">
        {valor ?? "No informado"}
      </dd>
    </div>
  );
}

function obtenerSlugPublico(actividad: ActividadPublicadorResumen): string | null {
  return actividad.slugPublico?.trim() || actividad.slug.trim() || null;
}

function obtenerUbicacion(actividad: ActividadPublicadorResumen): string {
  return (
    [actividad.ciudadNombre, actividad.barrioNombre].filter(Boolean).join(", ") ||
    "Ubicación no informada"
  );
}

function formatearEdades(actividad: ActividadPublicadorResumen): string | null {
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

function formatearPrecio(actividad: ActividadPublicadorResumen): string | null {
  if (!actividad.mostrarPrecio || actividad.precioReferencia === null) {
    return null;
  }

  return new Intl.NumberFormat("es-AR", {
    style: "currency",
    currency: "ARS",
    maximumFractionDigits: 0,
  }).format(actividad.precioReferencia);
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

function formatearFecha(valor: string): string {
  const fecha = new Date(valor);

  if (Number.isNaN(fecha.getTime())) {
    return "fecha no disponible";
  }

  return new Intl.DateTimeFormat("es-AR", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
  }).format(fecha);
}
