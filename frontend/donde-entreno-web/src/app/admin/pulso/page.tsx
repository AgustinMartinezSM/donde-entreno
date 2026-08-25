"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";

import { AdminGuard } from "../../../components/admin/AdminGuard";
import { Header } from "../../../components/layout/Header";
import { SectionHeader } from "../../../components/ui/SectionHeader";
import { StatusMessage } from "../../../components/ui/StatusMessage";
import { SurfaceCard } from "../../../components/ui/SurfaceCard";
import { obtenerSesionAdmin } from "../../../services/authService";
import { obtenerPulso, type Pulso } from "../../../services/pulsoService";

/*
  El pulso del producto.

  Existe para responder una pregunta antes de seguir construyendo: de
  todo lo que se agregó en las fases sociales, ¿qué se está usando?

  Muestra los números CRUDOS, sin porcentajes ni gráficos: con este
  volumen, un gráfico haría parecer tendencia lo que es ruido.
*/
export default function AdminPulsoPage() {
  return (
    <AdminGuard>
      <PulsoDelProducto />
    </AdminGuard>
  );
}

function PulsoDelProducto() {
  const router = useRouter();
  const [pulso, setPulso] = useState<Pulso | null>(null);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let componenteActivo = true;
    const sesion = obtenerSesionAdmin();

    if (!sesion) {
      router.replace(`/login?returnTo=${encodeURIComponent("/admin/pulso")}`);
      return () => {
        componenteActivo = false;
      };
    }

    async function cargar() {
      try {
        const actual = await obtenerPulso(sesion!.accessToken);

        if (componenteActivo) {
          setPulso(actual);
          setError(null);
        }
      } catch {
        if (componenteActivo) {
          setError("No pudimos leer el pulso. Probá de nuevo en unos minutos.");
        }
      } finally {
        if (componenteActivo) {
          setCargando(false);
        }
      }
    }

    void cargar();

    return () => {
      componenteActivo = false;
    };
  }, [router]);

  return (
    <main className="min-h-screen text-[var(--color-text)]">
      <section className="mx-auto w-full max-w-5xl px-4 py-6">
        <Header />

        <div className="mt-6">
          <SectionHeader
            eyebrow="Administración"
            title="El pulso del producto"
            description="Qué hay y qué se está usando. Son conteos agregados: ningún dato de una persona ni contenido de nadie."
          />
        </div>

        {error ? (
          <StatusMessage variant="error" role="alert" className="mt-6">
            {error}
          </StatusMessage>
        ) : cargando ? (
          <StatusMessage variant="info" role="status" className="mt-6">
            Contando...
          </StatusMessage>
        ) : (
          <div className="mt-6 grid gap-6">
            {(pulso?.bloques ?? []).map((bloque) => (
              <SurfaceCard as="section" key={bloque.titulo} className="p-5 sm:p-6">
                <h2 className="text-xs font-extrabold uppercase tracking-[0.16em] text-[var(--color-secondary)]">
                  {bloque.titulo}
                </h2>

                <dl className="mt-4 grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-4">
                  {bloque.metricas.map((metrica) => (
                    <div key={metrica.etiqueta}>
                      <dt className="text-xs text-[var(--color-muted)]">
                        {metrica.etiqueta}
                      </dt>
                      <dd
                        className={`mt-1 text-2xl font-extrabold ${
                          metrica.total === 0
                            ? "text-[var(--color-muted)]"
                            : "text-[var(--color-primary)]"
                        }`}
                      >
                        {metrica.total}
                      </dd>

                      {/*
                        La ventana de 30 días es lo que distingue algo
                        vivo de algo que pasó una vez hace meses.
                      */}
                      {metrica.ultimos30Dias !== null ? (
                        <p className="text-[11px] text-[var(--color-muted)]">
                          {metrica.ultimos30Dias} en 30 días
                        </p>
                      ) : null}
                    </div>
                  ))}
                </dl>
              </SurfaceCard>
            ))}

            <SurfaceCard variant="info" className="p-5">
              <p className="text-sm leading-6 text-[var(--color-muted)]">
                <strong className="text-[var(--color-primary)]">
                  Cómo leer esto:
                </strong>{" "}
                si las superficies sociales están en cero o casi, la
                conclusión útil no es que falten funciones — es que el valor
                estaba en otro lado. Vale mirarlo antes de invertir otro
                bloque en profundizarlas.
              </p>
            </SurfaceCard>
          </div>
        )}
      </section>
    </main>
  );
}
