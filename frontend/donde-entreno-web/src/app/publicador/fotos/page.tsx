"use client";

import { PublicadorGuard } from "../../../components/auth/PublicadorGuard";
import { CentroDeFotos } from "../../../components/publicador/CentroDeFotos";

export default function CentroDeFotosPage() {
  return (
    <PublicadorGuard>
      <CentroDeFotos />
    </PublicadorGuard>
  );
}
