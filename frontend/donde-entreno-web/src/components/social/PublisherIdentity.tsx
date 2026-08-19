import Image from "next/image";
import Link from "next/link";

import { formatearTipoPublicador } from "../../lib/formatoCatalogo";

type PublisherIdentityProps = {
  nombre?: string | null;
  tipo?: string | null;
  verificado?: boolean;
  /*
    - "compacta": listados densos (grillas de explorar, filas del feed).
    - "normal": header de card de feed.
    - "destacada": encabezado del detalle, donde el publicador firma el
      post y necesita el peso visual de un autor.
  */
  tamanio?: "compacta" | "normal" | "destacada";
  /*
    Destino del perfil público del publicador. Con href, el nombre se
    vuelve link (la identidad es navegable, como en cualquier red).
  */
  href?: string;
  /*
    Logo real del publicador. Sin logo caemos a las iniciales: el avatar
    nunca queda vacío.
  */
  avatarUrl?: string | null;
  /*
    Dato de contexto que se suma al tipo en la segunda línea, separado
    por un punto medio (por ejemplo "Publicada hace 3 días").
  */
  nota?: string | null;
};

export function PublisherIdentity({
  nombre,
  tipo,
  verificado = false,
  tamanio = "normal",
  href,
  avatarUrl,
  nota,
}: PublisherIdentityProps) {
  const nombreVisible = nombre?.trim() || "Comunidad DondeEntreno";
  const iniciales = obtenerIniciales(nombreVisible);
  const compacta = tamanio === "compacta";
  const destacada = tamanio === "destacada";

  const medidasAvatar = compacta
    ? "h-8 w-8 ring-2"
    : destacada
      ? "h-14 w-14 ring-4"
      : "h-11 w-11 ring-4";
  const subtitulo = [
    tipo ? formatearTipoPublicador(tipo) : "Actividad de la comunidad local",
    nota?.trim() || null,
  ]
    .filter(Boolean)
    .join(" · ");

  return (
    <div className={`flex min-w-0 items-center ${compacta ? "gap-2" : "gap-3"}`}>
      {avatarUrl ? (
        <span
          className={`relative shrink-0 overflow-hidden rounded-full bg-[var(--color-surface)] ring-[var(--color-info-soft)] ${medidasAvatar}`}
        >
          <Image
            src={avatarUrl}
            alt=""
            fill
            sizes="56px"
            className="object-cover"
          />
        </span>
      ) : (
        <span
          aria-hidden="true"
          className={`flex shrink-0 items-center justify-center rounded-full bg-[var(--color-brand)] font-extrabold tracking-[0.08em] text-white ring-[var(--color-info-soft)] ${medidasAvatar} ${
            compacta ? "text-[10px]" : destacada ? "text-sm" : "text-xs"
          }`}
        >
          {iniciales}
        </span>
      )}

      <div className="min-w-0">
        <div className="flex min-w-0 items-center gap-1.5">
          <p
            className={`truncate font-extrabold text-[var(--color-primary)] ${
              compacta ? "text-xs" : destacada ? "text-base" : "text-sm"
            }`}
          >
            {href ? (
              <Link
                href={href}
                className="rounded-sm transition hover:text-[#0B314D] hover:underline focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-[#4FB3D9]/30"
              >
                {nombreVisible}
              </Link>
            ) : (
              nombreVisible
            )}
          </p>
          {verificado ? (
            <span
              role="img"
              aria-label="Publicador verificado"
              title="Publicador verificado"
              className={`inline-flex shrink-0 items-center justify-center rounded-full bg-[var(--color-secondary)] text-white ${
                compacta ? "h-4 w-4" : "h-5 w-5"
              }`}
            >
              <svg
                viewBox="0 0 20 20"
                fill="none"
                stroke="currentColor"
                strokeWidth="2.2"
                strokeLinecap="round"
                strokeLinejoin="round"
                className={compacta ? "h-2.5 w-2.5" : "h-3 w-3"}
                aria-hidden="true"
              >
                <path d="m5.5 10 3 3 6-6" />
              </svg>
            </span>
          ) : null}
        </div>
        {!compacta ? (
          <p className="mt-0.5 truncate text-xs font-semibold text-[var(--color-muted)]">
            {subtitulo}
          </p>
        ) : null}
      </div>
    </div>
  );
}

function obtenerIniciales(nombre: string) {
  return nombre
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((parte) => parte.charAt(0).toLocaleUpperCase("es"))
    .join("");
}
