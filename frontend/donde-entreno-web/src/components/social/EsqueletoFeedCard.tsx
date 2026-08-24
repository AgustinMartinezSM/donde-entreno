/*
  Esqueleto de una card del feed. Vivía COPIADO en HomeFeedSeguidos y
  en ParaVos: dos archivos con el mismo markup que había que tocar de
  a dos. Acá vive una sola vez (Fase 6).
*/
export function EsqueletoFeedCard({
  className = "",
  radio = "24px",
}: {
  className?: string;
  radio?: string;
}) {
  return (
    <div
      aria-hidden="true"
      className={`animate-pulse overflow-hidden border border-[var(--color-border)] bg-[var(--color-surface)] p-4 ${className}`}
      style={{ borderRadius: radio }}
    >
      <div className="flex items-center gap-3">
        <span className="h-11 w-11 rounded-full bg-[var(--color-info-soft)]" />
        <div className="flex-1">
          <div className="h-3 w-1/3 rounded-full bg-[var(--color-info-soft)]" />
          <div className="mt-2 h-2.5 w-1/4 rounded-full bg-[var(--color-bg)]" />
        </div>
      </div>
      <div className="mt-4 h-48 rounded-[20px] bg-[var(--color-info-soft)]" />
      <div className="mt-4 h-4 w-2/3 rounded-full bg-[var(--color-info-soft)]" />
      <div className="mt-3 h-3 w-1/2 rounded-full bg-[var(--color-bg)]" />
    </div>
  );
}
