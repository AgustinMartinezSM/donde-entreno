"use client";

import { useState } from "react";

import { API_BASE_URL } from "../../lib/apiConfig";
import { formatearDistancia } from "../../lib/mapas";
import type { Actividad } from "../../types/actividad";
import { SocialActivityCard } from "../social/SocialActivityCard";
import { AppButton } from "../ui/AppButton";
import { SectionHeader } from "../ui/SectionHeader";
import { StatusMessage } from "../ui/StatusMessage";

type Resultado = {
  contenido: Actividad[];
  radioKm: number;
  sinCoordenadas: number;
  totalEnRadio: number;
};

const RADIOS = [1, 3, 5, 10];

/*
  "Cerca mío" (Fase 7).

  La ubicación se pide al navegador en el momento y viaja SOLO como
  parámetro de esta consulta: no se guarda en el dispositivo, no se
  manda a terceros y el backend no la persiste ni la loguea. Es lo que
  la página de /privacidad ya promete.

  Sin mapa a propósito: con la mayoría de las sedes todavía sin punto
  cargado, un mapa mostraría más huecos que pines. Ordenar por
  distancia da el valor real sin sumar una sola dependencia.
*/
export function CercaMio({ ciudadSlug }: { ciudadSlug?: string }) {
  const [estado, setEstado] = useState<
    "inicial" | "pidiendo" | "buscando" | "listo" | "sin-permiso" | "error"
  >("inicial");
  const [resultado, setResultado] = useState<Resultado | null>(null);
  const [radioKm, setRadioKm] = useState(5);

  function buscar(radio: number) {
    if (typeof navigator === "undefined" || !navigator.geolocation) {
      setEstado("error");
      return;
    }

    setEstado("pidiendo");
    setRadioKm(radio);

    navigator.geolocation.getCurrentPosition(
      (posicion) => {
        setEstado("buscando");

        const parametros = new URLSearchParams({
          lat: String(posicion.coords.latitude),
          lng: String(posicion.coords.longitude),
          radioKm: String(radio),
        });

        if (ciudadSlug) {
          parametros.set("ciudadSlug", ciudadSlug);
        }

        fetch(`${API_BASE_URL}/api/actividades/cerca?${parametros.toString()}`, {
          cache: "no-store",
        })
          .then((respuesta) => {
            if (!respuesta.ok) {
              throw new Error("fallo");
            }
            return respuesta.json();
          })
          .then((datos: Resultado) => {
            setResultado(datos);
            setEstado("listo");
          })
          .catch(() => setEstado("error"));
      },
      () => {
        /* Permiso denegado o no disponible: se dice, no se insiste. */
        setEstado("sin-permiso");
      },
      { timeout: 10000, maximumAge: 60000 }
    );
  }

  return (
    <section className="mt-8" aria-labelledby="cerca-mio-titulo">
      <SectionHeader
        eyebrow="Cerca tuyo"
        title="¿Qué hay cerca de dónde estás?"
        description="Usamos tu ubicación solo para esta búsqueda: no la guardamos."
        titleId="cerca-mio-titulo"
      />

      <div className="mt-4 flex flex-wrap items-center gap-2">
        {RADIOS.map((radio) => (
          <AppButton
            key={radio}
            type="button"
            size="sm"
            variant={radio === radioKm && estado === "listo" ? "primary" : "secondary"}
            onClick={() => buscar(radio)}
            disabled={estado === "pidiendo" || estado === "buscando"}
          >
            {radio} km
          </AppButton>
        ))}
      </div>

      {estado === "pidiendo" ? (
        <StatusMessage variant="info" role="status" className="mt-4">
          Esperando que nos des permiso para usar tu ubicación...
        </StatusMessage>
      ) : null}

      {estado === "buscando" ? (
        <StatusMessage variant="info" role="status" className="mt-4">
          Buscando actividades cerca tuyo...
        </StatusMessage>
      ) : null}

      {estado === "sin-permiso" ? (
        <StatusMessage variant="warning" className="mt-4">
          Sin acceso a tu ubicación no podemos ordenar por distancia. Podés
          filtrar por barrio, que da un resultado parecido.
        </StatusMessage>
      ) : null}

      {estado === "error" ? (
        <StatusMessage variant="warning" className="mt-4">
          No pudimos buscar por cercanía. Probá de nuevo en unos minutos.
        </StatusMessage>
      ) : null}

      {estado === "listo" && resultado ? (
        <div className="mt-4">
          {resultado.contenido.length === 0 ? (
            <StatusMessage variant="info">
              No encontramos actividades a menos de {resultado.radioKm} km.
              Probá con un radio más grande.
            </StatusMessage>
          ) : (
            <>
              <p className="text-sm font-bold text-[var(--color-primary)]">
                {resultado.totalEnRadio === 1
                  ? "1 actividad"
                  : `${resultado.totalEnRadio} actividades`}{" "}
                a menos de {resultado.radioKm} km
              </p>

              <div className="mt-4 grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
                {resultado.contenido.map((actividad) => (
                  <div key={actividad.id}>
                    <SocialActivityCard actividad={actividad} variante="compacta" />
                    {formatearDistancia(actividad.distanciaKm) ? (
                      <p className="mt-1.5 text-xs font-extrabold text-[var(--color-secondary)]">
                        {formatearDistancia(actividad.distanciaKm)}
                      </p>
                    ) : null}
                  </div>
                ))}
              </div>
            </>
          )}

          {/*
            Honestidad sobre el dato (decisión del plan): las que no
            tienen punto cargado NO se ubican en el centro del barrio
            para rellenar — se dice cuántas quedaron afuera.
          */}
          {resultado.sinCoordenadas > 0 ? (
            <p className="mt-4 text-xs leading-5 text-[var(--color-muted)]">
              {resultado.sinCoordenadas === 1
                ? "1 actividad no aparece acá porque todavía no cargó su ubicación exacta."
                : `${resultado.sinCoordenadas} actividades no aparecen acá porque todavía no cargaron su ubicación exacta.`}{" "}
              Podés encontrarlas filtrando por barrio.
            </p>
          ) : null}
        </div>
      ) : null}
    </section>
  );
}
