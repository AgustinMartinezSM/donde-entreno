"use client";

/*
  El número de mensajes sin leer al lado de la entrada de consultas.

  Con 0 NO se dibuja: un badge en cero es ruido que le enseña a la
  gente a ignorar los badges.
*/
export function BadgeNoLeidos({ cantidad }: { cantidad: number }) {
  if (!cantidad || cantidad <= 0) {
    return null;
  }

  return (
    <span
      className="ml-auto inline-flex min-w-5 items-center justify-center rounded-full bg-[var(--color-brand)] px-1.5 py-0.5 text-[11px] font-extrabold leading-none text-white"
      aria-label={`${cantidad} ${cantidad === 1 ? "mensaje sin leer" : "mensajes sin leer"}`}
    >
      {cantidad > 9 ? "9+" : cantidad}
    </span>
  );
}
