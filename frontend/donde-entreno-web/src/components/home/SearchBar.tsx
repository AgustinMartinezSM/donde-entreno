"use client";

import { useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import {
  obtenerCategoriasBusquedaDeportes,
  obtenerDestinoBusquedaDeportes,
  obtenerSugerenciasBusquedaDeportes,
} from "../../lib/deporteSearch";
import type { SugerenciaBusquedaDeporte } from "../../lib/deporteSearch";
import { obtenerDeportes } from "../../services/deportesService";
import type { Deporte } from "../../types/deporte";

type SearchBarProps = {
  valorInicial?: string;
  ciudadSlugActual?: string;
};

export function SearchBar({
  valorInicial = "",
  ciudadSlugActual = "",
}: SearchBarProps) {
  const [texto, setTexto] = useState(valorInicial);
  const [deportes, setDeportes] = useState<Deporte[]>([]);
  const [sugerenciasAbiertas, setSugerenciasAbiertas] = useState(false);
  const router = useRouter();

  useEffect(() => {
    let componenteActivo = true;

    obtenerDeportes()
      .then((deportesCargados) => {
        if (componenteActivo) {
          setDeportes(deportesCargados);
        }
      })
      .catch(() => {
        // Si fallan las sugerencias, el buscador conserva su navegación simple.
      });

    return () => {
      componenteActivo = false;
    };
  }, []);

  const categorias = useMemo(
    () => obtenerCategoriasBusquedaDeportes(deportes),
    [deportes]
  );

  const sugerencias = useMemo(
    () =>
      obtenerSugerenciasBusquedaDeportes({
        deportes,
        categorias,
        busqueda: texto,
        limite: 6,
      }),
    [categorias, deportes, texto]
  );

  const mostrarSugerencias = sugerenciasAbiertas && sugerencias.length > 0;

  function crearHrefExplorar(paramsBusqueda: Record<string, string>) {
    const params = new URLSearchParams();

    if (ciudadSlugActual) {
      params.set("ciudadSlug", ciudadSlugActual);
    }

    for (const [clave, valor] of Object.entries(paramsBusqueda)) {
      if (valor) {
        params.set(clave, valor);
      }
    }

    const query = params.toString();

    return query ? `/explorar?${query}` : "/explorar";
  }

  function construirHrefSugerencia(sugerencia: SugerenciaBusquedaDeporte) {
    if (sugerencia.tipo === "deporte") {
      return crearHrefExplorar({
        deporteSlug: sugerencia.valor,
        page: "0",
      });
    }

    return `/deportes?categoria=${encodeURIComponent(sugerencia.valor)}`;
  }

  function manejarCambioTexto(valor: string) {
    setTexto(valor);
    setSugerenciasAbiertas(true);
  }

  function seleccionarSugerencia(sugerencia: SugerenciaBusquedaDeporte) {
    setSugerenciasAbiertas(false);
    setTexto("");
    router.push(construirHrefSugerencia(sugerencia));
  }

  function manejarBusqueda(evento: React.FormEvent<HTMLFormElement>) {
    evento.preventDefault();
    setSugerenciasAbiertas(false);

    const textoLimpio = texto.trim();

    if (!textoLimpio) {
      router.push(crearHrefExplorar({}));
      return;
    }

    const destino = obtenerDestinoBusquedaDeportes({
      deportes,
      categorias,
      busqueda: textoLimpio,
    });

    if (destino?.tipo === "deporte") {
      router.push(
        crearHrefExplorar({
          deporteSlug: destino.valor,
          page: "0",
        })
      );
      return;
    }

    if (destino?.tipo === "categoria") {
      router.push(`/deportes?categoria=${encodeURIComponent(destino.valor)}`);
      return;
    }

    router.push(crearHrefExplorar({ texto: textoLimpio }));
  }

  return (
    <form
      onSubmit={manejarBusqueda}
      className="mt-8 w-full min-w-0 rounded-[24px] border border-[#BFDDEA] bg-white/95 p-2.5 shadow-[0_18px_45px_rgba(12,52,80,0.12)] transition duration-200 ease-out focus-within:border-[var(--color-accent)] focus-within:ring-4 focus-within:ring-[#DDEAF3] sm:p-3"
    >
      <div className="flex min-w-0 flex-col gap-3 sm:flex-row">
        <input
          type="text"
          value={texto}
          onChange={(evento) => manejarCambioTexto(evento.target.value)}
          onFocus={() => setSugerenciasAbiertas(true)}
          aria-label="Buscar deporte, actividad o club"
          placeholder="Buscar deporte, actividad o club"
          className="min-h-12 w-full min-w-0 flex-1 rounded-[18px] border border-transparent bg-[#F8FAFC] px-4 text-sm font-medium text-[var(--color-text)] outline-none transition duration-200 ease-out placeholder:text-[var(--color-muted)] hover:border-[#BFDDEA] focus:border-[var(--color-accent)] focus-visible:ring-2 focus-visible:ring-[#4FB3D9]/30 sm:min-h-14"
        />

        <button
          type="submit"
          className="min-h-12 w-full rounded-[18px] bg-[var(--color-primary)] px-6 font-bold text-white shadow-[var(--shadow-button)] transition duration-200 ease-out hover:-translate-y-0.5 hover:bg-[#0B314D] focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-[#4FB3D9]/30 focus-visible:ring-offset-2 active:scale-[0.98] sm:min-h-14 sm:w-auto sm:min-w-32"
        >
          Buscar
        </button>
      </div>

      {mostrarSugerencias ? (
        <div className="mt-2 min-w-0 rounded-[20px] border border-[#DDEAF3] bg-white p-2 shadow-[0_16px_38px_rgba(12,52,80,0.14)]">
          <p className="px-3 py-2 text-xs font-extrabold uppercase tracking-[0.14em] text-[var(--color-secondary)]">
            Sugerencias
          </p>
          <div className="grid gap-2">
            {sugerencias.map((sugerencia) => (
              <button
                key={sugerencia.id}
                type="button"
                onClick={() => seleccionarSugerencia(sugerencia)}
                className="flex min-w-0 items-start justify-between gap-3 rounded-[16px] border border-transparent bg-[#F8FAFC] px-3 py-3 text-left transition duration-200 ease-out hover:-translate-y-0.5 hover:border-[#BFDDEA] hover:bg-[#F8FCFE] active:scale-[0.98]"
              >
                <span className="min-w-0">
                  <span className="block text-sm font-extrabold text-[var(--color-primary)]">
                    {sugerencia.label}
                  </span>
                  <span className="mt-1 block text-xs font-medium leading-5 text-[var(--color-muted)]">
                    {sugerencia.textoAyuda}
                  </span>
                </span>
                <span className="shrink-0 rounded-full bg-[#E6F7EF] px-3 py-1 text-xs font-bold text-[#167A4A]">
                  {sugerencia.tipo === "deporte" ? "Deporte" : "Estilo"}
                </span>
              </button>
            ))}
          </div>
        </div>
      ) : null}
    </form>
  );
}
