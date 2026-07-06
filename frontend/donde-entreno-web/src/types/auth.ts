export const ROLES_AUTH = [
  "SUPER_ADMIN",
  "ADMIN",
  "PUBLICADOR",
  "USUARIO",
] as const;

export type RolAuth = (typeof ROLES_AUTH)[number];

// Compatibilidad con nombres previos usados por el panel admin.
export const ROLES_CONFIRMADOS = ROLES_AUTH;

export type RolConfirmado = RolAuth;

export const ROLES_ADMIN = ["SUPER_ADMIN", "ADMIN"] as const;

export type RolAdminConfirmado = (typeof ROLES_ADMIN)[number];

export type LoginRequest = {
  email: string;
  password: string;
};

export type AuthUsuario = {
  id: number;
  email: string;
  nombre: string;
  apellido: string;
  rol: RolAuth | string;
};

export type UsuarioActual = {
  id: number;
  nombre: string;
  apellido: string;
  email: string;
  rol: RolAuth | string;
  telefono: string | null;
  activo: boolean;
  emailVerificado: boolean;
};

export type LoginResponse = {
  tokenType: string;
  accessToken: string;

  // expiresIn se interpreta en segundos segun el LoginResponseDTO/JwtService.
  expiresIn: number;

  usuario: AuthUsuario;
};

export type SesionAuth = {
  tokenType: string;
  accessToken: string;

  // expiresAt se guarda como timestamp en milisegundos para validar la sesion local.
  expiresAt: number;

  usuario: AuthUsuario;
};

export type RegistroUsuarioRequest = {
  nombre: string;
  apellido: string;
  email: string;
  password: string;
  confirmarPassword: string;
  telefono?: string | null;
};

export type RegistroPublicadorRequest = {
  nombre: string;
  apellido: string;
  email: string;
  password: string;
  confirmarPassword: string;
  whatsapp: string;
  tipoPublicador: string;
  nombrePublico: string;
  ciudadPrincipalId: number;
  descripcion?: string | null;
  instagram?: string | null;
  emailContacto?: string | null;
  telefonoContacto?: string | null;
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

export type AdminLoginRequest = LoginRequest;

export type AdminUsuarioAutenticado = AuthUsuario;

export type AdminLoginResponse = LoginResponse;

export type AdminSesion = SesionAuth;
