"use client";

import { useState } from "react";

import { SurfaceCard } from "../ui/SurfaceCard";

/*
  La entrada guiada a Dondi (Fase 10).

  NO es un motor de recomendación nuevo. Es una puerta: tres preguntas
  con botones que arman la primera consulta y se la pasan al asistente
  que ya existe.

  La razón es deliberada — un "test de match deportivo" aparte sería un
  segundo motor que recomienda deportes, con sus propias reglas para
  entender "no quiero pelea", y dos cosas que mantener sincronizadas.
  Acá la inteligencia sigue siendo una sola.
*/

type Opcion = {
  /** Lo que ve la persona. */
  etiqueta: string;
  /** Lo que se le dice a Dondi. Va en primera persona: es su consulta. */
  frase: string;
};

type Pregunta = {
  id: string;
  titulo: string;
  ayuda?: string;
  opciones: Opcion[];
};

const PREGUNTAS: Pregunta[] = [
  {
    id: "motivo",
    titulo: "¿Qué te gustaría lograr?",
    opciones: [
      { etiqueta: "Moverme y estar mejor", frase: "quiero moverme y sentirme mejor" },
      { etiqueta: "Bajar un cambio", frase: "busco algo tranquilo para despejarme" },
      { etiqueta: "Superarme y competir", frase: "quiero exigirme y progresar" },
      { etiqueta: "Conocer gente", frase: "quiero algo grupal para conocer gente" },
    ],
  },
  {
    id: "compania",
    titulo: "¿Cómo te sentís más cómodo?",
    opciones: [
      { etiqueta: "En grupo", frase: "prefiero entrenar en grupo" },
      { etiqueta: "A mi ritmo", frase: "prefiero entrenar a mi ritmo" },
      { etiqueta: "Me da igual", frase: "" },
    ],
  },
  {
    id: "descarte",
    titulo: "¿Hay algo que prefieras evitar?",
    ayuda: "Dondi lo tiene en cuenta y no te lo propone.",
    opciones: [
      { etiqueta: "Nada de contacto ni pelea", frase: "nada de deportes de contacto ni de pelea" },
      { etiqueta: "Nada de pileta", frase: "nada de natación ni actividades en pileta" },
      { etiqueta: "Nada de eso", frase: "" },
    ],
  },
];

export function EntradaGuiada() {
  const [elegidas, setElegidas] = useState<Record<string, Opcion>>({});

  const respondidas = PREGUNTAS.filter((pregunta) => elegidas[pregunta.id]);
  const listo = respondidas.length === PREGUNTAS.length;

  /*
    La consulta se arma como la escribiría una persona, no como una
    lista de filtros: el motor del asistente entiende lenguaje natural,
    y "prefiero entrenar en grupo" le dice más que "grupal=true".

    Las opciones sin frase ("me da igual") no suman texto: mandar "me da
    igual" solo agrega ruido a lo que Dondi tiene que interpretar.
  */
  function armarConsulta() {
    const partes = PREGUNTAS.map((pregunta) => elegidas[pregunta.id]?.frase)
      .filter((frase): frase is string => Boolean(frase));

    return `Nunca entrené o hace mucho que no. ${partes.join(", ")}. ¿Qué deporte me recomendás?`;
  }

  function abrirDondi(consulta?: string) {
    window.dispatchEvent(
      new CustomEvent("donde-entreno:abrir-asistente", {
        detail: consulta ? { consulta } : undefined,
      })
    );
  }

  return (
    <SurfaceCard as="section" className="p-5 sm:p-6">
      <h2 className="text-lg font-extrabold text-[var(--color-primary)]">
        No sabés por dónde empezar
      </h2>
      <p className="mt-1 text-sm text-[var(--color-muted)]">
        Respondé tres cosas y se las paso a Dondi, que conoce las actividades
        que hay de verdad en tu ciudad.
      </p>

      <div className="mt-5 grid gap-5">
        {PREGUNTAS.map((pregunta) => (
          <fieldset key={pregunta.id}>
            <legend className="text-sm font-bold text-[var(--color-text)]">
              {pregunta.titulo}
            </legend>

            {pregunta.ayuda ? (
              <p className="mt-0.5 text-xs text-[var(--color-muted)]">
                {pregunta.ayuda}
              </p>
            ) : null}

            <div className="mt-2 flex flex-wrap gap-2">
              {pregunta.opciones.map((opcion) => {
                const elegida = elegidas[pregunta.id]?.etiqueta === opcion.etiqueta;

                return (
                  <button
                    key={opcion.etiqueta}
                    type="button"
                    aria-pressed={elegida}
                    onClick={() =>
                      setElegidas((previas) => ({
                        ...previas,
                        [pregunta.id]: opcion,
                      }))
                    }
                    className={`rounded-full border px-3 py-1.5 text-sm transition ${
                      elegida
                        ? "border-transparent bg-brand text-white"
                        : "border-[var(--color-border)] text-[var(--color-text)] hover:border-[var(--color-primary)]"
                    }`}
                  >
                    {opcion.etiqueta}
                  </button>
                );
              })}
            </div>
          </fieldset>
        ))}
      </div>

      <div className="mt-6 flex flex-col gap-2 sm:flex-row sm:items-center">
        <button
          type="button"
          disabled={!listo}
          onClick={() => abrirDondi(armarConsulta())}
          className="rounded-full bg-brand px-5 py-2.5 text-sm font-bold text-white transition disabled:cursor-not-allowed disabled:opacity-50"
        >
          {listo
            ? "Ver qué me recomienda"
            : `Elegí las ${PREGUNTAS.length} respuestas`}
        </button>

        <button
          type="button"
          onClick={() => abrirDondi()}
          className="text-sm font-semibold text-[var(--color-primary)] underline-offset-4 hover:underline"
        >
          Prefiero escribirle yo
        </button>
      </div>
    </SurfaceCard>
  );
}
