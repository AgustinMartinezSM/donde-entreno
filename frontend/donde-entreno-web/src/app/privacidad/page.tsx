import type { Metadata } from "next";
import Link from "next/link";

import { PaginaLegal, SeccionLegal } from "../../components/legal/PaginaLegal";

export const metadata: Metadata = {
  title: "Privacidad",
  description:
    "Cómo maneja DondeEntreno tus datos: qué pedimos, qué mostramos públicamente, qué no hacemos nunca, y cómo pedir la baja.",
  alternates: { canonical: "/privacidad" },
};

export default function PrivacidadPage() {
  return (
    <PaginaLegal
      eyebrow="Legal"
      titulo="Privacidad"
      descripcion="Los datos que pedimos son los mínimos para que la app funcione, y lo social público es siempre agregado y anónimo."
    >
      <SeccionLegal titulo="Qué datos pedimos">
        <p>
          Para crear una cuenta: nombre, email y contraseña. Para mejorar las
          recomendaciones: tu ciudad y tus deportes preferidos. Los
          publicadores además cargan los datos de contacto público de su
          actividad (WhatsApp, email, redes).
        </p>
      </SeccionLegal>

      <SeccionLegal titulo="Qué se ve públicamente">
        <p>
          Los contadores sociales (&ldquo;N personas entrenaron acá&rdquo;,
          guardados, me gusta) son agregados y anónimos: no mostramos quién, salvo que una
          función lo indique explícitamente y vos lo hayas elegido. Tu perfil,
          tus guardados y tus deportes son privados.
        </p>
        <p>
          Las funciones de cercanía, cuando existan, usan tu ubicación solo
          en el momento de la consulta y no la guardan.
        </p>
      </SeccionLegal>

      <SeccionLegal titulo="Qué no hacemos">
        <p>
          No vendemos datos personales. No publicamos tu ubicación. No usamos
          tu email para nada distinto de la operación de tu cuenta. Los
          mensajes que escribís al asistente no se registran: solo metadata
          mínima para que funcione.
        </p>
      </SeccionLegal>

      <SeccionLegal titulo="Baja y contacto">
        <p>
          Podés pedir la baja de tu cuenta y tus datos escribiéndonos a{" "}
          <a
            className="font-bold text-[var(--color-secondary)] underline-offset-2 hover:underline"
            href="mailto:dondeentrenoapp@gmail.com"
          >
            dondeentrenoapp@gmail.com
          </a>
          . Ver también las{" "}
          <Link
            className="font-bold text-[var(--color-secondary)] underline-offset-2 hover:underline"
            href="/normas"
          >
            normas de comunidad
          </Link>{" "}
          y los{" "}
          <Link
            className="font-bold text-[var(--color-secondary)] underline-offset-2 hover:underline"
            href="/terminos"
          >
            términos de uso
          </Link>
          .
        </p>
      </SeccionLegal>
    </PaginaLegal>
  );
}
