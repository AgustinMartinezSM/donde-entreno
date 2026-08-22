"use client";

import { useSyncExternalStore } from "react";

import {
  darLikeFotoCuenta,
  obtenerLikesFotosCuenta,
  quitarLikeFotoCuenta,
} from "../services/cuentaSyncService";

/*
  Likes en fotos (bloque 14): estado en memoria de la pestaña con el
  patrón optimista-con-reversa de favoritos. Los ids con like propio se
  cargan UNA vez por sesión de pestaña; cada toggle actualiza local al
  instante y empuja al backend — si el backend rechaza, se revierte.

  Sin persistencia local a propósito: el contador público viene del
  backend en cada carga, y un Set en memoria alcanza para pintar los
  corazones de la sesión.
*/

let idsConLike: Set<number> | null = null;
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
  snapshot = { version, ids: new Set(idsConLike ?? []) };

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

export function useLikesFotos(): Set<number> {
  return useSyncExternalStore(
    suscribir,
    () => snapshot,
    () => SNAPSHOT_SERVIDOR
  ).ids;
}

/** Carga los ids propios una sola vez por pestaña (idempotente). */
export function cargarLikesFotos(accessToken: string): void {
  if (idsConLike !== null || cargaEnVuelo !== null) {
    return;
  }

  cargaEnVuelo = obtenerLikesFotosCuenta(accessToken)
    .then((ids) => {
      idsConLike = new Set(ids);
      notificar();
    })
    .catch(() => {
      /* Backend viejo o sin red: los corazones quedan apagados. */
    })
    .finally(() => {
      cargaEnVuelo = null;
    });
}

/**
 * Alterna el like con actualización optimista y reversa si el backend
 * rechaza. Devuelve el estado local resultante (para el contador).
 */
export function toggleLikeFoto(accessToken: string, imagenId: number): boolean {
  if (idsConLike === null) {
    idsConLike = new Set();
  }

  const teniaLike = idsConLike.has(imagenId);

  if (teniaLike) {
    idsConLike.delete(imagenId);
  } else {
    idsConLike.add(imagenId);
  }
  notificar();

  const llamada = teniaLike
    ? quitarLikeFotoCuenta(accessToken, imagenId)
    : darLikeFotoCuenta(accessToken, imagenId);

  llamada.catch(() => {
    /* Reversa: el backend no lo tomó, el corazón vuelve a su estado. */
    if (idsConLike !== null) {
      if (teniaLike) {
        idsConLike.add(imagenId);
      } else {
        idsConLike.delete(imagenId);
      }
      notificar();
    }
  });

  return !teniaLike;
}

/** Al cerrar sesión, los corazones de la pestaña se apagan. */
export function limpiarLikesFotos(): void {
  idsConLike = null;
  notificar();
}
