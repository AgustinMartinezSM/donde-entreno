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
    info: "border-[#BFDDEA] bg-[#E8F6FB] text-[#0F6F8F]",
    success: "border-[#BDE8D0] bg-[#ECF9F2] text-[#1D7B4A]",
    warning: "border-[#F6C56D] bg-[#FFF4E5] text-[#8A4B00]",
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
