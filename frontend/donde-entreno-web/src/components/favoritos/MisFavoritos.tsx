"use client";

import Link from "next/link";
import { useRef, useState, useSyncExternalStore } from "react";
import type { ReactNode } from "react";

import {
  quitarFavorito,
  useFavoritos,
  useScopeFavoritosResuelto,
  type FavoritoGuardado,
} from "../../lib/favoritos";
import { construirUrlImagenBackend } from "../../lib/backendUrl";
import {
  obtenerImagenActividad,
  obtenerImagenFallbackActividad,
} from "../../lib/activityImages";
import { ActivityImage } from "../actividad/ActivityImage";
import { AppLinkButton } from "../ui/AppLinkButton";
import { IconoGuardar } from "../ui/IconoGuardar";
import { SectionHeader } from "../ui/SectionHeader";
import { SurfaceCard } from "../ui/SurfaceCard";

function suscripcionVacia() {
  return () => {};
}

type MisFavoritosProps = {
  /*
    Acciones extra para el estado vacío. Desde el perfil se aprovecha
    para ofrecer los deportes elegidos y el asistente; en /favoritos, que
    no tiene ese contexto, queda solo "Explorar actividades".
  */
  accionesVacio?: ReactNode;
};

/*
  Listado de actividades guardadas (V1 local).
  Renderiza desde el snapshot guardado en el dispositivo, sin llamar a la
  API: así la página funciona incluso sin backend levantado.
*/
export function MisFavoritos({ accionesVacio }: MisFavoritosProps = {}) {
  const favoritos = useFavoritos();
  const [anuncio, setAnuncio] = useState("");
  const regionRef = useRef<HTMLDivElement | null>(null);

  /*
    Durante SSR/hidratación, useFavoritos devuelve el snapshot vacío del
    servidor: sin esta bandera, alguien CON favoritos vería un instante
    el estado "todavía no guardaste actividades" antes de hidratar.
    useSyncExternalStore con snapshot de servidor false es la forma
    idiomática de detectar la hidratación sin setState en un effect.
  */
  const hidratado = useSyncExternalStore(
    suscripcionVacia,
    () => true,
    () => false
  );

  /*
    Además de hidratar, hay que saber de quién es la lista: hasta que la
    sesión resuelve, los favoritos vienen vacíos a propósito y sin esto
    alguien con actividades guardadas vería "Todavía no guardaste
    actividades" antes de que aparezcan.
  */
  const scopeResuelto = useScopeFavoritosResuelto();
  const listo = hidratado && scopeResuelto;

  /*
    Al quitar una tarjeta, su botón se desmonta y el foco caería a <body>.
    Movemos el foco a un destino estable (el encabezado de la sección) y
    anunciamos el cambio en una región aria-live para lectores de pantalla.
  */
  function manejarQuitar(favorito: FavoritoGuardado) {
    quitarFavorito(favorito.slug);
    setAnuncio(`${favorito.titulo} se quitó de favoritos.`);
    regionRef.current?.focus();
  }

  return (
    <div>
      <div
        ref={regionRef}
        tabIndex={-1}
        className="outline-none focus-visible:outline-none"
      >
        {/*
          "En tu cuenta" y no "en este dispositivo": desde el bloque de
          sync (fcc4fa5) los favoritos de una sesión iniciada viven en el
          backend, y esta vista solo existe detrás de AuthGuard.
        */}
        <SectionHeader
          eyebrow="Guardadas en tu cuenta"
          title="Mis favoritos"
          description="Las actividades que marcaste para volver a mirar."
        />
      </div>

      <p aria-live="polite" className="sr-only">
        {anuncio}
      </p>

      {!listo ? (
        <div
          role="status"
          aria-label="Cargando tus favoritos"
          className="mt-6 grid gap-5 sm:grid-cols-2 lg:grid-cols-3"
        >
          {[0, 1, 2].map((indice) => (
            <div
              key={indice}
              aria-hidden="true"
              className="animate-pulse overflow-hidden rounded-[var(--radius-lg)] border border-[#DDEAF3] bg-white p-3"
            >
              <div className="h-40 rounded-[var(--radius-md)] bg-[#E8F6FB]" />
              <div className="p-2 pt-4">
                <div className="h-4 w-24 rounded-full bg-[#E8F6FB]" />
                <div className="mt-3 h-5 w-3/4 rounded-full bg-[#F1F5F9]" />
                <div className="mt-2 h-4 w-1/2 rounded-full bg-[#F1F5F9]" />
              </div>
            </div>
          ))}
        </div>
      ) : favoritos.length === 0 ? (
        <SurfaceCard className="mt-6 flex flex-col items-center gap-4 p-10 text-center">
          <span
            aria-hidden="true"
            className="inline-flex h-14 w-14 items-center justify-center rounded-full bg-[#E8F6FB] text-[var(--color-primary)]"
          >
            <IconoGuardar className="h-7 w-7" />
          </span>

          <div>
            <h3 className="text-lg font-extrabold text-[var(--color-primary)]">
              Todavía no guardaste actividades
            </h3>

            <p className="mt-2 max-w-md text-sm text-[var(--color-muted)]">
              Cuando encuentres una actividad que te interese, tocá el botón
              de guardar y va a aparecer acá para que la retomes cuando
              quieras.
            </p>
          </div>

          <AppLinkButton href="/explorar" className="mt-2">
            Explorar actividades
          </AppLinkButton>

          {accionesVacio}
        </SurfaceCard>
      ) : (
        <>
          <div className="mt-6 grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
            {favoritos.map((favorito) => {
              const imagenBackend = construirUrlImagenBackend(
                favorito.imagenPrincipalUrl
              );
              const imagenUrl = obtenerImagenActividad({
                imagenBackend,
                deporteSlug: favorito.deporteSlug,
              });
              const imagenFallbackUrl = obtenerImagenFallbackActividad({
                deporteSlug: favorito.deporteSlug,
              });

              return (
                <SurfaceCard
                  key={favorito.slug}
                  as="article"
                  className="group overflow-hidden p-3 transition duration-200 ease-out hover:-translate-y-1 hover:border-[#BFDDEA] hover:shadow-[0_22px_55px_rgba(12,52,80,0.14)]"
                >
                  <Link
                    href={`/actividades/${favorito.slug}`}
                    className="block"
                    aria-label={`Ver detalle de ${favorito.titulo}`}
                  >
                    <ActivityImage
                      src={imagenUrl}
                      fallbackSrc={imagenFallbackUrl}
                      alt={favorito.titulo}
                      fallbackText={favorito.deporteNombre || "Actividad"}
                      heightClassName="h-40"
                    />
                  </Link>

                  <div className="p-2 pt-4">
                    <div className="mb-2 flex flex-wrap gap-2">
                      {favorito.deporteNombre && (
                        <span className="rounded-full bg-[#E6F7EF] px-3 py-1 text-xs font-extrabold text-[#1D7B4A]">
                          {favorito.deporteNombre}
                        </span>
                      )}

                      {favorito.mostrarPrecio &&
                        favorito.precioReferencia !== undefined &&
                        favorito.precioReferencia !== null && (
                          <span className="rounded-full bg-white px-3 py-1 text-xs font-extrabold text-[var(--color-primary)] ring-1 ring-[#DDEAF3]">
                            Desde ${favorito.precioReferencia}
                          </span>
                        )}
                    </div>

                    <h3 className="line-clamp-2 text-lg font-extrabold text-[var(--color-primary)]">
                      {favorito.titulo}
                    </h3>

                    <p className="mt-1 line-clamp-1 text-sm font-bold text-[var(--color-muted)]">
                      {favorito.barrioNombre || "Zona a confirmar"}
                      {favorito.ciudadNombre ? `, ${favorito.ciudadNombre}` : ""}
                    </p>

                    <div className="mt-4 flex gap-2">
                      <AppLinkButton
                        href={`/actividades/${favorito.slug}`}
                        size="sm"
                        className="flex-1"
                      >
                        Ver detalle
                      </AppLinkButton>

                      <button
                        type="button"
                        onClick={() => manejarQuitar(favorito)}
                        aria-label={`Quitar ${favorito.titulo} de favoritos`}
                        className="inline-flex min-h-10 items-center justify-center rounded-[18px] border border-red-200 bg-red-50 px-4 text-xs font-extrabold text-red-700 shadow-sm transition duration-200 ease-out hover:border-red-300 hover:bg-white active:scale-[0.98]"
                      >
                        Quitar
                      </button>
                    </div>
                  </div>
                </SurfaceCard>
              );
            })}
          </div>

          {/*
            La nota al pie decía "solo en este navegador... más adelante
            vas a poder sincronizarlos": quedó vieja con el bloque de
            sync. Ahora dice lo que es cierto — y suma confianza en vez
            de restar.
          */}
          <p className="mt-6 flex items-start gap-2 text-xs leading-5 text-[var(--color-muted)]">
            <span aria-hidden="true" className="mt-px">
              <IconoInfo />
            </span>
            Tus guardados están sincronizados con tu cuenta: entrá desde
            cualquier dispositivo y vas a ver la misma lista.
          </p>
        </>
      )}
    </div>
  );
}

function IconoInfo() {
  return (
    <svg
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
      className="h-4 w-4 shrink-0 text-[var(--color-accent)]"
      aria-hidden="true"
    >
      <circle cx="12" cy="12" r="9" />
      <path d="M12 11v5M12 8h.01" />
    </svg>
  );
}
