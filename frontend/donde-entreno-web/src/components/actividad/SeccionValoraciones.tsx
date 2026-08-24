"use client";

import { useEffect, useState } from "react";
import { usePathname, useRouter } from "next/navigation";
import type { FormEvent } from "react";

import { useAuthSession } from "../auth/AuthSessionProvider";
import {
  ConfianzaApiError,
  TAGS_VALORACION,
  eliminarValoracion,
  enviarValoracion,
  obtenerValoraciones,
  type ResumenValoraciones,
} from "../../services/confianzaService";
import { formatearFechaRelativa } from "../../lib/formatoFecha";
import { BotonReportar } from "../social/BotonReportar";
import { AppButton } from "../ui/AppButton";
import { SectionHeader } from "../ui/SectionHeader";
import { StatusMessage } from "../ui/StatusMessage";
import { SurfaceCard } from "../ui/SurfaceCard";

const ETIQUETAS_TAGS: Record<string, string> = Object.fromEntries(
  TAGS_VALORACION.map((tag) => [tag.valor, tag.etiqueta])
);

/*
  Valoraciones del detalle (Fase 3, script 29): promedio con 3+,
  distribución, lista con insignia Verificada, formulario propio
  (exige señal de uso — el backend responde el mensaje claro) y
  reportar ajenas. Client: carga después del shell del detalle.
*/
export function SeccionValoraciones({ actividadId }: { actividadId: number }) {
  const router = useRouter();
  const pathname = usePathname();
  const { status, accessToken } = useAuthSession();

  const [resumen, setResumen] = useState<ResumenValoraciones | null>(null);
  const [errorCarga, setErrorCarga] = useState(false);
  const [formAbierto, setFormAbierto] = useState(false);
  const [puntaje, setPuntaje] = useState(0);
  const [comentario, setComentario] = useState("");
  const [tags, setTags] = useState<string[]>([]);
  const [enviando, setEnviando] = useState(false);
  const [errorEnvio, setErrorEnvio] = useState<string | null>(null);

  useEffect(() => {
    let componenteActivo = true;

    obtenerValoraciones(actividadId, accessToken)
      .then((datos) => {
        if (componenteActivo) {
          setResumen(datos);
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

  const propia = resumen?.contenido.find((valoracion) => valoracion.esPropia);

  function abrirFormulario() {
    if (status !== "authenticated") {
      router.push(
        `/login?motivo=cuenta&returnTo=${encodeURIComponent(pathname ?? "/")}`
      );
      return;
    }

    if (propia) {
      setPuntaje(propia.puntaje);
      setComentario(propia.comentario ?? "");
      setTags(propia.tags);
    }

    setErrorEnvio(null);
    setFormAbierto(true);
  }

  async function manejarEnvio(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault();

    if (enviando || !accessToken) {
      return;
    }

    if (puntaje < 1) {
      setErrorEnvio("Elegí cuántas estrellas le das.");
      return;
    }

    setEnviando(true);
    setErrorEnvio(null);

    try {
      await enviarValoracion(accessToken, actividadId, puntaje, comentario, tags);
      const datos = await obtenerValoraciones(actividadId, accessToken);
      setResumen(datos);
      setFormAbierto(false);
    } catch (error: unknown) {
      setErrorEnvio(
        error instanceof ConfianzaApiError
          ? error.message
          : "No pudimos guardar tu valoración. Probá nuevamente."
      );
    } finally {
      setEnviando(false);
    }
  }

  async function manejarEliminar() {
    if (!accessToken || enviando) {
      return;
    }

    setEnviando(true);

    try {
      await eliminarValoracion(accessToken, actividadId);
      const datos = await obtenerValoraciones(actividadId, accessToken);
      setResumen(datos);
      setFormAbierto(false);
      setPuntaje(0);
      setComentario("");
      setTags([]);
    } catch {
      /* Best-effort. */
    } finally {
      setEnviando(false);
    }
  }

  return (
    <SurfaceCard as="section" id="valoraciones" className="scroll-mt-6 p-6 sm:p-8">
      <SectionHeader
        eyebrow="Confianza"
        title="Valoraciones"
        description="Opiniones reales de gente que guardó, probó o entrena en esta actividad."
      />

      {errorCarga ? (
        <StatusMessage variant="error" role="alert" className="mt-5">
          No pudimos cargar las valoraciones. Probá nuevamente.
        </StatusMessage>
      ) : resumen === null ? (
        <p className="mt-5 text-sm text-[var(--color-muted)]">Cargando...</p>
      ) : (
        <>
          <div className="mt-5 flex flex-wrap items-center gap-x-6 gap-y-3">
            {resumen.promedio !== null ? (
              <div className="flex items-center gap-2">
                <Estrellas valor={Math.round(resumen.promedio)} tamanio="grande" />
                <span className="text-2xl font-extrabold text-[var(--color-primary)]">
                  {resumen.promedio.toFixed(1)}
                </span>
              </div>
            ) : null}
            <p className="text-sm font-semibold text-[var(--color-muted)]">
              {resumen.cantidad === 0
                ? "Todavía no hay valoraciones: la tuya puede ser la primera."
                : resumen.cantidad === 1
                  ? "1 valoración"
                  : `${resumen.cantidad} valoraciones${
                      resumen.promedio === null
                        ? " (el promedio aparece con 3 o más)"
                        : ""
                    }`}
            </p>
          </div>

          {!formAbierto ? (
            <AppButton
              type="button"
              variant={propia ? "secondary" : "primary"}
              className="mt-4"
              onClick={abrirFormulario}
            >
              {propia ? "Editar mi valoración" : "Valorar esta actividad"}
            </AppButton>
          ) : (
            <form
              onSubmit={manejarEnvio}
              className="mt-5 rounded-[18px] border border-[var(--color-border-soft)] bg-[var(--color-bg)] p-4"
            >
              <p className="text-sm font-bold text-[var(--color-primary)]">
                ¿Cuántas estrellas le das?
              </p>
              <div className="mt-2 flex gap-1" role="radiogroup" aria-label="Puntaje">
                {[1, 2, 3, 4, 5].map((valor) => (
                  <button
                    key={valor}
                    type="button"
                    role="radio"
                    aria-checked={puntaje === valor}
                    aria-label={`${valor} ${valor === 1 ? "estrella" : "estrellas"}`}
                    onClick={() => setPuntaje(valor)}
                    className={`text-3xl transition duration-150 ease-out hover:scale-110 ${
                      valor <= puntaje
                        ? "text-[#F0B429]"
                        : "text-[var(--color-border-accent)]"
                    }`}
                  >
                    ★
                  </button>
                ))}
              </div>

              <div className="mt-3 flex flex-wrap gap-2">
                {TAGS_VALORACION.map((tag) => {
                  const activo = tags.includes(tag.valor);
                  return (
                    <button
                      key={tag.valor}
                      type="button"
                      aria-pressed={activo}
                      onClick={() =>
                        setTags((actuales) =>
                          activo
                            ? actuales.filter((cada) => cada !== tag.valor)
                            : [...actuales, tag.valor]
                        )
                      }
                      className={`min-h-9 rounded-full px-3 text-xs font-bold transition duration-200 ease-out ${
                        activo
                          ? "bg-[var(--color-brand)] text-white"
                          : "border border-[var(--color-border-accent)] bg-[var(--color-surface)] text-[var(--color-primary)]"
                      }`}
                    >
                      {tag.etiqueta}
                    </button>
                  );
                })}
              </div>

              <textarea
                value={comentario}
                onChange={(evento) => setComentario(evento.target.value)}
                maxLength={500}
                rows={3}
                placeholder="Contá tu experiencia (opcional)"
                disabled={enviando}
                className="mt-3 w-full rounded-[14px] border border-[var(--color-border-accent)] bg-[var(--color-surface)] px-3 py-2 text-sm text-[var(--color-text)] outline-none transition duration-200 ease-out focus:border-[var(--color-accent)] focus:ring-4 focus:ring-[var(--color-border-soft)]"
              />

              {errorEnvio ? (
                <StatusMessage variant="error" role="alert" className="mt-3">
                  {errorEnvio}
                </StatusMessage>
              ) : null}

              <div className="mt-3 flex flex-wrap gap-2">
                <AppButton type="submit" disabled={enviando}>
                  {enviando ? "Guardando..." : "Publicar valoración"}
                </AppButton>
                <AppButton
                  type="button"
                  variant="secondary"
                  onClick={() => setFormAbierto(false)}
                  disabled={enviando}
                >
                  Cancelar
                </AppButton>
                {propia ? (
                  <AppButton
                    type="button"
                    variant="danger"
                    onClick={() => void manejarEliminar()}
                    disabled={enviando}
                  >
                    Eliminar la mía
                  </AppButton>
                ) : null}
              </div>
            </form>
          )}

          {resumen.contenido.length > 0 ? (
            <ul className="mt-6 space-y-4">
              {resumen.contenido.map((valoracion) => (
                <li
                  key={valoracion.id}
                  className="rounded-[18px] border border-[var(--color-border-soft)] bg-[var(--color-bg)] p-4"
                >
                  <div className="flex flex-wrap items-center gap-x-3 gap-y-1">
                    <Estrellas valor={valoracion.puntaje} />
                    <span className="text-sm font-bold text-[var(--color-primary)]">
                      {valoracion.autorNombre}
                    </span>
                    {valoracion.verificada ? (
                      <span className="rounded-full bg-[var(--color-success-wash)] px-2 py-0.5 text-[11px] font-extrabold text-[var(--color-success)]">
                        ✓ Verificada
                      </span>
                    ) : null}
                    {valoracion.createdAt ? (
                      <span className="text-xs text-[var(--color-muted)]">
                        {formatearFechaRelativa(valoracion.createdAt)}
                      </span>
                    ) : null}
                  </div>

                  {valoracion.comentario ? (
                    <p className="mt-2 text-sm leading-6 text-[var(--color-text)]">
                      {valoracion.comentario}
                    </p>
                  ) : null}

                  {valoracion.tags.length > 0 ? (
                    <div className="mt-2 flex flex-wrap gap-1.5">
                      {valoracion.tags.map((tag) => (
                        <span
                          key={tag}
                          className="rounded-full bg-[var(--color-surface-soft)] px-2.5 py-1 text-[11px] font-bold text-[var(--color-primary)]"
                        >
                          {ETIQUETAS_TAGS[tag] ?? tag}
                        </span>
                      ))}
                    </div>
                  ) : null}

                  {!valoracion.esPropia ? (
                    <div className="mt-2">
                      <BotonReportar
                        tipoObjeto="VALORACION"
                        objetoId={valoracion.id}
                        etiquetaObjeto="esta valoración"
                        compacto
                      />
                    </div>
                  ) : null}
                </li>
              ))}
            </ul>
          ) : null}
        </>
      )}
    </SurfaceCard>
  );
}

function Estrellas({
  valor,
  tamanio = "normal",
}: {
  valor: number;
  tamanio?: "normal" | "grande";
}) {
  return (
    <span
      aria-label={`${valor} de 5 estrellas`}
      className={tamanio === "grande" ? "text-xl" : "text-sm"}
    >
      {[1, 2, 3, 4, 5].map((cada) => (
        <span
          key={cada}
          aria-hidden="true"
          className={
            cada <= valor ? "text-[#F0B429]" : "text-[var(--color-border-accent)]"
          }
        >
          ★
        </span>
      ))}
    </span>
  );
}
