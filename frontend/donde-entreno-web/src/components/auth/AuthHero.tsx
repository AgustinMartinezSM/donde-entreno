import Link from "next/link";
import type { ReactNode } from "react";

type AuthHeroProps = {
  /* Texto chico de arriba a la izquierda: "Usuario", "Publicador", la marca. */
  eyebrow: ReactNode;
  titulo: ReactNode;
  descripcion: ReactNode;
  /* Lo que la cuenta habilita. Tres ítems: con más, la columna se estira. */
  puntos: string[];
};

/*
  Cabecera de marca de las pantallas de acceso (ingresar, crear cuenta de
  usuario, crear cuenta de publicador).

  Las tres páginas tenían el mismo bloque copiado con otro copy, así que
  cualquier retoque visual había que hacerlo tres veces —y de hecho ya
  habían quedado con anchos de columna distintos. Acá vive una sola vez.

  En mobile es la banda que abre la pantalla; en desktop, la columna
  izquierda de la tarjeta.
*/
export function AuthHero({
  eyebrow,
  titulo,
  descripcion,
  puntos,
}: AuthHeroProps) {
  return (
    <aside className="gradient-brand decorative-orb relative overflow-hidden p-6 text-white sm:p-8 lg:p-10">
      {/*
        Trama de puntos propia (y no .decorative-dots): sobre la
        superficie oscura los puntos tienen que ser claros, y la utility
        compartida los pinta en celeste para fondos claros.

        Va abajo a la derecha y no a la izquierda: a la izquierda caía
        justo detrás del párrafo de la descripción y le ensuciaba la
        lectura.
      */}
      <div
        aria-hidden="true"
        className="pointer-events-none absolute -bottom-6 -right-6 h-28 w-28 opacity-30 [background-image:radial-gradient(rgba(255,255,255,0.55)_1.5px,transparent_1.5px)] [background-size:0.9rem_0.9rem]"
      />

      <div className="relative z-10">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <p className="text-xs font-extrabold uppercase tracking-[0.22em] text-[var(--color-success-border)]">
            {eyebrow}
          </p>
          <Link
            href="/"
            className="inline-flex min-h-9 items-center gap-1.5 rounded-full border border-white/25 bg-white/10 px-3 py-1.5 text-xs font-bold text-white/90 transition duration-200 ease-out hover:bg-white/20 hover:text-white"
          >
            <span aria-hidden="true">←</span> Volver al inicio
          </Link>
        </div>

        <h1 className="mt-5 text-3xl font-extrabold leading-tight sm:text-4xl">
          {titulo}
        </h1>

        {/*
          Regla clara y no .rule-accent: la utility va de verde a celeste
          y acá cae sobre el tramo verde del degradado de marca, donde
          desaparece. Sobre superficie oscura el acento tiene que ser luz.
        */}
        <span
          aria-hidden="true"
          className="mt-4 block h-1 w-11 rounded-full bg-gradient-to-r from-white to-white/40"
        />

        <p className="mt-4 max-w-sm text-sm leading-6 text-white/85 sm:text-base">
          {descripcion}
        </p>

        {/*
          Los tres puntos no aparecen en mobile chico.

          Son argumento de venta, no algo que haga falta para entrar: con
          ellos el hero medía 500px de los 844 de un teléfono y el
          formulario —lo único que la persona vino a hacer— arrancaba
          recién al 63% de la pantalla. Sin ellos el hero baja a ~290px y
          el formulario entra en la primera vista. Desde 640px vuelven,
          porque ahí ya hay lugar.
        */}
        <ul className="mt-8 hidden gap-2.5 sm:grid">
          {puntos.map((punto) => (
            <li
              key={punto}
              className="flex items-center gap-3 rounded-[18px] border border-white/18 bg-white/12 px-3 py-2.5 text-sm font-bold backdrop-blur"
            >
              <span className="icon-tile icon-tile-dark h-9 w-9 border border-white/20">
                <IconoCheck />
              </span>
              <span className="min-w-0">{punto}</span>
            </li>
          ))}
        </ul>
      </div>
    </aside>
  );
}

function IconoCheck() {
  return (
    <svg
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="2.4"
      strokeLinecap="round"
      strokeLinejoin="round"
      className="h-[18px] w-[18px]"
      aria-hidden="true"
    >
      <path d="m5 12.5 4.5 4.5L19 7.5" />
    </svg>
  );
}
