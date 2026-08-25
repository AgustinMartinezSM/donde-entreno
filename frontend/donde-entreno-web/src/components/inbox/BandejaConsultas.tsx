"use client";

import Image from "next/image";
import Link from "next/link";
import { useCallback, useEffect, useRef, useState } from "react";
import type { FormEvent } from "react";

import { useAuthSession } from "../auth/AuthSessionProvider";
import { BotonReportar } from "../social/BotonReportar";
import { AppButton } from "../ui/AppButton";
import { StatusMessage } from "../ui/StatusMessage";
import { SurfaceCard } from "../ui/SurfaceCard";
import { construirUrlImagenBackend } from "../../lib/backendUrl";
import { formatearFechaRelativa } from "../../lib/formatoFecha";
import {
  InboxApiError,
  MAX_TEXTO_MENSAJE,
  cerrarConsulta,
  consultar as enviarConsulta,
  obtenerBandejaPublicador,
  obtenerBandejaUsuario,
  obtenerHiloPublicador,
  obtenerHiloUsuario,
  responderConsulta,
  type Conversacion,
} from "../../services/inboxService";

/*
  La bandeja de consultas, para los dos lados (el publicador ve lo
  mismo con otros textos y sin poder cerrar ni iniciar).

  SIN REALTIME, a propósito: Render free tier hace spin-down por
  inactividad, así que un WebSocket sería una promesa que se incumple
  sola. El hilo abierto refresca cada 30 s y **solo con la pestaña
  visible**; fuera del hilo avisa la campanita que ya existe. Nada de
  polling global.
*/

const INTERVALO_POLLING_MS = 30_000;

export function BandejaConsultas({ lado }: { lado: "usuario" | "publicador" }) {
  const { accessToken } = useAuthSession();
  const esPublicador = lado === "publicador";

  const [conversaciones, setConversaciones] = useState<Conversacion[]>([]);
  const [abierta, setAbierta] = useState<Conversacion | null>(null);
  const [texto, setTexto] = useState("");
  const [cargando, setCargando] = useState(true);
  const [enviando, setEnviando] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [errorEnvio, setErrorEnvio] = useState<string | null>(null);

  const cargarBandeja = useCallback(async () => {
    if (!accessToken) {
      return;
    }

    try {
      const bandeja = esPublicador
        ? await obtenerBandejaPublicador(accessToken)
        : await obtenerBandejaUsuario(accessToken);

      setConversaciones(bandeja);
      setError(null);
    } catch (errorCarga: unknown) {
      setError(
        errorCarga instanceof InboxApiError
          ? errorCarga.message
          : "No pudimos cargar tus consultas. Probá de nuevo en unos minutos."
      );
    } finally {
      setCargando(false);
    }
  }, [accessToken, esPublicador]);

  const abrirHilo = useCallback(
    async (conversacionId: number) => {
      if (!accessToken) {
        return;
      }

      try {
        const hilo = esPublicador
          ? await obtenerHiloPublicador(accessToken, conversacionId)
          : await obtenerHiloUsuario(accessToken, conversacionId);

        setAbierta(hilo);
        /* Abrir marcó leído en el backend: la bandeja tiene que reflejarlo. */
        setConversaciones((actuales) =>
          actuales.map((cada) =>
            cada.id === conversacionId ? { ...cada, noLeidos: 0 } : cada
          )
        );
      } catch (errorHilo: unknown) {
        setErrorEnvio(
          errorHilo instanceof InboxApiError
            ? errorHilo.message
            : "No pudimos abrir la conversación."
        );
      }
    },
    [accessToken, esPublicador]
  );

  /*
    La carga inicial va DENTRO del efecto (mismo patrón que el resto
    del proyecto): llamar desde el efecto a una función async que hace
    setState dispara renders en cascada, y el lint lo marca.
  */
  useEffect(() => {
    let componenteActivo = true;

    if (!accessToken) {
      return () => {
        componenteActivo = false;
      };
    }

    async function cargarInicial() {
      try {
        const bandeja = esPublicador
          ? await obtenerBandejaPublicador(accessToken as string)
          : await obtenerBandejaUsuario(accessToken as string);

        if (!componenteActivo) {
          return;
        }

        setConversaciones(bandeja);
        setError(null);
      } catch (errorCarga: unknown) {
        if (!componenteActivo) {
          return;
        }

        setError(
          errorCarga instanceof InboxApiError
            ? errorCarga.message
            : "No pudimos cargar tus consultas. Probá de nuevo en unos minutos."
        );
      } finally {
        if (componenteActivo) {
          setCargando(false);
        }
      }
    }

    void cargarInicial();

    return () => {
      componenteActivo = false;
    };
  }, [accessToken, esPublicador]);

  /* Polling del hilo abierto, solo con la pestaña a la vista. */
  const idAbierta = abierta?.id;
  const refrescarRef = useRef(abrirHilo);

  /*
    La ref se actualiza en un efecto y no en el render: tocarla durante
    el render rompe con el modo concurrente (y el lint lo marca).
  */
  useEffect(() => {
    refrescarRef.current = abrirHilo;
  }, [abrirHilo]);

  useEffect(() => {
    if (!idAbierta) {
      return;
    }

    const temporizador = setInterval(() => {
      if (document.visibilityState === "visible") {
        void refrescarRef.current(idAbierta);
      }
    }, INTERVALO_POLLING_MS);

    return () => clearInterval(temporizador);
  }, [idAbierta]);

  async function manejarEnvio(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault();

    if (!accessToken || enviando || !texto.trim()) {
      return;
    }

    setEnviando(true);
    setErrorEnvio(null);

    try {
      if (esPublicador) {
        if (!abierta) {
          return;
        }
        await responderConsulta(accessToken, abierta.id, texto);
      } else {
        if (!abierta?.perfilPublicadorId) {
          return;
        }
        await enviarConsulta(accessToken, {
          perfilPublicadorId: abierta.perfilPublicadorId,
          actividadId: abierta.actividadId,
          texto,
        });
      }

      setTexto("");
      if (abierta) {
        await abrirHilo(abierta.id);
      }
      await cargarBandeja();
    } catch (errorEnviar: unknown) {
      setErrorEnvio(
        errorEnviar instanceof InboxApiError
          ? errorEnviar.message
          : "No pudimos enviar el mensaje. Probá de nuevo."
      );
    } finally {
      setEnviando(false);
    }
  }

  async function manejarCerrar() {
    if (!accessToken || !abierta) {
      return;
    }

    try {
      await cerrarConsulta(accessToken, abierta.id);
      await abrirHilo(abierta.id);
      await cargarBandeja();
    } catch {
      setErrorEnvio("No pudimos cerrar la consulta.");
    }
  }

  if (error) {
    return (
      <StatusMessage variant="error" role="alert" className="mt-4">
        {error}
      </StatusMessage>
    );
  }

  if (cargando) {
    return (
      <StatusMessage variant="info" role="status" className="mt-4">
        Cargando tus consultas...
      </StatusMessage>
    );
  }

  if (conversaciones.length === 0) {
    return (
      <SurfaceCard className="mt-4 p-6">
        <p className="text-sm text-[var(--color-muted)]">
          {esPublicador
            ? "Todavía no te consultaron por acá. Cuando alguien lo haga, te avisamos con la campanita."
            : "Todavía no consultaste a nadie. Desde cualquier actividad podés preguntar sin dar tu teléfono."}
        </p>
      </SurfaceCard>
    );
  }

  return (
    <div className="mt-4 grid gap-4 lg:grid-cols-[20rem_1fr] lg:items-start">
      {/* La lista */}
      <ul className="grid gap-2">
        {conversaciones.map((conversacion) => {
          const activa = abierta?.id === conversacion.id;
          const logo = construirUrlImagenBackend(conversacion.contraparteLogoUrl);

          return (
            <li key={conversacion.id}>
              <button
                type="button"
                onClick={() => void abrirHilo(conversacion.id)}
                aria-current={activa ? "true" : undefined}
                className={`flex w-full items-start gap-3 rounded-[16px] border p-3 text-left transition ${
                  activa
                    ? "border-[var(--color-primary)] bg-[var(--color-surface)]"
                    : "border-[var(--color-border-soft)] bg-[var(--color-surface)] hover:border-[var(--color-border-accent)]"
                }`}
              >
                {logo ? (
                  <span className="relative h-10 w-10 shrink-0 overflow-hidden rounded-full">
                    <Image src={logo} alt="" fill sizes="40px" className="object-cover" />
                  </span>
                ) : (
                  <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-[var(--color-brand)] text-sm font-extrabold text-white">
                    {(conversacion.contraparteNombre ?? "?").charAt(0).toUpperCase()}
                  </span>
                )}

                <span className="min-w-0 flex-1">
                  <span className="flex items-center justify-between gap-2">
                    <span className="truncate text-sm font-extrabold text-[var(--color-primary)]">
                      {conversacion.contraparteNombre}
                    </span>
                    {conversacion.noLeidos && conversacion.noLeidos > 0 ? (
                      <span className="rounded-full bg-[var(--color-brand)] px-2 py-0.5 text-xs font-extrabold text-white">
                        {conversacion.noLeidos}
                      </span>
                    ) : null}
                  </span>

                  {conversacion.actividadTitulo ? (
                    <span className="mt-0.5 block truncate text-xs text-[var(--color-muted)]">
                      Sobre {conversacion.actividadTitulo}
                    </span>
                  ) : null}

                  <span className="mt-1 block truncate text-xs text-[var(--color-muted)]">
                    {conversacion.ultimoMensajeTexto}
                  </span>
                </span>
              </button>
            </li>
          );
        })}
      </ul>

      {/* El hilo */}
      {abierta ? (
        <SurfaceCard as="section" className="p-5">
          <div className="flex flex-wrap items-center justify-between gap-2 border-b border-[var(--color-border-soft)] pb-3">
            <div className="min-w-0">
              <p className="text-sm font-extrabold text-[var(--color-primary)]">
                {abierta.contraparteNombre}
              </p>
              {abierta.actividadSlug && abierta.actividadTitulo ? (
                <Link
                  href={`/actividades/${abierta.actividadSlug}`}
                  className="text-xs text-[var(--color-muted)] underline underline-offset-2"
                >
                  Sobre {abierta.actividadTitulo}
                </Link>
              ) : null}
            </div>

            {!esPublicador && abierta.estado === "ABIERTA" ? (
              <AppButton
                type="button"
                variant="secondary"
                size="sm"
                onClick={() => void manejarCerrar()}
              >
                Cerrar consulta
              </AppButton>
            ) : null}
          </div>

          <ul className="mt-4 grid gap-3">
            {(abierta.mensajes ?? []).map((mensaje) => (
              <li
                key={mensaje.id}
                className={mensaje.esPropio ? "flex justify-end" : "flex justify-start"}
              >
                <div
                  className={`max-w-[85%] rounded-[16px] px-4 py-2 ${
                    mensaje.esPropio
                      ? "bg-[var(--color-brand)] text-white"
                      : "bg-[var(--color-surface-soft)] text-[var(--color-text)]"
                  }`}
                >
                  {mensaje.oculto ? (
                    /* El hueco queda: se moderó, no se borró la historia. */
                    <p className="text-sm italic opacity-80">
                      Este mensaje fue ocultado por moderación.
                    </p>
                  ) : (
                    <p className="whitespace-pre-line text-sm leading-6">
                      {mensaje.texto}
                    </p>
                  )}

                  <div className="mt-1 flex items-center justify-between gap-3">
                    <span
                      className={`text-[11px] ${
                        mensaje.esPropio ? "text-white/80" : "text-[var(--color-muted)]"
                      }`}
                    >
                      {mensaje.createdAt
                        ? formatearFechaRelativa(mensaje.createdAt)
                        : null}
                    </span>

                    {/* Solo lo del otro se reporta: lo propio se borra, no se denuncia. */}
                    {!mensaje.esPropio && !mensaje.oculto ? (
                      <BotonReportar
                        tipoObjeto="MENSAJE"
                        objetoId={mensaje.id}
                        etiquetaObjeto="este mensaje"
                        compacto
                      />
                    ) : null}
                  </div>
                </div>
              </li>
            ))}
          </ul>

          {abierta.estado === "CERRADA_POR_USUARIO" ? (
            <StatusMessage variant="info" className="mt-4">
              {esPublicador
                ? "La persona cerró esta consulta. No podés responder."
                : "Cerraste esta consulta. Si volvés a escribir, se reabre."}
            </StatusMessage>
          ) : null}

          {errorEnvio ? (
            <StatusMessage variant="error" role="alert" className="mt-4">
              {errorEnvio}
            </StatusMessage>
          ) : null}

          {esPublicador && abierta.estado === "CERRADA_POR_USUARIO" ? null : (
            <form className="mt-4" onSubmit={(evento) => void manejarEnvio(evento)}>
              <label htmlFor="texto-mensaje" className="sr-only">
                Escribí tu mensaje
              </label>
              <textarea
                id="texto-mensaje"
                value={texto}
                onChange={(evento) => setTexto(evento.target.value)}
                rows={3}
                maxLength={MAX_TEXTO_MENSAJE}
                placeholder={
                  esPublicador ? "Escribí tu respuesta..." : "Escribí tu consulta..."
                }
                className="w-full rounded-[16px] border border-[var(--color-border-accent)] bg-[var(--color-surface)] p-3 text-sm leading-6 text-[var(--color-text)] outline-none transition focus:border-[var(--color-primary)]"
              />

              <div className="mt-2">
                <AppButton type="submit" disabled={enviando || !texto.trim()}>
                  {enviando ? "Enviando..." : "Enviar"}
                </AppButton>
              </div>
            </form>
          )}
        </SurfaceCard>
      ) : (
        <SurfaceCard as="section" className="p-6">
          <p className="text-sm text-[var(--color-muted)]">
            Elegí una conversación para leerla.
          </p>
        </SurfaceCard>
      )}
    </div>
  );
}
