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
import {
  obtenerBarriosPublicacion,
  obtenerCiudadesPublicacion,
  obtenerDeportesPublicacion,
} from "../../services/catalogosPublicacionService";
import type {
  BarrioPublicacionOpcion,
  DeportePublicacionOpcion,
} from "../../types/catalogosPublicacion";
import type {
  ActividadPublicadorDetalle,
  SolicitudCambioDetalle,
  SolicitudCambioHorarioRequest,
  SolicitudCambioRequest,
} from "../../types/publicador";

const CLASE_INPUT =
  "mt-2 min-h-12 w-full rounded-[18px] border border-[var(--color-border-accent)] bg-[var(--color-bg)] px-4 text-base text-[var(--color-text)] outline-none transition duration-200 ease-out hover:border-[var(--color-accent)] focus:border-[var(--color-accent)] focus:ring-4 focus:ring-[var(--color-border-soft)] disabled:cursor-not-allowed disabled:opacity-70";

const NIVELES = ["PRINCIPIANTE", "INTERMEDIO", "AVANZADO", "TODOS"] as const;
const MODALIDADES = ["PRESENCIAL", "ONLINE", "MIXTA"] as const;
const ENFOQUES = ["RECREATIVO", "COMPETITIVO", "MIXTO"] as const;
const DIAS_SEMANA = [
  "LUNES",
  "MARTES",
  "MIERCOLES",
  "JUEVES",
  "VIERNES",
  "SABADO",
  "DOMINGO",
] as const;

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
  deporte: "Deporte",
  edadMinima: "Edad mínima",
  edadMaxima: "Edad máxima",
  enfoque: "Enfoque",
  ubicacion: "Ubicación",
  horarios: "Horarios",
};

type HorarioEditable = {
  diaSemana: string;
  horaInicio: string;
  horaFin: string;
  observacion: string;
};

/* Los inputs type="time" esperan HH:MM; el backend puede mandar HH:MM:SS. */
function recortarHora(hora: string): string {
  return hora.length > 5 ? hora.slice(0, 5) : hora;
}

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
  /*
    Con un id inválido en la URL no hay nada que cargar: el estado nace
    resuelto en error para no dejar un "Cargando..." infinito.
  */
  const idActividadInvalido = !Number.isFinite(actividadId);
  const { accessToken, cerrarSesion } = useAuthSession();

  const [actividad, setActividad] = useState<ActividadPublicadorDetalle | null>(null);
  const [cargando, setCargando] = useState(!idActividadInvalido);
  const [errorCarga, setErrorCarga] = useState<string | null>(
    idActividadInvalido
      ? "No encontramos esta actividad entre tus publicaciones."
      : null
  );

  const [titulo, setTitulo] = useState("");
  const [descripcion, setDescripcion] = useState("");
  const [precio, setPrecio] = useState("");
  const [mostrarPrecio, setMostrarPrecio] = useState(false);
  const [whatsapp, setWhatsapp] = useState("");
  const [instagram, setInstagram] = useState("");
  const [email, setEmail] = useState("");
  const [nivel, setNivel] = useState("");
  const [modalidad, setModalidad] = useState("");

  /* Campos nuevos: deporte, edades, enfoque, ubicación y horarios. */
  const [deporteId, setDeporteId] = useState("");
  const [edadMinima, setEdadMinima] = useState("");
  const [edadMaxima, setEdadMaxima] = useState("");
  const [enfoque, setEnfoque] = useState("");
  const [cambiarUbicacion, setCambiarUbicacion] = useState(false);
  const [ubicacionNombre, setUbicacionNombre] = useState("");
  const [ubicacionDireccion, setUbicacionDireccion] = useState("");
  const [ubicacionReferencia, setUbicacionReferencia] = useState("");
  const [ubicacionBarrioId, setUbicacionBarrioId] = useState("");
  const [cambiarHorarios, setCambiarHorarios] = useState(false);
  const [horarios, setHorarios] = useState<HorarioEditable[]>([]);

  /*
    Catálogos para los selects. Si alguno falla, la sección que lo
    necesita se deshabilita con aviso; el resto del formulario sigue.
  */
  const [deportes, setDeportes] = useState<DeportePublicacionOpcion[]>([]);
  const [barrios, setBarrios] = useState<BarrioPublicacionOpcion[]>([]);
  const [errorCatalogos, setErrorCatalogos] = useState<string | null>(null);

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

    async function cargarCatalogos(detalle: ActividadPublicadorDetalle) {
      try {
        const [deportesObtenidos, ciudades] = await Promise.all([
          obtenerDeportesPublicacion(),
          obtenerCiudadesPublicacion(),
        ]);

        if (!componenteActivo) {
          return;
        }

        setDeportes(deportesObtenidos);

        const ciudadDeLaActividad = ciudades.find(
          (ciudad) =>
            (detalle.ciudadSlug !== null && ciudad.slug === detalle.ciudadSlug) ||
            (detalle.ciudadNombre !== null && ciudad.nombre === detalle.ciudadNombre)
        );

        if (!ciudadDeLaActividad) {
          setErrorCatalogos(
            "No pudimos identificar la ciudad de la actividad: la sección de ubicación queda deshabilitada."
          );
          return;
        }

        const barriosObtenidos = await obtenerBarriosPublicacion(
          ciudadDeLaActividad.id
        );

        if (!componenteActivo) {
          return;
        }

        setBarrios(barriosObtenidos);
        setErrorCatalogos(null);
      } catch {
        if (componenteActivo) {
          setErrorCatalogos(
            "No pudimos cargar los catálogos de deportes y barrios: esas secciones quedan deshabilitadas. Recargá la página para reintentar."
          );
        }
      }
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
        setEdadMinima(
          detalle.edadMinima !== null ? String(detalle.edadMinima) : ""
        );
        setEdadMaxima(
          detalle.edadMaxima !== null ? String(detalle.edadMaxima) : ""
        );
        setEnfoque(detalle.enfoque ?? "");
        setUbicacionNombre(detalle.nombreLugar ?? "");
        setUbicacionDireccion(detalle.direccion ?? "");
        setUbicacionReferencia(detalle.referenciaUbicacion ?? "");
        setHorarios(
          detalle.horarios.map((horario) => ({
            diaSemana: horario.diaSemana,
            horaInicio: recortarHora(horario.horaInicio),
            horaFin: recortarHora(horario.horaFin),
            observacion: horario.observacion ?? "",
          }))
        );
        setErrorCarga(null);

        /*
          Catálogos en segundo plano: deportes para el select, y los
          barrios de la MISMA ciudad de la actividad (regla del backend:
          la ubicación nueva no puede cambiar de ciudad).
        */
        void cargarCatalogos(detalle);
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

    if (deporteId) {
      const deporteElegido = deportes.find(
        (deporte) => String(deporte.id) === deporteId
      );
      if (deporteElegido && deporteElegido.slug !== actividad.deporteSlug) {
        cambios.deporteId = deporteElegido.id;
      }
    }

    const edadMinimaLimpia = edadMinima.trim();
    if (edadMinimaLimpia) {
      const numero = Number(edadMinimaLimpia);
      if (Number.isInteger(numero) && numero !== actividad.edadMinima) {
        cambios.edadMinima = numero;
      }
    }

    const edadMaximaLimpia = edadMaxima.trim();
    if (edadMaximaLimpia) {
      const numero = Number(edadMaximaLimpia);
      if (Number.isInteger(numero) && numero !== actividad.edadMaxima) {
        cambios.edadMaxima = numero;
      }
    }

    if (enfoque && enfoque !== (actividad.enfoque ?? "")) {
      cambios.enfoque = enfoque;
    }

    if (cambiarUbicacion) {
      const direccionLimpia = ubicacionDireccion.trim();
      if (direccionLimpia) {
        cambios.ubicacionDireccion = direccionLimpia;
      }
      const barrioNumero = Number(ubicacionBarrioId);
      if (ubicacionBarrioId && Number.isInteger(barrioNumero)) {
        cambios.ubicacionBarrioId = barrioNumero;
      }
      const nombreLimpio = ubicacionNombre.trim();
      if (nombreLimpio) {
        cambios.ubicacionNombre = nombreLimpio;
      }
      const referenciaLimpia = ubicacionReferencia.trim();
      if (referenciaLimpia) {
        cambios.ubicacionReferencia = referenciaLimpia;
      }
    }

    if (cambiarHorarios) {
      cambios.cambiaHorarios = true;
      cambios.horarios = horarios
        .filter(
          (horario) =>
            horario.diaSemana && horario.horaInicio && horario.horaFin
        )
        .map((horario): SolicitudCambioHorarioRequest => {
          const observacionLimpia = horario.observacion.trim();
          return {
            diaSemana: horario.diaSemana,
            horaInicio: horario.horaInicio,
            horaFin: horario.horaFin,
            ...(observacionLimpia ? { observacion: observacionLimpia } : {}),
          };
        });
    }

    return cambios;
  }

  /*
    Espejo en el cliente de las reglas del backend, para dar el error
    antes del viaje. El backend sigue siendo la autoridad.
  */
  function validarCambios(cambios: SolicitudCambioRequest): string | null {
    if (!actividad) {
      return null;
    }

    const minimaResultante = cambios.edadMinima ?? actividad.edadMinima;
    const maximaResultante = cambios.edadMaxima ?? actividad.edadMaxima;
    if (
      minimaResultante !== null &&
      maximaResultante !== null &&
      minimaResultante > maximaResultante
    ) {
      return "La edad mínima resultante no puede superar la edad máxima.";
    }

    if (cambiarUbicacion) {
      if (!cambios.ubicacionDireccion || !cambios.ubicacionBarrioId) {
        return "Para proponer una ubicación nueva completá la dirección y elegí el barrio.";
      }
    }

    if (cambiarHorarios) {
      if (!cambios.horarios || cambios.horarios.length === 0) {
        return "Marcaste que cambian los horarios: agregá al menos un horario completo.";
      }
      for (const horario of cambios.horarios) {
        if (horario.horaFin <= horario.horaInicio) {
          return "En los horarios, la hora de fin debe ser posterior a la de inicio.";
        }
      }
    }

    return null;
  }

  function actualizarHorario(
    indice: number,
    cambiosFila: Partial<HorarioEditable>
  ) {
    setHorarios((filas) =>
      filas.map((fila, posicion) =>
        posicion === indice ? { ...fila, ...cambiosFila } : fila
      )
    );
  }

  function agregarHorario() {
    setHorarios((filas) => [
      ...filas,
      { diaSemana: "LUNES", horaInicio: "", horaFin: "", observacion: "" },
    ]);
  }

  function quitarHorario(indice: number) {
    setHorarios((filas) => filas.filter((_, posicion) => posicion !== indice));
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

    const errorValidacion = validarCambios(cambios);
    if (errorValidacion) {
      setErrorEnvio(errorValidacion);
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
      <main className="min-h-screen px-4 py-8 text-[var(--color-text)] sm:py-12">
        <section className="mx-auto w-full max-w-3xl">
          <SurfaceCard className="p-6 sm:p-8">
            <SectionHeader
              eyebrow="Solicitud enviada"
              title="Tus cambios quedaron en revisión"
              description="El equipo los va a revisar. La versión pública de la actividad no cambia hasta que se aprueben."
            />

            <div className="mt-6 rounded-[18px] border border-[var(--color-border-soft)] bg-[var(--color-bg)] p-4">
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
                    <span className="font-bold text-[var(--color-success)]">
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
    <main className="min-h-screen px-4 py-8 text-[var(--color-text)] sm:py-12">
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
                    className="h-5 w-5 rounded border-[var(--color-border-accent)] accent-[var(--color-secondary)]"
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
                  htmlFor="cambio-deporte"
                  className="text-sm font-bold text-[var(--color-primary)]"
                >
                  Deporte
                </label>
                <select
                  id="cambio-deporte"
                  value={deporteId}
                  onChange={(evento) => setDeporteId(evento.target.value)}
                  disabled={enviando || deportes.length === 0}
                  className={CLASE_INPUT}
                >
                  <option value="">
                    {actividad.deporteNombre
                      ? `Sin cambio (actual: ${actividad.deporteNombre})`
                      : "Sin cambio"}
                  </option>
                  {deportes.map((deporte) => (
                    <option key={deporte.id} value={String(deporte.id)}>
                      {deporte.nombre}
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label
                  htmlFor="cambio-enfoque"
                  className="text-sm font-bold text-[var(--color-primary)]"
                >
                  Enfoque
                </label>
                <select
                  id="cambio-enfoque"
                  value={enfoque}
                  onChange={(evento) => setEnfoque(evento.target.value)}
                  disabled={enviando}
                  className={CLASE_INPUT}
                >
                  <option value="">Sin cambio</option>
                  {ENFOQUES.map((opcion) => (
                    <option key={opcion} value={opcion}>
                      {opcion.charAt(0) + opcion.slice(1).toLowerCase()}
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label
                  htmlFor="cambio-edad-minima"
                  className="text-sm font-bold text-[var(--color-primary)]"
                >
                  Edad mínima
                </label>
                <input
                  id="cambio-edad-minima"
                  type="number"
                  min={0}
                  max={120}
                  value={edadMinima}
                  onChange={(evento) => setEdadMinima(evento.target.value)}
                  disabled={enviando}
                  className={CLASE_INPUT}
                />
              </div>

              <div>
                <label
                  htmlFor="cambio-edad-maxima"
                  className="text-sm font-bold text-[var(--color-primary)]"
                >
                  Edad máxima
                </label>
                <input
                  id="cambio-edad-maxima"
                  type="number"
                  min={0}
                  max={120}
                  value={edadMaxima}
                  onChange={(evento) => setEdadMaxima(evento.target.value)}
                  disabled={enviando}
                  className={CLASE_INPUT}
                />
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

              {errorCatalogos ? (
                <div className="lg:col-span-2">
                  <StatusMessage variant="error" role="alert">
                    {errorCatalogos}
                  </StatusMessage>
                </div>
              ) : null}

              <fieldset className="rounded-[18px] border border-[var(--color-border-soft)] p-4 lg:col-span-2">
                <legend className="px-2">
                  <label className="flex items-center gap-3 text-sm font-bold text-[var(--color-primary)]">
                    <input
                      type="checkbox"
                      checked={cambiarUbicacion}
                      onChange={(evento) =>
                        setCambiarUbicacion(evento.target.checked)
                      }
                      disabled={enviando || barrios.length === 0}
                      className="h-5 w-5 rounded border-[var(--color-border-accent)] accent-[var(--color-secondary)]"
                    />
                    Proponer cambio de ubicación
                  </label>
                </legend>
                <p className="text-xs text-[var(--color-muted)]">
                  La ubicación nueva tiene que ser de {actividad.ciudadNombre ?? "la misma ciudad"}:
                  para mudar la actividad de ciudad, contactá al equipo.
                </p>

                {cambiarUbicacion ? (
                  <div className="mt-4 grid gap-5 lg:grid-cols-2">
                    <div>
                      <label
                        htmlFor="cambio-ubicacion-nombre"
                        className="text-sm font-bold text-[var(--color-primary)]"
                      >
                        Nombre del lugar (opcional)
                      </label>
                      <input
                        id="cambio-ubicacion-nombre"
                        type="text"
                        maxLength={150}
                        value={ubicacionNombre}
                        onChange={(evento) =>
                          setUbicacionNombre(evento.target.value)
                        }
                        disabled={enviando}
                        className={CLASE_INPUT}
                      />
                    </div>

                    <div>
                      <label
                        htmlFor="cambio-ubicacion-barrio"
                        className="text-sm font-bold text-[var(--color-primary)]"
                      >
                        Barrio
                      </label>
                      <select
                        id="cambio-ubicacion-barrio"
                        value={ubicacionBarrioId}
                        onChange={(evento) =>
                          setUbicacionBarrioId(evento.target.value)
                        }
                        disabled={enviando}
                        className={CLASE_INPUT}
                      >
                        <option value="">Elegí el barrio</option>
                        {barrios.map((barrio) => (
                          <option key={barrio.id} value={String(barrio.id)}>
                            {barrio.nombre}
                          </option>
                        ))}
                      </select>
                    </div>

                    <div>
                      <label
                        htmlFor="cambio-ubicacion-direccion"
                        className="text-sm font-bold text-[var(--color-primary)]"
                      >
                        Dirección
                      </label>
                      <input
                        id="cambio-ubicacion-direccion"
                        type="text"
                        maxLength={255}
                        value={ubicacionDireccion}
                        onChange={(evento) =>
                          setUbicacionDireccion(evento.target.value)
                        }
                        disabled={enviando}
                        className={CLASE_INPUT}
                      />
                    </div>

                    <div>
                      <label
                        htmlFor="cambio-ubicacion-referencia"
                        className="text-sm font-bold text-[var(--color-primary)]"
                      >
                        Referencia (opcional)
                      </label>
                      <input
                        id="cambio-ubicacion-referencia"
                        type="text"
                        maxLength={255}
                        value={ubicacionReferencia}
                        onChange={(evento) =>
                          setUbicacionReferencia(evento.target.value)
                        }
                        disabled={enviando}
                        className={CLASE_INPUT}
                      />
                    </div>
                  </div>
                ) : null}
              </fieldset>

              <fieldset className="rounded-[18px] border border-[var(--color-border-soft)] p-4 lg:col-span-2">
                <legend className="px-2">
                  <label className="flex items-center gap-3 text-sm font-bold text-[var(--color-primary)]">
                    <input
                      type="checkbox"
                      checked={cambiarHorarios}
                      onChange={(evento) =>
                        setCambiarHorarios(evento.target.checked)
                      }
                      disabled={enviando}
                      className="h-5 w-5 rounded border-[var(--color-border-accent)] accent-[var(--color-secondary)]"
                    />
                    Proponer cambio de horarios
                  </label>
                </legend>
                <p className="text-xs text-[var(--color-muted)]">
                  Los horarios que dejes acá reemplazan TODOS los actuales:
                  quedan solo los de la lista.
                </p>

                {cambiarHorarios ? (
                  <div className="mt-4 space-y-3">
                    {horarios.map((horario, indice) => (
                      <div
                        key={indice}
                        className="grid gap-3 rounded-[14px] border border-[var(--color-border-soft)] bg-[var(--color-bg)] p-3 sm:grid-cols-[1fr_auto_auto_1fr_auto] sm:items-end"
                      >
                        <div>
                          <label
                            htmlFor={`cambio-horario-dia-${indice}`}
                            className="text-xs font-bold text-[var(--color-primary)]"
                          >
                            Día
                          </label>
                          <select
                            id={`cambio-horario-dia-${indice}`}
                            value={horario.diaSemana}
                            onChange={(evento) =>
                              actualizarHorario(indice, {
                                diaSemana: evento.target.value,
                              })
                            }
                            disabled={enviando}
                            className={CLASE_INPUT}
                          >
                            {DIAS_SEMANA.map((dia) => (
                              <option key={dia} value={dia}>
                                {dia.charAt(0) + dia.slice(1).toLowerCase()}
                              </option>
                            ))}
                          </select>
                        </div>

                        <div>
                          <label
                            htmlFor={`cambio-horario-inicio-${indice}`}
                            className="text-xs font-bold text-[var(--color-primary)]"
                          >
                            Desde
                          </label>
                          <input
                            id={`cambio-horario-inicio-${indice}`}
                            type="time"
                            value={horario.horaInicio}
                            onChange={(evento) =>
                              actualizarHorario(indice, {
                                horaInicio: evento.target.value,
                              })
                            }
                            disabled={enviando}
                            className={CLASE_INPUT}
                          />
                        </div>

                        <div>
                          <label
                            htmlFor={`cambio-horario-fin-${indice}`}
                            className="text-xs font-bold text-[var(--color-primary)]"
                          >
                            Hasta
                          </label>
                          <input
                            id={`cambio-horario-fin-${indice}`}
                            type="time"
                            value={horario.horaFin}
                            onChange={(evento) =>
                              actualizarHorario(indice, {
                                horaFin: evento.target.value,
                              })
                            }
                            disabled={enviando}
                            className={CLASE_INPUT}
                          />
                        </div>

                        <div>
                          <label
                            htmlFor={`cambio-horario-obs-${indice}`}
                            className="text-xs font-bold text-[var(--color-primary)]"
                          >
                            Observación (opcional)
                          </label>
                          <input
                            id={`cambio-horario-obs-${indice}`}
                            type="text"
                            maxLength={255}
                            value={horario.observacion}
                            onChange={(evento) =>
                              actualizarHorario(indice, {
                                observacion: evento.target.value,
                              })
                            }
                            disabled={enviando}
                            className={CLASE_INPUT}
                          />
                        </div>

                        <AppButton
                          type="button"
                          variant="secondary"
                          onClick={() => quitarHorario(indice)}
                          disabled={enviando}
                        >
                          Quitar
                        </AppButton>
                      </div>
                    ))}

                    <AppButton
                      type="button"
                      variant="secondary"
                      onClick={agregarHorario}
                      disabled={enviando}
                    >
                      Agregar horario
                    </AppButton>
                  </div>
                ) : null}
              </fieldset>

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
