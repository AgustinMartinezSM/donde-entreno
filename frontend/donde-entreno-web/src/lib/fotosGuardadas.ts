"use client";

import { useSyncExternalStore } from "react";

import {
  guardarFoto,
  obtenerIdsFotosGuardadas,
  quitarFotoGuardada,
} from "../services/galeriaSocialService";

/*
  Fotos guardadas (fase 4 social): mismo patrón que likesFotos — Set en
  memoria de la pestaña, carga única, toggle optimista con reversa. El
  guardado real vive en el backend (tabla foto_guardada).
*/

let idsGuardados: Set<number> | null = null;
let cargaEnVuelo: Promise<void> | null = null;
let version = 0;
/* Snapshot inmutable por versión: useSyncExternalStore compara identidad. */
let snapshot: { version: number; ids: Set<number> } = {
  version,
  ids: new Set(),
};

const suscriptores = new Set<() => void>();

function notificar() {
  version += 1;
  snapshot = { version, ids: new Set(idsGuardados ?? []) };

  for (const suscriptor of suscriptores) {
    suscriptor();
  }
}

function suscribir(callback: () => void): () => void {
  suscriptores.add(callback);
  return () => {
    suscriptores.delete(callback);
  };
}

const SNAPSHOT_SERVIDOR = { version: -1, ids: new Set<number>() };

export function useFotosGuardadas(): Set<number> {
  return useSyncExternalStore(
    suscribir,
    () => snapshot,
    () => SNAPSHOT_SERVIDOR
  ).ids;
}

/** Carga los ids guardados una sola vez por pestaña (idempotente). */
export function cargarFotosGuardadas(accessToken: string): void {
  if (idsGuardados !== null || cargaEnVuelo !== null) {
    return;
  }

  cargaEnVuelo = obtenerIdsFotosGuardadas(accessToken)
    .then((ids) => {
      idsGuardados = new Set(ids);
      notificar();
    })
    .catch(() => {
      /* Backend viejo o sin red: los bookmarks quedan apagados. */
    })
    .finally(() => {
      cargaEnVuelo = null;
    });
}

/**
 * Alterna el guardado con actualización optimista y reversa si el
 * backend rechaza. Devuelve el estado local resultante.
 */
export function toggleFotoGuardada(
  accessToken: string,
  imagenId: number
): boolean {
  if (idsGuardados === null) {
    idsGuardados = new Set();
  }

  const estabaGuardada = idsGuardados.has(imagenId);

  if (estabaGuardada) {
    idsGuardados.delete(imagenId);
  } else {
    idsGuardados.add(imagenId);
  }
  notificar();

  const llamada = estabaGuardada
    ? quitarFotoGuardada(accessToken, imagenId)
    : guardarFoto(accessToken, imagenId);

  llamada.catch(() => {
    /* Reversa: el backend no lo tomó, el bookmark vuelve a su estado. */
    if (idsGuardados !== null) {
      if (estabaGuardada) {
        idsGuardados.add(imagenId);
      } else {
        idsGuardados.delete(imagenId);
      }
      notificar();
    }
  });

  return !estabaGuardada;
}

/** Al cerrar sesión, los bookmarks de la pestaña se apagan. */
export function limpiarFotosGuardadas(): void {
  idsGuardados = null;
  notificar();
}
