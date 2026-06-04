"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";

type SearchBarProps = {
  /*
    Valor inicial opcional.
    Sirve para que en /explorar el buscador aparezca cargado
    con el texto actual de la búsqueda.
  */
  valorInicial?: string;
};

export function SearchBar({ valorInicial = "" }: SearchBarProps) {
  /*
    Estado local para guardar lo que escribe el usuario.
    Si viene valorInicial, arrancamos con ese texto.
  */
  const [texto, setTexto] = useState(valorInicial);

  /*
    useRouter nos permite navegar desde código.
  */
  const router = useRouter();

  function manejarBusqueda(evento: React.FormEvent<HTMLFormElement>) {
    /*
      Evitamos que el formulario recargue la página completa.
    */
    evento.preventDefault();

    /*
      Limpiamos espacios de más.
    */
    const textoLimpio = texto.trim();

    /*
      Si el usuario no escribió nada, lo mandamos a /explorar
      para ver todas las actividades.
    */
    if (!textoLimpio) {
      router.push("/explorar");
      return;
    }

    /*
      Armamos la URL de exploración con el texto buscado.
    */
    router.push(`/explorar?texto=${encodeURIComponent(textoLimpio)}`);
  }

  return (
    <form
      onSubmit={manejarBusqueda}
      className="mt-8 rounded-[var(--radius-lg)] border border-[var(--color-border)] bg-[var(--color-surface)] p-3 shadow-[var(--shadow-card)]"    >
      <div className="flex flex-col gap-3 sm:flex-row">
        <input
          type="text"
          value={texto}
          onChange={(evento) => setTexto(evento.target.value)}
          placeholder="Buscar actividad, deporte o club"
          className="min-h-12 flex-1 rounded-[var(--radius-md)] border border-[var(--color-border)] px-4 text-sm outline-none focus:border-[var(--color-accent)]"
        />

        <button
          type="submit"
          className="min-h-12 rounded-[var(--radius-md)] bg-[var(--color-primary)] px-6 font-bold text-white shadow-[var(--shadow-button)] sm:min-w-28"        >
          Buscar
        </button>
      </div>
    </form>
  );
}