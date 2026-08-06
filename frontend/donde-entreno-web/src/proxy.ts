import { NextResponse, type NextRequest } from "next/server";

import {
  NOMBRE_COOKIE_SESION,
  parsearValorCookieSesion,
} from "./lib/sesionCookie";
import { esRolAdmin, esRolPublicador, obtenerRutaInicialPorRol } from "./lib/authRedirects";

/*
  Guard de rutas privadas (convención proxy de Next 16, ex middleware).

  Evita servir el HTML de /admin, /publicador y /mi-cuenta a visitantes
  sin sesión, redirigiendo al login con returnTo. Complementa (no
  reemplaza) a los guards client-side existentes y, sobre todo, a la
  validación de JWT del backend, que es la única frontera de seguridad
  real: esta cookie es falsificable y solo mejora la UX.
*/
export function proxy(request: NextRequest) {
  const { pathname } = request.nextUrl;

  // El login de admin es público: redirige por su cuenta a /login.
  if (pathname === "/admin/login") {
    return NextResponse.next();
  }

  const sesion = parsearValorCookieSesion(
    request.cookies.get(NOMBRE_COOKIE_SESION)?.value
  );

  if (!sesion) {
    const urlLogin = request.nextUrl.clone();
    urlLogin.pathname = "/login";
    urlLogin.search = "";
    urlLogin.searchParams.set("returnTo", pathname);

    return NextResponse.redirect(urlLogin);
  }

  const tienePermiso = pathname.startsWith("/admin")
    ? esRolAdmin(sesion.rol)
    : pathname.startsWith("/publicador")
      ? esRolPublicador(sesion.rol)
      : true; // /mi-cuenta alcanza con tener sesión.

  if (!tienePermiso) {
    const urlDestino = request.nextUrl.clone();
    urlDestino.pathname = obtenerRutaInicialPorRol(sesion.rol);
    urlDestino.search = "";

    return NextResponse.redirect(urlDestino);
  }

  return NextResponse.next();
}

export const config = {
  matcher: [
    "/admin/:path*",
    "/publicador/:path*",
    "/mi-cuenta/:path*",
    "/favoritos/:path*",
  ],
};
