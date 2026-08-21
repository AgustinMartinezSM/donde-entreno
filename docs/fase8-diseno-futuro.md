# Fase 8 — Diseño futuro documentado (sin código)

Cierre del bloque "contenido visual, perfiles customizables, login UX y
modo oscuro V1" (`docs/bloque-contenido-visual-v1.md`). Esta fase no
implementa nada: deja **diseñado y priorizado** lo que quedó fuera del
bloque por necesitar migración, decisión de producto o infraestructura
que hoy no existe. Regla vigente: **nada de esto se implementa sin
pedido explícito** (criterio del roadmap).

Base: el diagnóstico de Fase 0 (sección A del doc del bloque) y el
feedback real registrado (`docs/feedback.md`: "le falta más imágenes";
mejoras candidatas: "referencias, valoraciones o información de
contacto más visible en el detalle").

---

## 1. Likes en fotos

**Qué es**: corazón por foto en el visor (LightboxFotos, fase 4) y en
las galerías, con contador público. Es la señal social más barata de
producir y la que más alimenta al publicador.

**Estado actual**: `meGusta.ts` es local-only y por ACTIVIDAD, no por
foto — no hay nada por imagen ni nada sincronizado.

**Diseño**:
- **Migración nueva** (script 21+): tabla `me_gusta_imagen` con el
  patrón exacto de favoritos — `usuario_id` (FK ON DELETE CASCADE),
  `imagen_id` (FK ON DELETE CASCADE), `created_at`, UNIQUE
  `(usuario_id, imagen_id)`.
- Endpoints (patrón sync de favoritos, `/api/usuario/**` autenticado):
  `PUT /api/usuario/likes-fotos/{imagenId}` (idempotente),
  `DELETE .../{imagenId}` (idempotente), y el contador público viaja
  DENTRO de los DTOs de imágenes existentes (`cantidadLikes` +
  `meGusta` si hay sesión) — sin endpoint público nuevo, sin N+1
  (query agrupada como el contador de seguidores).
- UI: corazón en el pie del visor y en la esquina de cada card de
  galería; anónimo → invitación a crear cuenta (patrón FavoritoButton).
- **Regla**: solo sobre imágenes APROBADAS y activas; un like sobre una
  imagen que luego se desactiva no se borra (patrón favoritos: se omite
  del conteo mientras no sea pública).

**Riesgos**: bajo. **Estimación**: chica (una migración + patrón ya
probado dos veces).

## 2. Secciones de galería reales (Instalaciones / Entrenamientos / Eventos)

**Qué es**: que el publicador organice sus fotos en secciones con
nombre, como pestañas dentro de la galería del perfil y del detalle.

**Estado actual**: el concepto no existe — `tipo_imagen` solo separa
PRINCIPAL/GALERIA/LOGO/PORTADA y el `orden` es plano (fase 2).

**Diseño recomendado** (la opción barata y suficiente):
- **Migración**: columna `imagen.seccion VARCHAR(30) NULL` con CHECK
  sobre un **catálogo fijo** (INSTALACIONES, ENTRENAMIENTOS, EVENTOS,
  EQUIPO) — NULL = "General" (todo lo existente queda ahí, sin
  backfill). La alternativa de tabla `seccion_galeria` por perfil (
  nombres libres) se descarta en V1: nombres libres = moderación de
  texto nueva, y cuatro secciones fijas cubren el caso real.
- Backend sin contrato roto: `PATCH .../imagenes/{id}` (fase 2) acepta
  `seccion`; los listados públicos exponen el campo; el orden sigue
  siendo global (no por sección) para no rehacer el drag de fase 2.
- UI: chips-filtro arriba de la grilla (Todo · Instalaciones · ...),
  solo si hay 2+ secciones en uso; el gestor del publicador suma un
  select por foto junto al editor de texto de fase 2.

**Riesgos**: bajo; el CHECK fija el dominio. **Estimación**: media.

## 3. Videos

**Qué es**: video corto en la galería de la actividad (el formato que
más convierte en deporte).

**Dos caminos, con recomendación clara**:
- **V1 recomendado — embeds**: campo `video_url` (YouTube/Instagram/
  Vimeo) en la actividad, validado por dominio con allowlist, mostrado
  como slide más del carrusel (thumbnail + play → iframe). **Sin
  storage, sin transcoding, sin moderación de binarios**: la moderación
  es la misma revisión humana del texto (la URL pasa por solicitud de
  cambio). Migración mínima: una columna.
- **V2 — subida nativa**: exige límites duros (30-60s, ~50 MB),
  transcoding (no lo hay), moderación de video (mucho más cara que la
  de imágenes) y presupuesto de Storage/egress en Supabase. **No
  encararlo hasta que los embeds queden cortos.**

**Gate**: decidir allowlist de dominios. **Estimación V1**: chica-media.

## 4. "Estoy entrenando acá" (check-in)

**Qué es**: botón en el detalle para marcar presencia; alimenta un
contador social ("12 personas entrenan acá") y, a futuro, el feed.

**Diseño**:
- **Migración**: tabla `entrenamiento_usuario` — `usuario_id`,
  `actividad_id`, `created_at`; sin UNIQUE (se puede entrenar muchas
  veces) pero con **rate limit** (1 check-in por actividad por día,
  validado en service).
- **Privacidad primero**: el contador público es AGREGADO y anónimo
  ("N personas entrenaron acá este mes"); el detalle de QUIÉN solo lo
  ve el propio usuario en su historial. Nada de mostrar nombres sin un
  opt-in explícito de perfil público de usuario (que hoy no existe).
- UI: botón secundario junto a Me gusta/Guardar/Compartir en el detalle.

**Gate**: definición de producto (¿qué promete el contador?) antes de
tocar nada. **Estimación**: media.

## 5. Valoraciones / referencias

**Qué es**: lo que pidió el feedback real — señales de confianza en el
detalle. Es lo más sensible de esta lista: una valoración mala es un
conflicto con el publicador, y una falsa es un fraude.

**Diseño por etapas, cada una con valor propio**:
- **Etapa A (sin migración de opinión)**: subir señales que YA existen
  — favoritos totales, likes (punto 1), check-ins (punto 4) — como
  "social proof" en el detalle. Cero riesgo de abuso textual.
- **Etapa B (estrellas sin texto)**: tabla `valoracion` — `usuario_id`,
  `actividad_id`, `puntaje 1..5`, UNIQUE por par, editable. **Solo
  cuentas con señal de uso real** (favorito o check-in previo sobre esa
  actividad) para frenar el fraude barato. Promedio visible recién con
  N≥3 (un 1 solitario no puede hundir a un club).
- **Etapa C (reseñas con texto)**: **NO sin moderación previa** — misma
  regla que comentarios (abajo). El circuito de moderación de imágenes
  (PENDIENTE→APROBADA/RECHAZADA + panel admin) es el molde a clonar.

**Gate**: decisión de producto sobre B (¿estrellas ya, o esperar más
masa de usuarios?). **Estimación**: A chica, B media, C grande.

## 6. Comentarios — explícitamente NO

**Regla del bloque, ratificada**: no hay comentarios de texto libre en
ninguna superficie hasta que exista un circuito de moderación de texto
(cola + panel + política). El primer texto libre que se acepte va a ser
el primer insulto publicado con el logo de la plataforma arriba. Cuando
se encare: clonar el circuito de moderación de imágenes, que ya probó
funcionar de punta a punta.

---

## Orden recomendado (si se piden)

1. **Likes en fotos** (1) — barato, patrón probado, alimenta el feed.
2. **Valoraciones etapa A** (5A) — cero migración de opinión, sube
   confianza con lo que ya hay.
3. **Secciones de galería** (2) — cuando haya volumen real de fotos.
4. **Video por embed** (3) — cuando algún publicador lo pida.
5. **Check-in** (4) y **estrellas** (5B) — con más masa de usuarios.

## Dependencias transversales

- **Avatar de usuario** (5d del bloque, sigue gateada): el CHECK XOR de
  dueño en `imagen` bloquea reusar la tabla; la vía barata es columna
  `usuario.avatar_url` con moderación diferida. Likes/check-ins/
  valoraciones se ven mejor con avatar, pero ninguno lo requiere.
- **Email saliente** (gates 5b/5c): nada de esta fase lo necesita.
- Toda migración nueva sigue las reglas de producción: script
  versionado, transacción propia, PRE/POST, Supabase antes que código.
