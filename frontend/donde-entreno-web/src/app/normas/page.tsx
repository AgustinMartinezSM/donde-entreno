import type { Metadata } from "next";
import Link from "next/link";

import { PaginaLegal, SeccionLegal } from "../../components/legal/PaginaLegal";

export const metadata: Metadata = {
  title: "Normas de comunidad",
  description:
    "Las normas de convivencia de DondeEntreno: qué contenido se permite, cómo funciona la moderación y las responsabilidades de usuarios y publicadores.",
  alternates: { canonical: "/normas" },
};

/*
  Normas de comunidad (Fase 1 de la etapa social). Texto base del
  draft docs/normas-comunidad-draft.md: no es texto legal definitivo,
  es la base clara que la comunidad necesita antes de abrir
  comentarios, reportes y chats.
*/
export default function NormasPage() {
  return (
    <PaginaLegal
      eyebrow="Comunidad"
      titulo="Normas de comunidad"
      descripcion="DondeEntreno es una comunidad para descubrir dónde entrenar y decidir con confianza. Estas son las reglas para que eso funcione."
    >
      <SeccionLegal titulo="Qué es DondeEntreno">
        <p>
          Una plataforma para descubrir dónde entrenar en tu ciudad:
          actividades deportivas reales, publicadas por clubes, gimnasios,
          profesores e instituciones locales, con fotos, horarios, opiniones
          y comunidad.
        </p>
      </SeccionLegal>

      <SeccionLegal titulo="Convivencia">
        <p>
          Tratá a las demás personas con respeto. No se permiten insultos,
          hostigamiento, discriminación ni ataques personales — en
          comentarios, opiniones, preguntas ni conversaciones.
        </p>
        <p>
          Hablá de deporte y de las actividades: el contenido fuera de tema,
          el spam y la publicidad no solicitada pueden ser eliminados. No
          publiques datos personales de otras personas sin su consentimiento.
        </p>
        <p>
          Las críticas honestas y respetuosas son bienvenidas. Las opiniones
          falsas, escritas para dañar o para inflar artificialmente una
          actividad, no están permitidas.
        </p>
        <p>
          Las consultas privadas son para consultar: hay un límite diario de
          conversaciones y mensajes para que la bandeja de los publicadores
          siga siendo útil. Un publicador nunca puede escribirte primero, y
          si cerrás una consulta deja de poder responderte.
        </p>
      </SeccionLegal>

      <SeccionLegal titulo="Contenido permitido y no permitido">
        <p>
          Permitido: fotos y videos de entrenamientos, instalaciones, clases
          y eventos deportivos; opiniones reales; preguntas sobre las
          actividades; avisos y novedades deportivas de los publicadores.
        </p>
        <p>
          No permitido: contenido violento, sexual o ilegal; contenido que
          exponga a menores sin autorización de sus responsables;
          suplantación de identidad; actividades falsas o engañosas; spam; y
          contenido que infrinja derechos de autor de terceros.
        </p>
      </SeccionLegal>

      <SeccionLegal titulo="Cómo funciona la moderación">
        <p>
          Las actividades nuevas pasan por revisión antes de publicarse:
          afectan el catálogo público y la confianza de la comunidad. El
          contenido social (fotos, comentarios, novedades, opiniones) se
          publica directo, sin aprobación previa: la comunidad puede
          reportarlo y DondeEntreno puede intervenirlo.
        </p>
        <p>
          DondeEntreno puede ocultar, eliminar o pausar cualquier contenido,
          actividad, perfil o conversación que incumpla estas normas, sin
          aviso previo cuando la gravedad lo amerite. Los publicadores pueden
          moderar el contenido en sus propias publicaciones.
        </p>
        <p>
          Las <strong>conversaciones privadas</strong> se moderan distinto,
          porque nadie más las ve: <strong>no las leemos</strong>. Solo
          intervenimos a partir de un reporte, y en ese caso vemos únicamente
          el mensaje reportado y hasta dos anteriores, que es el mínimo para
          entender qué pasó. Si un mensaje se oculta, en el hilo queda dicho
          que fue moderado: no reescribimos la conversación. El detalle está
          en la{" "}
          <Link
            className="font-bold text-[var(--color-secondary)] underline-offset-2 hover:underline"
            href="/privacidad"
          >
            política de privacidad
          </Link>
          .
        </p>
        <p>
          Los reportes se revisan; reportar de mala fe también incumple las
          normas. Incumplimientos reiterados pueden derivar en suspensión o
          baja de la cuenta.
        </p>
      </SeccionLegal>

      <SeccionLegal titulo="Responsabilidades del publicador">
        <p>
          La información publicada (horarios, precios, direcciones, fotos)
          debe ser veraz y estar actualizada: el publicador es responsable de
          la exactitud de su contenido. Las fotos deben ser propias o con
          derecho de uso, sin exponer a personas identificables sin su
          consentimiento — especialmente menores.
        </p>
      </SeccionLegal>

      <SeccionLegal titulo="Responsabilidades del usuario">
        <p>
          La actividad física es bajo tu propia responsabilidad: ante dudas,
          consultá a un profesional de la salud, e informate de las
          condiciones, requisitos y seguros de cada actividad antes de
          asistir. DondeEntreno conecta personas con actividades; no es el
          prestador ni garantiza sus condiciones — verificá horarios, precios
          y requisitos directamente con el publicador.
        </p>
      </SeccionLegal>

      <SeccionLegal titulo="Reportes y contacto">
        <p>
          Todo contenido social va a poder reportarse desde la propia app a
          medida que las funciones se habiliten. Mientras tanto, escribinos a{" "}
          <a
            className="font-bold text-[var(--color-secondary)] underline-offset-2 hover:underline"
            href="mailto:dondeentrenoapp@gmail.com"
          >
            dondeentrenoapp@gmail.com
          </a>{" "}
          o por Instagram{" "}
          <a
            className="font-bold text-[var(--color-secondary)] underline-offset-2 hover:underline"
            href="https://www.instagram.com/dondeentrenoapp"
            target="_blank"
            rel="noopener noreferrer"
          >
            @dondeentrenoapp
          </a>
          .
        </p>
        <p>
          Estas normas pueden actualizarse a medida que la comunidad crece.
          Ver también los{" "}
          <Link
            className="font-bold text-[var(--color-secondary)] underline-offset-2 hover:underline"
            href="/terminos"
          >
            términos de uso
          </Link>{" "}
          y la{" "}
          <Link
            className="font-bold text-[var(--color-secondary)] underline-offset-2 hover:underline"
            href="/privacidad"
          >
            política de privacidad
          </Link>
          .
        </p>
      </SeccionLegal>
    </PaginaLegal>
  );
}
