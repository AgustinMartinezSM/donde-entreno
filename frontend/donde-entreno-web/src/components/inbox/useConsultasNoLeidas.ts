"use client";

import { useEffect, useState } from "react";

import { useAuthSession } from "../auth/AuthSessionProvider";
import { esRolPublicador } from "../../lib/authRedirects";
import {
  obtenerContadorConsultasPublicador,
  obtenerContadorConsultasUsuario,
} from "../../services/inboxService";

/*
  El número del badge de consultas, por lado.

  SIN POLLING PROPIO, a propósito: cada mensaje nuevo ya genera una
  notificación, así que el aviso de "tenés algo" lo da la campanita
  —que ya consulta cada 60 s— y sumar un segundo poller global sería
  duplicar tráfico de fondo para un número que solo importa cuando
  alguien abre el menú a decidir a dónde ir.

  Por eso el contador se pide cuando el menú se ABRE (`activo`), y no
  en cada carga de página.
*/
export function useConsultasNoLeidas(activo: boolean) {
  const { status, accessToken, usuario } = useAuthSession();
  const [noLeidos, setNoLeidos] = useState<{ usuario: number; publicador: number }>({
    usuario: 0,
    publicador: 0,
  });

  const esPublicador = esRolPublicador(usuario?.rol ?? "");

  useEffect(() => {
    let componenteActivo = true;

    if (!activo || status !== "authenticated" || !accessToken) {
      return () => {
        componenteActivo = false;
      };
    }

    async function cargar() {
      const [delUsuario, delPublicador] = await Promise.all([
        obtenerContadorConsultasUsuario(accessToken as string).catch(() => 0),
        esPublicador
          ? obtenerContadorConsultasPublicador(accessToken as string).catch(() => 0)
          : Promise.resolve(0),
      ]);

      if (componenteActivo) {
        setNoLeidos({ usuario: delUsuario, publicador: delPublicador });
      }
    }

    void cargar();

    return () => {
      componenteActivo = false;
    };
  }, [activo, status, accessToken, esPublicador]);

  /** El número que corresponde a esa entrada del menú, o 0. */
  return function paraHref(href: string): number {
    if (href === "/mi-cuenta/consultas") {
      return noLeidos.usuario;
    }

    if (href === "/publicador/consultas") {
      return noLeidos.publicador;
    }

    return 0;
  };
}
