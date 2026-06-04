import Link from "next/link";

export function Header() {
  return (
    <header className="flex items-center justify-between py-4">
      {/*
        Logo textual inicial de DondeEntreno.
        Al hacer click vuelve a la home.
      */}
      <Link href="/" className="text-2xl font-extrabold tracking-tight">
        <span className="text-[var(--color-primary)]">Donde</span>
        <span className="text-[var(--color-secondary)]">Entreno</span>
      </Link>

      {/*
        Botón temporal del MVP.
        Más adelante va a llevar a una pantalla para publicar actividades.
      */}
      <Link
        href="/publicar"
        className="rounded-[var(--radius-md)] bg-[var(--color-primary)] px-4 py-3 text-sm font-bold text-white shadow-[var(--shadow-button)]"
      >
        Publicar
      </Link>
    </header>
  );
}
