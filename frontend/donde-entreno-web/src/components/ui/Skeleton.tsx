import { Header } from "../layout/Header";

/*
  Primitivas de skeleton (Fase 1 de la etapa social): carga moderna en
  vez de pantallas vacías o saltos bruscos. Tokens del tema para que
  funcionen igual en claro y oscuro.
*/

export function Skeleton({ className = "" }: { className?: string }) {
  return (
    <div
      aria-hidden="true"
      className={`animate-pulse rounded-[14px] bg-[var(--color-border-soft)] ${className}`}
    />
  );
}

/* Card de actividad fantasma: imagen + identidad + dos líneas. */
export function SkeletonCardActividad() {
  return (
    <div className="overflow-hidden rounded-[20px] border border-[var(--color-border)] bg-[var(--color-surface)] p-3">
      <div className="flex items-center gap-3">
        <Skeleton className="h-10 w-10 rounded-full" />
        <div className="flex-1 space-y-2">
          <Skeleton className="h-3 w-2/5" />
          <Skeleton className="h-3 w-1/4" />
        </div>
      </div>
      <Skeleton className="mt-3 aspect-[4/3] w-full rounded-[16px]" />
      <div className="mt-3 space-y-2">
        <Skeleton className="h-4 w-3/4" />
        <Skeleton className="h-3 w-1/2" />
      </div>
    </div>
  );
}

/* Grilla de cards fantasma para listados. */
export function SkeletonGrillaActividades({ cantidad = 6 }: { cantidad?: number }) {
  return (
    <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
      {Array.from({ length: cantidad }, (_, indice) => (
        <SkeletonCardActividad key={indice} />
      ))}
    </div>
  );
}

/*
  Shell de página para los loading.tsx: el Header es el REAL (es
  estático, así no parpadea entre skeleton y página) y el contenido es
  fantasma.
*/
export function SkeletonPagina({ children }: { children?: React.ReactNode }) {
  return (
    <main
      className="min-h-screen text-[var(--color-text)]"
      aria-busy="true"
      aria-label="Cargando"
    >
      <section className="mx-auto w-full max-w-6xl px-4 py-6">
        <Header />
        <div className="mt-8">{children}</div>
      </section>
    </main>
  );
}
