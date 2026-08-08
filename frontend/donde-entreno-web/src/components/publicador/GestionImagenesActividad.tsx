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

/*
  Las dos ubicaciones posibles de una foto, con su explicación al lado:
  antes el selector decía solo "Principal" y "Galería" y no había forma
  de saber qué implicaba cada una.
*/
const OPCIONES_TIPO = [
  {
    valor: "PRINCIPAL",
    titulo: "Imagen principal",
    ayuda: "Se usa como portada de la actividad y en las tarjetas públicas.",
  },
  {
    valor: "GALERIA",
    titulo: "Galería",
    ayuda: "Imágenes adicionales para mostrar más detalles de la actividad.",
  },
] as const;

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

  const idArchivo = `imagen-archivo-${actividadId}`;

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

  const principales = imagenes.filter(
    (imagen) => imagen.tipoImagen === "PRINCIPAL"
  );
  const galeria = imagenes.filter((imagen) => imagen.tipoImagen !== "PRINCIPAL");
  const sinImagenes = !cargando && !errorCarga && imagenes.length === 0;

  return (
    <SurfaceCard className="p-6 sm:p-8">
      <SectionHeader
        eyebrow="Imágenes"
        title="Fotos de la actividad"
        description="Subí fotos reales de las clases o del espacio. Las imágenes se publican después de revisión."
      />

      <form className="mt-6 grid gap-5" onSubmit={manejarEnvio}>
        <fieldset className="min-w-0 border-0 p-0">
          <legend className="text-sm font-bold text-[var(--color-primary)]">
            ¿Dónde va esta foto?
          </legend>

          <div className="mt-3 grid gap-3 sm:grid-cols-2">
            {OPCIONES_TIPO.map((opcion) => {
              const seleccionada = tipo === opcion.valor;

              return (
                <label
                  key={opcion.valor}
                  className={`flex cursor-pointer flex-col rounded-[18px] border p-4 transition duration-200 ease-out has-[:focus-visible]:ring-4 has-[:focus-visible]:ring-[#4FB3D9]/30 ${
                    seleccionada
                      ? "border-[var(--color-primary)] bg-[#F1F8FC] shadow-sm"
                      : "border-[#DDEAF3] bg-white hover:border-[#BFDDEA] hover:bg-[#F8FAFC]"
                  }`}
                >
                  <span className="flex items-center gap-2">
                    <input
                      type="radio"
                      name={`imagen-tipo-${actividadId}`}
                      value={opcion.valor}
                      checked={seleccionada}
                      onChange={() => setTipo(opcion.valor)}
                      disabled={subiendo}
                      className="h-4 w-4 shrink-0 accent-[var(--color-primary)]"
                    />
                    <span className="text-sm font-extrabold text-[var(--color-primary)]">
                      {opcion.titulo}
                    </span>
                  </span>
                  <span className="mt-2 text-xs leading-5 text-[var(--color-muted)]">
                    {opcion.ayuda}
                  </span>
                </label>
              );
            })}
          </div>
        </fieldset>

        <div className="min-w-0">
          <p className="text-sm font-bold text-[var(--color-primary)]">Archivo</p>

          {/*
            El input nativo queda oculto y el label hace de botón: el
            control por defecto mezclaba un botón sin estilo con el nombre
            del archivo y se desarmaba en pantallas angostas. El input
            sigue siendo el que recibe foco, y el label muestra el anillo
            con peer-focus-visible.
          */}
          <div className="mt-2 flex flex-wrap items-center gap-3">
            <input
              ref={inputArchivoRef}
              id={idArchivo}
              type="file"
              accept="image/jpeg,image/png,image/webp"
              onChange={manejarSeleccionArchivo}
              disabled={subiendo}
              className="peer sr-only"
            />
            <label
              htmlFor={idArchivo}
              className="inline-flex min-h-11 cursor-pointer items-center justify-center rounded-[18px] border border-[#BFDDEA] bg-white px-5 py-3 text-sm font-extrabold text-[var(--color-primary)] shadow-sm transition duration-200 ease-out hover:-translate-y-0.5 hover:border-[var(--color-primary)] hover:bg-[#F8FCFE] peer-focus-visible:ring-4 peer-focus-visible:ring-[#4FB3D9]/30 peer-disabled:cursor-not-allowed peer-disabled:opacity-50"
            >
              {archivo ? "Cambiar imagen" : "Elegir imagen"}
            </label>
            <p className="min-w-0 flex-1 truncate text-sm text-[var(--color-muted)]">
              {archivo ? archivo.name : "JPG, PNG o WebP · hasta 2 MB"}
            </p>
          </div>
        </div>

        {previewUrl ? (
          <div className="flex flex-wrap items-center gap-4 rounded-[18px] border border-[#DDEAF3] bg-[#F8FAFC] p-3">
            {/* Preview local del archivo elegido (object URL, no next/image). */}
            {/* eslint-disable-next-line @next/next/no-img-element */}
            <img
              src={previewUrl}
              alt="Vista previa de la imagen elegida"
              className="h-20 w-28 shrink-0 rounded-[12px] object-cover"
            />
            <p className="min-w-0 flex-1 text-sm text-[var(--color-muted)]">
              Se va a subir como{" "}
              <span className="font-bold text-[var(--color-primary)]">
                {tipo === "PRINCIPAL" ? "imagen principal" : "galería"}
              </span>
              . Tocá <span className="font-bold">Subir imagen</span> para
              enviarla a revisión.
            </p>
          </div>
        ) : null}

        <div className="flex flex-col gap-3 sm:flex-row-reverse sm:items-center sm:justify-between">
          <AppButton
            type="submit"
            disabled={subiendo || !archivo}
            fullWidth
            className="sm:w-auto"
          >
            {subiendo ? "Subiendo..." : "Subir imagen"}
          </AppButton>
          <p className="text-xs leading-5 text-[var(--color-muted)]">
            Las imágenes se publican después de revisión.
          </p>
        </div>
      </form>

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

      {sinImagenes ? (
        <p className="mt-6 text-sm leading-6 text-[var(--color-muted)]">
          Todavía no subiste imágenes para esta actividad. Mientras tanto se
          muestra la ilustración del deporte.
        </p>
      ) : null}

      {principales.length > 0 ? (
        <GrupoImagenes
          titulo="Imagen principal"
          ayuda="Portada de la actividad y de sus tarjetas públicas."
          imagenes={principales}
          onQuitar={manejarQuitar}
        />
      ) : null}

      {galeria.length > 0 ? (
        <GrupoImagenes
          titulo="Galería"
          ayuda="Fotos adicionales de la actividad."
          imagenes={galeria}
          onQuitar={manejarQuitar}
        />
      ) : null}
    </SurfaceCard>
  );
}

type GrupoImagenesProps = {
  titulo: string;
  ayuda: string;
  imagenes: ImagenActividadPublicador[];
  onQuitar: (imagen: ImagenActividadPublicador) => void;
};

/*
  Listado de un tipo de imagen. Separar principal de galería evita que
  el publicador tenga que deducir el destino leyendo la etiqueta de
  cada fila.
*/
function GrupoImagenes({
  titulo,
  ayuda,
  imagenes,
  onQuitar,
}: GrupoImagenesProps) {
  return (
    <section className="mt-6">
      <h3 className="text-sm font-extrabold text-[var(--color-primary)]">
        {titulo}
      </h3>
      <p className="mt-1 text-xs leading-5 text-[var(--color-muted)]">{ayuda}</p>

      <ul className="mt-3 grid gap-3">
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
                  className="h-20 w-28 shrink-0 rounded-[12px] object-cover"
                />
              ) : (
                /* Sin url: imagen rechazada (el archivo ya no existe). */
                <span
                  aria-hidden="true"
                  className="flex h-20 w-28 shrink-0 items-center justify-center rounded-[12px] bg-[#F1F5F9] text-xs font-bold text-[var(--color-muted)]"
                >
                  Sin vista previa
                </span>
              )}

              <div className="min-w-0 flex-1">
                <span
                  className={`inline-flex rounded-full px-3 py-1 text-xs font-extrabold ${
                    ESTILOS_ESTADO[imagen.estadoModeracion] ??
                    "bg-[#F8FAFC] text-[var(--color-muted)] ring-1 ring-[#DDEAF3]"
                  }`}
                >
                  {formatearEstado(imagen.estadoModeracion)}
                </span>

                {imagen.motivoRechazo ? (
                  <p className="mt-2 text-sm leading-6 text-red-700">
                    <span className="font-bold">Motivo:</span>{" "}
                    {imagen.motivoRechazo}
                  </p>
                ) : null}
              </div>

              {imagen.estadoModeracion === "PENDIENTE" ? (
                <button
                  type="button"
                  onClick={() => onQuitar(imagen)}
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
    </section>
  );
}
