import { API_BASE_URL } from "../lib/apiConfig";

/*
  Reportes de contenido (script 28, Fase 2 social). El POST es
  idempotente: reportar dos veces lo mismo no duplica.
*/

export type TipoObjetoReporte =
  | "IMAGEN"
  | "PERFIL_PUBLICADOR"
  | "ACTIVIDAD"
  | "VALORACION"
  | "PREGUNTA"
  | "COMENTARIO"
  | "NOVEDAD"
  | "EVENTO";

export const MOTIVOS_REPORTE = [
  { valor: "CONTENIDO_INAPROPIADO", etiqueta: "Contenido inapropiado" },
  { valor: "INFORMACION_FALSA", etiqueta: "Información falsa o engañosa" },
  { valor: "SPAM", etiqueta: "Spam o publicidad" },
  { valor: "SUPLANTACION", etiqueta: "Se hace pasar por otra persona" },
  { valor: "OTRO", etiqueta: "Otro motivo" },
] as const;

export class ReportesApiError extends Error {
  status: number | null;

  constructor(message: string, status: number | null = null) {
    super(message);
    this.name = "ReportesApiError";
    this.status = status;
  }
}

export type ReporteAdmin = {
  id: number;
  tipoObjeto: string;
  objetoId: number;
  motivo: string;
  detalle: string | null;
  estado: string;
  createdAt: string | null;
};

export type PaginaReportesAdmin = {
  contenido: ReporteAdmin[];
  paginaActual: number;
  totalPaginas: number;
  totalElementos: number;
  ultima: boolean;
};

export async function listarReportesAdmin(
  accessToken: string,
  estado?: string
): Promise<PaginaReportesAdmin> {
  const filtro = estado ? `?estado=${encodeURIComponent(estado)}&size=50` : "?size=50";
  let respuesta: Response;

  try {
    respuesta = await fetch(`${API_BASE_URL}/api/admin/reportes${filtro}`, {
      headers: {
        "Authorization": `Bearer ${accessToken}`,
        "Accept": "application/json",
      },
      cache: "no-store",
    });
  } catch {
    throw new ReportesApiError("No fue posible conectar con el servidor.");
  }

  if (!respuesta.ok) {
    throw new ReportesApiError(
      "No pudimos cargar los reportes.",
      respuesta.status
    );
  }

  return respuesta.json();
}

export async function cambiarEstadoReporteAdmin(
  accessToken: string,
  reporteId: number,
  estado: string
): Promise<ReporteAdmin> {
  let respuesta: Response;

  try {
    respuesta = await fetch(
      `${API_BASE_URL}/api/admin/reportes/${reporteId}/estado`,
      {
        method: "PATCH",
        headers: {
          "Authorization": `Bearer ${accessToken}`,
          "Content-Type": "application/json",
        },
        body: JSON.stringify({ estado }),
      }
    );
  } catch {
    throw new ReportesApiError("No fue posible conectar con el servidor.");
  }

  if (!respuesta.ok) {
    throw new ReportesApiError(
      "No pudimos actualizar el reporte.",
      respuesta.status
    );
  }

  return respuesta.json();
}

export async function enviarReporte(
  accessToken: string,
  tipoObjeto: TipoObjetoReporte,
  objetoId: number,
  motivo: string,
  detalle?: string
): Promise<void> {
  let respuesta: Response;

  try {
    respuesta = await fetch(`${API_BASE_URL}/api/usuario/reportes`, {
      method: "POST",
      headers: {
        "Authorization": `Bearer ${accessToken}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        tipoObjeto,
        objetoId,
        motivo,
        ...(detalle?.trim() ? { detalle: detalle.trim() } : {}),
      }),
    });
  } catch {
    throw new ReportesApiError("No fue posible conectar con el servidor.");
  }

  if (!respuesta.ok) {
    throw new ReportesApiError(
      "No pudimos enviar el reporte. Probá nuevamente.",
      respuesta.status
    );
  }
}
