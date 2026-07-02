"use client";

import { useMemo, useState } from "react";
import { useSearchParams } from "next/navigation";
import {
  coincideDeporteConBusqueda,
  normalizarCompacto,
  obtenerCategoriasBusquedaDeportes,
  obtenerPuntajeBusquedaDeporte,
  obtenerSugerenciasBusquedaDeportes,
  obtenerValorCategoria,
} from "../../lib/deporteSearch";
import type {
  CategoriaBusquedaDeporte,
  SugerenciaBusquedaDeporte,
} from "../../lib/deporteSearch";
import type { Deporte } from "../../types/deporte";
import { SectionHeader } from "../ui/SectionHeader";
import { StatusMessage } from "../ui/StatusMessage";
import { SurfaceCard } from "../ui/SurfaceCard";
import { SportCatalogCard } from "./SportCatalogCard";
import { SportsCategoryGroup } from "./SportsCategoryGroup";

type SportsCatalogProps = {
  deportes: Deporte[];
};

type GrupoDeportes = {
  clave: string;
  titulo: string;
  deportes: Deporte[];
};

const slugsPopulares = [
  "boxeo",
  "jiu-jitsu",
  "futbol",
  "yoga",
  "gimnasio",
  "natacion",
];

const LIMITE_GENERAL_POR_CATEGORIA = 6;
const LIMITE_CATEGORIA_FILTRADA = 12;
const MAXIMO_SUGERENCIAS = 6;

function obtenerClaveCategoria(deporte: Deporte, hayCategorias: boolean): string {
  const valorCategoria = obtenerValorCategoria(deporte);

  if (valorCategoria) {
    return valorCategoria;
  }

  return hayCategorias ? "otros-deportes" : "todos-los-deportes";
}

function agruparPorCategoria(deportes: Deporte[]): GrupoDeportes[] {
  const hayCategorias = deportes.some((deporte) => deporte.categoriaNombre);
  const grupos = new Map<string, GrupoDeportes>();

  for (const deporte of deportes) {
    const titulo =
      deporte.categoriaNombre ||
      (hayCategorias ? "Otros deportes" : "Todos los deportes");
    const clave = obtenerClaveCategoria(deporte, hayCategorias);
    const grupoExistente = grupos.get(clave);

    if (grupoExistente) {
      grupoExistente.deportes.push(deporte);
      continue;
    }

    grupos.set(clave, {
      clave,
      titulo,
      deportes: [deporte],
    });
  }

  return Array.from(grupos.values());
}

function esDeporte(valor: Deporte | undefined): valor is Deporte {
  return Boolean(valor);
}

function construirIdGrillaCategoria(claveCategoria: string): string {
  return `grilla-deportes-${normalizarCompacto(claveCategoria) || "categoria"}`;
}

export function SportsCatalog({ deportes }: SportsCatalogProps) {
  const searchParams = useSearchParams();
  const categoriaInicial = searchParams.get("categoria") || "";
  const deporteInicial = searchParams.get("deporte") || "";
  const busquedaInicial = searchParams.get("q") || "";
  const [busqueda, setBusqueda] = useState(busquedaInicial);
  const [categoriaSeleccionada, setCategoriaSeleccionada] =
    useState(categoriaInicial);
  const [deporteSeleccionado, setDeporteSeleccionado] =
    useState(deporteInicial);
  const [categoriasExpandidas, setCategoriasExpandidas] = useState<
    Record<string, boolean>
  >({});

  const deportesPopulares = useMemo(
    () =>
      slugsPopulares
        .map((slug) => deportes.find((deporte) => deporte.slug === slug))
        .filter(esDeporte),
    [deportes]
  );

  const categorias = useMemo<CategoriaBusquedaDeporte[]>(
    () => obtenerCategoriasBusquedaDeportes(deportes),
    [deportes]
  );
  const mostrarSugerenciasBusqueda = normalizarCompacto(busqueda).length >= 2;

  const sugerenciasBusqueda = useMemo<SugerenciaBusquedaDeporte[]>(() => {
    return obtenerSugerenciasBusquedaDeportes({
      deportes,
      categorias,
      busqueda,
      limite: MAXIMO_SUGERENCIAS,
    });
  }, [busqueda, categorias, deportes]);

  const deportesFiltrados = useMemo(() => {
    return deportes.filter((deporte) => {
      const coincideCategoria =
        !categoriaSeleccionada ||
        obtenerValorCategoria(deporte) === categoriaSeleccionada;
      const coincideDeporte =
        !deporteSeleccionado || deporte.slug === deporteSeleccionado;

      return (
        coincideCategoria &&
        coincideDeporte &&
        coincideDeporteConBusqueda(deporte, busqueda)
      );
    });
  }, [busqueda, categoriaSeleccionada, deporteSeleccionado, deportes]);

  const hayFiltrosActivos =
    busqueda.trim().length > 0 ||
    categoriaSeleccionada.length > 0 ||
    deporteSeleccionado.length > 0;

  const busquedaActiva = busqueda.trim().length > 0;
  const categoriaActiva = categoriaSeleccionada.length > 0;
  const deporteActivo = deporteSeleccionado.length > 0;

  const grupos = useMemo(() => {
    const gruposBase = agruparPorCategoria(deportesFiltrados);

    if (!busquedaActiva) {
      return gruposBase;
    }

    return gruposBase.map((grupo) => ({
      ...grupo,
      deportes: [...grupo.deportes].sort((deporteA, deporteB) => {
        const diferenciaPuntaje =
          obtenerPuntajeBusquedaDeporte(deporteB, busqueda) -
          obtenerPuntajeBusquedaDeporte(deporteA, busqueda);

        if (diferenciaPuntaje !== 0) {
          return diferenciaPuntaje;
        }

        return deporteA.nombre.localeCompare(deporteB.nombre, "es");
      }),
    }));
  }, [busqueda, busquedaActiva, deportesFiltrados]);

  const limitePorCategoria =
    busquedaActiva || deporteActivo
      ? null
      : categoriaActiva
        ? LIMITE_CATEGORIA_FILTRADA
        : LIMITE_GENERAL_POR_CATEGORIA;

  const mostrarPopulares = !hayFiltrosActivos && deportesPopulares.length > 0;

  function alternarCategoriaExpandida(valorCategoria: string) {
    setCategoriasExpandidas((categoriasActuales) => ({
      ...categoriasActuales,
      [valorCategoria]: !categoriasActuales[valorCategoria],
    }));
  }

  function limpiarFiltros() {
    setBusqueda("");
    setCategoriaSeleccionada("");
    setDeporteSeleccionado("");
    setCategoriasExpandidas({});
  }

  function seleccionarSugerencia(sugerencia: SugerenciaBusquedaDeporte) {
    if (sugerencia.tipo === "deporte") {
      setDeporteSeleccionado(sugerencia.valor);
      setCategoriaSeleccionada("");
    } else {
      setCategoriaSeleccionada(sugerencia.valor);
      setDeporteSeleccionado("");
    }

    setBusqueda("");
    setCategoriasExpandidas({});
  }

  return (
    <div className="mt-10 space-y-12">
      <section className="rounded-[var(--radius-xl)] border border-[#DDEAF3] bg-white/85 p-4 shadow-[0_16px_40px_rgba(12,52,80,0.08)] sm:p-6">
        <div className="grid gap-4 lg:grid-cols-[1fr_1fr_auto] lg:items-end">
          <div className="lg:col-span-3">
            <label
              htmlFor="buscar-deporte"
              className="text-sm font-bold text-[var(--color-primary)]"
            >
              Buscar deporte
            </label>
            <input
              id="buscar-deporte"
              type="search"
              value={busqueda}
              onChange={(evento) => setBusqueda(evento.target.value)}
              placeholder="Buscá por deporte, estilo o alias: bjj, gym, pileta..."
              className="mt-3 min-h-12 w-full rounded-[18px] border border-[#BFDDEA] bg-[#F8FAFC] px-4 text-sm font-medium text-[var(--color-text)] outline-none transition duration-200 ease-out placeholder:text-[var(--color-muted)] hover:border-[var(--color-accent)] focus:border-[var(--color-accent)] focus:ring-4 focus:ring-[#DDEAF3]"
            />

            {mostrarSugerenciasBusqueda && sugerenciasBusqueda.length > 0 ? (
              <div className="mt-3 rounded-[18px] border border-[#DDEAF3] bg-white p-2 shadow-[0_14px_35px_rgba(12,52,80,0.10)]">
                <p className="px-3 py-2 text-xs font-extrabold uppercase tracking-[0.14em] text-[var(--color-secondary)]">
                  Sugerencias
                </p>
                <div className="grid gap-2 sm:grid-cols-2">
                  {sugerenciasBusqueda.map((sugerencia) => (
                    <button
                      key={sugerencia.id}
                      type="button"
                      onClick={() => seleccionarSugerencia(sugerencia)}
                      className="flex items-center justify-between gap-3 rounded-[14px] border border-transparent bg-[#F8FAFC] px-3 py-3 text-left transition duration-200 ease-out hover:-translate-y-0.5 hover:border-[#BFDDEA] hover:bg-[#F8FCFE] active:scale-[0.98]"
                    >
                      <span className="text-sm font-bold text-[var(--color-primary)]">
                        {sugerencia.label}
                      </span>
                      <span className="shrink-0 rounded-full bg-[#E6F7EF] px-3 py-1 text-xs font-bold text-[#167A4A]">
                        {sugerencia.tipo === "deporte" ? "Deporte" : "Estilo"}
                      </span>
                    </button>
                  ))}
                </div>
              </div>
            ) : null}
          </div>

          <div>
            <label
              htmlFor="filtro-categoria"
              className="text-sm font-bold text-[var(--color-primary)]"
            >
              Estilo de actividad
            </label>
            <select
              id="filtro-categoria"
              value={categoriaSeleccionada}
              onChange={(evento) =>
                setCategoriaSeleccionada(evento.target.value)
              }
              className="mt-3 min-h-12 w-full rounded-[18px] border border-[#BFDDEA] bg-[#F8FAFC] px-4 text-sm font-bold text-[var(--color-primary)] outline-none transition duration-200 ease-out hover:border-[var(--color-accent)] focus:border-[var(--color-accent)] focus:ring-4 focus:ring-[#DDEAF3]"
            >
              <option value="">Todas</option>
              {categorias.map((categoria) => (
                <option key={categoria.valor} value={categoria.valor}>
                  {categoria.nombre}
                </option>
              ))}
            </select>
          </div>

          <div>
            <label
              htmlFor="filtro-deporte"
              className="text-sm font-bold text-[var(--color-primary)]"
            >
              Deporte
            </label>
            <select
              id="filtro-deporte"
              value={deporteSeleccionado}
              onChange={(evento) => setDeporteSeleccionado(evento.target.value)}
              className="mt-3 min-h-12 w-full rounded-[18px] border border-[#BFDDEA] bg-[#F8FAFC] px-4 text-sm font-bold text-[var(--color-primary)] outline-none transition duration-200 ease-out hover:border-[var(--color-accent)] focus:border-[var(--color-accent)] focus:ring-4 focus:ring-[#DDEAF3]"
            >
              <option value="">Todos</option>
              {deportes.map((deporte) => (
                <option key={deporte.id} value={deporte.slug}>
                  {deporte.nombre}
                </option>
              ))}
            </select>
          </div>

          <button
            type="button"
            onClick={limpiarFiltros}
            disabled={!hayFiltrosActivos}
            className="min-h-12 rounded-[18px] border border-[#BFDDEA] bg-white px-4 text-sm font-bold text-[var(--color-primary)] transition duration-200 ease-out hover:-translate-y-0.5 hover:border-[var(--color-primary)] hover:bg-[#F8FCFE] active:scale-[0.98] disabled:cursor-not-allowed disabled:opacity-50 disabled:hover:translate-y-0"
          >
            Limpiar filtros
          </button>
        </div>

        {hayFiltrosActivos ? (
          <p className="mt-4 text-sm font-bold text-[#167A4A]" role="status">
            Mostrando {deportesFiltrados.length}{" "}
            {deportesFiltrados.length === 1 ? "deporte" : "deportes"}
          </p>
        ) : null}
      </section>

      {mostrarPopulares ? (
        <section>
          <SectionHeader
            eyebrow="Deportes populares"
            title="Empezá por los más buscados"
            className="mb-4"
          />

          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {deportesPopulares.map((deporte) => (
              <SportCatalogCard key={deporte.id} deporte={deporte} />
            ))}
          </div>
        </section>
      ) : null}

      <SurfaceCard as="section" variant="soft" className="p-4 sm:p-6">
        <SectionHeader
          eyebrow="Todos los deportes"
          title="Elegí una opción para explorar actividades"
          className="mb-7"
        />

        {deportesFiltrados.length === 0 ? (
          <StatusMessage
            variant="info"
            title="No encontramos deportes con esa búsqueda"
            className="p-6 text-center"
          >
            <p>
              Probá con otro nombre, una categoría o una forma alternativa como
              bjj, gym o pileta.
            </p>
          </StatusMessage>
        ) : (
          <div className="space-y-10">
            {grupos.map((grupo) => {
              const estaExpandida = Boolean(categoriasExpandidas[grupo.clave]);
              const limiteActual = limitePorCategoria;
              const mostrarControlExpansion =
                limiteActual !== null && grupo.deportes.length > limiteActual;
              const deportesVisibles =
                limiteActual !== null &&
                grupo.deportes.length > limiteActual &&
                !estaExpandida
                  ? grupo.deportes.slice(0, limiteActual)
                  : grupo.deportes;
              const cantidadRestante =
                limiteActual !== null
                  ? Math.max(0, grupo.deportes.length - limiteActual)
                  : 0;

              return (
                <SportsCategoryGroup
                  key={grupo.clave}
                  titulo={grupo.titulo}
                  deportes={deportesVisibles}
                  totalDeportes={grupo.deportes.length}
                  idGrilla={construirIdGrillaCategoria(grupo.clave)}
                  mostrarControlExpansion={mostrarControlExpansion}
                  estaExpandida={estaExpandida}
                  cantidadRestante={cantidadRestante}
                  onAlternarExpansion={() =>
                    alternarCategoriaExpandida(grupo.clave)
                  }
                />
              );
            })}
          </div>
        )}
      </SurfaceCard>
    </div>
  );
}
