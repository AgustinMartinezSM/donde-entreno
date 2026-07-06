"use client";

import { useEffect } from "react";
import { usePathname, useRouter } from "next/navigation";
import { useAuthSession } from "./AuthSessionProvider";
import { SurfaceCard } from "../ui/SurfaceCard";
import { StatusMessage } from "../ui/StatusMessage";
import type { ReactNode } from "react";

type AuthGuardProps = {
  children: ReactNode;
  returnTo?: string;
};

export function AuthGuard({ children, returnTo }: AuthGuardProps) {
  const router = useRouter();
  const pathname = usePathname();
  const { status } = useAuthSession();
  const rutaRetorno = returnTo ?? pathname ?? "/";

  useEffect(() => {
    if (status !== "guest") {
      return;
    }

    router.replace(`/login?returnTo=${encodeURIComponent(rutaRetorno)}`);
  }, [router, rutaRetorno, status]);

  if (status === "loading") {
    return (
      <AuthGuardShell>
        <StatusMessage variant="info" role="status" title="Verificando sesión">
          Estamos confirmando tu acceso.
        </StatusMessage>
      </AuthGuardShell>
    );
  }

  if (status === "guest") {
    return (
      <AuthGuardShell>
        <StatusMessage variant="info" role="status" title="Redirigiendo">
          Te estamos llevando al inicio de sesión.
        </StatusMessage>
      </AuthGuardShell>
    );
  }

  return <>{children}</>;
}

function AuthGuardShell({ children }: { children: ReactNode }) {
  return (
    <main className="min-h-screen bg-gradient-to-br from-[#F8FAFC] via-white to-[#E8F6FB] px-4 py-8 text-[var(--color-text)]">
      <section className="mx-auto flex min-h-[calc(100vh-4rem)] w-full max-w-lg items-center justify-center">
        <SurfaceCard className="w-full p-6 text-center sm:p-7">
          {children}
        </SurfaceCard>
      </section>
    </main>
  );
}
