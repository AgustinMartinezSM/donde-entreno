import { API_BASE_URL } from "../lib/apiConfig";

/*
  El pulso del producto (Fase 10, paso 0): conteos agregados para
  responder "¿qué se está usando?" antes de construir más encima.

  Solo números: ningún dato de una persona, ningún contenido.
*/

export type MetricaPulso = {
  etiqueta: string;
  total: number;
  /** Cuántos de esos son de los últimos 30 días, cuando aplica. */
  ultimos30Dias: number | null;
};

export type BloquePulso = {
  titulo: string;
  metricas: MetricaPulso[];
};

export type Pulso = {
  bloques: BloquePulso[];
};

export async function obtenerPulso(accessToken: string): Promise<Pulso> {
  const respuesta = await fetch(`${API_BASE_URL}/api/admin/pulso`, {
    headers: {
      "Accept": "application/json",
      "Authorization": `Bearer ${accessToken}`,
    },
    cache: "no-store",
  });

  if (!respuesta.ok) {
    throw new Error("No pudimos leer el pulso.");
  }

  return (await respuesta.json()) as Pulso;
}
