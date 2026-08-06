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
*/

type Suscriptor = () => void;

export type AlmacenLocal<T> = {
  leer: () => T[];
  escribir: (items: T[]) => void;
  suscribir: (callback: Suscriptor) => () => void;
  obtenerSnapshot: () => T[];
  obtenerSnapshotServidor: () => T[];
};

const SNAPSHOT_SERVIDOR: never[] = [];

export function crearAlmacenLocal<T>(
  clave: string,
  esItemValido: (valor: unknown) => valor is T
): AlmacenLocal<T> {
  const eventoLocal = `dondeentreno:almacen:${clave}`;

  let cache: T[] = SNAPSHOT_SERVIDOR;
  let cacheInicializada = false;

  function puedeUsarStorage(): boolean {
    return typeof window !== "undefined" && "localStorage" in window;
  }

  function leerDeStorage(): T[] {
    if (!puedeUsarStorage()) {
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
    cache = leerDeStorage();
    cacheInicializada = true;
  }

  function leer(): T[] {
    if (!cacheInicializada) {
      refrescarCache();
    }

    return cache;
  }

  function escribir(items: T[]) {
    cache = items;
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
      if (evento.key === clave || evento.key === null) {
        refrescarCache();
        callback();
      }
    };

    const alCambiarEnEstaPestania = () => {
      callback();
    };

    window.addEventListener("storage", alCambiarEnOtraPestania);
    window.addEventListener(eventoLocal, alCambiarEnEstaPestania);

    return () => {
      window.removeEventListener("storage", alCambiarEnOtraPestania);
      window.removeEventListener(eventoLocal, alCambiarEnEstaPestania);
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
