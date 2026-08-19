"use client";

import { useRouter } from "next/navigation";
import { useAuthSession } from "./AuthSessionProvider";

/*
  Botón de cierre de sesión reutilizable para los paneles (publicador,
  admin), que no renderizan el Header público con su menú de sesión.
*/
export function CerrarSesionButton({ className = "" }: { className?: string }) {
  const router = useRouter();
  const { status, cerrarSesion } = useAuthSession();

  if (status !== "authenticated") {
    return null;
  }

  function manejarClick() {
    cerrarSesion();
    router.replace("/");
  }

  return (
    <button
      type="button"
      onClick={manejarClick}
      className={`inline-flex min-h-9 items-center rounded-full border border-[var(--color-border-accent)] bg-white/80 px-3 py-1.5 text-xs font-bold text-[var(--color-muted)] transition duration-200 ease-out hover:border-[var(--color-primary)] hover:text-[var(--color-primary)] ${className}`}
    >
      Cerrar sesión
    </button>
  );
}
