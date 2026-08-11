"use client";

/*
  Motor genérico sobre localStorage del navegador para features V1 sin backend
  (favoritos, me gusta). Guarda listas en localStorage y notifica cambios
  para que los componentes se mantengan sincronizados entre sí y entre
  pestañas (evento "storage" del navegador + evento propio en la misma
  pestaña).

  Está pensado para usarse con useSyncExternalStore: expone un snapshot
  cacheado (misma referencia mientras no haya cambios) y un snapshot de
  servidor vacío para que el HTML de SSR no dependa del dispositivo.

  Con `porUsuario`, la clave deja de ser fija y pasa a depender de quién
  está usando la app (ver scopeAlmacen.ts): sin eso, dos cuentas en la
  misma computadora comparten la misma lista.
*/

import {
  componerClaveConScope,
  obtenerScopeAlmacen,
  suscribirScopeAlmacen,
} from "./scopeAlmacen";

type Suscriptor = () => void;

export type AlmacenLocal<T> = {
  leer: () => T[];
  escribir: (items: T[]) => void;
  suscribir: (callback: Suscriptor) => () => void;
  obtenerSnapshot: () => T[];
  obtenerSnapshotServidor: () => T[];
};

type OpcionesAlmacen = {
  /*
    true: cada cuenta tiene su propia lista y, mientras no se sepa si hay
    sesión, no se lee ninguna.
  */
  porUsuario?: boolean;
};

const SNAPSHOT_SERVIDOR: never[] = [];

export function crearAlmacenLocal<T>(
  claveBase: string,
  esItemValido: (valor: unknown) => valor is T,
  opciones: OpcionesAlmacen = {}
): AlmacenLocal<T> {
  const eventoLocal = `dondeentreno:almacen:${claveBase}`;
  const porUsuario = opciones.porUsuario === true;

  let cache: T[] = SNAPSHOT_SERVIDOR;
  /* De qué clave salió la cache: si cambió el dueño, hay que releer. */
  let claveDeLaCache: string | null = null;
  let cacheInicializada = false;

  function puedeUsarStorage(): boolean {
    return typeof window !== "undefined" && "localStorage" in window;
  }

  /*
    null significa "no corresponde tocar el storage": o no hay navegador,
    o todavía no sabemos de quién es la lista.
  */
  function claveActual(): string | null {
    if (!porUsuario) {
      return claveBase;
    }

    return componerClaveConScope(claveBase, obtenerScopeAlmacen());
  }

  function leerDeStorage(clave: string | null): T[] {
    if (!puedeUsarStorage() || clave === null) {
      return SNAPSHOT_SERVIDOR;
    }

    try {
      const crudo = window.localStorage.getItem(clave);

      if (!crudo) {
        return SNAPSHOT_SERVIDOR;
      }

      const parseado: unknown = JSON.parse(crudo);

      if (!Array.isArray(parseado)) {
        return SNAPSHOT_SERVIDOR;
      }

      return parseado.filter(esItemValido);
    } catch {
      // Storage bloqueado o JSON corrupto: arrancamos vacío sin romper la UI.
      return SNAPSHOT_SERVIDOR;
    }
  }

  function refrescarCache() {
    const clave = claveActual();
    cache = leerDeStorage(clave);
    claveDeLaCache = clave;
    cacheInicializada = true;
  }

  function leer(): T[] {
    /*
      Releemos también cuando cambió el dueño: si no, alguien que cierra
      sesión seguiría viendo en pantalla la lista de la cuenta anterior
      hasta recargar.
    */
    if (!cacheInicializada || claveDeLaCache !== claveActual()) {
      refrescarCache();
    }

    return cache;
  }

  function escribir(items: T[]) {
    const clave = claveActual();

    /* Sin dueño resuelto no se escribe: iría a parar a la lista equivocada. */
    if (clave === null) {
      return;
    }

    cache = items;
    claveDeLaCache = clave;
    cacheInicializada = true;

    if (puedeUsarStorage()) {
      try {
        window.localStorage.setItem(clave, JSON.stringify(items));
      } catch {
        // Sin espacio o storage bloqueado: el estado sigue vivo en memoria.
      }

      window.dispatchEvent(new Event(eventoLocal));
    }
  }

  function suscribir(callback: Suscriptor) {
    if (typeof window === "undefined") {
      return () => {};
    }

    const alCambiarEnOtraPestania = (evento: StorageEvent) => {
      // key === null significa que se limpió todo el storage.
      if (evento.key === claveActual() || evento.key === null) {
        refrescarCache();
        callback();
      }
    };

    const alCambiarEnEstaPestania = () => {
      callback();
    };

    const alCambiarDeDuenio = () => {
      refrescarCache();
      callback();
    };

    window.addEventListener("storage", alCambiarEnOtraPestania);
    window.addEventListener(eventoLocal, alCambiarEnEstaPestania);
    const desuscribirScope = porUsuario
      ? suscribirScopeAlmacen(alCambiarDeDuenio)
      : () => {};

    return () => {
      window.removeEventListener("storage", alCambiarEnOtraPestania);
      window.removeEventListener(eventoLocal, alCambiarEnEstaPestania);
      desuscribirScope();
    };
  }

  return {
    leer,
    escribir,
    suscribir,
    obtenerSnapshot: leer,
    obtenerSnapshotServidor: () => SNAPSHOT_SERVIDOR,
  };
}

export type BanderaLocal = {
  leer: () => boolean;
  marcar: () => void;
  suscribir: (callback: Suscriptor) => () => void;
  obtenerSnapshot: () => boolean;
  obtenerSnapshotServidor: () => boolean;
};

/*
  Un sí/no persistente, con las mismas garantías que las listas: se
  sincroniza entre pestañas y, si es por usuario, cada cuenta tiene el
  suyo.
*/
export function crearBanderaLocal(
  claveBase: string,
  opciones: OpcionesAlmacen = {}
): BanderaLocal {
  const almacen = crearAlmacenLocal<true>(
    claveBase,
    (valor): valor is true => valor === true,
    opciones
  );

  return {
    leer: () => almacen.leer().length > 0,
    marcar: () => almacen.escribir([true]),
    suscribir: almacen.suscribir,
    obtenerSnapshot: () => almacen.obtenerSnapshot().length > 0,
    obtenerSnapshotServidor: () => false,
  };
}
