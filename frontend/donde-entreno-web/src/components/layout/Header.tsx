import Image from "next/image";
import Link from "next/link";

export function Header() {
  return (
    <header className="flex items-center justify-between gap-4 py-4">
      {/*
        Logo real de DondeEntreno.
        Al hacer click vuelve a la home.
      */}
      <Link
        href="/"
        className="flex items-center transition hover:opacity-90"
        aria-label="Ir al inicio de DondeEntreno"
      >
        <Image
          src="/brand/logo-horizontal.png"
          alt="DondeEntreno"
          width={180}
          height={48}
          priority
          className="h-auto w-[150px] sm:w-[175px]"
        />
      </Link>

      {/*
        Botón temporal del MVP.
        Más adelante va a llevar a una pantalla para publicar actividades.
      */}
      <Link
        href="/publicar"
        className="rounded-[var(--radius-md)] bg-[var(--color-primary)] px-4 py-3 text-sm font-bold text-white shadow-[var(--shadow-button)] transition hover:-translate-y-0.5"
      >
        Publicar
      </Link>
    </header>
  );
}
