"use client";

import { useAuthSession } from "../auth/AuthSessionProvider";
import { esRolAdmin, esRolPublicador } from "../../lib/authRedirects";
import { AppLinkButton } from "../ui/AppLinkButton";
import { StatusMessage } from "../ui/StatusMessage";
import { SurfaceCard } from "../ui/SurfaceCard";
import { BrandName } from "../brand/BrandName";
import type { ReactNode } from "react";

export function PublishAccessGate() {
  const { status, sesion, usuario } = useAuthSession();
  const rolActual = usuario?.rol ?? sesion?.usuario.rol ?? null;

  if (status === "loading") {
    return (
      <StatusMessage variant="info" role="status" className="mt-6">
        Revisando tu sesión...
      </StatusMessage>
    );
  }

  if (status === "guest" || !rolActual) {
    return (
      <PublicarGateCard
        eyebrow="Cuenta publicadora"
        title="Para publicar una actividad necesitás una cuenta de publicador."
        description="Ingresá con tu cuenta o creá un perfil publicador. Para cuidar la calidad de las publicaciones, las actividades se envían a revisión antes de quedar visibles."
        actions={
          <>
            <AppLinkButton
              href="/login?returnTo=/publicar"
              variant="primary"
              fullWidth
            >
              Iniciar sesión
            </AppLinkButton>
            <AppLinkButton
              href="/registro/publicador"
              variant="success"
              fullWidth
            >
              Crear cuenta de publicador
            </AppLinkButton>
          </>
        }
      />
    );
  }

  if (esRolPublicador(rolActual)) {
    return (
      <PublicarGateCard
        eyebrow="Panel publicador"
        title="Creá tu solicitud desde el panel publicador."
        description="Así podemos asociar el envío a tu perfil, mostrarte el código de seguimiento y mantener el estado de revisión en tus solicitudes."
        actions={
          <>
            <AppLinkButton
              href="/publicador/solicitudes/nueva"
              variant="primary"
              fullWidth
            >
              Crear nueva solicitud
            </AppLinkButton>
            <AppLinkButton
              href="/publicador/solicitudes"
              variant="secondary"
              fullWidth
            >
              Ver mis solicitudes
            </AppLinkButton>
          </>
        }
      />
    );
  }

  if (esRolAdmin(rolActual)) {
    return (
      <PublicarGateCard
        eyebrow="Cuenta administradora"
        title="Estás usando una cuenta administradora."
        description="Para revisar solicitudes y gestionar el panel interno, continuá desde el panel admin. Las publicaciones de actividades se trabajan desde cuentas publicadoras."
        actions={
          <AppLinkButton href="/admin/solicitudes" variant="primary" fullWidth>
            Ir al panel admin
          </AppLinkButton>
        }
      />
    );
  }

  return (
    <PublicarGateCard
      eyebrow="Cuenta de usuario"
      title="Para publicar necesitás una cuenta de publicador."
      description="Tu cuenta actual sirve para usar DondeEntreno como usuario. Para enviar actividades a revisión y seguir su estado, creá un perfil publicador."
      actions={
        <AppLinkButton href="/registro/publicador" variant="success" fullWidth>
          Crear cuenta de publicador
        </AppLinkButton>
      }
    />
  );
}

function PublicarGateCard({
  eyebrow,
  title,
  description,
  actions,
}: {
  eyebrow: string;
  title: string;
  description: string;
  actions: ReactNode;
}) {
  return (
    <SurfaceCard className="mt-6 overflow-hidden border-[#BDE8D0] bg-gradient-to-br from-white via-[#F8FCFE] to-[#E6F7EF] p-6 sm:p-8">
      <div className="grid gap-6 lg:grid-cols-[1fr_0.72fr] lg:items-center">
        <div>
          <p className="text-xs font-extrabold uppercase tracking-[0.2em] text-[#167A4A]">
            {eyebrow}
          </p>
          <h2 className="mt-3 text-3xl font-extrabold text-[var(--color-primary)] sm:text-4xl">
            {title}
          </h2>
          <p className="mt-4 max-w-2xl text-sm leading-6 text-[var(--color-muted)] sm:text-base">
            {description}
          </p>
          <p className="mt-5 text-xs font-extrabold uppercase tracking-[0.18em] text-[var(--color-muted)]">
            <BrandName className="inline" />
          </p>
          <div className="mt-5 rounded-[20px] border border-[#BDE8D0] bg-white/75 p-4 text-sm leading-6 text-[#167A4A]">
            La revisión previa ayuda a que cada actividad tenga información
            clara de horarios, ubicación y contacto antes de publicarse.
          </div>
        </div>

        <div className="grid gap-3">{actions}</div>
      </div>
    </SurfaceCard>
  );
}
