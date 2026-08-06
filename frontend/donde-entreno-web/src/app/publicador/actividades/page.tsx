"use client";

import { PublicadorGuard } from "../../../components/auth/PublicadorGuard";
import { PublicadorActividadesList } from "../../../components/publicador/PublicadorActividadesList";

export default function PublicadorActividadesPage() {
  return (
    <PublicadorGuard>
      <PublicadorActividadesList />
    </PublicadorGuard>
  );
}
