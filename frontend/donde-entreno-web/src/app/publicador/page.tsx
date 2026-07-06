"use client";

import { PublicadorGuard } from "../../components/auth/PublicadorGuard";
import { PublicadorDashboard } from "../../components/publicador/PublicadorDashboard";

export default function PublicadorPage() {
  return (
    <PublicadorGuard>
      <PublicadorDashboard />
    </PublicadorGuard>
  );
}
