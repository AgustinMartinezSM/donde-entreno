"use client";

import { useEffect, useRef, useState } from "react";
import { usePathname } from "next/navigation";
import { RESPUESTA_BIENVENIDA } from "../../lib/asistente/conocimiento";
import { motorAsistenteCascada } from "../../lib/asistente/motorCascada";
import type {
  MensajeAsistente,
  MensajeHistorial,
} from "../../lib/asistente/tipos";
import { AsistenteConversacion } from "./AsistenteConversacion";

/*
  Widget del Asistente DondeEntreno.

  - Se abre desde la barra inferior y desde el botón de la home; no hay
    burbuja flotante.
  - En desktop es una tarjeta de ~380px abajo a la derecha; en mobile, un
    bottom sheet a lo ancho.
  - El estado vive solo en memoria del componente: la conversación no se
    persiste en ningún lado y se pierde al cerrar la pestaña. Es a
    propósito, porque parte de lo que se escribe puede salir hacia un
    modelo externo.
*/

const ID_BIENVENIDA = "mensaje-bienvenida";

/*
  Cuántos turnos previos viajan al backend.

  Diez son cinco idas y vueltas: alcanza para que el asistente se acuerde
  de lo que rechazaste y preferiste, sin mandar la charla entera. El
  backend igual la vuelve a recortar por su cuenta.
*/
const MAX_TURNOS_ENVIADOS = 10;

/*
  Piso de tiempo antes de mostrar la respuesta, no espera fija.

  V1 sumaba 400 ms a TODA respuesta, incluidas las que iban al backend:
  eran 400 ms regalados encima de la red. Ahora las locales esperan este
  mínimo para que no aparezcan de golpe, y las remotas tardan lo que
  tardan, sin agregado.
*/
const PISO_RESPUESTA_MS = 260;

/*
  A partir de acá la consulta claramente salió a la red, así que se cambia
  el cartel: decir "buscando actividades" desde el milisegundo cero sería
  mentira en las que se resuelven en el navegador.
*/
const MS_HASTA_AVISAR_BUSQUEDA = 1200;

function crearMensajeBienvenida(): MensajeAsistente {
  return {
    id: ID_BIENVENIDA,
    autor: "asistente",
    texto: RESPUESTA_BIENVENIDA.texto,
    enlaces: RESPUESTA_BIENVENIDA.enlaces,
    opcionesRapidas: RESPUESTA_BIENVENIDA.opcionesRapidas,
  };
}

function esperar(milisegundos: number): Promise<void> {
  return new Promise((resolver) => setTimeout(resolver, milisegundos));
}

export function AsistenteWidget() {
  const [abierto, setAbierto] = useState(false);
  const [mensajes, setMensajes] = useState<MensajeAsistente[]>(() => [
    crearMensajeBienvenida(),
  ]);
  const [escribiendo, setEscribiendo] = useState(false);
  const [avisoDeEspera, setAvisoDeEspera] = useState("Pensando opciones…");
  const contadorMensajes = useRef(0);
  const temporizadorAviso = useRef<ReturnType<typeof setTimeout> | null>(null);
  const origenDelFoco = useRef<HTMLElement | null>(null);
  const panelRef = useRef<HTMLDivElement | null>(null);
  const debeDevolverFoco = useRef(false);
  const rutaActual = usePathname();

  useEffect(() => {
    return () => {
      if (temporizadorAviso.current) {
        clearTimeout(temporizadorAviso.current);
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

  /*
    Los últimos turnos, sin el saludo inicial.

    El mensaje de bienvenida no aporta nada al contexto y ocuparía lugar
    en el prompt: es el mismo texto siempre.
  */
  function armarHistorial(): MensajeHistorial[] {
    return mensajes
      .filter((mensaje) => mensaje.id !== ID_BIENVENIDA)
      .slice(-MAX_TURNOS_ENVIADOS)
      .map((mensaje) => ({ autor: mensaje.autor, texto: mensaje.texto }));
  }

  async function enviarMensaje(texto: string) {
    const textoLimpio = texto.trim();

    if (!textoLimpio || escribiendo) {
      return;
    }

    const historial = armarHistorial();

    setMensajes((mensajesPrevios) => [
      ...mensajesPrevios,
      { id: crearIdMensaje("usuario"), autor: "usuario", texto: textoLimpio },
    ]);
    setEscribiendo(true);
    setAvisoDeEspera("Pensando opciones…");

    temporizadorAviso.current = setTimeout(() => {
      setAvisoDeEspera("Buscando actividades reales…");
    }, MS_HASTA_AVISAR_BUSQUEDA);

    try {
      const [respuesta] = await Promise.all([
        motorAsistenteCascada.procesar(textoLimpio, { rutaActual, historial }),
        esperar(PISO_RESPUESTA_MS),
      ]);

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
    } catch {
      /*
        La cascada ya se traga los errores de red y cae al motor local,
        así que llegar acá es raro. Aun así la salida no puede ser un
        callejón: se ofrece Explorar.
      */
      setMensajes((mensajesPrevios) => [
        ...mensajesPrevios,
        {
          id: crearIdMensaje("asistente"),
          autor: "asistente",
          texto:
            "No pude responder ahora mismo. Probá de nuevo en un momento, o mirá las actividades directamente desde acá.",
          enlaces: [{ href: "/explorar", etiqueta: "Ir a Explorar" }],
        },
      ]);
    } finally {
      if (temporizadorAviso.current) {
        clearTimeout(temporizadorAviso.current);
        temporizadorAviso.current = null;
      }

      setEscribiendo(false);
    }
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
        avisoDeEspera={avisoDeEspera}
        onEnviarMensaje={enviarMensaje}
      />
    </div>
  );
}
