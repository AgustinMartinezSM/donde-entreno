"use client";

import { useAuthSession } from "../auth/AuthSessionProvider";
import { PublishForm } from "../publicar/PublishForm";
import { AppLinkButton } from "../ui/AppLinkButton";
import { StatusMessage } from "../ui/StatusMessage";
import { PublicadorPageHeader } from "./PublicadorPageHeader";

export function PublicadorSolicitudNueva() {
  const { accessToken } = useAuthSession();

  return (
    <main className="min-h-screen bg-gradient-to-br from-[#F8FAFC] via-white to-[#E8F6FB] px-4 py-8 text-[var(--color-text)] sm:py-12">
      <section className="mx-auto w-full max-w-6xl">
        <PublicadorPageHeader
          title="Nueva solicitud"
          description="Cargá los datos de la actividad que querés publicar. Quedará pendiente de revisión."
          action={
            <AppLinkButton
              href="/publicador/solicitudes"
              variant="secondary"
              fullWidth
            >
              Volver a mis solicitudes
            </AppLinkButton>
          }
        />

        {!accessToken ? (
          <StatusMessage variant="warning" role="alert" className="mt-6">
            Necesitás iniciar sesión como publicador para crear una solicitud.
          </StatusMessage>
        ) : (
          <PublishForm
            modo="publicador"
            accessToken={accessToken}
            tituloExitoPersonalizado="Tu solicitud fue enviada desde tu panel"
          />
        )}
      </section>
    </main>
  );
}
