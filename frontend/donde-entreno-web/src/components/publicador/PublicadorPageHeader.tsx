import Link from "next/link";
import type { ReactNode } from "react";
import { BrandName } from "../brand/BrandName";
import { CerrarSesionButton } from "../auth/CerrarSesionButton";

type PublicadorPageHeaderProps = {
  eyebrow?: string;
  title: ReactNode;
  description?: ReactNode;
  action?: ReactNode;
};

export function PublicadorPageHeader({
  /*
    "Mi perfil" y no "Panel publicador": panel es lenguaje de sistema.
    El publicador entra a su espacio, no a una consola.
  */
  eyebrow = "Mi perfil",
  title,
  description,
  action,
}: PublicadorPageHeaderProps) {
  return (
    <div className="relative overflow-hidden rounded-[28px] border border-[var(--color-border-accent)] bg-gradient-to-br from-[var(--color-surface)] via-[var(--color-surface-soft)] to-[var(--color-info-soft)] p-6 shadow-[0_22px_55px_rgba(12,52,80,0.12)] sm:p-8">
      <div className="absolute right-0 top-0 h-28 w-28 rounded-bl-full bg-[var(--color-accent)]/12" />
      <div className="absolute bottom-0 left-0 h-20 w-20 rounded-tr-full bg-[#2EB872]/8" />

      {/* Barra superior: identidad del sitio, el puente de vuelta al
          lado persona y la salida de sesión. El asistente ya no
          necesita botón acá: el launcher flotante de Dondi está
          presente también en estas páginas. */}
      <div className="relative flex flex-wrap items-center justify-between gap-3">
        <Link
          href="/"
          className="inline-flex items-center gap-2 text-xs font-extrabold uppercase tracking-[0.18em] text-[var(--color-muted)] transition hover:text-[var(--color-primary)]"
        >
          <BrandName className="inline" />
          <span aria-hidden="true">·</span>
          Ver el sitio
        </Link>

        <div className="flex flex-wrap items-center gap-3">
          {/*
            El espejo del conmutador de espacio: desde /mi-cuenta se
            llega acá con una tarjeta, y desde acá se vuelve con este
            link. Antes el único camino de salida era "Ver el sitio",
            que no dice que tu perfil deportivo existe.
          */}
          <Link
            href="/mi-cuenta"
            className="inline-flex min-h-9 items-center rounded-full border border-[var(--color-border-accent)] bg-[var(--color-surface)]/80 px-3 py-1.5 text-xs font-bold text-[var(--color-primary)] transition duration-200 ease-out hover:border-[var(--color-primary)] hover:bg-[var(--color-surface)]"
          >
            Mi perfil deportivo
          </Link>
          <CerrarSesionButton />
        </div>
      </div>

      <div className="relative mt-5 flex flex-col gap-5 lg:flex-row lg:items-end lg:justify-between">
        <div>
          <p className="text-xs font-extrabold uppercase tracking-[0.2em] text-[var(--color-success)]">
            {eyebrow}
          </p>
          <h1 className="mt-3 text-3xl font-extrabold text-[var(--color-primary)] sm:text-4xl">
            {title}
          </h1>
          {description ? (
            <p className="mt-3 max-w-2xl text-sm leading-6 text-[var(--color-muted)] sm:text-base">
              {description}
            </p>
          ) : null}
        </div>

        {action ? <div className="shrink-0">{action}</div> : null}
      </div>
    </div>
  );
}
