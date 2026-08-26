import type { ButtonHTMLAttributes } from "react";

type AppButtonProps = ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: "primary" | "secondary" | "outline" | "danger" | "success";
  size?: "sm" | "md" | "lg";
  fullWidth?: boolean;
};

const baseClassName =
  "inline-flex items-center justify-center rounded-[18px] font-extrabold shadow-sm transition duration-200 ease-out hover:-translate-y-0.5 focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-[#4FB3D9]/30 focus-visible:ring-offset-2 active:scale-[0.98] disabled:cursor-not-allowed disabled:opacity-50 disabled:hover:translate-y-0";

const variantClassNames: Record<NonNullable<AppButtonProps["variant"]>, string> = {
  /*
    El color de fondo queda debajo del degradado a propósito: si el
    degradado no se pintara, el botón sigue siendo azul de marca con
    texto blanco, nunca transparente.
  */
  primary:
    "gradient-cta gradient-cta-hover bg-[var(--color-brand)] text-white shadow-[var(--shadow-button)]",
  secondary:
    "border border-[var(--color-border-accent)] bg-[var(--color-surface)] text-[var(--color-primary)] hover:border-[var(--color-primary)] hover:bg-[var(--color-surface-soft)]",
  outline:
    "border border-[var(--color-border-accent)] bg-[var(--color-surface)]/70 text-[var(--color-primary)] hover:border-[var(--color-primary)] hover:bg-[var(--color-surface)]",
  /*
    danger ya NO usa la paleta red-* de Tailwind: la familia
    --color-danger tiene ahora superficie y borde propios, con los
    valores claros calcados de red-50/200/700 para no mover el modo
    claro. El motivo del cambio es el oscuro: `red-*` es fijo, así que
    un bloque de error quedaba casi blanco sobre el navy.
  */
  danger:
    "border border-[var(--color-danger-border)] bg-[var(--color-danger-surface)] text-[var(--color-danger)] hover:border-[var(--color-danger)] hover:bg-[var(--color-surface)]",
  success:
    "border border-[var(--color-success-border)] bg-[var(--color-success-wash)] text-[var(--color-success)] hover:border-[var(--color-secondary)] hover:bg-[var(--color-surface)]",
};

const sizeClassNames: Record<NonNullable<AppButtonProps["size"]>, string> = {
  sm: "min-h-10 px-4 py-2 text-xs",
  md: "min-h-11 px-5 py-3 text-sm",
  lg: "min-h-12 px-6 py-3 text-sm",
};

function unirClases(...clases: Array<string | undefined | false>) {
  return clases.filter(Boolean).join(" ");
}

export function AppButton({
  variant = "primary",
  size = "md",
  fullWidth = false,
  className,
  type,
  ...props
}: AppButtonProps) {
  return (
    <button
      type={type ?? "button"}
      className={unirClases(
        baseClassName,
        variantClassNames[variant],
        sizeClassNames[size],
        fullWidth && "w-full",
        className
      )}
      {...props}
    />
  );
}
