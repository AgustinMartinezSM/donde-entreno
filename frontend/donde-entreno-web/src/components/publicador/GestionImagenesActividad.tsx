"use client";

import Image from "next/image";
import { useEffect, useRef, useState } from "react";
import type { ChangeEvent, FormEvent } from "react";

import { useAuthSession } from "../auth/AuthSessionProvider";
import { AppButton } from "../ui/AppButton";
import { SectionHeader } from "../ui/SectionHeader";
import { StatusMessage } from "../ui/StatusMessage";
import { SurfaceCard } from "../ui/SurfaceCard";
import { construirUrlImagenBackend } from "../../lib/backendUrl";
import {
  PublicadorApiError,
  eliminarImagenActividad,
  listarImagenesActividad,
  subirImagenActividad,
} from "../../services/publicadorService";
import type { ImagenActividadPublicador } from "../../types/publicador";

const TAMANIO_MAXIMO_BYTES = 2 * 1024 * 1024;
const TIPOS_ARCHIVO_PERMITIDOS = ["image/jpeg", "image/png", "image/webp"];

const ESTILOS_ESTADO: Record<string, string> = {
  PENDIENTE: "bg-[#FFF7E6] text-[#8A5A00] ring-1 ring-[#F5D48F]",
  APROBADA: "bg-[#E6F7EF] text-[#1D7B4A] ring-1 ring-[#BDE8D0]",
  RECHAZADA: "bg-red-50 text-red-700 ring-1 ring-red-200",
};

function formatearEstado(estado: string): string {
  return estado
    .toLowerCase()
    .split("_")
    .map((parte) => parte.charAt(0).toUpperCase() + parte.slice(1))
    .join(" ");
}

/*
  Gestión de imágenes de una actividad publicada del publicador:
  subida con preview local y validación (2MB, JPG/PNG/WebP), listado
  con estado de moderación y retiro de pendientes.

  Las imágenes suben PENDIENTE: recién se ven en la página pública
  cuando el equipo las aprueba.
*/
export function GestionImagenesActividad({ actividadId }: { actividadId: number }) {
  const { accessToken } = useAuthSession();

  const [imagenes, setImagenes] = useState<ImagenActividadPublicador[]>([]);
  const [cargando, setCargando] = useState(true);
  const [errorCarga, setErrorCarga] = useState<string | null>(null);

  const [archivo, setArchivo] = useState<File | null>(null);
  const [previewUrl, setPreviewUrl] = useState<string | null>(null);
  const [tipo, setTipo] = useState<"PRINCIPAL" | "GALERIA">("GALERIA");
  const [subiendo, setSubiendo] = useState(false);
  const [mensaje, setMensaje] = useState<string | null>(null);
  const [errorSubida, setErrorSubida] = useState<string | null>(null);
  const inputArchivoRef = useRef<HTMLInputElement | null>(null);

  useEffect(() => {
    let componenteActivo = true;

    if (!accessToken) {
      return () => {
        componenteActivo = false;
      };
    }

    listarImagenesActividad(actividadId, accessToken)
      .then((lista) => {
        if (componenteActivo) {
          setImagenes(lista);
          setErrorCarga(null);
        }
      })
      .catch((error: unknown) => {
        if (componenteActivo) {
          setErrorCarga(
            error instanceof PublicadorApiError
              ? error.message
              : "No pudimos cargar las imágenes de la actividad."
          );
        }
      })
      .finally(() => {
        if (componenteActivo) {
          setCargando(false);
        }
      });

    return () => {
      componenteActivo = false;
    };
  }, [accessToken, actividadId]);

  /*
    El object URL del preview se libera al reemplazarlo y al desmontar.
  */
  useEffect(() => {
    return () => {
      if (previewUrl) {
        URL.revokeObjectURL(previewUrl);
      }
    };
  }, [previewUrl]);

  function manejarSeleccionArchivo(evento: ChangeEvent<HTMLInputElement>) {
    const seleccionado = evento.target.files?.[0] ?? null;
    setMensaje(null);
    setErrorSubida(null);

    if (!seleccionado) {
      setArchivo(null);
      setPreviewUrl(null);
      return;
    }

    if (!TIPOS_ARCHIVO_PERMITIDOS.includes(seleccionado.type)) {
      setArchivo(null);
      setPreviewUrl(null);
      setErrorSubida("Formato no permitido: usá JPG, PNG o WebP.");
      return;
    }

    if (seleccionado.size > TAMANIO_MAXIMO_BYTES) {
      setArchivo(null);
      setPreviewUrl(null);
      setErrorSubida("La imagen supera el tamaño máximo de 2 MB.");
      return;
    }

    setArchivo(seleccionado);
    setPreviewUrl(URL.createObjectURL(seleccionado));
  }

  async function manejarEnvio(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault();

    if (subiendo || !accessToken || !archivo) {
      return;
    }

    setSubiendo(true);
    setMensaje(null);
    setErrorSubida(null);

    try {
      const imagenNueva = await subirImagenActividad(
        actividadId,
        archivo,
        tipo,
        accessToken
      );

      setImagenes((previas) => [imagenNueva, ...previas]);
      setMensaje(
        "Imagen subida. Queda pendiente de revisión: se va a ver en la página pública cuando el equipo la apruebe."
      );
      setArchivo(null);
      setPreviewUrl(null);

      if (inputArchivoRef.current) {
        inputArchivoRef.current.value = "";
      }
    } catch (error: unknown) {
      setErrorSubida(
        error instanceof PublicadorApiError
          ? error.message
          : "No pudimos subir la imagen. Probá nuevamente."
      );
    } finally {
      setSubiendo(false);
    }
  }

  async function manejarQuitar(imagen: ImagenActividadPublicador) {
    if (!accessToken) {
      return;
    }

    setMensaje(null);
    setErrorSubida(null);

    try {
      await eliminarImagenActividad(actividadId, imagen.id, accessToken);
      setImagenes((previas) => previas.filter((item) => item.id !== imagen.id));
      setMensaje("Imagen retirada.");
    } catch (error: unknown) {
      setErrorSubida(
        error instanceof PublicadorApiError
          ? error.message
          : "No pudimos retirar la imagen. Probá nuevamente."
      );
    }
  }

  return (
    <SurfaceCard className="p-6 sm:p-8">
      <SectionHeader
        eyebrow="Imágenes"
        title="Fotos de la actividad"
        description="Subí fotos reales: el equipo las revisa antes de publicarlas."
      />

      <form className="mt-6 grid gap-4 sm:grid-cols-[1fr_auto_auto]" onSubmit={manejarEnvio}>
        <div>
          <label
            htmlFor={`imagen-archivo-${actividadId}`}
            className="text-sm font-bold text-[var(--color-primary)]"
          >
            Archivo (JPG, PNG o WebP, hasta 2 MB)
          </label>
          <input
            ref={inputArchivoRef}
            id={`imagen-archivo-${actividadId}`}
            type="file"
            accept="image/jpeg,image/png,image/webp"
            onChange={manejarSeleccionArchivo}
            disabled={subiendo}
            className="mt-2 block w-full text-sm text-[var(--color-muted)] file:mr-3 file:cursor-pointer file:rounded-full file:border-0 file:bg-[#E8F6FB] file:px-4 file:py-2.5 file:text-sm file:font-bold file:text-[var(--color-primary)] hover:file:bg-[#DDEFF8]"
          />
        </div>

        <div>
          <label
            htmlFor={`imagen-tipo-${actividadId}`}
            className="text-sm font-bold text-[var(--color-primary)]"
          >
            Tipo
          </label>
          <select
            id={`imagen-tipo-${actividadId}`}
            value={tipo}
            onChange={(evento) =>
              setTipo(evento.target.value === "PRINCIPAL" ? "PRINCIPAL" : "GALERIA")
            }
            disabled={subiendo}
            className="mt-2 min-h-11 rounded-[18px] border border-[#BFDDEA] bg-[#F8FAFC] px-4 text-sm font-bold text-[var(--color-text)] outline-none transition duration-200 ease-out hover:border-[var(--color-accent)] focus:border-[var(--color-accent)] focus:ring-4 focus:ring-[#DDEAF3]"
          >
            <option value="GALERIA">Galería</option>
            <option value="PRINCIPAL">Principal</option>
          </select>
        </div>

        <div className="flex items-end">
          <AppButton type="submit" disabled={subiendo || !archivo}>
            {subiendo ? "Subiendo..." : "Subir imagen"}
          </AppButton>
        </div>
      </form>

      {previewUrl ? (
        <div className="mt-4 flex items-center gap-4 rounded-[18px] border border-[#DDEAF3] bg-[#F8FAFC] p-3">
          {/* Preview local del archivo elegido (object URL, no next/image). */}
          {/* eslint-disable-next-line @next/next/no-img-element */}
          <img
            src={previewUrl}
            alt="Vista previa de la imagen elegida"
            className="h-20 w-28 rounded-[12px] object-cover"
          />
          <p className="text-sm text-[var(--color-muted)]">
            Vista previa. Tocá <span className="font-bold">Subir imagen</span>{" "}
            para enviarla a revisión.
          </p>
        </div>
      ) : null}

      {mensaje ? (
        <StatusMessage variant="success" role="status" className="mt-4">
          {mensaje}
        </StatusMessage>
      ) : null}

      {errorSubida ? (
        <StatusMessage variant="error" role="alert" className="mt-4">
          {errorSubida}
        </StatusMessage>
      ) : null}

      {cargando ? (
        <StatusMessage variant="info" role="status" className="mt-6">
          Cargando imágenes...
        </StatusMessage>
      ) : null}

      {errorCarga ? (
        <StatusMessage variant="error" role="alert" className="mt-6">
          {errorCarga}
        </StatusMessage>
      ) : null}

      {!cargando && !errorCarga && imagenes.length === 0 ? (
        <p className="mt-6 text-sm text-[var(--color-muted)]">
          Todavía no subiste imágenes para esta actividad. Mientras tanto se
          muestra la ilustración del deporte.
        </p>
      ) : null}

      {imagenes.length > 0 ? (
        <ul className="mt-6 grid gap-3">
          {imagenes.map((imagen) => {
            const urlAbsoluta = construirUrlImagenBackend(imagen.url);

            return (
              <li
                key={imagen.id}
                className="flex flex-wrap items-center gap-4 rounded-[18px] border border-[#DDEAF3] bg-white/80 p-3"
              >
                {urlAbsoluta ? (
                  <Image
                    src={urlAbsoluta}
                    alt={`Imagen ${imagen.tipoImagen.toLowerCase()} de la actividad`}
                    width={112}
                    height={80}
                    className="h-20 w-28 rounded-[12px] object-cover"
                  />
                ) : null}

                <div className="min-w-0 flex-1">
                  <div className="flex flex-wrap items-center gap-2">
                    <span className="rounded-full bg-[#E8F6FB] px-3 py-1 text-xs font-bold text-[#0F6F8F]">
                      {imagen.tipoImagen === "PRINCIPAL" ? "Principal" : "Galería"}
                    </span>
                    <span
                      className={`rounded-full px-3 py-1 text-xs font-extrabold ${
                        ESTILOS_ESTADO[imagen.estadoModeracion] ??
                        "bg-[#F8FAFC] text-[var(--color-muted)] ring-1 ring-[#DDEAF3]"
                      }`}
                    >
                      {formatearEstado(imagen.estadoModeracion)}
                    </span>
                  </div>

                  {imagen.motivoRechazo ? (
                    <p className="mt-2 text-sm text-red-700">
                      <span className="font-bold">Motivo:</span>{" "}
                      {imagen.motivoRechazo}
                    </p>
                  ) : null}
                </div>

                {imagen.estadoModeracion === "PENDIENTE" ? (
                  <button
                    type="button"
                    onClick={() => manejarQuitar(imagen)}
                    aria-label="Retirar imagen pendiente"
                    className="inline-flex min-h-10 items-center justify-center rounded-[18px] border border-red-200 bg-red-50 px-4 text-xs font-extrabold text-red-700 shadow-sm transition duration-200 ease-out hover:border-red-300 hover:bg-white active:scale-[0.98]"
                  >
                    Retirar
                  </button>
                ) : null}
              </li>
            );
          })}
        </ul>
      ) : null}
    </SurfaceCard>
  );
}
