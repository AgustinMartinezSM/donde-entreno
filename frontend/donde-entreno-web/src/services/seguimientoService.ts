import { API_BASE_URL } from "../lib/apiConfig";
import {
  construirAuthorization,
  ejecutarRequestJson,
  esErrorResponseApi,
  esObjeto,
  esStringONull,
} from "./apiHelpers";
import type {
  ActividadFeed,
  EstadoSeguimiento,
  PaginaFeedEventos,
  PublicadorSeguido,
} from "../types/seguimiento";

const BASE = `${API_BASE_URL}/api/usuario/seguimientos/publicadores`;

export class SeguimientoApiError extends Error {
  status: number | null;

  constructor(message: string, status: number | null = null) {
    super(message);
    this.name = "SeguimientoApiError";
    this.status = status;
  }
}

export async function obtenerEstadoSeguimiento(
  perfilPublicadorId: number,
  accessToken: string
): Promise<EstadoSeguimiento> {
  return ejecutar(
    `${BASE}/${encodeURIComponent(String(perfilPublicadorId))}/estado`,
    { method: "GET" },
    accessToken,
    esEstadoSeguimiento
  );
}

export async function seguirPublicador(
  perfilPublicadorId: number,
  accessToken: string
): Promise<EstadoSeguimiento> {
  return ejecutar(
    `${BASE}/${encodeURIComponent(String(perfilPublicadorId))}`,
    { method: "POST" },
    accessToken,
    esEstadoSeguimiento
  );
}

export async function dejarDeSeguirPublicador(
  perfilPublicadorId: number,
  accessToken: string
): Promise<void> {
  await ejecutar(
    `${BASE}/${encodeURIComponent(String(perfilPublicadorId))}`,
    { method: "DELETE" },
    accessToken,
    esCuerpoVacio
  );
}

export async function listarPublicadoresSeguidos(
  accessToken: string
): Promise<PublicadorSeguido[]> {
  return ejecutar(BASE, { method: "GET" }, accessToken, esListaPublicadoresSeguidos);
}

/*
  Feed de novedades: últimas actividades publicadas de los publicadores
  que sigue el usuario (top 20, más recientes primero).
*/
export async function obtenerFeedActividades(
  accessToken: string
): Promise<ActividadFeed[]> {
  return ejecutar(
    `${API_BASE_URL}/api/usuario/feed/actividades`,
    { method: "GET" },
    accessToken,
    esListaActividadesFeed
  );
}

/*
  Feed V2 (Fase 6): hechos de los publicadores seguidos, PAGINADO y
  multi-tipo. El endpoint viejo (arriba) sigue existiendo hasta que
  este esté desplegado en todos lados.
*/
export async function obtenerFeedEventos(
  accessToken: string,
  page: number,
  size: number
): Promise<PaginaFeedEventos> {
  return ejecutar(
    `${API_BASE_URL}/api/usuario/feed?page=${page}&size=${size}`,
    { method: "GET" },
    accessToken,
    esPaginaFeedEventos
  );
}

function esPaginaFeedEventos(valor: unknown): valor is PaginaFeedEventos {
  if (typeof valor !== "object" || valor === null) {
    return false;
  }

  const objeto = valor as Record<string, unknown>;

  return Array.isArray(objeto.contenido) && typeof objeto.ultima === "boolean";
}

async function ejecutar<T>(
  url: string,
  opciones: RequestInit,
  accessToken: string,
  validador: (valor: unknown) => valor is T
): Promise<T> {
  const authorization = construirAuthorization(
    accessToken,
    () => new SeguimientoApiError("Necesitás iniciar sesión.")
  );

  return ejecutarRequestJson(
    url,
    {
      ...opciones,
      headers: {
        Accept: "application/json",
        Authorization: authorization,
      },
      cache: "no-store",
    },
    validador,
    {
      crearErrorConexion: (error) =>
        error instanceof SeguimientoApiError
          ? error
          : new SeguimientoApiError("No fue posible conectar con el servidor."),
      crearErrorHttp: (status, cuerpo) => {
        const mensaje =
          esErrorResponseApi(cuerpo) && cuerpo.mensaje
            ? cuerpo.mensaje
            : mensajePorStatus(status);
        return new SeguimientoApiError(mensaje, status);
      },
      crearErrorFormatoInvalido: (status) =>
        new SeguimientoApiError(
          "La respuesta del servidor no tiene el formato esperado.",
          status
        ),
    }
  );
}

function mensajePorStatus(status: number): string {
  if (status === 401) {
    return "Tu sesión expiró o no es válida.";
  }
  if (status === 404) {
    return "No encontramos ese publicador.";
  }
  return "No se pudo completar la operación.";
}

function esEstadoSeguimiento(valor: unknown): valor is EstadoSeguimiento {
  return esObjeto(valor) && typeof valor.siguiendo === "boolean";
}

function esPublicadorSeguido(valor: unknown): valor is PublicadorSeguido {
  return (
    esObjeto(valor) &&
    typeof valor.perfilPublicadorId === "number" &&
    typeof valor.perfilPublicadorNombre === "string" &&
    esStringONull(valor.tipoPublicador) &&
    esStringONull(valor.ciudadPrincipalNombre) &&
    esStringONull(valor.seguidoDesde)
  );
}

function esListaPublicadoresSeguidos(
  valor: unknown
): valor is PublicadorSeguido[] {
  return Array.isArray(valor) && valor.every(esPublicadorSeguido);
}

/*
  Validamos solo el mínimo estructural (los campos que la UI necesita sí o
  sí); el resto del ActividadDTO es opcional en el tipo Actividad y las
  cards ya manejan su ausencia.
*/
function esActividadFeed(valor: unknown): valor is ActividadFeed {
  return (
    esObjeto(valor) &&
    typeof valor.id === "number" &&
    typeof valor.titulo === "string" &&
    typeof valor.slug === "string" &&
    esStringONull(valor.deporteNombre ?? null) &&
    esStringONull(valor.ciudadNombre ?? null) &&
    esStringONull(valor.barrioNombre ?? null) &&
    esStringONull(valor.perfilPublicadorNombre ?? null)
  );
}

function esListaActividadesFeed(valor: unknown): valor is ActividadFeed[] {
  return Array.isArray(valor) && valor.every(esActividadFeed);
}

function esCuerpoVacio(valor: unknown): valor is null {
  return valor === null;
}
