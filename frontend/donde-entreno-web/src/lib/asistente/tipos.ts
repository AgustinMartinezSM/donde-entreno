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
};

/*
  Motor del asistente.

  La interfaz es async a propósito: hoy la implementación es 100% local y
  determinística (ver motorLocal.ts), pero mañana se puede enchufar una IA real
  (una API remota, un modelo, etc.) implementando esta misma interfaz,
  sin tocar una sola línea de la UI.
*/
export interface MotorAsistente {
  procesar(
    entrada: string,
    contexto?: ContextoAsistente
  ): Promise<RespuestaAsistente>;
}
