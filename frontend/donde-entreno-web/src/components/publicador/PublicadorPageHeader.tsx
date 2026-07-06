import { BrandName } from "../brand/BrandName";
import type { ReactNode } from "react";

type PublicadorPageHeaderProps = {
  eyebrow?: string;
  title: ReactNode;
  description?: ReactNode;
  action?: ReactNode;
};

export function PublicadorPageHeader({
  eyebrow = "Panel publicador",
  title,
  description,
  action,
}: PublicadorPageHeaderProps) {
  return (
    <div className="flex flex-col gap-5 rounded-[28px] border border-[#DDEAF3] bg-white/80 p-6 shadow-[0_18px_45px_rgba(12,52,80,0.10)] sm:p-8 lg:flex-row lg:items-end lg:justify-between">
      <div>
        <p className="text-xs font-extrabold uppercase tracking-[0.2em] text-[var(--color-secondary)]">
          {eyebrow}
        </p>
        <h1 className="mt-3 text-3xl font-extrabold text-[var(--color-primary)] sm:text-4xl">
          {title}
        </h1>
        {description ? (
          <p className="mt-3 max-w-2xl text-sm leading-6 text-[var(--color-muted)] sm:text-base">
            {description}
          </p>
        ) : null}
        <p className="mt-4 text-xs font-extrabold uppercase tracking-[0.18em] text-[var(--color-muted)]">
          <BrandName className="inline" />
        </p>
      </div>

      {action ? <div className="shrink-0">{action}</div> : null}
    </div>
  );
}
