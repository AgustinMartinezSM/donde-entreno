import type { Metadata } from "next";
import { redirect } from "next/navigation";

export const metadata: Metadata = {
  title: "Panel administrador",
  description: "Acceso para el equipo de DondeEntreno.",
};

export default function AdminLoginPage() {
  redirect(`/login?returnTo=${encodeURIComponent("/admin/solicitudes")}`);
}
