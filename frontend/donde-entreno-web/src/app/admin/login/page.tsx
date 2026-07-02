import type { Metadata } from "next";
import { AdminLoginForm } from "../../../components/admin/AdminLoginForm";

export const metadata: Metadata = {
  title: "Panel administrador",
  description: "Acceso para el equipo de DondeEntreno.",
};

export default function AdminLoginPage() {
  return (
    <main className="min-h-screen bg-gradient-to-br from-[#F8FAFC] via-white to-[#E8F6FB] px-4 py-8 text-[var(--color-text)] sm:py-12">
      <section className="mx-auto flex min-h-[calc(100vh-4rem)] w-full max-w-6xl items-center justify-center">
        <AdminLoginForm />
      </section>
    </main>
  );
}
