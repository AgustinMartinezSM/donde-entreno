"use client";

import { PublicadorGuard } from "../../../components/auth/PublicadorGuard";
import { MiPerfilEditor } from "../../../components/publicador/MiPerfilEditor";

export default function PublicadorPerfilPage() {
  return (
    <PublicadorGuard>
      <MiPerfilEditor />
    </PublicadorGuard>
  );
}
