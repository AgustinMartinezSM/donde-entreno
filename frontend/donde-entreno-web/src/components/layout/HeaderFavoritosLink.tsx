"use client";

import Link from "next/link";

import { useAuthSession } from "../auth/AuthSessionProvider";
import { useFavoritos } from "../../lib/favoritos";
import { IconoGuardar } from "../ui/IconoGuardar";

/*
  Acceso a "Mis favoritos" desde el header.

  Regla de producto: los favoritos son exclusivos de usuarios con
  cuenta. Para visitantes anónimos el botón invita a iniciar sesión
  (login con aviso y returnTo a favoritos); para usuarios logueados
  muestra el contador de guardadas.
*/
export function HeaderFavoritosLink() {
  const { status } = useAuthSession();
  const favoritos = useFavoritos();

  const autenticado = status === "authenticated";
  const cantidad = autenticado ? favoritos.length : 0;

  const href = autenticado
    ? "/favoritos"
    : `/login?motivo=cuenta&returnTo=${encodeURIComponent("/favoritos")}`;

  const etiqueta = !autenticado
    ? "Favoritos (necesitás una cuenta)"
    : cantidad === 0
      ? "Ver mis favoritos"
      : cantidad === 1
        ? "Ver mis favoritos (1 guardada)"
        : `Ver mis favoritos (${cantidad} guardadas)`;

  return (
    <Link
      href={href}
      aria-label={etiqueta}
      className="relative hidden min-h-11 items-center gap-2 rounded-[var(--radius-md)] border border-[var(--color-border-accent)] bg-[var(--color-surface)] px-3 py-2 text-sm font-bold text-[var(--color-primary)] transition duration-200 ease-out hover:-translate-y-0.5 hover:border-[var(--color-primary)] hover:bg-[var(--color-surface-soft)] lg:inline-flex"
    >
      <IconoGuardar relleno={cantidad > 0} className="h-4 w-4" />

      <span className="hidden lg:inline">Guardados</span>

      {cantidad > 0 && (
        <span
          aria-hidden="true"
          className="inline-flex h-5 min-w-5 items-center justify-center rounded-full bg-[var(--color-success)] px-1.5 text-[11px] font-extrabold text-white"
        >
          {cantidad > 99 ? "99+" : cantidad}
        </span>
      )}
    </Link>
  );
}
