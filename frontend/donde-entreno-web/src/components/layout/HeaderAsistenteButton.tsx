"use client";

/*
  Acceso al asistente desde el header, para desktop.

  La barra inferior es lg:hidden, así que sin esto el asistente quedaba
  sin entrada en pantallas grandes fuera de la home. Dispara el mismo
  evento que la barra y que el botón de la home, así que el panel es
  siempre el mismo y no hay estado duplicado.
*/
export function HeaderAsistenteButton() {
  function abrirAsistente() {
    window.dispatchEvent(new Event("donde-entreno:abrir-asistente"));
  }

  return (
    <button
      type="button"
      onClick={abrirAsistente}
      aria-haspopup="dialog"
      aria-label="Abrir el asistente de DondeEntreno"
      className="hidden min-h-11 items-center gap-2 rounded-[var(--radius-md)] border border-[#BFDDEA] bg-white px-3 py-2 text-sm font-bold text-[var(--color-primary)] transition duration-200 ease-out hover:-translate-y-0.5 hover:border-[var(--color-primary)] hover:bg-[#F8FCFE] lg:inline-flex"
    >
      <svg
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        strokeWidth="2"
        strokeLinecap="round"
        strokeLinejoin="round"
        className="h-4 w-4 text-[var(--color-accent)]"
        aria-hidden="true"
      >
        <path d="M21 11.5a8.5 8.5 0 0 1-12.3 7.6L3 21l1.9-5.7A8.5 8.5 0 1 1 21 11.5z" />
        <path d="M8.5 11.5h.01M12 11.5h.01M15.5 11.5h.01" />
      </svg>
      Asistente
    </button>
  );
}
