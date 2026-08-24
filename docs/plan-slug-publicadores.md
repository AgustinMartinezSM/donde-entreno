# Plan — Slug amigable para perfiles de publicador

Estado: **CERRADO EN PRODUCCIÓN** (smoke de Agustín OK 2026-08-24,
aprobado con las 4 recomendaciones). Script 27 aplicado en Supabase y
local ANTES del deploy (backfill verificado en producción:
`club-atletico-kimberley` → 200). Backend `f34074d` (489 unit + 79
ITs; marcador 400→404 en path no numérico), frontend `e085009` +
`c4f8f21`. Cierra el pendiente D del consolidado.

**Desvío documentado del plan — el redirect canónico NO es un 308
HTTP**: en Next 16 el shell Y el metadata se streamean antes de que
corra el código de la página (verificado en producción: el
`permanentRedirect` del page component Y el de `generateMetadata`
salen como `<meta http-equiv="refresh" content="0;url=...">` dentro de
un 200, incluso con user-agent de Googlebot). Un 308 real exigiría
resolver id→slug en el middleware con una llamada a la API por
request. **Decisión: se acepta el meta refresh** — para el navegador
es instantáneo, Google trata el meta refresh de 0 segundos como
redirect permanente, el `canonical` apunta a la URL con slug y el
sitemap solo lista slugs. Si algún día se quiere el 308: middleware
con cache del mapeo id→slug.

## Qué cambia para la gente

- Los perfiles públicos pasan a tener URL con nombre:
  `/publicadores/club-atletico-sur`. Mejor para compartir y para SEO.
- **Ningún link viejo se rompe** (regla 7): `/publicadores/8` sigue
  funcionando y redirige (308) a la URL con slug — una sola URL
  canónica para los buscadores.

## Migración (script 27 — la única del bloque)

- `ALTER TABLE perfil_publicador ADD COLUMN slug VARCHAR(150) NULL`
  + índice **UNIQUE parcial** (`WHERE slug IS NOT NULL`).
- **Backfill en el mismo script** de los perfiles existentes: nombre
  normalizado (minúsculas, sin acentos, guiones), y ante colisión
  sufijo `-<id>`. El UPDATE es idempotente (`WHERE slug IS NULL`):
  re-ejecutable si hiciera falta.
- **Nullable a propósito**: la migración corre ANTES que el código
  (regla 2) y el backend viejo sigue creando perfiles sin slug durante
  la ventana. `NOT NULL` lo rompería. Un perfil creado en esa ventana
  queda con slug NULL y su página sigue andando por id; el mismo UPDATE
  del script lo sanea después.

## Backend (aditivo, sin contrato roto)

- `slug` en la entidad y en `PerfilPublicadorDTO` (listado y detalle).
- `GET /api/perfiles-publicadores/{idOSlug}`: el path acepta ambos —
  numérico resuelve por id (links y clientes viejos), no numérico por
  slug. Hoy un path no numérico da 400; pasa a resolver o 404.
- Generación al crear el perfil en los DOS puntos que existen
  (registro de publicador en `AuthService` y aprobación en
  `SolicitudPublicacionAdminService`), reusando el patrón
  `generarSlugUnico` de actividades (base normalizada + sufijo `-2`,
  `-3` ante colisión, mismo normalizador NFD).
- **El slug es ESTABLE**: renombrar el perfil (fase 5e) NO lo cambia —
  cambiar slugs rompe links compartidos y SEO. Si algún día se quiere
  renombrar URLs, será con tabla de alias y 301 (fuera de V1).

## Frontend

- `/publicadores/[id]` acepta id o slug: el param viaja tal cual al
  endpoint y el backend resuelve. Si se entró por id numérico y el
  perfil tiene slug → `redirect()` a la URL canónica con slug.
- Todos los links internos pasan a `slug ?? id` (cards, feed de
  seguidos, sugeridos, "Ver perfil" del detalle, dashboard, editor).
- El sitemap lista la URL con slug cuando existe.

## Verificación y deploy

1. Unit tests (generación y unicidad del slug, resolución id vs slug)
   + IT (perfil real: por slug 200, por id redirige/resuelve, slug en
   el DTO del listado) en contexto compartido.
2. Script 27 en Supabase y local (vos) → ITs → **backend** (marcador
   anónimo conductual: `GET /api/perfiles-publicadores/<slug-real>` —
   el build viejo responde 400 por no parsear el Long, el nuevo 200) →
   **frontend** (marcador en chunks) → tu smoke.

## Decisiones que pide este plan (con recomendación)

1. **Slug estable ante renombres**: recomiendo que NO cambie al editar
   el nombre del publicador — los links compartidos y el SEO valen más
   que la coincidencia exacta con el nombre del momento.
2. **Redirect canónico**: recomiendo que entrar por `/publicadores/8`
   redirija a la URL con slug (una sola URL por perfil para Google);
   la alternativa (servir ambas sin redirect) duplica contenido.
3. **Columna nullable + backfill idempotente** (no `NOT NULL`): es lo
   que respeta la regla "migración antes que código" sin romper el
   backend viejo en la ventana.
4. **Colisiones con sufijo numérico** (`club-union`, `club-union-2`):
   el patrón ya probado de actividades.
