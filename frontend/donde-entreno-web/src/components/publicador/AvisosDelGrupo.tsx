"use client";

import { useEffect, useState } from "react";
import type { FormEvent } from "react";

import { useAuthSession } from "../auth/AuthSessionProvider";
import { AppButton } from "../ui/AppButton";
import { SectionHeader } from "../ui/SectionHeader";
import { StatusMessage } from "../ui/StatusMessage";
import { SurfaceCard } from "../ui/SurfaceCard";
import { formatearFechaRelativa } from "../../lib/formatoFecha";
import {
  GruposApiError,
  MAX_TEXTO_AVISO,
  avisarAlGrupo,
  eliminarAviso,
  obtenerAviso,
  obtenerGrupo,
  ocultarComentarioComoPublicador,
  type AvisoGrupo,
} from "../../services/gruposService";

/*
  El grupo desde el lado del publicador: avisar y moderar.

  El publicador ve el grupo a través del MISMO camino que un miembro
  (es dueño de la actividad, así que se lo puede sumar), y no hay un
  endpoint aparte que le devuelva el contenido: una sola puerta es más
  fácil de razonar y de auditar.
*/
export function AvisosDelGrupo({
  actividadId,
  actividadTitulo,
}: {
  actividadId: number;
  actividadTitulo: string;
}) {
  const { accessToken } = useAuthSession();

  const [avisos, setAvisos] = useState<AvisoGrupo[]>([]);
  const [miembros, setMiembros] = useState(0);
  const [texto, setTexto] = useState("");
  const [abierto, setAbierto] = useState<AvisoGrupo | null>(null);
  const [cargando, setCargando] = useState(true);
  const [procesando, setProcesando] = useState(false);
  const [error, setError] = useState<string | null>(null);
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
        const grupo = await obtenerGrupo(accessToken as string, actividadId);

        if (componenteActivo) {
          setAvisos(grupo.avisos);
          setMiembros(grupo.cantidadMiembros ?? 0);
          setError(null);
        }
      } catch (errorCarga: unknown) {
        if (componenteActivo) {
          setError(
            errorCarga instanceof GruposApiError
              ? errorCarga.message
              : "No pudimos cargar el grupo."
          );
        }
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
  }, [accessToken, actividadId]);

  async function recargar() {
    if (!accessToken) {
      return;
    }

    const grupo = await obtenerGrupo(accessToken, actividadId);
    setAvisos(grupo.avisos);
    setMiembros(grupo.cantidadMiembros ?? 0);
  }

  async function manejarAvisar(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault();

    if (!accessToken || procesando || !texto.trim()) {
      return;
    }

    setProcesando(true);
    setError(null);
    setPublicado(false);

    try {
      await avisarAlGrupo(accessToken, actividadId, texto);
      setTexto("");
      setPublicado(true);
      await recargar();
    } catch (errorAvisar: unknown) {
      setError(
        errorAvisar instanceof GruposApiError
          ? errorAvisar.message
          : "No pudimos publicar el aviso."
      );
    } finally {
      setProcesando(false);
    }
  }

  async function manejarBorrar(aviso: AvisoGrupo) {
    if (!accessToken || procesando) {
      return;
    }

    setProcesando(true);

    try {
      await eliminarAviso(accessToken, aviso.id);
      await recargar();
      setAbierto(null);
    } catch {
      setError("No pudimos borrar el aviso.");
    } finally {
      setProcesando(false);
    }
  }

  async function abrirComentarios(aviso: AvisoGrupo) {
    if (!accessToken) {
      return;
    }

    if (abierto?.id === aviso.id) {
      setAbierto(null);
      return;
    }

    try {
      setAbierto(await obtenerAviso(accessToken, aviso.id));
    } catch {
      setError("No pudimos abrir los comentarios.");
    }
  }

  async function ocultarComentario(comentarioId: number) {
    if (!accessToken || !abierto) {
      return;
    }

    try {
      await ocultarComentarioComoPublicador(accessToken, comentarioId);
      setAbierto(await obtenerAviso(accessToken, abierto.id));
    } catch {
      setError("No pudimos ocultar el comentario.");
    }
  }

  return (
    <>
      <SectionHeader
        eyebrow="Grupo"
        title={`Avisos de ${actividadTitulo}`}
        description={`Lo que les contás a quienes vienen. ${miembros} ${
          miembros === 1 ? "persona está" : "personas están"
        } en el grupo.`}
      />

      <SurfaceCard className="mt-4 p-5 sm:p-6">
        <form onSubmit={(evento) => void manejarAvisar(evento)}>
          <label
            htmlFor="texto-aviso"
            className="text-sm font-bold text-[var(--color-primary)]"
          >
            ¿Qué querés avisarles?
          </label>
          <textarea
            id="texto-aviso"
            value={texto}
            onChange={(evento) => {
              setTexto(evento.target.value);
              setPublicado(false);
            }}
            rows={3}
            maxLength={MAX_TEXTO_AVISO}
            placeholder="Mañana suspendemos por lluvia. El sábado entrenamos normal."
            className="mt-2 w-full rounded-[16px] border border-[var(--color-border-accent)] bg-[var(--color-surface)] p-3 text-sm leading-6 text-[var(--color-text)] outline-none transition focus:border-[var(--color-primary)]"
          />
          <p className="mt-1 text-xs text-[var(--color-muted)]">
            Les llega con la campanita. Podés avisar hasta 2 veces por día.
          </p>

          {error ? (
            <StatusMessage variant="error" role="alert" className="mt-3">
              {error}
            </StatusMessage>
          ) : null}

          {publicado ? (
            <StatusMessage variant="success" role="status" className="mt-3">
              Aviso enviado al grupo.
            </StatusMessage>
          ) : null}

          <AppButton
            type="submit"
            className="mt-4"
            disabled={procesando || !texto.trim()}
          >
            {procesando ? "Enviando..." : "Avisar al grupo"}
          </AppButton>
        </form>
      </SurfaceCard>

      {cargando ? (
        <StatusMessage variant="info" role="status" className="mt-6">
          Cargando los avisos...
        </StatusMessage>
      ) : avisos.length === 0 ? (
        <SurfaceCard className="mt-6 p-6">
          <p className="text-sm text-[var(--color-muted)]">
            Todavía no avisaste nada en este grupo.
          </p>
        </SurfaceCard>
      ) : (
        <ul className="mt-6 space-y-3">
          {avisos.map((aviso) => (
            <li key={aviso.id}>
              <SurfaceCard as="article" className="p-5">
                <div className="flex flex-wrap items-start justify-between gap-3">
                  <p className="min-w-0 whitespace-pre-line text-sm leading-6 text-[var(--color-text)]">
                    {aviso.texto}
                  </p>

                  <AppButton
                    type="button"
                    variant="danger"
                    size="sm"
                    disabled={procesando}
                    onClick={() => void manejarBorrar(aviso)}
                  >
                    Borrar
                  </AppButton>
                </div>

                <div className="mt-3 flex flex-wrap items-center gap-3 text-xs text-[var(--color-muted)]">
                  <span>
                    {aviso.createdAt
                      ? formatearFechaRelativa(aviso.createdAt)
                      : null}
                  </span>
                  {aviso.cantidadMeGusta ? (
                    <span>{aviso.cantidadMeGusta} me gusta</span>
                  ) : null}

                  <button
                    type="button"
                    onClick={() => void abrirComentarios(aviso)}
                    className="font-bold text-[var(--color-primary)] underline underline-offset-4"
                  >
                    {abierto?.id === aviso.id ? "Ocultar" : "Comentarios"}
                    {aviso.cantidadComentarios
                      ? ` (${aviso.cantidadComentarios})`
                      : ""}
                  </button>
                </div>

                {abierto?.id === aviso.id ? (
                  <ul className="mt-4 space-y-3 border-t border-[var(--color-border-soft)] pt-4">
                    {(abierto.comentarios ?? []).length === 0 ? (
                      <li className="text-sm text-[var(--color-muted)]">
                        Sin comentarios.
                      </li>
                    ) : (
                      (abierto.comentarios ?? []).map((comentario) => (
                        <li
                          key={comentario.id}
                          className="flex flex-wrap items-start justify-between gap-2"
                        >
                          <div className="min-w-0">
                            <p className="text-xs font-bold text-[var(--color-primary)]">
                              {comentario.autorNombre}
                            </p>
                            <p className="mt-0.5 text-sm leading-6 text-[var(--color-text)]">
                              {comentario.texto}
                            </p>
                          </div>

                          {/* El publicador modera su propio grupo. */}
                          <AppButton
                            type="button"
                            variant="secondary"
                            size="sm"
                            onClick={() => void ocultarComentario(comentario.id)}
                          >
                            Ocultar
                          </AppButton>
                        </li>
                      ))
                    )}
                  </ul>
                ) : null}
              </SurfaceCard>
            </li>
          ))}
        </ul>
      )}
    </>
  );
}
