"use client";

import { PublicadorGuard } from "../../../../components/auth/PublicadorGuard";
import { PublicadorActividadDetail } from "../../../../components/publicador/PublicadorActividadDetail";

export default function PublicadorActividadDetallePage() {
  return (
    <PublicadorGuard>
      <PublicadorActividadDetail />
    </PublicadorGuard>
  );
}
