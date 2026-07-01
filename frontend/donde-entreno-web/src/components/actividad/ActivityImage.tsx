"use client";

import Image from "next/image";
import { useState } from "react";

type ActivityImageProps = {
  src?: string | null;
  fallbackSrc?: string | null;
  alt?: string | null;
  fallbackText?: string;
  heightClassName?: string;
};

export function ActivityImage({
  src,
  fallbackSrc,
  alt,
  fallbackText = "Actividad",
  heightClassName = "h-44",
}: ActivityImageProps) {
  /*
    Guardamos las URLs que fallaron al cargar.
    Así podemos intentar primero la imagen real, luego la imagen fallback,
    y finalmente mostrar el bloque textual si ambas fallan.
  */
  const [imagenesFallidas, setImagenesFallidas] = useState<string[]>([]);

  const srcLimpio = normalizarRutaImagen(src);
  const fallbackLimpio = normalizarRutaImagen(fallbackSrc);

  const imagenActual = obtenerPrimeraImagenDisponible(
    [srcLimpio, fallbackLimpio],
    imagenesFallidas
  );

  function manejarErrorImagen() {
    if (!imagenActual) {
      return;
    }

    setImagenesFallidas((imagenesActuales) => {
      if (imagenesActuales.includes(imagenActual)) {
        return imagenesActuales;
      }

      return [...imagenesActuales, imagenActual];
    });
  }

  if (!imagenActual) {
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
        src={imagenActual}
        alt={alt?.trim() || fallbackText}
        fill
        sizes="(max-width: 640px) 100vw, (max-width: 1024px) 50vw, 600px"
        className="object-cover"
        onError={manejarErrorImagen}
      />
    </div>
  );
}

function normalizarRutaImagen(valor?: string | null) {
  const valorLimpio = valor?.trim();

  if (!valorLimpio) {
    return null;
  }

  return valorLimpio;
}

function obtenerPrimeraImagenDisponible(
  imagenes: Array<string | null>,
  imagenesFallidas: string[]
) {
  return (
    imagenes.find(
      (imagen): imagen is string =>
        imagen !== null && !imagenesFallidas.includes(imagen)
    ) ?? null
  );
}
