/*
  Perfil público de publicador (GET /api/perfiles-publicadores).
  Es el DTO público del backend: sin datos privados de la cuenta.
*/
export type PerfilPublicadorPublico = {
  id: number;
  nombre: string;
  tipoPublicador?: string | null;
  descripcion?: string | null;
  emailContacto?: string | null;
  telefonoContacto?: string | null;
  whatsapp?: string | null;
  instagram?: string | null;
  sitioWeb?: string | null;
  verificado?: boolean | null;
};
