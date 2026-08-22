# Plan Bloques 13 + 14 — Colecciones de guardados y Likes en fotos

Estado: **propuesto, pendiente de aprobación de Agustín**. Los dos
bloques traen migración (scripts 22 y 23, versionados junto a este
plan): nada se aplica en Supabase sin tu autorización, y siempre ANTES
que el código. Orden de implementación: 13 primero, 14 después (tu
enumeración), en pushes separados.

## Bloque 13 — Colecciones de guardados V1

**Qué es** (roadmap: "colecciones, notas, ordenar, comparar, 'para
probar', 'cerca de casa'"): organizar los guardados en colecciones con
nombre propio ("Para probar", "Cerca de casa" son ejemplos que la
persona escribe, no features), más una nota corta por guardado.

**Recorte V1 a propósito**: un guardado vive en UNA colección (o en
ninguna: "Todos"). Ordenar a mano y comparar quedan para después — la
colección + la nota ya cubren el 80% del uso.

**Migración (script `22_colecciones_guardados.sql`)**:
- Tabla `coleccion_guardados`: `id`, `usuario_id` (FK ON DELETE
  CASCADE), `nombre VARCHAR(60) NOT NULL`, `created_at`, UNIQUE
  `(usuario_id, nombre)`.
- `favorito_actividad`: `ADD COLUMN coleccion_id BIGINT NULL REFERENCES
  coleccion_guardados(id) ON DELETE SET NULL` (borrar la colección no
  borra guardados: vuelven a "Todos") y `ADD COLUMN nota VARCHAR(280)`.
- Aditiva pura; el código viejo la ignora.

**Backend** (`/api/usuario/**`, ya autenticado):
- `GET/POST /api/usuario/colecciones` (listar con conteos, crear;
  máximo 20 por usuario), `PATCH /{id}` (renombrar), `DELETE /{id}`.
- `PATCH /api/usuario/favoritos/{slug}` con `{coleccionId?, nota?}`
  (null = quitar de la colección / borrar nota). El GET de favoritos
  suma `coleccionId` y `nota` (aditivo).

**Frontend**: en Guardados (`/mi-cuenta` y `/favoritos`): chips-filtro
por colección ("Todos" + las propias + "Nueva colección"), menú por
card para mover de colección y editar la nota, nota visible en la card.

## Bloque 14 — Likes en fotos V1

**Qué es**: corazón por foto en el visor (LightboxFotos) y contador
público. El diseño ya estaba hecho en `docs/fase8-diseno-futuro.md` §1;
esto lo ejecuta tal cual.

**Recorte V1 a propósito**: "ordenar por populares" recién cuando haya
volumen de likes; "reportar foto" exige circuito de moderación de
reportes que no existe — queda anotado, no entra.

**Migración (script `23_likes_fotos.sql`)**: tabla `me_gusta_imagen`
con el patrón exacto de favoritos — `usuario_id` (FK CASCADE),
`imagen_id` (FK CASCADE), `created_at`, UNIQUE `(usuario_id,
imagen_id)`, índice por `imagen_id` para los conteos.

**Backend**: `PUT/DELETE /api/usuario/likes-fotos/{imagenId}`
(idempotentes; solo imágenes APROBADAS y activas). Los DTOs públicos de
imágenes suman `cantidadLikes` (query agrupada, sin N+1 — patrón del
contador de seguidores) y, con sesión, el listado de likes propios va
en un `GET /api/usuario/likes-fotos` (ids) para pintar los corazones.

**Frontend**: corazón con contador en el pie de LightboxFotos y en la
esquina de las cards de galería del perfil público; anónimo → invitación
a crear cuenta (patrón FavoritoButton); optimista con reversa.

## Verificación y deploy (cada bloque por separado)

Unit + ITs nuevos con el patrón de siempre (SyncFavoritosIT como
molde). Dos pushes por bloque, marcadores OPTIONS/chunks, regla 12.
Orden estricto por bloque: script en Supabase Y local (vos) → ITs →
backend → frontend → tu smoke.

## Decisiones que piden estos planes

1. **13: un guardado en UNA colección** (no muchas) + nota de 280.
   **Recomendación: sí** — es el recorte que evita una tabla puente y
   una UI de multiselección que nadie pidió.
2. **13: borrar colección = los guardados vuelven a "Todos"** (ON
   DELETE SET NULL, nunca borra guardados). **Recomendación: sí.**
3. **14: ejecutar el diseño de F8 tal cual** (likes solo en fotos
   aprobadas, contador público en DTOs, sin "populares" ni "reportar"
   en V1). **Recomendación: sí.**
4. **Scripts 22 y 23**: autorizar y aplicar (Supabase + local) cuando
   arranque cada bloque — 22 primero, 23 recién al empezar el 14.
