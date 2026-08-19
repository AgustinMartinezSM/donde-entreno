"use client";

import { esRolAdmin, esRolPublicador } from "../../lib/authRedirects";
import { AppLinkButton } from "../ui/AppLinkButton";
import { SurfaceCard } from "../ui/SurfaceCard";
import { IconoMenuCuenta } from "./IconoMenuCuenta";

/*
  Puente al "otro mundo" de quien tiene dos: el publicador y el admin
  también son personas que entrenan, y su espacio de gestión ahora es
  una tarjeta clara dentro del perfil en vez del destino forzado de
  "Mi perfil". Es el conmutador de espacio del plan: visible arriba de
  la columna de apoyo, sin invadir el contenido.

  Para el usuario común no existe: no renderiza nada.
*/
export function EspacioDeRol({ rol }: { rol: string | null }) {
  if (!rol) {
    return null;
  }

  if (esRolAdmin(rol)) {
    return (
      <TarjetaEspacio
        eyebrow="Equipo"
        titulo="Administración"
        descripcion="Revisá las solicitudes de publicación y moderá las imágenes de la comunidad."
        icono="solicitudes"
        principal={{ href: "/admin/solicitudes", label: "Ir a administración" }}
        secundarias={[
          { href: "/admin/imagenes", label: "Imágenes pendientes" },
        ]}
      />
    );
  }

  if (esRolPublicador(rol)) {
    return (
      <TarjetaEspacio
        eyebrow="También publicás"
        titulo="Tu espacio de publicador"
        descripcion="Gestioná tus actividades, solicitudes e imágenes desde tu espacio."
        icono="publicador"
        principal={{ href: "/publicador", label: "Ir a mi espacio" }}
        secundarias={[
          { href: "/publicador/actividades", label: "Mis actividades" },
          { href: "/publicador/solicitudes", label: "Mis solicitudes" },
        ]}
      />
    );
  }

  return null;
}

function TarjetaEspacio({
  eyebrow,
  titulo,
  descripcion,
  icono,
  principal,
  secundarias,
}: {
  eyebrow: string;
  titulo: string;
  descripcion: string;
  icono: Parameters<typeof IconoMenuCuenta>[0]["tipo"];
  principal: { href: string; label: string };
  secundarias: Array<{ href: string; label: string }>;
}) {
  return (
    <SurfaceCard as="section" className="p-5">
      <div className="flex items-start gap-3">
        <span
          aria-hidden="true"
          className="gradient-brand flex h-11 w-11 shrink-0 items-center justify-center rounded-[14px] text-white shadow-[var(--shadow-button)]"
        >
          <IconoMenuCuenta tipo={icono} className="h-5 w-5" />
        </span>

        <div className="min-w-0">
          <p className="text-[11px] font-extrabold uppercase tracking-[0.16em] text-[var(--color-secondary)]">
            {eyebrow}
          </p>
          <h2 className="mt-0.5 text-lg font-extrabold leading-tight text-[var(--color-primary)]">
            {titulo}
          </h2>
        </div>
      </div>

      <p className="mt-3 text-sm leading-6 text-[var(--color-muted)]">
        {descripcion}
      </p>

      <div className="mt-4 grid gap-2">
        <AppLinkButton href={principal.href} size="sm" fullWidth>
          {principal.label}
        </AppLinkButton>

        {secundarias.length > 0 ? (
          <div
            className={`grid gap-2 ${
              secundarias.length > 1 ? "grid-cols-2" : ""
            }`}
          >
            {secundarias.map((accion) => (
              <AppLinkButton
                key={accion.href}
                href={accion.href}
                variant="secondary"
                size="sm"
                fullWidth
              >
                {accion.label}
              </AppLinkButton>
            ))}
          </div>
        ) : null}
      </div>
    </SurfaceCard>
  );
}
