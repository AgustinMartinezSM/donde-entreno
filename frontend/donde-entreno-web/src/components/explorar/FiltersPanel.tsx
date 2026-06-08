"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import type { FiltrosOpciones } from "../../types/filtros";

type FiltersPanelProps = {
  filtros: FiltrosOpciones;

  textoBuscado?: string;
  ciudadIdActual?: string;
  barrioIdActual?: string;
  deporteSlugActual?: string;
  nivelActual?: string;
  modalidadActual?: string;
  ordenActual?: string;
};

export function FiltersPanel({
  filtros,
  textoBuscado = "",
  ciudadIdActual = "",
  barrioIdActual = "",
  deporteSlugActual = "",
  nivelActual = "",
  modalidadActual = "",
  ordenActual = "",
}: FiltersPanelProps) {
  const router = useRouter();

  /*
    Guardamos en estado los filtros seleccionados.
    Arrancan con lo que venga desde la URL.
  */
  const [ciudadId, setCiudadId] = useState(ciudadIdActual);
  const [barrioId, setBarrioId] = useState(barrioIdActual);
  const [deporteSlug, setDeporteSlug] = useState(deporteSlugActual);
  const [nivel, setNivel] = useState(nivelActual);
  const [modalidad, setModalidad] = useState(modalidadActual);

  const hayFiltrosActivos =
    ciudadIdActual ||
    barrioIdActual ||
    deporteSlugActual ||
    nivelActual ||
    modalidadActual;

  const [filtrosAbiertos, setFiltrosAbiertos] = useState(
    Boolean(hayFiltrosActivos),
  );

  function aplicarFiltros() {
    const params = new URLSearchParams();

    /*
      UX importante:
      Si el usuario selecciona un deporte desde el filtro, limpiamos la búsqueda
      escrita para evitar combinaciones confusas como:

      /explorar?texto=jiu+jitsu&deporteSlug=boxeo

      Eso buscaría "jiu jitsu" dentro de Boxeo y probablemente no mostraría nada.
      Para el usuario común es más claro:
      selecciono Boxeo -> veo Boxeo.
    */
    if (textoBuscado && !deporteSlug) {
      params.set("texto", textoBuscado);
    }

    /*
      Conservamos el orden si ya existía.
    */
    if (ordenActual) {
      params.set("orden", ordenActual);
    }

    /*
      Agregamos solo los filtros que tengan valor.
    */
    if (ciudadId) {
      params.set("ciudadId", ciudadId);
    }

    if (barrioId) {
      params.set("barrioId", barrioId);
    }

    if (deporteSlug) {
      params.set("deporteSlug", deporteSlug);
    }

    if (nivel) {
      params.set("nivel", nivel);
    }

    if (modalidad) {
      params.set("modalidad", modalidad);
    }

    /*
      Siempre volvemos a la primera página al aplicar filtros.
    */
    params.set("page", "0");

    router.push(`/explorar?${params.toString()}`);
  }

  function limpiarFiltros() {
    /*
      Primero limpiamos el estado visual de los selects.
      Esto hace que vuelvan a mostrarse como "Todas" / "Todos".
    */
    setCiudadId("");
    setBarrioId("");
    setDeporteSlug("");
    setNivel("");
    setModalidad("");

    const params = new URLSearchParams();

    /*
      Conservamos búsqueda y orden si ya existían.
      Pero limpiamos ciudad, barrio, deporte, nivel y modalidad.
    */
    if (textoBuscado) {
      params.set("texto", textoBuscado);
    }

    if (ordenActual) {
      params.set("orden", ordenActual);
    }

    params.set("page", "0");

    router.push(`/explorar?${params.toString()}`);
  }

  return (
    <div className="mt-6 rounded-[var(--radius-lg)] border border-[var(--color-border)] bg-[var(--color-surface)] p-4 shadow-[var(--shadow-card)] sm:p-5">
      <div className="flex items-start justify-between gap-4">
        <div>
          <h2 className="text-lg font-extrabold text-[var(--color-primary)]">
            Filtros
          </h2>

          <p className="mt-1 text-sm text-[var(--color-muted)]">
            Refiná la búsqueda por ciudad, deporte, nivel o modalidad.
          </p>
        </div>

        <button
          type="button"
          onClick={() => setFiltrosAbiertos((valorActual) => !valorActual)}
          className="rounded-full border border-[var(--color-border)] px-3 py-2 text-xs font-bold text-[var(--color-primary)] sm:hidden"
        >
          {filtrosAbiertos ? "Ocultar" : "Mostrar"}
        </button>
      </div>

      <div className={`${filtrosAbiertos ? "block" : "hidden"} mt-4 sm:block`}>
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-5">
          {/* Filtro por ciudad */}
          <div className="flex flex-col gap-2">
            <label className="text-sm font-bold text-[var(--color-primary)]">
              Ciudad
            </label>

            <select
              value={ciudadId}
              onChange={(evento) => {
                setCiudadId(evento.target.value);
                setBarrioId("");
              }}
              className="min-h-12 rounded-[var(--radius-md)] border border-[var(--color-border)] bg-white px-3 text-sm outline-none focus:border-[var(--color-accent)]"
            >
              <option value="">Todas</option>

              {filtros.ciudades.map((ciudad) => (
                <option key={ciudad.id} value={ciudad.id}>
                  {ciudad.nombre}
                </option>
              ))}
            </select>
          </div>

          {/* Filtro por barrio */}
          <div className="flex flex-col gap-2">
            <label className="text-sm font-bold text-[var(--color-primary)]">
              Barrio
            </label>

            <select
              value={barrioId}
              onChange={(evento) => setBarrioId(evento.target.value)}
              className="min-h-12 rounded-[var(--radius-md)] border border-[var(--color-border)] bg-white px-3 text-sm outline-none focus:border-[var(--color-accent)]"
            >
              <option value="">Todos</option>

              {filtros.barrios
                .filter((barrio) => {
                  if (!ciudadId) return true;

                  return String(barrio.ciudadId) === ciudadId;
                })
                .map((barrio) => (
                  <option key={barrio.id} value={barrio.id}>
                    {barrio.nombre}
                  </option>
                ))}
            </select>
          </div>

          {/* Filtro por deporte */}
          <div className="flex flex-col gap-2">
            <label className="text-sm font-bold text-[var(--color-primary)]">
              Deporte
            </label>

            <select
              value={deporteSlug}
              onChange={(evento) => setDeporteSlug(evento.target.value)}
              className="min-h-12 rounded-[var(--radius-md)] border border-[var(--color-border)] bg-white px-3 text-sm outline-none focus:border-[var(--color-accent)]"
            >
              <option value="">Todos</option>

              {filtros.deportes.map((deporte) => (
                <option key={deporte.id} value={deporte.slug}>
                  {deporte.nombre}
                </option>
              ))}
            </select>
          </div>

          {/* Filtro por nivel */}
          <div className="flex flex-col gap-2">
            <label className="text-sm font-bold text-[var(--color-primary)]">
              Nivel
            </label>

            <select
              value={nivel}
              onChange={(evento) => setNivel(evento.target.value)}
              className="min-h-12 rounded-[var(--radius-md)] border border-[var(--color-border)] bg-white px-3 text-sm outline-none focus:border-[var(--color-accent)]"
            >
              <option value="">Todos</option>

              {filtros.niveles.map((nivelOpcion) => (
                <option key={nivelOpcion} value={nivelOpcion}>
                  {nivelOpcion}
                </option>
              ))}
            </select>
          </div>

          {/* Filtro por modalidad */}
          <div className="flex flex-col gap-2">
            <label className="text-sm font-bold text-[var(--color-primary)]">
              Modalidad
            </label>

            <select
              value={modalidad}
              onChange={(evento) => setModalidad(evento.target.value)}
              className="min-h-12 rounded-[var(--radius-md)] border border-[var(--color-border)] bg-white px-3 text-sm outline-none focus:border-[var(--color-accent)]"
            >
              <option value="">Todas</option>

              {filtros.modalidades.map((modalidadOpcion) => (
                <option key={modalidadOpcion} value={modalidadOpcion}>
                  {modalidadOpcion}
                </option>
              ))}
            </select>
          </div>
        </div>

        <div className="mt-5 flex flex-col gap-3 sm:flex-row sm:justify-end">
          <button
            type="button"
            onClick={limpiarFiltros}
            className="min-h-12 w-full rounded-[var(--radius-md)] border border-[var(--color-border)] px-5 text-sm font-bold text-[var(--color-primary)] sm:w-auto"
          >
            Limpiar
          </button>

          <button
            type="button"
            onClick={aplicarFiltros}
            className="min-h-12 w-full rounded-[var(--radius-md)] bg-[var(--color-primary)] px-5 text-sm font-bold text-white shadow-[var(--shadow-button)] sm:w-auto"
          >
            Aplicar filtros
          </button>
        </div>
      </div>
    </div>
  );
}