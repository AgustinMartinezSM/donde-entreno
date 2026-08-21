"use client";

import { useAuthSession } from "../auth/AuthSessionProvider";
import { AppLinkButton } from "../ui/AppLinkButton";
import { SurfaceCard } from "../ui/SurfaceCard";

const BENEFICIOS = [
  {
    titulo: "Guardá tus favoritas",
    detalle: "Marcá actividades y volvé a ellas cuando quieras.",
    icono: "guardar",
  },
  {
    titulo: "Seguí clubes y profes",
    detalle: "Enterate de las actividades de los publicadores que elegís.",
    icono: "corazon",
  },
  {
    titulo: "Elegí tus preferencias",
    detalle: "Tu ciudad, tus guardados y tus intereses, siempre a mano.",
    icono: "cuenta",
  },
] as const;

function IconoBeneficio({ tipo }: { tipo: (typeof BENEFICIOS)[number]["icono"] }) {
  const comun = {
    viewBox: "0 0 24 24",
    className: "h-5 w-5",
    fill: "none",
    stroke: "currentColor",
    strokeWidth: 2,
    strokeLinecap: "round" as const,
    strokeLinejoin: "round" as const,
    "aria-hidden": true,
  };

  if (tipo === "guardar") {
    return (
      <svg {...comun}>
        <path d="M6 3h12a1 1 0 0 1 1 1v17l-7-4.2L5 21V4a1 1 0 0 1 1-1z" />
      </svg>
    );
  }

  if (tipo === "corazon") {
    return (
      <svg {...comun}>
        <path d="M12 21s-6.7-4.3-9.3-8.1C.8 10 1.6 6.4 4.6 5.1c2-.9 4.3-.2 5.6 1.4L12 8.4l1.8-1.9c1.3-1.6 3.6-2.3 5.6-1.4 3 1.3 3.8 4.9 1.9 7.8C18.7 16.7 12 21 12 21z" />
      </svg>
    );
  }

  return (
    <svg {...comun}>
      <circle cx="12" cy="8" r="4" />
      <path d="M4 21c0-4 3.6-7 8-7s8 3 8 7" />
    </svg>
  );
}

/*
  CTA de creación de cuenta para la home.

  Solo se muestra a visitantes sin sesión: el objetivo es empujar el
  registro destacando lo que se desbloquea con una cuenta (guardar
  favoritos, seguir deportes, preferencias). Los usuarios logueados no
  la ven. Mientras se resuelve la sesión no se renderiza nada, para no
  mostrar un flash del bloque a quien ya tiene cuenta.
*/
export function HomeCrearCuentaCta() {
  const { status } = useAuthSession();

  if (status !== "guest") {
    return null;
  }

  return (
    <SurfaceCard
      as="section"
      variant="brand"
      className="mt-14 overflow-hidden p-6 sm:mt-16 sm:p-9"
    >
      <div className="grid gap-8 lg:grid-cols-[1fr_1fr] lg:items-center">
        <div>
          <p className="text-sm font-bold uppercase tracking-[0.2em] text-[#7FDCA8]">
            Creá tu cuenta gratis
          </p>
          <h2 className="mt-2 text-2xl font-extrabold leading-tight sm:text-3xl">
            Guardá lo que te gusta y armá tu espacio deportivo
          </h2>
          <p className="mt-3 max-w-xl text-base leading-7 text-[#BFDDEA]">
            Explorar es libre. Con una cuenta, además, guardás tus actividades
            favoritas, seguís clubes y profes, y elegís los deportes que más te
            interesan.
          </p>

          <div className="mt-6 flex flex-col gap-3 sm:flex-row">
            <AppLinkButton href="/registro" variant="success">
              Crear mi cuenta
            </AppLinkButton>
            <AppLinkButton
              href="/login"
              variant="secondary"
              className="border-white/40 bg-white/10 text-white hover:border-white hover:bg-white/20"
            >
              Ya tengo cuenta
            </AppLinkButton>
          </div>
        </div>

        <ul className="grid gap-3">
          {BENEFICIOS.map((beneficio) => (
            <li
              key={beneficio.titulo}
              className="flex items-start gap-4 rounded-[18px] border border-white/15 bg-white/10 p-4"
            >
              <span className="inline-flex h-11 w-11 shrink-0 items-center justify-center rounded-full bg-[var(--color-secondary)] text-white">
                <IconoBeneficio tipo={beneficio.icono} />
              </span>
              <div>
                <p className="font-extrabold">{beneficio.titulo}</p>
                <p className="mt-1 text-sm leading-6 text-[#BFDDEA]">
                  {beneficio.detalle}
                </p>
              </div>
            </li>
          ))}
        </ul>
      </div>
    </SurfaceCard>
  );
}
