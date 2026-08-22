# Plan — Valoraciones etapa A + check-in ("Entrené acá")

Estado: **propuesto, pendiente de aprobación de Agustín**. Implementa
los puntos 4 y 5A de `docs/fase8-diseno-futuro.md`: señales de
confianza en el detalle usando SOLO datos reales que ya existen o que
el usuario genera con un click — sin estrellas, sin texto libre, sin
nada que moderar (etapas B y C siguen gateadas).

## Qué ve la gente al final

- En el detalle de una actividad, junto a Me gusta · Guardar ·
  Compartir, un botón **"Entrené acá"**. Logueado, registra el
  entrenamiento de hoy (una vez por día); anónimo, invita a crear
  cuenta (patrón FavoritoButton).
- Arriba de la descripción, una fila de **social proof** con las
  señales agregadas: "N guardados · N me gusta · N personas entrenaron
  acá este mes". Cada señal aparece SOLO si es mayor que cero (regla
  vigente: nunca un cero falso). Sin nombres: números agregados y
  anónimos.

## Migración (script 26 — la única del bloque)

Tabla `entrenamiento_usuario`:

- `id BIGSERIAL PK`
- `usuario_id BIGINT NOT NULL` FK usuario **ON DELETE CASCADE**
- `actividad_id BIGINT NOT NULL` FK actividad **ON DELETE CASCADE**
- `created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()`
- **Sin UNIQUE** (se puede entrenar muchas veces); la regla "1 por día"
  es del service.
- Índices: `(actividad_id, created_at)` para el contador por ventana y
  `(usuario_id, actividad_id, created_at)` para el rate limit.

Transacción propia, PRE/POST en el script, Supabase y local ANTES del
código (regla 2). Aditiva pura: el código desplegado la ignora.

## Backend

**`POST /api/usuario/checkins/{actividadId}`** (autenticado, ya cubierto
por la regla `/api/usuario/**`):
- Valida que la actividad sea pública (activa + PUBLICADA); 404 si no.
- Si YA hay un check-in del usuario para esa actividad **hoy** (día en
  `America/Argentina/Buenos_Aires`), responde 200 con
  `yaRegistradoHoy=true` — idempotente, sin fila nueva.
- Si no, crea la fila y responde 201 con el contador actualizado.
- El rate limit se valida **contra la base**, no en memoria: Render
  reinicia seguido y en rotación conviven dos instancias — un contador
  en memoria dejaría colar duplicados.

**`GET /api/usuario/checkins/{actividadId}/hoy`** (autenticado): devuelve
`yaRegistradoHoy` para pintar el botón al cargar el detalle logueado.

**Social proof en `GET /api/actividades/{slug}/detalle`** (aditivo, sin
contrato roto): tres campos nuevos en el DTO del detalle —
- `cantidadFavoritos`: count de `favorito_actividad` por actividad.
- `cantidadLikesFotos`: count de `me_gusta_imagen` sobre las imágenes
  APROBADAS y activas de la actividad.
- `cantidadPersonasEntrenaron30Dias`: **count DISTINCT usuario_id** de
  los últimos 30 días.
Es el detalle de UNA actividad: tres counts directos, sin N+1. El
listado público NO cambia (ahí sí sería un fan-out; si algún día se
quiere en cards, va con query agrupada aparte).

## Frontend

- `CheckinButton` en la barra del detalle (junto a MeGustaButton):
  anónimo → invitación a login con returnTo (patrón FavoritoButton);
  logueado → POST, y el estado pasa a "Entrenaste hoy ✓" (también al
  cargar, vía el GET). Errores con el patrón de siempre (401 → login).
- Fila de social proof en el detalle (server component, los datos
  vienen en el mismo fetch del detalle): chips u oraciones cortas, solo
  las señales > 0; si todas son cero, la fila no se renderiza.
- Sin pantalla de historial en V1: el check-in se registra (queda para
  el feed futuro) pero no hay vista propia todavía.

## Verificación y deploy

1. Unit tests del service (regla 1/día, actividad no pública, distinct
   del contador) + IT en el contexto existente que corresponda (crear
   check-in real, idempotencia del mismo día, contador en el detalle
   público) — mismos valores de `@TestPropertySource` compartidos.
2. Script 26 en Supabase y local (Agustín) → ITs → deploy backend
   (marcador anónimo determinístico: `OPTIONS /api/usuario/checkins/1`
   responde `Allow` con POST solo en el build nuevo; regla 12 antes del
   deploy) → deploy frontend (marcador "Entrené acá" en chunks) →
   smoke de Agustín (check-in real, idempotencia al segundo click, el
   contador sube en el detalle, anónimo invitado a crear cuenta).

## Decisiones que pide este plan (con recomendación)

1. **Qué promete el contador** (el gate del diseño): recomiendo
   "**N personas entrenaron acá este mes**" = usuarios DISTINTOS en los
   últimos 30 días. Es honesto (personas, no clicks), resistente al
   spam del botón (el mismo usuario cuenta una vez) y fresco (un club
   activo lo muestra, uno muerto no arrastra números viejos).
2. **Regla del día**: 1 check-in por actividad por día, validado contra
   la base, con el día calculado en zona horaria argentina (no UTC: a
   las 22:00 de acá ya es "mañana" en UTC y permitiría el doble).
3. **Privacidad**: números agregados y anónimos en todas las
   superficies; nada de nombres sin un opt-in de perfil público que hoy
   no existe (regla del diseño, la dejo escrita en el código).
4. **Social proof solo positivo**: cada señal se muestra únicamente si
   es > 0, y NO hay promedio ni estrellas — la etapa B (estrellas)
   sigue gateada a tu decisión de producto con más masa de usuarios.
