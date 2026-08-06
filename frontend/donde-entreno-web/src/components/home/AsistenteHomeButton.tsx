"use client";

export function AsistenteHomeButton() {
  function abrirAsistente() {
    window.dispatchEvent(new Event("donde-entreno:abrir-asistente"));
  }

  return (
    <button
      id="asistente-home-trigger"
      type="button"
      onClick={abrirAsistente}
      className="inline-flex min-h-11 items-center justify-center gap-2 rounded-[16px] border border-[#BFDDEA] bg-white px-4 py-2.5 text-sm font-extrabold text-[var(--color-primary)] shadow-sm transition duration-200 ease-out hover:-translate-y-0.5 hover:border-[var(--color-primary)] hover:bg-[#F8FCFE] active:scale-[0.98]"
    >
      <svg
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        strokeWidth="2"
        strokeLinecap="round"
        strokeLinejoin="round"
        className="h-5 w-5 text-[var(--color-accent)]"
        aria-hidden="true"
      >
        <path d="M21 11.5a8.5 8.5 0 0 1-12.3 7.6L3 21l1.9-5.7A8.5 8.5 0 1 1 21 11.5z" />
        <path d="M8 12h.01M12 12h.01M16 12h.01" />
      </svg>
      Preguntale al asistente
    </button>
  );
}
