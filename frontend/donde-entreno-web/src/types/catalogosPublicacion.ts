// Modelo normalizado para el select de deportes del formulario de publicación.
// No es una copia completa del DTO que devuelve el backend.
export type DeportePublicacionOpcion = {
  id: number;
  nombre: string;
  slug: string;
};

// Modelo normalizado para el select de ciudades del formulario de publicación.
// No es una copia completa del DTO que devuelve el backend.
export type CiudadPublicacionOpcion = {
  id: number;
  nombre: string;
};

// Modelo normalizado para el select de barrios del formulario de publicación.
// No es una copia completa del DTO que devuelve el backend.
export type BarrioPublicacionOpcion = {
  id: number;
  nombre: string;
  ciudadId: number;
  ciudadNombre: string;
};
