import { Suspense } from "react";
import Image from "next/image";
import Link from "next/link";
import {
  CitySelector,
  CitySelectorFallback,
} from "../ciudades/CitySelector";
import { HeaderSessionMenu } from "../auth/HeaderSessionMenu";

export function Header() {
  return (
    <header className="flex flex-wrap items-center gap-3 py-4">
      {/*
        Logo real de DondeEntreno.
        Al hacer click vuelve a la home.
      */}
      <Link
        href="/"
        className="mr-auto flex min-w-0 items-center transition hover:opacity-90"
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

      <div className="order-3 w-full sm:order-none sm:w-auto">
        <Suspense fallback={<CitySelectorFallback />}>
          <CitySelector />
        </Suspense>
      </div>

      <Link
        href="/publicar"
        className="rounded-[var(--radius-md)] bg-[var(--color-primary)] px-4 py-3 text-sm font-bold text-white shadow-[var(--shadow-button)] transition hover:-translate-y-0.5"
      >
        Publicar
      </Link>

      <div className="order-4 w-full min-w-0 sm:order-none sm:w-auto">
        <HeaderSessionMenu />
      </div>
    </header>
  );
}
