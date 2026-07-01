"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";

type SearchBarProps = {
  valorInicial?: string;
};

export function SearchBar({ valorInicial = "" }: SearchBarProps) {
  const [texto, setTexto] = useState(valorInicial);
  const router = useRouter();

  function manejarBusqueda(evento: React.FormEvent<HTMLFormElement>) {
    evento.preventDefault();

    const textoLimpio = texto.trim();

    if (!textoLimpio) {
      router.push("/explorar");
      return;
    }

    router.push(`/explorar?texto=${encodeURIComponent(textoLimpio)}`);
  }

  return (
    <form
      onSubmit={manejarBusqueda}
      className="mt-8 rounded-[24px] border border-[#BFDDEA] bg-white/95 p-2.5 shadow-[0_18px_45px_rgba(12,52,80,0.12)] transition duration-200 ease-out focus-within:border-[var(--color-accent)] focus-within:ring-4 focus-within:ring-[#DDEAF3] sm:p-3"
    >
      <div className="flex flex-col gap-3 sm:flex-row">
        <input
          type="text"
          value={texto}
          onChange={(evento) => setTexto(evento.target.value)}
          placeholder="Buscar deporte, actividad o club"
          className="min-h-12 flex-1 rounded-[18px] border border-transparent bg-[#F8FAFC] px-4 text-sm font-medium text-[var(--color-text)] outline-none transition duration-200 ease-out placeholder:text-[var(--color-muted)] hover:border-[#BFDDEA] focus:border-[var(--color-accent)] sm:min-h-14"
        />

        <button
          type="submit"
          className="min-h-12 rounded-[18px] bg-[var(--color-primary)] px-6 font-bold text-white shadow-[var(--shadow-button)] transition duration-200 ease-out hover:-translate-y-0.5 hover:bg-[#0B314D] active:scale-[0.98] sm:min-h-14 sm:min-w-32"
        >
          Buscar
        </button>
      </div>
    </form>
  );
}
