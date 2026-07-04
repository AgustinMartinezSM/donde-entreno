import Link from "next/link";
import type { LinkProps } from "next/link";
import type { ReactNode } from "react";

type AppLinkButtonProps = {
  href: LinkProps["href"];
  children: ReactNode;
  variant?: "primary" | "secondary" | "outline" | "success";
  size?: "sm" | "md" | "lg";
  fullWidth?: boolean;
  className?: string;
};

const baseClassName =
  "inline-flex items-center justify-center rounded-[18px] font-extrabold shadow-sm transition duration-200 ease-out hover:-translate-y-0.5 focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-[#4FB3D9]/30 focus-visible:ring-offset-2 active:scale-[0.98]";

const variantClassNames: Record<
  NonNullable<AppLinkButtonProps["variant"]>,
  string
> = {
  primary:
    "bg-[var(--color-primary)] text-white shadow-[var(--shadow-button)] hover:bg-[#0B314D]",
  secondary:
    "border border-[#BFDDEA] bg-white text-[var(--color-primary)] hover:border-[var(--color-primary)] hover:bg-[#F8FCFE]",
  outline:
    "border border-[#BFDDEA] bg-white/70 text-[var(--color-primary)] hover:border-[var(--color-primary)] hover:bg-white",
  success:
    "border border-[#BDE8D0] bg-[#ECF9F2] text-[#1D7B4A] hover:border-[#2EB872] hover:bg-white",
};

const sizeClassNames: Record<NonNullable<AppLinkButtonProps["size"]>, string> = {
  sm: "min-h-10 px-4 py-2 text-xs",
  md: "min-h-11 px-5 py-3 text-sm",
  lg: "min-h-12 px-6 py-3 text-sm",
};

function unirClases(...clases: Array<string | undefined | false>) {
  return clases.filter(Boolean).join(" ");
}

export function AppLinkButton({
  href,
  children,
  variant = "primary",
  size = "md",
  fullWidth = false,
  className,
}: AppLinkButtonProps) {
  return (
    <Link
      href={href}
      className={unirClases(
        baseClassName,
        variantClassNames[variant],
        sizeClassNames[size],
        fullWidth && "w-full",
        className
      )}
    >
      {children}
    </Link>
  );
}
