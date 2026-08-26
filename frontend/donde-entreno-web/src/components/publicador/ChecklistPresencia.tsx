"use client";

import Link from "next/link";
import { useEffect, useState } from "react";

import {
  listarActividadesPublicador,
  listarImagenesActividad,
  listarImagenesPerfil,
  obtenerPerfilPublicador,
} from "../../services/publicadorService";
import type {
  ImagenActividadPublicador,
  PerfilPublicadorActual,
} from "../../types/publicador";

/*
  El "perfil de calidad" del publicador, en UN solo lugar.

  Vivía dentro del Centro de fotos (Fase 3 del bloque visual) y era el
  único lado donde se podía ver qué falta. Acá queda como componente
  compartido para poder mostrarlo también en el perfil, que es donde
  se resuelven cuatro de los seis pasos.

  Los pasos son solo los MEDIBLES con datos que ya existen: por eso no
  está "horarios cargados" — el resumen del panel no trae el dato y no
  se inventa.
*/

/** Fotos de galería que hacen que un detalle deje de sentirse vacío. */
export const GALERIA_RECOMENDADA = 3;

export type PasoPresencia = {
  clave: string;
  etiqueta: string;
  completado: boolean;
  href: string;
};

export type ResumenFotosActividad = {
  tienePrincipal: boolean;
  galeriaAprobada: number;
};

export function calcularPasosPresencia({
  perfil,
  imagenesPerfil,
  resumenActividades,
}: {
  perfil: PerfilPublicadorActual | null;
  imagenesPerfil: ImagenActividadPublicador[];
  resumenActividades: ResumenFotosActividad[];
}): PasoPresencia[] {
  const vigenteDe = (tipo: "LOGO" | "PORTADA") =>
    imagenesPerfil.find(
      (imagen) =>
        imagen.tipoImagen === tipo &&
        imagen.estadoModeracion !== "RECHAZADA" &&
        !(imagen.estadoModeracion === "APROBADA" && !imagen.activa)
    );

  return [
    {
      clave: "logo",
      etiqueta: "Logo cargado",
      completado: Boolean(vigenteDe("LOGO")),
      href: "/publicador/perfil",
    },
    {
      clave: "portada",
      etiqueta: "Portada cargada",
      completado: Boolean(vigenteDe("PORTADA")),
      href: "/publicador/perfil",
    },
    {
      clave: "principal",
      etiqueta: "Al menos una actividad con imagen principal",
      completado: resumenActividades.some((item) => item.tienePrincipal),
      href: "/publicador/actividades",
    },
    {
      clave: "galeria",
      etiqueta: `Una galería con ${GALERIA_RECOMENDADA} fotos o más`,
      completado: resumenActividades.some(
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
}

/**
 * Carga todo lo que el checklist necesita.
 *
 * Hace el mismo fan-out que el Centro de fotos (una llamada por
 * actividad, que con el volumen real son unidades) y es best-effort:
 * si algo falla devuelve `pasos` en null y el que llama simplemente
 * no dibuja el bloque. **Nunca un checklist a medias**: mostrar "2 de
 * 6" porque no cargaron las fotos sería peor que no mostrar nada.
 */
export function usePresenciaPublicador(accessToken: string | null) {
  const [pasos, setPasos] = useState<PasoPresencia[] | null>(null);

  useEffect(() => {
    let componenteActivo = true;

    if (!accessToken) {
      return () => {
        componenteActivo = false;
      };
    }

    async function cargar() {
      try {
        const [perfil, imagenesPerfil, paginaActividades] = await Promise.all([
          obtenerPerfilPublicador(accessToken as string),
          listarImagenesPerfil(accessToken as string),
          listarActividadesPublicador(
            { page: 0, size: 50, orden: "recientes" },
            accessToken as string
          ),
        ]);

        const fotos = await Promise.all(
          paginaActividades.contenido.map((actividad) =>
            listarImagenesActividad(actividad.id, accessToken as string).catch(
              () => [] as ImagenActividadPublicador[]
            )
          )
        );

        if (!componenteActivo) {
          return;
        }

        setPasos(
          calcularPasosPresencia({
            perfil,
            imagenesPerfil,
            resumenActividades: fotos.map((deLaActividad) => {
              const visibles = deLaActividad.filter(
                (foto) =>
                  !(foto.estadoModeracion === "APROBADA" && !foto.activa)
              );

              return {
                tienePrincipal: visibles.some(
                  (foto) =>
                    foto.tipoImagen === "PRINCIPAL" &&
                    foto.estadoModeracion === "APROBADA"
                ),
                galeriaAprobada: visibles.filter(
                  (foto) =>
                    foto.tipoImagen === "GALERIA" &&
                    foto.estadoModeracion === "APROBADA"
                ).length,
              };
            }),
          })
        );
      } catch {
        /* Best-effort: sin datos completos, no se dibuja. */
        if (componenteActivo) {
          setPasos(null);
        }
      }
    }

    void cargar();

    return () => {
      componenteActivo = false;
    };
  }, [accessToken]);

  return pasos;
}

export function ChecklistPresencia({
  pasos,
  titulo = "Tu presencia visual",
  claseTitulo = "mt-1 text-lg font-extrabold text-[var(--color-primary)]",
}: {
  pasos: PasoPresencia[];
  titulo?: string;
  claseTitulo?: string;
}) {
  const completados = pasos.filter((paso) => paso.completado).length;

  return (
    <>
      <h2 className={claseTitulo}>
        {titulo}: {completados} de {pasos.length}
      </h2>

      <ul className="mt-4 grid gap-2">
        {pasos.map((paso) => (
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
                className="flex items-start gap-2.5 rounded-[12px] px-2 py-1.5 text-sm font-bold text-[var(--color-primary)] transition duration-200 ease-out hover:bg-[var(--color-surface)]/60 focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-[var(--color-accent)]/30"
              >
                <MarcaPaso />
                {paso.etiqueta}
              </Link>
            )}
          </li>
        ))}
      </ul>
    </>
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
