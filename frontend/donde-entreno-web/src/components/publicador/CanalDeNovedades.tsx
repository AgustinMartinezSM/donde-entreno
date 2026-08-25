"use client";

import Image from "next/image";
import Link from "next/link";
import { useEffect, useState } from "react";
import type { FormEvent } from "react";

import { useAuthSession } from "../auth/AuthSessionProvider";
import { AppButton } from "../ui/AppButton";
import { SectionHeader } from "../ui/SectionHeader";
import { StatusMessage } from "../ui/StatusMessage";
import { SurfaceCard } from "../ui/SurfaceCard";
import { PublicadorPageHeader } from "./PublicadorPageHeader";
import { construirUrlImagenBackend } from "../../lib/backendUrl";
import { formatearFechaRelativa } from "../../lib/formatoFecha";
import {
  MAX_TEXTO_NOVEDAD,
  NovedadesApiError,
  eliminarNovedadPropia,
  obtenerMisNovedades,
  publicarNovedad,
  type Novedad,
} from "../../services/novedadesService";
import { obtenerPerfilPublicador } from "../../services/publicadorService";
import { obtenerFotosDelPublicador } from "../../services/perfilPublicadorService";
import type { ImagenPerfilPublicador } from "../../types/publicadorPublico";

/*
  Canal de novedades (script 34, Fase 8): el publicador cuenta algo sin
  tener que crear ni editar una actividad. Publica directo —moderación
  flexible desde la Fase 4— y va al feed de sus seguidores.

  La foto NO se sube acá: se elige entre las que ya están publicadas.
  Subir es un flujo propio (el centro de fotos) y duplicarlo acá sería
  una segunda cola de moderación con las mismas reglas.
*/
export function CanalDeNovedades() {
  const { accessToken } = useAuthSession();

  const [novedades, setNovedades] = useState<Novedad[]>([]);
  const [fotos, setFotos] = useState<ImagenPerfilPublicador[]>([]);
  const [texto, setTexto] = useState("");
  const [imagenId, setImagenId] = useState<number | null>(null);
  const [cargando, setCargando] = useState(true);
  const [publicando, setPublicando] = useState(false);
  const [borrando, setBorrando] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [errorFormulario, setErrorFormulario] = useState<string | null>(null);
  const [publicada, setPublicada] = useState(false);

  useEffect(() => {
    let componenteActivo = true;

    if (!accessToken) {
      return () => {
        componenteActivo = false;
      };
    }

    async function cargar() {
      try {
        const mias = await obtenerMisNovedades(accessToken as string);

        if (!componenteActivo) {
          return;
        }

        setNovedades(mias);
        setError(null);

        /*
          Las fotos son opcionales para publicar: si esta parte falla,
          el canal igual funciona sin selector.
        */
        try {
          const perfil = await obtenerPerfilPublicador(accessToken as string);
          const suyas = await obtenerFotosDelPublicador(perfil.id);

          if (componenteActivo) {
            setFotos(suyas);
          }
        } catch {
          /* Sin selector de fotos, pero con canal. */
        }
      } catch (errorCarga: unknown) {
        if (!componenteActivo) {
          return;
        }

        setError(
          errorCarga instanceof NovedadesApiError
            ? errorCarga.message
            : "No pudimos cargar tus novedades. Probá de nuevo en unos minutos."
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

  async function manejarPublicar(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault();

    if (!accessToken || publicando || texto.trim().length === 0) {
      return;
    }

    setPublicando(true);
    setErrorFormulario(null);
    setPublicada(false);

    try {
      const nueva = await publicarNovedad(accessToken, texto, imagenId);

      setNovedades((actuales) => [nueva, ...actuales]);
      setTexto("");
      setImagenId(null);
      setPublicada(true);
    } catch (errorPublicar: unknown) {
      setErrorFormulario(
        errorPublicar instanceof NovedadesApiError
          ? errorPublicar.message
          : "No pudimos publicar tu novedad. Probá de nuevo."
      );
    } finally {
      setPublicando(false);
    }
  }

  async function manejarBorrar(novedad: Novedad) {
    if (!accessToken || borrando !== null) {
      return;
    }

    setBorrando(novedad.id);
    setErrorFormulario(null);

    try {
      await eliminarNovedadPropia(accessToken, novedad.id);
      setNovedades((actuales) =>
        actuales.filter((cada) => cada.id !== novedad.id)
      );
    } catch (errorBorrar: unknown) {
      setErrorFormulario(
        errorBorrar instanceof NovedadesApiError
          ? errorBorrar.message
          : "No pudimos borrar esa novedad. Probá de nuevo."
      );
    } finally {
      setBorrando(null);
    }
  }

  const restantes = MAX_TEXTO_NOVEDAD - texto.trim().length;

  return (
    <main className="min-h-screen text-[var(--color-text)]">
      <section className="mx-auto w-full max-w-4xl px-4 py-6">
        <PublicadorPageHeader
          title="Novedades"
          description="Contales algo a quienes te siguen sin tener que crear ni editar una actividad: un cambio de horario, un cupo que se liberó, cómo salió el torneo."
        />

        <div className="mt-6">
          <SectionHeader
            eyebrow="Tu canal"
            title="Escribí una novedad"
            description="Se publica al instante en tu perfil y en el feed de tus seguidores. Podés publicar hasta 3 por día."
          />
        </div>

        <SurfaceCard className="mt-4 p-5 sm:p-6">
          <form onSubmit={(evento) => void manejarPublicar(evento)}>
            <label
              htmlFor="texto-novedad"
              className="text-sm font-bold text-[var(--color-primary)]"
            >
              ¿Qué querés contar?
            </label>
            <textarea
              id="texto-novedad"
              value={texto}
              onChange={(evento) => {
                setTexto(evento.target.value.slice(0, MAX_TEXTO_NOVEDAD));
                setPublicada(false);
              }}
              rows={4}
              maxLength={MAX_TEXTO_NOVEDAD}
              placeholder="Este sábado el turno de las 10 pasa a las 11. ¡Los esperamos!"
              className="mt-2 w-full rounded-[16px] border border-[var(--color-border-accent)] bg-[var(--color-surface)] p-3 text-sm leading-6 text-[var(--color-text)] outline-none transition focus:border-[var(--color-primary)]"
            />
            <p className="mt-1 text-xs text-[var(--color-muted)]">
              {restantes} caracteres disponibles
            </p>

            {fotos.length > 0 ? (
              <div className="mt-4">
                <p className="text-sm font-bold text-[var(--color-primary)]">
                  Sumale una foto (opcional)
                </p>
                <p className="mt-1 text-xs text-[var(--color-muted)]">
                  Elegí una de las que ya tenés publicadas. Para subir fotos
                  nuevas, entrá a{" "}
                  <Link
                    href="/publicador/fotos"
                    className="font-bold text-[var(--color-primary)] underline underline-offset-2"
                  >
                    Mis fotos
                  </Link>
                  .
                </p>

                <ul className="mt-3 flex gap-3 overflow-x-auto pb-2">
                  {fotos.slice(0, 12).map((foto) => {
                    const url = construirUrlImagenBackend(foto.url);
                    const elegida = imagenId === foto.id;

                    if (!url) {
                      return null;
                    }

                    return (
                      <li key={foto.id} className="shrink-0">
                        <button
                          type="button"
                          aria-pressed={elegida}
                          onClick={() =>
                            setImagenId(elegida ? null : foto.id)
                          }
                          className={`relative block h-20 w-20 overflow-hidden rounded-[14px] border-2 transition ${
                            elegida
                              ? "border-[var(--color-primary)]"
                              : "border-[var(--color-border-soft)] hover:border-[var(--color-border-accent)]"
                          }`}
                        >
                          <Image
                            src={url}
                            alt={foto.titulo ?? "Foto publicada"}
                            fill
                            sizes="80px"
                            className="object-cover"
                          />
                        </button>
                      </li>
                    );
                  })}
                </ul>
              </div>
            ) : null}

            {errorFormulario ? (
              <StatusMessage variant="error" role="alert" className="mt-4">
                {errorFormulario}
              </StatusMessage>
            ) : null}

            {publicada ? (
              <StatusMessage variant="success" role="status" className="mt-4">
                Publicada. Ya la ven en tu perfil y en su feed.
              </StatusMessage>
            ) : null}

            <div className="mt-4">
              <AppButton
                type="submit"
                disabled={publicando || texto.trim().length === 0}
              >
                {publicando ? "Publicando..." : "Publicar novedad"}
              </AppButton>
            </div>
          </form>
        </SurfaceCard>

        <div className="mt-8">
          <SectionHeader
            eyebrow="Historial"
            title="Lo que publicaste"
            description="Podés borrar cualquiera: deja de verse en tu perfil y en el feed."
          />
        </div>

        {error ? (
          <StatusMessage variant="error" role="alert" className="mt-4">
            {error}
          </StatusMessage>
        ) : cargando ? (
          <StatusMessage variant="info" role="status" className="mt-4">
            Cargando tus novedades...
          </StatusMessage>
        ) : novedades.length === 0 ? (
          <SurfaceCard className="mt-4 p-6">
            <p className="text-sm text-[var(--color-muted)]">
              Todavía no contaste nada. La primera novedad es la que más se
              nota: quienes te siguen reciben un aviso.
            </p>
          </SurfaceCard>
        ) : (
          <ul className="mt-4 space-y-3">
            {novedades.map((novedad) => {
              const url = construirUrlImagenBackend(novedad.imagenUrl);

              return (
                <li key={novedad.id}>
                  <SurfaceCard as="article" className="p-5">
                    <div className="flex items-start justify-between gap-4">
                      <div className="min-w-0">
                        <p className="whitespace-pre-line text-sm leading-6 text-[var(--color-text)]">
                          {novedad.texto}
                        </p>
                        <p className="mt-2 text-xs text-[var(--color-muted)]">
                          {novedad.createdAt
                            ? formatearFechaRelativa(novedad.createdAt)
                            : null}
                        </p>
                      </div>

                      <AppButton
                        type="button"
                        variant="danger"
                        size="sm"
                        disabled={borrando === novedad.id}
                        onClick={() => void manejarBorrar(novedad)}
                      >
                        {borrando === novedad.id ? "Borrando..." : "Borrar"}
                      </AppButton>
                    </div>

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
