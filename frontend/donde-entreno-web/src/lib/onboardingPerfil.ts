"use client";

/*
  Si la guía "Completá tu perfil deportivo" ya cumplió su función.

  El progreso se calcula en vivo con lo que la persona tiene hecho
  (ciudad, deportes, seguidos, guardados), y eso está bien para mostrar
  cuánto le falta. El problema es usarlo también para decidir si la
  tarjeta se muestra: alguien que completó los cinco pasos y después
  quitaba un favorito veía reaparecer el instructivo de bienvenida, como
  si volviera a ser nuevo en la app.

  Por eso el hecho de haber terminado se guarda aparte y no se recalcula.
  Una vez marcado, la guía no vuelve. Se marca de dos maneras: al
  completar los pasos, o cuando la persona la descarta con "Ahora no".

  Es por cuenta (ver scopeAlmacen.ts): una cuenta nueva en la misma
  computadora tiene que ver su guía.
*/

import { useSyncExternalStore } from "react";
import { crearBanderaLocal } from "./almacenLocal";

const bandera = crearBanderaLocal(
  "dondeentreno.onboardingPerfilDeportivoResuelto.v1",
  { porUsuario: true }
);

export function marcarOnboardingPerfilResuelto() {
  bandera.marcar();
}

export function useOnboardingPerfilResuelto(): boolean {
  return useSyncExternalStore(
    bandera.suscribir,
    bandera.obtenerSnapshot,
    bandera.obtenerSnapshotServidor
  );
}
