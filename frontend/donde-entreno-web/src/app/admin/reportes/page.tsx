"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";

import { AdminGuard } from "../../../components/admin/AdminGuard";
import { useAuthSession } from "../../../components/auth/AuthSessionProvider";
import { Header } from "../../../components/layout/Header";
import { AppButton } from "../../../components/ui/AppButton";
import { SectionHeader } from "../../../components/ui/SectionHeader";
import { StatusMessage } from "../../../components/ui/StatusMessage";
import { SurfaceCard } from "../../../components/ui/SurfaceCard";
import { obtenerSesionAdmin } from "../../../services/authService";
import {
  ReportesApiError,
  cambiarEstadoReporteAdmin,
  listarReportesAdmin,
  type ReporteAdmin,
} from "../../../services/reportesService";
import { formatearFechaRelativa } from "../../../lib/formatoFecha";

const ETIQUETAS_TIPO: Record<string, string> = {
  IMAGEN: "Foto",
  PERFIL_PUBLICADOR: "Perfil de publicador",
  ACTIVIDAD: "Actividad",
};

const ETIQUETAS_MOTIVO: Record<string, string> = {
  CONTENIDO_INAPROPIADO: "Contenido inapropiado",
  INFORMACION_FALSA: "Información falsa",
  SPAM: "Spam",
  SUPLANTACION: "Suplantación",
  OTRO: "Otro",
};

const ESTILOS_ESTADO: Record<string, string> = {
  PENDIENTE: "bg-[#FFF7E6] text-[#8A5A00] ring-1 ring-[#F5D48F]",
  REVISADO: "bg-[var(--color-info-soft)] text-[var(--color-info-deep)] ring-1 ring-[var(--color-border-accent)]",
  DESESTIMADO: "bg-[var(--color-surface-soft)] text-[var(--color-muted)] ring-1 ring-[var(--color-border-soft)]",
  ACCIONADO: "bg-[var(--color-success-soft)] text-[var(--color-success)] ring-1 ring-[var(--color-success-border)]",
};

const FILTROS_ESTADO = [
  { valor: "PENDIENTE", etiqueta: "Pendientes" },
  { valor: "", etiqueta: "Todos" },
  { valor: "REVISADO", etiqueta: "Revisados" },
  { valor: "DESESTIMADO", etiqueta: "Desestimados" },
  { valor: "ACCIONADO", etiqueta: "Accionados" },
];

/*
  Cola de reportes (script 28, Fase 2 social): el punto de intervención
  de la moderación flexible. El link "Ver" navega al contenido
  reportado cuando la ruta se puede armar desde acá.
*/
export default function AdminReportesPage() {
  return (
    <AdminGuard>
      <AdminReportesListado />
    </AdminGuard>
  );
}

function AdminReportesListado() {
  const router = useRouter();
  const { cerrarSesion } = useAuthSession();
  const [reportes, setReportes] = useState<ReporteAdmin[]>([]);
  const [filtroEstado, setFiltroEstado] = useState("PENDIENTE");
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [procesando, setProcesando] = useState<number | null>(null);

  useEffect(() => {
    let componenteActivo = true;
    const sesion = obtenerSesionAdmin();

    if (!sesion) {
      router.replace(`/login?returnTo=${encodeURIComponent("/admin/reportes")}`);
      return () => {
        componenteActivo = false;
      };
    }

    listarReportesAdmin(sesion.accessToken, filtroEstado || undefined)
      .then((pagina) => {
        if (componenteActivo) {
          setReportes(pagina.contenido);
          setError(null);
        }
      })
      .catch((errorCarga: unknown) => {
        if (!componenteActivo) {
          return;
        }

        if (errorCarga instanceof ReportesApiError && errorCarga.status === 401) {
          cerrarSesion();
          router.replace("/login?logout=1");
          return;
        }

        setError("No pudimos cargar los reportes. Probá nuevamente.");
      })
      .finally(() => {
        if (componenteActivo) {
          setCargando(false);
        }
      });

    return () => {
      componenteActivo = false;
    };
  }, [filtroEstado, router, cerrarSesion]);

  async function cambiarEstado(reporte: ReporteAdmin, estadoNuevo: string) {
    const sesion = obtenerSesionAdmin();
    if (!sesion || procesando !== null) {
      return;
    }

    setProcesando(reporte.id);

    try {
      const actualizado = await cambiarEstadoReporteAdmin(
        sesion.accessToken,
        reporte.id,
        estadoNuevo
      );
      setReportes((actuales) =>
        actuales.map((cada) => (cada.id === actualizado.id ? actualizado : cada))
      );
    } catch {
      setError("No pudimos actualizar el reporte. Probá nuevamente.");
    } finally {
      setProcesando(null);
    }
  }

  return (
    <main className="min-h-screen text-[var(--color-text)]">
      <section className="mx-auto w-full max-w-5xl px-4 py-6">
        <Header />

        <div className="mt-6">
          <SectionHeader
            eyebrow="Administración"
            title="Reportes de la comunidad"
            description="Lo que la comunidad marcó para revisar. Revisado = visto sin acción; Desestimado = no incumple; Accionado = se intervino el contenido."
          />
        </div>

        <div className="mt-5 flex flex-wrap gap-2">
          {FILTROS_ESTADO.map((filtro) => (
            <button
              key={filtro.valor}
              type="button"
              onClick={() => {
                /* El "cargando" se prende acá y no en el efecto (lint). */
                setCargando(true);
                setFiltroEstado(filtro.valor);
              }}
              aria-pressed={filtroEstado === filtro.valor}
              className={`min-h-10 rounded-full px-4 text-sm font-bold transition duration-200 ease-out ${
                filtroEstado === filtro.valor
                  ? "bg-[var(--color-brand)] text-white"
                  : "border border-[var(--color-border-accent)] bg-[var(--color-surface)] text-[var(--color-primary)] hover:border-[var(--color-primary)]"
              }`}
            >
              {filtro.etiqueta}
            </button>
          ))}
        </div>

        {error ? (
          <StatusMessage variant="error" role="alert" className="mt-6">
            {error}
          </StatusMessage>
        ) : null}

        {cargando ? (
          <StatusMessage variant="info" role="status" className="mt-6">
            Cargando reportes...
          </StatusMessage>
        ) : reportes.length === 0 && !error ? (
          <SurfaceCard className="mt-6 p-6">
            <p className="text-sm text-[var(--color-muted)]">
              No hay reportes {filtroEstado ? "en este estado" : "todavía"}. La
              comunidad está tranquila.
            </p>
          </SurfaceCard>
        ) : (
          <ul className="mt-6 space-y-3">
            {reportes.map((reporte) => (
              <SurfaceCard key={reporte.id} as="article" className="p-5">
                <div className="flex flex-wrap items-center justify-between gap-3">
                  <div className="min-w-0">
                    <p className="text-sm font-extrabold text-[var(--color-primary)]">
                      {ETIQUETAS_TIPO[reporte.tipoObjeto] ?? reporte.tipoObjeto}{" "}
                      #{reporte.objetoId} ·{" "}
                      {ETIQUETAS_MOTIVO[reporte.motivo] ?? reporte.motivo}
                    </p>
                    <p className="mt-1 text-xs text-[var(--color-muted)]">
                      {reporte.createdAt
                        ? formatearFechaRelativa(reporte.createdAt)
                        : null}
                    </p>
                  </div>
                  <span
                    className={`rounded-full px-3 py-1 text-xs font-extrabold ${
                      ESTILOS_ESTADO[reporte.estado] ?? ""
                    }`}
                  >
                    {reporte.estado.charAt(0) +
                      reporte.estado.slice(1).toLowerCase()}
                  </span>
                </div>

                {reporte.detalle ? (
                  <p className="mt-3 rounded-[14px] border border-[var(--color-border-soft)] bg-[var(--color-bg)] p-3 text-sm leading-6 text-[var(--color-muted)]">
                    &ldquo;{reporte.detalle}&rdquo;
                  </p>
                ) : null}

                <div className="mt-4 flex flex-wrap items-center gap-2">
                  {rutaDelObjeto(reporte) ? (
                    <Link
                      href={rutaDelObjeto(reporte) as string}
                      className="inline-flex min-h-10 items-center rounded-[14px] border border-[var(--color-border-accent)] bg-[var(--color-surface)] px-4 text-sm font-bold text-[var(--color-primary)] transition duration-200 ease-out hover:border-[var(--color-primary)]"
                    >
                      Ver contenido
                    </Link>
                  ) : null}

                  {reporte.estado === "PENDIENTE" ? (
                    <>
                      <AppButton
                        type="button"
                        variant="secondary"
                        size="sm"
                        disabled={procesando === reporte.id}
                        onClick={() => void cambiarEstado(reporte, "REVISADO")}
                      >
                        Marcar revisado
                      </AppButton>
                      <AppButton
                        type="button"
                        variant="secondary"
                        size="sm"
                        disabled={procesando === reporte.id}
                        onClick={() => void cambiarEstado(reporte, "DESESTIMADO")}
                      >
                        Desestimar
                      </AppButton>
                      <AppButton
                        type="button"
                        variant="danger"
                        size="sm"
                        disabled={procesando === reporte.id}
                        onClick={() => void cambiarEstado(reporte, "ACCIONADO")}
                      >
                        Marcar accionado
                      </AppButton>
                    </>
                  ) : null}
                </div>
              </SurfaceCard>
            ))}
          </ul>
        )}
      </section>
    </main>
  );
}

/*
  Ruta navegable al objeto reportado, cuando se puede armar desde el
  id: el perfil resuelve por id (redirige al slug solo); la foto no
  tiene página propia, así que no hay link directo en V1.
*/
function rutaDelObjeto(reporte: ReporteAdmin): string | null {
  if (reporte.tipoObjeto === "PERFIL_PUBLICADOR") {
    return `/publicadores/${reporte.objetoId}`;
  }

  return null;
}
