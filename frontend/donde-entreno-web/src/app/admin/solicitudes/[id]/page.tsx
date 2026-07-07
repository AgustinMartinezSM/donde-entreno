"use client";

import { useParams, useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { AdminGuard } from "../../../../components/admin/AdminGuard";
import { AdminEstadoBadge } from "../../../../components/admin/AdminEstadoBadge";
import { useAuthSession } from "../../../../components/auth/AuthSessionProvider";
import { BrandName } from "../../../../components/brand/BrandName";
import { AppButton } from "../../../../components/ui/AppButton";
import { AppLinkButton } from "../../../../components/ui/AppLinkButton";
import { SectionHeader } from "../../../../components/ui/SectionHeader";
import { StatusMessage } from "../../../../components/ui/StatusMessage";
import { SurfaceCard } from "../../../../components/ui/SurfaceCard";
import {
  hayLogoutRecienteAuth,
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
  const { cerrarSesion: cerrarSesionAuthContext } = useAuthSession();
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
          router.replace(
            obtenerRutaLoginAdmin(`/admin/solicitudes/${idParametro}`)
          );
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
            cerrarSesionAuthContext();
            router.replace("/login?logout=1");
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
  }, [cerrarSesionAuthContext, idParametro, router]);

  function cerrarSesion() {
    cerrarSesionAuthContext();
    setSesion(null);
    window.location.replace("/login?logout=1");
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
        cerrarSesionAuthContext();
        router.replace("/login?logout=1");
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
        <SurfaceCard className="mb-6 overflow-hidden rounded-[28px] shadow-[0_24px_65px_rgba(12,52,80,0.12)]">
          <div className="bg-gradient-to-br from-white via-[#F8FCFE] to-[#E6F7EF] p-5 sm:p-7">
            <div className="flex flex-col gap-5 lg:flex-row lg:items-start lg:justify-between">
              <div>
                <p className="text-xs font-extrabold uppercase tracking-[0.2em] text-[var(--color-secondary)]">
                  PANEL ADMINISTRADOR
                </p>
                <h1 className="mt-2 text-3xl font-extrabold leading-tight text-[var(--color-primary)] sm:text-4xl">
                  Detalle de solicitud
                </h1>
                <p className="mt-3 max-w-2xl text-sm leading-6 text-[var(--color-muted)] sm:text-base">
                  Revisá los datos enviados, verificá contacto y horarios, y
                  definí el próximo paso de la solicitud.
                </p>
                {sesion && (
                  <p className="mt-3 text-sm font-bold text-[var(--color-muted)]">
                    {sesion.usuario.nombre} {sesion.usuario.apellido} ·{" "}
                    {sesion.usuario.email}
                  </p>
                )}
              </div>

              <div className="flex flex-col gap-3 sm:flex-row">
                <AppLinkButton
                  href="/admin/solicitudes"
                  variant="secondary"
                  size="lg"
                >
                  Volver al listado
                </AppLinkButton>
                <AppButton
                  type="button"
                  onClick={cerrarSesion}
                  variant="primary"
                  size="lg"
                >
                  Cerrar sesión
                </AppButton>
              </div>
            </div>
          </div>
        </SurfaceCard>

        {cargandoDetalle && (
          <div
            role="status"
            className="rounded-[24px] border border-[#DDEAF3] bg-white p-7 text-center shadow-[0_14px_35px_rgba(12,52,80,0.08)]"
          >
            <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-full bg-[#E8F6FB]">
              <div className="h-8 w-8 animate-spin rounded-full border-4 border-[#BFDDEA] border-t-[var(--color-secondary)]" />
            </div>
            <p className="mt-4 text-sm font-bold text-[var(--color-primary)]">
              Cargando solicitud...
            </p>
          </div>
        )}

        {!cargandoDetalle && errorDetalle && (
          <StatusMessage
            variant="error"
            title="No pudimos cargar la solicitud"
            className="p-6"
          >
            <p className="mt-3">
              {errorDetalle}
            </p>
            <p className="mt-2 font-bold">
              Probá volver al listado o intentá nuevamente en unos minutos.
            </p>
          </StatusMessage>
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
      <SurfaceCard
        as="section"
        className="overflow-hidden rounded-[28px] shadow-[0_18px_45px_rgba(12,52,80,0.10)]"
      >
        <div className="bg-gradient-to-br from-[#F8FCFE] to-white p-5 sm:p-6">
          <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
            <div>
              <p className="text-xs font-extrabold uppercase tracking-[0.18em] text-[var(--color-muted)]">
                {solicitud.codigoSeguimiento}
              </p>
              <h2 className="mt-2 text-2xl font-extrabold leading-tight text-[var(--color-primary)] sm:text-3xl">
                {solicitud.nombreActividad}
              </h2>
              <p className="mt-3 text-sm font-bold text-[var(--color-muted)]">
                Solicitud recibida para revisión y publicación.
              </p>
            </div>
            <AdminEstadoBadge estado={solicitud.estado} />
          </div>

          <div className="mt-6 grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
            <CampoDetalle etiqueta="Identificador interno" valor={solicitud.id} />
            <CampoDetalle
              etiqueta="Creación"
              valor={formatearFecha(solicitud.createdAt)}
            />
            <CampoDetalle
              etiqueta="Actualización"
              valor={formatearFecha(solicitud.updatedAt)}
            />
            <CampoDetalle etiqueta="Canal de origen" valor={solicitud.origen} />
            <CampoDetalle
              etiqueta="Revisión iniciada"
              valor={formatearFechaOpcional(solicitud.revisionIniciadaAt)}
            />
            <CampoDetalle
              etiqueta="Revisión finalizada"
              valor={formatearFechaOpcional(solicitud.revisionFinalizadaAt)}
            />
            <CampoDetalle
              etiqueta="Actividad publicada"
              valor={solicitud.actividadGeneradaId}
            />
            <CampoDetalleEstado
              etiqueta="Estado actual"
              estado={solicitud.estado}
            />
          </div>
        </div>
      </SurfaceCard>

      <section className="grid gap-5 lg:grid-cols-2">
        <DetalleCard
          titulo="Publicador y contacto"
          descripcion="Datos para identificar a quién envió la solicitud y cómo comunicarse."
        >
          <CampoDetalle etiqueta="Tipo" valor={solicitud.tipoPublicador} />
          <CampoDetalle etiqueta="Nombre" valor={solicitud.nombrePublicador} />
          <CampoDetalle etiqueta="WhatsApp" valor={solicitud.whatsapp} />
          <CampoDetalle etiqueta="Instagram" valor={solicitud.instagram} />
          <CampoDetalle etiqueta="Email" valor={solicitud.email} />
        </DetalleCard>

        <DetalleCard
          titulo="Actividad"
          descripcion="Información principal que verá la persona interesada en entrenar."
        >
          <CampoDetalle etiqueta="Nombre" valor={solicitud.nombreActividad} />
          <CampoDetalle
            etiqueta="Deporte existente"
            valor={solicitud.deporteNombre}
          />
          <CampoDetalle etiqueta="ID deporte" valor={solicitud.deporteId} />
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
        <DetalleCard
          titulo="Ubicación"
          descripcion="Ciudad, barrio y referencias para ubicar dónde se realiza."
        >
          <CampoDetalle etiqueta="Ciudad existente" valor={solicitud.ciudadNombre} />
          <CampoDetalle etiqueta="ID ciudad" valor={solicitud.ciudadId} />
          <CampoDetalle etiqueta="Ciudad otra" valor={solicitud.ciudadOtra} />
          <CampoDetalle etiqueta="Barrio existente" valor={solicitud.barrioNombre} />
          <CampoDetalle etiqueta="ID barrio" valor={solicitud.barrioId} />
          <CampoDetalle etiqueta="Barrio otro" valor={solicitud.barrioOtro} />
          <CampoDetalle etiqueta="Nombre del lugar" valor={solicitud.nombreLugar} />
          <CampoDetalle etiqueta="Dirección" valor={solicitud.direccion} />
          <CampoDetalle
            etiqueta="Referencia"
            valor={solicitud.referenciaUbicacion}
          />
        </DetalleCard>

        <DetalleCard
          titulo="Revisión"
          descripcion="Notas y estado interno para tomar una decisión consistente."
        >
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

      <SurfaceCard as="section" className="rounded-[24px] p-5">
        <SectionHeader
          title="Horarios"
          description="Días y franjas cargadas por quien envió la solicitud."
        />

        {solicitud.horarios.length === 0 ? (
          <StatusMessage
            variant="info"
            title="Sin horarios informados"
            className="mt-4"
          >
            <p>
              La solicitud todavía no incluye días u horarios. Podés revisarlo
              con el publicador antes de aprobar.
            </p>
          </StatusMessage>
        ) : (
          <div className="mt-4 grid gap-3 md:grid-cols-2">
            {solicitud.horarios.map((horario) => (
              <HorarioCard key={horario.id} horario={horario} />
            ))}
          </div>
        )}
      </SurfaceCard>
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
    <SurfaceCard as="section" className="rounded-[24px] p-5">
      <SectionHeader
        eyebrow="Revisión"
        title="Acciones de revisión"
        className="border-b border-[#DDEAF3] pb-3"
      />

      <div className="mt-4 rounded-[18px] border border-[#DDEAF3] bg-[#F8FCFE] p-4">
        <CampoDetalleEstado
          etiqueta="Estado actual"
          estado={solicitud.estado}
        />
      </div>

      {estaRechazada && (
        <StatusMessage variant="error" className="mt-5 font-bold">
          Esta solicitud ya fue rechazada. El motivo queda registrado en la
          sección de revisión.
        </StatusMessage>
      )}

      {estaAprobada && (
        <StatusMessage variant="success" className="mt-5 font-bold">
          Esta solicitud ya fue aprobada y la actividad quedó lista para verse
          públicamente si el backend generó el enlace.
        </StatusMessage>
      )}

      {puedeAprobar && (
        <div className="mt-5 grid gap-4 rounded-[20px] border border-[#BDE8D0] bg-[#F6FCF8] p-4">
          <div>
            <h3 className="text-base font-extrabold text-[var(--color-primary)]">
              Aprobar publicación
            </h3>
            <p className="mt-1 text-sm leading-6 text-[var(--color-muted)]">
              Usá esta acción cuando la información esté completa. Se creará una
              actividad pública visible en{" "}
              <BrandName className="inline font-bold" />.
            </p>
          </div>
          {!confirmandoAprobacion && (
            <AppButton
              type="button"
              onClick={onSolicitarAprobacion}
              disabled={accionEnCurso}
              size="lg"
              fullWidth
            >
              {accionEnCurso ? "Procesando..." : "Aprobar y publicar actividad"}
            </AppButton>
          )}

          {confirmandoAprobacion && (
            <div className="rounded-[18px] border border-[#F7D87A] bg-[#FFF8E1] px-4 py-4">
              <p className="text-sm font-bold leading-6 text-[#7A5A00]">
                Esta acción creará una actividad pública visible en{" "}
                <BrandName className="inline" />. Revisá que los datos estén
                listos antes de confirmar.
              </p>
              <div className="mt-4 flex flex-col gap-3 sm:flex-row">
                <AppButton
                  type="button"
                  onClick={onCancelarAprobacion}
                  disabled={accionEnCurso}
                  variant="outline"
                  fullWidth
                  className="border-[#D9B94E] text-[#7A5A00] hover:border-[#A98300]"
                >
                  Cancelar
                </AppButton>
                <AppButton
                  type="button"
                  onClick={onConfirmarAprobacion}
                  disabled={accionEnCurso}
                  fullWidth
                >
                  {accionEnCurso ? "Procesando..." : "Confirmar aprobación"}
                </AppButton>
              </div>
            </div>
          )}
        </div>
      )}

      {puedeRechazar && (
        <div className="mt-5 grid gap-5 rounded-[20px] border border-[#DDEAF3] bg-[#F8FCFE] p-4">
          {estaEnRevision && (
            <StatusMessage variant="info" className="font-bold">
              La solicitud ya está en revisión. Podés rechazarla si detectás
              información insuficiente o incorrecta.
            </StatusMessage>
          )}

          {puedeMarcarEnRevision && (
            <div className="rounded-[18px] border border-[#A9D8EA] bg-white p-4">
              <h3 className="text-base font-extrabold text-[var(--color-primary)]">
                Seguimiento de revisión
              </h3>
              <p className="mt-1 text-sm leading-6 text-[var(--color-muted)]">
                Marcá la solicitud cuando empiece el análisis del equipo para
                que el listado refleje su avance.
              </p>
              <AppButton
                type="button"
                onClick={onMarcarEnRevision}
                disabled={accionEnCurso}
                fullWidth
                className="mt-3"
              >
                {accionEnCurso ? "Procesando..." : "Marcar en revisión"}
              </AppButton>
            </div>
          )}

          <div className="rounded-[18px] border border-red-100 bg-white p-4">
            <label
              htmlFor="motivo-rechazo"
              className="text-sm font-bold text-[var(--color-primary)]"
            >
              Motivo de rechazo
            </label>
            <p className="mt-1 text-sm leading-6 text-[var(--color-muted)]">
              Escribí un motivo claro y breve para dejar registro de la
              decisión.
            </p>
            <textarea
              id="motivo-rechazo"
              value={motivoRechazo}
              onChange={(evento) =>
                onMotivoRechazoChange(evento.target.value)
              }
              disabled={accionEnCurso}
              rows={4}
              className="mt-3 w-full resize-y rounded-[18px] border border-[#BFDDEA] bg-[#F8FAFC] px-4 py-3 text-sm leading-6 text-[var(--color-text)] outline-none transition duration-200 ease-out hover:border-[var(--color-accent)] focus:border-[var(--color-accent)] focus:ring-4 focus:ring-[#DDEAF3] disabled:cursor-not-allowed disabled:opacity-70"
            />
          </div>

          <AppButton
            type="button"
            onClick={onRechazarSolicitud}
            disabled={accionEnCurso}
            variant="danger"
            size="lg"
            fullWidth
          >
            {accionEnCurso ? "Procesando..." : "Rechazar solicitud"}
          </AppButton>
        </div>
      )}

      {errorAccion && (
        <StatusMessage variant="error" className="mt-5 font-bold">
          {errorAccion}
        </StatusMessage>
      )}

      {respuestaAprobacion && (
        <StatusMessage variant="success" className="mt-5 font-bold">
          <p>{respuestaAprobacion.mensaje}</p>
          <p className="mt-2">
            Actividad creada: {respuestaAprobacion.actividadTitulo}
          </p>
          <p className="mt-1">ID actividad: {respuestaAprobacion.actividadId}</p>
          {actividadSlugAprobada && (
            <AppLinkButton
              href={`/actividades/${actividadSlugAprobada}`}
              variant="success"
              size="sm"
              className="mt-3 rounded-[16px]"
            >
              Ver actividad pública
            </AppLinkButton>
          )}
        </StatusMessage>
      )}

      {exitoAccion && !respuestaAprobacion && (
        <StatusMessage variant="success" className="mt-5 font-bold">
          {exitoAccion}
        </StatusMessage>
      )}
    </SurfaceCard>
  );
}

function DetalleCard({
  titulo,
  descripcion,
  children,
}: {
  titulo: string;
  descripcion?: string;
  children: ReactNode;
}) {
  return (
    <SurfaceCard
      as="section"
      className="rounded-[24px] p-5 transition duration-200 ease-out hover:-translate-y-0.5 hover:shadow-[0_18px_42px_rgba(12,52,80,0.12)]"
    >
      <div className="border-b border-[#DDEAF3] pb-3">
        <SectionHeader title={titulo} description={descripcion} />
      </div>
      <dl className="mt-4 grid gap-3">{children}</dl>
    </SurfaceCard>
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
    <div className="rounded-[18px] border border-[#EDF4F8] bg-[#F8FCFE] px-4 py-3">
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
    <div className="rounded-[18px] border border-[#EDF4F8] bg-[#F8FCFE] px-4 py-3">
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
    <SurfaceCard
      as="article"
      variant="info"
      className="rounded-[20px] p-4 transition duration-200 ease-out hover:-translate-y-0.5 hover:shadow-[0_12px_30px_rgba(12,52,80,0.08)]"
    >
      <p className="inline-flex rounded-full bg-[#E6F7EF] px-3 py-1 text-sm font-extrabold text-[#167A4A]">
        {horario.diaSemana}
      </p>
      <dl className="mt-3 grid gap-3 text-sm">
        <CampoDetalle etiqueta="Registro interno" valor={horario.id} />
        <CampoDetalle etiqueta="Hora inicio" valor={horario.horaInicio} />
        <CampoDetalle etiqueta="Hora fin" valor={horario.horaFin} />
        <CampoDetalle etiqueta="Observación" valor={horario.observacion} />
      </dl>
    </SurfaceCard>
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

function obtenerRutaLoginAdmin(rutaRetorno: string): string {
  if (hayLogoutRecienteAuth()) {
    return "/login?logout=1";
  }

  return `/login?returnTo=${encodeURIComponent(rutaRetorno)}`;
}
