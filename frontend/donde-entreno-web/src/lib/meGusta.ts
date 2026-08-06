"use client";

/*
  "Me gusta" V1 (local): guardamos solo los slugs de las actividades que
  le gustaron a la persona en este dispositivo. No inventamos contadores
  globales: sin backend no hay métricas reales, así que la UI muestra
  únicamente el estado propio ("te gusta" / "no te gusta").

  Cuando haya backend de reacciones, reemplazar este módulo manteniendo
  los hooks como contrato.
*/

import { useSyncExternalStore } from "react";
import { crearAlmacenLocal } from "./almacenLocal";

function esSlug(valor: unknown): valor is string {
  return typeof valor === "string" && valor.length > 0;
}

const almacen = crearAlmacenLocal<string>("dondeentreno.meGusta.v1", esSlug);

export function tieneMeGusta(slug: string): boolean {
  return almacen.leer().includes(slug);
}

/*
  Alterna el "me gusta" y devuelve el estado final:
  true = ahora le gusta, false = se quitó.
*/
export function alternarMeGusta(slug: string): boolean {
  const actuales = almacen.leer();

  if (actuales.includes(slug)) {
    almacen.escribir(actuales.filter((item) => item !== slug));
    return false;
  }

  almacen.escribir([slug, ...actuales]);
  return true;
}

export function useMeGusta(): string[] {
  return useSyncExternalStore(
    almacen.suscribir,
    almacen.obtenerSnapshot,
    almacen.obtenerSnapshotServidor
  );
}

export function useTieneMeGusta(slug: string): boolean {
  const conMeGusta = useMeGusta();
  return conMeGusta.includes(slug);
}
