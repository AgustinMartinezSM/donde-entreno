import { Skeleton, SkeletonPagina } from "../../../components/ui/Skeleton";

/* Skeleton del detalle: identidad + carrusel + acciones + texto. */
export default function Loading() {
  return (
    <SkeletonPagina>
      <div className="mx-auto w-full max-w-4xl">
        <div className="flex items-center gap-3">
          <Skeleton className="h-12 w-12 rounded-full" />
          <div className="flex-1 space-y-2">
            <Skeleton className="h-4 w-1/3" />
            <Skeleton className="h-3 w-1/4" />
          </div>
          <Skeleton className="h-9 w-24" />
        </div>
        <Skeleton className="mt-4 aspect-[4/3] w-full rounded-[20px] sm:aspect-[16/9]" />
        <div className="mt-4 flex gap-2">
          <Skeleton className="h-11 w-24" />
          <Skeleton className="h-11 w-24" />
          <Skeleton className="h-11 w-24" />
        </div>
        <div className="mt-6 space-y-3">
          <Skeleton className="h-7 w-2/3" />
          <Skeleton className="h-4 w-full" />
          <Skeleton className="h-4 w-5/6" />
          <Skeleton className="h-4 w-1/2" />
        </div>
      </div>
    </SkeletonPagina>
  );
}
