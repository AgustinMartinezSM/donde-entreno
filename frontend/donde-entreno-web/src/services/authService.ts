import { API_BASE_URL } from "../lib/apiConfig";
import type {
  AdminLoginRequest,
  AdminLoginResponse,
  AdminSesion,
  AdminUsuarioAutenticado,
  AuthErrorResponse,
  AuthErroresPorCampo,
} from "../types/auth";

const ADMIN_SESSION_STORAGE_KEY = "donde_entreno_admin_session";

type AuthApiErrorOpciones = {
  status?: number | null;
  respuesta?: AuthErrorResponse | null;
};

export class AuthApiError extends Error {
  status: number | null;
  respuesta: AuthErrorResponse | null;

  constructor(message: string, opciones: AuthApiErrorOpciones = {}) {
    super(message);
    this.name = "AuthApiError";
    this.status = opciones.status ?? null;
    this.respuesta = opciones.respuesta ?? null;
  }
}

export async function loginAdmin(
  credenciales: AdminLoginRequest
): Promise<AdminLoginResponse> {
  let respuestaHttp: Response;

  try {
    respuestaHttp = await fetch(`${API_BASE_URL}/api/auth/login`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "Accept": "application/json",
      },
      body: JSON.stringify(credenciales),
    });
  } catch (error: unknown) {
    if (error instanceof AuthApiError) {
      throw error;
    }

    throw new AuthApiError(
      "No fue posible conectar con el servidor para iniciar sesion."
    );
  }

  const cuerpo: unknown = await leerJsonSeguro(respuestaHttp);

  if (!respuestaHttp.ok) {
    if (esAuthErrorResponse(cuerpo)) {
      throw new AuthApiError(
        cuerpo.mensaje || obtenerMensajeAuthPorStatus(respuestaHttp.status),
        {
          status: respuestaHttp.status,
          respuesta: cuerpo,
        }
      );
    }

    throw new AuthApiError(obtenerMensajeAuthPorStatus(respuestaHttp.status), {
      status: respuestaHttp.status,
    });
  }

  if (!esAdminLoginResponse(cuerpo)) {
    throw new AuthApiError(
      "La respuesta del servidor no tiene el formato esperado.",
      {
        status: respuestaHttp.status,
      }
    );
  }

  return cuerpo;
}

export function guardarSesionAdmin(respuesta: AdminLoginResponse): AdminSesion {
  const sesion: AdminSesion = {
    tokenType: respuesta.tokenType,
    accessToken: respuesta.accessToken,
    expiresAt: Date.now() + respuesta.expiresIn * 1000,
    usuario: respuesta.usuario,
  };

  if (puedeUsarSessionStorage()) {
    window.sessionStorage.setItem(
      ADMIN_SESSION_STORAGE_KEY,
      JSON.stringify(sesion)
    );
  }

  return sesion;
}

export function obtenerSesionAdmin(): AdminSesion | null {
  if (!puedeUsarSessionStorage()) {
    return null;
  }

  const sesionJson = window.sessionStorage.getItem(ADMIN_SESSION_STORAGE_KEY);

  if (!sesionJson) {
    return null;
  }

  try {
    const sesion: unknown = JSON.parse(sesionJson) as unknown;

    if (!esAdminSesion(sesion) || !esSesionAdminVigente(sesion)) {
      cerrarSesionAdmin();
      return null;
    }

    return sesion;
  } catch {
    cerrarSesionAdmin();
    return null;
  }
}

export function obtenerAccessTokenAdmin(): string | null {
  return obtenerSesionAdmin()?.accessToken ?? null;
}

export function cerrarSesionAdmin(): void {
  if (!puedeUsarSessionStorage()) {
    return;
  }

  window.sessionStorage.removeItem(ADMIN_SESSION_STORAGE_KEY);
}

export function esSesionAdminVigente(sesion: AdminSesion | null): boolean {
  return (
    sesion !== null &&
    typeof sesion.accessToken === "string" &&
    sesion.accessToken.trim().length > 0 &&
    Number.isFinite(sesion.expiresAt) &&
    sesion.expiresAt > Date.now()
  );
}

async function leerJsonSeguro(respuesta: Response): Promise<unknown> {
  try {
    const cuerpo: unknown = await respuesta.json();
    return cuerpo;
  } catch {
    return null;
  }
}

function obtenerMensajeAuthPorStatus(status: number): string {
  if (status === 401) {
    return "Email o password invalidos.";
  }

  if (status === 403) {
    return "No tenes permisos para acceder al panel admin.";
  }

  return "No se pudo iniciar sesion.";
}

function puedeUsarSessionStorage(): boolean {
  return typeof window !== "undefined" && "sessionStorage" in window;
}

function esObjeto(valor: unknown): valor is Record<string, unknown> {
  return typeof valor === "object" && valor !== null && !Array.isArray(valor);
}

function esAdminUsuarioAutenticado(
  valor: unknown
): valor is AdminUsuarioAutenticado {
  return (
    esObjeto(valor) &&
    typeof valor.id === "number" &&
    typeof valor.email === "string" &&
    typeof valor.nombre === "string" &&
    typeof valor.apellido === "string" &&
    typeof valor.rol === "string"
  );
}

function esAdminLoginResponse(valor: unknown): valor is AdminLoginResponse {
  return (
    esObjeto(valor) &&
    typeof valor.tokenType === "string" &&
    typeof valor.accessToken === "string" &&
    typeof valor.expiresIn === "number" &&
    esAdminUsuarioAutenticado(valor.usuario)
  );
}

function esAdminSesion(valor: unknown): valor is AdminSesion {
  return (
    esObjeto(valor) &&
    typeof valor.tokenType === "string" &&
    typeof valor.accessToken === "string" &&
    typeof valor.expiresAt === "number" &&
    esAdminUsuarioAutenticado(valor.usuario)
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
