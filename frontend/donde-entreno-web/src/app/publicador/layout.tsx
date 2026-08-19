import type { ReactNode } from "react";

/*
  El espacio de publicador queda en claro en la V1 del modo oscuro:
  misma razón y mismo mecanismo que el layout de administración (tokens
  redeclarados por el bloque "Luz forzada" de globals.css). Se borra
  cuando el área se barra.
*/
export default function PublicadorLayout({
  children,
}: {
  children: ReactNode;
}) {
  return <div data-fuerza-claro>{children}</div>;
}
