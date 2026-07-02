import type { ButtonHTMLAttributes } from "react";

type AppButtonProps = ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: "primary" | "secondary" | "outline" | "danger" | "success";
  size?: "sm" | "md" | "lg";
  fullWidth?: boolean;
};

const baseClassName =
  "inline-flex items-center justify-center rounded-[18px] font-extrabold shadow-sm transition duration-200 ease-out hover:-translate-y-0.5 active:scale-[0.98] disabled:cursor-not-allowed disabled:opacity-50 disabled:hover:translate-y-0";

const variantClassNames: Record<NonNullable<AppButtonProps["variant"]>, string> = {
  primary:
    "bg-[var(--color-primary)] text-white shadow-[var(--shadow-button)] hover:bg-[#0B314D]",
  secondary:
    "border border-[#BFDDEA] bg-white text-[var(--color-primary)] hover:border-[var(--color-primary)] hover:bg-[#F8FCFE]",
  outline:
    "border border-[#BFDDEA] bg-white/70 text-[var(--color-primary)] hover:border-[var(--color-primary)] hover:bg-white",
  danger:
    "border border-red-200 bg-red-50 text-red-700 hover:border-red-300 hover:bg-white",
  success:
    "border border-[#BDE8D0] bg-[#ECF9F2] text-[#1D7B4A] hover:border-[#2EB872] hover:bg-white",
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
