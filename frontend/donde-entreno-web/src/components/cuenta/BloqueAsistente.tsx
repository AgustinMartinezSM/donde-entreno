"use client";

import { AppButton } from "../ui/AppButton";
import { SurfaceCard } from "../ui/SurfaceCard";

type BloqueAsistenteProps = {
  titulo?: string;
  descripcion?: string;
  textoBoton?: string;
};

/*
  Acceso al asistente desde el perfil.

  El asistente no es una ruta: vive en un panel global que se abre con un
  evento, el mismo que usa la barra inferior. Por eso acá va un botón y
  no un link.
*/
export function BloqueAsistente({
  titulo = "¿No sabés por dónde empezar?",
  descripcion = "Contale qué buscás y te digo dónde entrenar cerca tuyo.",
  textoBoton = "Hablar con el asistente",
}: BloqueAsistenteProps) {
  return (
    <SurfaceCard
      variant="info"
      className="flex flex-col gap-4 p-5 sm:flex-row sm:items-center sm:justify-between sm:p-6"
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
        className="shrink-0"
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
