"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import type { FormEvent } from "react";

import { useAuthSession } from "../auth/AuthSessionProvider";
import { AppButton } from "../ui/AppButton";
import { SectionHeader } from "../ui/SectionHeader";
import { StatusMessage } from "../ui/StatusMessage";
import { SurfaceCard } from "../ui/SurfaceCard";
import { PublicadorPageHeader } from "./PublicadorPageHeader";
import { API_BASE_URL } from "../../lib/apiConfig";
import { formatearFechaEvento } from "../../lib/formatoFecha";
import {
  EventosApiError,
  MAX_DESCRIPCION_EVENTO,
  MAX_TITULO_EVENTO,
  cancelarEvento,
  eliminarEvento,
  obtenerMisEventos,
  publicarEvento,
  type Evento,
} from "../../services/eventosService";
import {
  listarActividadesPublicador,
  listarMisUbicaciones,
} from "../../services/publicadorService";
import type {
  ActividadPublicadorResumen,
  UbicacionPublicador,
} from "../../types/publicador";

type DeporteOpcion = { id: number; nombre: string };

/*
  La agenda del publicador (Fase 9): crear un evento con fecha, ver lo
  que viene y cancelarlo o borrarlo.

  Publica DIRECTO, sin cola de moderación, porque un evento CADUCA: un
  torneo del sábado esperando aprobación hasta el lunes es un evento
  perdido, y la función entera deja de usarse.
*/
export function AgendaDeEventos() {
  const { accessToken } = useAuthSession();

  const [eventos, setEventos] = useState<Evento[]>([]);
  const [ubicaciones, setUbicaciones] = useState<UbicacionPublicador[]>([]);
  const [actividades, setActividades] = useState<ActividadPublicadorResumen[]>([]);
  const [deportes, setDeportes] = useState<DeporteOpcion[]>([]);

  const [titulo, setTitulo] = useState("");
  const [descripcion, setDescripcion] = useState("");
  const [fecha, setFecha] = useState("");
  const [hora, setHora] = useState("");
  const [actividadId, setActividadId] = useState("");
  const [ubicacionId, setUbicacionId] = useState("");
  const [deporteId, setDeporteId] = useState("");
  const [cupo, setCupo] = useState("");
  const [esGratis, setEsGratis] = useState(false);
  const [precio, setPrecio] = useState("");

  const [cargando, setCargando] = useState(true);
  const [publicando, setPublicando] = useState(false);
  const [procesando, setProcesando] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [errorFormulario, setErrorFormulario] = useState<string | null>(null);
  const [publicado, setPublicado] = useState(false);

  useEffect(() => {
    let componenteActivo = true;

    if (!accessToken) {
      return () => {
        componenteActivo = false;
      };
    }

    async function cargar() {
      try {
        const [mios, sedes, paginaActividades] = await Promise.all([
          obtenerMisEventos(accessToken as string),
          listarMisUbicaciones(accessToken as string).catch(
            () => [] as UbicacionPublicador[]
          ),
          listarActividadesPublicador(
            { page: 0, size: 50, orden: "recientes" },
            accessToken as string
          ).catch(() => null),
        ]);

        if (!componenteActivo) {
          return;
        }

        setEventos(mios);
        setUbicaciones(sedes);
        setActividades(paginaActividades?.contenido ?? []);
        setError(null);

        /* El catálogo de deportes es público: sin token. */
        try {
          const respuesta = await fetch(`${API_BASE_URL}/api/deportes`, {
            cache: "no-store",
          });
          const data: unknown = await respuesta.json();

          if (componenteActivo && Array.isArray(data)) {
            setDeportes(
              data
                .filter(
                  (item): item is { id: number; nombre: string } =>
                    typeof item === "object" &&
                    item !== null &&
                    typeof (item as { id?: unknown }).id === "number" &&
                    typeof (item as { nombre?: unknown }).nombre === "string"
                )
                .map((item) => ({ id: item.id, nombre: item.nombre }))
            );
          }
        } catch {
          /* Sin lista de deportes: se elige por actividad. */
        }
      } catch (errorCarga: unknown) {
        if (!componenteActivo) {
          return;
        }

        setError(
          errorCarga instanceof EventosApiError
            ? errorCarga.message
            : "No pudimos cargar tu agenda. Probá de nuevo en unos minutos."
        );
      } finally {
        if (componenteActivo) {
          setCargando(false);
        }
      }
    }

    void cargar();

    return () => {
      componenteActivo = false;
    };
  }, [accessToken]);

  const actividadElegida = actividades.find(
    (actividad) => String(actividad.id) === actividadId
  );

  async function manejarPublicar(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault();

    if (!accessToken || publicando) {
      return;
    }

    if (!titulo.trim() || !descripcion.trim() || !fecha || !hora) {
      setErrorFormulario("Completá título, descripción, día y hora.");
      return;
    }

    setPublicando(true);
    setErrorFormulario(null);
    setPublicado(false);

    try {
      /*
        `new Date(...).toISOString()` convertiría a UTC y el backend
        recibiría otra hora escrita. Con el offset del dispositivo, la
        hora que el publicador tipeó es la que se guarda.
      */
      const local = new Date(`${fecha}T${hora}`);
      const offsetMinutos = -local.getTimezoneOffset();
      const signo = offsetMinutos >= 0 ? "+" : "-";
      const absoluto = Math.abs(offsetMinutos);
      const offset = `${signo}${String(Math.floor(absoluto / 60)).padStart(2, "0")}:${String(
        absoluto % 60
      ).padStart(2, "0")}`;

      const nuevo = await publicarEvento(accessToken, {
        titulo,
        descripcion,
        iniciaAt: `${fecha}T${hora}:00${offset}`,
        actividadId: actividadId ? Number(actividadId) : null,
        ubicacionId: ubicacionId ? Number(ubicacionId) : null,
        deporteId: deporteId ? Number(deporteId) : null,
        cupo: cupo ? Number(cupo) : null,
        esGratis,
        precioReferencia: !esGratis && precio ? Number(precio) : null,
      });

      setEventos((actuales) => [nuevo, ...actuales]);
      setTitulo("");
      setDescripcion("");
      setFecha("");
      setHora("");
      setCupo("");
      setPrecio("");
      setPublicado(true);
    } catch (errorPublicar: unknown) {
      setErrorFormulario(
        errorPublicar instanceof EventosApiError
          ? errorPublicar.message
          : "No pudimos publicar el evento. Probá de nuevo."
      );
    } finally {
      setPublicando(false);
    }
  }

  async function manejarCancelar(evento: Evento) {
    if (!accessToken || procesando !== null) {
      return;
    }

    setProcesando(evento.id);
    setErrorFormulario(null);

    try {
      await cancelarEvento(accessToken, evento.id);
      setEventos((actuales) =>
        actuales.map((cada) =>
          cada.id === evento.id ? { ...cada, estado: "CANCELADO" } : cada
        )
      );
    } catch {
      setErrorFormulario("No pudimos cancelar el evento. Probá de nuevo.");
    } finally {
      setProcesando(null);
    }
  }

  async function manejarBorrar(evento: Evento) {
    if (!accessToken || procesando !== null) {
      return;
    }

    setProcesando(evento.id);
    setErrorFormulario(null);

    try {
      await eliminarEvento(accessToken, evento.id);
      setEventos((actuales) => actuales.filter((cada) => cada.id !== evento.id));
    } catch {
      setErrorFormulario("No pudimos borrar el evento. Probá de nuevo.");
    } finally {
      setProcesando(null);
    }
  }

  return (
    <main className="min-h-screen text-[var(--color-text)]">
      <section className="mx-auto w-full max-w-4xl px-4 py-6">
        <PublicadorPageHeader
          title="Eventos"
          description="Un torneo, una clase abierta, un seminario: lo que pasa una vez y tiene fecha. Se publica al instante y va al feed de quienes te siguen."
        />

        <div className="mt-6">
          <SectionHeader
            eyebrow="Nuevo"
            title="Crear un evento"
            description="Lo que se repite todas las semanas no va acá: eso son los horarios de tu actividad."
          />
        </div>

        <SurfaceCard className="mt-4 p-5 sm:p-6">
          <form onSubmit={(evento) => void manejarPublicar(evento)}>
            <label
              htmlFor="titulo-evento"
              className="text-sm font-bold text-[var(--color-primary)]"
            >
              Título
            </label>
            <input
              id="titulo-evento"
              value={titulo}
              onChange={(evento) => setTitulo(evento.target.value)}
              maxLength={MAX_TITULO_EVENTO}
              placeholder="Torneo de verano"
              className="mt-2 w-full rounded-[14px] border border-[var(--color-border-accent)] bg-[var(--color-surface)] px-3 py-2 text-sm text-[var(--color-text)] outline-none transition focus:border-[var(--color-primary)]"
            />

            <label
              htmlFor="descripcion-evento"
              className="mt-4 block text-sm font-bold text-[var(--color-primary)]"
            >
              De qué se trata
            </label>
            <textarea
              id="descripcion-evento"
              value={descripcion}
              onChange={(evento) => setDescripcion(evento.target.value)}
              rows={4}
              maxLength={MAX_DESCRIPCION_EVENTO}
              placeholder="Categorías, qué llevar, cómo anotarse..."
              className="mt-2 w-full rounded-[16px] border border-[var(--color-border-accent)] bg-[var(--color-surface)] p-3 text-sm leading-6 text-[var(--color-text)] outline-none transition focus:border-[var(--color-primary)]"
            />

            <div className="mt-4 grid grid-cols-1 gap-4 sm:grid-cols-2">
              <div>
                <label
                  htmlFor="fecha-evento"
                  className="text-sm font-bold text-[var(--color-primary)]"
                >
                  Día
                </label>
                <input
                  id="fecha-evento"
                  type="date"
                  value={fecha}
                  onChange={(evento) => setFecha(evento.target.value)}
                  className="mt-2 w-full rounded-[14px] border border-[var(--color-border-accent)] bg-[var(--color-surface)] px-3 py-2 text-sm text-[var(--color-text)] outline-none transition focus:border-[var(--color-primary)]"
                />
              </div>

              <div>
                <label
                  htmlFor="hora-evento"
                  className="text-sm font-bold text-[var(--color-primary)]"
                >
                  Hora
                </label>
                <input
                  id="hora-evento"
                  type="time"
                  value={hora}
                  onChange={(evento) => setHora(evento.target.value)}
                  className="mt-2 w-full rounded-[14px] border border-[var(--color-border-accent)] bg-[var(--color-surface)] px-3 py-2 text-sm text-[var(--color-text)] outline-none transition focus:border-[var(--color-primary)]"
                />
              </div>
            </div>

            <div className="mt-4 grid grid-cols-1 gap-4 sm:grid-cols-2">
              <div>
                <label
                  htmlFor="actividad-evento"
                  className="text-sm font-bold text-[var(--color-primary)]"
                >
                  ¿Es de alguna actividad tuya? (opcional)
                </label>
                <select
                  id="actividad-evento"
                  value={actividadId}
                  onChange={(evento) => setActividadId(evento.target.value)}
                  className="mt-2 w-full rounded-[14px] border border-[var(--color-border-accent)] bg-[var(--color-surface)] px-3 py-2 text-sm text-[var(--color-text)] outline-none transition focus:border-[var(--color-primary)]"
                >
                  <option value="">No, es del club en general</option>
                  {actividades.map((actividad) => (
                    <option key={actividad.id} value={actividad.id}>
                      {actividad.titulo}
                    </option>
                  ))}
                </select>
                {actividadElegida ? (
                  <p className="mt-1 text-xs text-[var(--color-muted)]">
                    Toma el deporte y la sede de esa actividad.
                  </p>
                ) : null}
              </div>

              <div>
                <label
                  htmlFor="sede-evento"
                  className="text-sm font-bold text-[var(--color-primary)]"
                >
                  Sede
                </label>
                <select
                  id="sede-evento"
                  value={ubicacionId}
                  onChange={(evento) => setUbicacionId(evento.target.value)}
                  className="mt-2 w-full rounded-[14px] border border-[var(--color-border-accent)] bg-[var(--color-surface)] px-3 py-2 text-sm text-[var(--color-text)] outline-none transition focus:border-[var(--color-primary)]"
                >
                  <option value="">
                    {actividadElegida ? "La de la actividad" : "Elegí una sede"}
                  </option>
                  {ubicaciones.map((ubicacion) => (
                    <option key={ubicacion.id} value={ubicacion.id}>
                      {ubicacion.nombre}
                      {ubicacion.barrioNombre ? ` · ${ubicacion.barrioNombre}` : ""}
                    </option>
                  ))}
                </select>
              </div>
            </div>

            {!actividadElegida ? (
              <div className="mt-4">
                <label
                  htmlFor="deporte-evento"
                  className="text-sm font-bold text-[var(--color-primary)]"
                >
                  Deporte
                </label>
                <select
                  id="deporte-evento"
                  value={deporteId}
                  onChange={(evento) => setDeporteId(evento.target.value)}
                  className="mt-2 w-full rounded-[14px] border border-[var(--color-border-accent)] bg-[var(--color-surface)] px-3 py-2 text-sm text-[var(--color-text)] outline-none transition focus:border-[var(--color-primary)]"
                >
                  <option value="">Elegí un deporte</option>
                  {deportes.map((deporte) => (
                    <option key={deporte.id} value={deporte.id}>
                      {deporte.nombre}
                    </option>
                  ))}
                </select>
              </div>
            ) : null}

            <div className="mt-4 grid grid-cols-1 gap-4 sm:grid-cols-2">
              <div>
                <label
                  htmlFor="cupo-evento"
                  className="text-sm font-bold text-[var(--color-primary)]"
                >
                  Cupo (opcional)
                </label>
                <input
                  id="cupo-evento"
                  type="number"
                  min={1}
                  value={cupo}
                  onChange={(evento) => setCupo(evento.target.value)}
                  placeholder="20"
                  className="mt-2 w-full rounded-[14px] border border-[var(--color-border-accent)] bg-[var(--color-surface)] px-3 py-2 text-sm text-[var(--color-text)] outline-none transition focus:border-[var(--color-primary)]"
                />
                <p className="mt-1 text-xs text-[var(--color-muted)]">
                  Se muestra como dato. Las reservas siguen siendo por
                  WhatsApp.
                </p>
              </div>

              <div>
                <label className="flex min-h-11 items-center gap-2 text-sm font-bold text-[var(--color-primary)]">
                  <input
                    type="checkbox"
                    checked={esGratis}
                    onChange={(evento) => setEsGratis(evento.target.checked)}
                    className="h-4 w-4"
                  />
                  Es gratis
                </label>

                {!esGratis ? (
                  <input
                    aria-label="Precio de referencia"
                    type="number"
                    min={0}
                    value={precio}
                    onChange={(evento) => setPrecio(evento.target.value)}
                    placeholder="Precio de referencia"
                    className="mt-2 w-full rounded-[14px] border border-[var(--color-border-accent)] bg-[var(--color-surface)] px-3 py-2 text-sm text-[var(--color-text)] outline-none transition focus:border-[var(--color-primary)]"
                  />
                ) : null}
              </div>
            </div>

            {errorFormulario ? (
              <StatusMessage variant="error" role="alert" className="mt-4">
                {errorFormulario}
              </StatusMessage>
            ) : null}

            {publicado ? (
              <StatusMessage variant="success" role="status" className="mt-4">
                Publicado. Ya está en la agenda y en el feed de quienes te
                siguen.
              </StatusMessage>
            ) : null}

            <div className="mt-5">
              <AppButton type="submit" disabled={publicando}>
                {publicando ? "Publicando..." : "Publicar evento"}
              </AppButton>
            </div>
          </form>
        </SurfaceCard>

        <div className="mt-8">
          <SectionHeader
            eyebrow="Tu agenda"
            title="Lo que publicaste"
            description="Cancelar avisa que no va más y deja el link vivo; borrar lo saca de todos lados."
          />
        </div>

        {error ? (
          <StatusMessage variant="error" role="alert" className="mt-4">
            {error}
          </StatusMessage>
        ) : cargando ? (
          <StatusMessage variant="info" role="status" className="mt-4">
            Cargando tu agenda...
          </StatusMessage>
        ) : eventos.length === 0 ? (
          <SurfaceCard className="mt-4 p-6">
            <p className="text-sm text-[var(--color-muted)]">
              Todavía no organizaste ningún evento. Una clase abierta gratis es
              la forma más rápida de que alguien te conozca.
            </p>
          </SurfaceCard>
        ) : (
          <ul className="mt-4 space-y-3">
            {eventos.map((evento) => {
              const cuando = formatearFechaEvento(evento.iniciaAt);
              const cancelado = evento.estado === "CANCELADO";

              return (
                <li key={evento.id}>
                  <SurfaceCard as="article" className="p-5">
                    <div className="flex flex-wrap items-start justify-between gap-4">
                      <div className="min-w-0">
                        {cuando ? (
                          <p className="text-xs font-extrabold capitalize text-[var(--color-primary)]">
                            {cuando}
                          </p>
                        ) : null}
                        <p className="mt-1 text-sm font-extrabold text-[var(--color-text)]">
                          {evento.titulo}
                        </p>
                        <p className="mt-1 text-xs text-[var(--color-muted)]">
                          {evento.sedeNombre}
                          {evento.cantidadInteresados
                            ? ` · ${evento.cantidadInteresados} interesados`
                            : ""}
                        </p>

                        {cancelado ? (
                          <span className="mt-2 inline-flex rounded-full bg-[var(--color-error-soft)] px-3 py-1 text-xs font-extrabold text-[var(--color-error)]">
                            Cancelado
                          </span>
                        ) : null}
                      </div>

                      <div className="flex flex-wrap items-center gap-2">
                        <Link
                          href={`/eventos/${evento.slug}`}
                          className="inline-flex min-h-10 items-center rounded-[14px] border border-[var(--color-border-accent)] bg-[var(--color-surface)] px-3 text-sm font-bold text-[var(--color-primary)] transition hover:border-[var(--color-primary)]"
                        >
                          Ver
                        </Link>

                        {!cancelado ? (
                          <AppButton
                            type="button"
                            variant="secondary"
                            size="sm"
                            disabled={procesando === evento.id}
                            onClick={() => void manejarCancelar(evento)}
                          >
                            Cancelar
                          </AppButton>
                        ) : null}

                        <AppButton
                          type="button"
                          variant="danger"
                          size="sm"
                          disabled={procesando === evento.id}
                          onClick={() => void manejarBorrar(evento)}
                        >
                          Borrar
                        </AppButton>
                      </div>
                    </div>
                  </SurfaceCard>
                </li>
              );
            })}
          </ul>
        )}
      </section>
    </main>
  );
}
