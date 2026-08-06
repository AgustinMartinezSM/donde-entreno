"use client";

import { useEffect, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import type { FormEvent } from "react";

import { useAuthSession } from "../auth/AuthSessionProvider";
import { AppButton } from "../ui/AppButton";
import { AppLinkButton } from "../ui/AppLinkButton";
import { SectionHeader } from "../ui/SectionHeader";
import { StatusMessage } from "../ui/StatusMessage";
import { SurfaceCard } from "../ui/SurfaceCard";
import { PublicadorPageHeader } from "./PublicadorPageHeader";
import {
  PublicadorApiError,
  crearSolicitudCambio,
  obtenerActividadPublicador,
} from "../../services/publicadorService";
import type {
  ActividadPublicadorDetalle,
  SolicitudCambioDetalle,
  SolicitudCambioRequest,
} from "../../types/publicador";

const CLASE_INPUT =
  "mt-2 min-h-12 w-full rounded-[18px] border border-[#BFDDEA] bg-[#F8FAFC] px-4 text-base text-[var(--color-text)] outline-none transition duration-200 ease-out hover:border-[var(--color-accent)] focus:border-[var(--color-accent)] focus:ring-4 focus:ring-[#DDEAF3] disabled:cursor-not-allowed disabled:opacity-70";

const NIVELES = ["PRINCIPIANTE", "INTERMEDIO", "AVANZADO", "TODOS"] as const;
const MODALIDADES = ["PRESENCIAL", "ONLINE", "MIXTA"] as const;

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

/*
  Formulario de solicitud de cambios sobre una actividad publicada.

  Los campos arrancan con los valores actuales; al enviar, solo se
  proponen los que difieren del original. La actividad pública no
  cambia hasta que el equipo apruebe la solicitud.
*/
export function SolicitarCambiosForm() {
  const router = useRouter();
  const params = useParams<{ id: string }>();
  const actividadId = Number(params.id);
  const { accessToken, cerrarSesion } = useAuthSession();

  const [actividad, setActividad] = useState<ActividadPublicadorDetalle | null>(null);
  const [cargando, setCargando] = useState(true);
  const [errorCarga, setErrorCarga] = useState<string | null>(null);

  const [titulo, setTitulo] = useState("");
  const [descripcion, setDescripcion] = useState("");
  const [precio, setPrecio] = useState("");
  const [mostrarPrecio, setMostrarPrecio] = useState(false);
  const [whatsapp, setWhatsapp] = useState("");
  const [instagram, setInstagram] = useState("");
  const [email, setEmail] = useState("");
  const [nivel, setNivel] = useState("");
  const [modalidad, setModalidad] = useState("");

  const [enviando, setEnviando] = useState(false);
  const [errorEnvio, setErrorEnvio] = useState<string | null>(null);
  const [solicitudCreada, setSolicitudCreada] =
    useState<SolicitudCambioDetalle | null>(null);

  useEffect(() => {
    let componenteActivo = true;

    if (!accessToken || !Number.isFinite(actividadId)) {
      return () => {
        componenteActivo = false;
      };
    }

    obtenerActividadPublicador(actividadId, accessToken)
      .then((detalle) => {
        if (!componenteActivo) {
          return;
        }

        setActividad(detalle);
        setTitulo(detalle.titulo ?? "");
        setDescripcion(detalle.descripcion ?? "");
        setPrecio(
          detalle.precioReferencia !== null
            ? String(detalle.precioReferencia)
            : ""
        );
        setMostrarPrecio(Boolean(detalle.mostrarPrecio));
        setWhatsapp(detalle.whatsapp ?? "");
        setInstagram(detalle.instagram ?? "");
        setEmail(detalle.email ?? "");
        setNivel(detalle.nivel ?? "");
        setModalidad(detalle.modalidad ?? "");
        setErrorCarga(null);
      })
      .catch((error: unknown) => {
        if (!componenteActivo) {
          return;
        }

        if (error instanceof PublicadorApiError) {
          if (error.status === 401) {
            cerrarSesion();
            router.replace(
              `/login?returnTo=${encodeURIComponent("/publicador/actividades")}`
            );
            return;
          }

          setErrorCarga(error.message);
          return;
        }

        setErrorCarga("Ocurrió un problema inesperado al cargar la actividad.");
      })
      .finally(() => {
        if (componenteActivo) {
          setCargando(false);
        }
      });

    return () => {
      componenteActivo = false;
    };
  }, [accessToken, actividadId, cerrarSesion, router]);

  function construirCambiosPropuestos(): SolicitudCambioRequest {
    if (!actividad) {
      return {};
    }

    const cambios: SolicitudCambioRequest = {};

    const tituloLimpio = titulo.trim();
    if (tituloLimpio && tituloLimpio !== (actividad.titulo ?? "")) {
      cambios.titulo = tituloLimpio;
    }

    const descripcionLimpia = descripcion.trim();
    if (descripcionLimpia && descripcionLimpia !== (actividad.descripcion ?? "")) {
      cambios.descripcion = descripcionLimpia;
    }

    const precioLimpio = precio.trim();
    if (precioLimpio) {
      const precioNumero = Number(precioLimpio);
      if (Number.isFinite(precioNumero) && precioNumero !== actividad.precioReferencia) {
        cambios.precioReferencia = precioNumero;
      }
    }

    if (mostrarPrecio !== Boolean(actividad.mostrarPrecio)) {
      cambios.mostrarPrecio = mostrarPrecio;
    }

    const whatsappLimpio = whatsapp.trim();
    if (whatsappLimpio && whatsappLimpio !== (actividad.whatsapp ?? "")) {
      cambios.whatsappContacto = whatsappLimpio;
    }

    const instagramLimpio = instagram.trim();
    if (instagramLimpio && instagramLimpio !== (actividad.instagram ?? "")) {
      cambios.instagramContacto = instagramLimpio;
    }

    const emailLimpio = email.trim();
    if (emailLimpio && emailLimpio !== (actividad.email ?? "")) {
      cambios.emailContacto = emailLimpio;
    }

    if (nivel && nivel !== (actividad.nivel ?? "")) {
      cambios.nivel = nivel;
    }

    if (modalidad && modalidad !== (actividad.modalidad ?? "")) {
      cambios.modalidad = modalidad;
    }

    return cambios;
  }

  async function manejarEnvio(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault();

    if (enviando || !accessToken || !actividad) {
      return;
    }

    const cambios = construirCambiosPropuestos();

    if (Object.keys(cambios).length === 0) {
      setErrorEnvio(
        "No cambiaste ningún campo: modificá al menos uno antes de enviar."
      );
      return;
    }

    setEnviando(true);
    setErrorEnvio(null);

    try {
      const detalle = await crearSolicitudCambio(
        actividad.id,
        cambios,
        accessToken
      );

      setSolicitudCreada(detalle);
    } catch (error: unknown) {
      if (error instanceof PublicadorApiError) {
        if (error.status === 401) {
          cerrarSesion();
          router.replace(
            `/login?returnTo=${encodeURIComponent("/publicador/actividades")}`
          );
          return;
        }

        if (error.status === 409) {
          setErrorEnvio(
            "Esta actividad ya tiene una solicitud de cambio abierta. Esperá a que el equipo la resuelva antes de enviar otra."
          );
          return;
        }

        setErrorEnvio(error.message);
        return;
      }

      setErrorEnvio("No pudimos enviar la solicitud. Probá nuevamente.");
    } finally {
      setEnviando(false);
    }
  }

  if (solicitudCreada) {
    return (
      <main className="min-h-screen bg-gradient-to-br from-[#F8FAFC] via-white to-[#E8F6FB] px-4 py-8 text-[var(--color-text)] sm:py-12">
        <section className="mx-auto w-full max-w-3xl">
          <SurfaceCard className="p-6 sm:p-8">
            <SectionHeader
              eyebrow="Solicitud enviada"
              title="Tus cambios quedaron en revisión"
              description="El equipo los va a revisar. La versión pública de la actividad no cambia hasta que se aprueben."
            />

            <div className="mt-6 rounded-[18px] border border-[#DDEAF3] bg-[#F8FAFC] p-4">
              <p className="text-xs font-extrabold uppercase tracking-[0.14em] text-[var(--color-secondary)]">
                Cambios propuestos
              </p>
              <ul className="mt-3 space-y-2">
                {solicitudCreada.cambios.map((cambio) => (
                  <li key={cambio.campo} className="text-sm">
                    <span className="font-bold text-[var(--color-primary)]">
                      {ETIQUETAS_CAMPOS[cambio.campo] ?? cambio.campo}:
                    </span>{" "}
                    <span className="text-[var(--color-muted)] line-through">
                      {cambio.valorActual || "Sin valor"}
                    </span>{" "}
                    <span aria-hidden="true">→</span>{" "}
                    <span className="font-bold text-[#167A4A]">
                      {cambio.valorPropuesto}
                    </span>
                  </li>
                ))}
              </ul>
            </div>

            <div className="mt-6 grid gap-3 sm:grid-cols-2">
              <AppLinkButton href="/publicador/solicitudes-cambio" fullWidth>
                Ver mis solicitudes de cambio
              </AppLinkButton>
              <AppLinkButton
                href={`/publicador/actividades/${actividadId}`}
                variant="secondary"
                fullWidth
              >
                Volver a la actividad
              </AppLinkButton>
            </div>
          </SurfaceCard>
        </section>
      </main>
    );
  }

  return (
    <main className="min-h-screen bg-gradient-to-br from-[#F8FAFC] via-white to-[#E8F6FB] px-4 py-8 text-[var(--color-text)] sm:py-12">
      <section className="mx-auto w-full max-w-6xl">
        <PublicadorPageHeader
          title="Solicitar cambios"
          description="Proponé los cambios que necesites: el equipo los revisa y, si están bien, se publican."
          action={
            <AppLinkButton
              href={`/publicador/actividades/${actividadId}`}
              variant="secondary"
              fullWidth
            >
              Volver a la actividad
            </AppLinkButton>
          }
        />

        {cargando ? (
          <StatusMessage variant="info" role="status" className="mt-6">
            Cargando la actividad...
          </StatusMessage>
        ) : null}

        {errorCarga ? (
          <StatusMessage variant="error" role="alert" className="mt-6">
            {errorCarga}
          </StatusMessage>
        ) : null}

        {actividad ? (
          <SurfaceCard className="mt-6 p-6 sm:p-8">
            <SectionHeader
              eyebrow={actividad.titulo}
              title="Editá lo que quieras cambiar"
              description="Solo se envían los campos que modifiques respecto de la versión publicada."
            />

            <form className="mt-6 grid gap-5 lg:grid-cols-2" onSubmit={manejarEnvio}>
              <div className="lg:col-span-2">
                <label
                  htmlFor="cambio-titulo"
                  className="text-sm font-bold text-[var(--color-primary)]"
                >
                  Título
                </label>
                <input
                  id="cambio-titulo"
                  type="text"
                  maxLength={150}
                  value={titulo}
                  onChange={(evento) => setTitulo(evento.target.value)}
                  disabled={enviando}
                  className={CLASE_INPUT}
                />
              </div>

              <div className="lg:col-span-2">
                <label
                  htmlFor="cambio-descripcion"
                  className="text-sm font-bold text-[var(--color-primary)]"
                >
                  Descripción
                </label>
                <textarea
                  id="cambio-descripcion"
                  rows={4}
                  maxLength={5000}
                  value={descripcion}
                  onChange={(evento) => setDescripcion(evento.target.value)}
                  disabled={enviando}
                  className={`${CLASE_INPUT} min-h-28 py-3 leading-6`}
                />
              </div>

              <div>
                <label
                  htmlFor="cambio-precio"
                  className="text-sm font-bold text-[var(--color-primary)]"
                >
                  Precio de referencia
                </label>
                <input
                  id="cambio-precio"
                  type="number"
                  min={0}
                  step="0.01"
                  value={precio}
                  onChange={(evento) => setPrecio(evento.target.value)}
                  disabled={enviando}
                  className={CLASE_INPUT}
                />
              </div>

              <div className="flex items-end pb-3">
                <label className="flex min-h-12 items-center gap-3 text-sm font-bold text-[var(--color-primary)]">
                  <input
                    type="checkbox"
                    checked={mostrarPrecio}
                    onChange={(evento) => setMostrarPrecio(evento.target.checked)}
                    disabled={enviando}
                    className="h-5 w-5 rounded border-[#BFDDEA] accent-[#2EB872]"
                  />
                  Mostrar el precio públicamente
                </label>
              </div>

              <div>
                <label
                  htmlFor="cambio-nivel"
                  className="text-sm font-bold text-[var(--color-primary)]"
                >
                  Nivel
                </label>
                <select
                  id="cambio-nivel"
                  value={nivel}
                  onChange={(evento) => setNivel(evento.target.value)}
                  disabled={enviando}
                  className={CLASE_INPUT}
                >
                  <option value="">Sin cambio</option>
                  {NIVELES.map((opcion) => (
                    <option key={opcion} value={opcion}>
                      {opcion.charAt(0) + opcion.slice(1).toLowerCase()}
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label
                  htmlFor="cambio-modalidad"
                  className="text-sm font-bold text-[var(--color-primary)]"
                >
                  Modalidad
                </label>
                <select
                  id="cambio-modalidad"
                  value={modalidad}
                  onChange={(evento) => setModalidad(evento.target.value)}
                  disabled={enviando}
                  className={CLASE_INPUT}
                >
                  <option value="">Sin cambio</option>
                  {MODALIDADES.map((opcion) => (
                    <option key={opcion} value={opcion}>
                      {opcion.charAt(0) + opcion.slice(1).toLowerCase()}
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label
                  htmlFor="cambio-whatsapp"
                  className="text-sm font-bold text-[var(--color-primary)]"
                >
                  WhatsApp
                </label>
                <input
                  id="cambio-whatsapp"
                  type="text"
                  maxLength={30}
                  value={whatsapp}
                  onChange={(evento) => setWhatsapp(evento.target.value)}
                  disabled={enviando}
                  className={CLASE_INPUT}
                />
              </div>

              <div>
                <label
                  htmlFor="cambio-instagram"
                  className="text-sm font-bold text-[var(--color-primary)]"
                >
                  Instagram
                </label>
                <input
                  id="cambio-instagram"
                  type="text"
                  maxLength={150}
                  value={instagram}
                  onChange={(evento) => setInstagram(evento.target.value)}
                  disabled={enviando}
                  className={CLASE_INPUT}
                />
              </div>

              <div className="lg:col-span-2">
                <label
                  htmlFor="cambio-email"
                  className="text-sm font-bold text-[var(--color-primary)]"
                >
                  Email de contacto
                </label>
                <input
                  id="cambio-email"
                  type="email"
                  maxLength={150}
                  value={email}
                  onChange={(evento) => setEmail(evento.target.value)}
                  disabled={enviando}
                  className={CLASE_INPUT}
                />
              </div>

              {errorEnvio ? (
                <div className="lg:col-span-2">
                  <StatusMessage variant="error" role="alert">
                    {errorEnvio}
                  </StatusMessage>
                </div>
              ) : null}

              <div className="lg:col-span-2">
                <AppButton type="submit" disabled={enviando} fullWidth>
                  {enviando ? "Enviando..." : "Enviar solicitud de cambios"}
                </AppButton>
              </div>
            </form>
          </SurfaceCard>
        ) : null}
      </section>
    </main>
  );
}
