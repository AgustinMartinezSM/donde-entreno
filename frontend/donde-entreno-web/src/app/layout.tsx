import type { Metadata } from "next";
import { Geist, Geist_Mono } from "next/font/google";
import "./globals.css";
import { ScrollToTopButton } from "../components/layout/ScrollToTopButton";
import { Footer } from "../components/layout/Footer";

const geistSans = Geist({
  variable: "--font-geist-sans",
  subsets: ["latin"],
});

const geistMono = Geist_Mono({
  variable: "--font-geist-mono",
  subsets: ["latin"],
});

export const metadata: Metadata = {
  /*
    Metadata general del sitio.
    Next.js la usa para armar el <title>, la descripción y datos básicos
    que pueden leer buscadores o redes sociales.
  */
  title: {
    default: "DondeEntreno",
    template: "%s | DondeEntreno",
  },
  description:
    "Encontrá deportes, clubes, profesores, gimnasios y actividades deportivas cerca tuyo.",
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
      "Guía deportiva local para encontrar clubes, profesores, gimnasios y actividades deportivas cerca tuyo.",
    type: "website",
    locale: "es_AR",
    siteName: "DondeEntreno",
  },
  twitter: {
    card: "summary",
    title: "DondeEntreno",
    description:
      "Encontrá deportes, clubes, profesores, gimnasios y actividades deportivas cerca tuyo.",
  },
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html
      lang="es"
      className={`${geistSans.variable} ${geistMono.variable} h-full antialiased`}
    >
      <body className="min-h-full flex flex-col">
        {children}
        <Footer />
        <ScrollToTopButton />
      </body>
    </html>
  );
}
