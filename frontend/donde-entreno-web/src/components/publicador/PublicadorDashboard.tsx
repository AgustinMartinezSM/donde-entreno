"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { useAuthSession } from "../auth/AuthSessionProvider";
import { AppButton } from "../ui/AppButton";
import { AppLinkButton } from "../ui/AppLinkButton";
import { SectionHeader } from "../ui/SectionHeader";
import { StatusMessage } from "../ui/StatusMessage";
import { SurfaceCard } from "../ui/SurfaceCard";
import { PublicadorPageHeader } from "./PublicadorPageHeader";
import {
  PublicadorApiError,
  obtenerPerfilPublicador,
} from "../../services/publicadorService";
import type { PerfilPublicadorActual } from "../../types/publicador";
import { useState } from "react";

export function PublicadorDashboard() {
  const router = useRouter();
  const { accessToken, cerrarSesion } = useAuthSession();
  const [perfil, setPerfil] = useState<PerfilPublicadorActual | null>(null);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let componenteActivo = true;

    if (!accessToken) {
      return () => {
        componenteActivo = false;
      };
    }

    obtenerPerfilPublicador(accessToken)
      .then((perfilActual) => {
        if (!componenteActivo) {
          return;
        }

        setPerfil(perfilActual);
        setError(null);
      })
      .catch((errorCarga: unknown) => {
        if (!componenteActivo) {
          return;
        }

        if (errorCarga instanceof PublicadorApiError) {
          if (errorCarga.status === 401) {
            cerrarSesion();
            router.replace(
              `/login?returnTo=${encodeURIComponent("/publicador")}`
            );
            return;
          }

          if (errorCarga.status === 403) {
            setError("No tenés permisos para acceder al panel publicador.");
            return;
          }

          setError(errorCarga.message);
          return;
        }

        setError("Ocurrió un problema inesperado al cargar tu perfil.");
      })
      .finally(() => {
        if (!componenteActivo) {
          return;
        }

        setCargando(false);
      });

    return () => {
      componenteActivo = false;
    };
  }, [accessToken, cerrarSesion, router]);

  return (
    <main className="min-h-screen bg-gradient-to-br from-[#F8FAFC] via-white to-[#E8F6FB] px-4 py-8 text-[var(--color-text)] sm:py-12">
      <section className="mx-auto w-full max-w-6xl">
        <PublicadorPageHeader
          title="Panel publicador"
          description="Desde este panel podés seguir el estado de tus solicitudes y cuidar la información de tu perfil."
          action={
            <AppLinkButton href="/publicador/solicitudes" fullWidth>
              Mis solicitudes
            </AppLinkButton>
          }
        />

        {cargando ? (
          <StatusMessage variant="info" role="status" className="mt-6">
            Cargando tu perfil publicador...
          </StatusMessage>
        ) : null}

        {error ? (
          <StatusMessage variant="error" role="alert" className="mt-6">
            {error}
          </StatusMessage>
        ) : null}

        {perfil ? (
          <div className="mt-6 grid gap-5 lg:grid-cols-[1.1fr_0.9fr]">
            <SurfaceCard className="p-6 sm:p-8">
              <SectionHeader
                eyebrow="Perfil"
                title={`Hola, ${perfil.nombre}`}
                description="Estos son los datos visibles para gestionar tus publicaciones."
              />

              <dl className="mt-8 grid gap-4 sm:grid-cols-2">
                <DatoPerfil etiqueta="Estado" valor={formatearEstadoPerfil(perfil.estado)} />
                <DatoPerfil etiqueta="Tipo" valor={formatearCatalogo(perfil.tipoPublicador)} />
                <DatoPerfil etiqueta="Ciudad principal" valor={perfil.ciudadPrincipalNombre} />
                <DatoPerfil etiqueta="WhatsApp" valor={perfil.whatsapp} />
                <DatoPerfil etiqueta="Email de contacto" valor={perfil.emailContacto} />
                <DatoPerfil etiqueta="Instagram" valor={perfil.instagram} />
                <DatoPerfil etiqueta="Teléfono" valor={perfil.telefonoContacto} />
                <DatoPerfil etiqueta="Verificado" valor={perfil.verificado ? "Sí" : "No"} />
              </dl>

              {perfil.descripcion ? (
                <div className="mt-6 rounded-[20px] border border-[#DDEAF3] bg-[#F8FAFC] p-4">
                  <p className="text-xs font-extrabold uppercase tracking-[0.16em] text-[var(--color-secondary)]">
                    Descripción
                  </p>
                  <p className="mt-2 text-sm leading-6 text-[var(--color-muted)]">
                    {perfil.descripcion}
                  </p>
                </div>
              ) : null}
            </SurfaceCard>

            <SurfaceCard variant="success" className="p-6 sm:p-8">
              <SectionHeader
                eyebrow="Accesos rápidos"
                title="Seguimiento de publicación"
                description="Revisá el avance de cada solicitud enviada."
              />

              <div className="mt-8 grid gap-3">
                <AppLinkButton href="/publicador/solicitudes" fullWidth>
                  Mis solicitudes
                </AppLinkButton>
                <AppButton type="button" variant="secondary" disabled fullWidth>
                  Nueva solicitud
                </AppButton>
                <StatusMessage variant="info">
                  La creación de nuevas solicitudes desde tu panel se suma en el
                  próximo paso. Mientras tanto, podés seguir tus envíos actuales.
                </StatusMessage>
              </div>
            </SurfaceCard>
          </div>
        ) : null}
      </section>
    </main>
  );
}

function DatoPerfil({
  etiqueta,
  valor,
}: {
  etiqueta: string;
  valor: string | null;
}) {
  return (
    <div className="rounded-[18px] border border-[#DDEAF3] bg-white/75 p-4">
      <dt className="text-xs font-extrabold uppercase tracking-[0.14em] text-[var(--color-secondary)]">
        {etiqueta}
      </dt>
      <dd className="mt-2 break-words text-sm font-bold text-[var(--color-primary)]">
        {valor || "No informado"}
      </dd>
    </div>
  );
}

function formatearEstadoPerfil(estado: string): string {
  return formatearCatalogo(estado);
}

function formatearCatalogo(valor: string): string {
  return valor
    .toLowerCase()
    .split("_")
    .map((parte) => parte.charAt(0).toUpperCase() + parte.slice(1))
    .join(" ");
}
