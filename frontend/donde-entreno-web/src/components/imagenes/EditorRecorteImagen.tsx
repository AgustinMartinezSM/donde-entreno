"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import type { PointerEvent as ReactPointerEvent } from "react";

import { AppButton } from "../ui/AppButton";
import {
  ENCUADRE_INICIAL,
  MEDIDAS_DESTINO,
  ZOOM_MAXIMO,
  limitar,
  recortarImagen,
  type EncuadreRecorte,
  type TipoRecorte,
} from "../../lib/recorteImagen";

type EditorRecorteImagenProps = {
  archivo: File;
  /* Object URL del archivo: lo administra quien abre el editor. */
  url: string;
  tipo: TipoRecorte;
  onConfirmar: (recortada: File) => void;
  onCancelar: () => void;
};

type Medidas = { ancho: number; alto: number };

/*
  Editor de encuadre: el publicador elige qué parte de su foto se ve.

  El marco tiene la proporción real de destino, así que lo que se ve acá
  es exactamente lo que se va a publicar. La cuenta que posiciona la
  imagen en pantalla es la misma que después recorta el canvas, solo que
  en píxeles de pantalla en vez de píxeles de la imagen original.

  Se sube la imagen ya recortada: entra más liviana y todas las del
  mismo tipo quedan con la misma proporción.
*/
export function EditorRecorteImagen({
  archivo,
  url,
  tipo,
  onConfirmar,
  onCancelar,
}: EditorRecorteImagenProps) {
  const destino = MEDIDAS_DESTINO[tipo];

  /*
    Las dos medidas viven en estado, no en refs: la posición de la
    imagen se calcula en el render y leer un ref ahí no es confiable.
  */
  const [medidasOriginal, setMedidasOriginal] = useState<Medidas | null>(null);
  const [medidasMarco, setMedidasMarco] = useState<Medidas | null>(null);
  const [encuadre, setEncuadre] = useState<EncuadreRecorte>(ENCUADRE_INICIAL);
  const [procesando, setProcesando] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const marcoRef = useRef<HTMLDivElement | null>(null);
  const arrastre = useRef<{ x: number; y: number } | null>(null);

  /* Callback ref: mide al montar, sin efecto de por medio. */
  const medirMarco = useCallback((nodo: HTMLDivElement | null) => {
    marcoRef.current = nodo;

    if (nodo) {
      setMedidasMarco({ ancho: nodo.clientWidth, alto: nodo.clientHeight });
    }
  }, []);

  useEffect(() => {
    function remedir() {
      const nodo = marcoRef.current;

      if (nodo) {
        setMedidasMarco({ ancho: nodo.clientWidth, alto: nodo.clientHeight });
      }
    }

    window.addEventListener("resize", remedir);

    return () => window.removeEventListener("resize", remedir);
  }, []);

  /*
    Escala y sobrantes, con la misma cuenta que hace el recorte final:
    escala mínima que cubre el marco, por el zoom.
  */
  function calcularGeometria() {
    if (!medidasMarco || !medidasOriginal) {
      return null;
    }

    const escala =
      Math.max(
        medidasMarco.ancho / medidasOriginal.ancho,
        medidasMarco.alto / medidasOriginal.alto
      ) * encuadre.zoom;

    const anchoImagen = medidasOriginal.ancho * escala;
    const altoImagen = medidasOriginal.alto * escala;

    return {
      anchoImagen,
      altoImagen,
      sobranteX: Math.max(0, anchoImagen - medidasMarco.ancho),
      sobranteY: Math.max(0, altoImagen - medidasMarco.alto),
    };
  }

  const geometria = calcularGeometria();

  function moverEncuadre(deltaX: number, deltaY: number) {
    if (!geometria) {
      return;
    }

    const { sobranteX, sobranteY } = geometria;

    setEncuadre((previo) => ({
      ...previo,
      /* Sin sobrante no hay hacia dónde mover: el eje queda fijo. */
      desplazamientoX: sobranteX
        ? limitar(previo.desplazamientoX - deltaX / sobranteX, -0.5, 0.5)
        : 0,
      desplazamientoY: sobranteY
        ? limitar(previo.desplazamientoY - deltaY / sobranteY, -0.5, 0.5)
        : 0,
    }));
  }

  function alPresionar(evento: ReactPointerEvent<HTMLDivElement>) {
    evento.currentTarget.setPointerCapture(evento.pointerId);
    arrastre.current = { x: evento.clientX, y: evento.clientY };
  }

  function alMover(evento: ReactPointerEvent<HTMLDivElement>) {
    if (!arrastre.current) {
      return;
    }

    const deltaX = evento.clientX - arrastre.current.x;
    const deltaY = evento.clientY - arrastre.current.y;
    arrastre.current = { x: evento.clientX, y: evento.clientY };

    moverEncuadre(deltaX, deltaY);
  }

  function alSoltar(evento: ReactPointerEvent<HTMLDivElement>) {
    evento.currentTarget.releasePointerCapture(evento.pointerId);
    arrastre.current = null;
  }

  function alPresionarTecla(evento: React.KeyboardEvent<HTMLDivElement>) {
    const paso = evento.shiftKey ? 40 : 12;
    const movimientos: Record<string, [number, number]> = {
      ArrowLeft: [paso, 0],
      ArrowRight: [-paso, 0],
      ArrowUp: [0, paso],
      ArrowDown: [0, -paso],
    };

    const movimiento = movimientos[evento.key];

    if (movimiento) {
      evento.preventDefault();
      moverEncuadre(movimiento[0], movimiento[1]);
    }
  }

  async function confirmar() {
    if (procesando) {
      return;
    }

    setProcesando(true);
    setError(null);

    try {
      onConfirmar(await recortarImagen(archivo, tipo, encuadre));
    } catch {
      setError("No pudimos recortar la imagen. Probá con otra.");
      setProcesando(false);
    }
  }

  return (
    <div className="rounded-[18px] border border-[var(--color-border-soft)] bg-[var(--color-bg)] p-4">
      <div className="flex flex-wrap items-baseline justify-between gap-2">
        <p className="text-sm font-extrabold text-[var(--color-primary)]">
          Elegí qué parte se ve
        </p>
        <p className="text-xs font-semibold text-[var(--color-muted)]">
          {destino.recomendacion}
        </p>
      </div>

      <div
        ref={medirMarco}
        role="application"
        tabIndex={0}
        aria-label="Encuadre de la imagen. Arrastrá para mover, o usá las flechas del teclado."
        onPointerDown={alPresionar}
        onPointerMove={alMover}
        onPointerUp={alSoltar}
        onPointerCancel={alSoltar}
        onKeyDown={alPresionarTecla}
        style={{ aspectRatio: `${destino.ancho} / ${destino.alto}` }}
        className="relative mt-3 w-full cursor-grab touch-none overflow-hidden rounded-[12px] bg-[var(--color-info-soft)] active:cursor-grabbing focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-[#4FB3D9]/30"
      >
        {/* Preview local (object URL, no next/image). */}
        {/* eslint-disable-next-line @next/next/no-img-element */}
        <img
          src={url}
          alt=""
          draggable={false}
          /* onLoad es un event handler: acá sí se puede guardar estado. */
          onLoad={(evento) =>
            setMedidasOriginal({
              ancho: evento.currentTarget.naturalWidth,
              alto: evento.currentTarget.naturalHeight,
            })
          }
          onError={() => setError("No pudimos leer esta imagen.")}
          style={
            geometria
              ? {
                  width: `${geometria.anchoImagen}px`,
                  height: `${geometria.altoImagen}px`,
                  left: `${-geometria.sobranteX * (0.5 + encuadre.desplazamientoX)}px`,
                  top: `${-geometria.sobranteY * (0.5 + encuadre.desplazamientoY)}px`,
                }
              : { visibility: "hidden" }
          }
          className="pointer-events-none absolute max-w-none select-none"
        />
      </div>

      <label className="mt-3 flex items-center gap-3">
        <span className="text-xs font-bold text-[var(--color-primary)]">
          Zoom
        </span>
        <input
          type="range"
          min={1}
          max={ZOOM_MAXIMO}
          step={0.05}
          value={encuadre.zoom}
          onChange={(evento) =>
            setEncuadre((previo) => ({
              ...previo,
              zoom: Number(evento.target.value),
            }))
          }
          className="h-1.5 flex-1 accent-[var(--color-primary)]"
        />
      </label>

      {error ? (
        <p role="alert" className="mt-3 text-sm font-semibold text-red-700">
          {error}
        </p>
      ) : null}

      <div className="mt-4 flex flex-wrap gap-2">
        <AppButton type="button" onClick={confirmar} disabled={procesando}>
          {procesando ? "Recortando..." : "Usar este encuadre"}
        </AppButton>
        <AppButton
          type="button"
          variant="secondary"
          onClick={onCancelar}
          disabled={procesando}
        >
          Cancelar
        </AppButton>
        <AppButton
          type="button"
          variant="outline"
          onClick={() => setEncuadre(ENCUADRE_INICIAL)}
          disabled={procesando}
        >
          Centrar
        </AppButton>
      </div>
    </div>
  );
}
