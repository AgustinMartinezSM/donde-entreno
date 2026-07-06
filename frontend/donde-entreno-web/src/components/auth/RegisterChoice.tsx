"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { obtenerRutaInicialPorRol } from "../../lib/authRedirects";
import { useAuthSession } from "./AuthSessionProvider";
import { AppLinkButton } from "../ui/AppLinkButton";
import { SectionHeader } from "../ui/SectionHeader";
import { StatusMessage } from "../ui/StatusMessage";
import { SurfaceCard } from "../ui/SurfaceCard";
import { BrandName } from "../brand/BrandName";

export function RegisterChoice() {
  const router = useRouter();
  const { status, sesion, usuario } = useAuthSession();

  useEffect(() => {
    if (status !== "authenticated") {
      return;
    }

    const rolActual = usuario?.rol ?? sesion?.usuario.rol;

    if (!rolActual) {
      return;
    }

    router.replace(obtenerRutaInicialPorRol(rolActual));
  }, [router, sesion, status, usuario]);

  return (
    <main className="min-h-screen bg-gradient-to-br from-[#F8FAFC] via-white to-[#E8F6FB] px-4 py-8 text-[var(--color-text)] sm:py-12">
      <section className="mx-auto w-full max-w-6xl">
        <div className="mx-auto max-w-3xl text-center">
          <p className="text-xs font-extrabold uppercase tracking-[0.22em] text-[var(--color-secondary)]">
            <BrandName className="inline" />
          </p>
          <h1 className="mt-4 text-3xl font-extrabold text-[var(--color-primary)] sm:text-5xl">
            Crear cuenta en <BrandName className="inline" />
          </h1>
          <p className="mt-4 text-base leading-7 text-[var(--color-muted)] sm:text-lg">
            Elegí cómo querés usar la plataforma.
          </p>
        </div>

        {status === "loading" ? (
          <StatusMessage
            variant="info"
            role="status"
            className="mx-auto mt-8 max-w-lg text-center"
          >
            Revisando tu sesión...
          </StatusMessage>
        ) : null}

        <div className="mt-10 grid gap-5 lg:grid-cols-2">
          <SurfaceCard className="flex h-full flex-col p-6 transition duration-200 ease-out hover:-translate-y-1 hover:shadow-[0_24px_60px_rgba(12,52,80,0.14)] sm:p-8">
            <SectionHeader
              eyebrow="Usuario"
              title="Quiero encontrar actividades"
              description="Creá una cuenta para guardar tu información y preparar futuras funciones."
            />
            <div className="mt-8 flex flex-1 flex-col justify-end">
              <AppLinkButton href="/registro/usuario" fullWidth>
                Crear cuenta de usuario
              </AppLinkButton>
            </div>
          </SurfaceCard>

          <SurfaceCard
            variant="success"
            className="flex h-full flex-col p-6 transition duration-200 ease-out hover:-translate-y-1 hover:shadow-[0_24px_60px_rgba(12,52,80,0.14)] sm:p-8"
          >
            <SectionHeader
              eyebrow="Publicador"
              title="Quiero publicar actividades"
              description="Creá un perfil para enviar actividades a revisión y gestionarlas desde tu panel."
            />
            <div className="mt-8 flex flex-1 flex-col justify-end">
              <AppLinkButton
                href="/registro/publicador"
                variant="success"
                fullWidth
              >
                Crear cuenta de publicador
              </AppLinkButton>
            </div>
          </SurfaceCard>
        </div>

        <div className="mt-8 text-center">
          <AppLinkButton href="/login" variant="secondary">
            Ya tengo cuenta
          </AppLinkButton>
        </div>
      </section>
    </main>
  );
}
