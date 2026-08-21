import { API_BASE_URL } from "../lib/apiConfig";
import {
  construirAuthorization,
  ejecutarRequestJson,
  esErrorResponseApi,
  esObjeto,
  esStringONull,
} from "./apiHelpers";
import type {
  AdminLoginRequest,
  AdminLoginResponse,
  AdminSesion,
  AuthErrorResponse,
  AuthErroresPorCampo,
  AuthUsuario,
  CambiarPasswordRequest,
  LoginRequest,
  LoginResponse,
  RegistroPublicadorRequest,
  RegistroUsuarioRequest,
  SesionAuth,
  UsuarioActual,
} from "../types/auth";

const AUTH_SESSION_STORAGE_KEY = "donde_entreno_auth_session";
const ADMIN_SESSION_STORAGE_LEGACY_KEY = "donde_entreno_admin_session";
const LOGOUT_MARKER_STORAGE_KEY = "donde_entreno_logout_at";
const LOGOUT_RECIENTE_MS = 15_000;

/*
  El refresh token vive en localStorage A PROPOSITO (decision del
  2026-08-19, docs/plan-refresh-token.md): la cookie HttpOnly seria de
  terceros entre Vercel y Render y los navegadores la bloquean. Es lo
  UNICO que cruza de sessionStorage a localStorage: la sesion (access
  token) sigue siendo por pestaña como siempre.

  La clave se exporta para que el provider escuche el evento `storage`:
  cuando otra pestaña la borra (logout), esta tambien cierra sesion.
*/
export const REFRESH_TOKEN_STORAGE_KEY = "donde_entreno_refresh_token";

type RefreshTokenGuardado = {
  token: string;
  expiresAt: number;
};

type AuthApiErrorOpciones = {
  status?: number | null;
  respuesta?: AuthErrorResponse | null;
  erroresPorCampo?: AuthErroresPorCampo | null;
};

type ValidadorAuth<T> = (valor: unknown) => valor is T;

export class AuthApiError extends Error {
  status: number | null;
  respuesta: AuthErrorResponse | null;
  erroresPorCampo: AuthErroresPorCampo | null;

  constructor(message: string, opciones: AuthApiErrorOpciones = {}) {
    super(message);
    this.name = "AuthApiError";
    this.status = opciones.status ?? null;
    this.respuesta = opciones.respuesta ?? null;
    this.erroresPorCampo = opciones.erroresPorCampo ?? null;
  }
}

export async function login(request: LoginRequest): Promise<LoginResponse> {
  return ejecutarAuthRequest(
    `${API_BASE_URL}/api/auth/login`,
    {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "Accept": "application/json",
      },
      body: JSON.stringify(request),
    },
    esLoginResponse,
    "Email o password invalidos."
  );
}

export async function loginAdmin(
  credenciales: AdminLoginRequest
): Promise<AdminLoginResponse> {
  return login(credenciales);
}

export async function registrarUsuario(
  request: RegistroUsuarioRequest
): Promise<LoginResponse> {
  return ejecutarAuthRequest(
    `${API_BASE_URL}/api/auth/registro/usuario`,
    {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "Accept": "application/json",
      },
      body: JSON.stringify(request),
    },
    esLoginResponse,
    "No se pudo completar el registro."
  );
}

export async function registrarPublicador(
  request: RegistroPublicadorRequest
): Promise<LoginResponse> {
  return ejecutarAuthRequest(
    `${API_BASE_URL}/api/auth/registro/publicador`,
    {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "Accept": "application/json",
      },
      body: JSON.stringify(request),
    },
    esLoginResponse,
    "No se pudo completar el registro de publicador."
  );
}

export async function obtenerUsuarioActual(
  accessToken: string
): Promise<UsuarioActual> {
  return ejecutarAuthRequest(
    `${API_BASE_URL}/api/auth/me`,
    {
      method: "GET",
      headers: {
        "Accept": "application/json",
        "Authorization": construirAuthorizationAuth(accessToken),
      },
      cache: "no-store",
    },
    esUsuarioActual,
    "Tu sesion expiro o no es valida."
  );
}

/**
 * Cambio de contraseña con sesión activa (fase 5a). El backend revoca
 * todas las sesiones del usuario y responde una sesión nueva completa,
 * igual que el login: el que llama debe persistirla con
 * `guardarSesionAuth` para que este dispositivo quede adentro.
 */
export async function cambiarPassword(
  accessToken: string,
  request: CambiarPasswordRequest
): Promise<LoginResponse> {
  return ejecutarAuthRequest(
    `${API_BASE_URL}/api/auth/cambiar-password`,
    {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "Accept": "application/json",
        "Authorization": construirAuthorizationAuth(accessToken),
      },
      body: JSON.stringify(request),
    },
    esLoginResponse,
    "No se pudo cambiar la contraseña."
  );
}

export function guardarSesionAuth(respuesta: LoginResponse): SesionAuth {
  const sesion: SesionAuth = {
    tokenType: respuesta.tokenType,
    accessToken: respuesta.accessToken,
    expiresAt: Date.now() + respuesta.expiresIn * 1000,
    usuario: respuesta.usuario,
  };

  if (puedeUsarSessionStorage()) {
    window.sessionStorage.setItem(
      AUTH_SESSION_STORAGE_KEY,
      JSON.stringify(sesion)
    );
    window.sessionStorage.removeItem(ADMIN_SESSION_STORAGE_LEGACY_KEY);
    window.sessionStorage.removeItem(LOGOUT_MARKER_STORAGE_KEY);
  }

  /*
    Todos los caminos que crean sesion (login, registros, admin y el
    propio refresh) pasan por aca: es el unico punto donde el refresh
    token se persiste o se renueva.
  */
  guardarRefreshTokenGuardado(respuesta);

  return sesion;
}

/**
 * Rota el refresh token contra el backend y devuelve la sesion nueva
 * completa (misma forma que el login).
 */
export async function refrescarSesion(
  refreshToken: string
): Promise<LoginResponse> {
  return ejecutarAuthRequest(
    `${API_BASE_URL}/api/auth/refresh`,
    {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "Accept": "application/json",
      },
      body: JSON.stringify({ refreshToken }),
    },
    esLoginResponse,
    "Tu sesion expiro o no es valida."
  );
}

/**
 * Revoca la familia del refresh token en el servidor (logout real).
 *
 * Best effort a proposito: el logout local nunca debe fallar porque el
 * backend no conteste, y `keepalive` deja que el request sobreviva a la
 * navegacion que el logout dispara inmediatamente despues.
 */
export function revocarRefreshToken(refreshToken: string): void {
  try {
    void fetch(`${API_BASE_URL}/api/auth/logout`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({ refreshToken }),
      keepalive: true,
    }).catch(() => {
      /* Sin red no hay revocacion remota; el token local igual se borra. */
    });
  } catch {
    /* fetch puede no existir en SSR: no hay nada que revocar ahi. */
  }
}

/** El refresh token persistido, o null si no hay, esta vencido o es ilegible. */
export function obtenerRefreshTokenGuardado(): string | null {
  if (!puedeUsarLocalStorage()) {
    return null;
  }

  const crudo = window.localStorage.getItem(REFRESH_TOKEN_STORAGE_KEY);

  if (!crudo) {
    return null;
  }

  try {
    const guardado: unknown = JSON.parse(crudo) as unknown;

    if (!esRefreshTokenGuardado(guardado) || guardado.expiresAt <= Date.now()) {
      window.localStorage.removeItem(REFRESH_TOKEN_STORAGE_KEY);
      return null;
    }

    return guardado.token;
  } catch {
    window.localStorage.removeItem(REFRESH_TOKEN_STORAGE_KEY);
    return null;
  }
}

/*
  Vencimiento del refresh persistido: es el horizonte de la cookie
  liviana del proxy. Si la cookie venciera con el ACCESS token (60 min),
  el proxy redirigiria a login manana a la mañana antes de que el
  provider pudiera refrescar.
*/
export function obtenerVencimientoRefreshGuardado(): number | null {
  if (!puedeUsarLocalStorage()) {
    return null;
  }

  const crudo = window.localStorage.getItem(REFRESH_TOKEN_STORAGE_KEY);

  if (!crudo) {
    return null;
  }

  try {
    const guardado: unknown = JSON.parse(crudo) as unknown;

    if (!esRefreshTokenGuardado(guardado) || guardado.expiresAt <= Date.now()) {
      return null;
    }

    return guardado.expiresAt;
  } catch {
    return null;
  }
}

export function borrarRefreshTokenGuardado(): void {
  if (puedeUsarLocalStorage()) {
    window.localStorage.removeItem(REFRESH_TOKEN_STORAGE_KEY);
  }
}

function guardarRefreshTokenGuardado(respuesta: LoginResponse): void {
  if (!puedeUsarLocalStorage()) {
    return;
  }

  const token = respuesta.refreshToken?.trim();

  /*
    Sin refresh en la respuesta (backend viejo) el registro guardado se
    borra igual: pertenece a la sesion ANTERIOR, y dejarlo vivo podria
    resucitar mas tarde una cuenta que ya no es la logueada.
  */
  if (!token || !Number.isFinite(respuesta.refreshExpiresIn)) {
    window.localStorage.removeItem(REFRESH_TOKEN_STORAGE_KEY);
    return;
  }

  const guardado: RefreshTokenGuardado = {
    token,
    expiresAt: Date.now() + (respuesta.refreshExpiresIn as number) * 1000,
  };

  window.localStorage.setItem(
    REFRESH_TOKEN_STORAGE_KEY,
    JSON.stringify(guardado)
  );
}

export function obtenerSesionAuth(): SesionAuth | null {
  if (!puedeUsarSessionStorage()) {
    return null;
  }

  const sesionActual = leerSesionDesdeStorage(AUTH_SESSION_STORAGE_KEY);

  if (sesionActual) {
    return sesionActual;
  }

  const sesionLegacy = leerSesionDesdeStorage(ADMIN_SESSION_STORAGE_LEGACY_KEY);

  if (!sesionLegacy) {
    return null;
  }

  window.sessionStorage.setItem(
    AUTH_SESSION_STORAGE_KEY,
    JSON.stringify(sesionLegacy)
  );
  window.sessionStorage.removeItem(ADMIN_SESSION_STORAGE_LEGACY_KEY);

  return sesionLegacy;
}

export function obtenerAccessTokenAuth(): string | null {
  return obtenerSesionAuth()?.accessToken ?? null;
}

export function cerrarSesionAuth(): void {
  if (!puedeUsarSessionStorage()) {
    return;
  }

  window.sessionStorage.removeItem(AUTH_SESSION_STORAGE_KEY);
  window.sessionStorage.removeItem(ADMIN_SESSION_STORAGE_LEGACY_KEY);
  window.sessionStorage.setItem(LOGOUT_MARKER_STORAGE_KEY, String(Date.now()));
}

export function hayLogoutRecienteAuth(): boolean {
  if (!puedeUsarSessionStorage()) {
    return false;
  }

  const logoutAt = Number(
    window.sessionStorage.getItem(LOGOUT_MARKER_STORAGE_KEY)
  );

  if (!Number.isFinite(logoutAt) || logoutAt <= 0) {
    window.sessionStorage.removeItem(LOGOUT_MARKER_STORAGE_KEY);
    return false;
  }

  if (Date.now() - logoutAt > LOGOUT_RECIENTE_MS) {
    window.sessionStorage.removeItem(LOGOUT_MARKER_STORAGE_KEY);
    return false;
  }

  return true;
}

export function esSesionAuthVigente(sesion: SesionAuth | null): boolean {
  return (
    sesion !== null &&
    typeof sesion.accessToken === "string" &&
    sesion.accessToken.trim().length > 0 &&
    Number.isFinite(sesion.expiresAt) &&
    sesion.expiresAt > Date.now()
  );
}

export function guardarSesionAdmin(respuesta: AdminLoginResponse): AdminSesion {
  return guardarSesionAuth(respuesta);
}

export function obtenerSesionAdmin(): AdminSesion | null {
  return obtenerSesionAuth();
}

export function obtenerAccessTokenAdmin(): string | null {
  return obtenerAccessTokenAuth();
}

export function cerrarSesionAdmin(): void {
  cerrarSesionAuth();
  /*
    El shim de admin no pasa por el provider: si algun caller futuro lo
    usa, el refresh persistido no puede quedar vivo resucitando la
    sesion en el proximo boot. (Hoy todos los logout de admin van por el
    context, que ademas revoca en el servidor.)
  */
  borrarRefreshTokenGuardado();
}

export function esSesionAdminVigente(sesion: AdminSesion | null): boolean {
  return esSesionAuthVigente(sesion);
}

async function ejecutarAuthRequest<T>(
  url: string,
  opciones: RequestInit,
  validador: ValidadorAuth<T>,
  mensajeFallback: string
): Promise<T> {
  return ejecutarRequestJson(url, opciones, validador, {
    crearErrorConexion: (error) =>
      error instanceof AuthApiError
        ? error
        : new AuthApiError("No fue posible conectar con el servidor."),
    crearErrorHttp: (status, cuerpo) => {
      if (esErrorResponseApi(cuerpo)) {
        return new AuthApiError(
          obtenerMensajeErrorAuth(cuerpo.mensaje, mensajeFallback),
          {
            status,
            respuesta: cuerpo,
            erroresPorCampo: cuerpo.errores,
          }
        );
      }

      return new AuthApiError(obtenerMensajeErrorAuth(null, mensajeFallback), {
        status,
      });
    },
    crearErrorFormatoInvalido: (status) =>
      new AuthApiError(
        "La respuesta del servidor no tiene el formato esperado.",
        { status }
      ),
  });
}

function construirAuthorizationAuth(accessToken: string): string {
  return construirAuthorization(
    accessToken,
    () => new AuthApiError("Necesitas iniciar sesion.")
  );
}

function leerSesionDesdeStorage(storageKey: string): SesionAuth | null {
  const sesionJson = window.sessionStorage.getItem(storageKey);

  if (!sesionJson) {
    return null;
  }

  try {
    const sesion: unknown = JSON.parse(sesionJson) as unknown;

    if (!esSesionAuth(sesion) || !esSesionAuthVigente(sesion)) {
      window.sessionStorage.removeItem(storageKey);
      return null;
    }

    return sesion;
  } catch {
    window.sessionStorage.removeItem(storageKey);
    return null;
  }
}

function obtenerMensajeErrorAuth(
  mensajeBackend: string | null,
  mensajeFallback: string
): string {
  const mensajeLimpio = mensajeBackend?.trim();

  return mensajeLimpio ? mensajeLimpio : mensajeFallback;
}

function puedeUsarSessionStorage(): boolean {
  return typeof window !== "undefined" && "sessionStorage" in window;
}

function puedeUsarLocalStorage(): boolean {
  try {
    return typeof window !== "undefined" && "localStorage" in window;
  } catch {
    /* localStorage bloqueado por el navegador: la sesion no persiste. */
    return false;
  }
}

function esRefreshTokenGuardado(valor: unknown): valor is RefreshTokenGuardado {
  return (
    esObjeto(valor) &&
    typeof valor.token === "string" &&
    valor.token.trim().length > 0 &&
    typeof valor.expiresAt === "number" &&
    Number.isFinite(valor.expiresAt)
  );
}

function esAuthUsuario(valor: unknown): valor is AuthUsuario {
  return (
    esObjeto(valor) &&
    typeof valor.id === "number" &&
    typeof valor.email === "string" &&
    typeof valor.nombre === "string" &&
    typeof valor.apellido === "string" &&
    typeof valor.rol === "string"
  );
}

function esUsuarioActual(valor: unknown): valor is UsuarioActual {
  return (
    esObjeto(valor) &&
    typeof valor.id === "number" &&
    typeof valor.nombre === "string" &&
    typeof valor.apellido === "string" &&
    typeof valor.email === "string" &&
    typeof valor.rol === "string" &&
    esStringONull(valor.telefono) &&
    typeof valor.activo === "boolean" &&
    typeof valor.emailVerificado === "boolean"
  );
}

function esLoginResponse(valor: unknown): valor is LoginResponse {
  return (
    esObjeto(valor) &&
    typeof valor.tokenType === "string" &&
    typeof valor.accessToken === "string" &&
    typeof valor.expiresIn === "number" &&
    esAuthUsuario(valor.usuario)
  );
}

function esSesionAuth(valor: unknown): valor is SesionAuth {
  return (
    esObjeto(valor) &&
    typeof valor.tokenType === "string" &&
    typeof valor.accessToken === "string" &&
    typeof valor.expiresAt === "number" &&
    esAuthUsuario(valor.usuario)
  );
}
