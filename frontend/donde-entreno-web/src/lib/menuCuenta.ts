import { esRolAdmin, esRolPublicador } from "./authRedirects";

export type IconoCuentaTipo =
  | "perfil"
  | "guardados"
  | "deportes"
  | "siguiendo"
  | "publicador"
  | "actividades"
  | "solicitudes"
  | "imagenes"
  | "configuracion"
  | "salir";

export type ItemMenuCuenta = {
  href: string;
  label: string;
  icono: IconoCuentaTipo;
};

export type SeccionMenuCuenta = {
  /* null = sección sin encabezado visible. */
  titulo: string | null;
  items: ItemMenuCuenta[];
};

/*
  Las opciones del menú de cuenta, por rol, en un solo lugar.

  El menú desktop y el panel mobile mostraban (o directamente no
  mostraban) cosas distintas: desktop tenía menú con opciones y mobile
  saltaba directo a un destino por rol, así que un publicador en el
  teléfono no tenía NINGÚN camino a su perfil deportivo ni a sus
  guardadas. Definir las secciones acá garantiza que las dos superficies
  ofrezcan lo mismo y que un rol nuevo se agregue una sola vez.

  La regla de producto que esto encarna: "Mi perfil" lleva SIEMPRE al
  espacio personal (/mi-cuenta); el espacio de publicador y la
  administración son opciones separadas para quien corresponde, nunca el
  reemplazo de su lado persona.
*/
export function obtenerSeccionesCuenta(
  rol: string | null | undefined
): SeccionMenuCuenta[] {
  const secciones: SeccionMenuCuenta[] = [
    {
      titulo: "Tu espacio",
      items: [
        { href: "/mi-cuenta", label: "Mi perfil deportivo", icono: "perfil" },
        {
          href: "/favoritos",
          label: "Actividades guardadas",
          icono: "guardados",
        },
        {
          href: "/mi-cuenta?tab=deportes",
          label: "Mis deportes",
          icono: "deportes",
        },
        {
          href: "/mi-cuenta?tab=siguiendo",
          label: "Publicadores que sigo",
          icono: "siguiendo",
        },
        {
          /* Centro de Configuración (Fase 1 social): cuenta y preferencias. */
          href: "/configuracion",
          label: "Configuración",
          icono: "configuracion",
        },
      ],
    },
  ];

  if (rol && esRolPublicador(rol)) {
    secciones.push({
      titulo: "Publicador",
      items: [
        {
          href: "/publicador",
          label: "Mi espacio de publicador",
          icono: "publicador",
        },
        {
          href: "/publicador/actividades",
          label: "Mis actividades",
          icono: "actividades",
        },
        {
          href: "/publicador/solicitudes",
          label: "Mis solicitudes",
          icono: "solicitudes",
        },
        {
          href: "/publicador/fotos",
          label: "Mis fotos",
          icono: "imagenes",
        },
      ],
    });
  }

  if (rol && esRolAdmin(rol)) {
    secciones.push({
      titulo: "Administración",
      items: [
        {
          href: "/admin/solicitudes",
          label: "Solicitudes de publicación",
          icono: "solicitudes",
        },
        {
          href: "/admin/imagenes",
          label: "Imágenes pendientes",
          icono: "imagenes",
        },
        {
          /* Cola de reportes (Fase 2 social): moderación flexible. */
          href: "/admin/reportes",
          label: "Reportes",
          icono: "solicitudes",
        },
      ],
    });
  }

  return secciones;
}
