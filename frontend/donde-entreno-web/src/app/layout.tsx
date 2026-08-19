import type { Metadata, Viewport } from "next";
import { Inter, Sora } from "next/font/google";
import "./globals.css";
import { AsistenteWidget } from "../components/asistente/AsistenteWidget";
import { AuthSessionProvider } from "../components/auth/AuthSessionProvider";
import { SincronizadorCuenta } from "../components/auth/SincronizadorCuenta";
import { ScrollToTopButton } from "../components/layout/ScrollToTopButton";
import { Footer } from "../components/layout/Footer";
import { MobileNavigation } from "../components/layout/MobileNavigation";
import { SincronizadorTema } from "../components/tema/SincronizadorTema";
import { SCRIPT_TEMA_INICIAL } from "../lib/preferenciaTema";
import { SITE_URL } from "../lib/siteConfig";

const inter = Inter({
  subsets: ["latin"],
  weight: ["400", "500", "600", "700", "800"],
  display: "swap",
  variable: "--font-sans",
});

const sora = Sora({
  subsets: ["latin"],
  weight: ["600", "700", "800"],
  display: "swap",
  variable: "--font-display",
});

/*
  viewport-fit=cover es necesario para que env(safe-area-inset-bottom)
  tenga valor real en iPhone: sin esto, todos los ajustes de safe area
  de la navegación inferior y el asistente evalúan a 0.
*/
export const viewport: Viewport = {
  width: "device-width",
  initialScale: 1,
  viewportFit: "cover",
};

export const metadata: Metadata = {
  /*
    Metadata general del sitio.
    Next.js la usa para armar el <title>, la descripción y datos básicos
    que pueden leer buscadores o redes sociales.
  */
  metadataBase: new URL(SITE_URL),
  title: {
    default: "DondeEntreno",
    template: "%s | DondeEntreno",
  },
  description:
    "Descubrí deportes, clubes, profesores y actividades cerca tuyo, guardá tus favoritas y conectate con tu comunidad deportiva local.",
    icons: {
        icon: "/brand/favicon.png",
        shortcut: "/brand/favicon.png",
        apple: "/brand/favicon.png",
      },
  keywords: [
    "DondeEntreno",
    "deportes",
    "clubes",
    "gimnasios",
    "profesores deportivos",
    "actividades deportivas",
    "entrenamiento",
    "Mar del Plata",
  ],
  authors: [{ name: "DondeEntreno" }],
  creator: "DondeEntreno",
  openGraph: {
    title: "DondeEntreno",
    description:
      "Tu comunidad deportiva local para descubrir clubes, profesores, espacios y actividades cerca tuyo.",
    type: "website",
    locale: "es_AR",
    siteName: "DondeEntreno",
  },
  twitter: {
    card: "summary",
    title: "DondeEntreno",
    description:
      "Descubrí dónde entrenar y conectate con la comunidad deportiva de tu ciudad.",
  },
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    /*
      suppressHydrationWarning: el script anti-FOUC pone data-theme en
      <html> antes de que React hidrate, así que el atributo difiere del
      HTML del servidor a propósito.
    */
    <html
      lang="es"
      suppressHydrationWarning
      className={`${inter.variable} ${sora.variable} h-full antialiased`}
    >
      <body className="min-h-full flex flex-col">
        {/*
          Primero de todo el body y bloqueante a propósito: resuelve la
          preferencia de tema (sistema/claro/oscuro) y marca <html>
          ANTES de que se pinte nada — sin esto, cada visita en oscuro
          arrancaría con un flash claro.
        */}
        <script dangerouslySetInnerHTML={{ __html: SCRIPT_TEMA_INICIAL }} />

        <AuthSessionProvider>
          {/* Sincroniza favoritos y deportes con la cuenta al loguearse. */}
          <SincronizadorCuenta />
          {/* Mantiene el tema al día si cambia el sistema u otra pestaña. */}
          <SincronizadorTema />
          {children}
          <Footer />
          <MobileNavigation />
          <ScrollToTopButton />
          <AsistenteWidget />
        </AuthSessionProvider>
      </body>
    </html>
  );
}
