// Opción simple para selects o filtros.
// La usamos para ciudad, barrio, deporte, categoría, etc.
export type FiltroOpcion = {
  id: number;
  nombre: string;
  slug?: string;

  // Algunos endpoints pueden traer datos extra.
  descripcion?: string;
  iconoUrl?: string | null;
  orden?: number;

  // Relaciones opcionales, por ejemplo barrio -> ciudad o deporte -> categoría.
  ciudadId?: number;
  ciudadNombre?: string;

  categoriaId?: number;
  categoriaNombre?: string;
  categoriaSlug?: string;
};

// Representa la respuesta real del endpoint:
// GET /api/filtros/opciones
export type FiltrosOpciones = {
  categorias: FiltroOpcion[];
  deportes: FiltroOpcion[];
  ciudades: FiltroOpcion[];
  barrios: FiltroOpcion[];

  niveles: string[];
  modalidades: string[];
  ordenes: string[];
};