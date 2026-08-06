// Helpers compartidos por los services del frontend para consumir la API del backend.
//
// Acá viven solo utilidades genéricas: parseo defensivo de JSON, type guards
// estructurales, armado de headers/query strings y el flujo común de un request.
// Las clases de error y los textos de mensaje de cada service quedan en su propio
// módulo: cuando un helper necesita lanzar un error, recibe una "fábrica" de error
// como parámetro para no acoplar este módulo a ningún service puntual.

/**
 * Errores de validación por campo tal como los serializa el backend
 * (Bean Validation): un objeto cuyos valores son siempre strings.
 */
export type ErroresPorCampoApi = Record<string, string>;

/**
 * Forma estructural común de las respuestas de error del backend.
 * La comparten los endpoints de auth, panel admin, panel publicador y
 * solicitudes de publicación; cada service la re-expone con su propio alias
 * (AuthErrorResponse, AdminErrorResponse, etc.), todos estructuralmente idénticos.
 */
export type ErrorResponseApi = {
  status: number;
  error: string;
  mensaje: string;
  errores: ErroresPorCampoApi | null;
  path: string;

  // OffsetDateTime serializado.
  timestamp: string;
};

/**
 * Fábricas de error que cada service provee a `ejecutarRequestJson` para
 * conservar su propia clase de error y sus propios mensajes.
 */
export type ManejadoresErrorRequest = {
  /**
   * Se invoca cuando el fetch falla (red caída, CORS, etc.). Recibe el error
   * original para que el service pueda re-lanzar errores propios tal cual.
   */
  crearErrorConexion: (error: unknown) => Error;

  /**
   * Se invoca cuando el backend responde con un status no-OK.
   * Recibe el cuerpo ya parseado (o null si no era JSON válido).
   */
  crearErrorHttp: (status: number, cuerpo: unknown) => Error;

  /** Se invoca cuando la respuesta OK no pasa el validador de formato. */
  crearErrorFormatoInvalido: (status: number) => Error;
};

// ---------------------------------------------------------------------------
// Type guards genéricos
// ---------------------------------------------------------------------------

/** Confirma que el valor es un objeto plano (no null y no array). */
export function esObjeto(valor: unknown): valor is Record<string, unknown> {
  return typeof valor === "object" && valor !== null && !Array.isArray(valor);
}

/** Confirma que el valor es un string o null (campos opcionales del backend). */
export function esStringONull(valor: unknown): valor is string | null {
  return typeof valor === "string" || valor === null;
}

/** Confirma que el valor es un number o null (campos opcionales del backend). */
export function esNumberONull(valor: unknown): valor is number | null {
  return typeof valor === "number" || valor === null;
}

/** Confirma que el valor es un boolean o null (campos opcionales del backend). */
export function esBooleanONull(valor: unknown): valor is boolean | null {
  return typeof valor === "boolean" || valor === null;
}

/**
 * Type guard genérico para catálogos de literales declarados con `as const`:
 * confirma que `valor` es uno de los strings de `lista`.
 *
 * Reemplaza a los guards repetidos por service (estados, días de semana, etc.)
 * sin necesidad de casts.
 */
export function esValorDeLista<T extends string>(
  valor: unknown,
  lista: readonly T[]
): valor is T {
  return typeof valor === "string" && lista.some((item) => item === valor);
}

/**
 * Confirma que el valor tiene la forma de errores por campo del backend:
 * un objeto donde todos los valores son strings.
 */
export function esErroresPorCampoApi(
  valor: unknown
): valor is ErroresPorCampoApi {
  if (!esObjeto(valor)) {
    return false;
  }

  return Object.values(valor).every((mensaje) => typeof mensaje === "string");
}

/**
 * Confirma que el valor tiene la forma estándar de respuesta de error del
 * backend (status, error, mensaje, errores por campo, path y timestamp).
 */
export function esErrorResponseApi(valor: unknown): valor is ErrorResponseApi {
  return (
    esObjeto(valor) &&
    typeof valor.status === "number" &&
    typeof valor.error === "string" &&
    typeof valor.mensaje === "string" &&
    (valor.errores === null || esErroresPorCampoApi(valor.errores)) &&
    typeof valor.path === "string" &&
    typeof valor.timestamp === "string"
  );
}

// ---------------------------------------------------------------------------
// Flujo común de requests
// ---------------------------------------------------------------------------

/**
 * Lee el body JSON de una respuesta HTTP de forma segura.
 * Devuelve null si el body está vacío o no es JSON válido, en lugar de lanzar.
 */
export async function leerJsonSeguro(respuesta: Response): Promise<unknown> {
  try {
    const cuerpo: unknown = await respuesta.json();
    return cuerpo;
  } catch {
    return null;
  }
}

/**
 * Arma el valor del header Authorization con esquema Bearer.
 * Si el token está vacío (o es solo espacios), lanza el error que construya
 * `crearErrorSinSesion`; así cada service conserva su clase y mensaje propios.
 */
export function construirAuthorization(
  accessToken: string,
  crearErrorSinSesion: () => Error
): string {
  const token = accessToken.trim();

  if (!token) {
    throw crearErrorSinSesion();
  }

  return `Bearer ${token}`;
}

/**
 * Valida que un ID sea un entero positivo antes de interpolarlo en una URL.
 * Si no lo es, lanza el error que construya `crearErrorIdInvalido`.
 */
export function validarIdPositivo(
  id: number,
  crearErrorIdInvalido: () => Error
): number {
  if (!Number.isInteger(id) || id <= 0) {
    throw crearErrorIdInvalido();
  }

  return id;
}

/** Parámetros comunes de los listados paginados de la API. */
export type ParamsListadoPaginado = {
  estado?: string;
  page?: number;
  size?: number;
  orden?: string;
};

/**
 * Arma el sufijo de query string para listados paginados, siempre en el orden
 * estado → page → size → orden (solo incluye los parámetros con valor).
 * Devuelve "" si no hay ninguno, o "?clave=valor&..." si hay al menos uno.
 */
export function construirQueryListado(params: ParamsListadoPaginado): string {
  const parametros = new URLSearchParams();

  if (params.estado) {
    parametros.set("estado", params.estado);
  }

  if (typeof params.page === "number" && Number.isFinite(params.page)) {
    parametros.set("page", String(params.page));
  }

  if (typeof params.size === "number" && Number.isFinite(params.size)) {
    parametros.set("size", String(params.size));
  }

  if (params.orden) {
    parametros.set("orden", params.orden);
  }

  const queryString = parametros.toString();

  return queryString ? `?${queryString}` : "";
}

/**
 * Ejecuta un request JSON contra el backend con el flujo común de los services:
 *
 * 1. fetch (si falla la conexión → `crearErrorConexion`)
 * 2. parseo defensivo del body con `leerJsonSeguro`
 * 3. si el status no es OK → `crearErrorHttp`
 * 4. si la respuesta OK no pasa el `validador` de formato → `crearErrorFormatoInvalido`
 *
 * Los errores siempre se construyen con las fábricas de `manejadores`, así cada
 * service conserva su clase de error (AuthApiError, AdminApiError, etc.).
 */
export async function ejecutarRequestJson<T>(
  url: string,
  opciones: RequestInit,
  validador: (valor: unknown) => valor is T,
  manejadores: ManejadoresErrorRequest
): Promise<T> {
  let respuestaHttp: Response;

  try {
    respuestaHttp = await fetch(url, opciones);
  } catch (error: unknown) {
    throw manejadores.crearErrorConexion(error);
  }

  const cuerpo: unknown = await leerJsonSeguro(respuestaHttp);

  if (!respuestaHttp.ok) {
    throw manejadores.crearErrorHttp(respuestaHttp.status, cuerpo);
  }

  if (!validador(cuerpo)) {
    throw manejadores.crearErrorFormatoInvalido(respuestaHttp.status);
  }

  return cuerpo;
}

// ---------------------------------------------------------------------------
// Lectores defensivos para catálogos públicos (ciudades, deportes, etc.)
// ---------------------------------------------------------------------------
//
// Nota: los pares "Requerido" / "Opcional" son equivalentes en comportamiento
// (ambos devuelven null ante undefined/null), pero se mantienen los dos nombres
// porque expresan intenciones distintas en los parseos: un null de un campo
// "requerido" invalida el objeto completo, mientras que un campo "opcional"
// simplemente queda en null.

/** Devuelve el string recortado si es no vacío; null en cualquier otro caso. */
export function leerTextoRequerido(valor: unknown): string | null {
  if (typeof valor !== "string") {
    return null;
  }

  const textoLimpio = valor.trim();

  return textoLimpio.length > 0 ? textoLimpio : null;
}

/** Variante semántica de `leerTextoRequerido` para campos opcionales. */
export function leerTextoOpcional(valor: unknown): string | null {
  return leerTextoRequerido(valor);
}

/** Devuelve el número si es finito; null en cualquier otro caso. */
export function leerNumeroRequerido(valor: unknown): number | null {
  return typeof valor === "number" && Number.isFinite(valor) ? valor : null;
}

/** Variante semántica de `leerNumeroRequerido` para campos opcionales. */
export function leerNumeroOpcional(valor: unknown): number | null {
  return leerNumeroRequerido(valor);
}

/** Devuelve el booleano si lo es; null en cualquier otro caso. */
export function leerBooleanoRequerido(valor: unknown): boolean | null {
  return typeof valor === "boolean" ? valor : null;
}
