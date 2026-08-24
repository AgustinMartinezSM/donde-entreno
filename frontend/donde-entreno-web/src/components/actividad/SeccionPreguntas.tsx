"use client";

import { useEffect, useState } from "react";
import { usePathname, useRouter } from "next/navigation";
import type { FormEvent } from "react";

import { useAuthSession } from "../auth/AuthSessionProvider";
import { esRolPublicador } from "../../lib/authRedirects";
import {
  ConfianzaApiError,
  eliminarPregunta,
  enviarPregunta,
  obtenerPreguntas,
  responderPregunta,
  type PreguntaActividad,
} from "../../services/confianzaService";
import { formatearFechaRelativa } from "../../lib/formatoFecha";
import { BotonReportar } from "../social/BotonReportar";
import { AppButton } from "../ui/AppButton";
import { SectionHeader } from "../ui/SectionHeader";
import { StatusMessage } from "../ui/StatusMessage";
import { SurfaceCard } from "../ui/SurfaceCard";

/*
  Preguntas y respuestas del detalle (Fase 3, script 29), estilo
  MercadoLibre: la pregunta y su respuesta quedan públicas y ayudan a
  la siguiente persona. El botón "Responder" se ofrece a cualquier
  publicador logueado; el backend valida que sea el DUEÑO (404 si no).
*/
export function SeccionPreguntas({ actividadId }: { actividadId: number }) {
  const router = useRouter();
  const pathname = usePathname();
  const { status, usuario, accessToken } = useAuthSession();

  const [preguntas, setPreguntas] = useState<PreguntaActividad[] | null>(null);
  const [errorCarga, setErrorCarga] = useState(false);
  const [textoPregunta, setTextoPregunta] = useState("");
  const [enviando, setEnviando] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [respondiendo, setRespondiendo] = useState<number | null>(null);
  const [textoRespuesta, setTextoRespuesta] = useState("");

  const esPublicador = usuario ? esRolPublicador(usuario.rol) : false;

  useEffect(() => {
    let componenteActivo = true;

    obtenerPreguntas(actividadId, accessToken)
      .then((datos) => {
        if (componenteActivo) {
          setPreguntas(datos);
          setErrorCarga(false);
        }
      })
      .catch(() => {
        if (componenteActivo) {
          setErrorCarga(true);
        }
      });

    return () => {
      componenteActivo = false;
    };
  }, [actividadId, accessToken]);

  async function manejarPregunta(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault();

    if (status !== "authenticated" || !accessToken) {
      router.push(
        `/login?motivo=cuenta&returnTo=${encodeURIComponent(pathname ?? "/")}`
      );
      return;
    }

    if (enviando || !textoPregunta.trim()) {
      return;
    }

    setEnviando(true);
    setError(null);

    try {
      const creada = await enviarPregunta(accessToken, actividadId, textoPregunta);
      setPreguntas((actuales) => [creada, ...(actuales ?? [])]);
      setTextoPregunta("");
    } catch (excepcion: unknown) {
      setError(
        excepcion instanceof ConfianzaApiError
          ? excepcion.message
          : "No pudimos enviar tu pregunta. Probá nuevamente."
      );
    } finally {
      setEnviando(false);
    }
  }

  async function manejarRespuesta(preguntaId: number) {
    if (!accessToken || enviando || !textoRespuesta.trim()) {
      return;
    }

    setEnviando(true);
    setError(null);

    try {
      const actualizada = await responderPregunta(
        accessToken,
        preguntaId,
        textoRespuesta
      );
      setPreguntas((actuales) =>
        (actuales ?? []).map((cada) =>
          cada.id === actualizada.id ? actualizada : cada
        )
      );
      setRespondiendo(null);
      setTextoRespuesta("");
    } catch (excepcion: unknown) {
      setError(
        excepcion instanceof ConfianzaApiError && excepcion.status === 404
          ? "Solo quien publicó la actividad puede responder."
          : "No pudimos guardar la respuesta. Probá nuevamente."
      );
    } finally {
      setEnviando(false);
    }
  }

  async function manejarBorrar(preguntaId: number) {
    if (!accessToken || enviando) {
      return;
    }

    setEnviando(true);

    try {
      await eliminarPregunta(accessToken, preguntaId);
      setPreguntas((actuales) =>
        (actuales ?? []).filter((cada) => cada.id !== preguntaId)
      );
    } catch (excepcion: unknown) {
      setError(
        excepcion instanceof ConfianzaApiError
          ? excepcion.message
          : "No pudimos borrar la pregunta."
      );
    } finally {
      setEnviando(false);
    }
  }

  return (
    <SurfaceCard as="section" id="preguntas" className="scroll-mt-6 p-6 sm:p-8">
      <SectionHeader
        eyebrow="Consultas"
        title="Preguntas y respuestas"
        description="Preguntá en público: la respuesta del publicador queda para ayudar a la próxima persona."
      />

      <form onSubmit={manejarPregunta} className="mt-5 flex flex-col gap-2 sm:flex-row">
        <input
          type="text"
          value={textoPregunta}
          onChange={(evento) => setTextoPregunta(evento.target.value)}
          maxLength={500}
          placeholder="¿Hay clase de prueba? ¿Aceptan principiantes?"
          disabled={enviando}
          className="min-h-12 flex-1 rounded-[18px] border border-[var(--color-border-accent)] bg-[var(--color-bg)] px-4 text-sm text-[var(--color-text)] outline-none transition duration-200 ease-out focus:border-[var(--color-accent)] focus:ring-4 focus:ring-[var(--color-border-soft)]"
        />
        <AppButton type="submit" disabled={enviando || !textoPregunta.trim()}>
          Preguntar
        </AppButton>
      </form>

      {error ? (
        <StatusMessage variant="error" role="alert" className="mt-3">
          {error}
        </StatusMessage>
      ) : null}

      {errorCarga ? (
        <StatusMessage variant="error" role="alert" className="mt-5">
          No pudimos cargar las preguntas. Probá nuevamente.
        </StatusMessage>
      ) : preguntas === null ? (
        <p className="mt-5 text-sm text-[var(--color-muted)]">Cargando...</p>
      ) : preguntas.length === 0 ? (
        <p className="mt-5 text-sm text-[var(--color-muted)]">
          Todavía no hay preguntas. La tuya puede ayudar a más gente.
        </p>
      ) : (
        <ul className="mt-6 space-y-4">
          {preguntas.map((pregunta) => (
            <li
              key={pregunta.id}
              className="rounded-[18px] border border-[var(--color-border-soft)] bg-[var(--color-bg)] p-4"
            >
              <div className="flex flex-wrap items-start justify-between gap-2">
                <p className="min-w-0 flex-1 text-sm font-bold leading-6 text-[var(--color-primary)]">
                  {pregunta.pregunta}
                </p>
                {pregunta.createdAt ? (
                  <span className="shrink-0 text-xs text-[var(--color-muted)]">
                    {formatearFechaRelativa(pregunta.createdAt)}
                  </span>
                ) : null}
              </div>

              {pregunta.respuesta ? (
                <p className="mt-2 border-l-2 border-[var(--color-secondary)] pl-3 text-sm leading-6 text-[var(--color-muted)]">
                  {pregunta.respuesta}
                </p>
              ) : (
                <div className="mt-2 flex flex-wrap items-center gap-3">
                  <span className="text-xs font-bold text-[var(--color-muted)]">
                    Sin responder todavía
                  </span>

                  {esPublicador ? (
                    respondiendo === pregunta.id ? (
                      <div className="flex w-full flex-col gap-2 sm:flex-row">
                        <input
                          type="text"
                          value={textoRespuesta}
                          onChange={(evento) => setTextoRespuesta(evento.target.value)}
                          maxLength={1000}
                          placeholder="Tu respuesta pública"
                          disabled={enviando}
                          className="min-h-11 flex-1 rounded-[14px] border border-[var(--color-border-accent)] bg-[var(--color-surface)] px-3 text-sm text-[var(--color-text)] outline-none focus:border-[var(--color-accent)]"
                        />
                        <AppButton
                          type="button"
                          size="sm"
                          disabled={enviando || !textoRespuesta.trim()}
                          onClick={() => void manejarRespuesta(pregunta.id)}
                        >
                          Responder
                        </AppButton>
                      </div>
                    ) : (
                      <AppButton
                        type="button"
                        variant="secondary"
                        size="sm"
                        onClick={() => {
                          setRespondiendo(pregunta.id);
                          setTextoRespuesta("");
                        }}
                      >
                        Responder
                      </AppButton>
                    )
                  ) : null}

                  {pregunta.esPropia ? (
                    <button
                      type="button"
                      disabled={enviando}
                      onClick={() => void manejarBorrar(pregunta.id)}
                      className="text-xs text-[var(--color-muted)] underline-offset-2 hover:underline"
                    >
                      Borrar mi pregunta
                    </button>
                  ) : null}
                </div>
              )}

              {!pregunta.esPropia ? (
                <div className="mt-2">
                  <BotonReportar
                    tipoObjeto="PREGUNTA"
                    objetoId={pregunta.id}
                    etiquetaObjeto="esta pregunta"
                    compacto
                  />
                </div>
              ) : null}
            </li>
          ))}
        </ul>
      )}
    </SurfaceCard>
  );
}
