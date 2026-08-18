/*
  Dondi, el asistente de DondeEntreno.

  Es un SVG propio y no un asset descargado: pesa unos cientos de bytes,
  hereda los colores de marca y escala sin perder nitidez en el launcher
  (56px), en el encabezado del chat (40px) y en cada burbuja (28px).

  La cara es deliberadamente mínima —dos ojos y una sonrisa— porque a
  tamaño de burbuja cualquier detalle más se convierte en ruido.
*/

type DondiAvatarProps = {
  /* Lado del avatar en px. El trazo interno se ajusta solo. */
  tamanio?: number;
  className?: string;
  /*
    El punto verde de "en línea". Solo en el encabezado del chat: en las
    burbujas se repetiría en cada mensaje.
  */
  conEstado?: boolean;
};

export function DondiAvatar({
  tamanio = 40,
  className = "",
  conEstado = false,
}: DondiAvatarProps) {
  /* id único por instancia: dos gradientes con el mismo id se pisan. */
  const idGradiente = `dondi-gradiente-${tamanio}`;

  return (
    <span
      className={`relative inline-flex shrink-0 ${className}`}
      style={{ width: tamanio, height: tamanio }}
    >
      <svg
        viewBox="0 0 48 48"
        width={tamanio}
        height={tamanio}
        role="img"
        aria-label="Dondi, asistente de DondeEntreno"
        className="drop-shadow-sm"
      >
        <defs>
          <linearGradient id={idGradiente} x1="0" y1="0" x2="1" y2="1">
            <stop offset="0%" stopColor="#0F3D5E" />
            <stop offset="55%" stopColor="#1E7FA8" />
            <stop offset="100%" stopColor="#2EB872" />
          </linearGradient>
        </defs>

        <circle cx="24" cy="24" r="24" fill={`url(#${idGradiente})`} />

        {/* Antena: lo que lo lee como "bot" y no como una pelota. */}
        <line
          x1="24"
          y1="9"
          x2="24"
          y2="13.5"
          stroke="#A7F3CF"
          strokeWidth="2"
          strokeLinecap="round"
        />
        <circle cx="24" cy="8" r="2.4" fill="#2EB872" />

        {/* Cabeza */}
        <rect
          x="11"
          y="14"
          width="26"
          height="21"
          rx="9"
          fill="#FFFFFF"
          fillOpacity="0.96"
        />

        {/* Ojos */}
        <circle cx="19" cy="23.5" r="2.6" fill="#0F3D5E" />
        <circle cx="29" cy="23.5" r="2.6" fill="#0F3D5E" />
        {/* Brillo: sin esto la mirada queda inexpresiva. */}
        <circle cx="19.9" cy="22.6" r="0.85" fill="#FFFFFF" />
        <circle cx="29.9" cy="22.6" r="0.85" fill="#FFFFFF" />

        {/* Sonrisa */}
        <path
          d="M19.5 28.8c1.4 1.5 3 2.2 4.5 2.2s3.1-.7 4.5-2.2"
          stroke="#0F3D5E"
          strokeWidth="1.8"
          strokeLinecap="round"
          fill="none"
        />
      </svg>

      {conEstado ? (
        <span
          aria-hidden="true"
          className="absolute bottom-0 right-0 block h-3 w-3 rounded-full border-2 border-white bg-[var(--color-secondary)]"
        />
      ) : null}
    </span>
  );
}
