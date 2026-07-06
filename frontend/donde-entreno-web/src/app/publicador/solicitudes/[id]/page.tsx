"use client";

import { PublicadorGuard } from "../../../../components/auth/PublicadorGuard";
import { PublicadorSolicitudDetail } from "../../../../components/publicador/PublicadorSolicitudDetail";

export default function PublicadorSolicitudDetallePage() {
  return (
    <PublicadorGuard>
      <PublicadorSolicitudDetail />
    </PublicadorGuard>
  );
}
