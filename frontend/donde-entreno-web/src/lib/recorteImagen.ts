/*
  Medidas de destino de cada tipo de imagen.

  El recorte se hace en el cliente y se sube la imagen ya recortada: así
  todas las fotos del mismo tipo entran con la misma proporción (el feed
  y la grilla dejan de mezclar apaisadas con verticales) y el archivo
  llega más liviano al límite de 2 MB.

  Las proporciones salen de dónde se muestra cada una:
  - PRINCIPAL y GALERIA: el hero del detalle y las cards, apaisados.
  - LOGO: siempre dentro de un círculo.
  - PORTADA: la banda ancha del encabezado del perfil.
*/
export type TipoRecorte = "PRINCIPAL" | "GALERIA" | "LOGO" | "PORTADA";

export type MedidaDestino = {
  ancho: number;
  alto: number;
  /* Etiqueta para mostrarle al publicador qué conviene subir. */
  recomendacion: string;
  /* PNG en el logo para no perder la transparencia de las marcas. */
  formato: "image/jpeg" | "image/png";
};

export const MEDIDAS_DESTINO: Record<TipoRecorte, MedidaDestino> = {
  PRINCIPAL: {
    ancho: 1200,
    alto: 900,
    recomendacion: "Apaisada 4:3 · ideal 1200 × 900 px",
    formato: "image/jpeg",
  },
  GALERIA: {
    ancho: 1200,
    alto: 900,
    recomendacion: "Apaisada 4:3 · ideal 1200 × 900 px",
    formato: "image/jpeg",
  },
  LOGO: {
    ancho: 400,
    alto: 400,
    recomendacion: "Cuadrada 1:1 · ideal 400 × 400 px",
    formato: "image/png",
  },
  PORTADA: {
    ancho: 1200,
    alto: 400,
    recomendacion: "Panorámica 3:1 · ideal 1200 × 400 px",
    formato: "image/jpeg",
  },
};

export type EncuadreRecorte = {
  /* Zoom sobre la escala mínima que cubre el marco (1 = justo cubre). */
  zoom: number;
  /* Desplazamiento del centro, en fracción del sobrante (-0.5 a 0.5). */
  desplazamientoX: number;
  desplazamientoY: number;
};

export const ENCUADRE_INICIAL: EncuadreRecorte = {
  zoom: 1,
  desplazamientoX: 0,
  desplazamientoY: 0,
};

export const ZOOM_MAXIMO = 4;

/**
 * Recorta la imagen al tamaño de destino respetando el encuadre.
 *
 * Trabaja siempre en coordenadas de la imagen original, así que el
 * resultado no depende del tamaño con el que se mostró el editor en
 * pantalla.
 */
export async function recortarImagen(
  archivo: File,
  tipo: TipoRecorte,
  encuadre: EncuadreRecorte
): Promise<File> {
  const destino = MEDIDAS_DESTINO[tipo];
  const imagen = await cargarImagen(archivo);

  /*
    Escala mínima que cubre el marco: la mayor entre las dos, para que
    nunca quede un borde vacío.
  */
  const escalaCobertura = Math.max(
    destino.ancho / imagen.width,
    destino.alto / imagen.height
  );
  const escala = escalaCobertura * encuadre.zoom;

  /* Porción de la imagen original que entra en el marco. */
  const anchoVisible = destino.ancho / escala;
  const altoVisible = destino.alto / escala;

  const sobranteX = Math.max(0, imagen.width - anchoVisible);
  const sobranteY = Math.max(0, imagen.height - altoVisible);

  const origenX = sobranteX * (0.5 + limitar(encuadre.desplazamientoX, -0.5, 0.5));
  const origenY = sobranteY * (0.5 + limitar(encuadre.desplazamientoY, -0.5, 0.5));

  const lienzo = document.createElement("canvas");
  lienzo.width = destino.ancho;
  lienzo.height = destino.alto;

  const contexto = lienzo.getContext("2d");

  if (!contexto) {
    throw new Error("No se pudo preparar el recorte de la imagen.");
  }

  /* Fondo blanco: si el origen es PNG con transparencia y se exporta a
     JPEG, sin esto los transparentes salen negros. */
  if (destino.formato === "image/jpeg") {
    contexto.fillStyle = "#FFFFFF";
    contexto.fillRect(0, 0, destino.ancho, destino.alto);
  }

  contexto.imageSmoothingQuality = "high";
  contexto.drawImage(
    imagen,
    origenX,
    origenY,
    anchoVisible,
    altoVisible,
    0,
    0,
    destino.ancho,
    destino.alto
  );

  const blob = await new Promise<Blob | null>((resolver) => {
    lienzo.toBlob(resolver, destino.formato, 0.88);
  });

  if (!blob) {
    throw new Error("No se pudo generar la imagen recortada.");
  }

  const extension = destino.formato === "image/png" ? "png" : "jpg";

  return new File([blob], `${nombreBase(archivo.name)}.${extension}`, {
    type: destino.formato,
  });
}

function nombreBase(nombre: string): string {
  const sinExtension = nombre.replace(/\.[^.]+$/, "");

  return sinExtension.trim() || "imagen";
}

export function limitar(valor: number, minimo: number, maximo: number): number {
  return Math.min(maximo, Math.max(minimo, valor));
}

function cargarImagen(archivo: File): Promise<HTMLImageElement> {
  return new Promise((resolver, rechazar) => {
    const url = URL.createObjectURL(archivo);
    const imagen = new Image();

    imagen.onload = () => {
      URL.revokeObjectURL(url);
      resolver(imagen);
    };

    imagen.onerror = () => {
      URL.revokeObjectURL(url);
      rechazar(new Error("No se pudo leer la imagen."));
    };

    imagen.src = url;
  });
}
