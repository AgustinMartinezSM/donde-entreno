/*
  Formateo compartido de valores de catálogo y precios.

  El backend expone enums crudos en mayúsculas (PRINCIPIANTE, PRESENCIAL,
  HIBRIDA, CLUB_DEPORTIVO...). Antes cada componente los formateaba por su
  cuenta (o no los formateaba): este módulo es la única fuente de verdad
  para que la misma actividad hable igual en cards, filtros y detalle.
*/

const ETIQUETAS_CONOCIDAS: Record<string, string> = {
  PRINCIPIANTE: "Principiante",
  INTERMEDIO: "Intermedio",
  AVANZADO: "Avanzado",
  PRESENCIAL: "Presencial",
  ONLINE: "Online",
  HIBRIDA: "Híbrida",
  "HÍBRIDA": "Híbrida",
};

export function formatearEtiquetaCatalogo(valor: string): string {
  const valorNormalizado = valor.trim();

  if (!valorNormalizado) {
    return valorNormalizado;
  }

  const etiqueta = ETIQUETAS_CONOCIDAS[valorNormalizado.toUpperCase()];

  if (etiqueta) {
    return etiqueta;
  }

  return valorNormalizado
    .replaceAll("_", " ")
    .toLowerCase()
    .replace(/\b\p{L}/gu, (letra) => letra.toUpperCase());
}

/* CLUB_DEPORTIVO → "Club deportivo": solo la primera palabra lleva mayúscula. */
export function formatearTipoPublicador(tipo: string): string {
  const tipoNormalizado = tipo.trim().toLocaleLowerCase("es").replaceAll("_", " ");

  if (!tipoNormalizado) {
    return tipoNormalizado;
  }

  return (
    tipoNormalizado.charAt(0).toLocaleUpperCase("es") + tipoNormalizado.slice(1)
  );
}

const formateadorPesos = new Intl.NumberFormat("es-AR", {
  style: "currency",
  currency: "ARS",
  maximumFractionDigits: 0,
});

/* 12000 → "$ 12.000". Devuelve null si el precio no es mostrable. */
export function formatearPrecio(valor: number | null | undefined): string | null {
  if (valor == null || !Number.isFinite(valor) || valor <= 0) {
    return null;
  }

  return formateadorPesos.format(valor);
}
