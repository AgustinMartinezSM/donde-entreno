import type { Metadata } from "next";
import { Header } from "../../components/layout/Header";
import { SportsCatalog } from "../../components/deportes/SportsCatalog";
import { AppLinkButton } from "../../components/ui/AppLinkButton";
import { obtenerDeportes } from "../../services/deportesService";
import type { Deporte } from "../../types/deporte";

export const dynamic = "force-dynamic";

export const metadata: Metadata = {
  title: "Deportes",
  description:
    "Explorá deportes disponibles y encontrá actividades deportivas cerca tuyo en DondeEntreno.",
  openGraph: {
    title: "Deportes - DondeEntreno",
    description:
      "Explorá deportes disponibles y encontrá actividades, clubes, profes y espacios cerca tuyo.",
  },
};

type EstadoCatalogoProps = {
  titulo: string;
  descripcion: string;
  linkTexto: string;
};

function EstadoCatalogo({
  titulo,
  descripcion,
  linkTexto,
}: EstadoCatalogoProps) {
  return (
    <section className="mt-10 rounded-[var(--radius-xl)] border border-[#DDEAF3] bg-white/85 p-6 text-center shadow-[0_16px_40px_rgba(12,52,80,0.08)] sm:p-8">
      <h2 className="text-2xl font-extrabold text-[var(--color-primary)]">
        {titulo}
      </h2>
      <p className="mx-auto mt-3 max-w-xl text-sm leading-6 text-[var(--color-muted)] sm:text-base">
        {descripcion}
      </p>
      <AppLinkButton href="/explorar" className="mt-6">
        {linkTexto}
      </AppLinkButton>
    </section>
  );
}

export default async function DeportesPage() {
  let deportes: Deporte[] = [];
  let huboError = false;

  try {
    deportes = await obtenerDeportes();
  } catch {
    huboError = true;
  }

  return (
    <main className="min-h-screen bg-gradient-to-br from-[#F8FAFC] via-white to-[#E8F6FB] text-[var(--color-text)]">
      <section className="mx-auto w-full max-w-6xl px-4 py-6">
        <Header />

        <div className="py-8 sm:py-10">
          <section className="overflow-hidden rounded-[var(--radius-xl)] border border-[#DDEAF3] bg-gradient-to-br from-white via-[#F8FCFE] to-[#E8F6FB] p-5 shadow-[0_24px_60px_rgba(12,52,80,0.12)] sm:p-8 lg:p-10">
            <div className="max-w-3xl">
              <p className="text-sm font-bold uppercase tracking-[0.2em] text-[var(--color-secondary)]">
                DEPORTES
              </p>
              <h1 className="mt-3 text-4xl font-extrabold leading-tight text-[var(--color-primary)] sm:text-5xl">
                Elegí qué querés entrenar
              </h1>
              <p className="mt-4 text-base leading-7 text-[var(--color-muted)] sm:text-lg">
                Buscá por deporte y descubrí clases, clubes, profes y espacios
                para moverte cerca tuyo.
              </p>
              <div className="mt-6 flex flex-col gap-3 sm:flex-row">
                <AppLinkButton href="/explorar">
                  Ver todas las actividades
                </AppLinkButton>
                <AppLinkButton href="/" variant="secondary">
                  Volver al inicio
                </AppLinkButton>
              </div>
            </div>
          </section>

          {huboError ? (
            <EstadoCatalogo
              titulo="No pudimos cargar los deportes"
              descripcion="No pudimos traer el listado ahora. Podés volver a intentar en unos segundos o explorar actividades disponibles."
              linkTexto="Explorar actividades"
            />
          ) : deportes.length === 0 ? (
            <EstadoCatalogo
              titulo="Todavía no hay deportes disponibles"
              descripcion="Mientras preparamos nuevas opciones, podés revisar las actividades cargadas."
              linkTexto="Explorar actividades"
            />
          ) : (
            <SportsCatalog deportes={deportes} />
          )}
        </div>
      </section>
    </main>
  );
}
