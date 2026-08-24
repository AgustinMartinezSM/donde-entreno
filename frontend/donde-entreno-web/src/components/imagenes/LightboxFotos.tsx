"use client";

import Image from "next/image";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useEffect, useRef, useState } from "react";

import { esRolPublicador } from "../../lib/authRedirects";
import {
  cargarFotosGuardadas,
  toggleFotoGuardada,
  useFotosGuardadas,
} from "../../lib/fotosGuardadas";
import {
  cargarLikesFotos,
  toggleLikeFoto,
  useLikesFotos,
} from "../../lib/likesFotos";
import {
  comentarFoto,
  eliminarComentarioPropio,
  GaleriaSocialApiError,
  obtenerComentarios,
  ocultarComentarioEnMiFoto,
  type ComentarioImagen,
} from "../../services/galeriaSocialService";
import { useAuthSession } from "../auth/AuthSessionProvider";
import { BotonReportar } from "../social/BotonReportar";

export type FotoLightbox = {
  clave: string;
  /* URL absoluta ya validada como publicable. */
  url: string;
  alt: string;
  /* Epígrafe visible (el título que cargó el publicador), si existe. */
  epigrafe?: string | null;
  /* Link contextual ("Ver actividad") para galerías de perfil. */
  href?: string;
  hrefTexto?: string;
  /*
    Para el corazón (bloque 14): id real de la imagen y su contador
    público. Sin estos dos datos el corazón no se dibuja — un backend
    anterior al bloque simplemente no lo muestra.
  */
  imagenId?: number;
  cantidadLikes?: number | null;
  /*
    Fase 4 (galería social): contador de comentarios y su toggle.
    Opcionales por la misma razón: un backend viejo no los manda.
  */
  cantidadComentarios?: number | null;
  comentariosActivados?: boolean | null;
};

type LightboxFotosProps = {
  fotos: FotoLightbox[];
  /* null = cerrado; un índice = abierto en esa foto. */
  indice: number | null;
  onCerrar: () => void;
  onNavegar: (indice: number) => void;
};

/*
  Visor de fotos a pantalla completa (fase 4 del bloque visual).

  Mismo patrón que el resto de los modales de la app: <dialog> nativo
  con showModal() — foco contenido, Escape, top layer sobre cualquier
  flotante — sin librerías. Navegación con flechas (botones y teclado)
  y swipe horizontal en pantallas táctiles.

  El visor es oscuro en los dos temas a propósito: es la convención de
  todo visor de medios (la foto manda, la interfaz desaparece), así que
  acá no hay tokens de tema.
*/
export function LightboxFotos({
  fotos,
  indice,
  onCerrar,
  onNavegar,
}: LightboxFotosProps) {
  const dialogoRef = useRef<HTMLDialogElement | null>(null);
  const inicioSwipeRef = useRef<number | null>(null);
  const abierto = indice !== null && fotos.length > 0;

  const router = useRouter();
  const pathname = usePathname();
  const { status, accessToken, usuario } = useAuthSession();
  const likesPropios = useLikesFotos();
  const guardadasPropias = useFotosGuardadas();
  /* Panel de comentarios (fase 4): abierto solo a pedido, por foto. */
  const [comentariosAbiertos, setComentariosAbiertos] = useState(false);
  /*
    Ajustes locales del contador de comentarios: mismo criterio que los
    likes — el DTO trae la base y esta vista suma o resta lo que pasa acá.
  */
  const [ajustesComentarios, setAjustesComentarios] = useState<
    Map<number, number>
  >(new Map());
  /*
    Ajustes locales del contador (bloque 14): el cantidadLikes del DTO ya
    incluye mi like previo, así que cada toggle de esta vista suma o
    resta 1 sobre esa base — exacto sin re-consultar.
  */
  const [ajustesLikes, setAjustesLikes] = useState<Map<number, number>>(
    new Map()
  );

  /* Los ids propios se cargan recién cuando el visor se abre con sesión. */
  useEffect(() => {
    if (abierto && status === "authenticated" && accessToken) {
      cargarLikesFotos(accessToken);
      cargarFotosGuardadas(accessToken);
    }
  }, [abierto, status, accessToken]);

  /*
    Al cambiar de foto o cerrar el visor, el panel de comentarios se
    cierra. Patrón "ajustar estado cuando cambia una prop" (durante el
    render, no en un efecto: la regla set-state-in-effect lo prohíbe).
  */
  const [ultimoIndice, setUltimoIndice] = useState<number | null>(indice);
  if (ultimoIndice !== indice) {
    setUltimoIndice(indice);
    if (comentariosAbiertos) {
      setComentariosAbiertos(false);
    }
  }

  function alternarLike(imagenId: number) {
    if (status !== "authenticated" || !accessToken) {
      router.push(`/login?returnTo=${encodeURIComponent(pathname)}`);
      return;
    }

    const quedoConLike = toggleLikeFoto(accessToken, imagenId);
    setAjustesLikes((actual) => {
      const nuevo = new Map(actual);
      nuevo.set(imagenId, (nuevo.get(imagenId) ?? 0) + (quedoConLike ? 1 : -1));
      return nuevo;
    });
  }

  function alternarGuardado(imagenId: number) {
    if (status !== "authenticated" || !accessToken) {
      router.push(`/login?returnTo=${encodeURIComponent(pathname)}`);
      return;
    }

    toggleFotoGuardada(accessToken, imagenId);
  }

  function ajustarContadorComentarios(imagenId: number, delta: number) {
    setAjustesComentarios((actual) => {
      const nuevo = new Map(actual);
      nuevo.set(imagenId, (nuevo.get(imagenId) ?? 0) + delta);
      return nuevo;
    });
  }

  useEffect(() => {
    const dialogo = dialogoRef.current;

    if (!dialogo) {
      return;
    }

    if (abierto && !dialogo.open) {
      dialogo.showModal();
    } else if (!abierto && dialogo.open) {
      dialogo.close();
    }
  }, [abierto]);

  /* El fondo queda inerte pero aún scrollea: se congela mientras está abierto. */
  useEffect(() => {
    if (!abierto) {
      return;
    }

    const overflowPrevio = document.body.style.overflow;
    document.body.style.overflow = "hidden";

    return () => {
      document.body.style.overflow = overflowPrevio;
    };
  }, [abierto]);

  if (fotos.length === 0) {
    return null;
  }

  const posicion = Math.min(indice ?? 0, fotos.length - 1);
  const foto = fotos[posicion];
  const hayAnterior = posicion > 0;
  const haySiguiente = posicion < fotos.length - 1;

  function navegar(delta: number) {
    const destino = posicion + delta;

    if (destino >= 0 && destino < fotos.length) {
      onNavegar(destino);
    }
  }

  return (
    <dialog
      ref={dialogoRef}
      onClose={onCerrar}
      /* Click fuera de la foto y sus controles = cerrar. */
      onClick={(evento) => {
        if (evento.target === dialogoRef.current) {
          onCerrar();
        }
      }}
      onKeyDown={(evento) => {
        if (evento.key === "ArrowLeft") {
          evento.preventDefault();
          navegar(-1);
        }

        if (evento.key === "ArrowRight") {
          evento.preventDefault();
          navegar(1);
        }
      }}
      onTouchStart={(evento) => {
        inicioSwipeRef.current = evento.touches[0]?.clientX ?? null;
      }}
      onTouchEnd={(evento) => {
        const inicio = inicioSwipeRef.current;
        inicioSwipeRef.current = null;

        if (inicio === null) {
          return;
        }

        const delta = (evento.changedTouches[0]?.clientX ?? inicio) - inicio;

        /* Umbral de 48px: un tap no navega, un arrastre sí. */
        if (Math.abs(delta) > 48) {
          navegar(delta < 0 ? 1 : -1);
        }
      }}
      aria-label={`Foto ${posicion + 1} de ${fotos.length} en pantalla completa`}
      className="fixed inset-0 m-0 h-[100dvh] max-h-none w-screen max-w-none bg-[#050D15]/95 p-0 backdrop:bg-[#050D15]/80"
    >
      <div className="relative flex h-full w-full flex-col">
        {/* Barra superior: contador y cierre, siempre visibles. */}
        <div className="relative z-10 flex items-center justify-between px-4 pt-4">
          <span className="rounded-full bg-white/10 px-3 py-1 text-xs font-extrabold text-white">
            {posicion + 1} / {fotos.length}
          </span>

          <button
            type="button"
            onClick={onCerrar}
            aria-label="Cerrar la vista de fotos"
            className="flex h-11 w-11 items-center justify-center rounded-full bg-white/10 text-white transition duration-200 ease-out hover:bg-white/20 focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-[#4FB3D9]/50 active:scale-95"
          >
            <svg
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth="2"
              strokeLinecap="round"
              strokeLinejoin="round"
              className="h-5 w-5"
              aria-hidden="true"
            >
              <path d="M18 6 6 18" />
              <path d="m6 6 12 12" />
            </svg>
          </button>
        </div>

        {/* La foto: ocupa todo el medio, entera y sin recortar. */}
        <div className="relative min-h-0 flex-1 px-2 py-3 sm:px-14">
          <Image
            key={foto.clave}
            src={foto.url}
            alt={foto.alt}
            fill
            sizes="100vw"
            className="object-contain"
          />
        </div>

        <FlechaLightbox
          direccion="anterior"
          oculta={!hayAnterior}
          onClick={() => navegar(-1)}
        />
        <FlechaLightbox
          direccion="siguiente"
          oculta={!haySiguiente}
          onClick={() => navegar(1)}
        />

        {/* Pie: corazón, comentarios, guardar, epígrafe y link contextual. */}
        {foto.imagenId !== undefined ? (
          <div className="relative z-10 flex flex-wrap items-center justify-center gap-3 px-4 pb-2">
            {foto.cantidadLikes !== undefined && foto.cantidadLikes !== null ? (
              <BotonMeGustaFoto
                cantidad={
                  foto.cantidadLikes + (ajustesLikes.get(foto.imagenId) ?? 0)
                }
                activo={likesPropios.has(foto.imagenId)}
                onClick={() => alternarLike(foto.imagenId as number)}
              />
            ) : null}

            {/* Comentarios (fase 4): solo si la foto los tiene activados. */}
            {foto.comentariosActivados !== false &&
            foto.cantidadComentarios !== undefined &&
            foto.cantidadComentarios !== null ? (
              <BotonComentariosFoto
                cantidad={
                  foto.cantidadComentarios +
                  (ajustesComentarios.get(foto.imagenId) ?? 0)
                }
                abierto={comentariosAbiertos}
                onClick={() => setComentariosAbiertos((valor) => !valor)}
              />
            ) : null}

            <BotonGuardarFoto
              activo={guardadasPropias.has(foto.imagenId)}
              onClick={() => alternarGuardado(foto.imagenId as number)}
            />

            {/* Reportar (Fase 2 social): compacto y en tinta clara del visor. */}
            <span className="text-white/70 [&_button]:text-white/70 [&_button:hover]:text-white">
              <BotonReportar
                tipoObjeto="IMAGEN"
                objetoId={foto.imagenId}
                etiquetaObjeto="esta foto"
                compacto
              />
            </span>
          </div>
        ) : null}

        {/* Panel de comentarios: se superpone al pie, la foto sigue visible. */}
        {comentariosAbiertos && foto.imagenId !== undefined ? (
          <PanelComentariosFoto
            key={foto.imagenId}
            imagenId={foto.imagenId}
            comentariosActivados={foto.comentariosActivados !== false}
            esPublicador={usuario ? esRolPublicador(usuario.rol) : false}
            status={status}
            accessToken={accessToken}
            onIrALogin={() =>
              router.push(`/login?returnTo=${encodeURIComponent(pathname)}`)
            }
            onCerrar={() => setComentariosAbiertos(false)}
            onAjusteContador={(delta) =>
              ajustarContadorComentarios(foto.imagenId as number, delta)
            }
          />
        ) : null}

        {foto.epigrafe || foto.href ? (
          <div className="relative z-10 flex flex-wrap items-center justify-center gap-x-4 gap-y-2 px-4 pb-5 text-center">
            {foto.epigrafe ? (
              <p className="max-w-2xl text-sm font-semibold leading-6 text-white/85">
                {foto.epigrafe}
              </p>
            ) : null}

            {foto.href ? (
              <Link
                href={foto.href}
                className="rounded-full bg-white/10 px-4 py-1.5 text-xs font-extrabold text-white underline-offset-4 transition hover:bg-white/20 hover:underline focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-[#4FB3D9]/50"
              >
                {foto.hrefTexto ?? "Ver actividad"}
              </Link>
            ) : null}
          </div>
        ) : (
          <div className="pb-5" aria-hidden="true" />
        )}
      </div>
    </dialog>
  );
}

/*
  Corazón del visor (bloque 14): contador público + like propio.
  Anónimo → login con returnTo (lo resuelve el caller del onClick).
*/
function BotonMeGustaFoto({
  cantidad,
  activo,
  onClick,
}: {
  cantidad: number;
  activo: boolean;
  onClick: () => void;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      aria-pressed={activo}
      aria-label={
        activo
          ? `Quitar tu me gusta (${cantidad} en total)`
          : `Dar me gusta a esta foto (${cantidad} en total)`
      }
      className={`flex min-h-10 items-center gap-2 rounded-full px-4 text-sm font-extrabold transition duration-200 ease-out active:scale-95 ${
        activo
          ? "bg-white/20 text-white"
          : "bg-white/10 text-white/85 hover:bg-white/20"
      }`}
    >
      <svg
        viewBox="0 0 24 24"
        fill={activo ? "currentColor" : "none"}
        stroke="currentColor"
        strokeWidth="2"
        strokeLinecap="round"
        strokeLinejoin="round"
        className={`h-5 w-5 ${activo ? "text-[#FF6B81]" : ""}`}
        aria-hidden="true"
      >
        <path d="M19 14c1.49-1.46 3-3.21 3-5.5A5.5 5.5 0 0 0 16.5 3c-1.76 0-3 .5-4.5 2-1.5-1.5-2.74-2-4.5-2A5.5 5.5 0 0 0 2 8.5c0 2.3 1.5 4.05 3 5.5l7 7Z" />
      </svg>
      {cantidad}
    </button>
  );
}

/* Globo de comentarios (fase 4): abre y cierra el panel. */
function BotonComentariosFoto({
  cantidad,
  abierto,
  onClick,
}: {
  cantidad: number;
  abierto: boolean;
  onClick: () => void;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      aria-expanded={abierto}
      aria-label={
        abierto
          ? `Cerrar los comentarios (${cantidad} en total)`
          : `Ver los comentarios de esta foto (${cantidad} en total)`
      }
      className={`flex min-h-10 items-center gap-2 rounded-full px-4 text-sm font-extrabold transition duration-200 ease-out active:scale-95 ${
        abierto
          ? "bg-white/20 text-white"
          : "bg-white/10 text-white/85 hover:bg-white/20"
      }`}
    >
      <svg
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        strokeWidth="2"
        strokeLinecap="round"
        strokeLinejoin="round"
        className="h-5 w-5"
        aria-hidden="true"
      >
        <path d="M7.9 20A9 9 0 1 0 4 16.1L2 22Z" />
      </svg>
      {cantidad}
    </button>
  );
}

/* Bookmark (fase 4): guarda la foto en la cuenta. Anónimo → login. */
function BotonGuardarFoto({
  activo,
  onClick,
}: {
  activo: boolean;
  onClick: () => void;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      aria-pressed={activo}
      aria-label={
        activo ? "Quitar esta foto de tus guardadas" : "Guardar esta foto"
      }
      className={`flex h-10 w-10 items-center justify-center rounded-full transition duration-200 ease-out active:scale-95 ${
        activo
          ? "bg-white/20 text-white"
          : "bg-white/10 text-white/85 hover:bg-white/20"
      }`}
    >
      <svg
        viewBox="0 0 24 24"
        fill={activo ? "currentColor" : "none"}
        stroke="currentColor"
        strokeWidth="2"
        strokeLinecap="round"
        strokeLinejoin="round"
        className={`h-5 w-5 ${activo ? "text-[#F0B429]" : ""}`}
        aria-hidden="true"
      >
        <path d="m19 21-7-4-7 4V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2v16z" />
      </svg>
    </button>
  );
}

/*
  Panel de comentarios (fase 4): sheet oscuro anclado abajo del visor.
  Carga la lista al abrirse (key por imagenId lo reinicia al navegar).
  Publicar y borrar avisan al contador del pie vía onAjusteContador.
*/
function PanelComentariosFoto({
  imagenId,
  comentariosActivados,
  esPublicador,
  status,
  accessToken,
  onIrALogin,
  onCerrar,
  onAjusteContador,
}: {
  imagenId: number;
  comentariosActivados: boolean;
  esPublicador: boolean;
  status: string;
  accessToken: string | null;
  onIrALogin: () => void;
  onCerrar: () => void;
  onAjusteContador: (delta: number) => void;
}) {
  const [comentarios, setComentarios] = useState<ComentarioImagen[] | null>(
    null
  );
  const [errorCarga, setErrorCarga] = useState(false);
  const [texto, setTexto] = useState("");
  const [enviando, setEnviando] = useState(false);
  const [errorEnvio, setErrorEnvio] = useState<string | null>(null);

  useEffect(() => {
    let vigente = true;

    obtenerComentarios(imagenId, accessToken)
      .then((lista) => {
        if (vigente) {
          setComentarios(lista);
        }
      })
      .catch(() => {
        if (vigente) {
          setErrorCarga(true);
        }
      });

    return () => {
      vigente = false;
    };
  }, [imagenId, accessToken]);

  async function manejarEnviar() {
    if (status !== "authenticated" || !accessToken) {
      onIrALogin();
      return;
    }

    const limpio = texto.trim();

    if (!limpio || enviando) {
      return;
    }

    setEnviando(true);
    setErrorEnvio(null);

    try {
      const nuevo = await comentarFoto(accessToken, imagenId, limpio);
      setComentarios((actuales) => [...(actuales ?? []), nuevo]);
      setTexto("");
      onAjusteContador(1);
    } catch (error: unknown) {
      setErrorEnvio(
        error instanceof GaleriaSocialApiError
          ? error.message
          : "No pudimos publicar el comentario. Probá nuevamente."
      );
    } finally {
      setEnviando(false);
    }
  }

  async function manejarEliminar(comentario: ComentarioImagen) {
    if (!accessToken) {
      return;
    }

    try {
      await eliminarComentarioPropio(accessToken, comentario.id);
      setComentarios((actuales) =>
        (actuales ?? []).filter((item) => item.id !== comentario.id)
      );
      onAjusteContador(-1);
    } catch {
      /* Sin drama: el comentario queda y se puede reintentar. */
    }
  }

  async function manejarOcultar(comentario: ComentarioImagen) {
    if (!accessToken) {
      return;
    }

    try {
      await ocultarComentarioEnMiFoto(accessToken, comentario.id);
      setComentarios((actuales) =>
        (actuales ?? []).filter((item) => item.id !== comentario.id)
      );
      onAjusteContador(-1);
    } catch {
      /* El backend valida que la foto sea del publicador: si no, no pasa nada. */
    }
  }

  return (
    <div className="absolute inset-x-0 bottom-0 z-20 flex max-h-[60%] flex-col rounded-t-[20px] bg-[#0B1826] px-4 pb-4 pt-3 shadow-[0_-8px_30px_rgba(0,0,0,0.5)]">
      <div className="flex items-center justify-between pb-2">
        <h3 className="text-sm font-extrabold text-white">Comentarios</h3>
        <button
          type="button"
          onClick={onCerrar}
          aria-label="Cerrar los comentarios"
          className="flex h-9 w-9 items-center justify-center rounded-full bg-white/10 text-white transition hover:bg-white/20 active:scale-95"
        >
          <svg
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="2"
            strokeLinecap="round"
            strokeLinejoin="round"
            className="h-4 w-4"
            aria-hidden="true"
          >
            <path d="M18 6 6 18" />
            <path d="m6 6 12 12" />
          </svg>
        </button>
      </div>

      <div className="min-h-0 flex-1 overflow-y-auto">
        {errorCarga ? (
          <p className="py-4 text-center text-sm text-white/70">
            No pudimos cargar los comentarios. Probá nuevamente.
          </p>
        ) : comentarios === null ? (
          <p className="py-4 text-center text-sm text-white/70">Cargando...</p>
        ) : comentarios.length === 0 ? (
          <p className="py-4 text-center text-sm text-white/70">
            Todavía no hay comentarios. ¡Dejá el primero!
          </p>
        ) : (
          <ul className="grid gap-3 py-1">
            {comentarios.map((comentario) => (
              <li key={comentario.id} className="text-sm leading-6">
                <div className="flex flex-wrap items-baseline gap-x-2">
                  <span className="font-extrabold text-white">
                    {comentario.autorNombre}
                  </span>
                  <span className="text-white/85">{comentario.texto}</span>
                </div>
                <div className="mt-0.5 flex items-center gap-3 text-xs">
                  {comentario.esPropio ? (
                    <button
                      type="button"
                      onClick={() => manejarEliminar(comentario)}
                      className="font-bold text-white/60 underline-offset-2 transition hover:text-white hover:underline"
                    >
                      Eliminar
                    </button>
                  ) : (
                    <>
                      {esPublicador ? (
                        <button
                          type="button"
                          onClick={() => manejarOcultar(comentario)}
                          className="font-bold text-white/60 underline-offset-2 transition hover:text-white hover:underline"
                        >
                          Ocultar
                        </button>
                      ) : null}
                      <span className="text-white/60 [&_button]:text-white/60 [&_button:hover]:text-white">
                        <BotonReportar
                          tipoObjeto="COMENTARIO"
                          objetoId={comentario.id}
                          etiquetaObjeto="este comentario"
                          compacto
                        />
                      </span>
                    </>
                  )}
                </div>
              </li>
            ))}
          </ul>
        )}
      </div>

      {comentariosActivados ? (
        <form
          className="mt-2 flex items-center gap-2"
          onSubmit={(evento) => {
            evento.preventDefault();
            void manejarEnviar();
          }}
        >
          <input
            type="text"
            value={texto}
            maxLength={500}
            onChange={(evento) => setTexto(evento.target.value)}
            disabled={enviando}
            placeholder={
              status === "authenticated"
                ? "Escribí un comentario..."
                : "Iniciá sesión para comentar"
            }
            onFocus={() => {
              if (status !== "authenticated") {
                onIrALogin();
              }
            }}
            className="min-h-10 w-full rounded-full bg-white/10 px-4 text-sm text-white placeholder:text-white/50 outline-none transition focus:bg-white/15 disabled:opacity-60"
          />
          <button
            type="submit"
            disabled={enviando || !texto.trim()}
            className="rounded-full bg-white/15 px-4 py-2 text-sm font-extrabold text-white transition hover:bg-white/25 disabled:opacity-40"
          >
            {enviando ? "..." : "Publicar"}
          </button>
        </form>
      ) : (
        <p className="mt-2 text-center text-xs text-white/60">
          El publicador desactivó los comentarios en esta foto.
        </p>
      )}

      {errorEnvio ? (
        <p role="alert" className="mt-2 text-center text-xs text-[#FF8FA0]">
          {errorEnvio}
        </p>
      ) : null}
    </div>
  );
}

function FlechaLightbox({
  direccion,
  oculta,
  onClick,
}: {
  direccion: "anterior" | "siguiente";
  oculta: boolean;
  onClick: () => void;
}) {
  const esAnterior = direccion === "anterior";

  return (
    <button
      type="button"
      onClick={onClick}
      tabIndex={oculta ? -1 : 0}
      aria-hidden={oculta}
      aria-label={esAnterior ? "Foto anterior" : "Foto siguiente"}
      className={`absolute top-1/2 z-10 flex h-12 w-12 -translate-y-1/2 items-center justify-center rounded-full bg-white/10 text-white transition duration-200 ease-out hover:bg-white/20 focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-[#4FB3D9]/50 active:scale-95 ${
        esAnterior ? "left-3" : "right-3"
      } ${oculta ? "pointer-events-none opacity-0" : ""}`}
    >
      <svg
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        strokeWidth="2.5"
        strokeLinecap="round"
        strokeLinejoin="round"
        className="h-6 w-6"
        aria-hidden="true"
      >
        <path d={esAnterior ? "m15 5-7 7 7 7" : "m9 5 7 7-7 7"} />
      </svg>
    </button>
  );
}
