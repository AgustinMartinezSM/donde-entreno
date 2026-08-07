"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import type { ChangeEvent } from "react";

import { AdminGuard } from "../../../components/admin/AdminGuard";
import { useAuthSession } from "../../../components/auth/AuthSessionProvider";
import { AppLinkButton } from "../../../components/ui/AppLinkButton";
import { StatusMessage } from "../../../components/ui/StatusMessage";
import { SurfaceCard } from "../../../components/ui/SurfaceCard";
import { obtenerSesionAdmin } from "../../../services/authService";
import {
  AdminApiError,
  listarSolicitudesCambioAdmin,
} from "../../../services/adminSolicitudesService";
import type { SolicitudCambioResumen } from "../../../types/publicador";

const ETIQUETAS_CAMPOS: Record<string, string> = {
  titulo: "Título",
  descripcion: "Descripción",
  precioReferencia: "Precio",
  mostrarPrecio: "Mostrar precio",
  whatsappContacto: "WhatsApp",
  instagramContacto: "Instagram",
  emailContacto: "Email",
  nivel: "Nivel",
  modalidad: "Modalidad",
};

const ESTILOS_ESTADO: Record<string, string> = {
  PENDIENTE: "bg-[#FFF7E6] text-[#8A5A00] ring-1 ring-[#F5D48F]",
  EN_REVISION: "bg-[#E8F6FB] text-[#0F6F8F] ring-1 ring-[#BFDDEA]",
  APROBADA: "bg-[#E6F7EF] text-[#167A4A] ring-1 ring-[#BDE8D0]",
  RECHAZADA: "bg-red-50 text-red-700 ring-1 ring-red-200",
};

const FILTROS_ESTADO = [
  { valor: "", etiqueta: "Todas" },
  { valor: "PENDIENTE", etiqueta: "Pendiente" },
  { valor: "EN_REVISION", etiqueta: "En revisión" },
  { valor: "APROBADA", etiqueta: "Aprobada" },
  { valor: "RECHAZADA", etiqueta: "Rechazada" },
];

function formatearEstado(estado: string): string {
  return estado
    .toLowerCase()
    .split("_")
    .map((parte) => parte.charAt(0).toUpperCase() + parte.slice(1))
    .join(" ");
}

export default function AdminSolicitudesCambioPage() {
  return (
    <AdminGuard>
      <AdminSolicitudesCambioListado />
    </AdminGuard>
  );
}

function AdminSolicitudesCambioListado() {
  const router = useRouter();
  const { cerrarSesion } = useAuthSession();
  const [solicitudes, setSolicitudes] = useState<SolicitudCambioResumen[]>([]);
  const [filtroEstado, setFiltroEstado] = useState("");
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let componenteActivo = true;

    const sesion = obtenerSesionAdmin();

    if (!sesion) {
      router.replace(
        `/login?returnTo=${encodeURIComponent("/admin/solicitudes-cambio")}`
      );
      return () => {
        componenteActivo = false;
      };
    }

    listarSolicitudesCambioAdmin(
      {
        estado: filtroEstado || undefined,
        page: 0,
        size: 50,
        orden: "recientes",
      },
      sesion.accessToken
    )
      .then((pagina) => {
        if (!componenteActivo) {
          return;
        }

        setSolicitudes(pagina.contenido);
        setError(null);
      })
      .catch((errorCarga: unknown) => {
        if (!componenteActivo) {
          return;
        }

        if (errorCarga instanceof AdminApiError) {
          if (errorCarga.status === 401) {
            cerrarSesion();
            router.replace("/login?logout=1");
            return;
          }

          if (errorCarga.status === 403) {
            setError("No tenés permisos para acceder al panel administrador.");
            return;
          }

          setError(errorCarga.message);
          return;
        }

        setError("Ocurrió un problema inesperado al cargar las solicitudes.");
      })
      .finally(() => {
        if (componenteActivo) {
          setCargando(false);
        }
      });

    return () => {
      componenteActivo = false;
    };
  }, [cerrarSesion, filtroEstado, router]);

  function cambiarFiltroEstado(evento: ChangeEvent<HTMLSelectElement>) {
    setCargando(true);
    setError(null);
    setFiltroEstado(evento.target.value);
  }

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
                  Solicitudes de cambio
                </h1>
                <p className="mt-3 max-w-2xl text-sm leading-6 text-[var(--color-muted)] sm:text-base">
                  Cambios propuestos por publicadores sobre actividades ya
                  publicadas. La versión pública no cambia hasta que apruebes.
                </p>
              </div>

              <div className="flex flex-col gap-3 sm:flex-row lg:flex-col lg:items-end">
                <AppLinkButton href="/admin/solicitudes" variant="secondary">
                  Solicitudes de publicación
                </AppLinkButton>
                {/* La moderación de imágenes llega con el bloque 4 (Supabase
                    Storage); hasta entonces no hay ruta que linkear. */}
              </div>
            </div>
          </div>
        </SurfaceCard>

        <SurfaceCard className="mb-5 rounded-[24px] bg-white/90 p-5 shadow-[0_14px_35px_rgba(12,52,80,0.08)]">
          <label
            htmlFor="filtro-estado-cambio"
            className="block text-sm font-bold text-[var(--color-primary)]"
          >
            Filtrar por estado
          </label>
          <select
            id="filtro-estado-cambio"
            value={filtroEstado}
            onChange={cambiarFiltroEstado}
            disabled={cargando}
            className="mt-2 min-h-12 w-full max-w-sm rounded-[18px] border border-[#BFDDEA] bg-[#F8FAFC] px-4 text-sm font-bold text-[var(--color-text)] outline-none transition duration-200 ease-out hover:border-[var(--color-accent)] focus:border-[var(--color-accent)] focus:ring-4 focus:ring-[#DDEAF3] disabled:cursor-not-allowed disabled:opacity-70"
          >
            {FILTROS_ESTADO.map((opcion) => (
              <option key={opcion.etiqueta} value={opcion.valor}>
                {opcion.etiqueta}
              </option>
            ))}
          </select>
        </SurfaceCard>

        {cargando ? (
          <StatusMessage variant="info" role="status">
            Cargando solicitudes de cambio...
          </StatusMessage>
        ) : null}

        {error ? (
          <StatusMessage variant="error" role="alert">
            {error}
          </StatusMessage>
        ) : null}

        {!cargando && !error && solicitudes.length === 0 ? (
          <SurfaceCard className="p-10 text-center">
            <h3 className="text-lg font-extrabold text-[var(--color-primary)]">
              No hay solicitudes de cambio
              {filtroEstado ? ` en estado ${formatearEstado(filtroEstado)}` : ""}
            </h3>
            <p className="mx-auto mt-2 max-w-md text-sm text-[var(--color-muted)]">
              Cuando un publicador proponga cambios sobre una actividad
              publicada, van a aparecer acá para tu revisión.
            </p>
          </SurfaceCard>
        ) : null}

        {solicitudes.length > 0 ? (
          <div className="grid gap-4">
            {solicitudes.map((solicitud) => (
              <Link
                key={solicitud.id}
                href={`/admin/solicitudes-cambio/${solicitud.id}`}
                className="block rounded-[24px] outline-none transition duration-200 ease-out hover:-translate-y-0.5 focus-visible:ring-4 focus-visible:ring-[#4FB3D9]/30"
              >
                <SurfaceCard className="p-5 transition duration-200 ease-out hover:border-[#BFDDEA] hover:shadow-[0_18px_45px_rgba(12,52,80,0.12)] sm:p-6">
                  <div className="flex flex-wrap items-start justify-between gap-3">
                    <div className="min-w-0">
                      <p className="text-xs font-extrabold uppercase tracking-[0.14em] text-[var(--color-muted)]">
                        Solicitud #{solicitud.id}
                      </p>
                      <h3 className="mt-1 text-lg font-extrabold text-[var(--color-primary)]">
                        {solicitud.actividadTitulo || "Actividad"}
                      </h3>
                    </div>

                    <span
                      className={`rounded-full px-3 py-1.5 text-xs font-extrabold ${
                        ESTILOS_ESTADO[solicitud.estado] ??
                        "bg-[#F8FAFC] text-[var(--color-muted)] ring-1 ring-[#DDEAF3]"
                      }`}
                    >
                      {formatearEstado(solicitud.estado)}
                    </span>
                  </div>

                  <div className="mt-3 flex flex-wrap gap-2">
                    {solicitud.camposPropuestos.map((campo) => (
                      <span
                        key={campo}
                        className="rounded-full bg-[#E8F6FB] px-3 py-1 text-xs font-bold text-[#0F6F8F]"
                      >
                        {ETIQUETAS_CAMPOS[campo] ?? campo}
                      </span>
                    ))}
                  </div>
                </SurfaceCard>
              </Link>
            ))}
          </div>
        ) : null}
      </section>
    </main>
  );
}
