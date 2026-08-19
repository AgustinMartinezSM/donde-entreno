"use client";

import { useEffect } from "react";

import { aplicarTemaResuelto, suscribirseATema } from "../../lib/preferenciaTema";

/*
  Mantiene el data-theme de <html> al día DESPUÉS del primer paint (del
  primer paint se ocupa el script inline de layout.tsx): si cambia la
  preferencia en otra pestaña o el sistema pasa a oscuro con la
  preferencia en "Sistema", el tema acompaña sin recargar.
*/
export function SincronizadorTema() {
  useEffect(() => {
    return suscribirseATema(aplicarTemaResuelto);
  }, []);

  return null;
}
