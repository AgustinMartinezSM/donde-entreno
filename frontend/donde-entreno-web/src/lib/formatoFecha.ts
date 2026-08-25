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

/*
  Fecha de un EVENTO (Fase 9): "sábado 12 de septiembre, 18:00".

  Función aparte y no un caso más de `formatearFechaRelativa`: esa
  aplasta todo el futuro a "hoy" a propósito (el feed cuenta lo que ya
  pasó), y de un evento lo único que importa es exactamente cuándo es.
*/
export function formatearFechaEvento(
  fechaIso: string | null | undefined,
  opciones: { conHora?: boolean } = {}
): string | null {
  if (!fechaIso) {
    return null;
  }

  const fecha = new Date(fechaIso);

  if (Number.isNaN(fecha.getTime())) {
    return null;
  }

  const dia = fecha.toLocaleDateString("es-AR", {
    weekday: "long",
    day: "numeric",
    month: "long",
  });

  if (opciones.conHora === false) {
    return dia;
  }

  const hora = fecha.toLocaleTimeString("es-AR", {
    hour: "2-digit",
    minute: "2-digit",
  });

  return `${dia}, ${hora}`;
}

/*
  Cuánto falta, en palabras cortas para un chip ("hoy", "mañana",
  "en 3 días"). Devuelve null si ya pasó: un evento vencido no dice
  "hace 2 días", directamente no muestra chip.
*/
export function formatearCuantoFalta(
  fechaIso: string | null | undefined
): string | null {
  if (!fechaIso) {
    return null;
  }

  const fecha = new Date(fechaIso);

  if (Number.isNaN(fecha.getTime())) {
    return null;
  }

  const dias = Math.ceil((fecha.getTime() - Date.now()) / MS_POR_DIA);

  if (dias < 0) {
    return null;
  }

  if (dias === 0) {
    return "hoy";
  }

  if (dias === 1) {
    return "mañana";
  }

  if (dias <= 7) {
    return `en ${dias} días`;
  }

  return null;
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
