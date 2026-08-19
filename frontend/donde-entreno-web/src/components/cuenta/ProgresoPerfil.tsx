"use client";

import { marcarOnboardingPerfilResuelto } from "../../lib/onboardingPerfil";
import { AppButton } from "../ui/AppButton";
import { AppLinkButton } from "../ui/AppLinkButton";
import { SurfaceCard } from "../ui/SurfaceCard";
import type { PerfilDeportivo, TabPerfil } from "./usePerfilDeportivo";

type ProgresoPerfilProps = {
  perfil: PerfilDeportivo;
  onIrATab: (tab: TabPerfil) => void;
};

/*
  Progreso del perfil deportivo.

  Los pasos no se guardan en ningún lado: se calculan cada vez a partir
  de lo que ya existe (nombre de la cuenta, ciudad activa, deportes
  elegidos, gente seguida, actividades guardadas). Por eso no hace falta
  persistencia nueva y por eso tampoco puede quedar desactualizado.

  Cuando están los cinco pasos la tarjeta desaparece sola: es una guía
  para empezar, no un panel permanente. Y no vuelve: que se muestre
  depende de `mostrarOnboarding`, que recuerda si la persona ya terminó
  —o la descartó— en vez de recalcularlo. Antes se decidía solo con el
  progreso en vivo, así que quitar un favorito devolvía a alguien con la
  app usada al instructivo de bienvenida.
*/
export function ProgresoPerfil({ perfil, onIrATab }: ProgresoPerfilProps) {
  if (!perfil.mostrarOnboarding) {
    return null;
  }

  const totalPasos = perfil.pasos.length;

  return (
    <SurfaceCard variant="success" className="p-5 sm:p-6">
      <div className="flex flex-wrap items-baseline justify-between gap-2">
        <div>
          <p className="text-xs font-extrabold uppercase tracking-[0.14em] text-[var(--color-success)]">
            Para arrancar
          </p>
          <h2 className="mt-1 text-lg font-extrabold text-[var(--color-primary)] sm:text-xl">
            Completá tu perfil deportivo
          </h2>
        </div>

        <div className="flex items-center gap-3">
          <p className="text-sm font-extrabold text-[var(--color-success)]">
            {perfil.pasosCompletados} de {totalPasos}
          </p>
          {/*
            Salida para quien no quiere la guía. Se recuerda por cuenta,
            así que no reaparece en la próxima visita.
          */}
          <button
            type="button"
            onClick={marcarOnboardingPerfilResuelto}
            className="rounded-full px-2 py-1 text-xs font-bold text-[var(--color-muted)] transition duration-200 ease-out hover:bg-[var(--color-surface)] hover:text-[var(--color-primary)] focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-[#4FB3D9]/30"
          >
            Ahora no
          </button>
        </div>
      </div>

      <div
        role="progressbar"
        aria-valuenow={perfil.pasosCompletados}
        aria-valuemin={0}
        aria-valuemax={totalPasos}
        aria-label={`Progreso de tu perfil: ${perfil.pasosCompletados} de ${totalPasos} pasos`}
        className="mt-4 h-2 overflow-hidden rounded-full bg-[var(--color-surface)]"
      >
        <div
          className="h-full rounded-full bg-[var(--color-secondary)] transition-[width] duration-500 ease-out motion-reduce:transition-none"
          style={{ width: `${perfil.porcentaje}%` }}
        />
      </div>

      <ul className="mt-4 grid gap-2">
        {perfil.pasos.map((paso) => (
          <li
            key={paso.clave}
            className="flex flex-wrap items-center justify-between gap-2 rounded-[14px] bg-[var(--color-surface)]/80 px-3 py-2.5"
          >
            <span className="flex min-w-0 items-center gap-2.5">
              <MarcaPaso completado={paso.completado} />
              <span
                className={`truncate text-sm font-bold ${
                  paso.completado
                    ? "text-[var(--color-muted)] line-through decoration-[var(--color-success-border)] decoration-2"
                    : "text-[var(--color-primary)]"
                }`}
              >
                {paso.etiqueta}
              </span>
            </span>

            {!paso.completado && paso.accion ? (
              paso.accion.href ? (
                <AppLinkButton
                  href={paso.accion.href}
                  variant="secondary"
                  size="sm"
                  className="shrink-0"
                >
                  {paso.accion.texto}
                </AppLinkButton>
              ) : (
                <AppButton
                  variant="secondary"
                  size="sm"
                  className="shrink-0"
                  onClick={() => {
                    if (paso.accion?.tab) {
                      onIrATab(paso.accion.tab);
                    }
                  }}
                >
                  {paso.accion.texto}
                </AppButton>
              )
            ) : null}
          </li>
        ))}
      </ul>
    </SurfaceCard>
  );
}

function MarcaPaso({ completado }: { completado: boolean }) {
  return (
    <span
      role="img"
      aria-label={completado ? "Listo" : "Pendiente"}
      className={`flex h-5 w-5 shrink-0 items-center justify-center rounded-full ${
        completado
          ? "bg-[var(--color-secondary)] text-white"
          : "border-2 border-[var(--color-success-border)] bg-[var(--color-surface)]"
      }`}
    >
      {completado ? (
        <svg
          viewBox="0 0 20 20"
          fill="none"
          stroke="currentColor"
          strokeWidth="2.5"
          strokeLinecap="round"
          strokeLinejoin="round"
          className="h-3 w-3"
          aria-hidden="true"
        >
          <path d="m5.5 10 3 3 6-6" />
        </svg>
      ) : null}
    </span>
  );
}
