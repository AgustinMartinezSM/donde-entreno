import {
  Skeleton,
  SkeletonGrillaActividades,
  SkeletonPagina,
} from "../components/ui/Skeleton";

/*
  Skeleton raíz (Fase 1): cubre la Home y cualquier ruta sin loading
  propio. Genérico a propósito: barra + hero fantasma + grilla.
*/
export default function Loading() {
  return (
    <SkeletonPagina>
      <Skeleton className="h-40 w-full rounded-[24px] sm:h-52" />
      <div className="mt-6 flex gap-3 overflow-hidden">
        {Array.from({ length: 6 }, (_, indice) => (
          <Skeleton key={indice} className="h-16 w-16 shrink-0 rounded-full" />
        ))}
      </div>
      <div className="mt-8">
        <SkeletonGrillaActividades />
      </div>
    </SkeletonPagina>
  );
}
