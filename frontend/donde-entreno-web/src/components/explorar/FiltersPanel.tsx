"use client";

import { useState } from "react";
import type { ReactNode } from "react";
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
        {/*
          Tres columnas y no cinco: con cinco selects en fila cada campo
          quedaba en 211px a 1440px, y sumarle el tile de ícono lo dejaba
          en 159px útiles. El panel es colapsable, así que el alto extra
          de la segunda fila no le saca lugar a los resultados.
        */}
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {/* Filtro por ciudad */}
          <CampoFiltro
            htmlFor="filtro-ciudad"
            etiqueta="Ciudad"
            icono={<IconoCiudad />}
          >
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
          </CampoFiltro>

          {/* Filtro por barrio */}
          <CampoFiltro
            htmlFor="filtro-barrio"
            etiqueta="Barrio"
            icono={<IconoBarrio />}
          >
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
          </CampoFiltro>

          {/* Filtro por deporte */}
          <CampoFiltro
            htmlFor="filtro-deporte"
            etiqueta="Deporte"
            icono={<IconoDeporte />}
          >
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
          </CampoFiltro>

          {/* Filtro por nivel */}
          <CampoFiltro
            htmlFor="filtro-nivel"
            etiqueta="Nivel"
            icono={<IconoNivel />}
          >
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
          </CampoFiltro>

          {/* Filtro por modalidad */}
          <CampoFiltro
            htmlFor="filtro-modalidad"
            etiqueta="Modalidad"
            icono={<IconoModalidad />}
          >
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
          </CampoFiltro>
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

type CampoFiltroProps = {
  htmlFor: string;
  etiqueta: string;
  icono: ReactNode;
  children: ReactNode;
};

/*
  Una fila de filtro: tile de ícono a la izquierda, etiqueta y control a
  la derecha.

  El tile no es decoración gratis: cinco selects idénticos apilados en
  mobile se leen como un formulario largo y sin jerarquía, y el ícono es
  lo que deja distinguir de un vistazo en cuál está parada la persona.
  Va con aria-hidden porque la etiqueta ya nombra el campo.
*/
function CampoFiltro({ htmlFor, etiqueta, icono, children }: CampoFiltroProps) {
  return (
    <div className="flex items-start gap-3">
      {/*
        mt-8 alinea el centro del tile con el del select: la etiqueta
        mide 20px, el gap 8 y el select 48, así que su centro cae a 52px
        del tope; el tile de 40px arranca en 32 y cierra en el mismo 52.
      */}
      <span aria-hidden="true" className="icon-tile mt-8">
        {icono}
      </span>

      <div className="flex min-w-0 flex-1 flex-col gap-2">
        <label
          htmlFor={htmlFor}
          className="text-sm font-bold text-[var(--color-primary)]"
        >
          {etiqueta}
        </label>

        {children}
      </div>
    </div>
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

const propsIconoCampo = {
  viewBox: "0 0 24 24",
  fill: "none",
  stroke: "currentColor",
  strokeWidth: 2,
  strokeLinecap: "round",
  strokeLinejoin: "round",
  className: "h-5 w-5",
  "aria-hidden": true,
} as const;

function IconoFiltros() {
  return (
    <svg {...propsIconoCampo}>
      <path d="M4 6h16M7 12h10M10 18h4" />
    </svg>
  );
}

function IconoCiudad() {
  return (
    <svg {...propsIconoCampo}>
      <path d="M4 21V7l5-3v17M13 21V10l6 3v8M4 21h16" />
      <path d="M6.5 10h.01M6.5 13.5h.01M16 16h.01" />
    </svg>
  );
}

function IconoBarrio() {
  return (
    <svg {...propsIconoCampo}>
      <path d="M4 11.5 12 5l8 6.5" />
      <path d="M6 10.5V20h12v-9.5" />
      <path d="M10 20v-4.5h4V20" />
    </svg>
  );
}

function IconoDeporte() {
  return (
    <svg {...propsIconoCampo}>
      <circle cx="12" cy="12" r="8.5" />
      <path d="M12 3.5v5M12 15.5v5M3.5 12h5M15.5 12h5" />
      <circle cx="12" cy="12" r="3" />
    </svg>
  );
}

function IconoNivel() {
  return (
    <svg {...propsIconoCampo}>
      <path d="M5 20v-5M12 20V9M19 20V4" />
    </svg>
  );
}

function IconoModalidad() {
  return (
    <svg {...propsIconoCampo}>
      <circle cx="14.5" cy="4.8" r="1.9" />
      <path d="M6 21.5l3.2-5.6 3-1.9-1.2-4.4-3.6 2-1.2 3" />
      <path d="M12.2 9.6 15.5 12l3.5.4" />
      <path d="m12.4 14 1.9 3.2 2.4 2.6" />
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
