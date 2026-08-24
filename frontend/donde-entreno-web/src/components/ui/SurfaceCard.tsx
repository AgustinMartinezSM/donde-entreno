import type { ReactNode } from "react";

type SurfaceCardProps = {
  children: ReactNode;
  as?: "div" | "section" | "article";
  className?: string;
  variant?: "default" | "soft" | "success" | "info" | "brand";
  /* Ancla para navegación interna (lo usa el Centro de Configuración). */
  id?: string;
};

/*
  El color de fondo se elige con `variant`, nunca por className.

  Todas las variantes traen su propio bg, y una clase de fondo pasada
  por className queda con la misma especificidad: gana la que Tailwind
  emita última en la hoja, no la que se escribe última en el atributo.
  Así se rompió el CTA de crear cuenta, que pedía fondo azul y quedaba
  blanco con texto blanco encima, es decir invisible.

  Los gradientes sí se pueden pasar por className: son background-image
  y se apoyan sobre el color sin competir con él.
*/
const variantClassNames: Record<NonNullable<SurfaceCardProps["variant"]>, string> =
  {
    default: "border-[var(--color-border-soft)] bg-[var(--color-surface)] shadow-[0_16px_40px_rgba(12,52,80,0.08)]",
    soft: "border-[var(--color-border-soft)] bg-[var(--color-surface)]/75 shadow-[0_16px_40px_rgba(12,52,80,0.08)]",
    success: "border-[var(--color-success-border)] bg-[var(--color-success-surface)] shadow-[0_16px_40px_rgba(12,52,80,0.08)]",
    info: "border-[var(--color-border-accent)] bg-[var(--color-surface-soft)] shadow-[0_16px_40px_rgba(12,52,80,0.08)]",
    /* Superficie oscura de marca, para los bloques que cortan el blanco. */
    brand:
      "border-[var(--color-brand)] bg-[var(--color-brand)] text-white shadow-[0_18px_45px_rgba(15,61,94,0.18)]",
  };

function unirClases(...clases: Array<string | undefined | false>) {
  return clases.filter(Boolean).join(" ");
}

export function SurfaceCard({
  children,
  as: Tag = "div",
  className,
  variant = "default",
  id,
}: SurfaceCardProps) {
  return (
    <Tag
      id={id}
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
