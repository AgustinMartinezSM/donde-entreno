"use client";

import Link from "next/link";
import { useEffect, useRef, useState, useSyncExternalStore } from "react";
import type { ReactNode } from "react";

import {
  quitarFavorito,
  useFavoritos,
  useScopeFavoritosResuelto,
  type FavoritoGuardado,
} from "../../lib/favoritos";
import {
  crearColeccionCuenta,
  eliminarColeccionCuenta,
  obtenerColeccionesCuenta,
  obtenerFavoritosOrganizados,
  organizarFavoritoCuenta,
  renombrarColeccionCuenta,
  type ColeccionGuardados,
} from "../../services/cuentaSyncService";
import { useAuthSession } from "../auth/AuthSessionProvider";
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
  const { accessToken } = useAuthSession();

  /*
    Colecciones y organización (bloque 13): estado del backend, cargado
    aparte del snapshot local. Si falla, la lista plana sigue intacta —
    las colecciones son una capa opcional encima, nunca un bloqueo.
  */
  const [colecciones, setColecciones] = useState<ColeccionGuardados[] | null>(
    null
  );
  const [organizacion, setOrganizacion] = useState<
    Map<string, { coleccionId: number | null; nota: string | null }>
  >(new Map());
  const [filtro, setFiltro] = useState<number | null>(null);
  const [organizando, setOrganizando] = useState<FavoritoGuardado | null>(null);

  useEffect(() => {
    if (!accessToken) {
      return;
    }

    let activo = true;

    Promise.all([
      obtenerColeccionesCuenta(accessToken),
      obtenerFavoritosOrganizados(accessToken),
    ])
      .then(([listaColecciones, organizados]) => {
        if (!activo) {
          return;
        }

        setColecciones(listaColecciones);
        setOrganizacion(
          new Map(
            organizados.map((organizado) => [
              organizado.actividad.slug,
              {
                coleccionId: organizado.coleccionId,
                nota: organizado.nota,
              },
            ])
          )
        );
      })
      .catch(() => {
        /* Backend viejo o sin red: la lista plana alcanza. */
      });

    return () => {
      activo = false;
    };
  }, [accessToken]);

  async function refrescarColecciones() {
    if (!accessToken) {
      return;
    }

    try {
      setColecciones(await obtenerColeccionesCuenta(accessToken));
    } catch {
      /* El conteo puede quedar viejo un rato: no es un error para la UI. */
    }
  }

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

  async function manejarCrearColeccion() {
    if (!accessToken) {
      return;
    }

    const nombre = window.prompt(
      "Nombre de la nueva colección (por ejemplo: Para probar, Cerca de casa):"
    );

    if (!nombre || !nombre.trim()) {
      return;
    }

    try {
      const creada = await crearColeccionCuenta(accessToken, nombre.trim());
      setColecciones((actuales) => [...(actuales ?? []), creada]);
      setFiltro(creada.id);
    } catch {
      window.alert(
        "No pudimos crear la colección. Puede que ya exista una con ese nombre."
      );
    }
  }

  async function manejarRenombrarColeccion(coleccion: ColeccionGuardados) {
    if (!accessToken) {
      return;
    }

    const nombre = window.prompt("Nuevo nombre de la colección:", coleccion.nombre);

    if (!nombre || !nombre.trim() || nombre.trim() === coleccion.nombre) {
      return;
    }

    try {
      await renombrarColeccionCuenta(accessToken, coleccion.id, nombre.trim());
      await refrescarColecciones();
    } catch {
      window.alert(
        "No pudimos renombrar la colección. Puede que ya exista una con ese nombre."
      );
    }
  }

  async function manejarEliminarColeccion(coleccion: ColeccionGuardados) {
    if (!accessToken) {
      return;
    }

    const confirmado = window.confirm(
      `¿Eliminar la colección "${coleccion.nombre}"?\n\nTus guardados NO se borran: vuelven a "Todos".`
    );

    if (!confirmado) {
      return;
    }

    try {
      await eliminarColeccionCuenta(accessToken, coleccion.id);
      setFiltro(null);
      setOrganizacion((actual) => {
        const nueva = new Map(actual);
        for (const [slug, datos] of nueva) {
          if (datos.coleccionId === coleccion.id) {
            nueva.set(slug, { ...datos, coleccionId: null });
          }
        }
        return nueva;
      });
      await refrescarColecciones();
    } catch {
      window.alert("No pudimos eliminar la colección. Probá de nuevo.");
    }
  }

  async function manejarOrganizar(
    favorito: FavoritoGuardado,
    coleccionId: number | null,
    nota: string
  ) {
    if (!accessToken) {
      return;
    }

    await organizarFavoritoCuenta(
      accessToken,
      favorito.slug,
      coleccionId,
      nota.trim() || null
    );
    setOrganizacion((actual) => {
      const nueva = new Map(actual);
      nueva.set(favorito.slug, {
        coleccionId,
        nota: nota.trim() || null,
      });
      return nueva;
    });
    await refrescarColecciones();
  }

  const hayColecciones = colecciones !== null;
  const visibles =
    filtro === null
      ? favoritos
      : favoritos.filter(
          (favorito) => organizacion.get(favorito.slug)?.coleccionId === filtro
        );
  const coleccionActiva =
    filtro !== null
      ? (colecciones ?? []).find((coleccion) => coleccion.id === filtro) ?? null
      : null;

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
              className="animate-pulse overflow-hidden rounded-[var(--radius-lg)] border border-[var(--color-border-soft)] bg-[var(--color-surface)] p-3"
            >
              <div className="h-40 rounded-[var(--radius-md)] bg-[var(--color-info-soft)]" />
              <div className="p-2 pt-4">
                <div className="h-4 w-24 rounded-full bg-[var(--color-info-soft)]" />
                <div className="mt-3 h-5 w-3/4 rounded-full bg-[var(--color-bg)]" />
                <div className="mt-2 h-4 w-1/2 rounded-full bg-[var(--color-bg)]" />
              </div>
            </div>
          ))}
        </div>
      ) : favoritos.length === 0 ? (
        <SurfaceCard className="mt-6 flex flex-col items-center gap-4 p-10 text-center">
          <span
            aria-hidden="true"
            className="inline-flex h-14 w-14 items-center justify-center rounded-full bg-[var(--color-info-soft)] text-[var(--color-primary)]"
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
          {/*
            Chips de colecciones (bloque 13): filtran la MISMA lista, no
            piden nada nuevo. Solo aparecen cuando el backend las trae —
            sin colecciones cargadas la vista es la de siempre.
          */}
          {hayColecciones ? (
            <div className="mt-6 flex min-w-0 items-center gap-2 overflow-x-auto pb-1">
              <ChipColeccion
                activa={filtro === null}
                onClick={() => setFiltro(null)}
              >
                Todos ({favoritos.length})
              </ChipColeccion>

              {(colecciones ?? []).map((coleccion) => (
                <ChipColeccion
                  key={coleccion.id}
                  activa={filtro === coleccion.id}
                  onClick={() => setFiltro(coleccion.id)}
                >
                  {coleccion.nombre} ({coleccion.cantidad})
                </ChipColeccion>
              ))}

              <button
                type="button"
                onClick={manejarCrearColeccion}
                className="shrink-0 rounded-full border border-dashed border-[var(--color-border-accent)] px-3.5 py-2 text-sm font-bold text-[var(--color-muted)] transition duration-200 ease-out hover:border-[var(--color-secondary)] hover:text-[var(--color-primary)] active:scale-[0.98]"
              >
                + Nueva colección
              </button>
            </div>
          ) : null}

          {coleccionActiva ? (
            <div className="mt-3 flex items-center gap-3 text-xs font-bold text-[var(--color-muted)]">
              <button
                type="button"
                onClick={() => manejarRenombrarColeccion(coleccionActiva)}
                className="underline underline-offset-4 transition hover:text-[var(--color-primary)]"
              >
                Renombrar
              </button>
              <button
                type="button"
                onClick={() => manejarEliminarColeccion(coleccionActiva)}
                className="underline underline-offset-4 transition hover:text-[var(--color-danger)]"
              >
                Eliminar colección
              </button>
            </div>
          ) : null}

          {visibles.length === 0 ? (
            <p className="mt-6 rounded-[18px] border border-[var(--color-border-soft)] bg-[var(--color-surface)] p-6 text-center text-sm font-semibold text-[var(--color-muted)]">
              Esta colección todavía no tiene guardados. Tocá
              &ldquo;Organizar&rdquo; en cualquier tarjeta para sumarle
              actividades.
            </p>
          ) : null}

          <div className="mt-6 grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
            {visibles.map((favorito) => {
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
                  className="group overflow-hidden p-3 transition duration-200 ease-out hover:-translate-y-1 hover:border-[var(--color-border-accent)] hover:shadow-[0_22px_55px_rgba(12,52,80,0.14)]"
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
                        <span className="rounded-full bg-[var(--color-success-soft)] px-3 py-1 text-xs font-extrabold text-[var(--color-success)]">
                          {favorito.deporteNombre}
                        </span>
                      )}

                      {favorito.mostrarPrecio &&
                        favorito.precioReferencia !== undefined &&
                        favorito.precioReferencia !== null && (
                          <span className="rounded-full bg-[var(--color-surface)] px-3 py-1 text-xs font-extrabold text-[var(--color-primary)] ring-1 ring-[var(--color-border-soft)]">
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

                    {organizacion.get(favorito.slug)?.nota ? (
                      <p className="mt-2 line-clamp-2 rounded-[12px] bg-[var(--color-bg)] px-3 py-2 text-xs font-semibold italic leading-5 text-[var(--color-muted)]">
                        &ldquo;{organizacion.get(favorito.slug)?.nota}&rdquo;
                      </p>
                    ) : null}

                    <div className="mt-4 flex gap-2">
                      <AppLinkButton
                        href={`/actividades/${favorito.slug}`}
                        size="sm"
                        className="flex-1"
                      >
                        Ver detalle
                      </AppLinkButton>

                      {hayColecciones ? (
                        <button
                          type="button"
                          onClick={() => setOrganizando(favorito)}
                          aria-label={`Organizar ${favorito.titulo}: colección y nota`}
                          aria-haspopup="dialog"
                          className="inline-flex min-h-10 items-center justify-center rounded-[18px] border border-[var(--color-border-accent)] bg-[var(--color-surface)] px-3 text-xs font-extrabold text-[var(--color-primary)] shadow-sm transition duration-200 ease-out hover:border-[var(--color-secondary)] active:scale-[0.98]"
                        >
                          Organizar
                        </button>
                      ) : null}

                      <button
                        type="button"
                        onClick={() => manejarQuitar(favorito)}
                        aria-label={`Quitar ${favorito.titulo} de favoritos`}
                        className="inline-flex min-h-10 items-center justify-center rounded-[18px] border border-[var(--color-danger-border)] bg-[var(--color-danger-surface)] px-4 text-xs font-extrabold text-[var(--color-danger)] shadow-sm transition duration-200 ease-out hover:border-[var(--color-danger)] hover:bg-[var(--color-surface)] active:scale-[0.98]"
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

      <DialogoOrganizarGuardado
        favorito={organizando}
        colecciones={colecciones ?? []}
        organizacionActual={
          organizando ? (organizacion.get(organizando.slug) ?? null) : null
        }
        onCerrar={() => setOrganizando(null)}
        onGuardar={manejarOrganizar}
      />
    </div>
  );
}

function ChipColeccion({
  activa,
  onClick,
  children,
}: {
  activa: boolean;
  onClick: () => void;
  children: ReactNode;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      aria-pressed={activa}
      className={`shrink-0 rounded-full border px-3.5 py-2 text-sm font-bold transition duration-200 ease-out active:scale-[0.98] ${
        activa
          ? "border-[var(--color-success-border)] bg-[var(--color-success-surface)] text-[var(--color-success)]"
          : "border-[var(--color-border-soft)] bg-[var(--color-surface)] text-[var(--color-muted)] hover:border-[var(--color-border-accent)] hover:text-[var(--color-primary)]"
      }`}
    >
      {children}
    </button>
  );
}

/*
  Diálogo de organización (bloque 13): colección + nota en un solo
  guardado — reemplazo total, la UI manda el estado deseado completo.
  <dialog> nativo, como todos los modales de la app.
*/
function DialogoOrganizarGuardado({
  favorito,
  colecciones,
  organizacionActual,
  onCerrar,
  onGuardar,
}: {
  favorito: FavoritoGuardado | null;
  colecciones: ColeccionGuardados[];
  organizacionActual: { coleccionId: number | null; nota: string | null } | null;
  onCerrar: () => void;
  onGuardar: (
    favorito: FavoritoGuardado,
    coleccionId: number | null,
    nota: string
  ) => Promise<void>;
}) {
  const dialogoRef = useRef<HTMLDialogElement | null>(null);
  const [coleccionId, setColeccionId] = useState<number | null>(null);
  const [nota, setNota] = useState("");
  const [guardando, setGuardando] = useState(false);
  const [error, setError] = useState(false);
  const abierto = favorito !== null;

  useEffect(() => {
    const dialogo = dialogoRef.current;

    if (!dialogo) {
      return;
    }

    if (abierto && !dialogo.open) {
      /* Al abrir, arranca del estado actual del guardado. */
      setColeccionId(organizacionActual?.coleccionId ?? null);
      setNota(organizacionActual?.nota ?? "");
      setError(false);
      dialogo.showModal();
    } else if (!abierto && dialogo.open) {
      dialogo.close();
    }
  }, [abierto, organizacionActual]);

  async function guardar() {
    if (!favorito || guardando) {
      return;
    }

    setGuardando(true);
    setError(false);

    try {
      await onGuardar(favorito, coleccionId, nota);
      onCerrar();
    } catch {
      setError(true);
    } finally {
      setGuardando(false);
    }
  }

  return (
    <dialog
      ref={dialogoRef}
      onClose={onCerrar}
      aria-labelledby="organizar-guardado-titulo"
      className="w-[min(26rem,calc(100vw-2rem))] rounded-[var(--radius-xl)] border border-[var(--color-border-soft)] bg-[var(--color-surface)] p-0 text-[var(--color-text)] shadow-[0_24px_60px_rgba(12,52,80,0.28)] backdrop:bg-[#0F3D5E]/40 backdrop:backdrop-blur-sm"
    >
      <div className="px-5 py-5">
        <h2
          id="organizar-guardado-titulo"
          className="text-lg font-extrabold text-[var(--color-primary)]"
        >
          Organizar guardado
        </h2>
        <p className="mt-1 line-clamp-1 text-sm font-bold text-[var(--color-muted)]">
          {favorito?.titulo}
        </p>

        <label
          htmlFor="organizar-coleccion"
          className="mt-4 block text-sm font-extrabold text-[var(--color-primary)]"
        >
          Colección
        </label>
        <select
          id="organizar-coleccion"
          value={coleccionId === null ? "" : String(coleccionId)}
          onChange={(evento) =>
            setColeccionId(
              evento.target.value === "" ? null : Number(evento.target.value)
            )
          }
          disabled={guardando}
          className="mt-2 min-h-12 w-full rounded-[18px] border border-[var(--color-border-accent)] bg-[var(--color-bg)] px-4 text-base text-[var(--color-text)] outline-none transition focus:border-[var(--color-accent)] focus:ring-4 focus:ring-[var(--color-border-soft)]"
        >
          <option value="">Todos (sin colección)</option>
          {colecciones.map((coleccion) => (
            <option key={coleccion.id} value={String(coleccion.id)}>
              {coleccion.nombre}
            </option>
          ))}
        </select>

        <label
          htmlFor="organizar-nota"
          className="mt-4 block text-sm font-extrabold text-[var(--color-primary)]"
        >
          Nota personal
        </label>
        <textarea
          id="organizar-nota"
          rows={3}
          maxLength={280}
          value={nota}
          onChange={(evento) => setNota(evento.target.value)}
          disabled={guardando}
          placeholder="Por ejemplo: preguntar por el horario de la mañana."
          className="mt-2 w-full rounded-[18px] border border-[var(--color-border-accent)] bg-[var(--color-bg)] px-4 py-3 text-base leading-6 text-[var(--color-text)] outline-none transition focus:border-[var(--color-accent)] focus:ring-4 focus:ring-[var(--color-border-soft)]"
        />
        <p className="mt-1 text-right text-xs font-semibold text-[var(--color-muted)]">
          {nota.length}/280
        </p>

        {error ? (
          <p role="alert" className="mt-2 text-sm font-bold text-[var(--color-danger)]">
            No pudimos guardar los cambios. Probá de nuevo.
          </p>
        ) : null}

        <div className="mt-5 grid grid-cols-2 gap-3">
          <button
            type="button"
            onClick={onCerrar}
            disabled={guardando}
            className="inline-flex min-h-12 items-center justify-center rounded-[18px] border border-[var(--color-border-accent)] bg-[var(--color-surface)] px-5 text-base font-extrabold text-[var(--color-primary)] transition duration-200 ease-out hover:border-[var(--color-primary)] active:scale-[0.98]"
          >
            Cancelar
          </button>
          <button
            type="button"
            onClick={guardar}
            disabled={guardando}
            className="gradient-cta gradient-cta-hover inline-flex min-h-12 items-center justify-center rounded-[18px] bg-[var(--color-brand)] px-5 text-base font-extrabold text-white shadow-[var(--shadow-button)] transition duration-200 ease-out active:scale-[0.98] disabled:opacity-70"
          >
            {guardando ? "Guardando..." : "Guardar"}
          </button>
        </div>
      </div>
    </dialog>
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
