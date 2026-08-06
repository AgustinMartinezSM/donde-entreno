"use client";

import Link from "next/link";
import { useEffect, useRef, useState } from "react";
import type { MensajeAsistente } from "../../lib/asistente/tipos";

type AsistenteConversacionProps = {
  mensajes: MensajeAsistente[];
  escribiendo: boolean;
  onEnviarMensaje: (texto: string) => void;
};

/*
  Conversación del asistente: lista de burbujas, enlaces internos como chips,
  opciones rápidas como pills y el input para escribir.

  Las opciones rápidas solo se muestran en el último mensaje del asistente,
  para que la conversación no quede llena de botones viejos.
*/
export function AsistenteConversacion({
  mensajes,
  escribiendo,
  onEnviarMensaje,
}: AsistenteConversacionProps) {
  const [texto, setTexto] = useState("");
  const contenedorMensajesRef = useRef<HTMLDivElement | null>(null);
  const inputRef = useRef<HTMLInputElement | null>(null);

  /*
    Al abrir el panel, el foco va directo al input (requisito de accesibilidad).
  */
  useEffect(() => {
    inputRef.current?.focus();
  }, []);

  /*
    Auto-scroll al último mensaje cada vez que llega uno nuevo
    o aparece el indicador de "escribiendo".
  */
  useEffect(() => {
    const contenedor = contenedorMensajesRef.current;

    if (contenedor) {
      contenedor.scrollTop = contenedor.scrollHeight;
    }
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

  return (
    <>
      <div
        ref={contenedorMensajesRef}
        role="log"
        aria-live="polite"
        aria-label="Mensajes de la conversación"
        className="flex-1 space-y-3 overflow-y-auto bg-white px-4 py-4"
      >
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
              className={
                esAsistente
                  ? "flex flex-col items-start gap-2"
                  : "flex justify-end"
              }
            >
              <div
                className={
                  esAsistente
                    ? "max-w-[85%] rounded-2xl rounded-bl-md bg-[#F8FAFC] px-4 py-2.5 ring-1 ring-[#DDEAF3]"
                    : "max-w-[85%] rounded-2xl rounded-br-md bg-[#0F3D5E] px-4 py-2.5 shadow-sm"
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
                      className="inline-flex max-w-full items-center gap-1.5 rounded-full border border-[#BFDDEA] bg-white px-3 py-1.5 text-xs font-bold text-[var(--color-primary)] shadow-sm transition duration-200 ease-out hover:-translate-y-0.5 hover:border-[var(--color-primary)] hover:bg-[#F8FCFE] active:scale-[0.98]"
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
                      className="rounded-full border border-[#DDEAF3] bg-white px-3 py-1.5 text-left text-xs font-bold text-[var(--color-primary)] shadow-sm transition duration-200 ease-out hover:-translate-y-0.5 hover:border-[#BFDDEA] hover:bg-[#F8FCFE] active:scale-[0.98]"
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
            <div className="flex items-center gap-1.5 rounded-2xl rounded-bl-md bg-[#F8FAFC] px-4 py-3 ring-1 ring-[#DDEAF3]">
              <span className="sr-only">El asistente está escribiendo…</span>
              <span
                aria-hidden="true"
                className="h-1.5 w-1.5 animate-bounce rounded-full bg-[var(--color-accent)]"
              />
              <span
                aria-hidden="true"
                className="h-1.5 w-1.5 animate-bounce rounded-full bg-[var(--color-accent)] [animation-delay:150ms]"
              />
              <span
                aria-hidden="true"
                className="h-1.5 w-1.5 animate-bounce rounded-full bg-[var(--color-accent)] [animation-delay:300ms]"
              />
            </div>
          </div>
        ) : null}
      </div>

      <form
        onSubmit={manejarEnvio}
        className="border-t border-[#DDEAF3] bg-white p-3"
      >
        <div className="flex items-center gap-2">
          <input
            ref={inputRef}
            type="text"
            value={texto}
            onChange={(evento) => setTexto(evento.target.value)}
            aria-label="Escribí tu consulta para el asistente"
            placeholder="Escribí tu consulta…"
            className="min-h-11 w-full min-w-0 flex-1 rounded-full border border-[#DDEAF3] bg-[#F8FAFC] px-4 text-sm font-medium text-[var(--color-text)] outline-none transition duration-200 ease-out placeholder:text-[var(--color-muted)] hover:border-[#BFDDEA] focus:border-[var(--color-accent)] focus-visible:ring-2 focus-visible:ring-[#4FB3D9]/30"
          />
          <button
            type="submit"
            disabled={escribiendo || texto.trim().length === 0}
            aria-label="Enviar mensaje"
            className="flex h-11 w-11 shrink-0 items-center justify-center rounded-full bg-[#2EB872] text-white shadow-[0_14px_35px_rgba(46,184,114,0.28)] transition duration-200 ease-out hover:-translate-y-0.5 hover:bg-[#249B60] active:scale-95 disabled:cursor-not-allowed disabled:opacity-50 disabled:hover:translate-y-0"
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
      </form>
    </>
  );
}
