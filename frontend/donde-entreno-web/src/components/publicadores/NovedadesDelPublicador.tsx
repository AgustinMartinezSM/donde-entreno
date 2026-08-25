import Image from "next/image";

import { construirUrlImagenBackend } from "../../lib/backendUrl";
import { formatearFechaRelativa } from "../../lib/formatoFecha";
import type { Novedad } from "../../services/novedadesService";
import { BotonMeGustaNovedad } from "../social/BotonMeGustaNovedad";
import { BotonReportar } from "../social/BotonReportar";
import { SurfaceCard } from "../ui/SurfaceCard";

/*
  El canal del publicador en su perfil público (script 34, Fase 8).

  Server component salvo el reporte: el texto lo escribe una persona,
  así que cada novedad lleva su válvula de moderación flexible — misma
  regla que fotos, comentarios y preguntas desde la Fase 2 social.
*/
export function NovedadesDelPublicador({
  novedades,
}: {
  novedades: Novedad[];
}) {
  return (
    <ul className="mt-5 space-y-3">
      {novedades.map((novedad) => {
        const url = construirUrlImagenBackend(novedad.imagenUrl);

        return (
          <li key={novedad.id}>
            <SurfaceCard as="article" className="p-5">
              <p className="whitespace-pre-line text-sm leading-6 text-[var(--color-text)]">
                {novedad.texto}
              </p>

              {url ? (
                <div className="relative mt-3 h-48 w-full overflow-hidden rounded-[16px] sm:h-60">
                  <Image
                    src={url}
                    alt=""
                    fill
                    sizes="(max-width: 640px) 100vw, 60vw"
                    className="object-cover"
                  />
                </div>
              ) : null}

              <div className="mt-3 flex flex-wrap items-center justify-between gap-2">
                <div className="flex flex-wrap items-center gap-3">
                  <BotonMeGustaNovedad
                    novedadId={novedad.id}
                    meGustaInicial={Boolean(novedad.meGusta)}
                    cantidadInicial={novedad.cantidadMeGusta ?? 0}
                  />

                  <p className="text-xs text-[var(--color-muted)]">
                    {novedad.createdAt
                      ? formatearFechaRelativa(novedad.createdAt)
                      : null}
                  </p>
                </div>

                <BotonReportar
                  tipoObjeto="NOVEDAD"
                  objetoId={novedad.id}
                  etiquetaObjeto="esta novedad"
                  compacto
                />
              </div>
            </SurfaceCard>
          </li>
        );
      })}
    </ul>
  );
}
