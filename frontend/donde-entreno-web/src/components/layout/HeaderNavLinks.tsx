"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";

/*
  Links principales del header desktop con estado activo.
  Espeja el criterio de la navegación inferior mobile: la sección
  actual se marca visualmente y con aria-current.
*/

const LINKS = [
  { href: "/", label: "Inicio", esActivo: (ruta: string) => ruta === "/" },
  {
    href: "/explorar",
    label: "Explorar",
    esActivo: (ruta: string) =>
      ["/explorar", "/actividades", "/ciudades"].some(
        (prefijo) => ruta === prefijo || ruta.startsWith(`${prefijo}/`)
      ),
  },
  {
    href: "/deportes",
    label: "Deportes",
    esActivo: (ruta: string) =>
      ruta === "/deportes" || ruta.startsWith("/deportes/"),
  },
];

export function HeaderNavLinks() {
  const pathname = usePathname() ?? "/";

  return (
    <nav
      aria-label="Navegación principal"
      className="hidden items-center gap-1 lg:flex"
    >
      {LINKS.map((link) => {
        const activo = link.esActivo(pathname);

        return (
          <Link
            key={link.href}
            href={link.href}
            aria-current={activo ? "page" : undefined}
            className={`rounded-full px-3 py-2 text-sm font-extrabold transition ${
              activo
                ? "bg-[var(--color-success-soft)] text-[var(--color-success)]"
                : "text-[var(--color-muted)] hover:bg-[var(--color-surface-soft)] hover:text-[var(--color-primary)]"
            }`}
          >
            {link.label}
          </Link>
        );
      })}
    </nav>
  );
}
