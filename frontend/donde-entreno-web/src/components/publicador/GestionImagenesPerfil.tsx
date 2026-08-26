"use client";

import Image from "next/image";
import { useEffect, useRef, useState } from "react";
import type { ChangeEvent } from "react";

import { useAuthSession } from "../auth/AuthSessionProvider";
import { EditorRecorteImagen } from "../imagenes/EditorRecorteImagen";
import { SectionHeader } from "../ui/SectionHeader";
import { StatusMessage } from "../ui/StatusMessage";
import { SurfaceCard } from "../ui/SurfaceCard";
import { construirUrlImagenBackend } from "../../lib/backendUrl";
import { MEDIDAS_DESTINO } from "../../lib/recorteImagen";
import {
  PublicadorApiError,
  eliminarImagenPerfil,
  listarImagenesPerfil,
  subirImagenPerfil,
} from "../../services/publicadorService";
import type { ImagenActividadPublicador } from "../../types/publicador";

const TAMANIO_MAXIMO_BYTES = 2 * 1024 * 1024;
const TIPOS_ARCHIVO_PERMITIDOS = ["image/jpeg", "image/png", "image/webp"];

type TipoPerfil = "LOGO" | "PORTADA";

const RANURAS = [
  {
    tipo: "LOGO" as const,
    titulo: "Logo",
    ayuda: "Se muestra en el círculo de tu perfil y junto a tu nombre.",
    /* Clase del marco de vista previa: cuadrado y redondo como se ve. */
    marco: "h-24 w-24 rounded-full",
  },
  {
    tipo: "PORTADA" as const,
    titulo: "Portada",
    ayuda: "Es la banda ancha del encabezado de tu perfil público.",
    marco: "h-24 w-full rounded-[12px]",
  },
];

type Seleccion = {
  tipo: TipoPerfil;
  archivo: File;
  url: string;
};

/*
  Logo y portada del perfil publicador.

  Del perfil hay uno solo de cada tipo, así que el flujo es más corto que
  el de las actividades: elegir el archivo abre el encuadre y confirmarlo
  ya sube. Como la imagen queda fija en la identidad del perfil, el
  encuadre no es opcional acá: vale la pena elegir bien qué se ve.

  Igual que las de actividad, desde la fase 4 social se publican al
  instante (moderación flexible: reportes + admin, no revisión previa).
*/
export function GestionImagenesPerfil() {
  const { accessToken } = useAuthSession();

  const [imagenes, setImagenes] = useState<ImagenActividadPublicador[]>([]);
  const [cargando, setCargando] = useState(true);
  const [errorCarga, setErrorCarga] = useState<string | null>(null);

  const [seleccion, setSeleccion] = useState<Seleccion | null>(null);
  const [subiendo, setSubiendo] = useState(false);
  const [mensaje, setMensaje] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const inputsRef = useRef<Record<string, HTMLInputElement | null>>({});

  useEffect(() => {
    let activo = true;

    if (!accessToken) {
      return () => {
        activo = false;
      };
    }

    listarImagenesPerfil(accessToken)
      .then((lista) => {
        if (activo) {
          setImagenes(lista);
          setErrorCarga(null);
        }
      })
      .catch((fallo: unknown) => {
        if (activo) {
          setErrorCarga(
            fallo instanceof PublicadorApiError
              ? fallo.message
              : "No pudimos cargar las imágenes de tu perfil."
          );
        }
      })
      .finally(() => {
        if (activo) {
          setCargando(false);
        }
      });

    return () => {
      activo = false;
    };
  }, [accessToken]);

  function limpiarSeleccion() {
    setSeleccion((previa) => {
      if (previa) {
        URL.revokeObjectURL(previa.url);
      }

      return null;
    });
  }

  function manejarSeleccion(tipo: TipoPerfil, evento: ChangeEvent<HTMLInputElement>) {
    const archivo = evento.target.files?.[0] ?? null;
    setMensaje(null);
    setError(null);

    /* El input se limpia siempre: si no, elegir el mismo archivo dos
       veces seguidas no dispara el change. */
    const input = inputsRef.current[tipo];

    if (input) {
      input.value = "";
    }

    if (!archivo) {
      return;
    }

    if (!TIPOS_ARCHIVO_PERMITIDOS.includes(archivo.type)) {
      setError("Formato no permitido: usá JPG, PNG o WebP.");
      return;
    }

    if (archivo.size > TAMANIO_MAXIMO_BYTES) {
      setError("La imagen supera el tamaño máximo de 2 MB.");
      return;
    }

    limpiarSeleccion();
    setSeleccion({ tipo, archivo, url: URL.createObjectURL(archivo) });
  }

  async function subirRecortada(recortada: File) {
    if (!seleccion || !accessToken || subiendo) {
      return;
    }

    const tipo = seleccion.tipo;
    setSubiendo(true);
    setError(null);

    try {
      const creada = await subirImagenPerfil(recortada, tipo, accessToken);
      setImagenes((previas) => [creada, ...previas]);
      setMensaje(
        `${tipo === "LOGO" ? "¡Logo publicado! Ya se ve" : "¡Portada publicada! Ya se ve"} en tu perfil público.`
      );
      limpiarSeleccion();
    } catch (fallo: unknown) {
      setError(
        fallo instanceof PublicadorApiError
          ? fallo.message
          : "No pudimos subir la imagen. Probá nuevamente."
      );
    } finally {
      setSubiendo(false);
    }
  }

  async function retirar(imagen: ImagenActividadPublicador) {
    if (!accessToken) {
      return;
    }

    const aprobada = imagen.estadoModeracion === "APROBADA";

    /* Eliminar una aprobada saca la identidad visible del perfil. */
    if (
      aprobada &&
      !window.confirm(
        "¿Eliminar esta imagen? Deja de verse en tu perfil público y no se puede deshacer."
      )
    ) {
      return;
    }

    setMensaje(null);
    setError(null);

    try {
      await eliminarImagenPerfil(imagen.id, accessToken);
      setImagenes((previas) => previas.filter((item) => item.id !== imagen.id));
      setMensaje(aprobada ? "Imagen eliminada." : "Imagen retirada.");
    } catch (fallo: unknown) {
      setError(
        fallo instanceof PublicadorApiError
          ? fallo.message
          : "No pudimos eliminar la imagen. Probá nuevamente."
      );
    }
  }

  /*
    La vigente de cada tipo es la más reciente que no fue rechazada NI
    eliminada (aprobada inactiva = eliminada o reemplazada; fase 2).
  */
  function vigenteDe(tipo: TipoPerfil) {
    return imagenes.find(
      (imagen) =>
        imagen.tipoImagen === tipo &&
        imagen.estadoModeracion !== "RECHAZADA" &&
        !(imagen.estadoModeracion === "APROBADA" && !imagen.activa)
    );
  }

  function ultimoRechazoDe(tipo: TipoPerfil) {
    return imagenes.find(
      (imagen) =>
        imagen.tipoImagen === tipo &&
        imagen.estadoModeracion === "RECHAZADA" &&
        Boolean(imagen.motivoRechazo)
    );
  }

  return (
    <SurfaceCard className="p-6 sm:p-8">
      <SectionHeader
        eyebrow="Identidad"
        title="Logo y portada"
        description="Así te ve la gente en tu perfil público. Las imágenes se publican al instante."
      />

      {cargando ? (
        <StatusMessage variant="info" role="status" className="mt-5">
          Cargando tus imágenes...
        </StatusMessage>
      ) : null}

      {errorCarga ? (
        <StatusMessage variant="error" role="alert" className="mt-5">
          {errorCarga}
        </StatusMessage>
      ) : null}

      {mensaje ? (
        <StatusMessage variant="success" role="status" className="mt-5">
          {mensaje}
        </StatusMessage>
      ) : null}

      {error ? (
        <StatusMessage variant="error" role="alert" className="mt-5">
          {error}
        </StatusMessage>
      ) : null}

      {!cargando && !errorCarga ? (
        <div className="mt-6 grid gap-5">
          {RANURAS.map((ranura) => {
            const vigente = vigenteDe(ranura.tipo);
            const rechazo = ultimoRechazoDe(ranura.tipo);
            const url = construirUrlImagenBackend(vigente?.url);
            const pendiente = vigente?.estadoModeracion === "PENDIENTE";
            const idInput = `imagen-perfil-${ranura.tipo.toLowerCase()}`;

            return (
              <div
                key={ranura.tipo}
                className="rounded-[18px] border border-[var(--color-border-soft)] bg-[var(--color-surface)]/80 p-4"
              >
                <div className="flex flex-wrap items-start justify-between gap-3">
                  <div className="min-w-0">
                    <p className="text-sm font-extrabold text-[var(--color-primary)]">
                      {ranura.titulo}
                    </p>
                    <p className="mt-1 text-xs leading-5 text-[var(--color-muted)]">
                      {ranura.ayuda}
                    </p>
                    <p className="mt-1 text-xs font-semibold text-[var(--color-muted)]">
                      {MEDIDAS_DESTINO[ranura.tipo].recomendacion}
                    </p>
                  </div>

                  {pendiente ? (
                    <span className="rounded-full bg-[var(--color-warning-surface)] px-3 py-1 text-xs font-extrabold text-[var(--color-warning)] ring-1 ring-[var(--color-warning-border)]">
                      En revisión
                    </span>
                  ) : vigente ? (
                    <span className="rounded-full bg-[var(--color-success-soft)] px-3 py-1 text-xs font-extrabold text-[var(--color-success)] ring-1 ring-[var(--color-success-border)]">
                      Publicada
                    </span>
                  ) : null}
                </div>

                <div className="mt-4 flex flex-wrap items-center gap-4">
                  <div
                    className={`relative shrink-0 overflow-hidden border border-[var(--color-border-soft)] bg-[var(--color-surface-soft)] ${ranura.marco} ${
                      ranura.tipo === "PORTADA" ? "max-w-sm" : ""
                    }`}
                  >
                    {url ? (
                      <Image
                        src={url}
                        alt={`${ranura.titulo} de tu perfil`}
                        fill
                        sizes="240px"
                        className="object-cover"
                      />
                    ) : (
                      <span className="flex h-full w-full items-center justify-center text-center text-xs font-bold text-[var(--color-muted)]">
                        Sin {ranura.titulo.toLowerCase()}
                      </span>
                    )}
                  </div>

                  <div className="flex flex-wrap items-center gap-2">
                    <input
                      ref={(nodo) => {
                        inputsRef.current[ranura.tipo] = nodo;
                      }}
                      id={idInput}
                      type="file"
                      accept="image/jpeg,image/png,image/webp"
                      onChange={(evento) => manejarSeleccion(ranura.tipo, evento)}
                      disabled={subiendo}
                      className="peer sr-only"
                    />
                    <label
                      htmlFor={idInput}
                      className="inline-flex min-h-11 cursor-pointer items-center justify-center rounded-[18px] border border-[var(--color-border-accent)] bg-[var(--color-surface)] px-5 py-3 text-sm font-extrabold text-[var(--color-primary)] shadow-sm transition duration-200 ease-out hover:-translate-y-0.5 hover:border-[var(--color-primary)] hover:bg-[var(--color-surface-soft)] peer-focus-visible:ring-4 peer-focus-visible:ring-[var(--color-accent)]/30 peer-disabled:cursor-not-allowed peer-disabled:opacity-50"
                    >
                      {vigente ? "Cambiar" : `Subir ${ranura.titulo.toLowerCase()}`}
                    </label>

                    {vigente &&
                    (pendiente || vigente.estadoModeracion === "APROBADA") ? (
                      <button
                        type="button"
                        onClick={() => retirar(vigente)}
                        disabled={subiendo}
                        className="inline-flex min-h-10 items-center justify-center rounded-[18px] border border-[var(--color-danger-border)] bg-[var(--color-danger-surface)] px-4 text-xs font-extrabold text-[var(--color-danger)] shadow-sm transition duration-200 ease-out hover:border-[var(--color-danger)] hover:bg-[var(--color-surface)] disabled:opacity-50"
                      >
                        {pendiente ? "Retirar" : "Eliminar"}
                      </button>
                    ) : null}
                  </div>
                </div>

                {rechazo?.motivoRechazo ? (
                  <p className="mt-3 text-sm leading-6 text-[var(--color-danger)]">
                    <span className="font-bold">
                      Rechazamos la última que subiste:
                    </span>{" "}
                    {rechazo.motivoRechazo}
                  </p>
                ) : null}

                {seleccion?.tipo === ranura.tipo ? (
                  <div className="mt-4">
                    <EditorRecorteImagen
                      key={seleccion.url}
                      archivo={seleccion.archivo}
                      url={seleccion.url}
                      tipo={ranura.tipo}
                      onConfirmar={subirRecortada}
                      onCancelar={limpiarSeleccion}
                    />
                  </div>
                ) : null}
              </div>
            );
          })}
        </div>
      ) : null}
    </SurfaceCard>
  );
}
