"use client";

import { AppButton } from "../ui/AppButton";
import { SurfaceCard } from "../ui/SurfaceCard";

type BloqueAsistenteProps = {
  titulo?: string;
  descripcion?: string;
  textoBoton?: string;
  /*
    - "ancha": la card ocupa el ancho del contenido y el botón va al lado.
    - "lateral": la card vive en la columna de apoyo del perfil, que solo
      es angosta a partir de `xl` (304px). Hasta ahí se comporta como la
      ancha; desde ahí apila todo.

    Hace falta decírselo porque los breakpoints miden la ventana y no el
    contenedor: en una pantalla de 1440px la columna lateral mide 304px,
    así que `sm:flex-row` se activaba igual y le dejaba al texto 70px. El
    resultado era el título cayendo en vertical, una letra por línea.
  */
  disposicion?: "ancha" | "lateral";
};

/*
  Acceso al asistente desde el perfil.

  El asistente no es una ruta: vive en un panel global que se abre con un
  evento, el mismo que usa la barra inferior. Por eso acá va un botón y
  no un link.
*/
export function BloqueAsistente({
  titulo = "¿No sabés por dónde empezar?",
  descripcion = "Contame qué buscás y te recomiendo deportes o actividades cerca tuyo.",
  textoBoton = "Hablar con el asistente",
  disposicion = "ancha",
}: BloqueAsistenteProps) {
  const lateral = disposicion === "lateral";

  return (
    <SurfaceCard
      variant="info"
      className={`flex flex-col gap-4 p-5 sm:flex-row sm:items-center sm:justify-between sm:p-6 ${
        lateral ? "xl:flex-col xl:items-stretch xl:gap-0 xl:p-5" : ""
      }`}
    >
      <div className="flex min-w-0 items-start gap-3">
        <span
          aria-hidden="true"
          className="flex h-11 w-11 shrink-0 items-center justify-center rounded-full bg-white text-[var(--color-primary)] shadow-sm"
        >
          <svg
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="2"
            strokeLinecap="round"
            strokeLinejoin="round"
            className="h-5 w-5"
          >
            <path d="M21 11.5a8.5 8.5 0 0 1-12.3 7.6L3 21l1.9-5.7A8.5 8.5 0 1 1 21 11.5z" />
            <path d="M8.5 11.5h.01M12 11.5h.01M15.5 11.5h.01" />
          </svg>
        </span>

        {/*
          min-w-0 en la columna de texto: sin esto el contenido no puede
          encogerse por debajo de su ancho mínimo y empuja el layout en
          lugar de ajustarse.
        */}
        <div className="min-w-0">
          <p className="font-extrabold text-[var(--color-primary)]">{titulo}</p>
          <p className="mt-1 text-sm leading-6 text-[var(--color-muted)]">
            {descripcion}
          </p>
        </div>
      </div>

      <AppButton
        variant="secondary"
        size="sm"
        className={`shrink-0 ${lateral ? "xl:mt-4 xl:w-full" : ""}`}
        aria-haspopup="dialog"
        onClick={() =>
          window.dispatchEvent(new Event("donde-entreno:abrir-asistente"))
        }
      >
        {textoBoton}
      </AppButton>
    </SurfaceCard>
  );
}
