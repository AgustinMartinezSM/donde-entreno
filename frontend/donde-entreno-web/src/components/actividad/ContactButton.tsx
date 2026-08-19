type ContactButtonProps = {
  whatsapp?: string | null;
  instagram?: string | null;
  email?: string | null;
  /* Título de la actividad para el mensaje prellenado de WhatsApp. */
  tituloActividad?: string;
  className?: string;
};

const claseBoton =
  "block w-full rounded-[var(--radius-md)] bg-[var(--color-secondary)] px-4 py-3 text-center text-sm font-bold text-white shadow-[0_14px_35px_rgba(46,184,114,0.28)] transition duration-200 ease-out hover:-translate-y-0.5 hover:bg-[#249B60] active:scale-[0.98]";

export function ContactButton({
  whatsapp,
  instagram,
  email,
  tituloActividad,
  className = "mt-6",
}: ContactButtonProps) {
  const numeroWhatsapp = normalizarNumeroWhatsapp(whatsapp);

  if (numeroWhatsapp) {
    const mensaje = tituloActividad
      ? `Hola! Vi "${tituloActividad}" en DondeEntreno y quiero más info.`
      : "Hola! Vi tu actividad en DondeEntreno y quiero más info.";

    return (
      <a
        href={`https://wa.me/${numeroWhatsapp}?text=${encodeURIComponent(mensaje)}`}
        target="_blank"
        rel="noopener noreferrer"
        className={`${claseBoton} ${className}`}
      >
        Contactar por WhatsApp
      </a>
    );
  }

  /*
    Si no hay WhatsApp pero hay Instagram, mandamos a Instagram.
    El backend puede devolver usuario o URL completa.
  */
  if (instagram) {
    const instagramUrl = instagram.startsWith("http")
      ? instagram
      : `https://instagram.com/${instagram.replace(/^@/, "")}`;

    return (
      <a
        href={instagramUrl}
        target="_blank"
        rel="noopener noreferrer"
        className={`${claseBoton} ${className}`}
      >
        Contactar por Instagram
      </a>
    );
  }

  if (email) {
    return (
      <a href={`mailto:${email}`} className={`${claseBoton} ${className}`}>
        Contactar por email
      </a>
    );
  }

  return (
    <div
      className={`rounded-[var(--radius-md)] border border-[var(--color-border-soft)] bg-[var(--color-bg)] px-4 py-3 text-center text-sm font-bold leading-6 text-[var(--color-muted)] ${className}`}
    >
      Esta actividad todavía no cargó un canal de contacto.
    </div>
  );
}

/*
  wa.me exige solo dígitos con código de país. El campo llega libre desde
  el backend ("+54 223 555-1234", "223 4555555"), así que: quitamos todo
  lo que no sea dígito, sacamos el 0 inicial de área y anteponemos 54 si
  falta el código de país. Si igual queda algo inválido, devolvemos null
  y el botón cae al siguiente canal disponible.
*/
function normalizarNumeroWhatsapp(valor?: string | null): string | null {
  const digitos = valor?.replace(/\D/g, "");

  if (!digitos || digitos.length < 8) {
    return null;
  }

  const sinCeroInicial = digitos.replace(/^0+/, "");

  if (sinCeroInicial.startsWith("54")) {
    return sinCeroInicial;
  }

  return `54${sinCeroInicial}`;
}
