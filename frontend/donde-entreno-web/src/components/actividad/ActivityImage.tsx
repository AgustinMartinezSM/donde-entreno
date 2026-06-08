import Image from "next/image";

type ActivityImageProps = {
  src?: string | null;
  alt: string;
  fallbackText?: string;
  heightClassName?: string;
};

export function ActivityImage({
  src,
  alt,
  fallbackText = "Actividad",
  heightClassName = "h-44",
}: ActivityImageProps) {
  /*
    Si todavía no tenemos una imagen real desde el backend,
    mostramos un bloque visual prolijo con texto.
  */
  if (!src) {
    return (
      <div
        className={`flex ${heightClassName} items-center justify-center rounded-[var(--radius-lg)] bg-gradient-to-br from-[#E8F6FB] to-[#E6F7EF] px-4 text-center`}
      >
        <span className="text-sm font-extrabold uppercase tracking-[0.18em] text-[var(--color-primary)]">
          {fallbackText}
        </span>
      </div>
    );
  }

  return (
    <div
      className={`relative overflow-hidden rounded-[var(--radius-lg)] ${heightClassName}`}
    >
      <Image
        src={src}
        alt={alt}
        fill
        sizes="(max-width: 640px) 100vw, (max-width: 1024px) 50vw, 600px"
        className="object-cover"
      />
    </div>
  );
}