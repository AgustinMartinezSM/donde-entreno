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
          description="Cargá la información principal de la actividad. Cuanto más clara sea la solicitud, más fácil será revisarla."
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
          <>
            <StatusMessage variant="success" className="mt-6">
              Tené a mano nombre, ubicación, contacto y horarios reales. La
              actividad queda pendiente de revisión antes de publicarse.
            </StatusMessage>
            <PublishForm
              modo="publicador"
              accessToken={accessToken}
              tituloExitoPersonalizado="Tu solicitud fue enviada desde tu panel"
            />
          </>
        )}
      </section>
    </main>
  );
}
