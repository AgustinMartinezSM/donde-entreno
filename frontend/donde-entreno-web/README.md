# DondeEntreno — Frontend

Frontend web de **DondeEntreno**, la plataforma para encontrar deportes, clubes, profesores, gimnasios y actividades deportivas en tu ciudad.

- **Framework:** Next.js (App Router) + React + TypeScript
- **Estilos:** Tailwind CSS v4
- **Backend:** API REST Spring Boot (por defecto en `http://localhost:8080`)

## Requisitos

- Node.js 20+
- El backend corriendo localmente (ver `docs/backend.md` en la raíz del repo)

## Configuración

Copiar el archivo de ejemplo y ajustar si hace falta:

```bash
cp .env.example .env.local
```

Variables:

| Variable | Obligatoria | Descripción |
|---|---|---|
| `NEXT_PUBLIC_API_URL` | Sí | URL base del backend (local: `http://localhost:8080`) |
| `NEXT_PUBLIC_SITE_URL` | No | URL pública del frontend, usada para metadata/robots/sitemap (default: `http://localhost:3000`) |

## Comandos

```bash
npm install       # instalar dependencias
npm run dev       # desarrollo en http://localhost:3000
npm run build     # build de producción
npm run lint      # ESLint
npm run typecheck # verificación de tipos (tsc --noEmit)
```

## Estructura principal

```text
src/
  app/          # rutas (App Router): home, explorar, actividades, ciudades,
                # deportes, publicar, login/registro, mi-cuenta, publicador, admin
  components/   # componentes organizados por dominio
  services/     # clientes de la API REST (fetch + type guards)
  types/        # tipos TypeScript de la API
  lib/          # helpers (apiConfig, siteConfig, imágenes, búsqueda de deportes)
```

## Notas

- Todas las llamadas a la API usan `NEXT_PUBLIC_API_URL` como única fuente de la URL base (`src/lib/apiConfig.ts`).
- Las áreas `/admin` y `/publicador` requieren login con el rol correspondiente.
