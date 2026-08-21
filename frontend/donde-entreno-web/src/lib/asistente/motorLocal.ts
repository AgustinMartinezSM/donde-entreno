/*
  Motor local del Asistente DondeEntreno.

  Implementación 100% determinística sobre la base de conocimiento
  (conocimiento.ts): sin red, sin aleatoriedad. La misma entrada produce
  siempre la misma respuesta.

  Cómo decide:
  1. Normaliza la entrada (sin tildes ni mayúsculas, igual que deporteSearch).
  2. Puntúa las intenciones por coincidencia de palabras clave (frases enteras,
     con borde de palabra; las frases más largas pesan más).
  3. Resuelve deportes y categorías con el buscador existente (deporteSearch)
     contra un catálogo estático, probando la frase completa, bigramas y tokens.
  4. Elige en este orden: intención de prioridad alta → deporte/categoría con
     puntaje fuerte → intención de prioridad baja → deporte/categoría con
     puntaje aceptable → fallback amable.

  Además de la respuesta devuelve QUÉ CLASE de respuesta es, que es lo que
  la cascada usa para decidir si alcanza con el navegador o si conviene
  preguntarle al backend (ver motorCascada.ts).
*/

import type { Deporte } from "../../types/deporte";
import {
  normalizarTexto,
  obtenerCategoriasBusquedaDeportes,
  obtenerPuntajeBusquedaCategoria,
  obtenerPuntajeBusquedaDeporte,
} from "../deporteSearch";
import type { CategoriaBusquedaDeporte } from "../deporteSearch";
import {
  CATALOGO_DEPORTES_ASISTENTE,
  INTENCIONES_ASISTENTE,
  RESPUESTA_FALLBACK,
  crearRespuestaCategoria,
  crearRespuestaDeporte,
} from "./conocimiento";
import type { IntencionAsistente } from "./conocimiento";
import type {
  ContextoAsistente,
  MotorAsistente,
  RespuestaAsistente,
  ResultadoLocal,
} from "./tipos";

/*
  Umbrales tomados de los puntajes de deporteSearch:
  - 950 = coincidencia exacta de nombre, slug o alias (fuerte: le gana a saludos).
  - 450 = coincidencia por categoría relacionada (aceptable solo si no hubo intención).
*/
const PUNTAJE_DEPORTE_FUERTE = 950;
const PUNTAJE_DEPORTE_MINIMO = 450;

// Categorías derivadas del catálogo estático, calculadas una sola vez.
const CATEGORIAS_BUSQUEDA = obtenerCategoriasBusquedaDeportes(
  CATALOGO_DEPORTES_ASISTENTE
);

/*
  Tokens demasiado genéricos como para resolver un deporte por sí solos
  (sí participan en la frase completa y en los bigramas).
*/
const TOKENS_GENERICOS = new Set([
  "quiero",
  "quisiera",
  "busco",
  "buscar",
  "hacer",
  "practicar",
  "probar",
  "empezar",
  "arrancar",
  "deporte",
  "deportes",
  "actividad",
  "actividades",
  "clase",
  "clases",
  "algo",
  "alguna",
  "algun",
  "donde",
  "para",
  "sobre",
  "info",
  "informacion",
  "tengo",
  "ganas",
  "cerca",
  "zona",
  "lugar",
  "entrenar",
  "entreno",
  "entrenamiento",
]);

type CoincidenciaCatalogo =
  | { tipo: "deporte"; deporte: Deporte; puntaje: number }
  | { tipo: "categoria"; categoria: CategoriaBusquedaDeporte; puntaje: number };

/*
  Genera los textos candidatos para resolver contra el catálogo:
  la frase completa, los bigramas (para "muay thai" dentro de una oración)
  y los tokens sueltos que no sean genéricos.
*/
function obtenerCandidatosBusqueda(entradaNormalizada: string): string[] {
  const tokens = entradaNormalizada
    .split(" ")
    .filter((token) => token.length >= 2);
  const candidatos = new Set<string>();

  candidatos.add(entradaNormalizada);

  for (let indice = 0; indice < tokens.length - 1; indice += 1) {
    candidatos.add(`${tokens[indice]} ${tokens[indice + 1]}`);
  }

  for (const token of tokens) {
    if (token.length >= 3 && !TOKENS_GENERICOS.has(token)) {
      candidatos.add(token);
    }
  }

  return Array.from(candidatos);
}

/*
  Busca la mejor coincidencia de deporte o categoría reutilizando los puntajes
  de deporteSearch. Los empates se resuelven a favor del primer candidato y del
  orden del catálogo, así el resultado es siempre el mismo.
*/
function resolverCatalogo(
  entradaNormalizada: string
): CoincidenciaCatalogo | null {
  const candidatos = obtenerCandidatosBusqueda(entradaNormalizada);
  let mejorDeporte: { deporte: Deporte; puntaje: number } | null = null;
  let mejorCategoria: {
    categoria: CategoriaBusquedaDeporte;
    puntaje: number;
  } | null = null;

  for (const candidato of candidatos) {
    for (const deporte of CATALOGO_DEPORTES_ASISTENTE) {
      const puntaje = obtenerPuntajeBusquedaDeporte(deporte, candidato);

      if (puntaje > (mejorDeporte?.puntaje ?? 0)) {
        mejorDeporte = { deporte, puntaje };
      }
    }

    for (const categoria of CATEGORIAS_BUSQUEDA) {
      const puntaje = obtenerPuntajeBusquedaCategoria(categoria, candidato);

      if (puntaje > (mejorCategoria?.puntaje ?? 0)) {
        mejorCategoria = { categoria, puntaje };
      }
    }
  }

  // La categoría solo gana si supera estrictamente al deporte.
  if (
    mejorCategoria &&
    mejorCategoria.puntaje > (mejorDeporte?.puntaje ?? 0)
  ) {
    return { tipo: "categoria", ...mejorCategoria };
  }

  if (mejorDeporte) {
    return { tipo: "deporte", ...mejorDeporte };
  }

  return null;
}

/*
  Puntúa una intención: suma la longitud de cada palabra clave encontrada como
  frase entera (con borde de palabra). La igualdad exacta con toda la entrada
  vale triple, para que "hola" solo pese más que "hola" dentro de una oración.
*/
function puntuarIntencion(
  intencion: IntencionAsistente,
  entradaNormalizada: string
): number {
  const entradaConBordes = ` ${entradaNormalizada} `;
  let puntaje = 0;

  for (const palabraClave of intencion.palabrasClave) {
    const claveNormalizada = normalizarTexto(palabraClave);

    if (!claveNormalizada) {
      continue;
    }

    if (entradaNormalizada === claveNormalizada) {
      puntaje += claveNormalizada.length * 3;
    } else if (entradaConBordes.includes(` ${claveNormalizada} `)) {
      puntaje += claveNormalizada.length;
    }
  }

  /*
    Las exactas solo suman si son toda la entrada: dentro de una frase no
    participan (ver palabrasClaveExactas en conocimiento.ts).
  */
  for (const palabraClave of intencion.palabrasClaveExactas ?? []) {
    const claveNormalizada = normalizarTexto(palabraClave);

    if (claveNormalizada && entradaNormalizada === claveNormalizada) {
      puntaje += claveNormalizada.length * 3;
    }
  }

  return puntaje;
}

function elegirMejorIntencion(
  entradaNormalizada: string,
  prioridad: IntencionAsistente["prioridad"]
): IntencionAsistente | null {
  let mejor: { intencion: IntencionAsistente; puntaje: number } | null = null;

  for (const intencion of INTENCIONES_ASISTENTE) {
    if (intencion.prioridad !== prioridad) {
      continue;
    }

    const puntaje = puntuarIntencion(intencion, entradaNormalizada);

    // Comparación estricta: ante empate gana la intención declarada primero.
    if (puntaje > 0 && puntaje > (mejor?.puntaje ?? 0)) {
      mejor = { intencion, puntaje };
    }
  }

  return mejor?.intencion ?? null;
}

/*
  Pequeños ajustes según la ruta actual, para que el asistente se sienta
  situado dentro de la app (sigue siendo determinístico).
*/
function adaptarRespuestaAlContexto(
  intencion: IntencionAsistente,
  contexto?: ContextoAsistente
): RespuestaAsistente {
  const ruta = contexto?.rutaActual ?? "";

  if (intencion.id === "filtros" && ruta.startsWith("/explorar")) {
    return {
      ...intencion.respuesta,
      texto: `Justo estás en Explorar, así que los tenés a mano. ${intencion.respuesta.texto}`,
    };
  }

  if (intencion.id === "publicar" && ruta.startsWith("/publicar")) {
    return {
      ...intencion.respuesta,
      texto: `Ya estás en la página indicada. ${intencion.respuesta.texto}`,
    };
  }

  return intencion.respuesta;
}

function crearRespuestaCoincidencia(
  coincidencia: CoincidenciaCatalogo
): RespuestaAsistente {
  if (coincidencia.tipo === "deporte") {
    return crearRespuestaDeporte(coincidencia.deporte);
  }

  return crearRespuestaCategoria(
    coincidencia.categoria.valor,
    coincidencia.categoria.nombre
  );
}

/*
  Un saludo suelto se contesta acá; un saludo con una pregunta adentro, no.

  "hola" merece una respuesta instantánea y gratis. "holis, algún deporte
  que recomiendes?" también matchea el saludo, pero contestarlo con el
  saludo es exactamente lo que hacía sentir robot al asistente V1: lo que
  la persona quiere es la recomendación.
*/
function esSoloUnSaludo(
  intencion: IntencionAsistente,
  entradaNormalizada: string
): boolean {
  return intencion.palabrasClave.some(
    (palabraClave) => normalizarTexto(palabraClave) === entradaNormalizada
  );
}

/**
 * Resuelve la consulta en el navegador y dice qué clase de respuesta es.
 *
 * El "tipo" es lo que consume la cascada. Cuando devuelve "recomendacion"
 * o "fallback" está diciendo "tengo algo, pero el backend lo va a hacer
 * mejor": la respuesta que acompaña queda como red de contención.
 */
export function resolverLocal(
  entrada: string,
  contexto?: ContextoAsistente
): ResultadoLocal {
  const entradaNormalizada = normalizarTexto(entrada);

  if (!entradaNormalizada) {
    return { respuesta: RESPUESTA_FALLBACK, tipo: "fallback" };
  }

  const intencionAlta = elegirMejorIntencion(entradaNormalizada, "alta");

  if (intencionAlta) {
    return {
      respuesta: adaptarRespuestaAlContexto(intencionAlta, contexto),
      tipo: intencionAlta.tipo,
    };
  }

  const coincidencia = resolverCatalogo(entradaNormalizada);

  if (coincidencia && coincidencia.puntaje >= PUNTAJE_DEPORTE_FUERTE) {
    return {
      respuesta: crearRespuestaCoincidencia(coincidencia),
      tipo: "deporte",
      deporteResuelto: deporteResueltoDe(coincidencia),
    };
  }

  const intencionBaja = elegirMejorIntencion(entradaNormalizada, "baja");

  if (intencionBaja) {
    return {
      respuesta: adaptarRespuestaAlContexto(intencionBaja, contexto),
      tipo:
        intencionBaja.tipo === "conversacion" &&
        !esSoloUnSaludo(intencionBaja, entradaNormalizada)
          ? "recomendacion"
          : intencionBaja.tipo,
    };
  }

  if (coincidencia && coincidencia.puntaje >= PUNTAJE_DEPORTE_MINIMO) {
    return {
      respuesta: crearRespuestaCoincidencia(coincidencia),
      tipo: "deporte",
      deporteResuelto: deporteResueltoDe(coincidencia),
    };
  }

  return { respuesta: RESPUESTA_FALLBACK, tipo: "fallback" };
}

function deporteResueltoDe(
  coincidencia: CoincidenciaCatalogo
): { nombre: string; slug: string } | undefined {
  if (coincidencia.tipo !== "deporte") {
    return undefined;
  }

  return {
    nombre: coincidencia.deporte.nombre,
    slug: coincidencia.deporte.slug,
  };
}

/*
  Conectores gramaticales que no aportan señal (se suman a los tokens
  genéricos para el chequeo de resto). Solo importan los de 3+ letras:
  el resto ya lo filtra el largo mínimo.
*/
const CONECTORES_SIN_SENAL = new Set([
  "los",
  "las",
  "una",
  "uno",
  "unos",
  "unas",
  "que",
  "con",
  "del",
  "por",
  "como",
  "este",
  "esta",
  "esto",
  "hay",
  "muy",
  "mas",
  "mis",
  "sus",
  "pero",
  "hola",
  "buenas",
  "buenos",
  "gracias",
]);

/**
 * ¿La consulta trae MÁS señal que el deporte que el navegador resolvió?
 *
 * "yoga en Constitución" resuelve "yoga" localmente, pero "constitucion"
 * queda sin usar: el navegador no conoce barrios, días ni niveles, y el
 * backend sí. La regla: si después de sacar muletillas y las palabras
 * del propio deporte sobra algún token con contenido, hay que ceder.
 * Sin listas de barrios ni de días: cualquier palabra que el catálogo
 * no explique es razón suficiente para preguntar.
 */
export function tieneSenalMasAllaDelDeporte(
  entrada: string,
  deporteResuelto: { nombre: string; slug: string }
): boolean {
  const deporteDelCatalogo = CATALOGO_DEPORTES_ASISTENTE.find(
    (deporte) => deporte.slug === deporteResuelto.slug
  );

  const tokens = normalizarTexto(entrada)
    .split(" ")
    .filter((token) => token.length >= 3)
    .filter((token) => !TOKENS_GENERICOS.has(token))
    .filter((token) => !CONECTORES_SIN_SENAL.has(token));

  return tokens.some((token) => {
    /* Consumido por el deporte (nombre, slug o alias, vía el buscador). */
    if (
      deporteDelCatalogo &&
      obtenerPuntajeBusquedaDeporte(deporteDelCatalogo, token) > 0
    ) {
      return false;
    }

    /* Parte de un nombre compuesto ("muay" de "muay thai"). */
    const nombreNormalizado = normalizarTexto(deporteResuelto.nombre);
    if (nombreNormalizado.includes(token)) {
      return false;
    }

    return true;
  });
}

/*
  Motor local puro, sin cascada. Se mantiene como implementación de
  MotorAsistente para poder usarlo solo (por ejemplo, en tests) y porque
  es la red de contención de motorCascada.
*/
export const motorAsistenteLocal: MotorAsistente = {
  async procesar(
    entrada: string,
    contexto?: ContextoAsistente
  ): Promise<RespuestaAsistente> {
    return resolverLocal(entrada, contexto).respuesta;
  },
};
