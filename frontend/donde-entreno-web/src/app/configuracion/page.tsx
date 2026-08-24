"use client";

import { AuthGuard } from "../../components/auth/AuthGuard";
import { CentroConfiguracion } from "../../components/configuracion/CentroConfiguracion";

/*
  Centro de Configuración (Fase 1 de la etapa social). Detrás de
  sesión: acá se administra la cuenta. El guard del proxy ya corta el
  HTML a visitantes; AuthGuard cubre el lado cliente.
*/
export default function ConfiguracionPage() {
  return (
    <AuthGuard>
      <CentroConfiguracion />
    </AuthGuard>
  );
}
