"use client";

import { useState } from "react";
import { usePathname, useRouter } from "next/navigation";

import { useAuthSession } from "../auth/AuthSessionProvider";
import { alternarMeGusta, useTieneMeGusta } from "../../lib/meGusta";

type MeGustaButtonProps = {
  slug: string;
  titulo: string;
};

function IconoCorazon({ relleno }: { relleno: boolean }) {
  return (
    <svg
      viewBox="0 0 24 24"
      aria-hidden="true"
      className="h-5 w-5"
      fill={relleno ? "currentColor" : "none"}
      stroke="currentColor"
      strokeWidth={relleno ? 0 : 2}
      strokeLinecap="round"
      strokeLinejoin="round"
    >
      <path d="M12 21s-6.7-4.3-9.3-8.1C.8 10 1.6 6.4 4.6 5.1c2-.9 4.3-.2 5.6 1.4L12 8.4l1.8-1.9c1.3-1.6 3.6-2.3 5.6-1.4 3 1.3 3.8 4.9 1.9 7.8C18.7 16.7 12 21 12 21z" />
    </svg>
  );
}

/*
  Reacción "me gusta" V1 local: solo muestra el estado propio.
  No mostramos contadores porque sin backend no hay métricas reales.
*/
export function MeGustaButton({ slug, titulo }: MeGustaButtonProps) {
  const router = useRouter();
  const pathname = usePathname();
  const { status } = useAuthSession();
  const leGusta = useTieneMeGusta(slug);
  const [animando, setAnimando] = useState(false);

  const manejarClick = () => {
    /*
      Regla de producto: las reacciones son exclusivas de usuarios con
      cuenta; el anónimo va al login con aviso y returnTo.
    */
    if (status !== "authenticated") {
      router.push(
        `/login?motivo=cuenta&returnTo=${encodeURIComponent(pathname ?? "/")}`
      );
      return;
    }

    const ahoraLeGusta = alternarMeGusta(slug);

    if (ahoraLeGusta) {
      setAnimando(true);
    }
  };

  return (
    <button
      type="button"
      onClick={manejarClick}
      aria-label={leGusta ? `Te gusta ${titulo}` : `Me gusta ${titulo}`}
      /*
        En mobile queda solo el ícono: los tres botones de la barra de
        acciones con texto no entran en una fila a 375px y la fila se
        partía en dos. El nombre accesible lo sigue dando aria-label.
      */
      className={`inline-flex min-h-11 items-center justify-center gap-0 rounded-[18px] px-3 py-2.5 text-sm font-extrabold shadow-sm transition duration-200 ease-out hover:-translate-y-0.5 active:scale-[0.98] sm:gap-2 sm:px-4 ${
        leGusta
          ? "border border-[var(--color-success-border)] bg-[var(--color-success-wash)] text-[var(--color-success)] hover:border-[var(--color-secondary)]"
          : "border border-[var(--color-border-accent)] bg-white text-[var(--color-primary)] hover:border-[var(--color-primary)] hover:bg-[var(--color-surface-soft)]"
      }`}
    >
      <span
        onAnimationEnd={() => setAnimando(false)}
        className={`inline-flex ${animando ? "animate-[de-pop_0.35s_ease-out]" : ""} ${
          leGusta ? "text-[var(--color-secondary)]" : ""
        }`}
      >
        <IconoCorazon relleno={leGusta} />
      </span>
      <span className="hidden sm:inline">
        {leGusta ? "Te gusta" : "Me gusta"}
      </span>
    </button>
  );
}
