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
import { obtenerAccessTokenAuth } from "../services/authService";
import { reemplazarDeportesCuenta } from "../services/cuentaSyncService";

function esSlug(valor: unknown): valor is string {
  return typeof valor === "string" && valor.length > 0;
}

const almacen = crearAlmacenLocal<string>(
  "dondeentreno.deportesFavoritos.v1",
  esSlug,
  { porUsuario: true }
);

/*
  Con sesión, cada toggle empuja el CONJUNTO completo al backend (el
  contrato de /api/usuario/deportes es reemplazo total). Optimista con
  reversa: si el backend rechaza, el conjunto local vuelve al anterior.
*/
export function alternarDeporteFavorito(slug: string): boolean {
  const actuales = almacen.leer();
  const quedoElegido = !actuales.includes(slug);
  const nuevos = quedoElegido
    ? [...actuales, slug]
    : actuales.filter((item) => item !== slug);

  almacen.escribir(nuevos);

  const token = obtenerAccessTokenAuth();

  if (token) {
    void reemplazarDeportesCuenta(token, nuevos).catch(() => {
      almacen.escribir(actuales);
    });
  }

  return quedoElegido;
}

export function leerDeportesFavoritos(): string[] {
  return almacen.leer();
}

/* Solo para el sincronizador de cuenta: pisa la cache con el backend. */
export function reemplazarDeportesDesdeCuenta(slugs: string[]) {
  almacen.escribir(slugs);
}

export function useDeportesFavoritos(): string[] {
  return useSyncExternalStore(
    almacen.suscribir,
    almacen.obtenerSnapshot,
    almacen.obtenerSnapshotServidor
  );
}
