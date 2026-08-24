"use client";

import { AuthGuard } from "../../components/auth/AuthGuard";
import { Header } from "../../components/layout/Header";
import { FotosGuardadas } from "../../components/favoritos/FotosGuardadas";
import { MisFavoritos } from "../../components/favoritos/MisFavoritos";

/*
  Regla de producto: los favoritos son exclusivos de usuarios con
  cuenta. AuthGuard redirige al login (con returnTo) a los visitantes
  anónimos; el proxy de rutas privadas cubre además el primer request.
*/
export default function FavoritosPage() {
  return (
    <AuthGuard>
      <main className="min-h-screen text-[var(--color-text)]">
        <section className="mx-auto w-full max-w-6xl px-4 py-6">
          <Header />

          <div className="py-8 sm:py-10">
            <MisFavoritos />
            {/* Fotos guardadas (fase 4 social): solo aparece si hay alguna. */}
            <FotosGuardadas />
          </div>
        </section>
      </main>
    </AuthGuard>
  );
}
