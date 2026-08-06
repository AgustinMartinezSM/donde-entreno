type IconoGuardarProps = {
  /* true: bookmark relleno (estado guardado); false: solo contorno. */
  relleno?: boolean;
  /* Clases de tamaño/color; por defecto h-5 w-5 y hereda currentColor. */
  className?: string;
};

/*
  Ícono bookmark de favoritos, compartido por FavoritoButton,
  HeaderFavoritosLink y el estado vacío de Mis favoritos.
*/
export function IconoGuardar({
  relleno = false,
  className = "h-5 w-5",
}: IconoGuardarProps) {
  return (
    <svg
      viewBox="0 0 24 24"
      aria-hidden="true"
      className={className}
      fill={relleno ? "currentColor" : "none"}
      stroke="currentColor"
      strokeWidth={relleno ? 0 : 2}
      strokeLinecap="round"
      strokeLinejoin="round"
    >
      <path d="M6 3h12a1 1 0 0 1 1 1v17l-7-4.2L5 21V4a1 1 0 0 1 1-1z" />
    </svg>
  );
}
