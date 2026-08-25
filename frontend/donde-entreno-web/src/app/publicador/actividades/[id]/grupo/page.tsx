"use client";

import Link from "next/link";
import { use, useEffect, useState } from "react";

import { PublicadorGuard } from "../../../../../components/auth/PublicadorGuard";
import { useAuthSession } from "../../../../../components/auth/AuthSessionProvider";
import { AvisosDelGrupo } from "../../../../../components/publicador/AvisosDelGrupo";
import { PublicadorPageHeader } from "../../../../../components/publicador/PublicadorPageHeader";
import { StatusMessage } from "../../../../../components/ui/StatusMessage";
import { obtenerActividadPublicador } from "../../../../../services/publicadorService";

export default function GrupoDeLaActividadPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = use(params);

  return (
    <PublicadorGuard>
      <Contenido actividadId={Number(id)} />
    </PublicadorGuard>
  );
}

function Contenido({ actividadId }: { actividadId: number }) {
  const { accessToken } = useAuthSession();
  const [titulo, setTitulo] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let componenteActivo = true;

    if (!accessToken) {
      return () => {
        componenteActivo = false;
      };
    }

    /*
      La carga va dentro del efecto (patrón del resto del proyecto):
      llamar desde el efecto a algo que hace setState en cascada lo
      marca el lint, y con razón.
    */
    async function cargar() {
      try {
        const actividad = await obtenerActividadPublicador(
          actividadId,
          accessToken as string
        );

        if (componenteActivo) {
          setTitulo(actividad.titulo);
        }
      } catch {
        if (componenteActivo) {
          setError("No pudimos cargar la actividad.");
        }
      }
    }

    void cargar();

    return () => {
      componenteActivo = false;
    };
  }, [accessToken, actividadId]);

  return (
    <main className="min-h-screen text-[var(--color-text)]">
      <section className="mx-auto w-full max-w-4xl px-4 py-6">
        <PublicadorPageHeader
          title="Grupo de la actividad"
          description="Avisale a quienes vienen: cambios de horario, suspensiones, lo que necesiten saber."
        />

        <div className="mt-4">
          <Link
            href={`/publicador/actividades/${actividadId}`}
            className="text-sm font-bold text-[var(--color-muted)] transition hover:text-[var(--color-primary)]"
          >
            ← Volver a la actividad
          </Link>
        </div>

        {error ? (
          <StatusMessage variant="error" role="alert" className="mt-6">
            {error}
          </StatusMessage>
        ) : (
          <div className="mt-6">
            <AvisosDelGrupo
              actividadId={actividadId}
              actividadTitulo={titulo ?? "tu actividad"}
            />
          </div>
        )}
      </section>
    </main>
  );
}
