"use client";

/*
  Motor remoto del asistente: el endpoint público del backend.

  El backend resuelve la consulta contra el catálogo real y, si el motor
  local de allá tampoco entiende, la manda a Gemini para traducirla a
  filtros. Acá no sabemos ni nos importa cuál de los dos contestó.

  Nunca lanza: cualquier problema devuelve null y el asistente se queda
  con la respuesta del motor local del navegador.
*/

import { API_BASE_URL } from "../apiConfig";
import type { ContextoAsistente, EnlaceAsistente, RespuestaAsistente } from "./tipos";

/*
  Mismo tope que valida el backend: cortamos antes de gastar una request.
  El input además tiene maxLength, así que llegar acá pasado de largo es
  raro; queda como red por si el texto entra pegado desde otro lado.
*/
export const MAX_CARACTERES_CONSULTA = 300;
const MAX_CARACTERES = MAX_CARACTERES_CONSULTA;

/*
  Más que esto no se espera: el usuario ya tiene una respuesta local
  aceptable y hacerlo esperar de más es peor que dársela.
*/
const TIMEOUT_MS = 10_000;

export async function consultarAsistenteRemoto(
  entrada: string,
  contexto?: ContextoAsistente
): Promise<RespuestaAsistente | null> {
  const texto = entrada.trim();

  if (!API_BASE_URL || !texto || texto.length > MAX_CARACTERES) {
    return null;
  }

  try {
    const respuesta = await fetch(`${API_BASE_URL}/api/asistente/consulta`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        texto,
        rutaActual: contexto?.rutaActual,
        historial: contexto?.historial ?? [],
      }),
      signal: AbortSignal.timeout(TIMEOUT_MS),
    });

    if (!respuesta.ok) {
      /* 429, 503 o cualquier error: seguimos con lo que sabe el navegador. */
      return null;
    }

    return normalizarRespuesta(await respuesta.json());
  } catch {
    /* Sin red, timeout o JSON ilegible: idem. */
    return null;
  }
}

function normalizarRespuesta(valor: unknown): RespuestaAsistente | null {
  if (typeof valor !== "object" || valor === null) {
    return null;
  }

  const objeto = valor as Record<string, unknown>;

  if (typeof objeto.texto !== "string" || objeto.texto.trim().length === 0) {
    return null;
  }

  return {
    texto: objeto.texto,
    enlaces: normalizarEnlaces(objeto.enlaces),
    opcionesRapidas: normalizarOpciones(objeto.opcionesRapidas),
  };
}

/*
  Solo rutas internas.

  El backend arma los enlaces a partir de slugs de la base, así que no
  deberían venir absolutos nunca. Pero esto se renderiza como <Link> en
  la pantalla del usuario, y una URL externa metida en una respuesta de
  API no es algo que queramos poder pintar: se descarta y listo.
*/
function normalizarEnlaces(valor: unknown): EnlaceAsistente[] | undefined {
  if (!Array.isArray(valor)) {
    return undefined;
  }

  const enlaces = valor.filter((enlace): enlace is EnlaceAsistente => {
    if (typeof enlace !== "object" || enlace === null) {
      return false;
    }

    const objeto = enlace as Record<string, unknown>;

    return (
      typeof objeto.href === "string" &&
      objeto.href.startsWith("/") &&
      !objeto.href.startsWith("//") &&
      typeof objeto.etiqueta === "string" &&
      objeto.etiqueta.length > 0
    );
  });

  return enlaces.length > 0 ? enlaces : undefined;
}

function normalizarOpciones(valor: unknown): string[] | undefined {
  if (!Array.isArray(valor)) {
    return undefined;
  }

  const opciones = valor.filter(
    (opcion): opcion is string => typeof opcion === "string" && opcion.length > 0
  );

  return opciones.length > 0 ? opciones : undefined;
}
