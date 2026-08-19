"use client";

import Link from "next/link";
import { useEffect, useRef, useState } from "react";
import { MAX_CARACTERES_CONSULTA } from "../../lib/asistente/motorRemoto";
import type { MensajeAsistente } from "../../lib/asistente/tipos";

type AsistenteConversacionProps = {
  mensajes: MensajeAsistente[];
  escribiendo: boolean;
  avisoDeEspera: string;
  onEnviarMensaje: (texto: string) => void;
};

/* A partir de acá se muestra el contador: antes solo sería ruido. */
const CARACTERES_PARA_AVISAR = MAX_CARACTERES_CONSULTA - 60;

/*
  Conversación del asistente: lista de burbujas, enlaces internos como chips,
  opciones rápidas como pills y el input para escribir.

  Las opciones rápidas solo se muestran en el último mensaje del asistente,
  y solo si ese mensaje trae alguna: desde el asistente V2 muchas
  respuestas vienen sin ninguna, a propósito. Una respuesta que ya cierra
  no necesita botones encima.
*/
export function AsistenteConversacion({
  mensajes,
  escribiendo,
  avisoDeEspera,
  onEnviarMensaje,
}: AsistenteConversacionProps) {
  const [texto, setTexto] = useState("");
  const contenedorMensajesRef = useRef<HTMLDivElement | null>(null);
  const ultimaRespuestaRef = useRef<HTMLDivElement | null>(null);
  const inputRef = useRef<HTMLInputElement | null>(null);

  /*
    Al abrir el panel, el foco va directo al input (requisito de accesibilidad).
  */
  useEffect(() => {
    inputRef.current?.focus();
  }, []);

  /*
    Scroll: al ARRANQUE de la respuesta, no al final del panel.

    V1 hacía siempre scrollTop = scrollHeight, así que una respuesta larga
    te dejaba mirando su último renglón y tenías que subir para leerla
    desde el principio. Ahora, cuando llega una respuesta del asistente, se
    la lleva al tope del área visible; en cualquier otro caso (tu mensaje,
    el indicador de escritura) se baja hasta el final, que es lo que se
    espera.
  */
  useEffect(() => {
    const contenedor = contenedorMensajesRef.current;

    if (!contenedor) {
      return;
    }

    const ultimo = mensajes[mensajes.length - 1];
    const respuesta = ultimaRespuestaRef.current;

    if (ultimo?.autor === "asistente" && respuesta && !escribiendo) {
      contenedor.scrollTo({
        top: Math.max(0, respuesta.offsetTop - 12),
        behavior: "smooth",
      });
      return;
    }

    contenedor.scrollTop = contenedor.scrollHeight;
  }, [mensajes, escribiendo]);

  function manejarEnvio(evento: React.FormEvent<HTMLFormElement>) {
    evento.preventDefault();

    const textoLimpio = texto.trim();

    if (!textoLimpio || escribiendo) {
      return;
    }

    onEnviarMensaje(textoLimpio);
    setTexto("");
    inputRef.current?.focus();
  }

  function manejarOpcionRapida(opcion: string) {
    if (escribiendo) {
      return;
    }

    onEnviarMensaje(opcion);
    inputRef.current?.focus();
  }

  const ultimoMensaje = mensajes[mensajes.length - 1];
  const conversacionEmpezada = mensajes.length > 1;
  const restantes = MAX_CARACTERES_CONSULTA - texto.length;

  return (
    <>
      <div
        ref={contenedorMensajesRef}
        role="log"
        aria-live="polite"
        aria-label="Mensajes de la conversación"
        className="relative flex-1 space-y-3 overflow-y-auto bg-white px-4 py-4"
      >
        {/*
          Aviso de privacidad, arriba de la conversación y no fijo en el
          encabezado: se ve al abrir el asistente y se va con el scroll a
          medida que la charla avanza, sin comerse alto en un panel que ya
          es chico.

          Una vez que la charla arrancó se encoge a un renglón: en ese
          punto ya lo leíste, y lo que importa es la conversación. Dice
          "puede enviarse" porque es literal: lo que el asistente resuelve
          en el navegador nunca sale del dispositivo.
        */}
        <div className="rounded-2xl bg-[var(--color-surface-soft)] px-4 py-3 text-xs leading-5 text-[var(--color-muted)] ring-1 ring-[var(--color-border-soft)]">
          {conversacionEmpezada ? (
            <details className="group">
              <summary className="cursor-pointer list-none [&::-webkit-details-marker]:hidden">
                Tu mensaje <strong>puede enviarse a Google Gemini</strong>.
                <span className="ml-1 font-bold text-[var(--color-primary)]">
                  Cómo funciona
                  <span
                    aria-hidden="true"
                    className="ml-1 inline-block transition group-open:rotate-180"
                  >
                    ▾
                  </span>
                </span>
              </summary>
              <p className="mt-2">
                No compartas datos sensibles. Las recomendaciones de
                actividades salen de lo publicado en DondeEntreno: la IA no
                inventa clubes, precios ni enlaces, y no aprueba ni modifica
                nada. Si no hay actividades de algo, te lo decimos.
              </p>
            </details>
          ) : (
            <>
              <p>
                Al usar el asistente, tu mensaje <strong>puede enviarse a
                Google Gemini</strong> para generar una respuesta. No
                compartas datos sensibles.
              </p>

              <details className="group mt-1">
                <summary className="cursor-pointer list-none font-bold text-[var(--color-primary)] [&::-webkit-details-marker]:hidden">
                  Cómo funciona
                  <span
                    aria-hidden="true"
                    className="ml-1 inline-block transition group-open:rotate-180"
                  >
                    ▾
                  </span>
                </summary>
                <p className="mt-2">
                  Las recomendaciones de actividades salen de lo publicado en
                  DondeEntreno: la IA no inventa clubes, precios ni enlaces, y
                  no aprueba ni modifica nada. Si no hay actividades de algo,
                  te lo decimos.
                </p>
              </details>
            </>
          )}
        </div>

        {mensajes.map((mensaje) => {
          const esAsistente = mensaje.autor === "asistente";
          const esUltimoMensaje = ultimoMensaje?.id === mensaje.id;
          const mostrarEnlaces = Boolean(
            esAsistente && mensaje.enlaces && mensaje.enlaces.length > 0
          );
          const mostrarOpciones = Boolean(
            esAsistente &&
              esUltimoMensaje &&
              !escribiendo &&
              mensaje.opcionesRapidas &&
              mensaje.opcionesRapidas.length > 0
          );

          return (
            <div
              key={mensaje.id}
              ref={esAsistente && esUltimoMensaje ? ultimaRespuestaRef : null}
              className={
                esAsistente
                  ? "flex flex-col items-start gap-2"
                  : "flex justify-end"
              }
            >
              <div
                className={
                  esAsistente
                    ? "max-w-[85%] rounded-2xl rounded-bl-md bg-[var(--color-bg)] px-4 py-2.5 ring-1 ring-[var(--color-border-soft)]"
                    : "max-w-[85%] rounded-2xl rounded-br-md bg-[var(--color-primary)] px-4 py-2.5 shadow-sm"
                }
              >
                <p
                  className={
                    esAsistente
                      ? "whitespace-pre-line break-words text-sm leading-6 text-[var(--color-text)]"
                      : "whitespace-pre-line break-words text-sm leading-6 text-white"
                  }
                >
                  {mensaje.texto}
                </p>
              </div>

              {mostrarEnlaces ? (
                <div className="flex max-w-[90%] flex-wrap gap-2">
                  {mensaje.enlaces?.map((enlace) => (
                    <Link
                      key={`${mensaje.id}-${enlace.href}`}
                      href={enlace.href}
                      className="inline-flex max-w-full items-center gap-1.5 rounded-full border border-[var(--color-border-accent)] bg-white px-3 py-1.5 text-xs font-bold text-[var(--color-primary)] shadow-sm transition duration-200 ease-out hover:-translate-y-0.5 hover:border-[var(--color-primary)] hover:bg-[var(--color-surface-soft)] active:scale-[0.98]"
                    >
                      <span className="truncate">{enlace.etiqueta}</span>
                      <span aria-hidden="true" className="shrink-0">
                        →
                      </span>
                    </Link>
                  ))}
                </div>
              ) : null}

              {mostrarOpciones ? (
                <div className="flex max-w-[90%] flex-wrap gap-2">
                  {mensaje.opcionesRapidas?.map((opcion) => (
                    <button
                      key={`${mensaje.id}-${opcion}`}
                      type="button"
                      onClick={() => manejarOpcionRapida(opcion)}
                      className="rounded-full border border-[var(--color-border-soft)] bg-white px-3 py-1.5 text-left text-xs font-bold text-[var(--color-primary)] shadow-sm transition duration-200 ease-out hover:-translate-y-0.5 hover:border-[var(--color-border-accent)] hover:bg-[var(--color-surface-soft)] active:scale-[0.98]"
                    >
                      {opcion}
                    </button>
                  ))}
                </div>
              ) : null}
            </div>
          );
        })}

        {escribiendo ? (
          <div className="flex justify-start">
            <div className="flex items-center gap-2 rounded-2xl rounded-bl-md bg-[var(--color-bg)] px-4 py-2.5 ring-1 ring-[var(--color-border-soft)]">
              <span className="flex items-center gap-1.5" aria-hidden="true">
                <span className="h-1.5 w-1.5 animate-bounce rounded-full bg-[var(--color-accent)]" />
                <span className="h-1.5 w-1.5 animate-bounce rounded-full bg-[var(--color-accent)] [animation-delay:150ms]" />
                <span className="h-1.5 w-1.5 animate-bounce rounded-full bg-[var(--color-accent)] [animation-delay:300ms]" />
              </span>
              <span className="text-xs font-medium text-[var(--color-muted)]">
                {avisoDeEspera}
              </span>
            </div>
          </div>
        ) : null}
      </div>

      <form
        onSubmit={manejarEnvio}
        className="border-t border-[var(--color-border-soft)] bg-white p-3 pb-[max(0.75rem,env(safe-area-inset-bottom))] sm:pb-3"
      >
        <div className="flex items-center gap-2">
          <input
            ref={inputRef}
            type="text"
            value={texto}
            /*
              El tope lo valida el backend con un 400. Sin maxLength, pasarse
              de largo era una falla silenciosa: la consulta se descartaba
              antes de salir y el usuario veía una respuesta genérica sin
              entender por qué.
            */
            maxLength={MAX_CARACTERES_CONSULTA}
            onChange={(evento) => setTexto(evento.target.value)}
            aria-label="Escribí tu consulta para el asistente"
            placeholder="Escribí tu consulta…"
            className="min-h-11 w-full min-w-0 flex-1 rounded-full border border-[var(--color-border-soft)] bg-[var(--color-bg)] px-4 text-sm font-medium text-[var(--color-text)] outline-none transition duration-200 ease-out placeholder:text-[var(--color-muted)] hover:border-[var(--color-border-accent)] focus:border-[var(--color-accent)] focus-visible:ring-2 focus-visible:ring-[#4FB3D9]/30"
          />
          <button
            type="submit"
            disabled={escribiendo || texto.trim().length === 0}
            aria-label="Enviar mensaje"
            className="flex h-11 w-11 shrink-0 items-center justify-center rounded-full bg-[var(--color-secondary)] text-white shadow-[0_14px_35px_rgba(46,184,114,0.28)] transition duration-200 ease-out hover:-translate-y-0.5 hover:bg-[#249B60] active:scale-95 disabled:cursor-not-allowed disabled:opacity-50 disabled:hover:translate-y-0"
          >
            <svg
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth="2"
              strokeLinecap="round"
              strokeLinejoin="round"
              className="h-5 w-5"
              aria-hidden="true"
            >
              <path d="m22 2-7 20-4-9-9-4z" />
              <path d="M22 2 11 13" />
            </svg>
          </button>
        </div>

        {texto.length >= CARACTERES_PARA_AVISAR ? (
          <p
            aria-live="polite"
            className={
              restantes === 0
                ? "mt-1.5 px-2 text-right text-xs font-bold text-[#C2410C]"
                : "mt-1.5 px-2 text-right text-xs font-medium text-[var(--color-muted)]"
            }
          >
            {restantes === 0
              ? "Llegaste al máximo de 300 caracteres"
              : `Te quedan ${restantes} caracteres`}
          </p>
        ) : null}
      </form>
    </>
  );
}
