"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { AdminGuard } from "../../../components/admin/AdminGuard";
import { AdminEstadoBadge } from "../../../components/admin/AdminEstadoBadge";
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
        <div className="mb-6 flex flex-col gap-4 rounded-[var(--radius-xl)] border border-[#DDEAF3] bg-gradient-to-br from-white to-[#F8FCFE] p-5 shadow-[0_18px_45px_rgba(12,52,80,0.10)] sm:flex-row sm:items-center sm:justify-between">
          <div>
            <p className="text-xs font-extrabold uppercase tracking-[0.18em] text-[var(--color-secondary)]">
              Panel administrador
            </p>
            <h1 className="mt-2 text-2xl font-extrabold text-[var(--color-primary)] sm:text-3xl">
              Solicitudes de publicación
            </h1>
            {sesion && (
              <p className="mt-2 text-sm leading-6 text-[var(--color-muted)]">
                {sesion.usuario.nombre} {sesion.usuario.apellido} ·{" "}
                {sesion.usuario.email} · {sesion.usuario.rol}
              </p>
            )}
          </div>

          <button
            type="button"
            onClick={cerrarSesion}
            className="rounded-[var(--radius-md)] border border-[var(--color-border)] px-4 py-3 text-sm font-bold text-[var(--color-primary)] transition duration-200 ease-out hover:-translate-y-0.5 hover:border-[var(--color-primary)] hover:bg-white active:scale-[0.98]"
          >
            Cerrar sesión
          </button>
        </div>

        <div className="mb-5 rounded-[var(--radius-xl)] border border-[var(--color-border)] bg-[var(--color-surface)] p-5 shadow-[0_14px_35px_rgba(12,52,80,0.08)]">
          <div className="grid gap-4 sm:grid-cols-[1fr_auto] sm:items-end">
            <div>
              <label
                htmlFor="filtro-estado"
                className="text-sm font-bold text-[var(--color-primary)]"
              >
                Filtrar por estado
              </label>
              <select
                id="filtro-estado"
                value={filtroEstado}
                onChange={cambiarFiltroEstado}
                disabled={cargandoSolicitudes}
                className="mt-2 w-full rounded-[var(--radius-md)] border border-[var(--color-border)] bg-white px-4 py-3 text-sm font-bold text-[var(--color-text)] outline-none transition duration-200 ease-out hover:border-[#BFDDEA] focus:border-[var(--color-accent)] focus:ring-2 focus:ring-[#DDEAF3] disabled:cursor-not-allowed disabled:opacity-70"
              >
                {FILTROS_ESTADO.map((opcion) => (
                  <option key={opcion.etiqueta} value={opcion.valor}>
                    {opcion.etiqueta}
                  </option>
                ))}
              </select>
            </div>

            <p className="text-sm font-bold text-[var(--color-muted)]">
              Orden: recientes
            </p>
          </div>
        </div>

        {cargandoSolicitudes && (
          <div
            role="status"
            className="rounded-[var(--radius-xl)] border border-[var(--color-border)] bg-[var(--color-surface)] p-6 text-center shadow-[0_14px_35px_rgba(12,52,80,0.08)]"
          >
            <div className="mx-auto h-9 w-9 animate-spin rounded-full border-4 border-[#DDEAF3] border-t-[var(--color-secondary)]" />
            <p className="mt-4 text-sm font-bold text-[var(--color-primary)]">
              Cargando solicitudes...
            </p>
          </div>
        )}

        {!cargandoSolicitudes && errorSolicitudes && (
          <div
            role="alert"
            className="rounded-[var(--radius-xl)] border border-red-200 bg-red-50 p-6 shadow-[0_14px_35px_rgba(127,29,29,0.08)]"
          >
            <h2 className="text-xl font-extrabold text-red-700">
              No pudimos cargar las solicitudes
            </h2>
            <p className="mt-3 text-sm leading-6 text-red-700">
              {errorSolicitudes}
            </p>
          </div>
        )}

        {!cargandoSolicitudes && !errorSolicitudes && solicitudes.length === 0 && (
          <div className="rounded-[var(--radius-xl)] border border-[var(--color-border)] bg-[var(--color-surface)] p-6 text-center shadow-[0_14px_35px_rgba(12,52,80,0.08)]">
            <h2 className="text-xl font-extrabold text-[var(--color-primary)]">
              No hay solicitudes para mostrar.
            </h2>
            <p className="mx-auto mt-3 max-w-xl text-sm leading-6 text-[var(--color-muted)]">
              Cambiá el filtro de estado o volvé a intentar cuando existan
              solicitudes nuevas.
            </p>
          </div>
        )}

        {!cargandoSolicitudes && !errorSolicitudes && solicitudes.length > 0 && (
          <>
            <ListadoSolicitudes solicitudes={solicitudes} />

            <div className="mt-5 flex flex-col gap-3 rounded-[var(--radius-xl)] border border-[var(--color-border)] bg-[var(--color-surface)] p-4 shadow-[0_14px_35px_rgba(12,52,80,0.08)] sm:flex-row sm:items-center sm:justify-between">
              <p className="text-sm font-bold text-[var(--color-muted)]">
                Página {(paginaSolicitudes?.paginaActual ?? paginaActual) + 1}{" "}
                de {paginaSolicitudes?.totalPaginas ?? 1} ·{" "}
                {paginaSolicitudes?.totalElementos ?? solicitudes.length}{" "}
                solicitudes
              </p>

              <div className="flex gap-3">
                <button
                  type="button"
                  onClick={irPaginaAnterior}
                  disabled={!puedeIrAnterior}
                  className="flex-1 rounded-[var(--radius-md)] border border-[var(--color-border)] px-4 py-3 text-sm font-bold text-[var(--color-primary)] transition duration-200 ease-out hover:-translate-y-0.5 hover:border-[var(--color-primary)] active:scale-[0.98] disabled:cursor-not-allowed disabled:opacity-50 disabled:hover:translate-y-0 sm:flex-none"
                >
                  Anterior
                </button>
                <button
                  type="button"
                  onClick={irPaginaSiguiente}
                  disabled={!puedeIrSiguiente}
                  className="flex-1 rounded-[var(--radius-md)] bg-[var(--color-primary)] px-4 py-3 text-sm font-bold text-white shadow-[var(--shadow-button)] transition duration-200 ease-out hover:-translate-y-0.5 hover:bg-[#0B314D] active:scale-[0.98] disabled:cursor-not-allowed disabled:opacity-50 disabled:hover:translate-y-0 sm:flex-none"
                >
                  Siguiente
                </button>
              </div>
            </div>
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
    <div className="overflow-hidden rounded-[var(--radius-xl)] border border-[var(--color-border)] bg-[var(--color-surface)] shadow-[0_18px_45px_rgba(12,52,80,0.10)]">
      <div className="hidden overflow-x-auto lg:block">
        <table className="w-full border-collapse text-left text-sm">
          <thead className="bg-[#F8FCFE] text-xs uppercase text-[var(--color-muted)]">
            <tr>
              <th className="px-4 py-4 font-extrabold">Código</th>
              <th className="px-4 py-4 font-extrabold">Estado</th>
              <th className="px-4 py-4 font-extrabold">Actividad</th>
              <th className="px-4 py-4 font-extrabold">Publicador</th>
              <th className="px-4 py-4 font-extrabold">Deporte</th>
              <th className="px-4 py-4 font-extrabold">Ubicación</th>
              <th className="px-4 py-4 font-extrabold">Creación</th>
              <th className="px-4 py-4 font-extrabold">Acción</th>
            </tr>
          </thead>
          <tbody>
            {solicitudes.map((solicitud) => (
              <tr
                key={solicitud.id}
                className="border-t border-[var(--color-border)] transition duration-200 ease-out hover:bg-[#F8FCFE]"
              >
                <td className="px-4 py-4 font-bold text-[var(--color-primary)]">
                  {solicitud.codigoSeguimiento}
                </td>
                <td className="px-4 py-4">
                  <AdminEstadoBadge estado={solicitud.estado} size="sm" />
                </td>
                <td className="px-4 py-4 font-bold text-[var(--color-text)]">
                  {solicitud.nombreActividad}
                </td>
                <td className="px-4 py-4 text-[var(--color-muted)]">
                  {solicitud.nombrePublicador}
                </td>
                <td className="px-4 py-4 text-[var(--color-muted)]">
                  {obtenerDeporte(solicitud)}
                </td>
                <td className="px-4 py-4 text-[var(--color-muted)]">
                  {obtenerUbicacion(solicitud)}
                </td>
                <td className="px-4 py-4 text-[var(--color-muted)]">
                  {formatearFecha(solicitud.createdAt)}
                </td>
                <td className="px-4 py-4">
                  <BotonDetalleSolicitud id={solicitud.id} />
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div className="divide-y divide-[var(--color-border)] lg:hidden">
        {solicitudes.map((solicitud) => (
          <article key={solicitud.id} className="p-5 transition duration-200 ease-out hover:bg-[#F8FCFE]">
            <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
              <div>
                <p className="text-xs font-extrabold uppercase tracking-[0.16em] text-[var(--color-muted)]">
                  {solicitud.codigoSeguimiento}
                </p>
                <h2 className="mt-2 text-lg font-extrabold text-[var(--color-primary)]">
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
    </div>
  );
}

function BotonDetalleSolicitud({ id }: { id: number }) {
  return (
    <Link
      href={`/admin/solicitudes/${id}`}
      className="inline-flex rounded-[var(--radius-md)] border border-[var(--color-border)] px-3 py-2 text-xs font-bold text-[var(--color-primary)] transition duration-200 ease-out hover:-translate-y-0.5 hover:border-[var(--color-primary)] hover:bg-[#F8FCFE] active:scale-[0.98]"
    >
      Ver detalle
    </Link>
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
