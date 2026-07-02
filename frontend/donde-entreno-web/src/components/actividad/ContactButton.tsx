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
        className="mt-6 block w-full rounded-[var(--radius-md)] bg-[var(--color-secondary)] px-4 py-3 text-center text-sm font-bold text-white shadow-[0_14px_35px_rgba(46,184,114,0.28)] transition duration-200 ease-out hover:-translate-y-0.5 hover:bg-[#249B60] active:scale-[0.98]"
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
        className="mt-6 block w-full rounded-[var(--radius-md)] bg-[var(--color-secondary)] px-4 py-3 text-center text-sm font-bold text-white shadow-[0_14px_35px_rgba(46,184,114,0.28)] transition duration-200 ease-out hover:-translate-y-0.5 hover:bg-[#249B60] active:scale-[0.98]"
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
        className="mt-6 block w-full rounded-[var(--radius-md)] bg-[var(--color-secondary)] px-4 py-3 text-center text-sm font-bold text-white shadow-[0_14px_35px_rgba(46,184,114,0.28)] transition duration-200 ease-out hover:-translate-y-0.5 hover:bg-[#249B60] active:scale-[0.98]"
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
      className="mt-6 w-full cursor-not-allowed rounded-[var(--radius-md)] bg-slate-300 px-4 py-3 text-sm font-bold text-white opacity-80"
    >
      Contacto no disponible
    </button>
  );
}
