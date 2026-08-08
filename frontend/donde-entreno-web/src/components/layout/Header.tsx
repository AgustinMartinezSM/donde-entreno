import { Suspense } from "react";
import Image from "next/image";
import Link from "next/link";
import {
  CitySelector,
  CitySelectorFallback,
} from "../ciudades/CitySelector";
import { HeaderSessionMenu } from "../auth/HeaderSessionMenu";
import { HeaderAsistenteButton } from "./HeaderAsistenteButton";
import { HeaderFavoritosLink } from "./HeaderFavoritosLink";
import { HeaderNavLinks } from "./HeaderNavLinks";
import { MobileAccountShortcut } from "./MobileAccountShortcut";

export function Header() {
  return (
    /*
      -mx-4/px-4 compensa el px-4 de los contenedores de página para que
      el header pegado en mobile cubra el viewport completo (fondo y borde).
    */
    <header className="sticky top-0 z-40 -mx-4 border-b border-[#D9E2EC]/80 bg-[#F8FAFC]/95 px-4 py-3 backdrop-blur-xl sm:static sm:mx-0 sm:rounded-[22px] sm:border sm:bg-white/88 sm:px-4 sm:shadow-[0_10px_30px_rgba(15,61,94,0.07)]">
      <div className="flex min-w-0 items-center gap-3">
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
            className="h-auto w-[138px] sm:w-[160px] lg:w-[170px]"
          />
        </Link>

        <HeaderNavLinks />

        <div className="hidden md:block">
          <Suspense fallback={<CitySelectorFallback />}>
            <CitySelector idSelector="ciudad-activa-desktop" />
          </Suspense>
        </div>

        <HeaderAsistenteButton />

        <HeaderFavoritosLink />

        <Link
          href="/publicar"
          className="hidden min-h-11 items-center rounded-[18px] bg-[var(--color-primary)] px-4 py-2 text-sm font-bold text-white shadow-[var(--shadow-button)] transition hover:-translate-y-0.5 hover:bg-[#0B314D] lg:inline-flex"
        >
          Publicar
        </Link>

        <div className="hidden min-w-0 lg:block">
          <HeaderSessionMenu />
        </div>

        <div className="lg:hidden">
          <MobileAccountShortcut />
        </div>
      </div>

      <div className="mt-3 md:hidden">
        <Suspense fallback={<CitySelectorFallback />}>
          <CitySelector idSelector="ciudad-activa-mobile" />
        </Suspense>
      </div>
    </header>
  );
}
