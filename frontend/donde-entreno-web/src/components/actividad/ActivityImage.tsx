"use client";

import { useState } from "react";

type ActivityImageProps = {
  src?: string | null;
  alt: string;
  fallbackText: string;
  heightClassName?: string;
};

export function ActivityImage({
  src,
  alt,
  fallbackText,
  heightClassName = "h-56",
}: ActivityImageProps) {
  const [imagenConError, setImagenConError] = useState(false);

  if (!src || imagenConError) {
    return (
      <div
        className={`flex ${heightClassName} items-center justify-center rounded-[var(--radius-lg)] bg-[#E6F7EF]`}
      >
        <span className="text-sm font-bold text-[#167A4A]">
          {fallbackText}
        </span>
      </div>
    );
  }

  return (
    <div
      className={`${heightClassName} overflow-hidden rounded-[var(--radius-lg)] bg-[#E6F7EF]`}
    >
      <img
        src={src}
        alt={alt}
        className="h-full w-full object-cover"
        onError={() => setImagenConError(true)}
      />
    </div>
  );
}