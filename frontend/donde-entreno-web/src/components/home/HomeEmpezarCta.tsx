import { AppLinkButton } from "../ui/AppLinkButton";
import { SurfaceCard } from "../ui/SurfaceCard";

/*
  La puerta a /empezar (Fase 10).

  Va en la home cerca del cierre, al lado de "Cómo funciona": son
  vecinas de sentido — una explica cómo funciona el sitio y la otra
  cómo empezar a entrenar. Quien llegó hasta acá sin encontrar nada es
  exactamente a quien le sirve.
*/
export function HomeEmpezarCta() {
  return (
    <SurfaceCard as="section" className="mt-14 p-5 sm:mt-16 sm:p-7">
      <div className="grid gap-5 lg:grid-cols-[1.2fr_0.8fr] lg:items-center">
        <div>
          <p className="text-sm font-bold uppercase tracking-[0.2em] text-[var(--color-secondary)]">
            Primeros pasos
          </p>
          <h2 className="mt-2 text-2xl font-extrabold sm:text-3xl">
            ¿No sabés por dónde empezar?
          </h2>
          <p className="mt-3 max-w-2xl text-base leading-7 text-[var(--color-muted)]">
            Si nunca entrenaste, o hace años que no, hay una página para eso:
            tres preguntas para descubrir qué deporte puede ir con vos y las
            actividades para principiantes que hay cerca.
          </p>
        </div>

        <div className="flex lg:justify-end">
          <AppLinkButton href="/empezar">Empezar de cero</AppLinkButton>
        </div>
      </div>
    </SurfaceCard>
  );
}
