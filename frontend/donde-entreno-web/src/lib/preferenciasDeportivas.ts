"use client";

/*
  Preferencias deportivas V1 (local): los deportes favoritos del
  usuario viven en localStorage de este dispositivo, igual que los
  favoritos de actividades. Cuando exista backend de preferencias,
  este módulo es el único punto a reemplazar: los componentes
  consumen los hooks.

  Hoy alimentan los accesos rápidos de Mi cuenta; más adelante pueden
  personalizar la Home y las respuestas del asistente.
*/

import { useSyncExternalStore } from "react";
import { crearAlmacenLocal } from "./almacenLocal";

function esSlug(valor: unknown): valor is string {
  return typeof valor === "string" && valor.length > 0;
}

const almacen = crearAlmacenLocal<string>(
  "dondeentreno.deportesFavoritos.v1",
  esSlug
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
