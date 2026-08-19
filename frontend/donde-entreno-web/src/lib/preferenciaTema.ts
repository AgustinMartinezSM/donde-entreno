/*
  Preferencia de apariencia: sistema / claro / oscuro.

  Vive en localStorage SIN scope por usuario, a propósito: el tema es
  del dispositivo, no de la cuenta. Con scope por usuario, cada carga
  esperaría a resolver la sesión antes de saber el tema y toda la app
  tendría un flash de claro (el mismo motivo por el que la ciudad activa
  tampoco lo tiene).

  El atributo data-theme de <html> lleva SIEMPRE el tema RESUELTO
  ("light" o "dark", nunca "system"): lo pone el script anti-FOUC de
  layout.tsx antes del primer paint, y estas funciones lo mantienen al
  cambiar la preferencia, el sistema o desde otra pestaña. El CSS de
  globals.css se cuelga de :root[data-theme="dark"] y nada más.
*/

export type PreferenciaTema = "system" | "light" | "dark";
export type TemaResuelto = "light" | "dark";

const CLAVE_TEMA = "dondeEntreno.tema";
const EVENTO_TEMA = "dondeentreno:tema";
const MEDIA_OSCURO = "(prefers-color-scheme: dark)";

/*
  Script que layout.tsx inyecta inline ANTES del contenido del body: se
  ejecuta bloqueando el primer paint, que es la única forma de que una
  visita con preferencia oscura no vea un flash claro. Tiene que estar
  sincronizado con CLAVE_TEMA y con la lógica de resolverTema.
*/
export const SCRIPT_TEMA_INICIAL = `(function(){try{var p=localStorage.getItem("${CLAVE_TEMA}");var t=(p==="dark"||p==="light")?p:(window.matchMedia("${MEDIA_OSCURO}").matches?"dark":"light");document.documentElement.dataset.theme=t;}catch(e){document.documentElement.dataset.theme="light";}})();`;

export function leerPreferenciaTema(): PreferenciaTema {
  if (typeof window === "undefined") {
    return "system";
  }

  try {
    const valor = window.localStorage.getItem(CLAVE_TEMA);

    return valor === "dark" || valor === "light" ? valor : "system";
  } catch {
    return "system";
  }
}

export function resolverTema(preferencia: PreferenciaTema): TemaResuelto {
  if (preferencia === "dark" || preferencia === "light") {
    return preferencia;
  }

  if (typeof window === "undefined") {
    return "light";
  }

  return window.matchMedia(MEDIA_OSCURO).matches ? "dark" : "light";
}

export function aplicarTemaResuelto() {
  if (typeof document === "undefined") {
    return;
  }

  document.documentElement.dataset.theme = resolverTema(leerPreferenciaTema());
}

export function guardarPreferenciaTema(preferencia: PreferenciaTema) {
  try {
    window.localStorage.setItem(CLAVE_TEMA, preferencia);
  } catch {
    /* Sin storage (modo privado estricto), el tema vale por esta visita. */
  }

  aplicarTemaResuelto();
  window.dispatchEvent(new Event(EVENTO_TEMA));
}

/*
  Suscripción para useSyncExternalStore y para el sincronizador global:
  reacciona al cambio local (evento propio), al de otra pestaña
  (storage) y al del sistema operativo (matchMedia, que solo altera el
  resultado cuando la preferencia es "system").
*/
export function suscribirseATema(callback: () => void): () => void {
  const media = window.matchMedia(MEDIA_OSCURO);

  window.addEventListener(EVENTO_TEMA, callback);
  window.addEventListener("storage", callback);
  media.addEventListener("change", callback);

  return () => {
    window.removeEventListener(EVENTO_TEMA, callback);
    window.removeEventListener("storage", callback);
    media.removeEventListener("change", callback);
  };
}
