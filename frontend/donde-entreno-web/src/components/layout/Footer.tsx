import Image from "next/image";
import Link from "next/link";
import type { ReactNode } from "react";
import { BrandName } from "../brand/BrandName";

/*
  Pie de la app.

  Era una pila de tres listas de texto: sobre la superficie oscura, los
  enlaces se leían como párrafos y no como algo tocable, que en mobile es
  justo lo que hace falta. Ahora cada destino es una fila con su tile de
  ícono y su chevron —el mismo lenguaje de fila que usa el resto de la
  app— y contacto pasa a ser tres botones reales.

  Los íconos son SVG inline: no suman una request ni dependen de una
  familia de íconos nueva.
*/

type EnlaceNavegacion = {
  href: string;
  texto: string;
  icono: ReactNode;
};

const enlacesNavegacion: EnlaceNavegacion[] = [
  { href: "/", texto: "Inicio", icono: <IconoInicio /> },
  { href: "/deportes", texto: "Deportes", icono: <IconoPelota /> },
  { href: "/ciudades", texto: "Ciudades", icono: <IconoUbicacion /> },
  { href: "/explorar", texto: "Explorar actividades", icono: <IconoBrujula /> },
  { href: "/publicar", texto: "Publicar actividad", icono: <IconoMas /> },
];

/*
  Datos de contacto REALES (2026-08-18, pasados por Agustín). El
  WhatsApp anterior era un número de relleno (5492230000000) que abría
  un chat a la nada, así que salió hasta tener número real. El link de
  Facebook se normalizó a la forma pública del perfil: el que llegó era
  una URL de notificación con parámetros de sesión.
*/
const enlacesContacto = [
  {
    href: "https://www.instagram.com/dondenentrenoapp/",
    texto: "Instagram",
    externo: true,
    icono: <IconoInstagram />,
  },
  {
    href: "mailto:dondeentrenoapp@gmail.com",
    texto: "Email",
    externo: false,
    icono: <IconoMail />,
  },
  {
    href: "https://www.linkedin.com/in/dondeentreno",
    texto: "LinkedIn",
    externo: true,
    icono: <IconoLinkedin />,
  },
  {
    href: "https://www.facebook.com/profile.php?id=61591536422219",
    texto: "Facebook",
    externo: true,
    icono: <IconoFacebook />,
  },
];

export function Footer() {
  /*
    Año actual para que el copyright no quede viejo.
    Esto se calcula automáticamente cuando se renderiza el componente.
  */
  const anioActual = new Date().getFullYear();

  return (
    <footer className="decorative-orb relative mt-auto overflow-hidden border-t border-[#2A5B78] bg-[#08263B] text-white">
      {/*
        Velo de marca sobre el navy plano: el pie es la única superficie
        oscura grande de la app y sin él se leía como un rectángulo.
      */}
      <div
        aria-hidden="true"
        className="pointer-events-none absolute inset-x-0 top-0 h-px bg-gradient-to-r from-transparent via-[var(--color-secondary)] to-transparent opacity-70"
      />

      <div className="relative z-10 mx-auto w-full max-w-6xl px-4 py-10 sm:py-12">
        <div className="grid gap-9 md:grid-cols-[1.4fr_1fr_1fr] md:gap-12">
          <div>
            <Link
              href="/"
              className="inline-flex items-center text-2xl font-extrabold tracking-tight transition duration-200 ease-out hover:-translate-y-0.5"
              aria-label="Ir al inicio de DondeEntreno"
            >
              <BrandName onDark />
            </Link>

            <p className="mt-4 max-w-md text-sm leading-6 text-[#C9E4EF]">
              La comunidad deportiva local para descubrir clubes, profes,
              espacios y actividades cerca tuyo.
            </p>

            <Link
              href="/explorar"
              className="mt-5 inline-flex items-center gap-2.5 rounded-full border border-[#2A5B78] bg-white/5 py-2 pl-3 pr-4 text-xs font-bold uppercase tracking-[0.14em] text-[#A7F3CF] transition duration-200 ease-out hover:border-[var(--color-secondary)] hover:bg-white/10"
            >
              <IconoRayo />
              Descubrí, guardá y entrená cerca
              <span aria-hidden="true" className="text-sm">
                ›
              </span>
            </Link>
          </div>

          <div>
            <TituloPie>Navegación</TituloPie>

            {/*
              divide-y en vez de gap: las filas separadas por una línea
              muy tenue se leen como una lista de app y no como enlaces
              sueltos flotando.
            */}
            <nav className="mt-4 flex flex-col divide-y divide-white/8">
              {enlacesNavegacion.map((enlace) => (
                <Link
                  key={enlace.href}
                  href={enlace.href}
                  className="group flex min-h-12 items-center gap-3 py-2.5 text-sm font-bold text-[#C9E4EF] transition duration-200 ease-out hover:text-white"
                >
                  <span className="icon-tile icon-tile-dark h-9 w-9 transition duration-200 ease-out group-hover:scale-105">
                    {enlace.icono}
                  </span>
                  <span className="min-w-0 flex-1">{enlace.texto}</span>
                  <span
                    aria-hidden="true"
                    className="text-[#5D8CA8] transition duration-200 ease-out group-hover:translate-x-0.5 group-hover:text-[var(--color-secondary)]"
                  >
                    ›
                  </span>
                </Link>
              ))}
            </nav>
          </div>

          <div>
            <TituloPie>Contacto</TituloPie>

            {/* 2 columnas en sm: cuatro botones en fila de a 3 dejaban uno colgado. */}
            <div className="mt-4 grid gap-2.5 sm:grid-cols-2 md:grid-cols-1">
              {enlacesContacto.map((enlace) => (
                <a
                  key={enlace.href}
                  href={enlace.href}
                  target={enlace.externo ? "_blank" : undefined}
                  rel={enlace.externo ? "noopener noreferrer" : undefined}
                  className="flex min-h-12 items-center gap-2.5 rounded-[16px] border border-[#2A5B78] bg-white/5 px-3 py-2.5 text-sm font-bold text-[#C9E4EF] transition duration-200 ease-out hover:-translate-y-0.5 hover:border-[var(--color-secondary)] hover:bg-white/10 hover:text-white"
                >
                  <span className="shrink-0 text-[#A7F3CF]">{enlace.icono}</span>
                  <span className="min-w-0 truncate">{enlace.texto}</span>
                </a>
              ))}
            </div>
          </div>
        </div>

        <div className="mt-9 flex items-center gap-3 border-t border-[#2A5B78] pt-5">
          {/*
            Versión dark del isotipo (fondo navy propio): reemplaza al
            círculo "DE" de iniciales que hacía de logo improvisado.
          */}
          <Image
            src="/brand/logo-darkmode.png"
            alt=""
            aria-hidden="true"
            width={40}
            height={40}
            className="h-10 w-10 shrink-0 rounded-[12px] border border-[#2A5B78]"
          />

          <p className="text-xs leading-5 text-[#C9E4EF]">
            © {anioActual}{" "}
            <BrandName className="inline font-bold" onDark />. Todos los
            derechos reservados.
          </p>
        </div>
      </div>
    </footer>
  );
}

function TituloPie({ children }: { children: ReactNode }) {
  return (
    <div>
      <h2 className="text-sm font-extrabold uppercase tracking-[0.16em] text-white">
        {children}
      </h2>
      <span aria-hidden="true" className="rule-accent mt-2" />
    </div>
  );
}

/* ------------------------------ Íconos ------------------------------ */

const propsIcono = {
  viewBox: "0 0 24 24",
  fill: "none",
  stroke: "currentColor",
  strokeWidth: 2,
  strokeLinecap: "round",
  strokeLinejoin: "round",
  className: "h-[18px] w-[18px]",
  "aria-hidden": true,
} as const;

function IconoInicio() {
  return (
    <svg {...propsIcono}>
      <path d="M3 10.5 12 3l9 7.5" />
      <path d="M5.5 9.5V20a1 1 0 0 0 1 1H10v-5h4v5h3.5a1 1 0 0 0 1-1V9.5" />
    </svg>
  );
}

function IconoPelota() {
  return (
    <svg {...propsIcono}>
      <circle cx="12" cy="12" r="9" />
      <path d="M12 3v4.5M12 16.5V21M3 12h4.5M16.5 12H21" />
      <circle cx="12" cy="12" r="3.2" />
    </svg>
  );
}

function IconoUbicacion() {
  return (
    <svg {...propsIcono}>
      <path d="M12 21s7-6.1 7-11a7 7 0 1 0-14 0c0 4.9 7 11 7 11Z" />
      <circle cx="12" cy="10" r="2.6" />
    </svg>
  );
}

function IconoBrujula() {
  return (
    <svg {...propsIcono}>
      <circle cx="12" cy="12" r="9" />
      <path d="m15 9-2.2 4.8L8 16l2.2-4.8L15 9Z" />
    </svg>
  );
}

function IconoMas() {
  return (
    <svg {...propsIcono}>
      <circle cx="12" cy="12" r="9" />
      <path d="M12 8.5v7M8.5 12h7" />
    </svg>
  );
}

function IconoRayo() {
  return (
    <svg {...propsIcono} className="h-4 w-4">
      <path d="M13 2 4.5 13.5H11l-1 8.5 8.5-11.5H12l1-8.5Z" />
    </svg>
  );
}

function IconoInstagram() {
  return (
    <svg {...propsIcono}>
      <rect x="3.5" y="3.5" width="17" height="17" rx="5" />
      <circle cx="12" cy="12" r="3.8" />
      <circle cx="17" cy="7" r="1" fill="currentColor" stroke="none" />
    </svg>
  );
}

function IconoMail() {
  return (
    <svg {...propsIcono}>
      <rect x="3" y="5" width="18" height="14" rx="3" />
      <path d="m4 7.5 7.1 5a1.6 1.6 0 0 0 1.8 0L20 7.5" />
    </svg>
  );
}

function IconoLinkedin() {
  return (
    <svg {...propsIcono}>
      <rect x="3.5" y="3.5" width="17" height="17" rx="3" />
      <path d="M8 10.5V17M8 7.5v.01" />
      <path d="M12 17v-3.6a2.4 2.4 0 0 1 4.8 0V17M12 10.5V17" />
    </svg>
  );
}

function IconoFacebook() {
  return (
    <svg {...propsIcono}>
      <circle cx="12" cy="12" r="8.5" />
      <path d="M13.5 20v-7h2.3M13.5 13h-3M13.5 13v-2.3A2.2 2.2 0 0 1 15.7 8.5h.3" />
    </svg>
  );
}
