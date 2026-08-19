"use client";

/*
  Sincronización de la cuenta al iniciar sesión (script 20).

  Qué hace, en orden:
  1. Trae favoritos y deportes del backend (la fuente de verdad).
  2. SIEMBRA ÚNICA (decisión de Agustín, docs/plan-sync-favoritos.md):
     si la cuenta no tiene NADA en el backend y este dispositivo sí
     tiene datos locales de esa cuenta (de la era pre-sync), los sube
     una sola vez. Si el backend ya tiene algo, gana el backend — así un
     borrado hecho en otro dispositivo no resucita acá. La marca de
     "ya sembrado" es local y por cuenta.
  3. Pisa la cache local con lo del backend: los snapshots quedan VIVOS
     (título/precio/imagen actuales, no los del día en que se guardó).

  Un fallo de red aborta en silencio sin marcar nada: la UI sigue con la
  cache local y el próximo inicio de sesión lo reintenta. La prohibición
  de migrar el scope del INVITADO sigue intacta: esto corre solo con
  scope de cuenta.
*/

import type { Actividad } from "../types/actividad";
import type { FavoritoGuardado } from "./favoritos";
import {
  obtenerFavoritosCuenta,
  guardarFavoritoCuenta,
  obtenerDeportesCuenta,
  reemplazarDeportesCuenta,
} from "../services/cuentaSyncService";
import {
  leerFavoritos,
  reemplazarFavoritosDesdeCuenta,
} from "./favoritos";
import {
  leerDeportesFavoritos,
  reemplazarDeportesDesdeCuenta,
} from "./preferenciasDeportivas";
import {
  componerClaveConScope,
  obtenerScopeAlmacen,
  SCOPE_INVITADO,
} from "./scopeAlmacen";

const CLAVE_BASE_SEMBRADO = "dondeentreno.syncCuenta.sembrado.v1";

export async function sincronizarConCuenta(accessToken: string): Promise<void> {
  const scope = obtenerScopeAlmacen();

  /* Solo cuentas: el invitado no tiene backend y no se migra jamás. */
  if (scope === null || scope === SCOPE_INVITADO) {
    return;
  }

  const claveSembrado = componerClaveConScope(CLAVE_BASE_SEMBRADO, scope);

  if (!claveSembrado || typeof window === "undefined") {
    return;
  }

  try {
    let [favoritosRemotos, deportesRemotos] = await Promise.all([
      obtenerFavoritosCuenta(accessToken),
      obtenerDeportesCuenta(accessToken),
    ]);

    const yaSembrado = window.localStorage.getItem(claveSembrado) === "1";

    if (!yaSembrado) {
      const huboSiembra = await sembrarSiCorresponde(
        accessToken,
        favoritosRemotos,
        deportesRemotos
      );

      /*
        La marca se pone recién acá: si la siembra murió a mitad de
        camino por red, el próximo login la reintenta (los PUT son
        idempotentes, repetir no duplica).
      */
      window.localStorage.setItem(claveSembrado, "1");

      if (huboSiembra) {
        [favoritosRemotos, deportesRemotos] = await Promise.all([
          obtenerFavoritosCuenta(accessToken),
          obtenerDeportesCuenta(accessToken),
        ]);
      }
    }

    reemplazarFavoritosDesdeCuenta(
      mapearFavoritosVivos(favoritosRemotos, leerFavoritos())
    );
    reemplazarDeportesDesdeCuenta(deportesRemotos);
  } catch {
    /* Sin red o backend caído: la cache local sigue mandando por ahora. */
  }
}

/**
 * Sube lo local SOLO si el backend está vacío para ese recurso.
 * Devuelve true si subió algo (y hay que re-leer del backend).
 */
async function sembrarSiCorresponde(
  accessToken: string,
  favoritosRemotos: Actividad[],
  deportesRemotos: string[]
): Promise<boolean> {
  let huboSiembra = false;

  if (favoritosRemotos.length === 0) {
    const locales = leerFavoritos();

    for (const favorito of locales) {
      try {
        await guardarFavoritoCuenta(accessToken, favorito.slug);
        huboSiembra = true;
      } catch {
        /*
          Un 404 acá es un favorito de una actividad que ya no existe o
          se despublicó: se saltea y no frena al resto.
        */
      }
    }
  }

  if (deportesRemotos.length === 0) {
    const locales = leerDeportesFavoritos();

    if (locales.length > 0) {
      await reemplazarDeportesCuenta(accessToken, locales);
      huboSiembra = true;
    }
  }

  return huboSiembra;
}

/*
  Convierte las cards del backend en snapshots locales. El backend manda
  los datos VIVOS; el guardadoEn se conserva del snapshot local si el
  slug ya estaba (es el dato que el backend no tiene), y para los nuevos
  se sintetiza respetando el orden del backend (más reciente primero).
*/
function mapearFavoritosVivos(
  remotos: Actividad[],
  locales: FavoritoGuardado[]
): FavoritoGuardado[] {
  const guardadoEnPorSlug = new Map(
    locales.map((favorito) => [favorito.slug, favorito.guardadoEn])
  );
  const ahora = Date.now();

  return remotos.map((actividad, indice) => ({
    slug: actividad.slug,
    titulo: actividad.titulo,
    deporteNombre: actividad.deporteNombre,
    deporteSlug: actividad.deporteSlug,
    ciudadNombre: actividad.ciudadNombre,
    barrioNombre: actividad.barrioNombre,
    imagenPrincipalUrl: actividad.imagenPrincipalUrl,
    nivel: actividad.nivel,
    modalidad: actividad.modalidad,
    precioReferencia: actividad.precioReferencia,
    mostrarPrecio: actividad.mostrarPrecio,
    guardadoEn:
      guardadoEnPorSlug.get(actividad.slug) ??
      new Date(ahora - indice).toISOString(),
  }));
}
