/*
  A dónde te lleva ENTRAR a la app (login y registro, cuando no hay
  returnTo): usuario común y publicador aterrizan en el inicio — la
  decisión de producto es que entrar te deje en la app, no encerrado en
  un panel; el espacio de publicador queda a un toque en el menú de
  cuenta. El admin sí va a su panel: es equipo interno y entra a
  trabajar (decisión documentada en docs/bloque-contenido-visual-v1.md).

  No confundir con obtenerRutaInicialPorRol, que sigue siendo "tu
  espacio según rol" para el fallback del proxy (un rol pisando una ruta
  que no le corresponde vuelve a su casa, no al inicio).
*/
export function obtenerRutaPostLogin(rol: string): string {
  if (esRolAdmin(rol)) {
    return "/admin/solicitudes";
  }

  return "/";
}

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
