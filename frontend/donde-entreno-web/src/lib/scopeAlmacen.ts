"use client";

/*
  A quién pertenece lo que se guarda en este dispositivo.

  Los datos locales (favoritos) vivían en una única clave de
  localStorage por navegador, así que dos cuentas distintas en la misma
  computadora compartían la lista: una cuenta recién creada abría
  "Guardados" y encontraba actividades que nunca guardó.

  El scope resuelve de quién es cada lista. Tiene tres estados y los tres
  importan:

  - null      → todavía no sabemos si hay sesión. No se lee nada: mostrar
                la lista equivocada por un instante es justamente el bug.
  - "guest"   → visitante sin cuenta.
  - "u<id>"   → una cuenta puntual.

  No hay migración entre scopes, y es a propósito: pasar lo del navegador
  a la primera cuenta que se loguee es exactamente cómo se contaminan las
  cuentas nuevas. Si algún día se quiere importar, tiene que ser una
  acción explícita de la persona.
*/

export type ScopeAlmacen = string | null;

const EVENTO_SCOPE = "dondeentreno:scope-almacen";

export const SCOPE_INVITADO = "guest";

let scopeActual: ScopeAlmacen = null;

export function obtenerScopeAlmacen(): ScopeAlmacen {
  return scopeActual;
}

/*
  Lo llama el proveedor de sesión cuando resuelve quién está usando la
  app (y de nuevo en cada login/logout). El evento hace que las listas ya
  montadas se vuelvan a leer con la clave del nuevo dueño.
*/
export function establecerScopeAlmacen(scope: ScopeAlmacen) {
  if (scope === scopeActual) {
    return;
  }

  scopeActual = scope;

  if (typeof window !== "undefined") {
    window.dispatchEvent(new Event(EVENTO_SCOPE));
  }
}

export function suscribirScopeAlmacen(callback: () => void): () => void {
  if (typeof window === "undefined") {
    return () => {};
  }

  window.addEventListener(EVENTO_SCOPE, callback);

  return () => {
    window.removeEventListener(EVENTO_SCOPE, callback);
  };
}

/*
  Identificador estable de una cuenta. El id numérico es el que manda; el
  email normalizado queda de respaldo por si algún día llegara una sesión
  sin id.
*/
export function crearScopeDeUsuario(usuario: {
  id?: number | null;
  email?: string | null;
}): string {
  if (typeof usuario.id === "number" && Number.isFinite(usuario.id)) {
    return `u${usuario.id}`;
  }

  const email = usuario.email?.trim().toLocaleLowerCase("es");

  return email ? `e${email}` : SCOPE_INVITADO;
}

/*
  Clave real en localStorage.

  El visitante se queda con la clave histórica —sin sufijo— para no
  borrarle lo que ya tenía guardado en el navegador; las cuentas estrenan
  clave propia y por eso arrancan vacías, que es el comportamiento que se
  buscaba.
*/
export function componerClaveConScope(
  claveBase: string,
  scope: ScopeAlmacen
): string | null {
  if (scope === null) {
    return null;
  }

  return scope === SCOPE_INVITADO ? claveBase : `${claveBase}.${scope}`;
}
