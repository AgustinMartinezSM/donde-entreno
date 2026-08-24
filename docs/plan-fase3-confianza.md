# Plan — Fase 3 social: valoraciones, Q&A y el flujo "quiero probar / ya probé"

Estado: **propuesto, pendiente de aprobación de Agustín**. Es la capa
de CONFIANZA — lo que pidió el único feedback real registrado
("referencias, valoraciones") — construida sobre la infraestructura de
Fase 2 (reportes + notificaciones ya vivos en producción).

## Qué ve la gente al final

1. **El flujo propio de DondeEntreno**: Guardé → **Quiero probar** →
   **Ya probé** → Valoro / Entreno acá. "Quiero probar" vive junto al
   contacto (es pre-visita); al marcar "Ya probé" se invita a valorar.
2. **Valoraciones 1-5 con reseña** en el detalle: promedio y
   distribución de estrellas (el promedio aparece recién con 3+
   valoraciones — un 1 solitario no hunde a un club), tags rápidos
   ("Buen ambiente", "Ideal principiantes"...), insignia **"Verificada"**
   cuando la persona probó o entrena ahí, editar/eliminar la propia,
   reportar las ajenas.
3. **Preguntas y respuestas** por actividad (estilo MercadoLibre): el
   usuario pregunta en público, el publicador responde, y la respuesta
   queda para todos. Notificaciones en ambas direcciones.
4. El publicador ve en sus métricas cuánta gente **quiere probar** sus
   actividades (agregado y anónimo).

## Migración (script 29 — tres tablas + un ALTER, una transacción)

- `interes_actividad`: usuario_id FK CASCADE, actividad_id FK CASCADE,
  estado CHECK ('QUIERO_PROBAR','YA_PROBE'), created_at, updated_at,
  **UNIQUE (usuario, actividad)** — el flujo es una transición de
  estado, no filas acumuladas.
- `valoracion`: usuario_id FK CASCADE, actividad_id FK CASCADE,
  puntaje SMALLINT CHECK 1..5, comentario VARCHAR(500) NULL, tags
  VARCHAR(255) NULL (CSV de un catálogo fijo validado en el service),
  verificada BOOLEAN NOT NULL, estado CHECK ('VISIBLE',
  'OCULTA_POR_ADMIN') DEFAULT VISIBLE, created_at, updated_at,
  **UNIQUE (usuario, actividad)**. Índice (actividad_id, estado).
- `pregunta_actividad`: actividad_id FK CASCADE, usuario_id FK
  CASCADE, pregunta VARCHAR(500), respuesta VARCHAR(1000) NULL,
  respondida_at NULL, estado CHECK ('VISIBLE','OCULTA_POR_ADMIN',
  'ELIMINADA_POR_USUARIO') DEFAULT VISIBLE, created_at. La respuesta
  vive EN la misma fila (patrón MercadoLibre: una respuesta del
  publicador por pregunta). Índice (actividad_id, estado, created_at).
- **ALTER del CHECK `chk_reporte_tipo_objeto`**: sumar 'VALORACION' y
  'PREGUNTA' (la lección del script 25, esta vez aplicada a tiempo:
  el CHECK enumera valores y los valores nuevos exigen redefinirlo).

## Backend

- **InteresActividadService**: marcar QUIERO_PROBAR / YA_PROBE
  (upsert idempotente sobre el UNIQUE), quitar (delete), estado propio
  para pintar los botones. Contador agregado de "quieren probar" por
  actividad (query agrupada) → métricas del publicador y social proof.
- **ValoracionService**:
  - Crear/editar (upsert por el UNIQUE) **solo con señal de uso**:
    favorito, quiero probar, ya probé o check-in sobre ESA actividad.
    Sin señal → 403 con mensaje claro. `verificada = true` si la señal
    es fuerte (YA_PROBE o check-in) al momento de valorar.
  - Reseña de texto hasta 500 chars, publicación DIRECTA (filosofía
    nueva) — reportable como VALORACION y ocultable por admin.
  - Eliminar la propia; admin oculta cualquiera (estado).
  - GET público por actividad: promedio (null con N<3), cantidad,
    distribución 1..5, lista paginada de visibles (las reseñas se
    listan desde la primera; solo el promedio espera a 3).
  - Tags: catálogo fijo (BUEN_AMBIENTE, IDEAL_PRINCIPIANTES,
    PROFES_ATENTOS, BUENA_UBICACION, MUY_INTENSO, INSTALACIONES_COMODAS).
- **PreguntaService**: crear (auth, máx. 5 preguntas/día por usuario
  contra la base), responder (SOLO el dueño del perfil de la
  actividad), borrar la propia solo si no fue respondida, admin oculta.
  GET público por actividad (visibles). Notificaciones: pregunta nueva
  → publicador; respuesta → autor de la pregunta.
- **Social proof del detalle** suma: `cantidadQuierenProbar`,
  `valoracionPromedio` (null si N<3), `cantidadValoraciones`.
- Reportes: ReporteService acepta los dos tipos nuevos validando
  visibilidad (valoración VISIBLE, pregunta VISIBLE).
- La calificación por aspectos queda **solo diseñada** (columna JSON
  futura sobre `valoracion`), como pide el diseño de fase 8.

## Frontend

- **Detalle**: botón "Quiero probar" junto al contacto (pre-visita;
  la barra social ya tiene 4 acciones y a 375px no entra una quinta);
  al estar marcado, transiciona a "¿Ya la probaste?" → "Ya probé" →
  invitación a valorar. Sección **Valoraciones** (resumen con
  estrellas + distribución + lista con insignia Verificada + tu
  valoración editable + reportar) y sección **Preguntas** (lista +
  form si hay sesión + responder inline si sos el dueño).
- Notificaciones nuevas navegan al detalle (ruta con ancla).
- Métricas del publicador: "quieren probar" en el panel.
- Anónimo: ve todo, y cualquier acción invita a crear cuenta.

## Verificación y deploy

1. Unit tests (señal de uso, verificada, promedio N≥3, upserts,
   permisos de respuesta, rate limit de preguntas) + IT de flujo
   completo en contexto compartido (quiero probar → ya probé →
   valorar verificada → promedio oculto con N<3 → pregunta →
   respuesta del dueño → notificaciones emitidas → reporte de
   valoración).
2. Script 29 (vos, Supabase y local) → ITs → backend (marcador:
   `OPTIONS /api/actividades/1/valoraciones` u otro determinístico) →
   frontend (marcador "Quiero probar" en chunks) → tu smoke.

## Decisiones que pide este plan (con recomendación)

1. **Dónde vive "Quiero probar"**: junto al bloque de contacto del
   detalle (es una acción PRE-visita, como contactar), no en la barra
   social que ya está completa a 375px. "Ya probé" es el mismo control
   transicionando.
2. **Quién puede valorar**: cualquier señal de uso sobre esa
   actividad (guardado, quiero probar, ya probé, entrené acá); la
   insignia "Verificada" queda solo para señal fuerte (ya probé /
   entrené acá). Sin señal, el CTA explica cómo habilitarla. Frena el
   fraude barato sin hacer la función inusable.
3. **Reseña con texto desde V1** (hasta 500): los reportes y el
   ocultar de admin YA están vivos (F2), que era el prerrequisito.
   Publicación directa, moderación por reportes — la filosofía nueva.
4. **Promedio recién con 3+ valoraciones** (las reseñas se listan
   desde la primera); tags de catálogo fijo, sin texto libre en tags.
5. **Q&A minimalista**: una respuesta del publicador por pregunta (en
   la misma fila), borrar la propia solo sin responder, tope de 5
   preguntas por día por usuario.
