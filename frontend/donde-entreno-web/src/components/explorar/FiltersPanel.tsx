"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import type { FiltrosOpciones } from "../../types/filtros";
import { formatearEtiquetaCatalogo } from "../../lib/formatoCatalogo";
import { AppButton } from "../ui/AppButton";
import { SurfaceCard } from "../ui/SurfaceCard";

type FiltersPanelProps = {
  filtros: FiltrosOpciones;

  textoBuscado?: string;
  ciudadIdActual?: string;
  ciudadSlugActual?: string;
  barrioIdActual?: string;
  perfilPublicadorIdActual?: string;
  deporteSlugActual?: string;
  nivelActual?: string;
  modalidadActual?: string;
  ordenActual?: string;
};

const selectClassName =
  "min-h-12 rounded-[var(--radius-md)] border border-[#BFDDEA] bg-[#F8FAFC] px-3 text-sm outline-none transition duration-200 ease-out hover:border-[var(--color-accent)] focus:border-[var(--color-accent)] focus-visible:ring-4 focus-visible:ring-[#4FB3D9]/30";

export function FiltersPanel({
  filtros,
  textoBuscado = "",
  ciudadIdActual = "",
  ciudadSlugActual = "",
  barrioIdActual = "",
  perfilPublicadorIdActual = "",
  deporteSlugActual = "",
  nivelActual = "",
  modalidadActual = "",
  ordenActual = "",
}: FiltersPanelProps) {
  const router = useRouter();

  /*
    Si la ciudad activa vino por slug, resolvemos su id para que el select
    muestre la ciudad real en lugar de un "Todas" engañoso.
  */
  const ciudadIdResueltaDesdeSlug = ciudadSlugActual
    ? String(
        filtros.ciudades.find((ciudad) => ciudad.slug === ciudadSlugActual)
          ?.id ?? ""
      )
    : "";

  /*
    Guardamos en estado los filtros seleccionados.
    Arrancan con lo que venga desde la URL.
  */
  const [ciudadId, setCiudadId] = useState(
    ciudadSlugActual ? ciudadIdResueltaDesdeSlug : ciudadIdActual
  );
  const [barrioId, setBarrioId] = useState(barrioIdActual);
  const [deporteSlug, setDeporteSlug] = useState(deporteSlugActual);
  const [nivel, setNivel] = useState(nivelActual);
  const [modalidad, setModalidad] = useState(modalidadActual);
  const nivelesVisibles = filtrarOpcionesNeutrales(filtros.niveles, [
    "TODOS",
  ]);
  const modalidadesVisibles = filtrarOpcionesNeutrales(filtros.modalidades, [
    "TODAS",
    "TODOS",
  ]);

  /*
    Siempre colapsado al entrar, incluso con filtros activos: abierto
    ocupaba media pantalla en mobile y empujaba los resultados —que son
    lo que la persona vino a ver— abajo del pliegue. Lo que está
    filtrando no se pierde: se resume en chips debajo del encabezado.
  */
  const [filtrosAbiertos, setFiltrosAbiertos] = useState(false);
  const filtrosPanelId = "filtros-explorar";

  /*
    Los filtros activos en palabras, para el modo compacto. Salen de la
    URL (lo aplicado) y no del estado de los selects (lo tocado sin
    aplicar): el resumen tiene que describir los resultados que se están
    viendo, no una intención a medias.
  */
  const resumenFiltrosActivos = [
    filtros.ciudades.find(
      (ciudad) =>
        String(ciudad.id) === ciudadIdActual || ciudad.slug === ciudadSlugActual
    )?.nombre,
    filtros.barrios.find((barrio) => String(barrio.id) === barrioIdActual)
      ?.nombre,
    filtros.deportes.find((deporte) => deporte.slug === deporteSlugActual)
      ?.nombre,
    nivelActual ? formatearEtiquetaCatalogo(nivelActual) : null,
    modalidadActual ? formatearEtiquetaCatalogo(modalidadActual) : null,
  ].filter((valor): valor is string => Boolean(valor));

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
      Agregamos solo los filtros que tengan valor. Si el select quedó en
      la misma ciudad que vino por slug, conservamos el slug (URL estable);
      si el usuario eligió otra ciudad, pasamos su id.
    */
    if (
      ciudadSlugActual &&
      (!ciudadId || ciudadId === ciudadIdResueltaDesdeSlug)
    ) {
      params.set("ciudadSlug", ciudadSlugActual);
    } else if (ciudadId) {
      params.set("ciudadId", ciudadId);
    }

    if (barrioId) {
      params.set("barrioId", barrioId);
    }

    /* El filtro por publicador (llegado desde "seguidos") se conserva. */
    if (perfilPublicadorIdActual) {
      params.set("perfilPublicadorId", perfilPublicadorIdActual);
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
      (La ciudad activa se conserva: se cambia desde el selector de ciudad.)
    */
    setCiudadId(ciudadIdResueltaDesdeSlug);
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

    if (ciudadSlugActual) {
      params.set("ciudadSlug", ciudadSlugActual);
    }

    params.set("page", "0");

    router.push(`/explorar?${params.toString()}`);
  }

  return (
    <SurfaceCard className="relative mt-5 overflow-hidden p-4 decorative-dots sm:p-5">
      <div className="relative z-10 flex items-center justify-between gap-3">
        <div className="flex min-w-0 items-center gap-3">
          <span
            aria-hidden="true"
            className="flex h-11 w-11 shrink-0 items-center justify-center rounded-[var(--radius-md)] bg-[var(--color-info-soft)] text-[var(--color-primary)]"
          >
            <IconoFiltros />
          </span>

          <div className="min-w-0">
            <h2 className="text-base font-extrabold text-[var(--color-primary)] sm:text-lg">
              Filtros
            </h2>

            <p className="mt-0.5 line-clamp-2 text-xs leading-5 text-[var(--color-muted)] sm:text-sm">
              Refiná la búsqueda por ciudad, deporte, nivel o modalidad.
            </p>
          </div>
        </div>

        {/*
          El botón ya no es solo de mobile: en desktop el panel también
          arranca cerrado, así que sin esto no habría manera de abrirlo.
        */}
        <button
          type="button"
          onClick={() => setFiltrosAbiertos((valorActual) => !valorActual)}
          aria-expanded={filtrosAbiertos}
          aria-controls={filtrosPanelId}
          className="inline-flex min-h-11 shrink-0 items-center gap-1.5 rounded-full border border-[var(--color-border-accent)] bg-white px-4 text-xs font-extrabold text-[var(--color-primary)] shadow-sm transition duration-200 ease-out hover:border-[var(--color-primary)] hover:bg-[#F8FCFE] focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-[#4FB3D9]/30 active:scale-[0.98] sm:text-sm"
        >
          {filtrosAbiertos ? "Ocultar" : "Mostrar"}
          <span
            aria-hidden="true"
            className={`transition-transform duration-200 ease-out ${
              filtrosAbiertos ? "rotate-180" : ""
            }`}
          >
            <IconoChevronAbajo />
          </span>
        </button>
      </div>

      {/*
        Con el panel cerrado, lo que se está filtrando igual se ve: sin
        esto, colapsar equivalía a esconder el estado de la búsqueda.
      */}
      {!filtrosAbiertos && resumenFiltrosActivos.length > 0 ? (
        <ul className="relative z-10 mt-3 flex flex-wrap gap-1.5">
          {resumenFiltrosActivos.map((etiqueta) => (
            <li
              key={etiqueta}
              className="rounded-full bg-[var(--color-success-soft)] px-3 py-1 text-xs font-bold text-[var(--color-success)]"
            >
              {etiqueta}
            </li>
          ))}
        </ul>
      ) : null}

      <div
        id={filtrosPanelId}
        className={`relative z-10 ${filtrosAbiertos ? "block" : "hidden"} mt-4`}
      >
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-5">
          {/* Filtro por ciudad */}
          <div className="flex flex-col gap-2">
            <label
              htmlFor="filtro-ciudad"
              className="text-sm font-bold text-[var(--color-primary)]"
            >
              Ciudad
            </label>

            <select
              id="filtro-ciudad"
              value={ciudadId}
              onChange={(evento) => {
                setCiudadId(evento.target.value);
                setBarrioId("");
              }}
              className={selectClassName}
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
            <label
              htmlFor="filtro-barrio"
              className="text-sm font-bold text-[var(--color-primary)]"
            >
              Barrio
            </label>

            <select
              id="filtro-barrio"
              value={barrioId}
              onChange={(evento) => setBarrioId(evento.target.value)}
              className={selectClassName}
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
            <label
              htmlFor="filtro-deporte"
              className="text-sm font-bold text-[var(--color-primary)]"
            >
              Deporte
            </label>

            <select
              id="filtro-deporte"
              value={deporteSlug}
              onChange={(evento) => setDeporteSlug(evento.target.value)}
              className={selectClassName}
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
            <label
              htmlFor="filtro-nivel"
              className="text-sm font-bold text-[var(--color-primary)]"
            >
              Nivel
            </label>

            <select
              id="filtro-nivel"
              value={nivel}
              onChange={(evento) => setNivel(evento.target.value)}
              className={selectClassName}
            >
              <option value="">Todos</option>

              {nivelesVisibles.map((nivelOpcion) => (
                <option key={nivelOpcion} value={nivelOpcion}>
                  {formatearEtiquetaFiltro(nivelOpcion)}
                </option>
              ))}
            </select>
          </div>

          {/* Filtro por modalidad */}
          <div className="flex flex-col gap-2">
            <label
              htmlFor="filtro-modalidad"
              className="text-sm font-bold text-[var(--color-primary)]"
            >
              Modalidad
            </label>

            <select
              id="filtro-modalidad"
              value={modalidad}
              onChange={(evento) => setModalidad(evento.target.value)}
              className={selectClassName}
            >
              <option value="">Todas</option>

              {modalidadesVisibles.map((modalidadOpcion) => (
                <option key={modalidadOpcion} value={modalidadOpcion}>
                  {formatearEtiquetaFiltro(modalidadOpcion)}
                </option>
              ))}
            </select>
          </div>
        </div>

        <div className="mt-5 flex flex-col gap-3 sm:flex-row sm:justify-end">
          <AppButton
            onClick={limpiarFiltros}
            variant="secondary"
            size="lg"
            fullWidth
            className="sm:w-auto"
          >
            Limpiar
          </AppButton>

          <AppButton
            onClick={aplicarFiltros}
            variant="primary"
            size="lg"
            fullWidth
            className="sm:w-auto"
          >
            Aplicar filtros
          </AppButton>
        </div>
      </div>
    </SurfaceCard>
  );
}

function filtrarOpcionesNeutrales(opciones: string[], neutrales: string[]) {
  return opciones.filter((opcion) => {
    const opcionNormalizada = opcion.trim().toUpperCase();

    return opcionNormalizada && !neutrales.includes(opcionNormalizada);
  });
}

/* El formateo vive en lib/formatoCatalogo para que filtros y cards hablen igual. */
const formatearEtiquetaFiltro = formatearEtiquetaCatalogo;

function IconoFiltros() {
  return (
    <svg
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
      className="h-5 w-5"
      aria-hidden="true"
    >
      <path d="M4 6h16M7 12h10M10 18h4" />
    </svg>
  );
}

function IconoChevronAbajo() {
  return (
    <svg
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="2.5"
      strokeLinecap="round"
      strokeLinejoin="round"
      className="h-3.5 w-3.5"
      aria-hidden="true"
    >
      <path d="m6 9 6 6 6-6" />
    </svg>
  );
}
