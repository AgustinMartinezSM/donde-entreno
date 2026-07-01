import type { Metadata } from "next";
import { AdminLoginForm } from "../../../components/admin/AdminLoginForm";

export const metadata: Metadata = {
  title: "Panel administrador",
  description: "Acceso para el equipo de DondeEntreno.",
};

export default function AdminLoginPage() {
  return (
    <main className="min-h-screen bg-[var(--color-bg)] px-4 py-8 text-[var(--color-text)] sm:py-12">
      <section className="mx-auto flex min-h-[calc(100vh-4rem)] w-full max-w-5xl items-center justify-center">
        <AdminLoginForm />
      </section>
    </main>
  );
}
