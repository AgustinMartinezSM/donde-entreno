/*
  Formateo de fechas relativo en español ("hoy", "ayer", "hace 3 días",
  "hace 2 meses"). Es lo que hace que el feed se sienta feed y no listado.
*/

const formateadorRelativo = new Intl.RelativeTimeFormat("es-AR", {
  numeric: "auto",
});

const MS_POR_DIA = 24 * 60 * 60 * 1000;

/*
  Devuelve la fecha relativa a hoy, o null si la fecha no es válida
  (backend anterior al campo, valor corrupto): el que llama simplemente
  no muestra nada.
*/
export function formatearFechaRelativa(
  fechaIso: string | null | undefined
): string | null {
  if (!fechaIso) {
    return null;
  }

  const fecha = new Date(fechaIso);

  if (Number.isNaN(fecha.getTime())) {
    return null;
  }

  const dias = Math.round((fecha.getTime() - Date.now()) / MS_POR_DIA);

  /* Fechas futuras (reloj desfasado) se tratan como "hoy". */
  if (dias >= 0) {
    return formateadorRelativo.format(0, "day");
  }

  if (dias > -7) {
    return formateadorRelativo.format(dias, "day");
  }

  if (dias > -30) {
    return formateadorRelativo.format(Math.round(dias / 7), "week");
  }

  if (dias > -365) {
    return formateadorRelativo.format(Math.round(dias / 30), "month");
  }

  return formateadorRelativo.format(Math.round(dias / 365), "year");
}

/* Fecha absoluta corta para tooltips ("7 de agosto de 2026"). */
export function formatearFechaLarga(
  fechaIso: string | null | undefined
): string | null {
  if (!fechaIso) {
    return null;
  }

  const fecha = new Date(fechaIso);

  if (Number.isNaN(fecha.getTime())) {
    return null;
  }

  return fecha.toLocaleDateString("es-AR", {
    day: "numeric",
    month: "long",
    year: "numeric",
  });
}
