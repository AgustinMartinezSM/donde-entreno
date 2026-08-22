"use client";

import { useDeportesFavoritos } from "../../lib/preferenciasDeportivas";
import { useFavoritos } from "../../lib/favoritos";
import { useAuthSession } from "../auth/AuthSessionProvider";
import { RecomendadosParaVos } from "../cuenta/RecomendadosParaVos";

type HomeParaVosProps = {
  ciudadSlug: string;
  ciudadNombre: string;
};

/*
  "Para vos" en la Home (bloque 12): las mismas recomendaciones reales
  de /mi-cuenta —búsqueda pública filtrada por la ciudad y los deportes
  que la persona eligió— pero donde más se ven. Sin modelo de
  recomendación ni métricas inventadas, y el encabezado lo dice.

  Solo para sesiones iniciadas CON deportes elegidos: el visitante y el
  usuario sin preferencias ya tienen el feed de descubrimiento general,
  y duplicarlo con otro título sería ruido. Mientras la sesión resuelve
  tampoco se dibuja (mismo criterio que los chips de preferencias).
*/
export function HomeParaVos({ ciudadSlug, ciudadNombre }: HomeParaVosProps) {
  const { status } = useAuthSession();
  const deportes = useDeportesFavoritos();
  const favoritos = useFavoritos();

  if (status !== "authenticated" || deportes.length === 0) {
    return null;
  }

  return (
    <div className="mt-12 sm:mt-16">
      <RecomendadosParaVos
        deportesSlugs={deportes}
        ciudadSlug={ciudadSlug}
        ciudadNombre={ciudadNombre}
        slugsGuardados={favoritos.map((favorito) => favorito.slug)}
        maxVisibles={3}
      />
    </div>
  );
}
