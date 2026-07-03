"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { AdminGuard } from "../../../components/admin/AdminGuard";
import { AdminEstadoBadge } from "../../../components/admin/AdminEstadoBadge";
import { BrandName } from "../../../components/brand/BrandName";
import { AppButton } from "../../../components/ui/AppButton";
import { AppLinkButton } from "../../../components/ui/AppLinkButton";
import { StatusMessage } from "../../../components/ui/StatusMessage";
import { SurfaceCard } from "../../../components/ui/SurfaceCard";
import {
  cerrarSesionAdmin,
  obtenerSesionAdmin,
} from "../../../services/authService";
import {
  AdminApiError,
  listarSolicitudesAdmin,
} from "../../../services/adminSolicitudesService";
import type { AdminSesion } from "../../../types/auth";
import type {
  EstadoSolicitudAdmin,
  SolicitudPublicacionAdminResumen,
  SolicitudesPublicacionAdminPage,
} from "../../../types/adminSolicitudes";
import type { ChangeEvent } from "react";

const TAMANIO_PAGINA = 10;

type EstadoFiltro = EstadoSolicitudAdmin | "";

type OpcionFiltroEstado = {
  valor: EstadoFiltro;
  etiqueta: string;
};

type ResultadoCargaSolicitudes =
  | {
      tipo: "ok";
      sesion: AdminSesion;
      paginaSolicitudes: SolicitudesPublicacionAdminPage;
    }
  | {
      tipo: "sinSesion";
    };

const FILTROS_ESTADO: OpcionFiltroEstado[] = [
  {
    valor: "",
    etiqueta: "Todas",
  },
  {
    valor: "PENDIENTE",
    etiqueta: "Pendiente",
  },
  {
    valor: "EN_REVISION",
    etiqueta: "En revisión",
  },
  {
    valor: "RECHAZADA",
    etiqueta: "Rechazada",
  },
  {
    valor: "APROBADA",
    etiqueta: "Aprobada",
  },
];

export default function AdminSolicitudesPage() {
  return (
    <AdminGuard>
      <AdminSolicitudesListado />
    </AdminGuard>
  );
}

function AdminSolicitudesListado() {
  const router = useRouter();
  const [sesion, setSesion] = useState<AdminSesion | null>(null);
  const [paginaSolicitudes, setPaginaSolicitudes] =
    useState<SolicitudesPublicacionAdminPage | null>(null);
  const [filtroEstado, setFiltroEstado] = useState<EstadoFiltro>("");
  const [paginaActual, setPaginaActual] = useState(0);
  const [cargandoSolicitudes, setCargandoSolicitudes] = useState(true);
  const [errorSolicitudes, setErrorSolicitudes] = useState<string | null>(null);

  useEffect(() => {
    let componenteActivo = true;

    void cargarSolicitudesDesdeSesion(filtroEstado, paginaActual)
      .then((resultado) => {
        if (!componenteActivo) {
          return;
        }

        if (resultado.tipo === "sinSesion") {
          cerrarSesionAdmin();
          router.replace("/admin/login");
          return;
        }

        setSesion(resultado.sesion);
        setPaginaSolicitudes(resultado.paginaSolicitudes);
        setErrorSolicitudes(null);
      })
      .catch((error: unknown) => {
        if (!componenteActivo) {
          return;
        }

        if (error instanceof AdminApiError) {
          if (error.status === 401) {
            cerrarSesionAdmin();
            router.replace("/admin/login");
            return;
          }

          if (error.status === 403) {
            setErrorSolicitudes(
              "No tenés permisos para acceder al panel administrador."
            );
            return;
          }

          setErrorSolicitudes(error.message);
          return;
        }

        setErrorSolicitudes(
          "Ocurrió un problema inesperado al cargar las solicitudes."
        );
      })
      .finally(() => {
        if (!componenteActivo) {
          return;
        }

        setCargandoSolicitudes(false);
      });

    return () => {
      componenteActivo = false;
    };
  }, [filtroEstado, paginaActual, router]);

  function cerrarSesion() {
    cerrarSesionAdmin();
    router.replace("/admin/login");
  }

  function cambiarFiltroEstado(evento: ChangeEvent<HTMLSelectElement>) {
    setCargandoSolicitudes(true);
    setErrorSolicitudes(null);
    setFiltroEstado(evento.target.value as EstadoFiltro);
    setPaginaActual(0);
  }

  function irPaginaAnterior() {
    setCargandoSolicitudes(true);
    setErrorSolicitudes(null);
    setPaginaActual((pagina) => Math.max(pagina - 1, 0));
  }

  function irPaginaSiguiente() {
    if (paginaSolicitudes?.ultima) {
      return;
    }

    setCargandoSolicitudes(true);
    setErrorSolicitudes(null);
    setPaginaActual((pagina) => pagina + 1);
  }

  const solicitudes = paginaSolicitudes?.contenido ?? [];
  const puedeIrAnterior = !cargandoSolicitudes && paginaActual > 0;
  const puedeIrSiguiente =
    !cargandoSolicitudes && Boolean(paginaSolicitudes) && !paginaSolicitudes?.ultima;

  return (
    <main className="min-h-screen bg-gradient-to-br from-[#F8FAFC] via-white to-[#E8F6FB] px-4 py-6 text-[var(--color-text)] sm:py-10">
      <section className="mx-auto w-full max-w-6xl">
        <SurfaceCard className="mb-6 overflow-hidden rounded-[28px] shadow-[0_24px_65px_rgba(12,52,80,0.12)]">
          <div className="bg-gradient-to-br from-white via-[#F8FCFE] to-[#E6F7EF] p-5 sm:p-7">
            <div className="flex flex-col gap-5 lg:flex-row lg:items-start lg:justify-between">
              <div>
                <p className="text-xs font-extrabold uppercase tracking-[0.2em] text-[var(--color-secondary)]">
                  PANEL ADMINISTRADOR
                </p>
                <h1 className="mt-2 text-3xl font-extrabold leading-tight text-[var(--color-primary)] sm:text-4xl">
                  Solicitudes de publicación
                </h1>
                <p className="mt-3 max-w-2xl text-sm leading-6 text-[var(--color-muted)] sm:text-base">
                  Revisá, filtrá y gestioná las actividades enviadas para
                  publicarse en <BrandName className="inline font-bold" />.
                </p>
              </div>

              <div className="flex flex-col gap-3 sm:flex-row lg:flex-col lg:items-end">
                {sesion && (
                  <div className="rounded-[18px] border border-[#DDEAF3] bg-white/85 px-4 py-3 shadow-sm">
                    <p className="text-xs font-extrabold uppercase tracking-[0.14em] text-[var(--color-muted)]">
                      Usuario conectado
                    </p>
                    <p className="mt-1 text-sm font-extrabold text-[var(--color-primary)]">
                      {sesion.usuario.nombre} {sesion.usuario.apellido}
                    </p>
                    <p className="mt-1 text-xs font-bold text-[var(--color-muted)]">
                      {sesion.usuario.email}
                    </p>
                  </div>
                )}

                <AppButton
                  type="button"
                  onClick={cerrarSesion}
                  variant="secondary"
                >
                  Cerrar sesión
                </AppButton>
              </div>
            </div>
          </div>
        </SurfaceCard>

        <SurfaceCard className="mb-5 rounded-[24px] bg-white/90 p-5">
          <div className="grid gap-4 sm:grid-cols-[1fr_auto] sm:items-end">
            <div>
              <p className="text-xs font-extrabold uppercase tracking-[0.16em] text-[var(--color-secondary)]">
                Filtros
              </p>
              <label
                htmlFor="filtro-estado"
                className="mt-2 block text-sm font-bold text-[var(--color-primary)]"
              >
                Filtrar por estado
              </label>
              <select
                id="filtro-estado"
                value={filtroEstado}
                onChange={cambiarFiltroEstado}
                disabled={cargandoSolicitudes}
                className="mt-2 min-h-12 w-full rounded-[18px] border border-[#BFDDEA] bg-[#F8FAFC] px-4 text-sm font-bold text-[var(--color-text)] outline-none transition duration-200 ease-out hover:border-[var(--color-accent)] focus:border-[var(--color-accent)] focus:ring-4 focus:ring-[#DDEAF3] disabled:cursor-not-allowed disabled:opacity-70"
              >
                {FILTROS_ESTADO.map((opcion) => (
                  <option key={opcion.etiqueta} value={opcion.valor}>
                    {opcion.etiqueta}
                  </option>
                ))}
              </select>
            </div>

            <p className="rounded-full border border-[#DDEAF3] bg-[#F8FCFE] px-4 py-2 text-sm font-bold text-[var(--color-muted)]">
              Orden: recientes
            </p>
          </div>
        </SurfaceCard>

        {cargandoSolicitudes && (
          <div
            role="status"
            className="rounded-[24px] border border-[#DDEAF3] bg-white p-7 text-center shadow-[0_14px_35px_rgba(12,52,80,0.08)]"
          >
            <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-full bg-[#E8F6FB]">
              <div className="h-8 w-8 animate-spin rounded-full border-4 border-[#BFDDEA] border-t-[var(--color-secondary)]" />
            </div>
            <p className="mt-4 text-sm font-bold text-[var(--color-primary)]">
              Cargando solicitudes...
            </p>
          </div>
        )}

        {!cargandoSolicitudes && errorSolicitudes && (
          <StatusMessage
            variant="error"
            title="No pudimos cargar las solicitudes"
            className="p-6"
          >
            <p className="mt-3">
              {errorSolicitudes}
            </p>
            <p className="mt-2 font-bold">
              Intentá nuevamente en unos minutos.
            </p>
          </StatusMessage>
        )}

        {!cargandoSolicitudes && !errorSolicitudes && solicitudes.length === 0 && (
          <StatusMessage
            variant="info"
            title="No hay solicitudes para mostrar."
            className="p-7 text-center"
          >
            <p className="mx-auto max-w-xl">
              Cambiá el filtro de estado o volvé a intentar cuando existan
              solicitudes nuevas.
            </p>
          </StatusMessage>
        )}

        {!cargandoSolicitudes && !errorSolicitudes && solicitudes.length > 0 && (
          <>
            <ListadoSolicitudes solicitudes={solicitudes} />

            <SurfaceCard className="mt-5 flex flex-col gap-3 rounded-[24px] bg-white/90 p-4 sm:flex-row sm:items-center sm:justify-between">
              <p className="text-sm font-bold text-[var(--color-muted)]">
                Página {(paginaSolicitudes?.paginaActual ?? paginaActual) + 1}{" "}
                de {paginaSolicitudes?.totalPaginas ?? 1} ·{" "}
                {paginaSolicitudes?.totalElementos ?? solicitudes.length}{" "}
                solicitudes
              </p>

              <div className="flex gap-3">
                <AppButton
                  type="button"
                  onClick={irPaginaAnterior}
                  disabled={!puedeIrAnterior}
                  variant="secondary"
                  className="flex-1 sm:flex-none"
                >
                  Anterior
                </AppButton>
                <AppButton
                  type="button"
                  onClick={irPaginaSiguiente}
                  disabled={!puedeIrSiguiente}
                  variant="primary"
                  className="flex-1 sm:flex-none"
                >
                  Siguiente
                </AppButton>
              </div>
            </SurfaceCard>
          </>
        )}
      </section>
    </main>
  );
}

function ListadoSolicitudes({
  solicitudes,
}: {
  solicitudes: SolicitudPublicacionAdminResumen[];
}) {
  return (
    <SurfaceCard className="overflow-hidden rounded-[24px]">
      <div className="hidden overflow-x-auto lg:block">
        <table className="w-full border-collapse text-left text-sm">
          <thead className="bg-[#F8FCFE] text-xs uppercase tracking-[0.08em] text-[var(--color-muted)]">
            <tr>
              <th className="px-5 py-4 font-extrabold">Código</th>
              <th className="px-5 py-4 font-extrabold">Estado</th>
              <th className="px-5 py-4 font-extrabold">Actividad</th>
              <th className="px-5 py-4 font-extrabold">Publicador</th>
              <th className="px-5 py-4 font-extrabold">Deporte</th>
              <th className="px-5 py-4 font-extrabold">Ubicación</th>
              <th className="px-5 py-4 font-extrabold">Creación</th>
              <th className="px-5 py-4 font-extrabold">Acción</th>
            </tr>
          </thead>
          <tbody>
            {solicitudes.map((solicitud) => (
              <tr
                key={solicitud.id}
                className="border-t border-[#DDEAF3] transition duration-200 ease-out hover:bg-[#F8FCFE]"
              >
                <td className="px-5 py-5 font-extrabold text-[var(--color-primary)]">
                  {solicitud.codigoSeguimiento}
                </td>
                <td className="px-5 py-5">
                  <AdminEstadoBadge estado={solicitud.estado} size="sm" />
                </td>
                <td className="px-5 py-5 font-extrabold text-[var(--color-text)]">
                  {solicitud.nombreActividad}
                </td>
                <td className="px-5 py-5 text-[var(--color-muted)]">
                  {solicitud.nombrePublicador}
                </td>
                <td className="px-5 py-5 text-[var(--color-muted)]">
                  {obtenerDeporte(solicitud)}
                </td>
                <td className="px-5 py-5 text-[var(--color-muted)]">
                  {obtenerUbicacion(solicitud)}
                </td>
                <td className="px-5 py-5 text-[var(--color-muted)]">
                  {formatearFecha(solicitud.createdAt)}
                </td>
                <td className="px-5 py-5">
                  <BotonDetalleSolicitud id={solicitud.id} />
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div className="divide-y divide-[#DDEAF3] lg:hidden">
        {solicitudes.map((solicitud) => (
          <article
            key={solicitud.id}
            className="p-5 transition duration-200 ease-out hover:bg-[#F8FCFE]"
          >
            <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
              <div>
                <p className="text-xs font-extrabold uppercase tracking-[0.16em] text-[var(--color-muted)]">
                  {solicitud.codigoSeguimiento}
                </p>
                <h2 className="mt-2 text-xl font-extrabold leading-tight text-[var(--color-primary)]">
                  {solicitud.nombreActividad}
                </h2>
              </div>
              <AdminEstadoBadge estado={solicitud.estado} size="sm" />
            </div>

            <dl className="mt-4 grid gap-3 text-sm">
              <div>
                <dt className="font-bold text-[var(--color-primary)]">
                  Publicador
                </dt>
                <dd className="mt-1 text-[var(--color-muted)]">
                  {solicitud.nombrePublicador}
                </dd>
              </div>
              <div>
                <dt className="font-bold text-[var(--color-primary)]">
                  Deporte
                </dt>
                <dd className="mt-1 text-[var(--color-muted)]">
                  {obtenerDeporte(solicitud)}
                </dd>
              </div>
              <div>
                <dt className="font-bold text-[var(--color-primary)]">
                  Ubicación
                </dt>
                <dd className="mt-1 text-[var(--color-muted)]">
                  {obtenerUbicacion(solicitud)}
                </dd>
              </div>
              <div>
                <dt className="font-bold text-[var(--color-primary)]">
                  Creación
                </dt>
                <dd className="mt-1 text-[var(--color-muted)]">
                  {formatearFecha(solicitud.createdAt)}
                </dd>
              </div>
            </dl>

            <div className="mt-5">
              <BotonDetalleSolicitud id={solicitud.id} />
            </div>
          </article>
        ))}
      </div>
    </SurfaceCard>
  );
}

function BotonDetalleSolicitud({ id }: { id: number }) {
  return (
    <AppLinkButton
      href={`/admin/solicitudes/${id}`}
      variant="secondary"
      size="sm"
      className="rounded-[16px]"
    >
      Ver detalle
    </AppLinkButton>
  );
}

function obtenerDeporte(solicitud: SolicitudPublicacionAdminResumen): string {
  return solicitud.deporteNombre ?? solicitud.deporteOtro ?? "Sin deporte";
}

function obtenerUbicacion(
  solicitud: SolicitudPublicacionAdminResumen
): string {
  const ciudad = solicitud.ciudadNombre ?? solicitud.ciudadOtra;
  const barrio = solicitud.barrioNombre ?? solicitud.barrioOtro;

  if (ciudad && barrio) {
    return `${ciudad} · ${barrio}`;
  }

  return ciudad ?? barrio ?? "Sin ubicación";
}

function formatearFecha(fechaIso: string): string {
  const fecha = new Date(fechaIso);

  if (Number.isNaN(fecha.getTime())) {
    return fechaIso;
  }

  return new Intl.DateTimeFormat("es-AR", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  }).format(fecha);
}

async function cargarSolicitudesDesdeSesion(
  estado: EstadoFiltro,
  pagina: number
): Promise<ResultadoCargaSolicitudes> {
  const sesion = obtenerSesionAdmin();

  if (!sesion) {
    return {
      tipo: "sinSesion",
    };
  }

  const paginaSolicitudes = await listarSolicitudesAdmin(
    {
      estado,
      page: pagina,
      size: TAMANIO_PAGINA,
      orden: "recientes",
    },
    sesion.accessToken
  );

  return {
    tipo: "ok",
    sesion,
    paginaSolicitudes,
  };
}
