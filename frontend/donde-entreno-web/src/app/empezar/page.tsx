import type { Metadata } from "next";
import Link from "next/link";

import { EntradaGuiada } from "../../components/empezar/EntradaGuiada";
import { Header } from "../../components/layout/Header";
import { SocialActivityCard } from "../../components/social/SocialActivityCard";
import { AppLinkButton } from "../../components/ui/AppLinkButton";
import { SectionHeader } from "../../components/ui/SectionHeader";
import { SurfaceCard } from "../../components/ui/SurfaceCard";
import { DEFAULT_CITY_SLUG } from "../../lib/ciudadActiva";
import { GUIAS } from "../../lib/guias";
import { buscarActividades } from "../../services/actividadService";
import type { Actividad } from "../../types/actividad";

export const dynamic = "force-dynamic";

export const metadata: Metadata = {
  title: "Empezar a entrenar",
  description:
    "¿Nunca entrenaste o hace mucho que no? Encontrá actividades para principiantes en tu ciudad y descubrí qué deporte va con vos.",
  alternates: { canonical: "/empezar" },
  openGraph: {
    title: "Empezar a entrenar - DondeEntreno",
    description:
      "Actividades para principiantes y una guía corta para dar el primer paso.",
  },
};

/*
  "Para arrancar de cero" (Fase 10).

  Es una landing que ORDENA lo que ya existe, no un motor nuevo: las
  actividades salen de la búsqueda pública con nivel=PRINCIPIANTE, y la
  recomendación personalizada la sigue haciendo Dondi.

  Todo lo que se afirma acá es general y verificable (qué llevar, que se
  puede preguntar antes de ir). No hay promesas sobre precios, cupos ni
  clases de prueba: eso lo dice cada actividad, no esta página.
*/
export default async function EmpezarPage() {
  let actividades: Actividad[] = [];

  try {
    const respuesta = await buscarActividades({
      ciudadSlug: DEFAULT_CITY_SLUG,
      nivel: "PRINCIPIANTE",
      page: 0,
      size: 6,
    });

    /*
      Mismo criterio que la home: con foto adelante. Para alguien que
      nunca entrenó, ver el lugar importa más que a nadie.
    */
    actividades = [...respuesta.contenido].sort(
      (a, b) =>
        Number(Boolean(b.imagenPrincipalUrl)) -
        Number(Boolean(a.imagenPrincipalUrl))
    );
  } catch (error) {
    console.error("Error al cargar actividades para principiantes:", error);
  }

  return (
    <main className="min-h-screen overflow-x-clip text-[var(--color-text)]">
      <section className="mx-auto w-full max-w-5xl px-4 py-6">
        <Header />

        <div className="mt-6">
          <SectionHeader
            eyebrow="Primeros pasos"
            title="Empezar de cero está bien"
            description="Si nunca entrenaste, o hace años que no, esta página es para vos: qué deporte puede ir con vos, qué actividades para principiantes hay cerca y qué esperar del primer día."
          />
        </div>

        <div className="mt-6 grid gap-6">
          <EntradaGuiada />

          <section>
            <div className="flex flex-wrap items-end justify-between gap-3">
              <h2 className="text-lg font-extrabold text-[var(--color-primary)]">
                Actividades para principiantes
              </h2>

              <AppLinkButton
                href="/explorar?nivel=PRINCIPIANTE&page=0"
                variant="outline"
              >
                Ver todas
              </AppLinkButton>
            </div>

            {actividades.length === 0 ? (
              /*
                Sin datos no se dibuja una grilla vacía ni se inventa
                contenido: se ofrece el camino que sí funciona.
              */
              <SurfaceCard className="mt-3 p-5">
                <p className="text-sm text-[var(--color-muted)]">
                  Ahora mismo no podemos mostrar actividades para
                  principiantes. Probá{" "}
                  <Link
                    href="/explorar"
                    className="font-semibold text-[var(--color-primary)] underline-offset-4 hover:underline"
                  >
                    explorar todas las actividades
                  </Link>
                  .
                </p>
              </SurfaceCard>
            ) : (
              <div className="mt-3 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
                {actividades.map((actividad) => (
                  <SocialActivityCard
                    key={actividad.id}
                    actividad={actividad}
                    variante="compacta"
                  />
                ))}
              </div>
            )}
          </section>

          {/*
            Las guías por deporte, cuando existen: quien ya sabe qué
            quiere probar necesita el detalle, no la orientación.
          */}
          {GUIAS.length > 0 ? (
            <SurfaceCard as="section" className="p-5 sm:p-6">
              <h2 className="text-lg font-extrabold text-[var(--color-primary)]">
                Guías por deporte
              </h2>
              <p className="mt-1 text-sm text-[var(--color-muted)]">
                Qué es, cómo es la primera clase y qué preguntar antes de
                anotarte.
              </p>

              <ul className="mt-3 flex flex-wrap gap-2">
                {GUIAS.map((guia) => (
                  <li key={guia.slug}>
                    <Link
                      href={`/guias/${guia.slug}`}
                      className="inline-block rounded-full border border-[var(--color-border)] px-3 py-1.5 text-sm font-semibold text-[var(--color-primary)] transition hover:border-[var(--color-primary)]"
                    >
                      {guia.titulo}
                    </Link>
                  </li>
                ))}
              </ul>
            </SurfaceCard>
          ) : null}

          <SurfaceCard as="section" className="p-5 sm:p-6">
            <h2 className="text-lg font-extrabold text-[var(--color-primary)]">
              Qué esperar del primer día
            </h2>

            <div className="mt-3 grid gap-4 sm:grid-cols-2">
              <div>
                <h3 className="text-sm font-bold">Preguntá antes de ir</h3>
                <p className="mt-1 text-sm leading-6 text-[var(--color-muted)]">
                  Cada actividad tiene su contacto y su horario publicado. Si
                  algo no figura —si hace falta ropa puntual, si podés ir a
                  mirar—, se pregunta y listo: nadie empieza sabiendo.
                </p>
              </div>

              <div>
                <h3 className="text-sm font-bold">Llevá poco</h3>
                <p className="mt-1 text-sm leading-6 text-[var(--color-muted)]">
                  Ropa cómoda, calzado deportivo y agua alcanzan para casi
                  cualquier primera clase. El equipamiento específico se compra
                  después de saber si te gusta, no antes.
                </p>
              </div>

              <div>
                <h3 className="text-sm font-bold">Nivel principiante es literal</h3>
                <p className="mt-1 text-sm leading-6 text-[var(--color-muted)]">
                  Las actividades marcadas para principiantes esperan gente sin
                  experiencia. Llegar sin saber nada es exactamente lo previsto.
                </p>
              </div>

              <div>
                <h3 className="text-sm font-bold">Probar no es elegir</h3>
                <p className="mt-1 text-sm leading-6 text-[var(--color-muted)]">
                  Podés ir a más de un lugar antes de decidir. Guardá las que te
                  interesen con el corazón y compará con calma.
                </p>
              </div>
            </div>
          </SurfaceCard>
        </div>
      </section>
    </main>
  );
}
