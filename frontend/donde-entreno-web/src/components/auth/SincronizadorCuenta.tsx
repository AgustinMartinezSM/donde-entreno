"use client";

import { useEffect, useRef } from "react";
import { sincronizarConCuenta } from "../../lib/sincronizacionCuenta";
import { useAuthSession } from "./AuthSessionProvider";

/*
  Dispara la sincronización de favoritos y deportes cuando la sesión
  queda autenticada. Vive como componente aparte (montado en el layout,
  adentro del provider) para no mezclar la orquestación de datos con la
  de la sesión.

  Una sola corrida por cuenta y por carga de página: el ref evita que
  cada renovación silenciosa del access token (cada 50 minutos) dispare
  otra sincronización completa.
*/
export function SincronizadorCuenta() {
  const { status, accessToken, sesion } = useAuthSession();
  const cuentaSincronizada = useRef<number | null>(null);

  useEffect(() => {
    if (status !== "authenticated" || !accessToken || !sesion) {
      /* En logout se rearma: volver a entrar sincroniza de nuevo. */
      if (status === "guest") {
        cuentaSincronizada.current = null;
      }
      return;
    }

    const cuenta = sesion.usuario.id;

    if (cuentaSincronizada.current === cuenta) {
      return;
    }

    cuentaSincronizada.current = cuenta;
    void sincronizarConCuenta(accessToken);
  }, [status, accessToken, sesion]);

  return null;
}
