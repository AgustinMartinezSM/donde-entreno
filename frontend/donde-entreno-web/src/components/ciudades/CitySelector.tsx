"use client";

import { useEffect, useState } from "react";
import { usePathname, useRouter, useSearchParams } from "next/navigation";

import {
  DEFAULT_CITY_SLUG,
  buscarCiudadPorId,
  buscarCiudadPorSlug,
  guardarSlugCiudadActiva,
  leerSlugCiudadGuardada,
  normalizarSlugCiudad,
  obtenerCiudadFallback,
  obtenerSlugCiudadDesdeRuta,
  ordenarCiudadesActivas,
} from "../../lib/ciudadActiva";
import { obtenerCiudades } from "../../services/ciudadService";
import type { Ciudad } from "../../types/ciudad";

const selectorContenedorClassName =
  "flex h-11 w-full min-w-0 items-center gap-2 rounded-full border border-[#BFDDEA] bg-white/95 px-3 text-sm font-bold text-[var(--color-primary)] shadow-sm transition duration-200 ease-out focus-within:border-[var(--color-accent)] focus-within:ring-4 focus-within:ring-[#4FB3D9]/25 sm:w-auto";

export function CitySelectorFallback() {
  return (
    <div className={selectorContenedorClassName} role="status">
      <span className="hidden shrink-0 text-xs font-extrabold uppercase tracking-[0.12em] text-[var(--color-secondary)] sm:inline">
        Ciudad
      </span>
      <span className="truncate">Mar del Plata</span>
    </div>
  );
}

export function CitySelector() {
  const router = useRouter();
  const pathname = usePathname();
  const searchParams = useSearchParams();
  const searchParamsSerializados = searchParams.toString();

  const [ciudades, setCiudades] = useState<Ciudad[]>([]);
  const [, setVersionCiudadGuardada] = useState(0);
  const [cargando, setCargando] = useState(true);
  const [huboError, setHuboError] = useState(false);

  const ciudadSlugDesdeQuery = normalizarSlugCiudad(
    searchParams.get("ciudadSlug")
  );
  const ciudadSlugDesdeRuta = obtenerSlugCiudadDesdeRuta(pathname);
  const ciudadSlugDesdeUrl = ciudadSlugDesdeQuery ?? ciudadSlugDesdeRuta;
  const ciudadIdDesdeUrl = searchParams.get("ciudadId");
  const ciudadResuelta =
    !cargando && !huboError && ciudades.length > 0
      ? resolverCiudadActiva({
          ciudades,
          ciudadSlugDesdeUrl,
          ciudadIdDesdeUrl,
        })
      : null;
  const slugSeleccionado = ciudadResuelta?.slug ?? DEFAULT_CITY_SLUG;

  useEffect(() => {
    let componenteActivo = true;

    obtenerCiudades()
      .then((ciudadesObtenidas) => {
        if (!componenteActivo) {
          return;
        }

        setCiudades(ordenarCiudadesActivas(ciudadesObtenidas));
        setHuboError(false);
      })
      .catch(() => {
        if (componenteActivo) {
          setHuboError(true);
        }
      })
      .finally(() => {
        if (componenteActivo) {
          setCargando(false);
        }
      });

    return () => {
      componenteActivo = false;
    };
  }, []);

  useEffect(() => {
    if (!ciudadResuelta) {
      return;
    }

    guardarSlugCiudadActiva(ciudadResuelta.slug);

    if (pathname === "/explorar" && !ciudadSlugDesdeUrl && !ciudadIdDesdeUrl) {
      const params = new URLSearchParams(searchParamsSerializados);
      params.set("ciudadSlug", ciudadResuelta.slug);
      params.set("page", "0");
      router.replace(`/explorar?${params.toString()}`);
    }
  }, [
    ciudadIdDesdeUrl,
    ciudadResuelta,
    ciudadSlugDesdeUrl,
    pathname,
    router,
    searchParamsSerializados,
  ]);

  function manejarCambioCiudad(evento: React.ChangeEvent<HTMLSelectElement>) {
    const nuevaCiudad = buscarCiudadPorSlug(ciudades, evento.target.value);

    if (!nuevaCiudad) {
      return;
    }

    guardarSlugCiudadActiva(nuevaCiudad.slug);
    setVersionCiudadGuardada((versionActual) => versionActual + 1);

    if (pathname === "/explorar") {
      const params = new URLSearchParams(searchParamsSerializados);
      params.delete("ciudadId");
      params.set("ciudadSlug", nuevaCiudad.slug);
      params.set("page", "0");
      router.push(`/explorar?${params.toString()}`);
      return;
    }

    if (obtenerSlugCiudadDesdeRuta(pathname)) {
      router.push(`/ciudades/${encodeURIComponent(nuevaCiudad.slug)}`);
    }
  }

  if (cargando) {
    return <CitySelectorFallback />;
  }

  if (huboError || ciudades.length === 0) {
    return <CitySelectorFallback />;
  }

  return (
    <div className="w-full min-w-0 sm:w-auto">
      <label htmlFor="ciudad-activa" className="sr-only">
        Seleccionar ciudad activa
      </label>

      <div className={selectorContenedorClassName}>
        <span className="hidden shrink-0 text-xs font-extrabold uppercase tracking-[0.12em] text-[var(--color-secondary)] sm:inline">
          Ciudad
        </span>

        <select
          id="ciudad-activa"
          value={slugSeleccionado}
          onChange={manejarCambioCiudad}
          aria-label="Seleccionar ciudad activa"
          className="w-full min-w-0 max-w-full bg-transparent text-sm font-extrabold text-[var(--color-primary)] outline-none focus-visible:outline-none sm:w-auto"
        >
          {ciudades.map((ciudad) => (
            <option key={ciudad.id} value={ciudad.slug}>
              {ciudad.nombre}
            </option>
          ))}
        </select>
      </div>
    </div>
  );
}

function resolverCiudadActiva({
  ciudades,
  ciudadSlugDesdeUrl,
  ciudadIdDesdeUrl,
}: {
  ciudades: Ciudad[];
  ciudadSlugDesdeUrl: string | null;
  ciudadIdDesdeUrl: string | null;
}) {
  if (ciudadSlugDesdeUrl) {
    return (
      buscarCiudadPorSlug(ciudades, ciudadSlugDesdeUrl) ??
      obtenerCiudadFallback(ciudades)
    );
  }

  if (ciudadIdDesdeUrl) {
    return (
      buscarCiudadPorId(ciudades, ciudadIdDesdeUrl) ??
      obtenerCiudadFallback(ciudades)
    );
  }

  return (
    buscarCiudadPorSlug(ciudades, leerSlugCiudadGuardada()) ??
    obtenerCiudadFallback(ciudades)
  );
}
