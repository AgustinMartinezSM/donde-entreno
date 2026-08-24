import { API_BASE_URL } from "./apiConfig";

/*
  Tracking anónimo de interacciones (script 28, Fase 2 social).
  Beacon best-effort: JAMÁS bloquea ni rompe la navegación — si falla,
  simplemente no se cuenta. Sin datos del usuario, solo el evento.
*/

export type TipoInteraccion =
  | "VISTA_DETALLE"
  | "CLICK_WHATSAPP"
  | "CLICK_COMPARTIR";

export function registrarInteraccion(
  actividadId: number,
  tipo: TipoInteraccion
): void {
  enviarBeacon(
    `${API_BASE_URL}/api/actividades/${actividadId}/interacciones`,
    tipo
  );
}

/**
 * Interacción sobre el PERFIL del publicador (Fase 5). Va a su propio
 * endpoint: el evento del perfil no cuelga de ninguna actividad, así
 * las métricas por actividad no se ensucian.
 */
export function registrarInteraccionPerfil(
  perfilPublicadorId: number,
  tipo: TipoInteraccion
): void {
  enviarBeacon(
    `${API_BASE_URL}/api/perfiles-publicadores/${perfilPublicadorId}/interacciones`,
    tipo
  );
}

function enviarBeacon(url: string, tipo: TipoInteraccion): void {
  try {
    /*
      fetch keepalive y NO sendBeacon a propósito: la API vive en otro
      origen y un beacon con content-type JSON exige preflight CORS,
      que sendBeacon no puede hacer — fallaría en silencio siempre.
      keepalive sobrevive a la navegación (el click de WhatsApp abre
      otra pestaña) y sí pasa por el preflight normal.
    */
    void fetch(url, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ tipo }),
      keepalive: true,
    }).catch(() => {
      /* Best-effort. */
    });
  } catch {
    /* Best-effort. */
  }
}
