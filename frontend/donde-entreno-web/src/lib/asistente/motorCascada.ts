"use client";

/*
  Motor en cascada: primero el local, y solo si no entendió, el remoto.

  El orden importa y es la decisión de fondo del bloque G. El motor local
  es instantáneo, gratis y determinístico, y hoy resuelve bien la mayoría
  de las consultas concretas. Preguntarle al backend por todo cambiaría
  respuestas que ya son correctas por otras más lentas y pagas, y ataría
  el asistente entero a que un servicio externo esté vivo.

  Así, el costo se paga únicamente en las consultas que el navegador no
  sabe resolver, y si el backend o el modelo se caen, el asistente sigue
  funcionando exactamente como antes de este bloque.
*/

import { RESPUESTA_FALLBACK } from "./conocimiento";
import { motorAsistenteLocal } from "./motorLocal";
import { consultarAsistenteRemoto } from "./motorRemoto";
import type { ContextoAsistente, MotorAsistente, RespuestaAsistente } from "./tipos";

export const motorAsistenteCascada: MotorAsistente = {
  async procesar(
    entrada: string,
    contexto?: ContextoAsistente
  ): Promise<RespuestaAsistente> {
    const local = await motorAsistenteLocal.procesar(entrada, contexto);

    /*
      El motor local devuelve el MISMO objeto RESPUESTA_FALLBACK cuando no
      entiende, así que comparar por identidad es exacto: no hace falta
      adivinar por el texto.
    */
    if (local !== RESPUESTA_FALLBACK) {
      return local;
    }

    const remota = await consultarAsistenteRemoto(entrada, contexto);

    return remota ?? local;
  },
};
