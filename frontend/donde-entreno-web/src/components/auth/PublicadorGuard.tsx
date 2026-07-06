"use client";

import { RoleGuard } from "./RoleGuard";
import type { ReactNode } from "react";

type PublicadorGuardProps = {
  children: ReactNode;
  returnTo?: string;
};

export function PublicadorGuard({
  children,
  returnTo,
}: PublicadorGuardProps) {
  return (
    <RoleGuard rolesPermitidos={["PUBLICADOR"]} returnTo={returnTo}>
      {children}
    </RoleGuard>
  );
}
