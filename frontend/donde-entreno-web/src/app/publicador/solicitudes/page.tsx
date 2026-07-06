"use client";

import { PublicadorGuard } from "../../../components/auth/PublicadorGuard";
import { PublicadorSolicitudesList } from "../../../components/publicador/PublicadorSolicitudesList";

export default function PublicadorSolicitudesPage() {
  return (
    <PublicadorGuard>
      <PublicadorSolicitudesList />
    </PublicadorGuard>
  );
}
