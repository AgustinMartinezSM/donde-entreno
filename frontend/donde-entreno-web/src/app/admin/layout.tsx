import type { ReactNode } from "react";

/*
  La administración queda en claro en la V1 del modo oscuro: es
  superficie interna con blancos todavía sin barrer, y forzar la luz acá
  evita entregarla a medio teñir. El atributo redeclara los tokens del
  tema (globals.css, bloque "Luz forzada") para todo el subárbol. Cuando
  el área se barra, este layout se borra.
*/
export default function AdminLayout({ children }: { children: ReactNode }) {
  return <div data-fuerza-claro>{children}</div>;
}
