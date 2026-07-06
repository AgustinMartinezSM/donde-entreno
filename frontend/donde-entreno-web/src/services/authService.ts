import { API_BASE_URL } from "../lib/apiConfig";
import type {
  AdminLoginRequest,
  AdminLoginResponse,
  AdminSesion,
  AuthErrorResponse,
  AuthErroresPorCampo,
  AuthUsuario,
  LoginRequest,
  LoginResponse,
  RegistroPublicadorRequest,
  RegistroUsuarioRequest,
  SesionAuth,
  UsuarioActual,
} from "../types/auth";

const AUTH_SESSION_STORAGE_KEY = "donde_entreno_auth_session";
const ADMIN_SESSION_STORAGE_LEGACY_KEY = "donde_entreno_admin_session";

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
        "Authorization": construirAuthorization(accessToken),
      },
      cache: "no-store",
    },
    esUsuarioActual,
    "Tu sesion expiro o no es valida."
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
  }

  return sesion;
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
  let respuestaHttp: Response;

  try {
    respuestaHttp = await fetch(url, opciones);
  } catch (error: unknown) {
    if (error instanceof AuthApiError) {
      throw error;
    }

    throw new AuthApiError("No fue posible conectar con el servidor.");
  }

  const cuerpo: unknown = await leerJsonSeguro(respuestaHttp);

  if (!respuestaHttp.ok) {
    if (esAuthErrorResponse(cuerpo)) {
      throw new AuthApiError(
        obtenerMensajeErrorAuth(respuestaHttp.status, cuerpo.mensaje, mensajeFallback),
        {
          status: respuestaHttp.status,
          respuesta: cuerpo,
          erroresPorCampo: cuerpo.errores,
        }
      );
    }

    throw new AuthApiError(
      obtenerMensajeErrorAuth(respuestaHttp.status, null, mensajeFallback),
      {
        status: respuestaHttp.status,
      }
    );
  }

  if (!validador(cuerpo)) {
    throw new AuthApiError(
      "La respuesta del servidor no tiene el formato esperado.",
      {
        status: respuestaHttp.status,
      }
    );
  }

  return cuerpo;
}

function construirAuthorization(accessToken: string): string {
  const token = accessToken.trim();

  if (!token) {
    throw new AuthApiError("Necesitas iniciar sesion.");
  }

  return `Bearer ${token}`;
}

async function leerJsonSeguro(respuesta: Response): Promise<unknown> {
  try {
    const cuerpo: unknown = await respuesta.json();
    return cuerpo;
  } catch {
    return null;
  }
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
  status: number,
  mensajeBackend: string | null,
  mensajeFallback: string
): string {
  const mensajeLimpio = mensajeBackend?.trim();

  if (mensajeLimpio) {
    return mensajeLimpio;
  }

  if (status === 401 || status === 403 || status === 409) {
    return mensajeFallback;
  }

  return mensajeFallback;
}

function puedeUsarSessionStorage(): boolean {
  return typeof window !== "undefined" && "sessionStorage" in window;
}

function esObjeto(valor: unknown): valor is Record<string, unknown> {
  return typeof valor === "object" && valor !== null && !Array.isArray(valor);
}

function esStringONull(valor: unknown): valor is string | null {
  return typeof valor === "string" || valor === null;
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

function esAuthErroresPorCampo(
  valor: unknown
): valor is AuthErroresPorCampo {
  if (!esObjeto(valor)) {
    return false;
  }

  return Object.values(valor).every((mensaje) => typeof mensaje === "string");
}

function esAuthErrorResponse(valor: unknown): valor is AuthErrorResponse {
  return (
    esObjeto(valor) &&
    typeof valor.status === "number" &&
    typeof valor.error === "string" &&
    typeof valor.mensaje === "string" &&
    (valor.errores === null || esAuthErroresPorCampo(valor.errores)) &&
    typeof valor.path === "string" &&
    typeof valor.timestamp === "string"
  );
}
