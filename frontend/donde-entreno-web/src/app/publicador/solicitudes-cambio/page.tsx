"use client";

import { PublicadorGuard } from "../../../components/auth/PublicadorGuard";
import { SolicitudesCambioList } from "../../../components/publicador/SolicitudesCambioList";

export default function SolicitudesCambioPage() {
  return (
    <PublicadorGuard>
      <SolicitudesCambioList />
    </PublicadorGuard>
  );
}
