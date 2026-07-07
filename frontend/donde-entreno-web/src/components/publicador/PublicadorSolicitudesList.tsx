"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { useAuthSession } from "../auth/AuthSessionProvider";
import { AppButton } from "../ui/AppButton";
import { AppLinkButton } from "../ui/AppLinkButton";
import { SectionHeader } from "../ui/SectionHeader";
import { StatusMessage } from "../ui/StatusMessage";
import { SurfaceCard } from "../ui/SurfaceCard";
import { PublicadorPageHeader } from "./PublicadorPageHeader";
import {
  PublicadorApiError,
  listarSolicitudesPublicador,
} from "../../services/publicadorService";
import { PublicadorSolicitudEstadoBadge } from "./PublicadorSolicitudEstadoBadge";
import type {
  SolicitudPublicadorResumen,
  SolicitudesPublicadorPage,
} from "../../types/publicador";
import type { EstadoSolicitudPublicacion } from "../../types/solicitudPublicacion";
import type { ChangeEvent } from "react";

const TAMANIO_PAGINA = 10;

type EstadoFiltro = EstadoSolicitudPublicacion | "";

const FILTROS_ESTADO: Array<{ valor: EstadoFiltro; etiqueta: string }> = [
  { valor: "", etiqueta: "Todas" },
  { valor: "PENDIENTE", etiqueta: "Pendiente" },
  { valor: "EN_REVISION", etiqueta: "En revisión" },
  { valor: "APROBADA", etiqueta: "Aprobada" },
  { valor: "RECHAZADA", etiqueta: "Rechazada" },
];

export function PublicadorSolicitudesList() {
  const router = useRouter();
  const { accessToken, cerrarSesion } = useAuthSession();
  const [paginaSolicitudes, setPaginaSolicitudes] =
    useState<SolicitudesPublicadorPage | null>(null);
  const [filtroEstado, setFiltroEstado] = useState<EstadoFiltro>("");
  const [paginaActual, setPaginaActual] = useState(0);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let componenteActivo = true;

    if (!accessToken) {
      return () => {
        componenteActivo = false;
      };
    }

    listarSolicitudesPublicador(
      {
        estado: filtroEstado,
        page: paginaActual,
        size: TAMANIO_PAGINA,
        orden: "recientes",
      },
      accessToken
    )
      .then((pagina) => {
        if (!componenteActivo) {
          return;
        }

        setPaginaSolicitudes(pagina);
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
                "/publicador/solicitudes"
              )}`
            );
            return;
          }

          if (errorCarga.status === 403) {
            setError("No tenés permisos para acceder al panel publicador.");
            return;
          }

          setError(errorCarga.message);
          return;
        }

        setError("Ocurrió un problema inesperado al cargar tus solicitudes.");
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
  }, [accessToken, cerrarSesion, filtroEstado, paginaActual, router]);

  function cambiarFiltroEstado(evento: ChangeEvent<HTMLSelectElement>) {
    setCargando(true);
    setError(null);
    setFiltroEstado(evento.target.value as EstadoFiltro);
    setPaginaActual(0);
  }

  function irPaginaAnterior() {
    setCargando(true);
    setError(null);
    setPaginaActual((pagina) => Math.max(pagina - 1, 0));
  }

  function irPaginaSiguiente() {
    if (paginaSolicitudes?.ultima) {
      return;
    }

    setCargando(true);
    setError(null);
    setPaginaActual((pagina) => pagina + 1);
  }

  const solicitudes = paginaSolicitudes?.contenido ?? [];
  const puedeIrAnterior = !cargando && paginaActual > 0;
  const puedeIrSiguiente =
    !cargando && Boolean(paginaSolicitudes) && !paginaSolicitudes?.ultima;

  return (
    <main className="min-h-screen bg-gradient-to-br from-[#F8FAFC] via-white to-[#E8F6FB] px-4 py-8 text-[var(--color-text)] sm:py-12">
      <section className="mx-auto w-full max-w-6xl">
        <PublicadorPageHeader
          title="Seguimiento de solicitudes"
          description="Revisá en qué estado está cada actividad que enviaste y volvé al detalle cuando necesites consultar datos."
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

        <SurfaceCard className="mt-6 border-[#BDE8D0] bg-gradient-to-br from-white via-white to-[#F8FCFE] p-5 sm:p-6">
          <div className="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
            <SectionHeader
              eyebrow="Solicitudes"
              title="Actividades enviadas"
              description="Filtrá por estado y abrí cada solicitud para ver su información completa."
            />

            <div className="w-full lg:w-64">
              <label
                htmlFor="filtro-estado-publicador"
                className="text-sm font-bold text-[var(--color-primary)]"
              >
                Filtrar por estado
              </label>
              <select
                id="filtro-estado-publicador"
                value={filtroEstado}
                onChange={cambiarFiltroEstado}
                disabled={cargando}
                className="mt-2 min-h-11 w-full rounded-[16px] border border-[#BFDDEA] bg-white px-4 text-sm font-bold text-[var(--color-primary)] outline-none transition focus:border-[var(--color-accent)] focus:ring-4 focus:ring-[#DDEAF3] disabled:cursor-not-allowed disabled:opacity-70"
              >
                {FILTROS_ESTADO.map((opcion) => (
                  <option key={opcion.valor || "todas"} value={opcion.valor}>
                    {opcion.etiqueta}
                  </option>
                ))}
              </select>
            </div>
          </div>

          {cargando ? (
            <StatusMessage variant="info" role="status" className="mt-6">
              Cargando solicitudes...
            </StatusMessage>
          ) : null}

          {error ? (
            <StatusMessage variant="error" role="alert" className="mt-6">
              {error}
            </StatusMessage>
          ) : null}

          {!cargando && !error && solicitudes.length === 0 ? (
            <div className="mt-6 rounded-[var(--radius-lg)] border border-[#BDE8D0] bg-[#E6F7EF] p-4 text-sm leading-6 text-[#167A4A]">
              <p className="font-extrabold text-[var(--color-primary)]">
                No hay solicitudes para mostrar.
              </p>
              <p className="mt-2">
                Cuando cargues una actividad desde tu panel, vas a poder seguir
                el estado de revisión desde acá.
              </p>
              <AppLinkButton
                href="/publicador/solicitudes/nueva"
                size="sm"
                className="mt-4"
              >
                Crear nueva solicitud
              </AppLinkButton>
            </div>
          ) : null}

          {solicitudes.length > 0 ? (
            <div className="mt-6 grid gap-4">
              {solicitudes.map((solicitud) => (
                <SolicitudCard key={solicitud.id} solicitud={solicitud} />
              ))}
            </div>
          ) : null}

          {paginaSolicitudes ? (
            <div className="mt-6 flex flex-col gap-3 border-t border-[#DDEAF3] pt-5 sm:flex-row sm:items-center sm:justify-between">
              <p className="text-sm font-bold text-[var(--color-muted)]">
                Página {paginaSolicitudes.paginaActual + 1} de{" "}
                {Math.max(paginaSolicitudes.totalPaginas, 1)}
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

function SolicitudCard({
  solicitud,
}: {
  solicitud: SolicitudPublicadorResumen;
}) {
  return (
    <article className="rounded-[22px] border border-[#DDEAF3] bg-gradient-to-br from-white to-[#F8FCFE] p-4 shadow-sm transition duration-200 ease-out hover:-translate-y-0.5 hover:border-[#BDE8D0] hover:shadow-[0_18px_45px_rgba(12,52,80,0.10)] sm:p-5">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
        <div className="min-w-0">
          <p className="text-xs font-extrabold uppercase tracking-[0.16em] text-[var(--color-secondary)]">
            {solicitud.codigoSeguimiento}
          </p>
          <h2 className="mt-2 text-xl font-extrabold text-[var(--color-primary)]">
            {solicitud.nombreActividad}
          </h2>
          <p className="mt-2 text-sm leading-6 text-[var(--color-muted)]">
            {obtenerDeporte(solicitud)} · {obtenerUbicacion(solicitud)}
          </p>
        </div>
        <PublicadorSolicitudEstadoBadge estado={solicitud.estado} size="sm" />
      </div>

      {solicitud.motivoRechazo ? (
        <StatusMessage variant="error" className="mt-4">
          {solicitud.motivoRechazo}
        </StatusMessage>
      ) : null}

      <div className="mt-5 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <p className="text-sm font-bold text-[var(--color-muted)]">
          Enviada el {formatearFecha(solicitud.createdAt)}
        </p>
        <Link
          href={`/publicador/solicitudes/${solicitud.id}`}
          className="inline-flex items-center justify-center rounded-[18px] border border-[#BDE8D0] bg-[#E6F7EF] px-4 py-2 text-sm font-extrabold text-[#167A4A] transition duration-200 ease-out hover:-translate-y-0.5 hover:border-[#2EB872] hover:bg-white"
        >
          Ver detalle
        </Link>
      </div>
    </article>
  );
}

function obtenerDeporte(solicitud: SolicitudPublicadorResumen): string {
  return solicitud.deporteNombre ?? solicitud.deporteOtro ?? "Deporte no informado";
}

function obtenerUbicacion(solicitud: SolicitudPublicadorResumen): string {
  const ciudad = solicitud.ciudadNombre ?? solicitud.ciudadOtra;
  const barrio = solicitud.barrioNombre ?? solicitud.barrioOtro;

  return [ciudad, barrio].filter(Boolean).join(", ") || "Ubicación no informada";
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
