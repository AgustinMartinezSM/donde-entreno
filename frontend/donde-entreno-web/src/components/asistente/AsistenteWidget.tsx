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
import { DondiAvatar } from "./DondiAvatar";

/*
  Dondi, el asistente de DondeEntreno.

  - Un launcher circular flotante lo deja disponible en toda la app, en
    lugar de tarjetas que ocupaban media pantalla de la home: el
    asistente es una herramienta al alcance, no una sección que compite
    con el contenido.
  - En desktop es una tarjeta abajo a la derecha; en mobile, un bottom
    sheet con velo.
  - Minimizar (—) y cerrar (X) hacen cosas distintas a propósito:
    minimizar guarda la charla para volver a ella —el caso real es leer
    una recomendación, ir a mirarla y seguir preguntando— y cerrar la da
    por terminada. Dos botones que hicieran lo mismo serían ruido.
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
  const launcherRef = useRef<HTMLButtonElement | null>(null);
  const debeDevolverFoco = useRef(false);
  const rutaActual = usePathname();

  /* Hay charla cuando la persona ya escribió: el saludo solo no cuenta. */
  const hayConversacion = mensajes.length > 1;

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
      const origen = origenDelFoco.current;
      /*
        Si quien abrió el panel ya no está en pantalla (se navegó a otra
        ruta con el chat abierto), el foco va al launcher, que existe
        siempre.
      */
      const destinoFoco =
        origen?.isConnected === true ? origen : launcherRef.current;
      destinoFoco?.focus();
    }
  }, [abierto]);

  function crearIdMensaje(autor: MensajeAsistente["autor"]): string {
    contadorMensajes.current += 1;

    return `mensaje-${autor}-${contadorMensajes.current}`;
  }

  /* Guarda la charla: se vuelve a ella tal como quedó. */
  function minimizar() {
    debeDevolverFoco.current = true;
    setAbierto(false);
  }

  /* Da la charla por terminada: la próxima vez arranca del saludo. */
  function cerrar() {
    debeDevolverFoco.current = true;
    setAbierto(false);

    if (temporizadorAviso.current) {
      clearTimeout(temporizadorAviso.current);
      temporizadorAviso.current = null;
    }

    setMensajes([crearMensajeBienvenida()]);
    setEscribiendo(false);
    contadorMensajes.current = 0;
  }

  function manejarTeclaPanel(evento: React.KeyboardEvent<HTMLDivElement>) {
    if (evento.key === "Escape") {
      evento.stopPropagation();
      /* Escape minimiza: nadie espera perder la conversación con Escape. */
      minimizar();
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
    Cerrado, el asistente no desaparece: queda el launcher. Antes no
    había ninguno y el acceso dependía de la barra inferior o de un
    bloque de la home, así que en el resto de las pantallas el asistente
    directamente no existía.

    La excepción son las pantallas de acceso: ahí el launcher se
    superponía al botón de enviar del formulario —medido en /login a
    390×844: el botón va de x41 a x350 y el launcher de x16 a x72, y
    `elementFromPoint` sobre esa franja devolvía el launcher, o sea que
    tocar el borde izquierdo de "Ingresar" abría a Dondi en lugar de
    iniciar sesión—. Además Dondi no tiene nada que aportar mientras
    alguien escribe su contraseña. El panel sigue siendo accesible desde
    la barra inferior, así que no se pierde el acceso: se saca el botón
    flotante, no el asistente.
  */
  if (!abierto) {
    if (esRutaDeAcceso(rutaActual)) {
      return null;
    }

    return (
      <DondiLauncher
        ref={launcherRef}
        hayConversacion={hayConversacion}
        onAbrir={() => {
          origenDelFoco.current = launcherRef.current;
          setAbierto(true);
        }}
      />
    );
  }

  return (
    <>
      {/*
        Velo solo en mobile, donde el sheet tapa la página: sin él, el
        contenido de atrás se sigue leyendo como si fuera tocable. Cierra
        minimizando, que es lo que espera quien toca fuera de un panel.
      */}
      <div
        aria-hidden="true"
        onClick={minimizar}
        className="fixed inset-0 z-[55] bg-[#0B314D]/40 backdrop-blur-[2px] lg:hidden"
      />

      <div
        ref={panelRef}
        role="dialog"
        aria-modal="true"
        aria-label="Dondi, asistente de DondeEntreno"
        onKeyDown={manejarTeclaPanel}
        className="fixed inset-x-0 bottom-0 z-[60] flex h-[min(36rem,88dvh)] w-full animate-[de-sheet_0.28s_ease-out] flex-col overflow-hidden rounded-t-[28px] border border-[var(--color-border-soft)] bg-white shadow-[0_-18px_50px_rgba(12,52,80,0.28)] lg:inset-x-auto lg:bottom-5 lg:right-5 lg:h-[min(34rem,calc(100dvh-6rem))] lg:w-[400px] lg:animate-[de-entrada_0.2s_ease-out] lg:rounded-[28px]"
      >
        {/* Agarradera: la señal de que esto es un panel que se baja. */}
        <div aria-hidden="true" className="flex justify-center pt-2.5 lg:hidden">
          <span className="h-1.5 w-11 rounded-full bg-[#CBD9E5]" />
        </div>

        <div className="gradient-deep flex items-center gap-3 px-4 py-3.5 sm:px-5">
          <DondiAvatar tamanio={40} conEstado />

          <div className="min-w-0 flex-1">
            <p className="font-display text-base font-bold leading-tight text-white">
              Dondi
            </p>
            <p className="mt-0.5 truncate text-xs font-medium leading-4 text-[#BFDDEA]">
              Tu asistente deportivo de DondeEntreno
            </p>
          </div>

          <button
            type="button"
            onClick={minimizar}
            aria-label="Minimizar el chat y seguir navegando"
            title="Minimizar"
            className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full text-white transition duration-200 ease-out hover:bg-white/15 focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-white/40 active:scale-95"
          >
            <svg
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth="2.5"
              strokeLinecap="round"
              className="h-5 w-5"
              aria-hidden="true"
            >
              <path d="M6 12h12" />
            </svg>
          </button>

          <button
            type="button"
            onClick={cerrar}
            aria-label="Cerrar el chat y terminar la conversación"
            title="Cerrar"
            className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full text-white transition duration-200 ease-out hover:bg-white/15 focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-white/40 active:scale-95"
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
    </>
  );
}

/*
  Pantallas donde la persona está completando credenciales: ingresar,
  elegir tipo de cuenta, los dos registros y el acceso de administración.
  Se compara por prefijo para cubrir las rutas hijas de /registro.
*/
const RUTAS_DE_ACCESO = ["/login", "/registro", "/admin/login"];

function esRutaDeAcceso(ruta: string | null) {
  if (!ruta) {
    return false;
  }

  return RUTAS_DE_ACCESO.some(
    (base) => ruta === base || ruta.startsWith(`${base}/`)
  );
}

type DondiLauncherProps = {
  hayConversacion: boolean;
  onAbrir: () => void;
  ref?: React.Ref<HTMLButtonElement>;
};

/*
  Launcher flotante de Dondi.

  Va a la IZQUIERDA en mobile a propósito: la esquina derecha ya la ocupa
  el botón de volver arriba, y dos botones flotantes en el mismo lugar se
  pisan. En desktop pasa a la derecha, donde no hay competencia y es el
  lugar canónico de un chat.

  El bottom sale de la altura de la barra inferior más la safe area, así
  no queda tapado ni flotando sobre los íconos.
*/
function DondiLauncher({ hayConversacion, onAbrir, ref }: DondiLauncherProps) {
  return (
    <button
      ref={ref}
      type="button"
      onClick={onAbrir}
      aria-haspopup="dialog"
      aria-label={
        hayConversacion
          ? "Abrir el chat con Dondi. Tenés una conversación en curso"
          : "Abrir el chat con Dondi, tu asistente deportivo"
      }
      className="group fixed left-4 z-50 flex h-14 w-14 items-center justify-center rounded-full shadow-lifted transition duration-200 ease-out hover:-translate-y-0.5 focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-[#4FB3D9]/50 active:scale-95 lg:bottom-5 lg:left-auto lg:right-5"
      style={{ bottom: "calc(5.25rem + env(safe-area-inset-bottom))" }}
    >
      {/*
        El halo late detrás del botón, no en el botón: si escalara el
        avatar, el objetivo táctil se movería bajo el dedo.
      */}
      <span
        aria-hidden="true"
        className="absolute inset-0 rounded-full bg-[var(--color-secondary)]/40 motion-safe:animate-[de-halo_2.8s_ease-in-out_infinite]"
      />

      <DondiAvatar tamanio={56} className="relative" />

      {hayConversacion ? (
        <span
          aria-hidden="true"
          className="absolute -right-0.5 -top-0.5 h-4 w-4 rounded-full border-2 border-white bg-[var(--color-secondary)]"
        />
      ) : null}
    </button>
  );
}
