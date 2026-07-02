import type { ReactNode } from "react";

type SurfaceCardProps = {
  children: ReactNode;
  as?: "div" | "section" | "article";
  className?: string;
  variant?: "default" | "soft" | "success" | "info";
};

const variantClassNames: Record<NonNullable<SurfaceCardProps["variant"]>, string> =
  {
    default: "border-[#DDEAF3] bg-white shadow-[0_16px_40px_rgba(12,52,80,0.08)]",
    soft: "border-[#DDEAF3] bg-white/75 shadow-[0_16px_40px_rgba(12,52,80,0.08)]",
    success: "border-[#BDE8D0] bg-[#F6FCF8] shadow-[0_16px_40px_rgba(12,52,80,0.08)]",
    info: "border-[#BFDDEA] bg-[#F8FCFE] shadow-[0_16px_40px_rgba(12,52,80,0.08)]",
  };

function unirClases(...clases: Array<string | undefined | false>) {
  return clases.filter(Boolean).join(" ");
}

export function SurfaceCard({
  children,
  as: Tag = "div",
  className,
  variant = "default",
}: SurfaceCardProps) {
  return (
    <Tag
      className={unirClases(
        "rounded-[var(--radius-xl)] border",
        variantClassNames[variant],
        className
      )}
    >
      {children}
    </Tag>
  );
}
