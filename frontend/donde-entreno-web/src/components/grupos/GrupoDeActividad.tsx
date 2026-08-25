"use client";

import Image from "next/image";
import { useEffect, useState } from "react";
import { usePathname, useRouter } from "next/navigation";
import type { FormEvent } from "react";

import { useAuthSession } from "../auth/AuthSessionProvider";
import { BotonReportar } from "../social/BotonReportar";
import { AppButton } from "../ui/AppButton";
import { StatusMessage } from "../ui/StatusMessage";
import { SurfaceCard } from "../ui/SurfaceCard";
import { construirUrlImagenBackend } from "../../lib/backendUrl";
import { formatearFechaRelativa } from "../../lib/formatoFecha";
import {
  GruposApiError,
  MAX_TEXTO_COMENTARIO,
  comentarAviso,
  darMeGustaAviso,
  obtenerAviso,
  obtenerGrupo,
  quitarMeGustaAviso,
  salirDelGrupo,
  unirseAlGrupo,
  type AvisoGrupo,
  type GrupoActividad,
} from "../../services/gruposService";

/*
  El grupo de una actividad, en su detalle público.

  Quien no es miembro ve la invitación, nunca el contenido: el backend
  devuelve la lista de avisos VACÍA para no-miembros, así que esto no
  filtra nada en el cliente — no hay nada que filtrar.
*/
export function GrupoDeActividad({
  actividadId,
  actividadTitulo,
}: {
  actividadId: number;
  actividadTitulo: string;
}) {
  const router = useRouter();
  const pathname = usePathname();
  const { status, accessToken } = useAuthSession();

  const [grupo, setGrupo] = useState<GrupoActividad | null>(null);
  const [cargando, setCargando] = useState(true);
  const [procesando, setProcesando] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [abierto, setAbierto] = useState<AvisoGrupo | null>(null);
  const [comentario, setComentario] = useState("");

  useEffect(() => {
    let componenteActivo = true;

    /*
      Sin sesión no se toca el estado: el render ya sale antes por el
      caso anónimo, así que `cargando` no llega a mirarse. (Setearlo
      acá era un setState sincrónico dentro del efecto.)
    */
    if (status !== "authenticated" || !accessToken) {
      return () => {
        componenteActivo = false;
      };
    }

    async function cargar() {
      try {
        const actual = await obtenerGrupo(accessToken as string, actividadId);

        if (componenteActivo) {
          setGrupo(actual);
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
  }, [status, accessToken, actividadId]);

  async function manejarUnirse() {
    if (status !== "authenticated" || !accessToken) {
      router.push(`/login?returnTo=${encodeURIComponent(pathname)}`);
      return;
    }

    setProcesando(true);

    try {
      setGrupo(await unirseAlGrupo(accessToken, actividadId));
      setError(null);
    } catch (errorUnirse: unknown) {
      setError(
        errorUnirse instanceof GruposApiError
          ? errorUnirse.message
          : "No pudimos sumarte al grupo."
      );
    } finally {
      setProcesando(false);
    }
  }

  async function manejarSalir() {
    if (!accessToken) {
      return;
    }

    setProcesando(true);

    try {
      await salirDelGrupo(accessToken, actividadId);
      setGrupo(await obtenerGrupo(accessToken, actividadId));
      setAbierto(null);
    } catch {
      setError("No pudimos sacarte del grupo.");
    } finally {
      setProcesando(false);
    }
  }

  async function abrirAviso(aviso: AvisoGrupo) {
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
      setError("No pudimos abrir el aviso.");
    }
  }

  async function manejarComentar(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault();

    if (!accessToken || !abierto || !comentario.trim() || procesando) {
      return;
    }

    setProcesando(true);

    try {
      await comentarAviso(accessToken, abierto.id, comentario);
      setComentario("");
      setAbierto(await obtenerAviso(accessToken, abierto.id));
    } catch (errorComentar: unknown) {
      setError(
        errorComentar instanceof GruposApiError
          ? errorComentar.message
          : "No pudimos publicar tu comentario."
      );
    } finally {
      setProcesando(false);
    }
  }

  async function alternarMeGusta(aviso: AvisoGrupo) {
    if (!accessToken) {
      return;
    }

    try {
      const respuesta = aviso.meGusta
        ? await quitarMeGustaAviso(accessToken, aviso.id)
        : await darMeGustaAviso(accessToken, aviso.id);

      const actualizar = (cada: AvisoGrupo) =>
        cada.id === aviso.id
          ? {
              ...cada,
              meGusta: respuesta.meGusta,
              cantidadMeGusta: respuesta.cantidadMeGusta,
            }
          : cada;

      setGrupo((actual) =>
        actual ? { ...actual, avisos: actual.avisos.map(actualizar) } : actual
      );
      setAbierto((actual) => (actual ? actualizar(actual) : actual));
    } catch {
      /* Silencioso: el número vuelve solo en la próxima carga. */
    }
  }

  /* Anónimo: la invitación, sin pedirle nada al backend. */
  if (status !== "authenticated") {
    return (
      <SurfaceCard className="mt-5 p-6">
        <h3 className="text-base font-extrabold text-[var(--color-primary)]">
          El grupo de {actividadTitulo}
        </h3>
        <p className="mt-2 text-sm leading-6 text-[var(--color-muted)]">
          Quienes van reciben acá los avisos de quien organiza: cambios de
          horario, suspensiones, novedades. Entrá a tu cuenta para sumarte.
        </p>
        <AppButton
          type="button"
          className="mt-4"
          onClick={() =>
            router.push(`/login?returnTo=${encodeURIComponent(pathname)}`)
          }
        >
          Iniciar sesión
        </AppButton>
      </SurfaceCard>
    );
  }

  if (cargando) {
    return (
      <StatusMessage variant="info" role="status" className="mt-5">
        Cargando el grupo...
      </StatusMessage>
    );
  }

  if (error && !grupo) {
    return (
      <StatusMessage variant="error" role="alert" className="mt-5">
        {error}
      </StatusMessage>
    );
  }

  if (!grupo?.esMiembro) {
    return (
      <SurfaceCard className="mt-5 p-6">
        <h3 className="text-base font-extrabold text-[var(--color-primary)]">
          Sumate al grupo de {actividadTitulo}
        </h3>
        <p className="mt-2 text-sm leading-6 text-[var(--color-muted)]">
          Vas a recibir los avisos de quien organiza —cambios de horario,
          suspensiones— y vas a poder responder. Podés salir cuando quieras.
        </p>
        {grupo?.cantidadMiembros ? (
          <p className="mt-1 text-xs text-[var(--color-muted)]">
            {grupo.cantidadMiembros}{" "}
            {grupo.cantidadMiembros === 1 ? "persona ya está" : "personas ya están"}.
          </p>
        ) : null}

        {error ? (
          <StatusMessage variant="error" role="alert" className="mt-3">
            {error}
          </StatusMessage>
        ) : null}

        <AppButton
          type="button"
          className="mt-4"
          disabled={procesando}
          onClick={() => void manejarUnirse()}
        >
          {procesando ? "Sumándote..." : "Sumarme al grupo"}
        </AppButton>
      </SurfaceCard>
    );
  }

  return (
    <div className="mt-5">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <p className="text-sm text-[var(--color-muted)]">
          Sos parte del grupo · {grupo.cantidadMiembros}{" "}
          {grupo.cantidadMiembros === 1 ? "miembro" : "miembros"}
        </p>

        <AppButton
          type="button"
          variant="secondary"
          size="sm"
          disabled={procesando}
          onClick={() => void manejarSalir()}
        >
          Salir del grupo
        </AppButton>
      </div>

      {error ? (
        <StatusMessage variant="error" role="alert" className="mt-3">
          {error}
        </StatusMessage>
      ) : null}

      {grupo.avisos.length === 0 ? (
        <SurfaceCard className="mt-4 p-6">
          <p className="text-sm text-[var(--color-muted)]">
            Todavía no hay avisos. Cuando quien organiza tenga algo que
            contarles, te va a llegar acá y con la campanita.
          </p>
        </SurfaceCard>
      ) : (
        <ul className="mt-4 space-y-3">
          {grupo.avisos.map((aviso) => {
            const url = construirUrlImagenBackend(aviso.imagenUrl);
            const estaAbierto = abierto?.id === aviso.id;
            const vigente = estaAbierto ? abierto : aviso;

            return (
              <li key={aviso.id}>
                <SurfaceCard as="article" className="p-5">
                  <p className="whitespace-pre-line text-sm leading-6 text-[var(--color-text)]">
                    {aviso.texto}
                  </p>

                  {url ? (
                    <div className="relative mt-3 h-44 w-full overflow-hidden rounded-[16px] sm:h-56">
                      <Image
                        src={url}
                        alt=""
                        fill
                        sizes="(max-width: 640px) 100vw, 60vw"
                        className="object-cover"
                      />
                    </div>
                  ) : null}

                  <div className="mt-3 flex flex-wrap items-center gap-3">
                    <button
                      type="button"
                      onClick={() => void alternarMeGusta(vigente)}
                      aria-pressed={Boolean(vigente.meGusta)}
                      className={`inline-flex min-h-9 items-center gap-1.5 rounded-full px-3 text-sm font-bold transition ${
                        vigente.meGusta
                          ? "bg-[var(--color-brand)] text-white"
                          : "border border-[var(--color-border-accent)] bg-[var(--color-surface)] text-[var(--color-primary)]"
                      }`}
                    >
                      ♥{" "}
                      {vigente.cantidadMeGusta && vigente.cantidadMeGusta > 0
                        ? vigente.cantidadMeGusta
                        : "Me gusta"}
                    </button>

                    <button
                      type="button"
                      onClick={() => void abrirAviso(aviso)}
                      className="text-sm font-bold text-[var(--color-primary)] underline underline-offset-4"
                    >
                      {estaAbierto ? "Ocultar" : "Comentarios"}
                      {aviso.cantidadComentarios
                        ? ` (${aviso.cantidadComentarios})`
                        : ""}
                    </button>

                    <span className="text-xs text-[var(--color-muted)]">
                      {aviso.createdAt
                        ? formatearFechaRelativa(aviso.createdAt)
                        : null}
                    </span>

                    <span className="ml-auto">
                      <BotonReportar
                        tipoObjeto="AVISO_GRUPO"
                        objetoId={aviso.id}
                        etiquetaObjeto="este aviso"
                        compacto
                      />
                    </span>
                  </div>

                  {estaAbierto ? (
                    <div className="mt-4 border-t border-[var(--color-border-soft)] pt-4">
                      <ul className="space-y-3">
                        {(abierto?.comentarios ?? []).map((cada) => (
                          <li key={cada.id}>
                            <p className="text-xs font-bold text-[var(--color-primary)]">
                              {cada.autorNombre}
                            </p>
                            <p className="mt-0.5 text-sm leading-6 text-[var(--color-text)]">
                              {cada.texto}
                            </p>
                            <div className="mt-1 flex items-center gap-3">
                              <span className="text-[11px] text-[var(--color-muted)]">
                                {cada.createdAt
                                  ? formatearFechaRelativa(cada.createdAt)
                                  : null}
                              </span>
                              {!cada.esPropio ? (
                                <BotonReportar
                                  tipoObjeto="COMENTARIO_GRUPO"
                                  objetoId={cada.id}
                                  etiquetaObjeto="este comentario"
                                  compacto
                                />
                              ) : null}
                            </div>
                          </li>
                        ))}
                      </ul>

                      <form
                        className="mt-3"
                        onSubmit={(evento) => void manejarComentar(evento)}
                      >
                        <label htmlFor={`comentario-${aviso.id}`} className="sr-only">
                          Escribí un comentario
                        </label>
                        <textarea
                          id={`comentario-${aviso.id}`}
                          value={comentario}
                          onChange={(evento) => setComentario(evento.target.value)}
                          rows={2}
                          maxLength={MAX_TEXTO_COMENTARIO}
                          placeholder="Escribí algo al grupo..."
                          className="w-full rounded-[14px] border border-[var(--color-border-accent)] bg-[var(--color-surface)] p-2.5 text-sm leading-6 text-[var(--color-text)] outline-none transition focus:border-[var(--color-primary)]"
                        />
                        <AppButton
                          type="submit"
                          size="sm"
                          className="mt-2"
                          disabled={procesando || !comentario.trim()}
                        >
                          Comentar
                        </AppButton>
                      </form>
                    </div>
                  ) : null}
                </SurfaceCard>
              </li>
            );
          })}
        </ul>
      )}
    </div>
  );
}
