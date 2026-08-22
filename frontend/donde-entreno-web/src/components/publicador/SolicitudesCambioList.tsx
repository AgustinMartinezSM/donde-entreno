"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";

import { useAuthSession } from "../auth/AuthSessionProvider";
import { AppLinkButton } from "../ui/AppLinkButton";
import { StatusMessage } from "../ui/StatusMessage";
import { SurfaceCard } from "../ui/SurfaceCard";
import { PublicadorPageHeader } from "./PublicadorPageHeader";
import {
  PublicadorApiError,
  listarSolicitudesCambio,
} from "../../services/publicadorService";
import type { SolicitudCambioResumen } from "../../types/publicador";

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
  deporte: "Deporte",
  edadMinima: "Edad mínima",
  edadMaxima: "Edad máxima",
  enfoque: "Enfoque",
  ubicacion: "Ubicación",
  horarios: "Horarios",
};

const ESTILOS_ESTADO: Record<string, string> = {
  PENDIENTE: "bg-[#FFF7E6] text-[#8A5A00] ring-1 ring-[#F5D48F]",
  EN_REVISION: "bg-[var(--color-info-soft)] text-[var(--color-info-deep)] ring-1 ring-[var(--color-border-accent)]",
  APROBADA: "bg-[var(--color-success-soft)] text-[var(--color-success)] ring-1 ring-[var(--color-success-border)]",
  RECHAZADA: "bg-red-50 text-red-700 ring-1 ring-red-200",
};

function formatearEstado(estado: string): string {
  return estado
    .toLowerCase()
    .split("_")
    .map((parte) => parte.charAt(0).toUpperCase() + parte.slice(1))
    .join(" ");
}

function formatearFecha(fecha: string | null): string {
  if (!fecha) {
    return "Fecha no disponible";
  }

  const valor = new Date(fecha);

  if (Number.isNaN(valor.getTime())) {
    return "Fecha no disponible";
  }

  return new Intl.DateTimeFormat("es-AR", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
  }).format(valor);
}

/*
  Listado de solicitudes de cambio del publicador.
  El estado de cada una cuenta si el equipo ya la resolvió.
*/
export function SolicitudesCambioList() {
  const router = useRouter();
  const { accessToken, cerrarSesion } = useAuthSession();
  const [solicitudes, setSolicitudes] = useState<SolicitudCambioResumen[]>([]);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let componenteActivo = true;

    if (!accessToken) {
      return () => {
        componenteActivo = false;
      };
    }

    listarSolicitudesCambio({ page: 0, size: 50, orden: "recientes" }, accessToken)
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

        if (errorCarga instanceof PublicadorApiError) {
          if (errorCarga.status === 401) {
            cerrarSesion();
            router.replace(
              `/login?returnTo=${encodeURIComponent("/publicador/solicitudes-cambio")}`
            );
            return;
          }

          setError(errorCarga.message);
          return;
        }

        setError("Ocurrió un problema inesperado al cargar tus solicitudes.");
      })
      .finally(() => {
        if (componenteActivo) {
          setCargando(false);
        }
      });

    return () => {
      componenteActivo = false;
    };
  }, [accessToken, cerrarSesion, router]);

  return (
    <main className="min-h-screen px-4 py-8 text-[var(--color-text)] sm:py-12">
      <section className="mx-auto w-full max-w-6xl">
        <PublicadorPageHeader
          title="Mis solicitudes de cambio"
          description="Seguimiento de los cambios que pediste sobre tus actividades publicadas."
          action={
            <AppLinkButton href="/publicador/actividades" variant="secondary" fullWidth>
              Ir a mis actividades
            </AppLinkButton>
          }
        />

        {cargando ? (
          <StatusMessage variant="info" role="status" className="mt-6">
            Cargando tus solicitudes de cambio...
          </StatusMessage>
        ) : null}

        {error ? (
          <StatusMessage variant="error" role="alert" className="mt-6">
            {error}
          </StatusMessage>
        ) : null}

        {!cargando && !error && solicitudes.length === 0 ? (
          <SurfaceCard className="mt-6 flex flex-col items-center gap-4 p-10 text-center">
            <div>
              <h3 className="text-lg font-extrabold text-[var(--color-primary)]">
                Todavía no pediste cambios
              </h3>
              <p className="mt-2 max-w-md text-sm text-[var(--color-muted)]">
                Desde el detalle de cualquiera de tus actividades publicadas
                podés proponer cambios de título, descripción, precio,
                contacto, nivel o modalidad.
              </p>
            </div>
            <AppLinkButton href="/publicador/actividades" className="mt-2">
              Ver mis actividades
            </AppLinkButton>
          </SurfaceCard>
        ) : null}

        {solicitudes.length > 0 ? (
          <div className="mt-6 grid gap-4">
            {solicitudes.map((solicitud) => (
              <SurfaceCard key={solicitud.id} className="p-5 sm:p-6">
                <div className="flex flex-wrap items-start justify-between gap-3">
                  <div className="min-w-0">
                    <p className="text-xs font-extrabold uppercase tracking-[0.14em] text-[var(--color-muted)]">
                      Solicitud #{solicitud.id} · {formatearFecha(solicitud.createdAt)}
                    </p>
                    <h3 className="mt-1 text-lg font-extrabold text-[var(--color-primary)]">
                      {solicitud.actividadTitulo || "Actividad"}
                    </h3>
                  </div>

                  <span
                    className={`rounded-full px-3 py-1.5 text-xs font-extrabold ${
                      ESTILOS_ESTADO[solicitud.estado] ??
                      "bg-[var(--color-bg)] text-[var(--color-muted)] ring-1 ring-[var(--color-border-soft)]"
                    }`}
                  >
                    {formatearEstado(solicitud.estado)}
                  </span>
                </div>

                <div className="mt-3 flex flex-wrap gap-2">
                  {solicitud.camposPropuestos.map((campo) => (
                    <span
                      key={campo}
                      className="rounded-full bg-[var(--color-info-soft)] px-3 py-1 text-xs font-bold text-[var(--color-info-deep)]"
                    >
                      {ETIQUETAS_CAMPOS[campo] ?? campo}
                    </span>
                  ))}
                </div>
              </SurfaceCard>
            ))}
          </div>
        ) : null}
      </section>
    </main>
  );
}
