"use client";

import { PublicadorGuard } from "../../../components/auth/PublicadorGuard";
import { AgendaDeEventos } from "../../../components/publicador/AgendaDeEventos";

export default function AgendaDeEventosPage() {
  return (
    <PublicadorGuard>
      <AgendaDeEventos />
    </PublicadorGuard>
  );
}
