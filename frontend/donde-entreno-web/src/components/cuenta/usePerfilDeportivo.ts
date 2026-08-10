"use client";

import { useMemo, useSyncExternalStore } from "react";

import { CATALOGO_DEPORTES_ASISTENTE } from "../../lib/asistente/conocimiento";
import { leerSlugCiudadGuardada } from "../../lib/ciudadActiva";
import { useFavoritos } from "../../lib/favoritos";
import { useDeportesFavoritos } from "../../lib/preferenciasDeportivas";
import type { FavoritoGuardado } from "../../lib/favoritos";

export type PasoPerfil = {
  clave: "nombre" | "ciudad" | "deportes" | "siguiendo" | "guardados";
  etiqueta: string;
  completado: boolean;
  /* Adónde va quien todavía no lo hizo. Sin CTA, un paso pendiente es un reproche. */
  accion: { texto: string; tab?: TabPerfil; href?: string } | null;
};

export type TabPerfil = "para-vos" | "guardados" | "siguiendo" | "deportes";

export type PerfilDeportivo = {
  nombre: string;
  nombreCompleto: string;
  iniciales: string;
  ciudadSlug: string | null;
  ciudadNombre: string | null;
  favoritos: FavoritoGuardado[];
  /* Slugs elegidos que además existen en el catálogo real. */
  deportesSlugs: string[];
  deportesNombres: string[];
  cantidadSiguiendo: number | null;
  pasos: PasoPerfil[];
  pasosCompletados: number;
  porcentaje: number;
  /* true cuando todavía falta algún paso y sabemos lo suficiente para decirlo. */
  perfilIncompleto: boolean;
  /* El primer paso pendiente: es el que se ofrece como acción principal. */
  proximoPaso: PasoPerfil | null;
};

/*
  Estado del perfil deportivo del usuario, en un solo lugar.

  La cabecera, la tarjeta de progreso y las solapas necesitan los mismos
  datos (deportes elegidos, guardados, a quién sigue, ciudad activa) y
  antes cada pieza los leía por su cuenta. Acá se leen una vez y se
  derivan los números que se muestran.

  Nada de esto inventa datos: deportes, guardados y ciudad viven en este
  dispositivo, y la cantidad de seguidos viene del backend.
*/
export function usePerfilDeportivo({
  nombre,
  apellido,
  cantidadSiguiendo,
}: {
  nombre: string;
  apellido: string;
  cantidadSiguiendo: number | null;
}): PerfilDeportivo {
  const favoritos = useFavoritos();
  const deportesElegidos = useDeportesFavoritos();

  /*
    La ciudad activa vive en localStorage: se lee recién después de
    hidratar (snapshot de servidor null) para no desincronizar el HTML de
    SSR con el primer render del cliente.
  */
  const ciudadSlug = useSyncExternalStore(
    suscripcionVacia,
    leerSlugCiudadGuardadaSinFallar,
    () => null
  );

  return useMemo(() => {
    const nombreCompleto = [nombre.trim(), apellido.trim()]
      .filter(Boolean)
      .join(" ");

    /*
      Solo mostramos deportes que existen en el catálogo real: un slug
      viejo guardado en el dispositivo no debe aparecer como interés ni
      llevar a una búsqueda vacía.
    */
    const deportesDelCatalogo = CATALOGO_DEPORTES_ASISTENTE.filter((deporte) =>
      deportesElegidos.includes(deporte.slug)
    );

    const pasos: PasoPerfil[] = [
      {
        clave: "nombre",
        etiqueta: "Tu nombre",
        completado: nombreCompleto.length > 0,
        accion: null,
      },
      {
        clave: "ciudad",
        etiqueta: "Tu ciudad",
        completado: Boolean(ciudadSlug),
        accion: { texto: "Elegir ciudad", href: "/ciudades" },
      },
      {
        clave: "deportes",
        etiqueta: "Tus deportes",
        completado: deportesDelCatalogo.length > 0,
        accion: { texto: "Elegir deportes", tab: "deportes" },
      },
      {
        clave: "siguiendo",
        etiqueta: "Seguir a un club o profe",
        /*
          Mientras el dato no llegó (null) damos el paso por hecho: es
          preferible a marcarle un pendiente a alguien que quizás ya
          sigue a diez personas.
        */
        completado: cantidadSiguiendo === null || cantidadSiguiendo > 0,
        accion: { texto: "Ver a quién seguir", tab: "siguiendo" },
      },
      {
        clave: "guardados",
        etiqueta: "Guardar una actividad",
        completado: favoritos.length > 0,
        accion: { texto: "Explorar actividades", href: "/explorar" },
      },
    ];

    const pasosCompletados = pasos.filter((paso) => paso.completado).length;
    const proximoPaso = pasos.find((paso) => !paso.completado) ?? null;

    return {
      nombre: nombre.trim(),
      nombreCompleto,
      iniciales: obtenerIniciales(nombreCompleto),
      ciudadSlug,
      ciudadNombre: formatearSlugCiudad(ciudadSlug),
      favoritos,
      deportesSlugs: deportesDelCatalogo.map((deporte) => deporte.slug),
      deportesNombres: deportesDelCatalogo.map((deporte) => deporte.nombre),
      cantidadSiguiendo,
      pasos,
      pasosCompletados,
      porcentaje: Math.round((pasosCompletados / pasos.length) * 100),
      perfilIncompleto: proximoPaso !== null,
      proximoPaso,
    };
  }, [nombre, apellido, ciudadSlug, deportesElegidos, favoritos, cantidadSiguiendo]);
}

function obtenerIniciales(nombre: string): string {
  const iniciales = nombre
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((parte) => parte.charAt(0).toLocaleUpperCase("es"))
    .join("");

  return iniciales || "?";
}

/* "mar-del-plata" → "Mar del Plata": las palabras cortas quedan en minúscula. */
function formatearSlugCiudad(slug: string | null): string | null {
  if (!slug) {
    return null;
  }

  return slug
    .split("-")
    .map((parte, indice) =>
      indice === 0 || parte.length > 3
        ? parte.charAt(0).toUpperCase() + parte.slice(1)
        : parte
    )
    .join(" ");
}

function suscripcionVacia() {
  return () => {};
}

function leerSlugCiudadGuardadaSinFallar(): string | null {
  try {
    return leerSlugCiudadGuardada();
  } catch {
    return null;
  }
}
