type BrandNameProps = {
  className?: string;
  onDark?: boolean;
};

// Usamos este componente para respetar la identidad visual del nombre DondeEntreno en toda la interfaz.
export function BrandName({ className = "", onDark = false }: BrandNameProps) {
  const colorDonde = onDark ? "text-white" : "text-[var(--color-primary)]";

  return (
    <span className={className}>
      <span className={colorDonde}>Donde</span>
      <span className="text-[var(--color-secondary)]">Entreno</span>
    </span>
  );
}
