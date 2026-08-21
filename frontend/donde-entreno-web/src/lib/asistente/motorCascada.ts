"use client";

/*
  Motor en cascada: qué se resuelve en el navegador y qué va al backend.

  El asistente V1 preguntaba primero al navegador SIEMPRE, y solo iba al
  backend si no había entendido nada. Eso hacía dos cosas mal:

  - "no quiero básquet" lo entendía el motor local (ve la palabra
    "básquet") y contestaba con actividades de básquet. Un rechazo leído
    como pedido.
  - "holis, algún deporte que recomiendes?" matcheaba el saludo y se
    contestaba con un saludo.

  En los dos casos el navegador entendía ALGO y por eso nunca cedía,
  aunque la consulta tuviera más señal de la que él puede aprovechar. Esa
  era la limitación conocida del bloque anterior, y esto la arregla.

  La regla nueva reparte por clase de consulta:

  - Ayuda de la app (cómo publicar, dónde veo mis imágenes, qué es la
    imagen principal): SIEMPRE local. La respuesta es la misma con o sin
    contexto, es instantánea, gratis y funciona sin conexión.
  - Un saludo suelto: local.
  - Un deporte nombrado a secas ("busco karate"), sin charla previa ni
    negaciones: local. Es la consulta más común y ya se resolvía bien.
  - Todo lo demás — preferencias, rechazos, correcciones, cualquier cosa
    con conversación previa: al backend, que tiene memoria de la charla,
    conoce el catálogo real y puede recomendar deportes que todavía no
    están cargados.

  Si el backend no contesta (sin red, timeout, 429, caído), se usa lo que
  el navegador tenía. Nunca se queda mudo.
*/

import { normalizarTexto } from "../deporteSearch";
import { resolverLocal, tieneSenalMasAllaDelDeporte } from "./motorLocal";
import { consultarAsistenteRemoto } from "./motorRemoto";
import type {
  ContextoAsistente,
  MotorAsistente,
  RespuestaAsistente,
} from "./tipos";

/*
  Señales de que la consulta lleva más información de la que el motor
  local sabe usar: rechazos y preferencias.

  Alcanza con que sean groseras — no deciden la respuesta, solo si vale la
  pena preguntarle al backend. Del otro lado hay un analizador serio.
*/
const SENALES_CONVERSACIONALES = [
  /* rechazos */
  "no",
  "nada",
  "sin",
  "tampoco",
  "odio",
  "detesto",
  "aburre",
  "aburrido",
  "aburrida",
  "miedo",
  "cansa",
  "harto",
  "harta",
  "evito",
  "menos",
  "otra",
  "otras",
  "otro",
  /* preferencias */
  "social",
  "sociales",
  "gente",
  "grupo",
  "grupal",
  "amigos",
  "tranqui",
  "tranquilo",
  "tranquila",
  "suave",
  "relajado",
  "intenso",
  "intensa",
  "exigente",
  "varien",
  "variado",
  "variedad",
  "canso",
  "estres",
  "ansiedad",
  "competir",
  "competitivo",
  "torneo",
  "aire",
  "resistencia",
  "aguante",
  "cardio",
  /* salud: siempre conviene que lo maneje el backend */
  "lesion",
  "duele",
  "dolor",
  "rodilla",
  "espalda",
  "operado",
  "embarazada",
];

function tieneSenalConversacional(entrada: string): boolean {
  const conBordes = ` ${normalizarTexto(entrada)} `;

  return SENALES_CONVERSACIONALES.some((senal) =>
    conBordes.includes(` ${senal} `)
  );
}

export const motorAsistenteCascada: MotorAsistente = {
  async procesar(
    entrada: string,
    contexto?: ContextoAsistente
  ): Promise<RespuestaAsistente> {
    const local = resolverLocal(entrada, contexto);
    const hayCharlaPrevia = (contexto?.historial?.length ?? 0) > 0;

    if (local.tipo === "ayuda-app" || local.tipo === "conversacion") {
      return local.respuesta;
    }

    /*
      El deporte nombrado a secas se queda local SOLO si la consulta no
      trae nada más: ni charla previa, ni señal conversacional, ni un
      resto que el catálogo no explique. "yoga en Constitución" resuelve
      "yoga" acá, pero "constitucion" sobra — y el backend sí sabe
      filtrar por barrio, día o nivel (la limitación A3 que esto cierra).
    */
    if (
      local.tipo === "deporte" &&
      !hayCharlaPrevia &&
      !tieneSenalConversacional(entrada) &&
      (!local.deporteResuelto ||
        !tieneSenalMasAllaDelDeporte(entrada, local.deporteResuelto))
    ) {
      return local.respuesta;
    }

    const remota = await consultarAsistenteRemoto(entrada, contexto);

    return remota ?? local.respuesta;
  },
};
