import { API_BASE_URL } from "../lib/apiConfig";
import type {
  BarrioPublicacionOpcion,
  CiudadPublicacionOpcion,
  DeportePublicacionOpcion,
} from "../types/catalogosPublicacion";

type ValidadorCatalogo<T> = (valor: unknown) => valor is T;
type NormalizadorCatalogo<TEntrada, TSalida> = (valor: TEntrada) => TSalida;

export class CatalogosPublicacionApiError extends Error {
  status: number | null;

  constructor(message: string, status: number | null = null) {
    super(message);
    this.name = "CatalogosPublicacionApiError";
    this.status = status;
  }
}

export async function obtenerDeportesPublicacion(): Promise<
  DeportePublicacionOpcion[]
> {
  return obtenerCatalogoPublicacion(
    `${API_BASE_URL}/api/deportes`,
    esDeportePublicacionOpcion,
    normalizarDeportePublicacion,
    "No se pudieron obtener los deportes para publicación."
  );
}

export async function obtenerCiudadesPublicacion(): Promise<
  CiudadPublicacionOpcion[]
> {
  return obtenerCatalogoPublicacion(
    `${API_BASE_URL}/api/ciudades`,
    esCiudadPublicacionOpcion,
    normalizarCiudadPublicacion,
    "No se pudieron obtener las ciudades para publicación."
  );
}

export async function obtenerBarriosPublicacion(
  ciudadId: number
): Promise<BarrioPublicacionOpcion[]> {
  if (!Number.isInteger(ciudadId) || ciudadId <= 0) {
    throw new CatalogosPublicacionApiError(
      "La ciudad seleccionada debe tener un ID entero mayor que cero."
    );
  }

  return obtenerCatalogoPublicacion(
    `${API_BASE_URL}/api/barrios?ciudadId=${ciudadId}`,
    esBarrioPublicacionOpcion,
    normalizarBarrioPublicacion,
    "No se pudieron obtener los barrios para publicación."
  );
}

async function obtenerCatalogoPublicacion<TEntrada, TSalida>(
  url: string,
  esItemValido: ValidadorCatalogo<TEntrada>,
  normalizarItem: NormalizadorCatalogo<TEntrada, TSalida>,
  mensajeErrorHttp: string
): Promise<TSalida[]> {
  let respuestaHttp: Response;

  try {
    respuestaHttp = await fetch(url, {
      method: "GET",
      headers: {
        "Accept": "application/json",
      },
      cache: "no-store",
    });
  } catch (error: unknown) {
    if (error instanceof CatalogosPublicacionApiError) {
      throw error;
    }

    throw new CatalogosPublicacionApiError(
      "No fue posible conectar con el servidor para obtener los catálogos de publicación."
    );
  }

  const cuerpo: unknown = await leerJsonSeguro(respuestaHttp);

  if (!respuestaHttp.ok) {
    throw new CatalogosPublicacionApiError(
      obtenerMensajeBackend(cuerpo) ?? mensajeErrorHttp,
      respuestaHttp.status
    );
  }

  if (!Array.isArray(cuerpo) || !cuerpo.every(esItemValido)) {
    throw new CatalogosPublicacionApiError(
      "La respuesta del servidor no tiene el formato esperado.",
      respuestaHttp.status
    );
  }

  return cuerpo.map(normalizarItem);
}

async function leerJsonSeguro(respuesta: Response): Promise<unknown> {
  try {
    const cuerpo: unknown = await respuesta.json();
    return cuerpo;
  } catch {
    return null;
  }
}

function obtenerMensajeBackend(cuerpo: unknown): string | null {
  if (!esObjeto(cuerpo) || typeof cuerpo.mensaje !== "string") {
    return null;
  }

  return cuerpo.mensaje;
}

function esObjeto(valor: unknown): valor is Record<string, unknown> {
  return typeof valor === "object" && valor !== null && !Array.isArray(valor);
}

function esDeportePublicacionOpcion(
  valor: unknown
): valor is DeportePublicacionOpcion {
  return (
    esObjeto(valor) &&
    typeof valor.id === "number" &&
    typeof valor.nombre === "string" &&
    typeof valor.slug === "string"
  );
}

function esCiudadPublicacionOpcion(
  valor: unknown
): valor is CiudadPublicacionOpcion {
  return (
    esObjeto(valor) &&
    typeof valor.id === "number" &&
    typeof valor.nombre === "string"
  );
}

function esBarrioPublicacionOpcion(
  valor: unknown
): valor is BarrioPublicacionOpcion {
  return (
    esObjeto(valor) &&
    typeof valor.id === "number" &&
    typeof valor.nombre === "string" &&
    typeof valor.ciudadId === "number" &&
    typeof valor.ciudadNombre === "string"
  );
}

function normalizarDeportePublicacion(
  deporte: DeportePublicacionOpcion
): DeportePublicacionOpcion {
  return {
    id: deporte.id,
    nombre: deporte.nombre,
    slug: deporte.slug,
  };
}

function normalizarCiudadPublicacion(
  ciudad: CiudadPublicacionOpcion
): CiudadPublicacionOpcion {
  return {
    id: ciudad.id,
    nombre: ciudad.nombre,
  };
}

function normalizarBarrioPublicacion(
  barrio: BarrioPublicacionOpcion
): BarrioPublicacionOpcion {
  return {
    id: barrio.id,
    nombre: barrio.nombre,
    ciudadId: barrio.ciudadId,
    ciudadNombre: barrio.ciudadNombre,
  };
}
