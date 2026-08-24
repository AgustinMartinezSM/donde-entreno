import { API_BASE_URL } from "../lib/apiConfig";
import type { UsuarioActual } from "../types/auth";

/*
  Edición de datos del usuario (Fase 2 social): nombre y apellido,
  inline desde /configuracion. El email no se edita (credencial de
  login, gateado por la verificación de email PAUSADA).
*/

export class UsuarioPerfilApiError extends Error {
  status: number | null;

  constructor(message: string, status: number | null = null) {
    super(message);
    this.name = "UsuarioPerfilApiError";
    this.status = status;
  }
}

export async function actualizarDatosUsuario(
  accessToken: string,
  nombre: string,
  apellido: string
): Promise<UsuarioActual> {
  let respuesta: Response;

  try {
    respuesta = await fetch(`${API_BASE_URL}/api/usuario/perfil`, {
      method: "PATCH",
      headers: {
        "Authorization": `Bearer ${accessToken}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({ nombre, apellido }),
    });
  } catch {
    throw new UsuarioPerfilApiError("No fue posible conectar con el servidor.");
  }

  if (!respuesta.ok) {
    let mensaje = "No pudimos guardar tus datos. Probá nuevamente.";

    try {
      const cuerpo: unknown = await respuesta.json();
      if (
        typeof cuerpo === "object" &&
        cuerpo !== null &&
        typeof (cuerpo as { mensaje?: unknown }).mensaje === "string"
      ) {
        mensaje = (cuerpo as { mensaje: string }).mensaje;
      }
    } catch {
      /* Cuerpo ilegible: queda el mensaje genérico. */
    }

    throw new UsuarioPerfilApiError(mensaje, respuesta.status);
  }

  return respuesta.json();
}
