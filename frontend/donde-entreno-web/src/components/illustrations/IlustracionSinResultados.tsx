/*
  Ilustración decorativa para estados sin resultados de búsqueda.
  Mapa con recorrido, pin de ubicación (esencia del logo) y lupa,
  en la paleta DondeEntreno. Es decorativa: siempre aria-hidden.
*/
export function IlustracionSinResultados() {
  return (
    <svg
      viewBox="0 0 220 140"
      aria-hidden="true"
      className="mx-auto h-28 w-auto"
      fill="none"
    >
      {/* Mapa de fondo */}
      <rect
        x="20"
        y="18"
        width="180"
        height="104"
        rx="16"
        fill="#E8F6FB"
      />

      {/* Recorrido punteado */}
      <path
        d="M44 98 C 70 60, 96 108, 122 70 S 168 44, 182 52"
        stroke="#4FB3D9"
        strokeWidth="3"
        strokeLinecap="round"
        strokeDasharray="1 10"
      />

      {/* Pin de ubicación */}
      <path
        d="M88 34 c -13 0 -22 9.5 -22 21 c 0 15 22 33 22 33 s 22 -18 22 -33 c 0 -11.5 -9 -21 -22 -21 z"
        fill="#0F3D5E"
      />
      <circle cx="88" cy="56" r="8.5" fill="#2EB872" />

      {/* Lupa */}
      <circle
        cx="152"
        cy="88"
        r="17"
        fill="rgba(248, 250, 252, 0.85)"
        stroke="#0F3D5E"
        strokeWidth="5"
      />
      <line
        x1="165"
        y1="101"
        x2="178"
        y2="114"
        stroke="#0F3D5E"
        strokeWidth="6"
        strokeLinecap="round"
      />
      <line
        x1="146"
        y1="82"
        x2="158"
        y2="94"
        stroke="#BFDDEA"
        strokeWidth="3"
        strokeLinecap="round"
      />
    </svg>
  );
}
