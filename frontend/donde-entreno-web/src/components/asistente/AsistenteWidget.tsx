"use client";

import { useEffect, useRef, useState } from "react";
import { usePathname } from "next/navigation";
import { RESPUESTA_BIENVENIDA } from "../../lib/asistente/conocimiento";
import { motorAsistenteCascada } from "../../lib/asistente/motorCascada";
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
  const origenDelFoco = useRef<HTMLElement | null>(null);
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
      /*
        Guardamos quién abrió el asistente para devolverle el foco al
        cerrar: ahora el disparador puede ser la barra inferior o el
        botón de la home, así que no hay un único elemento fijo.
      */
      if (document.activeElement instanceof HTMLElement) {
        origenDelFoco.current = document.activeElement;
      }

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
    Al cerrar el panel devolvemos el foco a quien lo abrió, para no dejar
    colgado al usuario de teclado.
  */
  useEffect(() => {
    if (!abierto && debeDevolverFoco.current) {
      debeDevolverFoco.current = false;
      const destinoFoco =
        origenDelFoco.current ??
        document.getElementById("asistente-home-trigger");
      destinoFoco?.focus();
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
      Pequeño delay simulado antes de responder, con indicador de
      "escribiendo", para que la conversación se sienta viva. Las que
      resuelve el motor local salen igual de rápido que antes; las que
      van al backend tardan lo que tarde, con el mismo indicador.
    */
    temporizadorRespuesta.current = setTimeout(() => {
      motorAsistenteCascada
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

  /*
    Sin burbuja flotante: el asistente se abre desde la barra inferior y
    desde el botón de la home. Una burbuja fija en la esquina se
    superponía con las barras de acción de las páginas (ya tapó una vez
    el CTA de WhatsApp del detalle) y se llevaba un lugar de la pantalla
    en todas las vistas.
  */
  if (!abierto) {
    return null;
  }

  return (
    <div
      ref={panelRef}
      role="dialog"
      aria-modal="true"
      aria-label="Asistente DondeEntreno"
      onKeyDown={manejarTeclaPanel}
      className="fixed inset-x-0 bottom-0 z-[60] flex h-[min(34rem,85dvh)] w-full animate-[de-entrada_0.2s_ease-out] flex-col overflow-hidden rounded-t-[var(--radius-xl)] border border-[#DDEAF3] bg-white shadow-[0_24px_60px_rgba(12,52,80,0.28)] lg:inset-x-auto lg:bottom-5 lg:right-5 lg:h-[min(34rem,calc(100dvh-6rem))] lg:w-[380px] lg:rounded-[var(--radius-xl)]"
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
