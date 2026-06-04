// Guardamos en una constante la URL base del backend.
// Esta URL viene desde el archivo .env.local.
export const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL;

// Validamos que la variable exista.
// Esto nos ayuda a detectar rápido si nos olvidamos de crear .env.local.
if (!API_BASE_URL) {
  throw new Error(
    "Falta configurar NEXT_PUBLIC_API_URL en el archivo .env.local"
  );
}