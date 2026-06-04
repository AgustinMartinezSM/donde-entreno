import { API_BASE_URL } from "../lib/apiConfig";

// Esta función por ahora solo devuelve la URL base del backend.
// La usamos para probar que .env.local está bien configurado.
export function obtenerUrlBaseApi() {
  return API_BASE_URL;
}