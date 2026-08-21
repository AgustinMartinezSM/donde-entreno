"use client";

import Image from "next/image";
import Link from "next/link";
import { useEffect, useState } from "react";

import { useAuthSession } from "../auth/AuthSessionProvider";
import { AppLinkButton } from "../ui/AppLinkButton";
import { SectionHeader } from "../ui/SectionHeader";
import { StatusMessage } from "../ui/StatusMessage";
import { SurfaceCard } from "../ui/SurfaceCard";
import { PublicadorPageHeader } from "./PublicadorPageHeader";
import { construirUrlImagenBackend } from "../../lib/backendUrl";
import { obtenerImagenFallbackActividad } from "../../lib/activityImages";
import {
  PublicadorApiError,
  listarActividadesPublicador,
  listarImagenesActividad,
  listarImagenesPerfil,
  obtenerPerfilPublicador,
} from "../../services/publicadorService";
import type {
  ActividadPublicadorResumen,
  ImagenActividadPublicador,
  PerfilPublicadorActual,
} from "../../types/publicador";

/*
  Cuántas fotos aprobadas de galería consideramos "una galería que
  genera confianza" para el checklist. Tres alcanza para que el detalle
  público deje de sentirse vacío.
*/
const GALERIA_RECOMENDADA = 3;

type FotosPorActividad = Record<number, ImagenActividadPublicador[]>;

/*
  Centro de fotos del publicador (Media Center V1, fase 3 del bloque
  visual): TODO lo visual en un solo lugar — la identidad del perfil
  (logo y portada), el estado de las fotos de cada actividad y un
  checklist de presencia. No inventa datos: todo sale de los endpoints
  que ya existen, y cada fila lleva a la pantalla donde se gestiona.
*/
export function CentroDeFotos() {
  const { accessToken } = useAuthSession();

  const [perfil, setPerfil] = useState<PerfilPublicadorActual | null>(null);
  const [imagenesPerfil, setImagenesPerfil] = useState<
    ImagenActividadPublicador[]
  >([]);
  const [actividades, setActividades] = useState<ActividadPublicadorResumen[]>(
    []
  );
  const [fotosPorActividad, setFotosPorActividad] = useState<FotosPorActividad>(
    {}
  );
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let componenteActivo = true;

    if (!accessToken) {
      return () => {
        componenteActivo = false;
      };
    }

    async function cargar() {
      try {
        /*
          Las tres fuentes principales en paralelo; después, las fotos
          de cada actividad también en paralelo. Con el volumen real de
          actividades por publicador (unidades, no cientos) el fan-out
          es barato y evita un endpoint agregado nuevo.
        */
        const [perfilActual, dePerfil, paginaActividades] = await Promise.all([
          obtenerPerfilPublicador(accessToken as string),
          listarImagenesPerfil(accessToken as string),
          listarActividadesPublicador(
            { page: 0, size: 50, orden: "recientes" },
            accessToken as string
          ),
        ]);

        const listaActividades = paginaActividades.contenido;

        const fotos = await Promise.all(
          listaActividades.map((actividad) =>
            listarImagenesActividad(actividad.id, accessToken as string).catch(
              () => [] as ImagenActividadPublicador[]
            )
          )
        );

        if (!componenteActivo) {
          return;
        }

        const porActividad: FotosPorActividad = {};
        listaActividades.forEach((actividad, indice) => {
          porActividad[actividad.id] = fotos[indice];
        });

        setPerfil(perfilActual);
        setImagenesPerfil(dePerfil);
        setActividades(listaActividades);
        setFotosPorActividad(porActividad);
        setError(null);
      } catch (errorCarga: unknown) {
        if (!componenteActivo) {
          return;
        }

        setError(
          errorCarga instanceof PublicadorApiError
            ? errorCarga.message
            : "No pudimos cargar tu centro de fotos. Probá de nuevo en unos minutos."
        );
      } finally {
        if (componenteActivo) {
          setCargando(false);
        }
      }
    }

    void cargar();

    return () => {
      componenteActivo = false;
    };
  }, [accessToken]);

  /*
    La vigente de cada tipo: la más reciente que no fue rechazada ni
    eliminada (mismo criterio que la gestión del perfil).
  */
  function vigenteDe(tipo: "LOGO" | "PORTADA") {
    return imagenesPerfil.find(
      (imagen) =>
        imagen.tipoImagen === tipo &&
        imagen.estadoModeracion !== "RECHAZADA" &&
        !(imagen.estadoModeracion === "APROBADA" && !imagen.activa)
    );
  }

  const logo = vigenteDe("LOGO");
  const portada = vigenteDe("PORTADA");

  const resumenPorActividad = actividades.map((actividad) => {
    const fotos = fotosPorActividad[actividad.id] ?? [];
    const visibles = fotos.filter(
      (foto) => !(foto.estadoModeracion === "APROBADA" && !foto.activa)
    );

    return {
      actividad,
      aprobadas: visibles.filter(
        (foto) => foto.estadoModeracion === "APROBADA"
      ).length,
      pendientes: visibles.filter(
        (foto) => foto.estadoModeracion === "PENDIENTE"
      ).length,
      galeriaAprobada: visibles.filter(
        (foto) =>
          foto.tipoImagen === "GALERIA" && foto.estadoModeracion === "APROBADA"
      ).length,
      tienePrincipal: visibles.some(
        (foto) =>
          foto.tipoImagen === "PRINCIPAL" &&
          foto.estadoModeracion === "APROBADA"
      ),
    };
  });

  /*
    Checklist de presencia: solo pasos medibles con los datos que ya
    tenemos (por eso no está "horarios cargados": el resumen del panel
    no los trae y no vamos a inventar el dato).
  */
  const checklist = [
    {
      clave: "logo",
      etiqueta: "Logo cargado",
      completado: Boolean(logo),
      href: "/publicador/perfil",
    },
    {
      clave: "portada",
      etiqueta: "Portada cargada",
      completado: Boolean(portada),
      href: "/publicador/perfil",
    },
    {
      clave: "principal",
      etiqueta: "Al menos una actividad con imagen principal",
      completado: resumenPorActividad.some((item) => item.tienePrincipal),
      href: "/publicador/actividades",
    },
    {
      clave: "galeria",
      etiqueta: `Una galería con ${GALERIA_RECOMENDADA} fotos o más`,
      completado: resumenPorActividad.some(
        (item) => item.galeriaAprobada >= GALERIA_RECOMENDADA
      ),
      href: "/publicador/actividades",
    },
    {
      clave: "descripcion",
      etiqueta: "Descripción del perfil completa",
      completado: Boolean(perfil?.descripcion?.trim()),
      href: "/publicador/perfil",
    },
    {
      clave: "whatsapp",
      etiqueta: "WhatsApp de contacto cargado",
      completado: Boolean(perfil?.whatsapp?.trim()),
      href: "/publicador/perfil",
    },
  ];
  const pasosCompletados = checklist.filter((paso) => paso.completado).length;

  return (
    <main className="min-h-screen px-4 py-8 text-[var(--color-text)] sm:py-12">
      <section className="mx-auto w-full max-w-6xl">
        <PublicadorPageHeader
          eyebrow="Centro de fotos"
          title="Tus fotos, en un solo lugar"
          description="La identidad visual de tu perfil y las fotos de cada actividad, con su estado de revisión y accesos directos para gestionarlas."
          action={
            <AppLinkButton href="/publicador" variant="secondary" fullWidth>
              Volver al panel
            </AppLinkButton>
          }
        />

        {cargando ? (
          <StatusMessage variant="info" role="status" className="mt-6">
            Cargando tu centro de fotos...
          </StatusMessage>
        ) : null}

        {error ? (
          <StatusMessage variant="error" role="alert" className="mt-6">
            {error}
          </StatusMessage>
        ) : null}

        {!cargando && !error ? (
          <div className="mt-8 grid gap-8 lg:grid-cols-[1fr_20rem] lg:items-start">
            <div className="grid min-w-0 gap-8">
              {/* A. Identidad visual del perfil */}
              <SurfaceCard as="section" className="p-6 sm:p-8">
                <SectionHeader
                  eyebrow="Tu identidad"
                  title="Logo y portada"
                  description="Son la cara de tu perfil público: aparecen arriba de todo y en cada actividad que publicás."
                />

                <div className="mt-6 grid gap-4 sm:grid-cols-2">
                  <RanuraIdentidad
                    titulo="Logo"
                    imagen={logo}
                    forma="circulo"
                  />
                  <RanuraIdentidad
                    titulo="Portada"
                    imagen={portada}
                    forma="banda"
                  />
                </div>

                <AppLinkButton
                  href="/publicador/perfil"
                  variant="secondary"
                  size="sm"
                  className="mt-5"
                >
                  Gestionar logo y portada
                </AppLinkButton>
              </SurfaceCard>

              {/* B. Fotos por actividad */}
              <SurfaceCard as="section" className="p-6 sm:p-8">
                <SectionHeader
                  eyebrow="Tus actividades"
                  title="Fotos por actividad"
                  description="El estado visual de cada actividad publicada: su portada, cuántas fotos tiene y cuántas esperan revisión."
                />

                {actividades.length === 0 ? (
                  <div className="mt-6 rounded-[18px] border border-[var(--color-border-accent)] bg-[var(--color-surface-soft)] p-4 text-sm leading-6 text-[var(--color-muted)]">
                    <p className="font-extrabold text-[var(--color-primary)]">
                      Todavía no tenés actividades publicadas.
                    </p>
                    <p className="mt-1">
                      Cuando una solicitud sea aprobada, sus fotos se gestionan
                      desde acá.
                    </p>
                    <AppLinkButton
                      href="/publicador/solicitudes/nueva"
                      size="sm"
                      className="mt-3"
                    >
                      Crear una solicitud
                    </AppLinkButton>
                  </div>
                ) : (
                  <ul className="mt-6 grid gap-3">
                    {resumenPorActividad.map(
                      ({ actividad, aprobadas, pendientes, tienePrincipal }) => (
                        <FilaActividad
                          key={actividad.id}
                          actividad={actividad}
                          aprobadas={aprobadas}
                          pendientes={pendientes}
                          tienePrincipal={tienePrincipal}
                        />
                      )
                    )}
                  </ul>
                )}
              </SurfaceCard>
            </div>

            {/* C. Checklist de presencia */}
            <SurfaceCard
              as="section"
              variant="info"
              className="p-5 sm:p-6 lg:sticky lg:top-6"
            >
              <p className="text-xs font-extrabold uppercase tracking-[0.16em] text-[var(--color-info-deep)]">
                Recomendaciones
              </p>
              <h2 className="mt-1 text-lg font-extrabold text-[var(--color-primary)]">
                Tu presencia visual: {pasosCompletados} de {checklist.length}
              </h2>

              <ul className="mt-4 grid gap-2">
                {checklist.map((paso) => (
                  <li key={paso.clave}>
                    {paso.completado ? (
                      <span className="flex items-start gap-2.5 rounded-[12px] px-2 py-1.5 text-sm font-bold text-[var(--color-success)]">
                        <MarcaPaso completado />
                        {paso.etiqueta}
                      </span>
                    ) : (
                      /* Un pendiente sin camino es un reproche: cada uno linkea. */
                      <Link
                        href={paso.href}
                        className="flex items-start gap-2.5 rounded-[12px] px-2 py-1.5 text-sm font-bold text-[var(--color-primary)] transition duration-200 ease-out hover:bg-[var(--color-surface)]/60 focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-[#4FB3D9]/30"
                      >
                        <MarcaPaso />
                        {paso.etiqueta}
                      </Link>
                    )}
                  </li>
                ))}
              </ul>

              <p className="mt-5 border-t border-[var(--color-border-accent)] pt-4 text-sm leading-6 text-[var(--color-muted)]">
                Las actividades con fotos reales generan más confianza y ayudan
                a que las personas decidan dónde entrenar.
              </p>
            </SurfaceCard>
          </div>
        ) : null}
      </section>
    </main>
  );
}

/*
  Ranura de identidad (logo o portada): la vista previa con la forma
  real que tiene en el perfil público, y su estado de revisión al lado.
*/
function RanuraIdentidad({
  titulo,
  imagen,
  forma,
}: {
  titulo: string;
  imagen: ImagenActividadPublicador | undefined;
  forma: "circulo" | "banda";
}) {
  const url = imagen ? construirUrlImagenBackend(imagen.url) : null;

  return (
    <div className="rounded-[18px] border border-[var(--color-border-soft)] bg-[var(--color-bg)] p-4">
      <div className="flex items-center justify-between gap-3">
        <p className="text-sm font-extrabold text-[var(--color-primary)]">
          {titulo}
        </p>
        <EstadoImagen imagen={imagen} />
      </div>

      <div className="mt-3">
        {url ? (
          <Image
            src={url}
            alt={`${titulo} de tu perfil`}
            width={forma === "circulo" ? 96 : 384}
            height={forma === "circulo" ? 96 : 128}
            className={
              forma === "circulo"
                ? "h-24 w-24 rounded-full object-cover"
                : "h-24 w-full rounded-[12px] object-cover"
            }
          />
        ) : (
          <div
            className={`flex items-center justify-center bg-[var(--color-surface)] text-xs font-bold text-[var(--color-muted)] ${
              forma === "circulo"
                ? "h-24 w-24 rounded-full"
                : "h-24 w-full rounded-[12px]"
            }`}
          >
            Sin {titulo.toLowerCase()}
          </div>
        )}
      </div>
    </div>
  );
}

/* Una fila del listado de actividades con su estado visual. */
function FilaActividad({
  actividad,
  aprobadas,
  pendientes,
  tienePrincipal,
}: {
  actividad: ActividadPublicadorResumen;
  aprobadas: number;
  pendientes: number;
  tienePrincipal: boolean;
}) {
  const imagenBackend = construirUrlImagenBackend(
    actividad.imagenPrincipalUrl
  );
  const fallback = obtenerImagenFallbackActividad({
    deporteSlug: actividad.deporteSlug ?? undefined,
  });

  return (
    <li className="flex flex-wrap items-center gap-4 rounded-[18px] border border-[var(--color-border-soft)] bg-[var(--color-surface)]/80 p-3">
      <Image
        src={imagenBackend ?? fallback}
        alt={`Imagen principal de ${actividad.titulo}`}
        width={112}
        height={80}
        className="h-20 w-28 shrink-0 rounded-[12px] object-cover"
      />

      <div className="min-w-0 flex-1">
        <p className="truncate text-sm font-extrabold text-[var(--color-primary)]">
          {actividad.titulo}
        </p>

        <div className="mt-2 flex flex-wrap items-center gap-2 text-xs font-bold">
          <span className="rounded-full bg-[var(--color-success-soft)] px-2.5 py-1 text-[var(--color-success)]">
            {aprobadas} {aprobadas === 1 ? "aprobada" : "aprobadas"}
          </span>

          {pendientes > 0 ? (
            <span className="rounded-full bg-[var(--color-warning-surface)] px-2.5 py-1 text-[var(--color-warning)]">
              {pendientes} en revisión
            </span>
          ) : null}

          {!tienePrincipal ? (
            /*
              El aviso que más vale: sin principal, las cards públicas
              caen a la ilustración del deporte.
            */
            <span className="rounded-full bg-[var(--color-info-soft)] px-2.5 py-1 text-[var(--color-info-deep)]">
              Sin imagen principal
            </span>
          ) : null}
        </div>
      </div>

      <AppLinkButton
        href={`/publicador/actividades/${actividad.id}`}
        variant="outline"
        size="sm"
      >
        Gestionar fotos
      </AppLinkButton>
    </li>
  );
}

/* Chip de estado de una imagen de identidad. */
function EstadoImagen({
  imagen,
}: {
  imagen: ImagenActividadPublicador | undefined;
}) {
  if (!imagen) {
    return (
      <span className="rounded-full bg-[var(--color-surface)] px-2.5 py-1 text-xs font-extrabold text-[var(--color-muted)] ring-1 ring-[var(--color-border-soft)]">
        Sin cargar
      </span>
    );
  }

  if (imagen.estadoModeracion === "PENDIENTE") {
    return (
      <span className="rounded-full bg-[var(--color-warning-surface)] px-2.5 py-1 text-xs font-extrabold text-[var(--color-warning)]">
        En revisión
      </span>
    );
  }

  return (
    <span className="rounded-full bg-[var(--color-success-soft)] px-2.5 py-1 text-xs font-extrabold text-[var(--color-success)]">
      Aprobada
    </span>
  );
}

function MarcaPaso({ completado = false }: { completado?: boolean }) {
  return (
    <span
      aria-hidden="true"
      className={`mt-0.5 flex h-[18px] w-[18px] shrink-0 items-center justify-center rounded-full text-[10px] font-extrabold ${
        completado
          ? "bg-[var(--color-secondary)] text-white"
          : "bg-[var(--color-surface)] text-transparent ring-2 ring-[var(--color-border-accent)]"
      }`}
    >
      ✓
    </span>
  );
}
