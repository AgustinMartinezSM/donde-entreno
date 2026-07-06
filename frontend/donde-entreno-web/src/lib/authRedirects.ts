export function obtenerRutaInicialPorRol(rol: string): string {
  if (esRolAdmin(rol)) {
    return "/admin/solicitudes";
  }

  if (esRolPublicador(rol)) {
    return "/publicador";
  }

  if (rol === "USUARIO") {
    return "/mi-cuenta";
  }

  return "/";
}

export function esRolAdmin(rol: string): boolean {
  return rol === "SUPER_ADMIN" || rol === "ADMIN";
}

export function esRolPublicador(rol: string): boolean {
  return rol === "PUBLICADOR";
}
