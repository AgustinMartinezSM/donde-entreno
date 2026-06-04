type ContactButtonProps = {
  whatsapp?: string | null;
  instagram?: string | null;
  email?: string | null;
};

export function ContactButton({
  whatsapp,
  instagram,
  email,
}: ContactButtonProps) {
  /*
    Si hay WhatsApp, armamos un link a WhatsApp.
    Más adelante podemos agregar un mensaje automático.
  */
  if (whatsapp) {
    return (
      <a
        href={`https://wa.me/${whatsapp}`}
        target="_blank"
        rel="noopener noreferrer"
        className="mt-6 block w-full rounded-[var(--radius-md)] bg-[var(--color-secondary)] px-4 py-3 text-center text-sm font-bold text-white"
      >
        Contactar por WhatsApp
      </a>
    );
  }

  /*
    Si no hay WhatsApp pero hay Instagram, mandamos a Instagram.
    Acá asumimos que el backend devuelve usuario o URL.
  */
  if (instagram) {
    const instagramUrl = instagram.startsWith("http")
      ? instagram
      : `https://instagram.com/${instagram}`;

    return (
      <a
        href={instagramUrl}
        target="_blank"
        rel="noopener noreferrer"
        className="mt-6 block w-full rounded-[var(--radius-md)] bg-[var(--color-secondary)] px-4 py-3 text-center text-sm font-bold text-white"
      >
        Contactar por Instagram
      </a>
    );
  }

  /*
    Si no hay WhatsApp ni Instagram, pero hay email,
    abrimos el cliente de correo.
  */
  if (email) {
    return (
      <a
        href={`mailto:${email}`}
        className="mt-6 block w-full rounded-[var(--radius-md)] bg-[var(--color-secondary)] px-4 py-3 text-center text-sm font-bold text-white"
      >
        Contactar por email
      </a>
    );
  }

  /*
    Si no hay ningún dato de contacto, mostramos el botón desactivado.
  */
  return (
    <button
      disabled
      className="mt-6 w-full cursor-not-allowed rounded-[var(--radius-md)] bg-slate-300 px-4 py-3 text-sm font-bold text-white"
    >
      Contacto no disponible
    </button>
  );
}