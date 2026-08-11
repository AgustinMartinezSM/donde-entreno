"use client";

/*
  Favoritos V1 (local): las actividades guardadas viven en localStorage
  de este dispositivo. Guardamos un snapshot de los datos visibles de la
  card para poder renderizar "Mis favoritos" sin depender de la API.

  **Cada cuenta tiene su propia lista** (`porUsuario`). Con una sola
  clave por navegador, una cuenta recién creada abría "Guardados" y
  encontraba actividades de otra persona que había usado la misma
  computadora. El visitante sin cuenta conserva la clave histórica, así
  que no pierde lo que ya tenía; las cuentas estrenan lista propia y no
  heredan nada — ver scopeAlmacen.ts.

  Cuando exista sincronización con backend/cuenta de usuario, este módulo
  es el único punto a reemplazar: los componentes consumen los hooks.
*/

import { useSyncExternalStore } from "react";
import { crearAlmacenLocal } from "./almacenLocal";
import { obtenerScopeAlmacen, suscribirScopeAlmacen } from "./scopeAlmacen";

export type FavoritoGuardado = {
  slug: string;
  titulo: string;
  deporteNombre?: string;
  deporteSlug?: string;
  ciudadNombre?: string;
  barrioNombre?: string;
  imagenPrincipalUrl?: string | null;
  nivel?: string;
  modalidad?: string;
  precioReferencia?: number | null;
  mostrarPrecio?: boolean;
  guardadoEn: string;
};

export type DatosFavorito = Omit<FavoritoGuardado, "guardadoEn">;

function esStringOpcional(valor: unknown): boolean {
  return valor === undefined || typeof valor === "string";
}

function esStringNullOpcional(valor: unknown): boolean {
  return valor === undefined || valor === null || typeof valor === "string";
}

function esNumberNullOpcional(valor: unknown): boolean {
  return valor === undefined || valor === null || typeof valor === "number";
}

function esBooleanOpcional(valor: unknown): boolean {
  return valor === undefined || typeof valor === "boolean";
}

/*
  Valida TODOS los campos del snapshot (también los opcionales): una
  entrada corrupta en localStorage con tipos incorrectos no debe pasar
  el filtro, porque rompería el render de /favoritos en cada visita.
*/
function esFavoritoGuardado(valor: unknown): valor is FavoritoGuardado {
  if (typeof valor !== "object" || valor === null || Array.isArray(valor)) {
    return false;
  }

  const objeto = valor as Record<string, unknown>;

  return (
    typeof objeto.slug === "string" &&
    objeto.slug.length > 0 &&
    typeof objeto.titulo === "string" &&
    typeof objeto.guardadoEn === "string" &&
    esStringOpcional(objeto.deporteNombre) &&
    esStringOpcional(objeto.deporteSlug) &&
    esStringOpcional(objeto.ciudadNombre) &&
    esStringOpcional(objeto.barrioNombre) &&
    esStringOpcional(objeto.nivel) &&
    esStringOpcional(objeto.modalidad) &&
    esStringNullOpcional(objeto.imagenPrincipalUrl) &&
    esNumberNullOpcional(objeto.precioReferencia) &&
    esBooleanOpcional(objeto.mostrarPrecio)
  );
}

const almacen = crearAlmacenLocal<FavoritoGuardado>(
  "dondeentreno.favoritos.v1",
  esFavoritoGuardado,
  { porUsuario: true }
);

export function leerFavoritos(): FavoritoGuardado[] {
  return almacen.leer();
}

export function esFavorito(slug: string): boolean {
  return almacen.leer().some((favorito) => favorito.slug === slug);
}

/*
  Alterna el estado de favorito y devuelve el estado final:
  true = quedó guardado, false = quedó quitado.
*/
export function alternarFavorito(datos: DatosFavorito): boolean {
  const actuales = almacen.leer();

  if (actuales.some((favorito) => favorito.slug === datos.slug)) {
    almacen.escribir(
      actuales.filter((favorito) => favorito.slug !== datos.slug)
    );
    return false;
  }

  const nuevo: FavoritoGuardado = {
    ...datos,
    guardadoEn: new Date().toISOString(),
  };

  almacen.escribir([nuevo, ...actuales]);
  return true;
}

export function quitarFavorito(slug: string) {
  almacen.escribir(
    almacen.leer().filter((favorito) => favorito.slug !== slug)
  );
}

export function useFavoritos(): FavoritoGuardado[] {
  return useSyncExternalStore(
    almacen.suscribir,
    almacen.obtenerSnapshot,
    almacen.obtenerSnapshotServidor
  );
}

/*
  Si ya sabemos de quién es la lista.

  Mientras la sesión se resuelve, la lista está vacía a propósito (no por
  falta de datos), así que quien la muestre debe distinguir "todavía no
  sé" de "no guardaste nada": si no, la primera pantalla de alguien con
  favoritos es "Todavía no guardaste actividades".
*/
export function useScopeFavoritosResuelto(): boolean {
  return useSyncExternalStore(
    suscribirScopeAlmacen,
    () => obtenerScopeAlmacen() !== null,
    () => false
  );
}

export function useEsFavorito(slug: string): boolean {
  const favoritos = useFavoritos();
  return favoritos.some((favorito) => favorito.slug === slug);
}
