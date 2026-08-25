"use client";

import { PublicadorGuard } from "../../../components/auth/PublicadorGuard";
import { CanalDeNovedades } from "../../../components/publicador/CanalDeNovedades";

export default function CanalDeNovedadesPage() {
  return (
    <PublicadorGuard>
      <CanalDeNovedades />
    </PublicadorGuard>
  );
}
