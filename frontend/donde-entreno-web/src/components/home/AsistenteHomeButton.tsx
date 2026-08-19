"use client";

import { DondiAvatar } from "../asistente/DondiAvatar";

/*
  Acceso compacto a Dondi desde la home.

  Compacto a propósito: el acceso principal es el launcher flotante, que
  está en todas las pantallas. Este es un recordatorio en el lugar donde
  alguien recién llegado no sabe por dónde empezar, no un bloque que se
  lleve media home.
*/
export function AsistenteHomeButton() {
  function abrirAsistente() {
    window.dispatchEvent(new Event("donde-entreno:abrir-asistente"));
  }

  return (
    <button
      id="asistente-home-trigger"
      type="button"
      onClick={abrirAsistente}
      aria-haspopup="dialog"
      className="inline-flex min-h-11 items-center justify-center gap-2 rounded-full border border-[var(--color-border-accent)] bg-[var(--color-surface)] py-1.5 pl-1.5 pr-4 text-sm font-extrabold text-[var(--color-primary)] shadow-sm transition duration-200 ease-out hover:-translate-y-0.5 hover:border-[var(--color-primary)] hover:bg-[var(--color-surface-soft)] focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-[#4FB3D9]/30 active:scale-[0.98]"
    >
      <DondiAvatar tamanio={28} />
      Preguntale a Dondi
    </button>
  );
}
