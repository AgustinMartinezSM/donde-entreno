"use client";

import { useEffect, useRef, useState } from "react";
import { usePathname } from "next/navigation";
import { RESPUESTA_BIENVENIDA } from "../../lib/asistente/conocimiento";
import { motorAsistenteLocal } from "../../lib/asistente/motorLocal";
import type { MensajeAsistente } from "../../lib/asistente/tipos";
import { AsistenteConversacion } from "./AsistenteConversacion";

/*
  Widget flotante del Asistente DondeEntreno.

  - Burbuja circular azul en la esquina inferior derecha. En mobile deja lugar
    para la navegación inferior y convive con el botón de volver arriba.
  - Al abrir: tarjeta de ~380px anclada abajo a la derecha en desktop, y
    bottom sheet a lo ancho en mobile.
  - El estado vive solo en memoria del componente: no se persiste nada.
*/

// Delay fijo para simular que el asistente "escribe" (determinístico, 300-500ms).
const RETRASO_RESPUESTA_MS = 400;

function crearMensajeBienvenida(): MensajeAsistente {
  return {
    id: "mensaje-bienvenida",
    autor: "asistente",
    texto: RESPUESTA_BIENVENIDA.texto,
    enlaces: RESPUESTA_BIENVENIDA.enlaces,
    opcionesRapidas: RESPUESTA_BIENVENIDA.opcionesRapidas,
  };
}

export function AsistenteWidget() {
  const [abierto, setAbierto] = useState(false);
  const [mensajes, setMensajes] = useState<MensajeAsistente[]>(() => [
    crearMensajeBienvenida(),
  ]);
  const [escribiendo, setEscribiendo] = useState(false);
  const contadorMensajes = useRef(0);
  const temporizadorRespuesta = useRef<ReturnType<typeof setTimeout> | null>(
    null
  );
  const botonLauncherRef = useRef<HTMLButtonElement | null>(null);
  const panelRef = useRef<HTMLDivElement | null>(null);
  const debeDevolverFoco = useRef(false);
  const rutaActual = usePathname();

  /*
    Limpieza del delay simulado si el componente se desmonta
    mientras el asistente "escribe".
  */
  useEffect(() => {
    return () => {
      if (temporizadorRespuesta.current) {
        clearTimeout(temporizadorRespuesta.current);
      }
    };
  }, []);

  useEffect(() => {
    function abrirDesdeLaPagina() {
      setAbierto(true);
    }

    window.addEventListener(
      "donde-entreno:abrir-asistente",
      abrirDesdeLaPagina
    );

    return () => {
      window.removeEventListener(
        "donde-entreno:abrir-asistente",
        abrirDesdeLaPagina
      );
    };
  }, []);

  /*
    Al cerrar el panel, devolvemos el foco a la burbuja
    para no dejar colgado al usuario de teclado.
  */
  useEffect(() => {
    if (!abierto && debeDevolverFoco.current) {
      debeDevolverFoco.current = false;
      botonLauncherRef.current?.focus();
    }
  }, [abierto]);

  function crearIdMensaje(autor: MensajeAsistente["autor"]): string {
    contadorMensajes.current += 1;

    return `mensaje-${autor}-${contadorMensajes.current}`;
  }

  function cerrar() {
    debeDevolverFoco.current = true;
    setAbierto(false);
  }

  function manejarTeclaPanel(evento: React.KeyboardEvent<HTMLDivElement>) {
    if (evento.key === "Escape") {
      evento.stopPropagation();
      cerrar();
      return;
    }

    /*
      Contención de foco: mientras el panel está abierto (sobre todo en
      mobile, donde es un bottom sheet que tapa la página), el Tab no debe
      escapar hacia contenido oculto detrás del sheet. Ciclamos el foco
      entre el primer y el último elemento enfocable del panel.
    */
    if (evento.key !== "Tab") {
      return;
    }

    const panel = panelRef.current;

    if (!panel) {
      return;
    }

    const enfocables = Array.from(
      panel.querySelectorAll<HTMLElement>(
        'button:not([disabled]), [href], input:not([disabled]), textarea:not([disabled]), select:not([disabled]), [tabindex]:not([tabindex="-1"])'
      )
    );

    if (enfocables.length === 0) {
      return;
    }

    const primero = enfocables[0];
    const ultimo = enfocables[enfocables.length - 1];
    const activo = document.activeElement;

    if (evento.shiftKey && activo === primero) {
      evento.preventDefault();
      ultimo.focus();
    } else if (!evento.shiftKey && activo === ultimo) {
      evento.preventDefault();
      primero.focus();
    }
  }

  function enviarMensaje(texto: string) {
    const textoLimpio = texto.trim();

    if (!textoLimpio || escribiendo) {
      return;
    }

    setMensajes((mensajesPrevios) => [
      ...mensajesPrevios,
      { id: crearIdMensaje("usuario"), autor: "usuario", texto: textoLimpio },
    ]);
    setEscribiendo(true);

    /*
      Pequeño delay simulado antes de responder, con indicador de "escribiendo",
      para que la conversación se sienta viva. La respuesta en sí es
      determinística: sale del motor local.
    */
    temporizadorRespuesta.current = setTimeout(() => {
      motorAsistenteLocal
        .procesar(textoLimpio, { rutaActual })
        .then((respuesta) => {
          setMensajes((mensajesPrevios) => [
            ...mensajesPrevios,
            {
              id: crearIdMensaje("asistente"),
              autor: "asistente",
              texto: respuesta.texto,
              enlaces: respuesta.enlaces,
              opcionesRapidas: respuesta.opcionesRapidas,
            },
          ]);
        })
        .catch(() => {
          setMensajes((mensajesPrevios) => [
            ...mensajesPrevios,
            {
              id: crearIdMensaje("asistente"),
              autor: "asistente",
              texto:
                "Uy, algo salió mal de mi lado. ¿Probás de nuevo con otras palabras?",
            },
          ]);
        })
        .finally(() => {
          setEscribiendo(false);
        });
    }, RETRASO_RESPUESTA_MS);
  }

  if (!abierto) {
    return (
      <button
        ref={botonLauncherRef}
        type="button"
        onClick={() => setAbierto(true)}
        aria-label="Abrir asistente de DondeEntreno"
        aria-haspopup="dialog"
        className="fixed bottom-[calc(5.75rem+env(safe-area-inset-bottom))] right-4 z-[60] flex h-14 w-14 items-center justify-center rounded-full bg-[#0F3D5E] text-white shadow-[0_12px_30px_rgba(0,47,73,0.28)] ring-4 ring-[#4FB3D9]/20 transition duration-200 ease-out hover:-translate-y-1 hover:scale-105 hover:bg-[#0B314D] active:scale-95 md:bottom-20 md:right-5"
      >
        <svg
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          strokeWidth="2"
          strokeLinecap="round"
          strokeLinejoin="round"
          className="h-6 w-6"
          aria-hidden="true"
        >
          <path d="M21 11.5a8.38 8.38 0 0 1-.9 3.8 8.5 8.5 0 0 1-7.6 4.7 8.38 8.38 0 0 1-3.8-.9L3 21l1.9-5.7a8.38 8.38 0 0 1-.9-3.8 8.5 8.5 0 0 1 4.7-7.6 8.38 8.38 0 0 1 3.8-.9h.5a8.48 8.48 0 0 1 8 8v.5z" />
          <circle cx="8.5" cy="11.5" r="0.5" fill="currentColor" />
          <circle cx="12.5" cy="11.5" r="0.5" fill="currentColor" />
          <circle cx="16.5" cy="11.5" r="0.5" fill="currentColor" />
        </svg>
      </button>
    );
  }

  return (
    <div
      ref={panelRef}
      role="dialog"
      aria-modal="true"
      aria-label="Asistente DondeEntreno"
      onKeyDown={manejarTeclaPanel}
      className="fixed inset-x-0 bottom-0 z-[60] flex h-[min(34rem,85dvh)] w-full animate-[de-entrada_0.2s_ease-out] flex-col overflow-hidden rounded-t-[var(--radius-xl)] border border-[#DDEAF3] bg-white shadow-[0_24px_60px_rgba(12,52,80,0.28)] sm:inset-x-auto sm:bottom-5 sm:right-5 sm:h-[min(34rem,calc(100dvh-6rem))] sm:w-[380px] sm:rounded-[var(--radius-xl)]"
    >
      <div className="flex items-center justify-between gap-3 bg-[#0F3D5E] px-5 py-4">
        <div className="min-w-0">
          <p className="font-display text-base font-bold leading-tight text-white">
            Asistente DondeEntreno
          </p>
          <p className="mt-1 flex items-center gap-1.5 text-xs font-medium leading-4 text-[#BFDDEA]">
            <span
              aria-hidden="true"
              className="h-2 w-2 shrink-0 rounded-full bg-[#2EB872]"
            />
            Te ayudo a encontrar dónde entrenar
          </p>
        </div>
        <button
          type="button"
          onClick={cerrar}
          aria-label="Cerrar asistente"
          className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full text-white transition duration-200 ease-out hover:bg-white/15 active:scale-95"
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
            <path d="M18 6 6 18" />
            <path d="m6 6 12 12" />
          </svg>
        </button>
      </div>

      <AsistenteConversacion
        mensajes={mensajes}
        escribiendo={escribiendo}
        onEnviarMensaje={enviarMensaje}
      />
    </div>
  );
}
