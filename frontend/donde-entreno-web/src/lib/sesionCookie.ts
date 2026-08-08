/*
  Cookie liviana de sesión para el guard de rutas (src/proxy.ts).

  IMPORTANTE (seguridad): esta cookie NO es una frontera de seguridad.
  Solo guarda el rol y el vencimiento para mejorar la UX (redirigir antes
  de servir el HTML de páginas privadas). Nunca guarda el JWT.
  La protección real de los datos es la validación del token en el
  backend: cualquier request a /api/admin/** o /api/publicador/** sin un
  JWT válido devuelve 401/403 aunque alguien falsifique esta cookie.
*/

export const NOMBRE_COOKIE_SESION = "de_sesion";

export type DatosCookieSesion = {
  rol: string;
  expiresAt: number;
};

export function serializarValorCookieSesion(datos: DatosCookieSesion): string {
  return `${datos.rol}.${datos.expiresAt}`;
}

/*
  Parsea el valor de la cookie. Devuelve null si falta, está mal formada
  o ya venció: para el guard eso equivale a "sin sesión".
*/
export function parsearValorCookieSesion(
  valor: string | undefined
): DatosCookieSesion | null {
  if (!valor) {
    return null;
  }

  const separador = valor.lastIndexOf(".");

  if (separador <= 0) {
    return null;
  }

  const rol = valor.slice(0, separador);
  const expiresAt = Number(valor.slice(separador + 1));

  if (!rol || !Number.isFinite(expiresAt)) {
    return null;
  }

  if (expiresAt <= Date.now()) {
    return null;
  }

  return { rol, expiresAt };
}

/*
  Sincroniza la cookie con la sesión actual (solo en el navegador).
  Con null la borra (logout o sesión vencida).
*/
export function sincronizarCookieSesion(
  sesion: { expiresAt: number; usuario: { rol: string } } | null
) {
  if (typeof document === "undefined") {
    return;
  }

  if (!sesion || sesion.expiresAt <= Date.now()) {
    document.cookie = `${NOMBRE_COOKIE_SESION}=; ${atributosCookie(0)}`;
    return;
  }

  const maxAgeSegundos = Math.max(
    0,
    Math.floor((sesion.expiresAt - Date.now()) / 1000)
  );
  const valor = serializarValorCookieSesion({
    rol: sesion.usuario.rol,
    expiresAt: sesion.expiresAt,
  });

  document.cookie = `${NOMBRE_COOKIE_SESION}=${valor}; ${atributosCookie(
    maxAgeSegundos
  )}`;
}

/*
  Atributos comunes de la cookie, compartidos entre el alta y el borrado
  para que el borrado matchee siempre con lo que se escribió.

  Secure va condicionado al protocolo: en producción impide que la
  cookie viaje por una conexión sin cifrar, y en el dev local
  (http://localhost) ponerla siempre haría que el navegador descarte la
  escritura y el guard de rutas privadas dejara de funcionar.
*/
function atributosCookie(maxAgeSegundos: number) {
  const seguro = window.location.protocol === "https:" ? "; Secure" : "";

  return `path=/; max-age=${maxAgeSegundos}; SameSite=Lax${seguro}`;
}
