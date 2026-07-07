"use client";

import { useEffect, useMemo, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import { useAuthSession } from "../auth/AuthSessionProvider";
import { AppLinkButton } from "../ui/AppLinkButton";
import { SectionHeader } from "../ui/SectionHeader";
import { StatusMessage } from "../ui/StatusMessage";
import { SurfaceCard } from "../ui/SurfaceCard";
import { PublicadorPageHeader } from "./PublicadorPageHeader";
import {
  PublicadorApiError,
  obtenerSolicitudPublicador,
} from "../../services/publicadorService";
import { PublicadorSolicitudEstadoBadge } from "./PublicadorSolicitudEstadoBadge";
import type { SolicitudPublicadorDetalle } from "../../types/publicador";

export function PublicadorSolicitudDetail() {
  const router = useRouter();
  const params = useParams<{ id: string }>();
  const { accessToken, cerrarSesion } = useAuthSession();
  const idSolicitud = useMemo(() => Number(params.id), [params.id]);
  const idSolicitudInvalido =
    !Number.isInteger(idSolicitud) || idSolicitud <= 0;
  const [solicitud, setSolicitud] =
    useState<SolicitudPublicadorDetalle | null>(null);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [noEncontrada, setNoEncontrada] = useState(false);

  useEffect(() => {
    let componenteActivo = true;

    if (!accessToken) {
      return () => {
        componenteActivo = false;
      };
    }

    if (idSolicitudInvalido) {
      return () => {
        componenteActivo = false;
      };
    }

    obtenerSolicitudPublicador(idSolicitud, accessToken)
      .then((solicitudActual) => {
        if (!componenteActivo) {
          return;
        }

        setSolicitud(solicitudActual);
        setError(null);
        setNoEncontrada(false);
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
                `/publicador/solicitudes/${idSolicitud}`
              )}`
            );
            return;
          }

          if (errorCarga.status === 403) {
            setError("No tenés permisos para acceder al panel publicador.");
            return;
          }

          if (errorCarga.status === 404) {
            setNoEncontrada(true);
            return;
          }

          setError(errorCarga.message);
          return;
        }

        setError("Ocurrió un problema inesperado al cargar la solicitud.");
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
  }, [accessToken, cerrarSesion, idSolicitud, idSolicitudInvalido, router]);

  const mostrarCargando = cargando && !idSolicitudInvalido;
  const mostrarNoEncontrada = noEncontrada || idSolicitudInvalido;

  return (
    <main className="min-h-screen bg-gradient-to-br from-[#F8FAFC] via-white to-[#E8F6FB] px-4 py-8 text-[var(--color-text)] sm:py-12">
      <section className="mx-auto w-full max-w-6xl">
        <PublicadorPageHeader
          title="Detalle de solicitud"
          description="Revisá la información enviada, el estado de revisión y los próximos pasos de tu publicación."
          action={
            <AppLinkButton
              href="/publicador/solicitudes"
              variant="secondary"
              fullWidth
            >
              Volver a mis solicitudes
            </AppLinkButton>
          }
        />

        {mostrarCargando ? (
          <StatusMessage variant="info" role="status" className="mt-6">
            Cargando solicitud...
          </StatusMessage>
        ) : null}

        {error ? (
          <StatusMessage variant="error" role="alert" className="mt-6">
            {error}
          </StatusMessage>
        ) : null}

        {mostrarNoEncontrada ? (
          <SurfaceCard className="mt-6 p-6 text-center sm:p-8">
            <StatusMessage variant="warning" role="alert">
              No encontramos esta solicitud.
            </StatusMessage>
            <div className="mt-5">
              <AppLinkButton href="/publicador/solicitudes" variant="secondary">
                Volver a mis solicitudes
              </AppLinkButton>
            </div>
          </SurfaceCard>
        ) : null}

        {solicitud ? (
          <div className="mt-6 grid gap-5 lg:grid-cols-[1.1fr_0.9fr]">
            <SurfaceCard className="border-[#BDE8D0] bg-gradient-to-br from-white via-white to-[#F8FCFE] p-6 sm:p-8">
              <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
                <div>
                  <p className="text-xs font-extrabold uppercase tracking-[0.16em] text-[var(--color-secondary)]">
                    {solicitud.codigoSeguimiento}
                  </p>
                  <h2 className="mt-2 text-2xl font-extrabold text-[var(--color-primary)] sm:text-3xl">
                    {solicitud.nombreActividad}
                  </h2>
                </div>
                <PublicadorSolicitudEstadoBadge estado={solicitud.estado} />
              </div>

              <MensajeEstado solicitud={solicitud} />

              <div className="mt-8 grid gap-5">
                <SeccionDetalle titulo="Actividad">
                  <CampoDetalle etiqueta="Deporte" valor={obtenerDeporte(solicitud)} />
                  <CampoDetalle etiqueta="Descripción" valor={solicitud.descripcion} />
                  <CampoDetalle etiqueta="Nivel" valor={formatearCatalogo(solicitud.nivel)} />
                  <CampoDetalle etiqueta="Enfoque" valor={formatearCatalogo(solicitud.enfoque)} />
                  <CampoDetalle etiqueta="Modalidad" valor={formatearCatalogo(solicitud.modalidad)} />
                  <CampoDetalle etiqueta="Edades" valor={formatearEdades(solicitud)} />
                  <CampoDetalle etiqueta="Precio" valor={formatearPrecio(solicitud)} />
                </SeccionDetalle>

                <SeccionDetalle titulo="Ubicación">
                  <CampoDetalle etiqueta="Ciudad" valor={solicitud.ciudadNombre ?? solicitud.ciudadOtra} />
                  <CampoDetalle etiqueta="Barrio" valor={solicitud.barrioNombre ?? solicitud.barrioOtro} />
                  <CampoDetalle etiqueta="Lugar" valor={solicitud.nombreLugar} />
                  <CampoDetalle etiqueta="Dirección" valor={solicitud.direccion} />
                  <CampoDetalle etiqueta="Referencia" valor={solicitud.referenciaUbicacion} />
                </SeccionDetalle>

                <SeccionDetalle titulo="Contacto">
                  <CampoDetalle etiqueta="WhatsApp" valor={solicitud.whatsapp} />
                  <CampoDetalle etiqueta="Instagram" valor={solicitud.instagram} />
                  <CampoDetalle etiqueta="Email" valor={solicitud.email} />
                </SeccionDetalle>
              </div>
            </SurfaceCard>

            <div className="grid gap-5">
              <SurfaceCard variant="info" className="p-6 sm:p-8">
                <SectionHeader
                  eyebrow="Revisión"
                  title="Seguimiento de revisión"
                  description="Fechas, observaciones y señales útiles para entender cómo avanza tu solicitud."
                />
                <dl className="mt-6 grid gap-4">
                  <CampoDetalle etiqueta="Creada" valor={formatearFechaHora(solicitud.createdAt)} />
                  <CampoDetalle etiqueta="Actualizada" valor={formatearFechaHora(solicitud.updatedAt)} />
                  <CampoDetalle etiqueta="Inicio de revisión" valor={formatearFechaHora(solicitud.revisionIniciadaAt)} />
                  <CampoDetalle etiqueta="Fin de revisión" valor={formatearFechaHora(solicitud.revisionFinalizadaAt)} />
                  <CampoDetalle etiqueta="Observaciones enviadas" valor={solicitud.observacionesSolicitante} />
                </dl>

                {solicitud.motivoRechazo ? (
                  <StatusMessage variant="error" className="mt-5">
                    {solicitud.motivoRechazo}
                  </StatusMessage>
                ) : null}

                {solicitud.actividadGeneradaId ? (
                  <StatusMessage variant="success" className="mt-5">
                    Actividad publicada vinculada: #{solicitud.actividadGeneradaId}
                  </StatusMessage>
                ) : null}
              </SurfaceCard>

              <SurfaceCard className="p-6 sm:p-8">
                <SectionHeader
                  eyebrow="Horarios"
                  title="Horarios asociados"
                  description="Días y franjas informadas en la solicitud."
                />
                {solicitud.horarios.length > 0 ? (
                  <div className="mt-6 grid gap-3">
                    {solicitud.horarios.map((horario) => (
                      <div
                        key={horario.id}
                        className="rounded-[18px] border border-[#DDEAF3] bg-gradient-to-br from-white to-[#F8FCFE] p-4 transition duration-200 ease-out hover:border-[#BDE8D0]"
                      >
                        <p className="text-sm font-extrabold text-[var(--color-primary)]">
                          {formatearCatalogo(horario.diaSemana)}
                        </p>
                        <p className="mt-1 text-sm font-bold text-[var(--color-muted)]">
                          {horario.horaInicio} a {horario.horaFin}
                        </p>
                        {horario.observacion ? (
                          <p className="mt-2 text-sm leading-6 text-[var(--color-muted)]">
                            {horario.observacion}
                          </p>
                        ) : null}
                      </div>
                    ))}
                  </div>
                ) : (
                  <StatusMessage variant="info" className="mt-6">
                    No se informaron horarios.
                  </StatusMessage>
                )}
              </SurfaceCard>
            </div>
          </div>
        ) : null}
      </section>
    </main>
  );
}

function MensajeEstado({
  solicitud,
}: {
  solicitud: SolicitudPublicadorDetalle;
}) {
  if (solicitud.estado === "RECHAZADA") {
    return (
      <StatusMessage variant="error" className="mt-6">
        Tu solicitud fue rechazada. Revisá el motivo para poder corregir la
        información en un nuevo envío.
      </StatusMessage>
    );
  }

  if (solicitud.estado === "APROBADA") {
    return (
      <StatusMessage variant="success" className="mt-6">
        Tu solicitud fue aprobada y ya está vinculada a una actividad publicada.
      </StatusMessage>
    );
  }

  if (solicitud.estado === "EN_REVISION") {
    return (
      <StatusMessage variant="info" className="mt-6">
        Estamos revisando tu solicitud.
      </StatusMessage>
    );
  }

  return (
    <StatusMessage variant="warning" className="mt-6">
      Tu solicitud está pendiente de revisión.
    </StatusMessage>
  );
}

function SeccionDetalle({
  titulo,
  children,
}: {
  titulo: string;
  children: React.ReactNode;
}) {
  return (
    <section className="rounded-[22px] border border-[#DDEAF3] bg-white/80 p-4 shadow-[0_10px_24px_rgba(12,52,80,0.05)]">
      <h3 className="text-lg font-extrabold text-[var(--color-primary)]">
        {titulo}
      </h3>
      <dl className="mt-4 grid gap-4 sm:grid-cols-2">{children}</dl>
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
      <dt className="text-xs font-extrabold uppercase tracking-[0.14em] text-[var(--color-secondary)]">
        {etiqueta}
      </dt>
      <dd className="mt-1 break-words text-sm font-bold text-[var(--color-primary)]">
        {valor ?? "No informado"}
      </dd>
    </div>
  );
}

function obtenerDeporte(solicitud: SolicitudPublicadorDetalle): string {
  return solicitud.deporteNombre ?? solicitud.deporteOtro ?? "No informado";
}

function formatearEdades(solicitud: SolicitudPublicadorDetalle): string {
  if (solicitud.edadMinima === null && solicitud.edadMaxima === null) {
    return "No informado";
  }

  if (solicitud.edadMinima !== null && solicitud.edadMaxima !== null) {
    return `${solicitud.edadMinima} a ${solicitud.edadMaxima} años`;
  }

  if (solicitud.edadMinima !== null) {
    return `Desde ${solicitud.edadMinima} años`;
  }

  return `Hasta ${solicitud.edadMaxima} años`;
}

function formatearPrecio(solicitud: SolicitudPublicadorDetalle): string {
  if (!solicitud.mostrarPrecio || solicitud.precioReferencia === null) {
    return "No informado";
  }

  return new Intl.NumberFormat("es-AR", {
    style: "currency",
    currency: "ARS",
    maximumFractionDigits: 0,
  }).format(solicitud.precioReferencia);
}

function formatearCatalogo(valor: string): string {
  return valor
    .toLowerCase()
    .split("_")
    .map((parte) => parte.charAt(0).toUpperCase() + parte.slice(1))
    .join(" ");
}

function formatearFechaHora(valor: string | null): string | null {
  if (!valor) {
    return null;
  }

  const fecha = new Date(valor);

  if (Number.isNaN(fecha.getTime())) {
    return null;
  }

  return new Intl.DateTimeFormat("es-AR", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  }).format(fecha);
}
