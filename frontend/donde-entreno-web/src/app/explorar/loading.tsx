import {
  Skeleton,
  SkeletonGrillaActividades,
  SkeletonPagina,
} from "../../components/ui/Skeleton";

/* Skeleton de Explorar: buscador + chips + grilla. */
export default function Loading() {
  return (
    <SkeletonPagina>
      <Skeleton className="h-12 w-full rounded-[18px] sm:max-w-xl" />
      <div className="mt-4 flex gap-2">
        <Skeleton className="h-8 w-24 rounded-full" />
        <Skeleton className="h-8 w-28 rounded-full" />
        <Skeleton className="h-8 w-20 rounded-full" />
      </div>
      <div className="mt-6">
        <SkeletonGrillaActividades />
      </div>
    </SkeletonPagina>
  );
}
