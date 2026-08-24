import {
  Skeleton,
  SkeletonGrillaActividades,
  SkeletonPagina,
} from "../../../components/ui/Skeleton";

/* Skeleton del perfil: portada + avatar + tabs + grilla. */
export default function Loading() {
  return (
    <SkeletonPagina>
      <Skeleton className="h-36 w-full rounded-[24px] sm:h-44" />
      <div className="mt-4 flex items-center gap-4">
        <Skeleton className="h-20 w-20 rounded-full" />
        <div className="flex-1 space-y-2">
          <Skeleton className="h-5 w-1/3" />
          <Skeleton className="h-3 w-1/4" />
        </div>
        <Skeleton className="h-10 w-28" />
      </div>
      <div className="mt-6 flex gap-2">
        <Skeleton className="h-10 w-28" />
        <Skeleton className="h-10 w-24" />
        <Skeleton className="h-10 w-20" />
      </div>
      <div className="mt-6">
        <SkeletonGrillaActividades cantidad={3} />
      </div>
    </SkeletonPagina>
  );
}
