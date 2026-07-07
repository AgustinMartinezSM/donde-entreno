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
    <div className="relative overflow-hidden rounded-[28px] border border-[#BDE8D0] bg-gradient-to-br from-white via-[#F8FCFE] to-[#E6F7EF] p-6 shadow-[0_22px_55px_rgba(12,52,80,0.12)] sm:p-8">
      <div className="absolute right-0 top-0 h-28 w-28 rounded-bl-full bg-[#2EB872]/12" />
      <div className="relative flex flex-col gap-5 lg:flex-row lg:items-end lg:justify-between">
        <div>
          <p className="text-xs font-extrabold uppercase tracking-[0.2em] text-[#167A4A]">
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
    </div>
  );
}
