"use client";

import Image from "next/image";

/*
  El avatar del usuario, en un solo lugar (fix UX 2026-08-22): foto si
  hay, iniciales si no — con el MISMO fallback en header, sheet, barra
  inferior y cabecera del perfil. La foto sale de usuario.avatarUrl,
  que vive en la sesión y se actualiza vía actualizarUsuario del
  provider: cambiarla en un lado la cambia en todos.
*/
/* Laxo a propósito: acepta UsuarioActual y también el usuario de la sesión. */
type IdentidadUsuario = {
  nombre?: string | null;
  apellido?: string | null;
  avatarUrl?: string | null;
};

export function AvatarUsuario({
  usuario,
  className = "",
  claseTexto = "",
}: {
  usuario: IdentidadUsuario | null;
  /* Medidas, ring y sombra las pone cada lugar; acá va la identidad. */
  className?: string;
  claseTexto?: string;
}) {
  const iniciales = obtenerInicialesUsuario(usuario);

  return (
    <span
      aria-hidden="true"
      className={`relative flex shrink-0 items-center justify-center overflow-hidden rounded-full bg-[var(--color-brand)] font-extrabold tracking-[0.08em] text-white ${className} ${claseTexto}`}
    >
      {usuario?.avatarUrl ? (
        <Image
          src={usuario.avatarUrl}
          alt=""
          fill
          sizes="112px"
          className="object-cover"
        />
      ) : (
        iniciales
      )}
    </span>
  );
}

export function obtenerInicialesUsuario(
  usuario: IdentidadUsuario | null
): string {
  const partes = [usuario?.nombre, usuario?.apellido]
    .map((parte) => parte?.trim().charAt(0).toLocaleUpperCase("es") ?? "")
    .filter(Boolean);

  return partes.join("") || "TU";
}
