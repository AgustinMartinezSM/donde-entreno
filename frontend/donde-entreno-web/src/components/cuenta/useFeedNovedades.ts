"use client";

import { useEffect, useState } from "react";

import { obtenerFeedActividades } from "../../services/seguimientoService";
import type { ActividadFeed } from "../../types/seguimiento";

export type FeedNovedades = {
  novedades: ActividadFeed[] | null;
  error: boolean;
  cargando: boolean;
};

/*
  Últimas actividades de los publicadores que sigue el usuario.

  Vive como hook y no dentro del listado porque "Para vos" necesita
  saber si hay novedades antes de decidir qué mostrar: con novedades
  abre por ellas, y sin novedades abre por a quién seguir.
*/
export function useFeedNovedades(accessToken: string | null): FeedNovedades {
  const [novedades, setNovedades] = useState<ActividadFeed[] | null>(null);
  const [error, setError] = useState(false);

  useEffect(() => {
    let activo = true;

    if (!accessToken) {
      return () => {
        activo = false;
      };
    }

    obtenerFeedActividades(accessToken)
      .then((lista) => {
        if (activo) {
          setNovedades(lista);
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

  return {
    novedades,
    error,
    cargando: !error && novedades === null,
  };
}
