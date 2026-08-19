import type { ReactNode } from "react";

/*
  El flujo de publicar (PublishForm es el archivo con más blancos sin
  barrer de toda la app) queda en claro en la V1 del modo oscuro, igual
  que administración y el espacio de publicador. Se borra cuando el
  formulario se tokenice.
*/
export default function PublicarLayout({
  children,
}: {
  children: ReactNode;
}) {
  return <div data-fuerza-claro>{children}</div>;
}
