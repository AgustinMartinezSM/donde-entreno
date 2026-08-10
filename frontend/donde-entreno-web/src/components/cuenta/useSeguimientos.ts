"use client";

import { useCallback, useEffect, useMemo, useState } from "react";

import {
  dejarDeSeguirPublicador,
  listarPublicadoresSeguidos,
  seguirPublicador,
} from "../../services/seguimientoService";
import type { PublicadorSeguido } from "../../types/seguimiento";

export type Seguimientos = {
  seguidos: PublicadorSeguido[] | null;
  error: boolean;
  cargando: boolean;
  /* Ids con "dejar de seguir" aplicado en esta visita (permite deshacer). */
  idsNoSeguidos: number[];
  idProcesando: number | null;
  /* Cuántos sigue ahora mismo, ya descontando los que soltó en esta visita. */
  cantidad: number | null;
  alternar: (publicador: PublicadorSeguido) => Promise<void>;
};

/*
  Estado compartido de "a quién sigo" para toda la página de mi perfil.

  Vive acá y no dentro del listado porque la cabecera muestra el contador
  y el listado muestra las filas: si cada uno pidiera lo suyo, la misma
  vista haría dos veces el mismo request y el contador se quedaría
  clavado cuando alguien deja de seguir desde el listado.
*/
export function useSeguimientos(accessToken: string | null): Seguimientos {
  const [seguidos, setSeguidos] = useState<PublicadorSeguido[] | null>(null);
  const [error, setError] = useState(false);
  const [idsNoSeguidos, setIdsNoSeguidos] = useState<number[]>([]);
  const [idProcesando, setIdProcesando] = useState<number | null>(null);

  useEffect(() => {
    let activo = true;

    if (!accessToken) {
      return () => {
        activo = false;
      };
    }

    listarPublicadoresSeguidos(accessToken)
      .then((lista) => {
        if (activo) {
          setSeguidos(lista);
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

  const alternar = useCallback(
    async (publicador: PublicadorSeguido) => {
      if (!accessToken || idProcesando !== null) {
        return;
      }

      const dejaba = !idsNoSeguidos.includes(publicador.perfilPublicadorId);
      setIdProcesando(publicador.perfilPublicadorId);
      setIdsNoSeguidos((ids) =>
        dejaba
          ? [...ids, publicador.perfilPublicadorId]
          : ids.filter((id) => id !== publicador.perfilPublicadorId)
      );

      try {
        if (dejaba) {
          await dejarDeSeguirPublicador(
            publicador.perfilPublicadorId,
            accessToken
          );
        } else {
          await seguirPublicador(publicador.perfilPublicadorId, accessToken);
        }
      } catch {
        /* Revertimos el cambio optimista si la API falla. */
        setIdsNoSeguidos((ids) =>
          dejaba
            ? ids.filter((id) => id !== publicador.perfilPublicadorId)
            : [...ids, publicador.perfilPublicadorId]
        );
      } finally {
        setIdProcesando(null);
      }
    },
    [accessToken, idProcesando, idsNoSeguidos]
  );

  return useMemo(
    () => ({
      seguidos,
      error,
      cargando: !error && seguidos === null,
      idsNoSeguidos,
      idProcesando,
      /* null mientras no sabemos: mejor un guion que un cero falso. */
      cantidad:
        seguidos === null
          ? null
          : Math.max(0, seguidos.length - idsNoSeguidos.length),
      alternar,
    }),
    [seguidos, error, idsNoSeguidos, idProcesando, alternar]
  );
}
