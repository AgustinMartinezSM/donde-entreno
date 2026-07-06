import type { Metadata } from "next";
import { RegisterChoice } from "../../components/auth/RegisterChoice";

export const metadata: Metadata = {
  title: "Crear cuenta",
  description:
    "Elegí cómo querés usar DondeEntreno: encontrar actividades o publicar propuestas deportivas.",
};

export default function RegistroPage() {
  return <RegisterChoice />;
}
