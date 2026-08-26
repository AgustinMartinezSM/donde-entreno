"use client";

import { listarDiasOrdenados } from "../../lib/formatoCatalogo";
import { useEffect, useState } from "react";
import Link from "next/link";

import { useFavoritos } from "../../lib/favoritos";
import { obtenerDetalleActividad } from "../../services/actividadService";
import type { ActividadDetalle } from "../../types/actividad";
import { SurfaceCard } from "../ui/SurfaceCard";

/*
  El comparador de guardadas (Fase 10).

  Frontend puro: los candidatos salen de los favoritos, que ya viven
  sincronizados con la cuenta, y las filas que faltan se completan con
  el detalle público que ya existe.

  Dos reglas de producto que están en el código:

  1. Una fila que NINGUNA de las elegidas tiene, NO se dibuja. Un
     comparador lleno de guiones se siente roto, y sugiere que el dato
     falta cuando en realidad nadie lo cargó.
  2. Se comparan hasta 3. Con más, la tabla deja de entrar en un
     teléfono y la comparación deja de ayudar a decidir.
*/

const MAXIMO_A_COMPARAR = 3;

type FilaComparacion = {
  etiqueta: string;
  /** Devuelve el valor a mostrar, o null si esa actividad no lo tiene. */
  valor: (datos: DatosComparables) => string | null;
};

type DatosComparables = {
  titulo: string;
  slug: string;
  deporte: string | null;
  zona: string | null;
  nivel: string | null;
  modalidad: string | null;
  precio: string | null;
  horarios: string | null;
  valoracion: string | null;
};

const FILAS: FilaComparacion[] = [
  { etiqueta: "Deporte", valor: (d) => d.deporte },
  { etiqueta: "Zona", valor: (d) => d.zona },
  { etiqueta: "Nivel", valor: (d) => d.nivel },
  { etiqueta: "Modalidad", valor: (d) => d.modalidad },
  { etiqueta: "Precio", valor: (d) => d.precio },
  { etiqueta: "Días", valor: (d) => d.horarios },
  { etiqueta: "Valoración", valor: (d) => d.valoracion },
];

function formatearPrecio(valor: number | null | undefined, mostrar?: boolean) {
  /*
    El publicador puede elegir no mostrar el precio: acá se respeta esa
    decisión igual que en el resto de la app.
  */
  if (!mostrar || valor === null || valor === undefined) {
    return null;
  }

  return `Desde $ ${valor.toLocaleString("es-AR")}`;
}

function formatearNivel(nivel?: string | null) {
  if (!nivel) {
    return null;
  }

  const texto = nivel.toLowerCase();

  return texto.charAt(0).toUpperCase() + texto.slice(1);
}

function formatearDias(detalle: ActividadDetalle | undefined) {
  /*
    Los días sin repetir y ordenados por semana: "Lunes, Miércoles"
    dice más para decidir que cinco filas de horas. La traducción del
    enum y el orden viven en lib/formatoCatalogo, compartidos con el
    detalle público.
  */
  const dias = listarDiasOrdenados(detalle?.horarios ?? []);

  return dias.length > 0 ? dias.join(", ") : null;
}

function formatearValoracion(detalle: ActividadDetalle | undefined) {
  const promedio = detalle?.socialProof?.valoracionPromedio;
  const cantidad = detalle?.socialProof?.cantidadValoraciones ?? 0;

  if (promedio === null || promedio === undefined || cantidad === 0) {
    return null;
  }

  return `★ ${promedio.toFixed(1)} (${cantidad})`;
}

export function ComparadorGuardados() {
  const favoritos = useFavoritos();
  const [elegidos, setElegidos] = useState<string[]>([]);
  const [detalles, setDetalles] = useState<Record<string, ActividadDetalle>>({});
  const [cargando, setCargando] = useState(false);

  /*
    El detalle solo se pide para lo que se está comparando, y una vez
    por slug: es lo que agrega días y valoración, que el favorito
    guardado no tiene.
  */
  useEffect(() => {
    let vigente = true;
    const faltantes = elegidos.filter((slug) => !detalles[slug]);

    if (faltantes.length === 0) {
      return () => {
        vigente = false;
      };
    }

    async function traer() {
      setCargando(true);

      const resultados = await Promise.all(
        faltantes.map(async (slug) => {
          try {
            return [slug, await obtenerDetalleActividad(slug)] as const;
          } catch {
            /*
              Si el detalle falla, la comparación sigue con lo que el
              favorito ya tiene: se pierden días y valoración, no la
              pantalla entera.
            */
            return null;
          }
        })
      );

      if (!vigente) {
        return;
      }

      setDetalles((previos) => {
        const nuevos = { ...previos };

        for (const resultado of resultados) {
          if (resultado) {
            nuevos[resultado[0]] = resultado[1];
          }
        }

        return nuevos;
      });
      setCargando(false);
    }

    void traer();

    return () => {
      vigente = false;
    };
  }, [elegidos, detalles]);

  /* Con una sola guardada no hay nada que comparar. */
  if (favoritos.length < 2) {
    return null;
  }

  function alternar(slug: string) {
    setElegidos((previos) => {
      if (previos.includes(slug)) {
        return previos.filter((elegido) => elegido !== slug);
      }

      if (previos.length >= MAXIMO_A_COMPARAR) {
        return previos;
      }

      return [...previos, slug];
    });
  }

  const datos: DatosComparables[] = elegidos.map((slug) => {
    const favorito = favoritos.find((item) => item.slug === slug);
    const detalle = detalles[slug];
    const zona = [favorito?.barrioNombre, favorito?.ciudadNombre]
      .filter(Boolean)
      .join(", ");

    return {
      titulo: favorito?.titulo ?? slug,
      slug,
      deporte: favorito?.deporteNombre ?? null,
      zona: zona || null,
      nivel: formatearNivel(favorito?.nivel),
      modalidad: formatearNivel(favorito?.modalidad),
      precio: formatearPrecio(favorito?.precioReferencia, favorito?.mostrarPrecio),
      horarios: formatearDias(detalle),
      valoracion: formatearValoracion(detalle),
    };
  });

  /* La regla: si nadie tiene el dato, la fila no existe. */
  const filasVisibles = FILAS.filter((fila) =>
    datos.some((dato) => fila.valor(dato) !== null)
  );

  const completo = elegidos.length >= MAXIMO_A_COMPARAR;

  return (
    <SurfaceCard as="section" className="mt-8 p-5 sm:p-6">
      <h2 className="text-lg font-extrabold text-[var(--color-primary)]">
        Comparar guardadas
      </h2>
      <p className="mt-1 text-sm text-[var(--color-muted)]">
        Elegí hasta {MAXIMO_A_COMPARAR} para verlas una al lado de la otra.
      </p>

      <div className="mt-4 flex flex-wrap gap-2">
        {favoritos.map((favorito) => {
          const elegido = elegidos.includes(favorito.slug);

          return (
            <button
              key={favorito.slug}
              type="button"
              aria-pressed={elegido}
              disabled={!elegido && completo}
              onClick={() => alternar(favorito.slug)}
              className={`max-w-full truncate rounded-full border px-3 py-1.5 text-sm transition ${
                elegido
                  ? "border-transparent bg-brand text-white"
                  : "border-[var(--color-border)] text-[var(--color-text)] hover:border-[var(--color-primary)] disabled:cursor-not-allowed disabled:opacity-40"
              }`}
            >
              {favorito.titulo}
            </button>
          );
        })}
      </div>

      {elegidos.length < 2 ? (
        <p className="mt-4 text-sm text-[var(--color-muted)]">
          Elegí al menos dos.
        </p>
      ) : (
        <div className="mt-5 overflow-x-auto">
          <table className="w-full min-w-[32rem] border-collapse text-left text-sm">
            <thead>
              <tr>
                <th className="w-28 pb-3 pr-3 align-bottom text-xs font-bold uppercase tracking-[0.14em] text-[var(--color-muted)]">
                  {cargando ? "Cargando…" : ""}
                </th>

                {datos.map((dato) => (
                  <th key={dato.slug} className="pb-3 pr-3 align-bottom">
                    <Link
                      href={`/actividades/${dato.slug}`}
                      className="font-bold text-[var(--color-primary)] underline-offset-4 hover:underline"
                    >
                      {dato.titulo}
                    </Link>
                  </th>
                ))}
              </tr>
            </thead>

            <tbody>
              {filasVisibles.map((fila) => (
                <tr
                  key={fila.etiqueta}
                  className="border-t border-[var(--color-border-soft)]"
                >
                  <th
                    scope="row"
                    className="py-2 pr-3 align-top text-xs font-bold uppercase tracking-[0.14em] text-[var(--color-muted)]"
                  >
                    {fila.etiqueta}
                  </th>

                  {datos.map((dato) => (
                    <td
                      key={dato.slug}
                      className="py-2 pr-3 align-top text-[var(--color-text)]"
                    >
                      {/*
                        Acá SÍ va un guion: la fila existe porque alguna
                        de las elegidas tiene el dato, y que a esta le
                        falte es justamente información para decidir.
                      */}
                      {fila.valor(dato) ?? (
                        <span className="text-[var(--color-muted)]">—</span>
                      )}
                    </td>
                  ))}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </SurfaceCard>
  );
}
