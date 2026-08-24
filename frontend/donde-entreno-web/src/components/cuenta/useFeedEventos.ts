"use client";

import { useCallback, useEffect, useState } from "react";

import { obtenerFeedEventos } from "../../services/seguimientoService";
import type { FeedEvento } from "../../types/seguimiento";

const TAMANIO_PAGINA = 10;

export type FeedEventosPaginado = {
  eventos: FeedEvento[] | null;
  error: boolean;
  cargando: boolean;
  cargandoMas: boolean;
  hayMas: boolean;
  cargarMas: () => void;
};

/*
  Feed de hechos de los publicadores seguidos, PAGINADO (Fase 6).

  A diferencia del patrón de paginación del panel —que REEMPLAZA la
  página— acá se ACUMULA: el feed se lee hacia abajo, no se navega.

  Botón "Ver más" y no scroll infinito a propósito: el proyecto no usa
  scroll infinito en ningún lado, y con este volumen agrega
  complejidad (observer, foco, restaurar posición) sin beneficio.
*/
export function useFeedEventos(accessToken: string | null): FeedEventosPaginado {
  const [eventos, setEventos] = useState<FeedEvento[] | null>(null);
  const [error, setError] = useState(false);
  const [pagina, setPagina] = useState(0);
  const [hayMas, setHayMas] = useState(false);
  const [cargandoMas, setCargandoMas] = useState(false);

  useEffect(() => {
    let activo = true;

    if (!accessToken) {
      return () => {
        activo = false;
      };
    }

    obtenerFeedEventos(accessToken, 0, TAMANIO_PAGINA)
      .then((paginaFeed) => {
        if (activo) {
          setEventos(paginaFeed.contenido);
          setHayMas(!paginaFeed.ultima);
          setPagina(0);
          setError(false);
        }
      })
      .catch(() => {
        if (activo) {
          setError(true);
        }
      });

    return () => {
      activo = false;
    };
  }, [accessToken]);

  const cargarMas = useCallback(() => {
    if (!accessToken || cargandoMas || !hayMas) {
      return;
    }

    setCargandoMas(true);
    const siguiente = pagina + 1;

    obtenerFeedEventos(accessToken, siguiente, TAMANIO_PAGINA)
      .then((paginaFeed) => {
        /*
          Se acumula filtrando por id: si un evento nuevo entró entre
          dos pedidos, el corrimiento podría repetir uno.
        */
        setEventos((actuales) => {
          const previos = actuales ?? [];
          const vistos = new Set(previos.map((evento) => evento.id));
          return [
            ...previos,
            ...paginaFeed.contenido.filter((evento) => !vistos.has(evento.id)),
          ];
        });
        setHayMas(!paginaFeed.ultima);
        setPagina(siguiente);
      })
      .catch(() => {
        /* El "Ver más" que falla no rompe lo ya cargado. */
        setHayMas(false);
      })
      .finally(() => {
        setCargandoMas(false);
      });
  }, [accessToken, cargandoMas, hayMas, pagina]);

  return {
    eventos,
    error,
    cargando: !error && eventos === null,
    cargandoMas,
    hayMas,
    cargarMas,
  };
}
