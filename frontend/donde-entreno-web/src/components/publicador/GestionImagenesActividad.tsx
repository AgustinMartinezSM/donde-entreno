"use client";

import Image from "next/image";
import { useCallback, useEffect, useRef, useState } from "react";
import type { ChangeEvent, FormEvent } from "react";

import { useAuthSession } from "../auth/AuthSessionProvider";
import { AppButton } from "../ui/AppButton";
import { SectionHeader } from "../ui/SectionHeader";
import { StatusMessage } from "../ui/StatusMessage";
import { SurfaceCard } from "../ui/SurfaceCard";
import { construirUrlImagenBackend } from "../../lib/backendUrl";
import { ENCUADRE_INICIAL, recortarImagen } from "../../lib/recorteImagen";
import { EditorRecorteImagen } from "../imagenes/EditorRecorteImagen";
import {
  PublicadorApiError,
  actualizarTextoImagen,
  elegirImagenPrincipal,
  eliminarImagenActividad,
  listarImagenesActividad,
  ordenarImagenesActividad,
  subirImagenActividad,
} from "../../services/publicadorService";
import type { ImagenActividadPublicador } from "../../types/publicador";

const TAMANIO_MAXIMO_BYTES = 2 * 1024 * 1024;
const TIPOS_ARCHIVO_PERMITIDOS = ["image/jpeg", "image/png", "image/webp"];

const ESTILOS_ESTADO: Record<string, string> = {
  PENDIENTE: "bg-[#FFF7E6] text-[#8A5A00] ring-1 ring-[#F5D48F]",
  APROBADA: "bg-[var(--color-success-soft)] text-[var(--color-success)] ring-1 ring-[var(--color-success-border)]",
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

type ArchivoElegido = {
  archivo: File;
  /* Object URL del preview, se libera al quitar o al desmontar. */
  url: string;
  /*
    Si el publicador ya eligió el encuadre, el archivo que se guarda acá
    es el recortado. Las que no se ajustan se recortan igual al subir,
    centradas, para que todas entren con la misma proporción.
  */
  ajustado: boolean;
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

  /*
    Selección múltiple: cada archivo viaja en su propio request porque el
    endpoint recibe uno por vez. Guardamos el object URL junto al archivo
    para poder liberarlo cuando se quita de la selección.
  */
  const [seleccion, setSeleccion] = useState<ArchivoElegido[]>([]);
  const [tipo, setTipo] = useState<"PRINCIPAL" | "GALERIA">("GALERIA");
  const [subiendo, setSubiendo] = useState(false);
  const [progreso, setProgreso] = useState<{ hecho: number; total: number } | null>(
    null
  );
  const [mensaje, setMensaje] = useState<string | null>(null);
  const [errorSubida, setErrorSubida] = useState<string | null>(null);
  /* URL del preview que está abierto en el editor de encuadre. */
  const [editando, setEditando] = useState<string | null>(null);
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
    Ref espejo de la selección: el cleanup del efecto de desmontaje corre
    una sola vez y necesita ver la lista final, no la del primer render.
  */
  const seleccionRef = useRef<ArchivoElegido[]>([]);

  useEffect(() => {
    seleccionRef.current = seleccion;
  }, [seleccion]);

  /*
    Al desmontar liberamos los object URL que sigan vivos. La selección
    se limpia con limpiarSeleccion, que también los libera.
  */
  useEffect(() => {
    return () => {
      seleccionRef.current.forEach((elegido) => URL.revokeObjectURL(elegido.url));
    };
  }, []);

  function limpiarSeleccion() {
    seleccion.forEach((elegido) => URL.revokeObjectURL(elegido.url));
    setSeleccion([]);

    if (inputArchivoRef.current) {
      inputArchivoRef.current.value = "";
    }
  }

  function manejarSeleccionArchivos(evento: ChangeEvent<HTMLInputElement>) {
    const elegidos = Array.from(evento.target.files ?? []);
    setMensaje(null);
    setErrorSubida(null);

    if (elegidos.length === 0) {
      return;
    }

    const aceptados: ArchivoElegido[] = [];
    const rechazados: string[] = [];

    for (const archivo of elegidos) {
      if (!TIPOS_ARCHIVO_PERMITIDOS.includes(archivo.type)) {
        rechazados.push(`${archivo.name} (formato no permitido)`);
        continue;
      }

      if (archivo.size > TAMANIO_MAXIMO_BYTES) {
        rechazados.push(`${archivo.name} (supera 2 MB)`);
        continue;
      }

      aceptados.push({
        archivo,
        url: URL.createObjectURL(archivo),
        ajustado: false,
      });
    }

    /*
      La portada es una sola: si el tipo es PRINCIPAL nos quedamos con el
      primer archivo válido y avisamos, en vez de subir varias que se
      pisarían entre sí al aprobarse.
    */
    const recorte = tipo === "PRINCIPAL" ? aceptados.slice(0, 1) : aceptados;

    aceptados.slice(recorte.length).forEach((sobra) => URL.revokeObjectURL(sobra.url));

    seleccion.forEach((previo) => URL.revokeObjectURL(previo.url));
    setSeleccion(recorte);

    const avisos: string[] = [];

    if (rechazados.length > 0) {
      avisos.push(`No se agregaron: ${rechazados.join(", ")}.`);
    }

    if (tipo === "PRINCIPAL" && aceptados.length > 1) {
      avisos.push(
        "La imagen principal es una sola: dejamos la primera. Para subir varias, elegí Galería."
      );
    }

    setErrorSubida(avisos.length > 0 ? avisos.join(" ") : null);
  }

  function quitarDeLaSeleccion(url: string) {
    URL.revokeObjectURL(url);
    setSeleccion((previas) => previas.filter((elegido) => elegido.url !== url));
    setEditando((actual) => (actual === url ? null : actual));
    setMensaje(null);
    setErrorSubida(null);
  }

  /*
    Reemplaza el archivo por su versión recortada y refresca el preview,
    para que la miniatura muestre el encuadre elegido y no el original.
  */
  function aplicarRecorte(url: string, recortada: File) {
    setSeleccion((previas) =>
      previas.map((elegido) => {
        if (elegido.url !== url) {
          return elegido;
        }

        URL.revokeObjectURL(elegido.url);

        return {
          archivo: recortada,
          url: URL.createObjectURL(recortada),
          ajustado: true,
        };
      })
    );
    setEditando(null);
  }

  function cambiarTipo(nuevoTipo: "PRINCIPAL" | "GALERIA") {
    setTipo(nuevoTipo);
    setMensaje(null);
    setErrorSubida(null);

    /* Pasar a PRINCIPAL con varias elegidas deja solo la primera. */
    if (nuevoTipo === "PRINCIPAL" && seleccion.length > 1) {
      seleccion.slice(1).forEach((sobra) => URL.revokeObjectURL(sobra.url));
      setSeleccion((previas) => previas.slice(0, 1));
      setErrorSubida(
        "La imagen principal es una sola: dejamos la primera de las que elegiste."
      );
    }
  }

  async function manejarEnvio(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault();

    if (subiendo || !accessToken || seleccion.length === 0) {
      return;
    }

    setSubiendo(true);
    setMensaje(null);
    setErrorSubida(null);
    setProgreso({ hecho: 0, total: seleccion.length });

    const subidas: ImagenActividadPublicador[] = [];
    const fallidas: string[] = [];

    /*
      Secuencial a propósito: son requests multipart y el orden en que
      quedan cargadas es el que ve el publicador en su listado.
    */
    for (const [indice, elegido] of seleccion.entries()) {
      try {
        /*
          Las que no pasaron por el editor se recortan centradas: así
          todas las imágenes del mismo tipo entran con la proporción de
          destino y el feed deja de mezclar apaisadas con verticales.
        */
        const archivoFinal = elegido.ajustado
          ? elegido.archivo
          : await recortarImagen(elegido.archivo, tipo, ENCUADRE_INICIAL);

        const imagenNueva = await subirImagenActividad(
          actividadId,
          archivoFinal,
          tipo,
          accessToken
        );
        subidas.push(imagenNueva);
      } catch (error: unknown) {
        fallidas.push(
          `${elegido.archivo.name}: ${
            error instanceof PublicadorApiError
              ? error.message
              : "no pudimos subirla"
          }`
        );
      } finally {
        setProgreso({ hecho: indice + 1, total: seleccion.length });
      }
    }

    if (subidas.length > 0) {
      setImagenes((previas) => [...subidas.reverse(), ...previas]);
      setMensaje(
        subidas.length === 1
          ? "Imagen subida. Queda pendiente de revisión: se va a ver en la página pública cuando el equipo la apruebe."
          : `${subidas.length} imágenes subidas. Quedan pendientes de revisión: se van a ver en la página pública cuando el equipo las apruebe.`
      );
    }

    setErrorSubida(
      fallidas.length > 0
        ? `No pudimos subir ${fallidas.length === 1 ? "una imagen" : `${fallidas.length} imágenes`}. ${fallidas.join(" · ")}`
        : null
    );

    limpiarSeleccion();
    setProgreso(null);
    setSubiendo(false);
  }

  /*
    Refetch tras las acciones de fase 2 (ordenar, hacer principal): el
    backend recalcula orden y tipos, así que releer es más simple y más
    fiel que reconstruir el estado a mano.
  */
  const recargarImagenes = useCallback(async () => {
    if (!accessToken) {
      return;
    }

    try {
      setImagenes(await listarImagenesActividad(actividadId, accessToken));
    } catch {
      /* La próxima acción o recarga de página lo reintenta. */
    }
  }, [accessToken, actividadId]);

  async function manejarQuitar(imagen: ImagenActividadPublicador) {
    if (!accessToken) {
      return;
    }

    const aprobada = imagen.estadoModeracion === "APROBADA";

    /*
      Retirar una pendiente es reversible (se vuelve a subir en un
      minuto); eliminar una aprobada saca una foto que ya estaba en la
      página pública — eso sí merece confirmación.
    */
    if (
      aprobada &&
      !window.confirm(
        "¿Eliminar esta foto? Deja de verse en la página pública y no se puede deshacer."
      )
    ) {
      return;
    }

    setMensaje(null);
    setErrorSubida(null);

    try {
      await eliminarImagenActividad(actividadId, imagen.id, accessToken);
      setImagenes((previas) => previas.filter((item) => item.id !== imagen.id));
      setMensaje(aprobada ? "Foto eliminada." : "Imagen retirada.");
    } catch (error: unknown) {
      setErrorSubida(
        error instanceof PublicadorApiError
          ? error.message
          : "No pudimos eliminar la imagen. Probá nuevamente."
      );
    }
  }

  /* Mueve una foto un lugar dentro de la galería aprobada. */
  async function manejarMover(
    imagen: ImagenActividadPublicador,
    direccion: -1 | 1
  ) {
    if (!accessToken) {
      return;
    }

    const ids = galeriaAprobada.map((item) => item.id);
    const desde = ids.indexOf(imagen.id);
    const hasta = desde + direccion;

    if (desde < 0 || hasta < 0 || hasta >= ids.length) {
      return;
    }

    [ids[desde], ids[hasta]] = [ids[hasta], ids[desde]];

    setMensaje(null);
    setErrorSubida(null);

    try {
      await ordenarImagenesActividad(actividadId, ids, accessToken);
      await recargarImagenes();
    } catch (error: unknown) {
      setErrorSubida(
        error instanceof PublicadorApiError
          ? error.message
          : "No pudimos reordenar la galería. Probá nuevamente."
      );
    }
  }

  async function manejarHacerPrincipal(imagen: ImagenActividadPublicador) {
    if (!accessToken) {
      return;
    }

    setMensaje(null);
    setErrorSubida(null);

    try {
      await elegirImagenPrincipal(actividadId, imagen.id, accessToken);
      await recargarImagenes();
      setMensaje(
        "Listo: esa foto ahora es la principal. La anterior pasó a la galería."
      );
    } catch (error: unknown) {
      setErrorSubida(
        error instanceof PublicadorApiError
          ? error.message
          : "No pudimos cambiar la imagen principal. Probá nuevamente."
      );
    }
  }

  async function manejarGuardarTexto(
    imagen: ImagenActividadPublicador,
    titulo: string,
    descripcion: string
  ) {
    if (!accessToken) {
      return;
    }

    setMensaje(null);
    setErrorSubida(null);

    try {
      const actualizada = await actualizarTextoImagen(
        actividadId,
        imagen.id,
        /* Siempre los dos: el string vacío limpia (semántica PATCH). */
        { titulo, descripcion },
        accessToken
      );
      setImagenes((previas) =>
        previas.map((item) => (item.id === actualizada.id ? actualizada : item))
      );
      setMensaje("Texto de la foto guardado.");
    } catch (error: unknown) {
      setErrorSubida(
        error instanceof PublicadorApiError
          ? error.message
          : "No pudimos guardar el texto. Probá nuevamente."
      );
      throw error;
    }
  }

  const editandoElegido = seleccion.find((elegido) => elegido.url === editando);
  /*
    Las aprobadas inactivas son historia (reemplazadas o eliminadas): no
    se listan. Antes una principal reemplazada seguía apareciendo como
    "Aprobada" y confundía.
  */
  const visibles = imagenes.filter(
    (imagen) => !(imagen.estadoModeracion === "APROBADA" && !imagen.activa)
  );
  const principales = visibles.filter(
    (imagen) => imagen.tipoImagen === "PRINCIPAL"
  );
  const galeriaAprobada = visibles
    .filter(
      (imagen) =>
        imagen.tipoImagen === "GALERIA" && imagen.estadoModeracion === "APROBADA"
    )
    .sort((a, b) => (a.orden ?? 0) - (b.orden ?? 0));
  const galeriaEnRevision = visibles.filter(
    (imagen) =>
      imagen.tipoImagen !== "PRINCIPAL" && imagen.estadoModeracion !== "APROBADA"
  );
  const galeria = [...galeriaAprobada, ...galeriaEnRevision];
  const sinImagenes = !cargando && !errorCarga && visibles.length === 0;

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
                      : "border-[var(--color-border-soft)] bg-white hover:border-[var(--color-border-accent)] hover:bg-[var(--color-bg)]"
                  }`}
                >
                  <span className="flex items-center gap-2">
                    <input
                      type="radio"
                      name={`imagen-tipo-${actividadId}`}
                      value={opcion.valor}
                      checked={seleccionada}
                      onChange={() => cambiarTipo(opcion.valor)}
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
              /* Varias solo en galería: la portada es una sola. */
              multiple={tipo === "GALERIA"}
              onChange={manejarSeleccionArchivos}
              disabled={subiendo}
              className="peer sr-only"
            />
            <label
              htmlFor={idArchivo}
              className="inline-flex min-h-11 cursor-pointer items-center justify-center rounded-[18px] border border-[var(--color-border-accent)] bg-white px-5 py-3 text-sm font-extrabold text-[var(--color-primary)] shadow-sm transition duration-200 ease-out hover:-translate-y-0.5 hover:border-[var(--color-primary)] hover:bg-[var(--color-surface-soft)] peer-focus-visible:ring-4 peer-focus-visible:ring-[#4FB3D9]/30 peer-disabled:cursor-not-allowed peer-disabled:opacity-50"
            >
              {seleccion.length > 0
                ? "Cambiar selección"
                : tipo === "GALERIA"
                  ? "Elegir imágenes"
                  : "Elegir imagen"}
            </label>
            <p className="min-w-0 flex-1 truncate text-sm text-[var(--color-muted)]">
              {seleccion.length > 0
                ? `${seleccion.length} ${
                    seleccion.length === 1 ? "archivo elegido" : "archivos elegidos"
                  }`
                : tipo === "GALERIA"
                  ? "JPG, PNG o WebP · hasta 2 MB · podés elegir varias"
                  : "JPG, PNG o WebP · hasta 2 MB"}
            </p>
          </div>
        </div>

        {seleccion.length > 0 ? (
          <div className="rounded-[18px] border border-[var(--color-border-soft)] bg-[var(--color-bg)] p-3">
            <p className="text-sm text-[var(--color-muted)]">
              Se {seleccion.length === 1 ? "va" : "van"} a subir como{" "}
              <span className="font-bold text-[var(--color-primary)]">
                {tipo === "PRINCIPAL" ? "imagen principal" : "galería"}
              </span>
              .
            </p>

            <ul className="mt-3 flex flex-wrap gap-3">
              {seleccion.map((elegido) => (
                <li key={elegido.url} className="relative">
                  {/* Preview local (object URL, no next/image). */}
                  {/* eslint-disable-next-line @next/next/no-img-element */}
                  <img
                    src={elegido.url}
                    alt={`Vista previa de ${elegido.archivo.name}`}
                    className="h-20 w-28 rounded-[12px] object-cover"
                  />
                  <button
                    type="button"
                    onClick={() => quitarDeLaSeleccion(elegido.url)}
                    disabled={subiendo}
                    aria-label={`Quitar ${elegido.archivo.name} de la selección`}
                    className="absolute -right-2 -top-2 inline-flex h-7 w-7 items-center justify-center rounded-full border border-[var(--color-border-soft)] bg-white text-sm font-extrabold text-[var(--color-primary)] shadow-sm transition duration-200 ease-out hover:border-red-300 hover:text-red-700 disabled:opacity-50"
                  >
                    ×
                  </button>
                  <button
                    type="button"
                    onClick={() =>
                      setEditando((actual) =>
                        actual === elegido.url ? null : elegido.url
                      )
                    }
                    disabled={subiendo}
                    className="mt-1.5 w-full text-xs font-extrabold text-[var(--color-primary)] underline decoration-[var(--color-border-accent)] underline-offset-4 hover:decoration-[var(--color-primary)] disabled:opacity-50"
                  >
                    {elegido.ajustado ? "Reencuadrar" : "Ajustar"}
                  </button>
                </li>
              ))}
            </ul>

            {editandoElegido ? (
              <div className="mt-4">
                <EditorRecorteImagen
                  key={editandoElegido.url}
                  archivo={editandoElegido.archivo}
                  url={editandoElegido.url}
                  tipo={tipo}
                  onConfirmar={(recortada) =>
                    aplicarRecorte(editandoElegido.url, recortada)
                  }
                  onCancelar={() => setEditando(null)}
                />
              </div>
            ) : null}
          </div>
        ) : null}

        <div className="flex flex-col gap-3 sm:flex-row-reverse sm:items-center sm:justify-between">
          <AppButton
            type="submit"
            disabled={subiendo || seleccion.length === 0}
            fullWidth
            className="sm:w-auto"
          >
            {subiendo
              ? progreso
                ? `Subiendo ${progreso.hecho} de ${progreso.total}...`
                : "Subiendo..."
              : seleccion.length > 1
                ? `Subir ${seleccion.length} imágenes`
                : "Subir imagen"}
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
          onGuardarTexto={manejarGuardarTexto}
        />
      ) : null}

      {galeria.length > 0 ? (
        <GrupoImagenes
          titulo="Galería"
          ayuda="Fotos adicionales de la actividad. Las aprobadas se muestran en este orden."
          imagenes={galeria}
          onQuitar={manejarQuitar}
          onGuardarTexto={manejarGuardarTexto}
          idsOrdenables={galeriaAprobada.map((imagen) => imagen.id)}
          onMover={manejarMover}
          onHacerPrincipal={manejarHacerPrincipal}
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
  onGuardarTexto: (
    imagen: ImagenActividadPublicador,
    titulo: string,
    descripcion: string
  ) => Promise<void>;
  /* Solo la galería aprobada se ordena y puede pasar a principal. */
  idsOrdenables?: number[];
  onMover?: (imagen: ImagenActividadPublicador, direccion: -1 | 1) => void;
  onHacerPrincipal?: (imagen: ImagenActividadPublicador) => void;
};

/*
  Listado de un tipo de imagen. Separar principal de galería evita que
  el publicador tenga que deducir el destino leyendo la etiqueta de
  cada fila. Desde la fase 2, las aprobadas de la galería suman orden
  (flechas), promoción a principal, texto y eliminación.
*/
function GrupoImagenes({
  titulo,
  ayuda,
  imagenes,
  onQuitar,
  onGuardarTexto,
  idsOrdenables,
  onMover,
  onHacerPrincipal,
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
          const aprobada = imagen.estadoModeracion === "APROBADA";
          const pendiente = imagen.estadoModeracion === "PENDIENTE";
          const posicionOrden = idsOrdenables?.indexOf(imagen.id) ?? -1;
          const ordenable = aprobada && posicionOrden >= 0;

          return (
            <li
              key={imagen.id}
              className="rounded-[18px] border border-[var(--color-border-soft)] bg-[var(--color-surface)]/80 p-3"
            >
              <div className="flex flex-wrap items-center gap-4">
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
                    className="flex h-20 w-28 shrink-0 items-center justify-center rounded-[12px] bg-[var(--color-bg)] text-xs font-bold text-[var(--color-muted)]"
                  >
                    Sin vista previa
                  </span>
                )}

                <div className="min-w-0 flex-1">
                  <span
                    className={`inline-flex rounded-full px-3 py-1 text-xs font-extrabold ${
                      ESTILOS_ESTADO[imagen.estadoModeracion] ??
                      "bg-[var(--color-bg)] text-[var(--color-muted)] ring-1 ring-[var(--color-border-soft)]"
                    }`}
                  >
                    {formatearEstado(imagen.estadoModeracion)}
                  </span>

                  {imagen.titulo || imagen.descripcion ? (
                    <p className="mt-2 truncate text-sm leading-6 text-[var(--color-muted)]">
                      {[imagen.titulo, imagen.descripcion]
                        .filter(Boolean)
                        .join(" · ")}
                    </p>
                  ) : null}

                  {imagen.motivoRechazo ? (
                    <p className="mt-2 text-sm leading-6 text-red-700">
                      <span className="font-bold">Motivo:</span>{" "}
                      {imagen.motivoRechazo}
                    </p>
                  ) : null}
                </div>

                <div className="flex flex-wrap items-center gap-2">
                  {ordenable && onMover ? (
                    <>
                      <button
                        type="button"
                        onClick={() => onMover(imagen, -1)}
                        disabled={posicionOrden === 0}
                        aria-label="Mover la foto un lugar hacia adelante"
                        className="inline-flex h-10 w-10 items-center justify-center rounded-[14px] border border-[var(--color-border-accent)] bg-[var(--color-surface)] text-sm font-extrabold text-[var(--color-primary)] shadow-sm transition duration-200 ease-out hover:border-[var(--color-primary)] disabled:cursor-not-allowed disabled:opacity-40"
                      >
                        ↑
                      </button>
                      <button
                        type="button"
                        onClick={() => onMover(imagen, 1)}
                        disabled={
                          posicionOrden === (idsOrdenables?.length ?? 0) - 1
                        }
                        aria-label="Mover la foto un lugar hacia atrás"
                        className="inline-flex h-10 w-10 items-center justify-center rounded-[14px] border border-[var(--color-border-accent)] bg-[var(--color-surface)] text-sm font-extrabold text-[var(--color-primary)] shadow-sm transition duration-200 ease-out hover:border-[var(--color-primary)] disabled:cursor-not-allowed disabled:opacity-40"
                      >
                        ↓
                      </button>
                    </>
                  ) : null}

                  {ordenable && onHacerPrincipal ? (
                    <button
                      type="button"
                      onClick={() => onHacerPrincipal(imagen)}
                      className="inline-flex min-h-10 items-center justify-center rounded-[18px] border border-[var(--color-border-accent)] bg-[var(--color-surface)] px-4 text-xs font-extrabold text-[var(--color-primary)] shadow-sm transition duration-200 ease-out hover:border-[var(--color-primary)] hover:bg-[var(--color-surface-soft)] active:scale-[0.98]"
                    >
                      Hacer principal
                    </button>
                  ) : null}

                  {pendiente || aprobada ? (
                    <button
                      type="button"
                      onClick={() => onQuitar(imagen)}
                      aria-label={
                        pendiente
                          ? "Retirar imagen pendiente"
                          : "Eliminar foto aprobada"
                      }
                      className="inline-flex min-h-10 items-center justify-center rounded-[18px] border border-red-200 bg-red-50 px-4 text-xs font-extrabold text-red-700 shadow-sm transition duration-200 ease-out hover:border-red-300 hover:bg-[var(--color-surface)] active:scale-[0.98]"
                    >
                      {pendiente ? "Retirar" : "Eliminar"}
                    </button>
                  ) : null}
                </div>
              </div>

              {pendiente || aprobada ? (
                <EditorTextoImagen imagen={imagen} onGuardar={onGuardarTexto} />
              ) : null}
            </li>
          );
        })}
      </ul>
    </section>
  );
}

/*
  Texto de la foto (título y descripción): alimenta el texto alternativo
  y el epígrafe públicos. Colapsado por defecto para no volver cada fila
  un formulario.
*/
function EditorTextoImagen({
  imagen,
  onGuardar,
}: {
  imagen: ImagenActividadPublicador;
  onGuardar: (
    imagen: ImagenActividadPublicador,
    titulo: string,
    descripcion: string
  ) => Promise<void>;
}) {
  const [abierto, setAbierto] = useState(false);
  const [titulo, setTitulo] = useState(imagen.titulo ?? "");
  const [descripcion, setDescripcion] = useState(imagen.descripcion ?? "");
  const [guardando, setGuardando] = useState(false);

  async function manejarGuardar() {
    if (guardando) {
      return;
    }

    setGuardando(true);

    try {
      await onGuardar(imagen, titulo, descripcion);
      setAbierto(false);
    } catch {
      /* El error ya lo mostró el contenedor; el formulario queda abierto. */
    } finally {
      setGuardando(false);
    }
  }

  if (!abierto) {
    return (
      <button
        type="button"
        onClick={() => {
          setTitulo(imagen.titulo ?? "");
          setDescripcion(imagen.descripcion ?? "");
          setAbierto(true);
        }}
        className="mt-2 text-xs font-extrabold text-[var(--color-primary)] underline decoration-[var(--color-border-accent)] underline-offset-4 transition hover:decoration-[var(--color-primary)]"
      >
        {imagen.titulo || imagen.descripcion
          ? "Editar texto de la foto"
          : "Agregar texto a la foto"}
      </button>
    );
  }

  return (
    <div className="mt-3 grid gap-3 rounded-[14px] border border-[var(--color-border-soft)] bg-[var(--color-bg)] p-3 sm:grid-cols-2">
      <label className="block">
        <span className="text-xs font-bold text-[var(--color-primary)]">
          Título
        </span>
        <input
          type="text"
          value={titulo}
          maxLength={150}
          onChange={(evento) => setTitulo(evento.target.value)}
          disabled={guardando}
          placeholder="Ej: Sala de musculación"
          className="mt-1.5 min-h-10 w-full rounded-[12px] border border-[var(--color-border-accent)] bg-[var(--color-surface)] px-3 text-sm text-[var(--color-text)] outline-none transition focus:border-[var(--color-accent)] focus:ring-4 focus:ring-[var(--color-border-soft)] disabled:opacity-60"
        />
      </label>

      <label className="block">
        <span className="text-xs font-bold text-[var(--color-primary)]">
          Descripción
        </span>
        <input
          type="text"
          value={descripcion}
          maxLength={255}
          onChange={(evento) => setDescripcion(evento.target.value)}
          disabled={guardando}
          placeholder="Se usa como texto alternativo de la foto"
          className="mt-1.5 min-h-10 w-full rounded-[12px] border border-[var(--color-border-accent)] bg-[var(--color-surface)] px-3 text-sm text-[var(--color-text)] outline-none transition focus:border-[var(--color-accent)] focus:ring-4 focus:ring-[var(--color-border-soft)] disabled:opacity-60"
        />
      </label>

      <div className="flex gap-2 sm:col-span-2">
        <AppButton size="sm" onClick={manejarGuardar} disabled={guardando}>
          {guardando ? "Guardando..." : "Guardar texto"}
        </AppButton>
        <AppButton
          size="sm"
          variant="secondary"
          onClick={() => setAbierto(false)}
          disabled={guardando}
        >
          Cancelar
        </AppButton>
      </div>
    </div>
  );
}
