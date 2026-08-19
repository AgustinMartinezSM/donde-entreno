import type { ReactNode } from "react";

type StatusMessageProps = {
  variant?: "info" | "success" | "warning" | "error";
  title?: ReactNode;
  children?: ReactNode;
  className?: string;
  role?: "status" | "alert";
};

const variantClassNames: Record<NonNullable<StatusMessageProps["variant"]>, string> =
  {
    info: "border-[var(--color-border-accent)] bg-[var(--color-info-soft)] text-[var(--color-info-deep)]",
    success: "border-[var(--color-success-border)] bg-[var(--color-success-wash)] text-[var(--color-success)]",
    warning: "border-[var(--color-warning-border)] bg-[var(--color-warning-surface)] text-[var(--color-warning)]",
    error: "border-red-200 bg-red-50 text-red-700",
  };

function unirClases(...clases: Array<string | undefined | false>) {
  return clases.filter(Boolean).join(" ");
}

export function StatusMessage({
  variant = "info",
  title,
  children,
  className,
  role,
}: StatusMessageProps) {
  const defaultRole = variant === "error" || variant === "warning" ? "alert" : "status";

  return (
    <div
      role={role ?? defaultRole}
      className={unirClases(
        "rounded-[var(--radius-lg)] border px-4 py-3 text-sm leading-6 shadow-sm",
        variantClassNames[variant],
        className
      )}
    >
      {title ? <p className="font-extrabold">{title}</p> : null}
      {children ? <div className={title ? "mt-1" : undefined}>{children}</div> : null}
    </div>
  );
}
