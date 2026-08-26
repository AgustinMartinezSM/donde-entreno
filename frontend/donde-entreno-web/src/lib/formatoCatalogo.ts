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

/* Igual que formatearEtiquetaCatalogo, tolerando null. */
export function formatearEtiquetaCatalogoONull(
  valor: string | null | undefined
): string | null {
  return valor ? formatearEtiquetaCatalogo(valor) : null;
}

/*
  Los días llevan mapa explícito y no pasan por el capitalizador
  genérico: el enum viaja SIN acento (MIERCOLES, SABADO) y "Miercoles"
  en pantalla se lee como un error de tipeo del sitio.

  El orden de este objeto ES el orden de la semana, y se usa para
  ordenar en listarDiasOrdenados.
*/
const NOMBRE_DEL_DIA: Record<string, string> = {
  LUNES: "Lunes",
  MARTES: "Martes",
  MIERCOLES: "Miércoles",
  JUEVES: "Jueves",
  VIERNES: "Viernes",
  SABADO: "Sábado",
  DOMINGO: "Domingo",
};

/* MIERCOLES → "Miércoles". Un valor desconocido cae al capitalizador. */
export function formatearDiaSemana(
  dia: string | null | undefined
): string | null {
  if (!dia) {
    return null;
  }

  return NOMBRE_DEL_DIA[dia.trim().toUpperCase()] ?? formatearEtiquetaCatalogo(dia);
}

/*
  Días únicos y ordenados por semana. Ordenar importa: "Jueves, Martes"
  se lee como un error aunque sea fiel al orden de carga.
*/
export function listarDiasOrdenados(
  horarios: Array<{ diaSemana?: string | null }>
): string[] {
  const orden = Object.values(NOMBRE_DEL_DIA);
  const dias: string[] = [];

  for (const horario of horarios) {
    const nombre = formatearDiaSemana(horario.diaSemana);

    if (nombre && !dias.includes(nombre)) {
      dias.push(nombre);
    }
  }

  return dias.sort((a, b) => orden.indexOf(a) - orden.indexOf(b));
}
