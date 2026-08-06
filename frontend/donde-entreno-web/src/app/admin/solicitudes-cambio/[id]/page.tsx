"use client";

import { useEffect, useState } from "react";
import { useParams, useRouter } from "next/navigation";

import { AdminGuard } from "../../../../components/admin/AdminGuard";
import { useAuthSession } from "../../../../components/auth/AuthSessionProvider";
import { AppButton } from "../../../../components/ui/AppButton";
import { AppLinkButton } from "../../../../components/ui/AppLinkButton";
import { StatusMessage } from "../../../../components/ui/StatusMessage";
import { SurfaceCard } from "../../../../components/ui/SurfaceCard";
import { obtenerSesionAdmin } from "../../../../services/authService";
import {
  AdminApiError,
  aprobarSolicitudCambioAdmin,
  cambiarEstadoSolicitudCambioAdmin,
  obtenerSolicitudCambioAdmin,
} from "../../../../services/adminSolicitudesService";
import type { SolicitudCambioDetalle } from "../../../../types/publicador";

const ETIQUETAS_CAMPOS: Record<string, string> = {
  titulo: "Título",
  descripcion: "Descripción",
  precioReferencia: "Precio de referencia",
  mostrarPrecio: "Mostrar precio",
  whatsappContacto: "WhatsApp",
  instagramContacto: "Instagram",
  emailContacto: "Email de contacto",
  nivel: "Nivel",
  modalidad: "Modalidad",
};

const ESTILOS_ESTADO: Record<string, string> = {
  PENDIENTE: "bg-[#FFF7E6] text-[#8A5A00] ring-1 ring-[#F5D48F]",
  EN_REVISION: "bg-[#E8F6FB] text-[#0F6F8F] ring-1 ring-[#BFDDEA]",
  APROBADA: "bg-[#E6F7EF] text-[#167A4A] ring-1 ring-[#BDE8D0]",
  RECHAZADA: "bg-red-50 text-red-700 ring-1 ring-red-200",
};

function formatearEstado(estado: string): string {
  return estado
    .toLowerCase()
    .split("_")
    .map((parte) => parte.charAt(0).toUpperCase() + parte.slice(1))
    .join(" ");
}

export default function AdminSolicitudCambioDetallePage() {
  return (
    <AdminGuard>
      <AdminSolicitudCambioDetalle />
    </AdminGuard>
  );
}

function AdminSolicitudCambioDetalle() {
  const router = useRouter();
  const params = useParams<{ id: string }>();
  const solicitudId = Number(params.id);
  const { cerrarSesion } = useAuthSession();

  const [solicitud, setSolicitud] = useState<SolicitudCambioDetalle | null>(null);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [motivoRechazo, setMotivoRechazo] = useState("");
  const [mostrandoRechazo, setMostrandoRechazo] = useState(false);
  const [accionEnCurso, setAccionEnCurso] = useState(false);
  const [mensajeAccion, setMensajeAccion] = useState<string | null>(null);
  const [errorAccion, setErrorAccion] = useState<string | null>(null);

  useEffect(() => {
    let componenteActivo = true;

    const sesion = obtenerSesionAdmin();

    if (!sesion || !Number.isFinite(solicitudId)) {
      router.replace(
        `/login?returnTo=${encodeURIComponent("/admin/solicitudes-cambio")}`
      );
      return () => {
        componenteActivo = false;
      };
    }

    obtenerSolicitudCambioAdmin(solicitudId, sesion.accessToken)
      .then((detalle) => {
        if (!componenteActivo) {
          return;
        }

        setSolicitud(detalle);
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

          setError(errorCarga.message);
          return;
        }

        setError("Ocurrió un problema inesperado al cargar la solicitud.");
      })
      .finally(() => {
        if (componenteActivo) {
          setCargando(false);
        }
      });

    return () => {
      componenteActivo = false;
    };
  }, [cerrarSesion, router, solicitudId]);

  const estaAbierta =
    solicitud?.estado === "PENDIENTE" || solicitud?.estado === "EN_REVISION";

  async function ejecutarAccion(
    accion: () => Promise<SolicitudCambioDetalle>,
    mensajeExito: string
  ) {
    if (accionEnCurso) {
      return;
    }

    setAccionEnCurso(true);
    setErrorAccion(null);
    setMensajeAccion(null);

    try {
      const detalleActualizado = await accion();
      setSolicitud(detalleActualizado);
      setMensajeAccion(mensajeExito);
      setMostrandoRechazo(false);
      setMotivoRechazo("");
    } catch (errorAccionDesconocido: unknown) {
      if (errorAccionDesconocido instanceof AdminApiError) {
        if (errorAccionDesconocido.status === 401) {
          cerrarSesion();
          router.replace("/login?logout=1");
          return;
        }

        setErrorAccion(errorAccionDesconocido.message);
        return;
      }

      setErrorAccion("No pudimos completar la acción. Probá nuevamente.");
    } finally {
      setAccionEnCurso(false);
    }
  }

  function iniciarRevision() {
    const sesion = obtenerSesionAdmin();
    if (!sesion || !solicitud) {
      return;
    }

    void ejecutarAccion(
      () =>
        cambiarEstadoSolicitudCambioAdmin(
          solicitud.id,
          { estado: "EN_REVISION" },
          sesion.accessToken
        ),
      "La solicitud pasó a revisión."
    );
  }

  function aprobar() {
    const sesion = obtenerSesionAdmin();
    if (!sesion || !solicitud) {
      return;
    }

    void ejecutarAccion(
      () => aprobarSolicitudCambioAdmin(solicitud.id, sesion.accessToken),
      "Cambios aprobados y aplicados a la actividad publicada."
    );
  }

  function rechazar() {
    const sesion = obtenerSesionAdmin();
    const motivo = motivoRechazo.trim();

    if (!sesion || !solicitud) {
      return;
    }

    if (!motivo) {
      setErrorAccion("Indicá el motivo del rechazo.");
      return;
    }

    void ejecutarAccion(
      () =>
        cambiarEstadoSolicitudCambioAdmin(
          solicitud.id,
          { estado: "RECHAZADA", motivoRechazo: motivo },
          sesion.accessToken
        ),
      "La solicitud fue rechazada."
    );
  }

  return (
    <main className="min-h-screen bg-gradient-to-br from-[#F8FAFC] via-white to-[#E8F6FB] px-4 py-6 text-[var(--color-text)] sm:py-10">
      <section className="mx-auto w-full max-w-4xl">
        <div className="mb-5">
          <AppLinkButton
            href="/admin/solicitudes-cambio"
            variant="secondary"
            size="sm"
            className="rounded-full"
          >
            ← Volver a solicitudes de cambio
          </AppLinkButton>
        </div>

        {cargando ? (
          <StatusMessage variant="info" role="status">
            Cargando la solicitud...
          </StatusMessage>
        ) : null}

        {error ? (
          <StatusMessage variant="error" role="alert">
            {error}
          </StatusMessage>
        ) : null}

        {solicitud ? (
          <SurfaceCard className="p-6 sm:p-8">
            <div className="flex flex-wrap items-start justify-between gap-3">
              <div className="min-w-0">
                <p className="text-xs font-extrabold uppercase tracking-[0.14em] text-[var(--color-muted)]">
                  Solicitud de cambio #{solicitud.id}
                </p>
                <h1 className="mt-1 text-2xl font-extrabold text-[var(--color-primary)] sm:text-3xl">
                  {solicitud.actividadTitulo || "Actividad"}
                </h1>
                <p className="mt-1 text-sm font-bold text-[var(--color-muted)]">
                  Pedida por {solicitud.perfilPublicadorNombre || "publicador"}
                </p>
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

            {solicitud.actividadSlug ? (
              <div className="mt-4">
                <AppLinkButton
                  href={`/actividades/${solicitud.actividadSlug}`}
                  variant="outline"
                  size="sm"
                >
                  Ver la actividad publicada
                </AppLinkButton>
              </div>
            ) : null}

            <div className="mt-6">
              <h2 className="text-lg font-extrabold text-[var(--color-primary)]">
                Comparación de cambios
              </h2>
              <p className="mt-1 text-sm text-[var(--color-muted)]">
                Lo publicado hoy contra lo que propone el publicador.
              </p>

              <div className="mt-4 overflow-x-auto">
                <table className="w-full min-w-[560px] border-separate border-spacing-0 text-sm">
                  <thead>
                    <tr>
                      <th className="rounded-tl-[14px] border border-[#DDEAF3] bg-[#F8FAFC] px-4 py-3 text-left font-extrabold text-[var(--color-primary)]">
                        Campo
                      </th>
                      <th className="border-y border-r border-[#DDEAF3] bg-[#F8FAFC] px-4 py-3 text-left font-extrabold text-[var(--color-primary)]">
                        Publicado hoy
                      </th>
                      <th className="rounded-tr-[14px] border-y border-r border-[#DDEAF3] bg-[#E6F7EF] px-4 py-3 text-left font-extrabold text-[#167A4A]">
                        Propuesto
                      </th>
                    </tr>
                  </thead>
                  <tbody>
                    {solicitud.cambios.map((cambio) => (
                      <tr key={cambio.campo}>
                        <td className="border-x border-b border-[#DDEAF3] px-4 py-3 font-bold text-[var(--color-primary)]">
                          {ETIQUETAS_CAMPOS[cambio.campo] ?? cambio.campo}
                        </td>
                        <td className="border-b border-r border-[#DDEAF3] px-4 py-3 text-[var(--color-muted)]">
                          {cambio.valorActual || "Sin valor"}
                        </td>
                        <td className="border-b border-r border-[#DDEAF3] bg-[#F4FBF7] px-4 py-3 font-bold text-[#167A4A]">
                          {cambio.valorPropuesto}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>

            {solicitud.motivoRechazo ? (
              <StatusMessage variant="error" className="mt-6">
                <p className="font-bold">Motivo del rechazo</p>
                <p className="mt-1">{solicitud.motivoRechazo}</p>
              </StatusMessage>
            ) : null}

            {mensajeAccion ? (
              <StatusMessage variant="success" role="status" className="mt-6">
                {mensajeAccion}
              </StatusMessage>
            ) : null}

            {errorAccion ? (
              <StatusMessage variant="error" role="alert" className="mt-6">
                {errorAccion}
              </StatusMessage>
            ) : null}

            {estaAbierta ? (
              <div className="mt-6 border-t border-[#DDEAF3] pt-6">
                <div className="grid gap-3 sm:grid-cols-3">
                  {solicitud.estado === "PENDIENTE" ? (
                    <AppButton
                      type="button"
                      variant="secondary"
                      onClick={iniciarRevision}
                      disabled={accionEnCurso}
                      fullWidth
                    >
                      Iniciar revisión
                    </AppButton>
                  ) : null}

                  <AppButton
                    type="button"
                    variant="success"
                    onClick={aprobar}
                    disabled={accionEnCurso}
                    fullWidth
                  >
                    {accionEnCurso ? "Procesando..." : "Aprobar y aplicar"}
                  </AppButton>

                  <AppButton
                    type="button"
                    variant="danger"
                    onClick={() => setMostrandoRechazo((valor) => !valor)}
                    disabled={accionEnCurso}
                    fullWidth
                  >
                    Rechazar
                  </AppButton>
                </div>

                {mostrandoRechazo ? (
                  <div className="mt-4">
                    <label
                      htmlFor="motivo-rechazo"
                      className="text-sm font-bold text-[var(--color-primary)]"
                    >
                      Motivo del rechazo (se le muestra al publicador)
                    </label>
                    <textarea
                      id="motivo-rechazo"
                      rows={3}
                      maxLength={2000}
                      value={motivoRechazo}
                      onChange={(evento) => setMotivoRechazo(evento.target.value)}
                      disabled={accionEnCurso}
                      className="mt-2 min-h-24 w-full rounded-[18px] border border-[#BFDDEA] bg-[#F8FAFC] px-4 py-3 text-base leading-6 text-[var(--color-text)] outline-none transition duration-200 ease-out hover:border-[var(--color-accent)] focus:border-[var(--color-accent)] focus:ring-4 focus:ring-[#DDEAF3] disabled:cursor-not-allowed disabled:opacity-70"
                    />
                    <AppButton
                      type="button"
                      variant="danger"
                      onClick={rechazar}
                      disabled={accionEnCurso}
                      className="mt-3"
                    >
                      Confirmar rechazo
                    </AppButton>
                  </div>
                ) : null}
              </div>
            ) : null}
          </SurfaceCard>
        ) : null}
      </section>
    </main>
  );
}
