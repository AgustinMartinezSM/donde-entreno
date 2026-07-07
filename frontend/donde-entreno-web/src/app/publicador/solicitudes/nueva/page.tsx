"use client";

import { PublicadorGuard } from "../../../../components/auth/PublicadorGuard";
import { PublicadorSolicitudNueva } from "../../../../components/publicador/PublicadorSolicitudNueva";

export default function PublicadorSolicitudNuevaPage() {
  return (
    <PublicadorGuard>
      <PublicadorSolicitudNueva />
    </PublicadorGuard>
  );
}
