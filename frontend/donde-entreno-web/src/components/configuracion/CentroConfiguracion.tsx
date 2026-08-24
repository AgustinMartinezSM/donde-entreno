"use client";

import { useMemo, useState } from "react";
import Link from "next/link";

import { esRolAdmin, esRolPublicador } from "../../lib/authRedirects";
import { useAuthSession } from "../auth/AuthSessionProvider";
import { DialogoDatosDeCuenta } from "../cuenta/MenuAjustes";
import { DialogoCambiarPassword } from "../cuenta/DialogoCambiarPassword";
import { Header } from "../layout/Header";
import { SelectorTema } from "../tema/SelectorTema";
import { SectionHeader } from "../ui/SectionHeader";
import { SurfaceCard } from "../ui/SurfaceCard";

/*
  Centro de Configuración V1 (Fase 1 de la etapa social).

  Separación de producto: "Mi perfil" es cómo me veo y mi contenido;
  "Configuración" es cómo administro mi cuenta y mis preferencias.
  V1 reordena lo que YA existe (nada de backend nuevo): cada opción es
  un link a donde la función vive, o abre el diálogo existente. La
  estructura de secciones ya deja lugar a las futuras (privacidad,
  notificaciones, chats) sin rediseñar.
*/

type OpcionConfiguracion = {
  id: string;
  titulo: string;
  descripcion: string;
  /* Palabras extra para el buscador ("contraseña", "tema", etc.). */
  claves?: string;
} & (
  | { tipo: "link"; href: string }
  | { tipo: "accion"; accion: "datos" | "password" | "cerrar-sesion" }
  | { tipo: "tema" }
);

type SeccionConfiguracion = {
  id: string;
  titulo: string;
  opciones: OpcionConfiguracion[];
};

export function CentroConfiguracion() {
  const { usuario, cerrarSesion } = useAuthSession();
  const [busqueda, setBusqueda] = useState("");
  const [datosAbiertos, setDatosAbiertos] = useState(false);
  const [passwordAbierto, setPasswordAbierto] = useState(false);

  const rol = usuario?.rol ?? null;

  const secciones = useMemo(() => construirSecciones(rol), [rol]);

  const busquedaLimpia = busqueda.trim().toLowerCase();
  const seccionesVisibles = busquedaLimpia
    ? secciones
        .map((seccion) => ({
          ...seccion,
          opciones: seccion.opciones.filter((opcion) =>
            `${opcion.titulo} ${opcion.descripcion} ${opcion.claves ?? ""}`
              .toLowerCase()
              .includes(busquedaLimpia)
          ),
        }))
        .filter((seccion) => seccion.opciones.length > 0)
    : secciones;

  function manejarAccion(accion: "datos" | "password" | "cerrar-sesion") {
    if (accion === "datos") {
      setDatosAbiertos(true);
    } else if (accion === "password") {
      setPasswordAbierto(true);
    } else {
      cerrarSesion();
    }
  }

  return (
    <main className="min-h-screen text-[var(--color-text)]">
      <section className="mx-auto w-full max-w-5xl px-4 py-6">
        <Header />

        <div className="mt-6">
          <SectionHeader
            eyebrow="Tu cuenta"
            title="Configuración"
            description="Administrá tu cuenta, tus preferencias y tu apariencia. Tu contenido (guardados, deportes, seguidos) vive en Mi perfil."
          />
        </div>

        <label className="mt-5 block">
          <span className="sr-only">Buscar en configuración</span>
          <input
            type="search"
            value={busqueda}
            onChange={(evento) => setBusqueda(evento.target.value)}
            placeholder="Buscar en configuración (contraseña, tema, deportes...)"
            className="min-h-12 w-full rounded-[18px] border border-[var(--color-border-accent)] bg-[var(--color-surface)] px-4 text-base text-[var(--color-text)] outline-none transition duration-200 ease-out hover:border-[var(--color-accent)] focus:border-[var(--color-accent)] focus:ring-4 focus:ring-[var(--color-border-soft)] sm:max-w-md"
          />
        </label>

        {/* Desktop: nav de anclas a la izquierda; mobile: apilado. */}
        <div className="mt-6 gap-8 lg:grid lg:grid-cols-[210px_1fr]">
          <nav
            aria-label="Secciones de configuración"
            className="mb-6 hidden lg:block"
          >
            <ul className="sticky top-6 space-y-1">
              {seccionesVisibles.map((seccion) => (
                <li key={seccion.id}>
                  <a
                    href={`#${seccion.id}`}
                    className="block rounded-[12px] px-3 py-2 text-sm font-bold text-[var(--color-muted)] transition duration-200 ease-out hover:bg-[var(--color-surface-soft)] hover:text-[var(--color-primary)]"
                  >
                    {seccion.titulo}
                  </a>
                </li>
              ))}
            </ul>
          </nav>

          <div className="space-y-6">
            {seccionesVisibles.length === 0 ? (
              <SurfaceCard className="p-6">
                <p className="text-sm text-[var(--color-muted)]">
                  No encontramos opciones para &ldquo;{busqueda.trim()}&rdquo;.
                  Probá con otra palabra, como &ldquo;contraseña&rdquo; o
                  &ldquo;tema&rdquo;.
                </p>
              </SurfaceCard>
            ) : (
              seccionesVisibles.map((seccion) => (
                <SurfaceCard
                  key={seccion.id}
                  as="section"
                  id={seccion.id}
                  className="scroll-mt-6 p-5 sm:p-6"
                >
                  <h2 className="text-sm font-extrabold uppercase tracking-[0.14em] text-[var(--color-secondary)]">
                    {seccion.titulo}
                  </h2>

                  <ul className="mt-3 divide-y divide-[var(--color-divisor)]">
                    {seccion.opciones.map((opcion) => (
                      <li key={opcion.id}>
                        {opcion.tipo === "tema" ? (
                          <div className="py-3">
                            <p className="text-sm font-bold text-[var(--color-primary)]">
                              {opcion.titulo}
                            </p>
                            <p className="mt-0.5 text-xs text-[var(--color-muted)]">
                              {opcion.descripcion}
                            </p>
                            <div className="mt-2">
                              <SelectorTema />
                            </div>
                          </div>
                        ) : opcion.tipo === "link" ? (
                          <Link
                            href={opcion.href}
                            className="group flex min-h-14 items-center gap-3 py-3 transition duration-200 ease-out"
                          >
                            <FilaOpcion
                              titulo={opcion.titulo}
                              descripcion={opcion.descripcion}
                            />
                          </Link>
                        ) : (
                          <button
                            type="button"
                            onClick={() => manejarAccion(opcion.accion)}
                            className="group flex min-h-14 w-full items-center gap-3 py-3 text-left transition duration-200 ease-out"
                          >
                            <FilaOpcion
                              titulo={opcion.titulo}
                              descripcion={opcion.descripcion}
                              peligrosa={opcion.accion === "cerrar-sesion"}
                            />
                          </button>
                        )}
                      </li>
                    ))}
                  </ul>
                </SurfaceCard>
              ))
            )}
          </div>
        </div>
      </section>

      <DialogoDatosDeCuenta
        usuario={usuario}
        abierto={datosAbiertos}
        onCerrar={() => setDatosAbiertos(false)}
      />

      <DialogoCambiarPassword
        abierto={passwordAbierto}
        onCerrar={() => setPasswordAbierto(false)}
      />
    </main>
  );
}

function FilaOpcion({
  titulo,
  descripcion,
  peligrosa = false,
}: {
  titulo: string;
  descripcion: string;
  peligrosa?: boolean;
}) {
  return (
    <>
      <span className="min-w-0 flex-1">
        <span
          className={`block text-sm font-bold ${
            peligrosa
              ? "text-red-700"
              : "text-[var(--color-primary)] group-hover:text-[var(--color-secondary)]"
          }`}
        >
          {titulo}
        </span>
        <span className="mt-0.5 block text-xs text-[var(--color-muted)]">
          {descripcion}
        </span>
      </span>
      <span
        aria-hidden="true"
        className="text-[var(--color-muted)] transition duration-200 ease-out group-hover:translate-x-0.5 group-hover:text-[var(--color-secondary)]"
      >
        ›
      </span>
    </>
  );
}

function construirSecciones(rol: string | null): SeccionConfiguracion[] {
  const secciones: SeccionConfiguracion[] = [
    {
      id: "cuenta",
      titulo: "Tu cuenta",
      opciones: [
        {
          id: "datos",
          tipo: "accion",
          accion: "datos",
          titulo: "Datos de mi cuenta",
          descripcion: "Nombre, apellido, email y rol (solo lectura por ahora).",
          claves: "email nombre",
        },
        {
          id: "avatar",
          tipo: "link",
          href: "/mi-cuenta",
          titulo: "Foto de perfil",
          descripcion: "Se cambia desde la cabecera de Mi perfil (la camarita).",
          claves: "avatar imagen foto",
        },
        {
          id: "ciudad",
          tipo: "link",
          href: "/ciudades",
          titulo: "Cambiar ciudad",
          descripcion: "La ciudad donde buscás actividades.",
        },
      ],
    },
    {
      id: "perfil-deportivo",
      titulo: "Perfil deportivo",
      opciones: [
        {
          id: "deportes",
          tipo: "link",
          href: "/mi-cuenta?tab=deportes",
          titulo: "Mis deportes",
          descripcion: "Los deportes que te interesan mueven tus recomendaciones.",
          claves: "preferencias favoritos",
        },
        {
          id: "guardados",
          tipo: "link",
          href: "/favoritos",
          titulo: "Guardados y colecciones",
          descripcion: "Tus actividades guardadas, organizadas en colecciones.",
        },
        {
          id: "siguiendo",
          tipo: "link",
          href: "/mi-cuenta?tab=siguiendo",
          titulo: "Siguiendo",
          descripcion: "Los clubes, profes y gimnasios que seguís.",
          claves: "publicadores seguir",
        },
      ],
    },
    {
      id: "apariencia",
      titulo: "Apariencia",
      opciones: [
        {
          id: "tema",
          tipo: "tema",
          titulo: "Tema",
          descripcion:
            "Sistema, claro u oscuro. Se guarda en este dispositivo.",
          claves: "modo oscuro claro dark",
        },
      ],
    },
  ];

  if (rol && esRolPublicador(rol)) {
    secciones.push({
      id: "publicador",
      titulo: "Publicador",
      opciones: [
        {
          id: "espacio-publicador",
          tipo: "link",
          href: "/publicador",
          titulo: "Mi espacio de publicador",
          descripcion: "Panel con tus actividades, solicitudes y métricas.",
        },
        {
          id: "perfil-publicador",
          tipo: "link",
          href: "/publicador/perfil",
          titulo: "Perfil público",
          descripcion: "Nombre, descripción, logo y portada que ve la gente.",
          claves: "logo portada",
        },
        {
          id: "fotos-publicador",
          tipo: "link",
          href: "/publicador/fotos",
          titulo: "Mis fotos",
          descripcion: "El centro de fotos de tus actividades.",
        },
      ],
    });
  }

  if (rol && esRolAdmin(rol)) {
    secciones.push({
      id: "administracion",
      titulo: "Administración",
      opciones: [
        {
          id: "admin-solicitudes",
          tipo: "link",
          href: "/admin/solicitudes",
          titulo: "Solicitudes de publicación",
          descripcion: "La cola de actividades nuevas para revisar.",
        },
        {
          id: "admin-cambios",
          tipo: "link",
          href: "/admin/solicitudes-cambio",
          titulo: "Solicitudes de cambio",
          descripcion: "Cambios propuestos sobre actividades publicadas.",
        },
        {
          id: "admin-imagenes",
          tipo: "link",
          href: "/admin/imagenes",
          titulo: "Moderación de imágenes",
          descripcion: "Fotos pendientes de revisión.",
        },
      ],
    });
  }

  secciones.push(
    {
      id: "seguridad",
      titulo: "Seguridad",
      opciones: [
        {
          id: "password",
          tipo: "accion",
          accion: "password",
          titulo: "Cambiar contraseña",
          descripcion: "Cerramos tus otras sesiones al cambiarla.",
          claves: "clave seguridad",
        },
        {
          id: "cerrar-sesion",
          tipo: "accion",
          accion: "cerrar-sesion",
          titulo: "Cerrar sesión",
          descripcion: "Salir de tu cuenta en este dispositivo.",
          claves: "logout salir",
        },
      ],
    },
    {
      id: "ayuda",
      titulo: "Ayuda y normas",
      opciones: [
        {
          id: "normas",
          tipo: "link",
          href: "/normas",
          titulo: "Normas de comunidad",
          descripcion: "Cómo convivimos y cómo funciona la moderación.",
        },
        {
          id: "terminos",
          tipo: "link",
          href: "/terminos",
          titulo: "Términos de uso",
          descripcion: "Las condiciones de uso de DondeEntreno.",
        },
        {
          id: "privacidad",
          tipo: "link",
          href: "/privacidad",
          titulo: "Privacidad",
          descripcion: "Qué datos pedimos y qué se ve públicamente.",
        },
        {
          id: "contacto",
          tipo: "link",
          href: "mailto:dondeentrenoapp@gmail.com",
          titulo: "Reportar un problema",
          descripcion: "Escribinos y lo miramos: dondeentrenoapp@gmail.com.",
          claves: "soporte ayuda contacto reporte",
        },
      ],
    }
  );

  return secciones;
}
