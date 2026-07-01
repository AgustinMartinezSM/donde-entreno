export const ROLES_CONFIRMADOS = [
  "SUPER_ADMIN",
  "ADMIN",
  "PUBLICADOR",
] as const;

export type RolConfirmado = (typeof ROLES_CONFIRMADOS)[number];

export const ROLES_ADMIN = ["SUPER_ADMIN", "ADMIN"] as const;

export type RolAdminConfirmado = (typeof ROLES_ADMIN)[number];

export type AdminLoginRequest = {
  email: string;
  password: string;
};

export type AdminUsuarioAutenticado = {
  id: number;
  email: string;
  nombre: string;
  apellido: string;

  // El backend puede devolver roles confirmados u otros roles futuros.
  rol: string;
};

export type AdminLoginResponse = {
  tokenType: string;
  accessToken: string;

  // expiresIn se interpreta en segundos segun el LoginResponseDTO/JwtService.
  expiresIn: number;

  usuario: AdminUsuarioAutenticado;
};

export type AdminSesion = {
  tokenType: string;
  accessToken: string;

  // expiresAt se guarda como timestamp en milisegundos para validar la sesion local.
  expiresAt: number;

  /*
    Auth temporal para admin local:
    no es la estrategia definitiva de produccion y nunca guarda el password.
  */
  usuario: AdminUsuarioAutenticado;
};

export type AuthErroresPorCampo = Record<string, string>;

export type AuthErrorResponse = {
  status: number;
  error: string;
  mensaje: string;
  errores: AuthErroresPorCampo | null;
  path: string;

  // OffsetDateTime serializado.
  timestamp: string;
};
