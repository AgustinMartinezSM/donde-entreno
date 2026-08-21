# Plan Fase 6 — Visibilidad de actividades: pausar y reanudar (sin migración)

Estado: **aprobado por Agustín con las 4 recomendaciones (2026-08-21) e implementado tal cual**. Bloque: `docs/bloque-contenido-visual-v1.md`. Resuelve el gate #5 del bloque (semántica de pausa).

## Diagnóstico (2026-08-21)

- El CHECK de la base (`chk_actividad_estado_publicacion`, script 01) **ya incluye `PAUSADA`**: no hay migración. El estado existe desde el día uno pero ningún código lo escribe ni lo lee — es el "estado muerto" del diagnóstico de Fase 0.
- **Por qué pausar hoy rompería el panel**: el panel del publicador entero filtra `activa AND estado_publicacion='PUBLICADA'` — el listado y el detalle (`PublicadorActividadService`), la gestión de fotos (`ImagenPublicadorService.obtenerActividadDelPublicador`), la creación de solicitudes de cambio (`SolicitudCambioActividadService`) y **la aprobación admin de un cambio** (`SolicitudCambioActividadAdminService`, busca la "actividad vigente" como PUBLICADA). Una actividad pausada desaparecería de TODO eso: invisible para su dueño, ingestionable, y con sus solicitudes de cambio aprobables pero inaplicables.
- El público filtra `PUBLICADA` en: catálogo, búsqueda, detalle por slug, feed de seguidos, y **favoritos** (la lista del backend omite lo no publicado; el favorito en sí no se borra). El sitemap del frontend se genera de la API pública → una pausada sale sola.
- `PublicadorActividadResumenDTO` **ya expone `estadoPublicacion`**: el chip "Pausada" del panel no necesita cambios de DTO.

## Semántica (la decisión del gate #5)

- **`PAUSADA` = pausa voluntaria del publicador**, reversible por él mismo, sin moderación: la actividad deja de existir para el público pero sigue entera para su dueño. Únicas transiciones: `PUBLICADA → PAUSADA` y `PAUSADA → PUBLICADA`.
- **`activa=false` queda como baja administrativa** (admin/scripts), como hoy. No se toca.
- **Sin borrado destructivo**: pausar no borra nada — ni fotos, ni horarios, ni favoritos, ni solicitudes.

## Backend

**`PATCH /api/publicador/actividades/{id}/visibilidad`** con body `{ "visible": true | false }` (rol PUBLICADOR, dueño de la actividad).

- `visible=false`: `PUBLICADA → PAUSADA`. `visible=true`: `PAUSADA → PUBLICADA`. Idempotente (pausar lo pausado = 200 sin cambio). Cualquier otro estado → 404 (la actividad "del panel" solo existe en esos dos estados).
- Log grepeable: `ACTIVIDAD_VISIBILIDAD actividadId={} de={} a={}` (solo metadata).

**Fix del panel** — las queries pasan de estado exacto a `IN (PUBLICADA, PAUSADA)`:
1. Listado del panel (con el estado ya visible en el DTO).
2. Detalle del panel.
3. Gestión de imágenes (subir/ordenar/principal/eliminar **se permite en pausa**: pausar ≠ congelar la gestión — es justamente cuándo un publicador quiere ordenar la casa).
4. Crear solicitudes de cambio sobre una pausada: **se permite** (mismo criterio).
5. Aprobación admin de cambios: encuentra la actividad vigente aunque esté pausada — el cambio aprobado aplica y la deja pausada, listo para cuando reanude.

**Lo que NO se auto-rechaza**: las solicitudes de cambio abiertas al momento de pausar siguen su curso (con el fix del punto 5 son aplicables). El "auto-rechazo de solicitudes sobre actividad no publicada" que barajaba el doc del bloque deja de ser necesario con este diseño — es mejor: nadie pierde trabajo hecho.

**Métricas del dashboard**: "publicadas" sigue contando solo PUBLICADA (es la promesa del número); se agrega el contador aditivo `actividadesPausadas` para que el dashboard pueda mostrar "X publicadas · Y pausadas".

**El público no cambia una línea**: todo lo público sigue filtrando `PUBLICADA`, así que la pausada desaparece sola de catálogo, búsqueda, detalle (404), feed, sitemap y listas de favoritos ajenas — y reaparece al reanudar. Los favoritos de los usuarios no se borran: la lista los omite mientras dura la pausa.

## Frontend

- **Detalle de actividad del panel**: botón "Pausar actividad" / "Reanudar actividad" con confirmación al pausar que explica el efecto ("no va a aparecer en búsquedas, en tu perfil ni en los guardados de los usuarios; la reanudás cuando quieras; tus fotos y datos no se tocan").
- **Chip "Pausada"** en el listado del panel y en el detalle (el DTO ya trae el estado). En el Centro de fotos, las pausadas se listan igual (ya salen del mismo endpoint) con su chip.
- Público: sin cambios.

## Tests

- **Unit**: transiciones válidas/idempotentes/inválidas, permisos (otra cuenta → 404), y que el listado del panel devuelve ambos estados.
- **IT de flujo completo** (patrón de los ITs existentes): crear publicada → pausar → el detalle público da 404, el catálogo y el feed la omiten, la lista de favoritos la omite (sin borrar el favorito) → el panel la sigue viendo (listado, detalle, fotos) → reanudar → todo vuelve, favorito incluido.

## Deploy

Dos pushes: backend primero (marcador `OPTIONS .../visibilidad` → `Allow` con `PATCH`; regla 12), frontend después (marcador "Pausar actividad" en chunks del panel — vía cookie-trick, la ruta es privada). Smoke de Agustín: pausar una actividad real, verla desaparecer del sitio público y de guardados, seguir gestionándola en el panel, reanudarla.

## Decisiones que pide este plan

1. **Semántica**: PAUSADA = pausa voluntaria reversible del publicador; `activa` queda para baja administrativa. **Recomendación: sí** (cierra el gate #5).
2. **En pausa se puede gestionar todo** (fotos, textos vía solicitud de cambio): pausar oculta, no congela. **Recomendación: sí.**
3. **No auto-rechazar solicitudes abiertas al pausar**: los cambios aprobados aplican sobre la pausada. **Recomendación: sí.**
4. **Favoritos se omiten durante la pausa sin borrarse** (reaparecen al reanudar). **Recomendación: sí** (es lo que el código ya hace solo).
