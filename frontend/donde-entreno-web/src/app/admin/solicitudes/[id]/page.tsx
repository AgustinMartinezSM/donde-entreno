"use client";

import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { AdminGuard } from "../../../../components/admin/AdminGuard";
import { AdminEstadoBadge } from "../../../../components/admin/AdminEstadoBadge";
import {
  cerrarSesionAdmin,
  obtenerSesionAdmin,
} from "../../../../services/authService";
import {
  AdminApiError,
  aprobarSolicitudAdmin,
  cambiarEstadoSolicitudAdmin,
  obtenerSolicitudAdmin,
} from "../../../../services/adminSolicitudesService";
import type { AdminSesion } from "../../../../types/auth";
import type {
  EstadoSolicitudAdmin,
  SolicitudPublicacionAprobacionResponse,
  SolicitudPublicacionAdminDetalle,
  SolicitudPublicacionAdminHorario,
} from "../../../../types/adminSolicitudes";
import type { ReactNode } from "react";

type ResultadoCargaDetalle =
  | {
      tipo: "ok";
      sesion: AdminSesion;
      solicitud: SolicitudPublicacionAdminDetalle;
    }
  | {
      tipo: "sinSesion";
    }
  | {
      tipo: "idInvalido";
    };

export default function AdminSolicitudDetallePage() {
  return (
    <AdminGuard>
      <AdminSolicitudDetalle />
    </AdminGuard>
  );
}

function AdminSolicitudDetalle() {
  const router = useRouter();
  const params = useParams<{ id: string }>();
  const idParametro = params.id;
  const [sesion, setSesion] = useState<AdminSesion | null>(null);
  const [solicitud, setSolicitud] =
    useState<SolicitudPublicacionAdminDetalle | null>(null);
  const [cargandoDetalle, setCargandoDetalle] = useState(true);
  const [errorDetalle, setErrorDetalle] = useState<string | null>(null);
  const [accionEnCurso, setAccionEnCurso] = useState(false);
  const [errorAccion, setErrorAccion] = useState<string | null>(null);
  const [exitoAccion, setExitoAccion] = useState<string | null>(null);
  const [motivoRechazo, setMotivoRechazo] = useState("");
  const [confirmandoAprobacion, setConfirmandoAprobacion] = useState(false);
  const [respuestaAprobacion, setRespuestaAprobacion] =
    useState<SolicitudPublicacionAprobacionResponse | null>(null);

  useEffect(() => {
    let componenteActivo = true;

    void cargarDetalleDesdeSesion(idParametro)
      .then((resultado) => {
        if (!componenteActivo) {
          return;
        }

        if (resultado.tipo === "sinSesion") {
          cerrarSesionAdmin();
          router.replace("/admin/login");
          return;
        }

        if (resultado.tipo === "idInvalido") {
          setErrorDetalle("No encontramos la solicitud solicitada.");
          return;
        }

        setSesion(resultado.sesion);
        setSolicitud(resultado.solicitud);
        setErrorDetalle(null);
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
            setErrorDetalle(
              "No tenés permisos para acceder al panel administrador."
            );
            return;
          }

          if (error.status === 404) {
            setErrorDetalle("No encontramos la solicitud solicitada.");
            return;
          }

          setErrorDetalle(error.message);
          return;
        }

        setErrorDetalle(
          "Ocurrió un problema inesperado al cargar la solicitud."
        );
      })
      .finally(() => {
        if (!componenteActivo) {
          return;
        }

        setCargandoDetalle(false);
      });

    return () => {
      componenteActivo = false;
    };
  }, [idParametro, router]);

  function cerrarSesion() {
    cerrarSesionAdmin();
    router.replace("/admin/login");
  }

  function manejarCambioMotivoRechazo(valor: string) {
    setMotivoRechazo(valor);
    setErrorAccion(null);
    setExitoAccion(null);
  }

  function manejarSolicitarAprobacion() {
    setErrorAccion(null);
    setExitoAccion(null);
    setRespuestaAprobacion(null);
    setConfirmandoAprobacion(true);
  }

  function manejarCancelarAprobacion() {
    setConfirmandoAprobacion(false);
  }

  async function manejarMarcarEnRevision() {
    if (!sesion || !solicitud || accionEnCurso) {
      return;
    }

    setAccionEnCurso(true);
    setErrorAccion(null);
    setExitoAccion(null);
    setRespuestaAprobacion(null);
    setConfirmandoAprobacion(false);

    try {
      await cambiarEstadoSolicitudAdmin(
        solicitud.id,
        {
          estado: "EN_REVISION",
          motivoRechazo: null,
        },
        sesion.accessToken
      );

      await recargarDetalleSolicitud(solicitud.id, sesion.accessToken);
      setExitoAccion("Solicitud marcada en revisión.");
    } catch (error: unknown) {
      manejarErrorAccion(error);
    } finally {
      setAccionEnCurso(false);
    }
  }

  async function manejarRechazarSolicitud() {
    if (!sesion || !solicitud || accionEnCurso) {
      return;
    }

    const motivoNormalizado = motivoRechazo.trim();

    if (!motivoNormalizado) {
      setExitoAccion(null);
      setErrorAccion("Ingresá un motivo para rechazar la solicitud.");
      return;
    }

    setAccionEnCurso(true);
    setErrorAccion(null);
    setExitoAccion(null);
    setRespuestaAprobacion(null);
    setConfirmandoAprobacion(false);

    try {
      await cambiarEstadoSolicitudAdmin(
        solicitud.id,
        {
          estado: "RECHAZADA",
          motivoRechazo: motivoNormalizado,
        },
        sesion.accessToken
      );

      await recargarDetalleSolicitud(solicitud.id, sesion.accessToken);
      setMotivoRechazo("");
      setExitoAccion("Solicitud rechazada correctamente.");
    } catch (error: unknown) {
      manejarErrorAccion(error);
    } finally {
      setAccionEnCurso(false);
    }
  }

  async function manejarConfirmarAprobacion() {
    if (!sesion || !solicitud || accionEnCurso) {
      return;
    }

    setAccionEnCurso(true);
    setErrorAccion(null);
    setExitoAccion(null);

    try {
      const respuesta = await aprobarSolicitudAdmin(
        solicitud.id,
        sesion.accessToken
      );

      setRespuestaAprobacion(respuesta);
      setConfirmandoAprobacion(false);
      setExitoAccion(respuesta.mensaje);
      await recargarDetalleSolicitud(solicitud.id, sesion.accessToken);
    } catch (error: unknown) {
      manejarErrorAccion(
        error,
        "Ocurrió un problema inesperado al aprobar la solicitud."
      );
    } finally {
      setAccionEnCurso(false);
    }
  }

  async function recargarDetalleSolicitud(
    solicitudId: number,
    accessToken: string
  ) {
    const solicitudActualizada = await obtenerSolicitudAdmin(
      solicitudId,
      accessToken
    );

    setSolicitud(solicitudActualizada);
  }

  function manejarErrorAccion(
    error: unknown,
    mensajeErrorDesconocido = "Ocurrió un problema inesperado al actualizar la solicitud."
  ) {
    if (error instanceof AdminApiError) {
      if (error.status === 401) {
        cerrarSesionAdmin();
        router.replace("/admin/login");
        return;
      }

      if (error.status === 403) {
        setErrorAccion("No tenés permisos para realizar esta acción.");
        return;
      }

      if (error.status === 404) {
        setErrorAccion("No encontramos la solicitud solicitada.");
        return;
      }

      setErrorAccion(error.message);
      return;
    }

    setErrorAccion(mensajeErrorDesconocido);
  }

  return (
    <main className="min-h-screen bg-gradient-to-br from-[#F8FAFC] via-white to-[#E8F6FB] px-4 py-6 text-[var(--color-text)] sm:py-10">
      <section className="mx-auto w-full max-w-6xl">
        <div className="mb-6 flex flex-col gap-4 rounded-[var(--radius-xl)] border border-[#DDEAF3] bg-gradient-to-br from-white to-[#F8FCFE] p-5 shadow-[0_18px_45px_rgba(12,52,80,0.10)] sm:flex-row sm:items-center sm:justify-between">
          <div>
            <p className="text-xs font-extrabold uppercase tracking-[0.18em] text-[var(--color-secondary)]">
              Panel administrador
            </p>
            <h1 className="mt-2 text-2xl font-extrabold text-[var(--color-primary)] sm:text-3xl">
              Detalle de solicitud
            </h1>
            {sesion && (
              <p className="mt-2 text-sm leading-6 text-[var(--color-muted)]">
                {sesion.usuario.nombre} {sesion.usuario.apellido} ·{" "}
                {sesion.usuario.email} · {sesion.usuario.rol}
              </p>
            )}
          </div>

          <div className="flex flex-col gap-3 sm:flex-row">
            <Link
              href="/admin/solicitudes"
              className="rounded-[var(--radius-md)] border border-[var(--color-border)] px-4 py-3 text-center text-sm font-bold text-[var(--color-primary)] transition duration-200 ease-out hover:-translate-y-0.5 hover:border-[var(--color-primary)] hover:bg-white active:scale-[0.98]"
            >
              Volver al listado
            </Link>
            <button
              type="button"
              onClick={cerrarSesion}
              className="rounded-[var(--radius-md)] bg-[var(--color-primary)] px-4 py-3 text-sm font-bold text-white shadow-[var(--shadow-button)] transition duration-200 ease-out hover:-translate-y-0.5 hover:bg-[#0B314D] active:scale-[0.98]"
            >
              Cerrar sesión
            </button>
          </div>
        </div>

        {cargandoDetalle && (
          <div
            role="status"
            className="rounded-[var(--radius-xl)] border border-[var(--color-border)] bg-[var(--color-surface)] p-6 text-center shadow-[0_14px_35px_rgba(12,52,80,0.08)]"
          >
            <div className="mx-auto h-9 w-9 animate-spin rounded-full border-4 border-[#DDEAF3] border-t-[var(--color-secondary)]" />
            <p className="mt-4 text-sm font-bold text-[var(--color-primary)]">
              Cargando solicitud...
            </p>
          </div>
        )}

        {!cargandoDetalle && errorDetalle && (
          <div
            role="alert"
            className="rounded-[var(--radius-xl)] border border-red-200 bg-red-50 p-6 shadow-[0_14px_35px_rgba(127,29,29,0.08)]"
          >
            <h2 className="text-xl font-extrabold text-red-700">
              No pudimos cargar la solicitud
            </h2>
            <p className="mt-3 text-sm leading-6 text-red-700">
              {errorDetalle}
            </p>
          </div>
        )}

        {!cargandoDetalle && !errorDetalle && solicitud && (
          <DetalleSolicitud
            solicitud={solicitud}
            motivoRechazo={motivoRechazo}
            accionEnCurso={accionEnCurso}
            errorAccion={errorAccion}
            exitoAccion={exitoAccion}
            confirmandoAprobacion={confirmandoAprobacion}
            respuestaAprobacion={respuestaAprobacion}
            onMotivoRechazoChange={manejarCambioMotivoRechazo}
            onMarcarEnRevision={manejarMarcarEnRevision}
            onRechazarSolicitud={manejarRechazarSolicitud}
            onSolicitarAprobacion={manejarSolicitarAprobacion}
            onCancelarAprobacion={manejarCancelarAprobacion}
            onConfirmarAprobacion={manejarConfirmarAprobacion}
          />
        )}
      </section>
    </main>
  );
}

function DetalleSolicitud({
  solicitud,
  motivoRechazo,
  accionEnCurso,
  errorAccion,
  exitoAccion,
  confirmandoAprobacion,
  respuestaAprobacion,
  onMotivoRechazoChange,
  onMarcarEnRevision,
  onRechazarSolicitud,
  onSolicitarAprobacion,
  onCancelarAprobacion,
  onConfirmarAprobacion,
}: {
  solicitud: SolicitudPublicacionAdminDetalle;
  motivoRechazo: string;
  accionEnCurso: boolean;
  errorAccion: string | null;
  exitoAccion: string | null;
  confirmandoAprobacion: boolean;
  respuestaAprobacion: SolicitudPublicacionAprobacionResponse | null;
  onMotivoRechazoChange: (motivo: string) => void;
  onMarcarEnRevision: () => void;
  onRechazarSolicitud: () => void;
  onSolicitarAprobacion: () => void;
  onCancelarAprobacion: () => void;
  onConfirmarAprobacion: () => void;
}) {
  return (
    <div className="grid gap-5">
      <section className="rounded-[var(--radius-xl)] border border-[var(--color-border)] bg-[var(--color-surface)] p-5 shadow-[0_18px_45px_rgba(12,52,80,0.10)]">
        <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
          <div>
            <p className="text-xs font-extrabold uppercase tracking-[0.18em] text-[var(--color-muted)]">
              {solicitud.codigoSeguimiento}
            </p>
            <h2 className="mt-2 text-2xl font-extrabold text-[var(--color-primary)]">
              {solicitud.nombreActividad}
            </h2>
          </div>
          <AdminEstadoBadge estado={solicitud.estado} />
        </div>

        <div className="mt-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          <CampoDetalle etiqueta="ID interno" valor={solicitud.id} />
          <CampoDetalle etiqueta="Creación" valor={formatearFecha(solicitud.createdAt)} />
          <CampoDetalle
            etiqueta="Actualización"
            valor={formatearFecha(solicitud.updatedAt)}
          />
          <CampoDetalle etiqueta="Origen" valor={solicitud.origen} />
          <CampoDetalle
            etiqueta="Revisión iniciada"
            valor={formatearFechaOpcional(solicitud.revisionIniciadaAt)}
          />
          <CampoDetalle
            etiqueta="Revisión finalizada"
            valor={formatearFechaOpcional(solicitud.revisionFinalizadaAt)}
          />
          <CampoDetalle
            etiqueta="Actividad generada"
            valor={solicitud.actividadGeneradaId}
          />
          <CampoDetalleEstado
            etiqueta="Estado actual"
            estado={solicitud.estado}
          />
        </div>
      </section>

      <section className="grid gap-5 lg:grid-cols-2">
        <DetalleCard titulo="Publicador">
          <CampoDetalle etiqueta="Tipo" valor={solicitud.tipoPublicador} />
          <CampoDetalle etiqueta="Nombre" valor={solicitud.nombrePublicador} />
          <CampoDetalle etiqueta="WhatsApp" valor={solicitud.whatsapp} />
          <CampoDetalle etiqueta="Instagram" valor={solicitud.instagram} />
          <CampoDetalle etiqueta="Email" valor={solicitud.email} />
        </DetalleCard>

        <DetalleCard titulo="Actividad">
          <CampoDetalle etiqueta="Nombre" valor={solicitud.nombreActividad} />
          <CampoDetalle
            etiqueta="Deporte existente"
            valor={solicitud.deporteNombre}
          />
          <CampoDetalle etiqueta="Deporte ID" valor={solicitud.deporteId} />
          <CampoDetalle etiqueta="Deporte otro" valor={solicitud.deporteOtro} />
          <CampoDetalle etiqueta="Descripción" valor={solicitud.descripcion} />
          <CampoDetalle etiqueta="Nivel" valor={solicitud.nivel} />
          <CampoDetalle etiqueta="Enfoque" valor={solicitud.enfoque} />
          <CampoDetalle etiqueta="Modalidad" valor={solicitud.modalidad} />
          <CampoDetalle etiqueta="Edad mínima" valor={solicitud.edadMinima} />
          <CampoDetalle etiqueta="Edad máxima" valor={solicitud.edadMaxima} />
          <CampoDetalle
            etiqueta="Mostrar precio"
            valor={solicitud.mostrarPrecio ? "Sí" : "No"}
          />
          <CampoDetalle
            etiqueta="Precio de referencia"
            valor={formatearPrecio(solicitud.precioReferencia)}
          />
        </DetalleCard>
      </section>

      <section className="grid gap-5 lg:grid-cols-2">
        <DetalleCard titulo="Ubicación">
          <CampoDetalle etiqueta="Ciudad existente" valor={solicitud.ciudadNombre} />
          <CampoDetalle etiqueta="Ciudad ID" valor={solicitud.ciudadId} />
          <CampoDetalle etiqueta="Ciudad otra" valor={solicitud.ciudadOtra} />
          <CampoDetalle etiqueta="Barrio existente" valor={solicitud.barrioNombre} />
          <CampoDetalle etiqueta="Barrio ID" valor={solicitud.barrioId} />
          <CampoDetalle etiqueta="Barrio otro" valor={solicitud.barrioOtro} />
          <CampoDetalle etiqueta="Nombre del lugar" valor={solicitud.nombreLugar} />
          <CampoDetalle etiqueta="Dirección" valor={solicitud.direccion} />
          <CampoDetalle
            etiqueta="Referencia"
            valor={solicitud.referenciaUbicacion}
          />
        </DetalleCard>

        <DetalleCard titulo="Revisión">
          <CampoDetalle
            etiqueta="Observaciones del solicitante"
            valor={solicitud.observacionesSolicitante}
          />
          <CampoDetalle etiqueta="Motivo rechazo" valor={solicitud.motivoRechazo} />
          <CampoDetalle
            etiqueta="Observaciones internas"
            valor={solicitud.observacionesRevision}
          />
          <CampoDetalle
            etiqueta="Revisor"
            valor={formatearRevisor(solicitud)}
          />
          <CampoDetalleEstado
            etiqueta="Estado actual"
            estado={solicitud.estado}
          />
        </DetalleCard>

        <AccionesRevision
          solicitud={solicitud}
          motivoRechazo={motivoRechazo}
          accionEnCurso={accionEnCurso}
          errorAccion={errorAccion}
          exitoAccion={exitoAccion}
          confirmandoAprobacion={confirmandoAprobacion}
          respuestaAprobacion={respuestaAprobacion}
          onMotivoRechazoChange={onMotivoRechazoChange}
          onMarcarEnRevision={onMarcarEnRevision}
          onRechazarSolicitud={onRechazarSolicitud}
          onSolicitarAprobacion={onSolicitarAprobacion}
          onCancelarAprobacion={onCancelarAprobacion}
          onConfirmarAprobacion={onConfirmarAprobacion}
        />
      </section>

      <section className="rounded-[var(--radius-xl)] border border-[var(--color-border)] bg-[var(--color-surface)] p-5 shadow-[0_14px_35px_rgba(12,52,80,0.08)]">
        <h2 className="text-xl font-extrabold text-[var(--color-primary)]">
          Horarios
        </h2>

        {solicitud.horarios.length === 0 ? (
          <p className="mt-4 text-sm leading-6 text-[var(--color-muted)]">
            No hay horarios informados.
          </p>
        ) : (
          <div className="mt-4 grid gap-3 md:grid-cols-2">
            {solicitud.horarios.map((horario) => (
              <HorarioCard key={horario.id} horario={horario} />
            ))}
          </div>
        )}
      </section>
    </div>
  );
}

function AccionesRevision({
  solicitud,
  motivoRechazo,
  accionEnCurso,
  errorAccion,
  exitoAccion,
  confirmandoAprobacion,
  respuestaAprobacion,
  onMotivoRechazoChange,
  onMarcarEnRevision,
  onRechazarSolicitud,
  onSolicitarAprobacion,
  onCancelarAprobacion,
  onConfirmarAprobacion,
}: {
  solicitud: SolicitudPublicacionAdminDetalle;
  motivoRechazo: string;
  accionEnCurso: boolean;
  errorAccion: string | null;
  exitoAccion: string | null;
  confirmandoAprobacion: boolean;
  respuestaAprobacion: SolicitudPublicacionAprobacionResponse | null;
  onMotivoRechazoChange: (motivo: string) => void;
  onMarcarEnRevision: () => void;
  onRechazarSolicitud: () => void;
  onSolicitarAprobacion: () => void;
  onCancelarAprobacion: () => void;
  onConfirmarAprobacion: () => void;
}) {
  const estaRechazada = solicitud.estado === "RECHAZADA";
  const estaAprobada = solicitud.estado === "APROBADA";
  const estaEnRevision = solicitud.estado === "EN_REVISION";
  const puedeMarcarEnRevision = solicitud.estado === "PENDIENTE";
  const puedeAprobar =
    solicitud.estado === "PENDIENTE" || solicitud.estado === "EN_REVISION";
  const puedeRechazar =
    solicitud.estado === "PENDIENTE" || solicitud.estado === "EN_REVISION";
  const actividadSlugAprobada = respuestaAprobacion?.actividadSlug.trim() ?? "";

  return (
    <section className="rounded-[var(--radius-xl)] border border-[var(--color-border)] bg-[var(--color-surface)] p-5 shadow-[0_14px_35px_rgba(12,52,80,0.08)]">
      <h2 className="text-xl font-extrabold text-[var(--color-primary)]">
        Acciones de revisión
      </h2>

      <div className="mt-4">
        <CampoDetalleEstado
          etiqueta="Estado actual"
          estado={solicitud.estado}
        />
      </div>

      {estaRechazada && (
        <p className="mt-5 rounded-[var(--radius-md)] border border-red-200 bg-red-50 px-4 py-3 text-sm font-bold text-red-700">
          Esta solicitud ya fue rechazada.
        </p>
      )}

      {estaAprobada && (
        <p className="mt-5 rounded-[var(--radius-md)] border border-[#BDE8D0] bg-[#ECF9F2] px-4 py-3 text-sm font-bold text-[#1D7B4A]">
          Esta solicitud ya fue aprobada.
        </p>
      )}

      {puedeAprobar && (
        <div className="mt-5 grid gap-4">
          {!confirmandoAprobacion && (
            <button
              type="button"
              onClick={onSolicitarAprobacion}
              disabled={accionEnCurso}
              className="rounded-[var(--radius-md)] bg-[var(--color-primary)] px-5 py-3 text-sm font-bold text-white shadow-[var(--shadow-button)] transition duration-200 ease-out hover:-translate-y-0.5 hover:bg-[#0B314D] active:scale-[0.98] disabled:cursor-not-allowed disabled:opacity-50 disabled:hover:translate-y-0"
            >
              {accionEnCurso ? "Procesando..." : "Aprobar y publicar actividad"}
            </button>
          )}

          {confirmandoAprobacion && (
            <div className="rounded-[var(--radius-md)] border border-[#F7D87A] bg-[#FFF8E1] px-4 py-4">
              <p className="text-sm font-bold leading-6 text-[#7A5A00]">
                Esta acción creará una actividad pública visible en
                DondeEntreno. ¿Querés continuar?
              </p>
              <div className="mt-4 flex flex-col gap-3 sm:flex-row">
                <button
                  type="button"
                  onClick={onCancelarAprobacion}
                  disabled={accionEnCurso}
                  className="rounded-[var(--radius-md)] border border-[#D9B94E] px-5 py-3 text-sm font-bold text-[#7A5A00] transition duration-200 ease-out hover:-translate-y-0.5 hover:border-[#A98300] hover:bg-white active:scale-[0.98] disabled:cursor-not-allowed disabled:opacity-50"
                >
                  Cancelar
                </button>
                <button
                  type="button"
                  onClick={onConfirmarAprobacion}
                  disabled={accionEnCurso}
                  className="rounded-[var(--radius-md)] bg-[var(--color-primary)] px-5 py-3 text-sm font-bold text-white shadow-[var(--shadow-button)] transition duration-200 ease-out hover:-translate-y-0.5 hover:bg-[#0B314D] active:scale-[0.98] disabled:cursor-not-allowed disabled:opacity-50 disabled:hover:translate-y-0"
                >
                  {accionEnCurso ? "Procesando..." : "Confirmar aprobación"}
                </button>
              </div>
            </div>
          )}
        </div>
      )}

      {puedeRechazar && (
        <div className="mt-5 grid gap-5">
          {estaEnRevision && (
            <p className="rounded-[var(--radius-md)] border border-[#A9D8EA] bg-[#EEF8FC] px-4 py-3 text-sm font-bold text-[var(--color-primary)]">
              La solicitud ya está en revisión.
            </p>
          )}

          {puedeMarcarEnRevision && (
            <div className="flex flex-col gap-3 sm:flex-row">
              <button
                type="button"
                onClick={onMarcarEnRevision}
                disabled={accionEnCurso}
                className="rounded-[var(--radius-md)] bg-[var(--color-primary)] px-5 py-3 text-sm font-bold text-white shadow-[var(--shadow-button)] transition duration-200 ease-out hover:-translate-y-0.5 hover:bg-[#0B314D] active:scale-[0.98] disabled:cursor-not-allowed disabled:opacity-50 disabled:hover:translate-y-0"
              >
                {accionEnCurso ? "Procesando..." : "Marcar en revisión"}
              </button>
            </div>
          )}

          <div>
            <label
              htmlFor="motivo-rechazo"
              className="text-sm font-bold text-[var(--color-primary)]"
            >
              Motivo de rechazo
            </label>
            <textarea
              id="motivo-rechazo"
              value={motivoRechazo}
              onChange={(evento) =>
                onMotivoRechazoChange(evento.target.value)
              }
              disabled={accionEnCurso}
              rows={4}
              className="mt-2 w-full resize-y rounded-[var(--radius-md)] border border-[var(--color-border)] bg-white px-4 py-3 text-sm leading-6 text-[var(--color-text)] outline-none transition duration-200 ease-out hover:border-[#BFDDEA] focus:border-[var(--color-accent)] focus:ring-2 focus:ring-[#DDEAF3] disabled:cursor-not-allowed disabled:opacity-70"
            />
          </div>

          <button
            type="button"
            onClick={onRechazarSolicitud}
            disabled={accionEnCurso}
            className="rounded-[var(--radius-md)] border border-red-200 bg-red-50 px-5 py-3 text-sm font-bold text-red-700 transition duration-200 ease-out hover:-translate-y-0.5 hover:border-red-300 hover:bg-white active:scale-[0.98] disabled:cursor-not-allowed disabled:opacity-50"
          >
            {accionEnCurso ? "Procesando..." : "Rechazar solicitud"}
          </button>
        </div>
      )}

      {errorAccion && (
        <p
          role="alert"
          className="mt-5 rounded-[var(--radius-md)] border border-red-200 bg-red-50 px-4 py-3 text-sm font-bold text-red-700"
        >
          {errorAccion}
        </p>
      )}

      {respuestaAprobacion && (
        <div
          role="status"
          className="mt-5 rounded-[var(--radius-md)] border border-[#BDE8D0] bg-[#ECF9F2] px-4 py-4 text-sm font-bold text-[#1D7B4A]"
        >
          <p>{respuestaAprobacion.mensaje}</p>
          <p className="mt-2">
            Actividad creada: {respuestaAprobacion.actividadTitulo}
          </p>
          <p className="mt-1">ID actividad: {respuestaAprobacion.actividadId}</p>
          {actividadSlugAprobada && (
            <Link
              href={`/actividades/${actividadSlugAprobada}`}
              className="mt-3 inline-flex rounded-[var(--radius-md)] bg-[#1D7B4A] px-4 py-2 text-sm font-bold text-white transition duration-200 ease-out hover:-translate-y-0.5 hover:bg-[#16683E] active:scale-[0.98]"
            >
              Ver actividad pública
            </Link>
          )}
        </div>
      )}

      {exitoAccion && !respuestaAprobacion && (
        <p
          role="status"
          className="mt-5 rounded-[var(--radius-md)] border border-[#BDE8D0] bg-[#ECF9F2] px-4 py-3 text-sm font-bold text-[#1D7B4A]"
        >
          {exitoAccion}
        </p>
      )}
    </section>
  );
}

function DetalleCard({
  titulo,
  children,
}: {
  titulo: string;
  children: ReactNode;
}) {
  return (
    <section className="rounded-[var(--radius-xl)] border border-[var(--color-border)] bg-[var(--color-surface)] p-5 shadow-[0_14px_35px_rgba(12,52,80,0.08)] transition duration-200 ease-out hover:-translate-y-0.5 hover:border-[#DDEAF3]">
      <h2 className="text-xl font-extrabold text-[var(--color-primary)]">
        {titulo}
      </h2>
      <dl className="mt-4 grid gap-4">{children}</dl>
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
      <dt className="text-sm font-bold text-[var(--color-primary)]">
        {etiqueta}
      </dt>
      <dd className="mt-1 whitespace-pre-wrap text-sm leading-6 text-[var(--color-muted)]">
        {formatearValor(valor)}
      </dd>
    </div>
  );
}

function CampoDetalleEstado({
  etiqueta,
  estado,
}: {
  etiqueta: string;
  estado: EstadoSolicitudAdmin;
}) {
  return (
    <div>
      <dt className="text-sm font-bold text-[var(--color-primary)]">
        {etiqueta}
      </dt>
      <dd className="mt-2">
        <AdminEstadoBadge estado={estado} size="sm" />
      </dd>
    </div>
  );
}

function HorarioCard({
  horario,
}: {
  horario: SolicitudPublicacionAdminHorario;
}) {
  return (
    <article className="rounded-[var(--radius-lg)] border border-[#DDEAF3] bg-[#F8FCFE] p-4 transition duration-200 ease-out hover:-translate-y-0.5 hover:shadow-[0_12px_30px_rgba(12,52,80,0.08)]">
      <p className="text-sm font-extrabold text-[var(--color-primary)]">
        {horario.diaSemana}
      </p>
      <dl className="mt-3 grid gap-3 text-sm">
        <CampoDetalle etiqueta="ID horario" valor={horario.id} />
        <CampoDetalle etiqueta="Hora inicio" valor={horario.horaInicio} />
        <CampoDetalle etiqueta="Hora fin" valor={horario.horaFin} />
        <CampoDetalle etiqueta="Observación" valor={horario.observacion} />
      </dl>
    </article>
  );
}

function formatearValor(valor: string | number | null): string {
  if (valor === null) {
    return "No informado";
  }

  if (typeof valor === "string" && valor.trim().length === 0) {
    return "No informado";
  }

  return String(valor);
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

function formatearFechaOpcional(fechaIso: string | null): string | null {
  return fechaIso ? formatearFecha(fechaIso) : null;
}

function formatearPrecio(precio: number | null): string | null {
  if (precio === null) {
    return null;
  }

  return new Intl.NumberFormat("es-AR", {
    style: "currency",
    currency: "ARS",
  }).format(precio);
}

function formatearRevisor(
  solicitud: SolicitudPublicacionAdminDetalle
): string | null {
  if (!solicitud.revisor) {
    return null;
  }

  return `${solicitud.revisor.nombre} ${solicitud.revisor.apellido} · ${solicitud.revisor.email} · ${solicitud.revisor.rol ?? "Sin rol"}`;
}

async function cargarDetalleDesdeSesion(
  idParametro: string
): Promise<ResultadoCargaDetalle> {
  const id = Number(idParametro);

  if (!Number.isInteger(id) || id <= 0) {
    return {
      tipo: "idInvalido",
    };
  }

  const sesion = obtenerSesionAdmin();

  if (!sesion) {
    return {
      tipo: "sinSesion",
    };
  }

  const solicitud = await obtenerSolicitudAdmin(id, sesion.accessToken);

  return {
    tipo: "ok",
    sesion,
    solicitud,
  };
}
