"use client";

import Image from "next/image";
import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import type { ChangeEvent } from "react";

import { AdminGuard } from "../../../components/admin/AdminGuard";
import { useAuthSession } from "../../../components/auth/AuthSessionProvider";
import { AppButton } from "../../../components/ui/AppButton";
import { AppLinkButton } from "../../../components/ui/AppLinkButton";
import { StatusMessage } from "../../../components/ui/StatusMessage";
import { SurfaceCard } from "../../../components/ui/SurfaceCard";
import { construirUrlImagenBackend } from "../../../lib/backendUrl";
import { obtenerSesionAdmin } from "../../../services/authService";
import {
  AdminApiError,
  aprobarImagenAdmin,
  listarImagenesAdmin,
  rechazarImagenAdmin,
  type ImagenAdmin,
} from "../../../services/adminSolicitudesService";

const ESTILOS_ESTADO: Record<string, string> = {
  PENDIENTE: "bg-[#FFF7E6] text-[#8A5A00] ring-1 ring-[#F5D48F]",
  APROBADA: "bg-[#E6F7EF] text-[#1D7B4A] ring-1 ring-[#BDE8D0]",
  RECHAZADA: "bg-red-50 text-red-700 ring-1 ring-red-200",
};

const FILTROS_ESTADO = [
  { valor: "PENDIENTE", etiqueta: "Pendientes" },
  { valor: "APROBADA", etiqueta: "Aprobadas" },
  { valor: "RECHAZADA", etiqueta: "Rechazadas" },
  { valor: "", etiqueta: "Todas" },
];

function formatearEstado(estado: string): string {
  return estado
    .toLowerCase()
    .split("_")
    .map((parte) => parte.charAt(0).toUpperCase() + parte.slice(1))
    .join(" ");
}

export default function AdminImagenesPage() {
  return (
    <AdminGuard>
      <AdminImagenesListado />
    </AdminGuard>
  );
}

function AdminImagenesListado() {
  const router = useRouter();
  const { cerrarSesion } = useAuthSession();
  const [imagenes, setImagenes] = useState<ImagenAdmin[]>([]);
  /*
    La cola arranca en PENDIENTE: es el trabajo diario del admin.
  */
  const [filtroEstado, setFiltroEstado] = useState("PENDIENTE");
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [imagenEnAccion, setImagenEnAccion] = useState<number | null>(null);
  const [rechazoAbierto, setRechazoAbierto] = useState<number | null>(null);
  const [motivoRechazo, setMotivoRechazo] = useState("");
  const [mensajeAccion, setMensajeAccion] = useState<string | null>(null);
  const [errorAccion, setErrorAccion] = useState<string | null>(null);

  useEffect(() => {
    let componenteActivo = true;

    const sesion = obtenerSesionAdmin();

    if (!sesion) {
      router.replace(
        `/login?returnTo=${encodeURIComponent("/admin/imagenes")}`
      );
      return () => {
        componenteActivo = false;
      };
    }

    listarImagenesAdmin(
      { estado: filtroEstado || undefined, page: 0, size: 50 },
      sesion.accessToken
    )
      .then((pagina) => {
        if (componenteActivo) {
          setImagenes(pagina.contenido);
          setError(null);
        }
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

        setError("Ocurrió un problema inesperado al cargar las imágenes.");
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
    setMensajeAccion(null);
    setErrorAccion(null);
    setRechazoAbierto(null);
    setFiltroEstado(evento.target.value);
  }

  function actualizarImagenEnLista(imagenActualizada: ImagenAdmin) {
    setImagenes((previas) =>
      filtroEstado && imagenActualizada.estadoModeracion !== filtroEstado
        ? previas.filter((item) => item.id !== imagenActualizada.id)
        : previas.map((item) =>
            item.id === imagenActualizada.id ? imagenActualizada : item
          )
    );
  }

  async function manejarAprobar(imagen: ImagenAdmin) {
    const sesion = obtenerSesionAdmin();

    if (!sesion || imagenEnAccion !== null) {
      return;
    }

    setImagenEnAccion(imagen.id);
    setMensajeAccion(null);
    setErrorAccion(null);

    try {
      const actualizada = await aprobarImagenAdmin(imagen.id, sesion.accessToken);
      actualizarImagenEnLista(actualizada);
      setMensajeAccion("Imagen aprobada: ya se ve en la página pública.");
    } catch (errorAprobar: unknown) {
      setErrorAccion(
        errorAprobar instanceof AdminApiError
          ? errorAprobar.message
          : "No pudimos aprobar la imagen. Probá nuevamente."
      );
    } finally {
      setImagenEnAccion(null);
    }
  }

  async function manejarRechazar(imagen: ImagenAdmin) {
    const sesion = obtenerSesionAdmin();
    const motivo = motivoRechazo.trim();

    if (!sesion || imagenEnAccion !== null) {
      return;
    }

    if (!motivo) {
      setErrorAccion("Indicá el motivo del rechazo.");
      return;
    }

    setImagenEnAccion(imagen.id);
    setMensajeAccion(null);
    setErrorAccion(null);

    try {
      const actualizada = await rechazarImagenAdmin(
        imagen.id,
        motivo,
        sesion.accessToken
      );
      actualizarImagenEnLista(actualizada);
      setMensajeAccion("Imagen rechazada: el publicador va a ver el motivo.");
      setRechazoAbierto(null);
      setMotivoRechazo("");
    } catch (errorRechazar: unknown) {
      setErrorAccion(
        errorRechazar instanceof AdminApiError
          ? errorRechazar.message
          : "No pudimos rechazar la imagen. Probá nuevamente."
      );
    } finally {
      setImagenEnAccion(null);
    }
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
                  Moderación de imágenes
                </h1>
                <p className="mt-3 max-w-2xl text-sm leading-6 text-[var(--color-muted)] sm:text-base">
                  Fotos subidas por publicadores. Nada se muestra en público
                  hasta que lo apruebes.
                </p>
              </div>

              <div className="flex flex-col gap-3 sm:flex-row lg:flex-col lg:items-end">
                <AppLinkButton href="/admin/solicitudes" variant="secondary">
                  Solicitudes de publicación
                </AppLinkButton>
                <AppLinkButton href="/admin/solicitudes-cambio" variant="outline">
                  Solicitudes de cambio
                </AppLinkButton>
              </div>
            </div>
          </div>
        </SurfaceCard>

        <SurfaceCard className="mb-5 rounded-[24px] bg-white/90 p-5 shadow-[0_14px_35px_rgba(12,52,80,0.08)]">
          <label
            htmlFor="filtro-estado-imagenes"
            className="block text-sm font-bold text-[var(--color-primary)]"
          >
            Filtrar por estado
          </label>
          <select
            id="filtro-estado-imagenes"
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

        {mensajeAccion ? (
          <StatusMessage variant="success" role="status" className="mb-4">
            {mensajeAccion}
          </StatusMessage>
        ) : null}

        {errorAccion ? (
          <StatusMessage variant="error" role="alert" className="mb-4">
            {errorAccion}
          </StatusMessage>
        ) : null}

        {cargando ? (
          <StatusMessage variant="info" role="status">
            Cargando imágenes...
          </StatusMessage>
        ) : null}

        {error ? (
          <StatusMessage variant="error" role="alert">
            {error}
          </StatusMessage>
        ) : null}

        {!cargando && !error && imagenes.length === 0 ? (
          <SurfaceCard className="p-10 text-center">
            <h3 className="text-lg font-extrabold text-[var(--color-primary)]">
              No hay imágenes
              {filtroEstado
                ? ` en estado ${formatearEstado(filtroEstado).toLowerCase()}`
                : ""}
            </h3>
            <p className="mx-auto mt-2 max-w-md text-sm text-[var(--color-muted)]">
              Cuando un publicador suba fotos para sus actividades van a
              aparecer acá para tu revisión.
            </p>
          </SurfaceCard>
        ) : null}

        {imagenes.length > 0 ? (
          <div className="grid gap-4">
            {imagenes.map((imagen) => {
              const urlAbsoluta = construirUrlImagenBackend(imagen.url);
              const esPendiente = imagen.estadoModeracion === "PENDIENTE";
              const enAccion = imagenEnAccion === imagen.id;

              return (
                <SurfaceCard key={imagen.id} className="p-5 sm:p-6">
                  <div className="flex flex-wrap items-start gap-4">
                    {urlAbsoluta ? (
                      <Image
                        src={urlAbsoluta}
                        alt={`Imagen ${imagen.tipoImagen.toLowerCase()} de ${
                          imagen.actividadTitulo ?? "una actividad"
                        }`}
                        width={160}
                        height={112}
                        className="h-28 w-40 rounded-[14px] object-cover"
                      />
                    ) : null}

                    <div className="min-w-0 flex-1">
                      <div className="flex flex-wrap items-center gap-2">
                        <span
                          className={`rounded-full px-3 py-1.5 text-xs font-extrabold ${
                            ESTILOS_ESTADO[imagen.estadoModeracion] ??
                            "bg-[#F8FAFC] text-[var(--color-muted)] ring-1 ring-[#DDEAF3]"
                          }`}
                        >
                          {formatearEstado(imagen.estadoModeracion)}
                        </span>
                        <span className="rounded-full bg-[#E8F6FB] px-3 py-1 text-xs font-bold text-[#0F6F8F]">
                          {imagen.tipoImagen === "PRINCIPAL"
                            ? "Principal"
                            : "Galería"}
                        </span>
                      </div>

                      <h3 className="mt-2 text-lg font-extrabold text-[var(--color-primary)]">
                        {imagen.actividadTitulo || "Actividad"}
                      </h3>

                      {imagen.actividadSlug ? (
                        <AppLinkButton
                          href={`/actividades/${imagen.actividadSlug}`}
                          variant="outline"
                          size="sm"
                          className="mt-2"
                        >
                          Ver la actividad publicada
                        </AppLinkButton>
                      ) : null}

                      {imagen.motivoRechazo ? (
                        <p className="mt-2 text-sm text-red-700">
                          <span className="font-bold">Motivo:</span>{" "}
                          {imagen.motivoRechazo}
                        </p>
                      ) : null}
                    </div>

                    {esPendiente ? (
                      <div className="flex w-full flex-col gap-2 sm:w-auto">
                        <AppButton
                          type="button"
                          variant="success"
                          onClick={() => manejarAprobar(imagen)}
                          disabled={enAccion || imagenEnAccion !== null}
                        >
                          {enAccion ? "Procesando..." : "Aprobar"}
                        </AppButton>
                        <AppButton
                          type="button"
                          variant="danger"
                          onClick={() => {
                            setRechazoAbierto((previo) =>
                              previo === imagen.id ? null : imagen.id
                            );
                            setMotivoRechazo("");
                            setErrorAccion(null);
                          }}
                          disabled={imagenEnAccion !== null}
                        >
                          Rechazar
                        </AppButton>
                      </div>
                    ) : null}
                  </div>

                  {rechazoAbierto === imagen.id ? (
                    <div className="mt-4 border-t border-[#DDEAF3] pt-4">
                      <label
                        htmlFor={`motivo-rechazo-${imagen.id}`}
                        className="text-sm font-bold text-[var(--color-primary)]"
                      >
                        Motivo del rechazo (se le muestra al publicador)
                      </label>
                      <textarea
                        id={`motivo-rechazo-${imagen.id}`}
                        rows={2}
                        maxLength={2000}
                        value={motivoRechazo}
                        onChange={(evento) => setMotivoRechazo(evento.target.value)}
                        disabled={imagenEnAccion !== null}
                        className="mt-2 min-h-20 w-full rounded-[18px] border border-[#BFDDEA] bg-[#F8FAFC] px-4 py-3 text-base leading-6 text-[var(--color-text)] outline-none transition duration-200 ease-out hover:border-[var(--color-accent)] focus:border-[var(--color-accent)] focus:ring-4 focus:ring-[#DDEAF3] disabled:cursor-not-allowed disabled:opacity-70"
                      />
                      <AppButton
                        type="button"
                        variant="danger"
                        onClick={() => manejarRechazar(imagen)}
                        disabled={imagenEnAccion !== null}
                        className="mt-3"
                      >
                        Confirmar rechazo
                      </AppButton>
                    </div>
                  ) : null}
                </SurfaceCard>
              );
            })}
          </div>
        ) : null}
      </section>
    </main>
  );
}
