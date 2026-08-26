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
  Fecha de un EVENTO (Fase 9): "sábado, 12 de septiembre, 18:00".

  Función aparte y no un caso más de `formatearFechaRelativa`: esa
  aplasta todo el futuro a "hoy" a propósito (el feed cuenta lo que ya
  pasó), y de un evento lo único que importa es exactamente cuándo es.
*/
/*
  Los eventos se muestran en la HORA DEL LUGAR, leída del propio string
  ISO, y no convertida con `new Date(...)`.

  Dos razones, y la segunda es un bug real:

  1. Un evento pasa a una hora en su sede. Alguien mirándolo desde otra
     zona horaria quiere saber a qué hora es ALLÁ, no en su reloj.
  2. Tres de los cuatro lugares que muestran fecha de evento son SERVER
     components, y en Vercel el servidor corre en UTC: `getHours()`
     devolvía 21:30 para un evento de las 18:30 en Argentina. Medido.

  El backend manda `iniciaAt` con offset (2026-09-12T18:30:00-03:00),
  así que la hora escrita en el string ES la que tipeó el publicador.
*/
const DIAS_SEMANA = [
  "domingo", "lunes", "martes", "miércoles",
  "jueves", "viernes", "sábado",
];

const MESES_DEL_ANIO = [
  "enero", "febrero", "marzo", "abril", "mayo", "junio",
  "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre",
];

type PartesFecha = {
  anio: number;
  mes: number;
  dia: number;
  hora: string;
  minutos: string;
};

function leerPartes(iso: string): PartesFecha | null {
  const partes = /^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2})/.exec(iso);

  if (!partes) {
    return null;
  }

  const [, anio, mes, dia, hora, minutos] = partes;

  return {
    anio: Number(anio),
    mes: Number(mes),
    dia: Number(dia),
    hora,
    minutos,
  };
}

/*
  El día de la semana se calcula con Date.UTC sobre los números leídos:
  así no lo corre la zona del entorno, que es justamente el problema
  que este módulo evita.
*/
function nombreDelDia(partes: PartesFecha): string {
  const enUtc = new Date(Date.UTC(partes.anio, partes.mes - 1, partes.dia));

  return DIAS_SEMANA[enUtc.getUTCDay()];
}

export function formatearFechaEvento(
  fechaIso: string | null | undefined,
  opciones: { conHora?: boolean } = {}
): string | null {
  if (!fechaIso) {
    return null;
  }

  const partes = leerPartes(fechaIso);

  if (!partes) {
    return null;
  }

  const mes = MESES_DEL_ANIO[partes.mes - 1];

  if (!mes) {
    return null;
  }

  const dia = `${nombreDelDia(partes)}, ${partes.dia} de ${mes}`;

  if (opciones.conHora === false) {
    return dia;
  }

  return `${dia}, ${partes.hora}:${partes.minutos}`;
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
