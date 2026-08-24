"use client";

import { useEffect, useRef } from "react";

import { registrarInteraccion } from "../../lib/interacciones";

/*
  Beacon de vista del detalle (Fase 2 social): una vez por montaje,
  anónimo y best-effort. No renderiza nada.
*/
export function RegistroVistaDetalle({ actividadId }: { actividadId: number }) {
  const registrado = useRef(false);

  useEffect(() => {
    if (registrado.current) {
      return;
    }

    registrado.current = true;
    registrarInteraccion(actividadId, "VISTA_DETALLE");
  }, [actividadId]);

  return null;
}
