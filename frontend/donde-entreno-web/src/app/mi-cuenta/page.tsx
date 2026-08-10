"use client";

import { useState, useSyncExternalStore } from "react";
import { useRouter } from "next/navigation";

import { AuthGuard } from "../../components/auth/AuthGuard";
import { useAuthSession } from "../../components/auth/AuthSessionProvider";
import { Header } from "../../components/layout/Header";
import { esRolAdmin, esRolPublicador } from "../../lib/authRedirects";
import { leerSlugCiudadGuardada } from "../../lib/ciudadActiva";
import { useFavoritos } from "../../lib/favoritos";
import { useDeportesFavoritos } from "../../lib/preferenciasDeportivas";
import { AppButton } from "../../components/ui/AppButton";
import { AppLinkButton } from "../../components/ui/AppLinkButton";
import { StatusMessage } from "../../components/ui/StatusMessage";
import { SurfaceCard } from "../../components/ui/SurfaceCard";
import { FeedNovedades } from "../../components/cuenta/FeedNovedades";
import { PreferenciasDeportivas } from "../../components/cuenta/PreferenciasDeportivas";
import { PublicadoresSeguidos } from "../../components/cuenta/PublicadoresSeguidos";
import { useSeguimientos } from "../../components/cuenta/useSeguimientos";
import { MisFavoritos } from "../../components/favoritos/MisFavoritos";

export default function MiCuentaPage() {
  return (
    <AuthGuard>
      <MiCuentaContenido />
    </AuthGuard>
  );
}

const TABS = [
  { clave: "novedades", etiqueta: "Novedades" },
  { clave: "guardados", etiqueta: "Guardados" },
  { clave: "siguiendo", etiqueta: "Siguiendo" },
  { clave: "ajustes", etiqueta: "Ajustes" },
] as const;

type ClaveTab = (typeof TABS)[number]["clave"];

/*
  Mi perfil: el espacio del usuario dentro de la app social.

  Antes era una pila vertical de cinco tarjetas (novedades, seguidos,
  guardados, preferencias y datos), todas visibles a la vez: en mobile
  eran varias pantallas de scroll y nada quedaba priorizado. Ahora sigue
  el mismo lenguaje que el perfil público del publicador — cabecera con
  identidad y números reales, y solapas — para que las dos caras de la
  plataforma se lean igual.
*/
function MiCuentaContenido() {
  const router = useRouter();
  const { sesion, usuario, accessToken, cerrarSesion } = useAuthSession();
  const [tabActiva, setTabActiva] = useState<ClaveTab>("novedades");

  const usuarioVisible = usuario ?? sesion?.usuario ?? null;
  const rolActual = usuarioVisible?.rol ?? null;
  const nombre = usuarioVisible?.nombre?.trim() || "";
  const apellido = usuarioVisible?.apellido?.trim() || "";
  const nombreCompleto = [nombre, apellido].filter(Boolean).join(" ");

  const favoritos = useFavoritos();
  const deportesFavoritos = useDeportesFavoritos();
  const seguimientos = useSeguimientos(accessToken ?? null);

  /*
    La ciudad activa vive en localStorage: se lee recién después de
    hidratar (snapshot de servidor null) para no desincronizar el HTML
    de SSR con el primer render del cliente.
  */
  const slugCiudad = useSyncExternalStore(
    suscripcionVacia,
    leerSlugCiudadGuardadaSinFallar,
    () => null
  );
  const ciudadActiva = formatearSlugCiudad(slugCiudad);

  function manejarCerrarSesion() {
    cerrarSesion();
    router.replace("/");
  }

  return (
    <main className="min-h-screen bg-[var(--color-bg)] text-[var(--color-text)]">
      <section className="mx-auto w-full max-w-6xl px-4 py-6">
        <Header />

        <div className="py-7 sm:py-9">
          {/* Cabecera del perfil: misma estructura que /publicadores/[id] */}
          <SurfaceCard as="article" className="overflow-hidden">
            <div className="relative h-20 sm:h-24">
              <div
                aria-hidden="true"
                className="absolute inset-0 bg-gradient-to-br from-[#0F3D5E] via-[#145276] to-[#2EB872]"
              />
            </div>

            <div className="px-5 pb-6 sm:px-8 sm:pb-8">
              <div className="flex flex-col gap-5 sm:flex-row sm:items-end sm:justify-between sm:gap-6">
                <div className="flex min-w-0 items-end gap-4">
                  {/*
                    relative + z-10: la banda de arriba es un elemento
                    posicionado y sin esto le comía la mitad al avatar.
                  */}
                  <span
                    aria-hidden="true"
                    className="relative z-10 -mt-12 flex h-20 w-20 shrink-0 items-center justify-center rounded-full bg-[var(--color-primary)] text-xl font-extrabold tracking-[0.08em] text-white shadow-[0_10px_24px_rgba(15,61,94,0.18)] ring-4 ring-white sm:-mt-16 sm:h-28 sm:w-28 sm:text-2xl"
                  >
                    {obtenerIniciales(nombreCompleto)}
                  </span>

                  <div className="min-w-0 pb-1">
                    <p className="text-sm font-bold uppercase tracking-[0.14em] text-[var(--color-secondary)]">
                      Mi perfil deportivo
                    </p>
                    <h1 className="mt-1 truncate text-2xl font-extrabold leading-tight text-[var(--color-primary)] sm:text-3xl">
                      {nombre ? `Hola, ${nombre}` : "Hola"}
                    </h1>
                  </div>
                </div>

                <div className="grid grid-cols-2 items-stretch gap-2 sm:flex sm:shrink-0 sm:gap-3">
                  <AppLinkButton href="/explorar" size="sm">
                    Explorar
                  </AppLinkButton>
                  <AppLinkButton
                    href={obtenerHrefPrincipal(rolActual)}
                    variant="secondary"
                    size="sm"
                  >
                    {obtenerTextoAccionPrincipal(rolActual)}
                  </AppLinkButton>
                </div>
              </div>

              {/*
                Números reales, nada estimado: guardados y deportes salen
                de este dispositivo y seguidos del backend.
              */}
              <div className="mt-6 grid grid-cols-3 gap-2 sm:max-w-lg sm:gap-3">
                <EstadisticaPerfil
                  valor={favoritos.length}
                  etiqueta={
                    favoritos.length === 1 ? "guardada" : "guardadas"
                  }
                  activa={tabActiva === "guardados"}
                  onClick={() => setTabActiva("guardados")}
                />
                <EstadisticaPerfil
                  valor={seguimientos.cantidad}
                  etiqueta="siguiendo"
                  activa={tabActiva === "siguiendo"}
                  onClick={() => setTabActiva("siguiendo")}
                />
                <EstadisticaPerfil
                  valor={deportesFavoritos.length}
                  etiqueta={
                    deportesFavoritos.length === 1 ? "deporte" : "deportes"
                  }
                  activa={tabActiva === "ajustes"}
                  onClick={() => setTabActiva("ajustes")}
                />
              </div>

              {slugCiudad ? (
                <p className="mt-4 flex items-center gap-2 text-sm font-semibold text-[var(--color-muted)]">
                  <IconoUbicacion />
                  Explorando en <strong>{ciudadActiva}</strong>
                </p>
              ) : null}
            </div>
          </SurfaceCard>

          {/*
            Solapas con botones y aria-current, no con roles de tablist:
            no implementamos navegación por flechas, así que prometer la
            semántica de tabs sería peor que no usarla.
          */}
          {/*
            Grilla de cuatro en mobile en vez de fila con scroll: a 375px
            las cuatro solapas no entran en el ancho y "Ajustes" quedaba
            fuera de pantalla, sin nada que insinuara que había más.
          */}
          <nav
            className="mt-8 grid grid-cols-4 border-b border-[#D9E2EC] pb-px sm:flex sm:gap-2"
            aria-label="Secciones de mi perfil"
          >
            {TABS.map((tab) => {
              const activa = tab.clave === tabActiva;

              return (
                <button
                  key={tab.clave}
                  type="button"
                  onClick={() => setTabActiva(tab.clave)}
                  aria-current={activa ? "true" : undefined}
                  className={`-mb-px shrink-0 border-b-2 px-1 py-3 text-xs font-extrabold transition duration-200 ease-out sm:px-4 sm:text-sm ${
                    activa
                      ? "border-[var(--color-secondary)] text-[var(--color-primary)]"
                      : "border-transparent text-[var(--color-muted)] hover:border-[#BFDDEA] hover:text-[var(--color-primary)]"
                  }`}
                >
                  {tab.etiqueta}
                </button>
              );
            })}
          </nav>

          <div className="mt-7">
            {tabActiva === "novedades" ? <FeedNovedades /> : null}

            {tabActiva === "guardados" ? <MisFavoritos /> : null}

            {tabActiva === "siguiendo" ? (
              <PublicadoresSeguidos seguimientos={seguimientos} />
            ) : null}

            {tabActiva === "ajustes" ? (
              <div className="grid gap-5">
                <PreferenciasDeportivas />

                <SurfaceCard as="section" className="p-6 sm:p-8">
                  <h2 className="text-2xl font-extrabold text-[var(--color-primary)] sm:text-3xl">
                    Datos de mi cuenta
                  </h2>

                  {usuarioVisible ? (
                    <dl className="mt-6 grid gap-4 sm:grid-cols-2">
                      <DatoCuenta
                        etiqueta="Nombre"
                        valor={usuarioVisible.nombre}
                      />
                      <DatoCuenta
                        etiqueta="Apellido"
                        valor={usuarioVisible.apellido}
                      />
                      <DatoCuenta etiqueta="Email" valor={usuarioVisible.email} />
                      <DatoCuenta
                        etiqueta="Rol"
                        valor={formatearRol(usuarioVisible.rol)}
                      />
                    </dl>
                  ) : (
                    <StatusMessage variant="info" className="mt-6">
                      Estamos preparando los datos de tu cuenta.
                    </StatusMessage>
                  )}

                  <div className="mt-6 flex flex-col gap-3 sm:flex-row">
                    <AppLinkButton href="/ciudades" variant="outline">
                      Cambiar ciudad
                    </AppLinkButton>
                    <AppButton
                      type="button"
                      variant="secondary"
                      onClick={manejarCerrarSesion}
                    >
                      Cerrar sesión
                    </AppButton>
                  </div>
                </SurfaceCard>
              </div>
            ) : null}
          </div>
        </div>
      </section>
    </main>
  );
}

/*
  Un número del perfil. Mientras el dato no llegó mostramos un guion en
  vez de un cero: un "0 siguiendo" que después salta a 3 se lee como un
  dato, no como una carga.
*/
function EstadisticaPerfil({
  valor,
  etiqueta,
  activa,
  onClick,
}: {
  valor: number | null;
  etiqueta: string;
  activa: boolean;
  onClick: () => void;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={`rounded-[18px] border px-3 py-3 text-center transition duration-200 ease-out hover:-translate-y-0.5 ${
        activa
          ? "border-[#BDE8D0] bg-[#F6FCF8]"
          : "border-[#DDEAF3] bg-white hover:border-[#BFDDEA]"
      }`}
    >
      <span className="block text-2xl font-extrabold leading-none text-[var(--color-primary)]">
        {valor === null ? "—" : valor}
      </span>
      <span className="mt-1 block text-xs font-bold text-[var(--color-muted)]">
        {etiqueta}
      </span>
    </button>
  );
}

function DatoCuenta({ etiqueta, valor }: { etiqueta: string; valor: string }) {
  return (
    <div className="rounded-[18px] border border-[#DDEAF3] bg-white/85 p-4 shadow-[0_10px_24px_rgba(12,52,80,0.05)]">
      <dt className="text-xs font-extrabold uppercase tracking-[0.14em] text-[var(--color-secondary)]">
        {etiqueta}
      </dt>
      <dd className="mt-2 break-words text-sm font-bold text-[var(--color-primary)]">
        {valor}
      </dd>
    </div>
  );
}

function IconoUbicacion() {
  return (
    <svg
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
      className="h-4 w-4 shrink-0 text-[var(--color-accent)]"
      aria-hidden="true"
    >
      <path d="M20 10c0 5-8 11-8 11S4 15 4 10a8 8 0 1 1 16 0z" />
      <circle cx="12" cy="10" r="2.5" />
    </svg>
  );
}

function obtenerIniciales(nombre: string): string {
  const iniciales = nombre
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((parte) => parte.charAt(0).toLocaleUpperCase("es"))
    .join("");

  return iniciales || "?";
}

function formatearSlugCiudad(slug: string | null): string {
  if (!slug) {
    return "Sin elegir";
  }

  return slug
    .split("-")
    .map((parte, indice) =>
      indice === 0 || parte.length > 3
        ? parte.charAt(0).toUpperCase() + parte.slice(1)
        : parte
    )
    .join(" ");
}

function obtenerHrefPrincipal(rol: string | null): string {
  if (rol && esRolPublicador(rol)) {
    return "/publicador";
  }

  if (rol && esRolAdmin(rol)) {
    return "/admin/solicitudes";
  }

  return "/publicar";
}

function obtenerTextoAccionPrincipal(rol: string | null): string {
  if (rol && esRolPublicador(rol)) {
    return "Mi perfil publicador";
  }

  if (rol && esRolAdmin(rol)) {
    return "Administración";
  }

  return "Publicar actividad";
}

function formatearRol(rol: string): string {
  /* Los roles vienen como ROLE_USUARIO y se leían "Role Usuario". */
  return rol
    .replace(/^ROLE_/i, "")
    .toLowerCase()
    .split("_")
    .map((parte) => parte.charAt(0).toUpperCase() + parte.slice(1))
    .join(" ");
}

function suscripcionVacia() {
  return () => {};
}

function leerSlugCiudadGuardadaSinFallar(): string | null {
  try {
    return leerSlugCiudadGuardada();
  } catch {
    return null;
  }
}
