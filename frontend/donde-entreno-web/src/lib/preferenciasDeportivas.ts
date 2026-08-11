"use client";

/*
  Preferencias deportivas V1 (local): los deportes favoritos del
  usuario viven en localStorage de este dispositivo, igual que los
  favoritos de actividades. Cuando exista backend de preferencias,
  este módulo es el único punto a reemplazar: los componentes
  consumen los hooks.

  **Cada cuenta elige los suyos** (`porUsuario`), por lo mismo que los
  favoritos: con una sola clave por navegador, una cuenta nueva heredaba
  los deportes de otra persona que hubiera usado la misma computadora, y
  además arrancaba con ese paso del perfil dado por hecho. El visitante
  conserva la clave histórica — ver scopeAlmacen.ts.

  Alimentan los accesos rápidos del perfil, los chips de la Home y las
  recomendaciones de "Para vos".
*/

import { useSyncExternalStore } from "react";
import { crearAlmacenLocal } from "./almacenLocal";

function esSlug(valor: unknown): valor is string {
  return typeof valor === "string" && valor.length > 0;
}

const almacen = crearAlmacenLocal<string>(
  "dondeentreno.deportesFavoritos.v1",
  esSlug,
  { porUsuario: true }
);

export function alternarDeporteFavorito(slug: string): boolean {
  const actuales = almacen.leer();

  if (actuales.includes(slug)) {
    almacen.escribir(actuales.filter((item) => item !== slug));
    return false;
  }

  almacen.escribir([...actuales, slug]);
  return true;
}

export function useDeportesFavoritos(): string[] {
  return useSyncExternalStore(
    almacen.suscribir,
    almacen.obtenerSnapshot,
    almacen.obtenerSnapshotServidor
  );
}
