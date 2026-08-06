"use client";

import { PublicadorGuard } from "../../../../../components/auth/PublicadorGuard";
import { SolicitarCambiosForm } from "../../../../../components/publicador/SolicitarCambiosForm";

export default function SolicitarCambiosPage() {
  return (
    <PublicadorGuard>
      <SolicitarCambiosForm />
    </PublicadorGuard>
  );
}
