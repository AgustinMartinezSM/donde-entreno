export type Deporte = {
  id: number;
  nombre: string;
  slug: string;
  descripcion: string | null;
  iconoUrl: string | null;
  orden: number | null;
  categoriaId: number | null;
  categoriaNombre: string | null;
  categoriaSlug: string | null;
};
