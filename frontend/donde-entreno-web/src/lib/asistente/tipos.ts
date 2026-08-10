/*
  Contratos del Asistente DondeEntreno.

  Acá viven los tipos que comparten la UI (widget + conversación) y el motor
  que genera las respuestas. La UI solo conoce estos contratos: no sabe si la
  respuesta sale de un motor local o de un servicio externo.
*/

// Enlace interno que el asistente ofrece como acción ("Ver Boxeo", "Ir a Explorar", etc.).
export type EnlaceAsistente = {
  href: string;
  etiqueta: string;
};

// Autor de cada burbuja de la conversación.
export type AutorMensaje = "usuario" | "asistente";

// Un mensaje ya renderizable en la conversación.
export type MensajeAsistente = {
  id: string;
  autor: AutorMensaje;
  texto: string;
  // Enlaces internos sugeridos (se muestran como chips debajo del texto).
  enlaces?: EnlaceAsistente[];
  // Sugerencias de seguimiento: al tocarlas se envían como si el usuario las escribiera.
  opcionesRapidas?: string[];
};

/*
  Un turno pasado, tal como viaja al backend.

  Es la memoria del asistente y vive únicamente en la pestaña: no se
  persiste en localStorage ni en la base. Si el usuario recarga, la charla
  arranca de cero, que es lo que corresponde para algo cuyo contenido puede
  salir hacia un modelo externo.
*/
export type MensajeHistorial = {
  autor: AutorMensaje;
  texto: string;
};

// Lo que devuelve el motor para una entrada del usuario.
export type RespuestaAsistente = {
  texto: string;
  enlaces?: EnlaceAsistente[];
  opcionesRapidas?: string[];
};

// Contexto opcional que la UI le puede pasar al motor para afinar la respuesta.
export type ContextoAsistente = {
  // Ruta actual de la app (por ejemplo "/explorar"), por si la respuesta puede aprovecharla.
  rutaActual?: string;
  // Turnos previos de la conversación, del más viejo al más nuevo.
  historial?: MensajeHistorial[];
};

/*
  Qué clase de respuesta produjo el motor local.

  Existe para que la cascada pueda decidir sin volver a analizar el texto.
  La diferencia que importa: una respuesta de "ayuda-app" (cómo publicar,
  dónde veo mis imágenes) es igual de buena con o sin contexto, así que se
  resuelve en el navegador; una recomendación depende de toda la
  conversación, así que hay que mandarla al backend.
*/
export type TipoRespuestaLocal =
  | "ayuda-app"
  | "conversacion"
  | "recomendacion"
  | "deporte"
  | "fallback";

export type ResultadoLocal = {
  respuesta: RespuestaAsistente;
  tipo: TipoRespuestaLocal;
};

/*
  Motor del asistente.

  La interfaz es async a propósito: la implementación local es
  determinística y sin red, pero la cascada consulta el backend cuando la
  consulta lo amerita, sin que la UI se entere.
*/
export interface MotorAsistente {
  procesar(
    entrada: string,
    contexto?: ContextoAsistente
  ): Promise<RespuestaAsistente>;
}
