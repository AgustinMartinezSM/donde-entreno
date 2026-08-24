import type { Metadata } from "next";
import Link from "next/link";

import { PaginaLegal, SeccionLegal } from "../../components/legal/PaginaLegal";

export const metadata: Metadata = {
  title: "Términos de uso",
  description:
    "Condiciones de uso de DondeEntreno: qué ofrece la plataforma, responsabilidades de usuarios y publicadores, y uso responsable de las actividades deportivas.",
  alternates: { canonical: "/terminos" },
};

export default function TerminosPage() {
  return (
    <PaginaLegal
      eyebrow="Legal"
      titulo="Términos de uso"
      descripcion="Condiciones básicas para usar DondeEntreno. No es un contrato ilegible: es lo que podés esperar de la plataforma y lo que la plataforma espera de vos."
    >
      <SeccionLegal titulo="El servicio">
        <p>
          DondeEntreno es una plataforma de descubrimiento de actividades
          deportivas locales. Conectamos personas que quieren entrenar con
          clubes, gimnasios, profesores e instituciones que publican sus
          actividades. DondeEntreno no presta las actividades, no cobra por
          ellas y no es parte de la relación entre el usuario y el publicador.
        </p>
      </SeccionLegal>

      <SeccionLegal titulo="Tu cuenta">
        <p>
          La cuenta es personal: no compartas tu contraseña. Sos responsable
          de la actividad realizada con tu cuenta. Podés pedir la baja de tu
          cuenta y de tus datos escribiéndonos.
        </p>
      </SeccionLegal>

      <SeccionLegal titulo="Contenido y publicadores">
        <p>
          El publicador es responsable de la veracidad y actualización de la
          información que publica (horarios, precios, direcciones, fotos) y
          de contar con los derechos del contenido que sube. DondeEntreno
          puede revisar, ocultar, eliminar o pausar contenido y actividades
          que incumplan las{" "}
          <Link
            className="font-bold text-[var(--color-secondary)] underline-offset-2 hover:underline"
            href="/normas"
          >
            normas de comunidad
          </Link>
          .
        </p>
      </SeccionLegal>

      <SeccionLegal titulo="Uso responsable y actividad física">
        <p>
          La práctica deportiva es bajo tu propia responsabilidad. Verificá
          las condiciones, requisitos, aptitud física y seguros de cada
          actividad directamente con el publicador antes de asistir.
          DondeEntreno no garantiza las condiciones de las actividades
          publicadas.
        </p>
      </SeccionLegal>

      <SeccionLegal titulo="Cambios">
        <p>
          El servicio y estos términos pueden evolucionar; los cambios
          importantes se anuncian en la app. Ver también la{" "}
          <Link
            className="font-bold text-[var(--color-secondary)] underline-offset-2 hover:underline"
            href="/privacidad"
          >
            política de privacidad
          </Link>
          . Contacto:{" "}
          <a
            className="font-bold text-[var(--color-secondary)] underline-offset-2 hover:underline"
            href="mailto:dondeentrenoapp@gmail.com"
          >
            dondeentrenoapp@gmail.com
          </a>
          .
        </p>
      </SeccionLegal>
    </PaginaLegal>
  );
}
