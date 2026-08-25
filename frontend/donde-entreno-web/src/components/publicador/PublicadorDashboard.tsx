"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { useAuthSession } from "../auth/AuthSessionProvider";
import { AppLinkButton } from "../ui/AppLinkButton";
import { SectionHeader } from "../ui/SectionHeader";
import { StatusMessage } from "../ui/StatusMessage";
import { SurfaceCard } from "../ui/SurfaceCard";
import { PublicadorPageHeader } from "./PublicadorPageHeader";
import { PublicadorMetricasPanel } from "./PublicadorMetricasPanel";
import {
  PublicadorApiError,
  obtenerMetricasPublicador,
  obtenerPerfilPublicador,
} from "../../services/publicadorService";
import type {
  MetricasPublicador,
  PerfilPublicadorActual,
} from "../../types/publicador";
import { useState } from "react";

export function PublicadorDashboard() {
  const router = useRouter();
  const { accessToken, cerrarSesion } = useAuthSession();
  const [perfil, setPerfil] = useState<PerfilPublicadorActual | null>(null);
  const [metricas, setMetricas] = useState<MetricasPublicador | null>(null);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let componenteActivo = true;

    if (!accessToken) {
      return () => {
        componenteActivo = false;
      };
    }

    obtenerMetricasPublicador(accessToken)
      .then((metricasActuales) => {
        if (componenteActivo) {
          setMetricas(metricasActuales);
        }
      })
      .catch(() => {
        // Las métricas son un resumen best-effort: si fallan, el panel
        // sigue funcionando sin ellas (el perfil es lo crítico).
      });

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
            setError("No tenés permisos para ver esto: es una cuenta de publicador lo que hace falta.");
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
    <main className="min-h-screen px-4 py-8 text-[var(--color-text)] sm:py-12">
      <section className="mx-auto w-full max-w-6xl">
        <PublicadorPageHeader
          title={
            perfil ? `Hola, ${perfil.nombre}` : "Tu espacio para publicar"
          }
          description="Gestioná tu presencia deportiva: publicá actividades, seguí el estado de tus solicitudes y mirá cómo crece tu comunidad."
          action={
            /* Un solo CTA primario: el resto vive en las tiles de métricas
               y en los accesos rápidos (antes estaban triplicados). */
            <AppLinkButton href="/publicador/solicitudes/nueva" fullWidth>
              Nueva solicitud
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

        {metricas && !error ? (
          <PublicadorMetricasPanel metricas={metricas} />
        ) : null}

        {perfil ? (
          <div className="mt-6 grid gap-5 lg:grid-cols-[1.1fr_0.9fr]">
            <SurfaceCard className="border-[var(--color-success-border)] bg-gradient-to-br from-white via-white to-[var(--color-surface-soft)] p-6 sm:p-8">
              <SectionHeader
                eyebrow="Perfil"
                title="Así te ve la comunidad"
                description="Estos datos identifican tus publicaciones. Mantenerlos completos mejora tu presencia ante quienes buscan dónde entrenar."
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
                <div className="mt-6 rounded-[20px] border border-[var(--color-success-border)] bg-[#E6F7EF]/55 p-4">
                  <p className="text-xs font-extrabold uppercase tracking-[0.16em] text-[var(--color-secondary)]">
                    Descripción
                  </p>
                  <p className="mt-2 text-sm leading-6 text-[var(--color-muted)]">
                    {perfil.descripcion}
                  </p>
                </div>
              ) : null}
            </SurfaceCard>

            <SurfaceCard variant="info" className="p-6 sm:p-8">
              <SectionHeader
                eyebrow="Accesos rápidos"
                title="Publicá con seguimiento"
                description="Cargá nuevas solicitudes, revisá las aprobadas y seguí cada estado desde tu panel."
              />

              <div className="mt-8 grid gap-3">
                <AppLinkButton
                  href="/publicador/solicitudes/nueva"
                  fullWidth
                >
                  Nueva solicitud
                </AppLinkButton>
                <AppLinkButton
                  href="/publicador/actividades"
                  variant="secondary"
                  fullWidth
                >
                  Mis actividades aprobadas
                </AppLinkButton>
                <AppLinkButton
                  href="/publicador/solicitudes"
                  variant="outline"
                  fullWidth
                >
                  Mis solicitudes
                </AppLinkButton>
                <AppLinkButton
                  href="/publicador/solicitudes-cambio"
                  variant="outline"
                  fullWidth
                >
                  Solicitudes de cambio
                </AppLinkButton>
                <AppLinkButton
                  href="/publicador/fotos"
                  variant="outline"
                  fullWidth
                >
                  Centro de fotos
                </AppLinkButton>
                <AppLinkButton
                  href="/publicador/novedades"
                  variant="outline"
                  fullWidth
                >
                  Contar una novedad
                </AppLinkButton>
                <AppLinkButton
                  href="/publicador/eventos"
                  variant="outline"
                  fullWidth
                >
                  Organizar un evento
                </AppLinkButton>
                <AppLinkButton
                  href="/publicador/perfil"
                  variant="outline"
                  fullWidth
                >
                  Editar mi perfil
                </AppLinkButton>
                {/*
                  El espejo que faltaba: cómo te ve la gente. Solo si el
                  perfil está activo — la ruta pública devuelve 404 para
                  perfiles inactivos y un link a un 404 es peor que nada.
                  Es la vista PUBLICADA: lo pendiente de moderación no
                  aparece ahí todavía.
                */}
                {perfil?.activo ? (
                  <AppLinkButton
                    href={`/publicadores/${perfil.id}`}
                    variant="outline"
                    fullWidth
                  >
                    Ver mi perfil público
                  </AppLinkButton>
                ) : null}
                <StatusMessage variant="info">
                  Revisá las actividades que ya fueron aprobadas y publicadas.
                  También podés enviar una nueva solicitud o seguir el estado de
                  las que están en revisión.
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
    <div className="rounded-[18px] border border-[var(--color-border-soft)] bg-white/80 p-4 shadow-[0_10px_24px_rgba(12,52,80,0.05)]">
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
