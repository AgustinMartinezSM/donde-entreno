import type { ReactNode } from "react";

type SectionHeaderProps = {
  eyebrow?: string;
  title: ReactNode;
  description?: ReactNode;
  action?: ReactNode;
  align?: "left" | "center";
  className?: string;
};

function unirClases(...clases: Array<string | undefined | false>) {
  return clases.filter(Boolean).join(" ");
}

export function SectionHeader({
  eyebrow,
  title,
  description,
  action,
  align = "left",
  className,
}: SectionHeaderProps) {
  const centrado = align === "center";

  return (
    <div
      className={unirClases(
        "flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between",
        centrado && "text-center sm:block",
        className
      )}
    >
      <div className={unirClases(centrado && "mx-auto max-w-2xl")}>
        {eyebrow ? (
          <p className="text-sm font-bold uppercase tracking-[0.2em] text-[var(--color-secondary)]">
            {eyebrow}
          </p>
        ) : null}
        <h2
          className={unirClases(
            "text-2xl font-extrabold text-[var(--color-primary)] sm:text-3xl",
            eyebrow && "mt-2"
          )}
        >
          {title}
        </h2>
        {description ? (
          <p className="mt-2 max-w-2xl text-sm leading-6 text-[var(--color-muted)] sm:text-base">
            {description}
          </p>
        ) : null}
      </div>

      {action ? <div className="shrink-0">{action}</div> : null}
    </div>
  );
}
