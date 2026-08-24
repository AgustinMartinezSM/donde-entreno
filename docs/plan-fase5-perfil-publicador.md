# Plan — Fase 5 social: el perfil del publicador como perfil de verdad

Estado: ✅ **CERRADA EN PRODUCCIÓN** (smoke de Agustín OK, 2026-08-24).
`main` = `origin/main` = **`8118a36`**; script 31 aplicado por él en
Supabase y local antes del deploy; backend `cfc36e0` y frontend
`8118a36` desplegados en dos tandas con marcadores anónimos
verificados (404→200 en el GET de valoraciones del perfil, y el DTO
trayendo los cuatro stats). Suite unit + ITs en verde con el script
aplicado (incluido `PerfilPublicadorSocialIT`, 5 flujos).

**Trampa de verificación que dejó esta fase**: el primer marcador del
frontend buscaba un string en los **chunks JS**, y
`/publicadores/[id]` es un **server component** — sus textos viajan en
el HTML, no en el bundle del cliente. El segundo intento (contar
ocurrencias de un texto que debía desaparecer) también falló: dio un
FALSO NEGATIVO porque cada frase aparece **dos veces** en la respuesta
—una en el HTML renderizado y otra en el payload RSC embebido—, así
que el conteo esperado estaba mal calibrado y el deploy parecía no
haber salido cuando ya estaba vivo. Para server components, verificar
con un string que **exista** en el HTML nuevo (o con un componente
`"use client"` de la fase, que sí va a los chunks).

Notas de la implementación, sobre lo planeado:

- El tab Opiniones **aparece solo si hay contenido**, igual que Fotos:
  una solapa vacía es una promesa rota. `resolverTab` cae en
  Actividades si alguien entra por URL a una solapa que no aplica.
- Las destacadas van **arriba** del listado y se filtran de abajo, para
  que no aparezcan dos veces en la misma pantalla.
- El selector del panel elige sobre **las actividades de la página
  actual**: con más de una página hay que navegar. Aceptable mientras
  el catálogo es chico; anotado por si crece.
- `MINIMO_PARA_PROMEDIO` pasó de privada a pública en
  `ValoracionService` en vez de duplicar el 3.
- La resolución de IP (`X-Forwarded-For`) se mudó de
  `InteraccionesController` a `LimitadorInteracciones`: ahora la
  comparten los dos endpoints de tracking en vez de duplicarla.

La Fase 4 hizo que las fotos fueran sociales. Esta fase hace que el
**perfil que las contiene** deje de ser una ficha de contacto y pase a
ser el lugar donde alguien decide si entrena ahí: números reales,
opiniones de todas sus actividades juntas, lo que el publicador quiere
mostrar primero, y el WhatsApp midiendo conversión como en el detalle.

## Qué ve la gente al final

1. **Una cabecera con números reales**: actividades · seguidores ·
   fotos · valoración promedio. Hoy hay tres chips sueltos y el único
   contador nativo del backend es `cantidadSeguidores`.
2. **Un tab "Opiniones"** con las valoraciones de TODAS sus
   actividades juntas, con el promedio del publicador y la actividad
   de cada reseña. Hoy las valoraciones existen solo dentro de cada
   actividad: quien mira el perfil no ve ninguna.
3. **Actividades destacadas**: el publicador elige hasta 3 que van
   primero en su perfil. Hoy el orden es el de la búsqueda.
4. **Preguntas visibles en el perfil** (las respondidas de sus
   actividades): la prueba social más barata que ya tenemos cargada.
5. **El WhatsApp del perfil midiendo** igual que el del detalle.
6. **El checklist de calidad**, que hoy vive escondido en el Centro de
   fotos, mostrado como progreso del perfil (solo para el dueño).

## Estado real hoy (auditado en código, no de memoria)

**Backend**

- `PerfilPublicadorDTO` tiene: id, nombre, tipoPublicador, descripcion,
  emailContacto, telefonoContacto, whatsapp, instagram, sitioWeb,
  verificado, **cantidadSeguidores**, slug, logoUrl. **No tiene**
  cantidad de actividades, de fotos, promedio ni cantidad de opiniones.
  Los dos últimos campos agregados (`cantidadSeguidores`, `logoUrl`)
  entraron **por setter, fuera del constructor**, con query agrupado en
  los listados: ese es el patrón a seguir.
- `ValoracionRepository`: todas sus queries agrupan por
  **`actividadId`**. No hay nada por publicador. `Valoracion` guarda
  `actividadId` **plano** (sin relación JPA), así que el agregado por
  publicador se resuelve con un join explícito contra `Actividad`.
- `PreguntaActividadRepository`: idem, solo por actividad.
- `Actividad` **no tiene ningún campo** de destacada/orden/prioridad
  → es lo único de esta fase que necesita migración.
- `FeedController` expone `GET /api/usuario/feed/actividades`:
  privado, multi-publicador, para el usuario logueado. No sirve como
  "novedades de este publicador".
- `PublicadorMetricasDTO` (panel privado) ya trae vistas30Dias,
  contactosWhatsapp30Dias y quierenProbar. No tiene promedio ni fotos.
- Solo hay **tres** endpoints públicos de perfil: el listado, el
  detalle por id-o-slug y sus imágenes. Las actividades del publicador
  salen del catálogo general con `?perfilPublicadorId=`.
- Deuda menor detectada de paso:
  `ActividadService.obtenerActividadesPorPerfilPublicador` es **código
  muerto** (ningún caller). Se borra en esta fase.

**Frontend** (`app/publicadores/[id]/page.tsx`, server component)

- Tabs por query param `?tab=` con redirect canónico id→slug:
  **Actividades · Fotos · Info**.
- La cabecera muestra chips: "N actividades publicadas" (sale del
  `totalElementos` de la búsqueda), "N seguidores" (solo si > 0) y
  "Perfil verificado". No hay fila de stats.
- **El `ContactButton` del perfil no recibe `actividadId`, y sin eso su
  `onClick` queda `undefined`: el click de WhatsApp desde el perfil no
  registra NADA.** El detalle de actividad sí lo pasa, en sus dos
  botones. Lo mismo con `CompartirButton`.
- El texto vacío del `ContactButton` dice "Esta actividad todavía no
  cargó un canal de contacto": mal redactado en un perfil.
- La página hace hasta **10 fetches** `no-store` por vista, porque las
  fotos del perfil se juntan con un fan-out de una llamada por
  actividad (deuda ya documentada en el propio archivo).
- El markup de avatar/nombre/verificado está **duplicado**: existe
  `PublisherIdentity` y el perfil no lo usa.
- `SocialProofFila` **no es un componente**: es una función local
  dentro de la página de detalle.
- El checklist de 6 pasos ya existe en `CentroDeFotos.tsx`, calculado
  100% en el cliente sobre datos ya disponibles.

## Migración (script 31 — una columna, una transacción)

```sql
ALTER TABLE actividad
    ADD COLUMN destacada_orden SMALLINT NULL;

CREATE INDEX idx_actividad_destacada
    ON actividad (perfil_publicador_id, destacada_orden)
    WHERE destacada_orden IS NOT NULL;
```

- `NULL` = no destacada (todas las filas existentes). 1..3 = posición.
- **Sin CHECK que enumere valores**: el rango se valida en el service.
  (La lección del script 25 es sobre CHECKs de catálogo; acá un CHECK
  `BETWEEN 1 AND 3` obligaría a migrar si mañana querés 5 destacadas.)
- **Ojo `SMALLINT`**: la entidad necesita
  `@JdbcTypeCode(SqlTypes.SMALLINT)` sobre el `Integer` o
  `ddl-auto=validate` no arranca (lección del script 29, costó 88 ITs
  muertos a los 0s).
- Índice parcial: solo indexa las destacadas, que son 3 por publicador.

## Backend

1. **Stats del perfil, aditivos por setter** en `PerfilPublicadorDTO`:
   `cantidadActividades`, `cantidadFotos`, `valoracionPromedio`
   (`Double`, null con menos de 3 — **la misma regla que ya rige en el
   detalle**, `MINIMO_PARA_PROMEDIO = 3`) y `cantidadValoraciones`.
   - El hook ya existe: `PerfilPublicadorService.enriquecerDetalle`
     para el detalle y `mapearConSeguidores` (GROUP BY batch) para los
     listados. Ahí se inyectan, con **queries agrupadas** y nunca por
     fila: el listado de publicadores ya sufrió N+1 una vez.
   - **El count de actividades sale gratis**: `ActividadRepository`
     ya tiene `countByPerfilPublicador_IdAndActivaTrueAndEstadoPublicacion...`
     (hoy lo usa solo el panel privado). Deja de hacer falta el
     workaround del frontend de leer el `total` de la búsqueda.
   - El **count de fotos por perfil no existe** (`ImagenRepository`
     cuenta por actividad y por estado de moderación, no por perfil):
     es query nueva. Criterio propuesto: fotos visibles del perfil
     **más** las de sus actividades, que es lo que el tab Fotos ya
     muestra — así el número de la cabecera y el del tab coinciden.
2. **`GET /api/perfiles-publicadores/{idOSlug}/valoraciones`**
   (público, paginado): las valoraciones VISIBLES de todas sus
   actividades, cada una con el título y slug de la actividad, más el
   resumen (promedio + cantidad + distribución). Reusa el DTO de
   valoración agregándole el contexto de actividad.
3. **`GET /api/perfiles-publicadores/{idOSlug}/preguntas`** (público):
   las preguntas **respondidas** de sus actividades, con su actividad.
   Solo respondidas: una pregunta sin responder en el perfil es una
   mala señal, y en el detalle ya se ven todas.
4. **`GET /api/perfiles-publicadores/{id}/fotos`** (público): todas las
   fotos visibles del publicador (perfil + actividades) en **un** query.
   Mata el fan-out de 6 requests y habilita la grilla única que la
   Fase 4 dejó pendiente.
5. **Destacadas**: `PUT /api/publicador/actividades/destacadas` con la
   lista ordenada de ids (máx. 3, todas del publicador del token, todas
   PUBLICADAS). Valida y reescribe `destacada_orden` en una
   transacción, igual que el orden de imágenes de la Fase 2. El listado
   público del perfil las devuelve primero.
6. **Interacción a nivel perfil**: hoy `evento_interaccion` cuelga de
   una actividad. Para medir el WhatsApp del perfil, **sin migración**:
   registrar el evento contra la **actividad destacada/primera** del
   publicador desvirtúa el dato. Ver decisión 4.

## Frontend

- **Cabecera nueva**: fila de stats (Actividades · Seguidores · Fotos ·
  ★ Promedio), cada uno navegando a su tab; se omite el que no tenga
  dato (nunca un cero falso, regla ya vigente con seguidores).
  Reutilizar `PublisherIdentity` en vez del markup duplicado.
- **Tab "Opiniones"** con el resumen (promedio + distribución) y la
  lista, cada reseña linkeando a su actividad. Debajo, las preguntas
  respondidas.
- **Destacadas** primero en el tab Actividades, con su chip.
- **Panel del publicador**: elegir destacadas (hasta 3) desde su
  listado de actividades.
- **Checklist de presencia** movido a un componente compartido y
  mostrado también en el perfil **solo si lo mira su dueño**.
- **`ContactButton` con contexto de perfil**: mensaje prellenado con el
  nombre del publicador, texto vacío corregido, y tracking (ver
  decisión 4).
- Extraer `SocialProofFila` a `components/social/`.

## Verificación y deploy

1. Unit (agregados por publicador, regla del promedio con N<3,
   validación de destacadas: ajenas, no publicadas, más de 3,
   duplicadas) + IT de flujo completo en contexto compartido.
2. Script 31 (vos) → ITs → **backend primero** (marcador: el GET de
   valoraciones del perfil pasa de 401 a 200) → frontend → tu smoke.

## Decisiones que pide este plan (con recomendación)

1. **El tab "Novedades" NO entra en esta fase.** El roadmap ya pone la
   tabla `feed_event` en la Fase 6 y los **canales de novedades**
   (posts que el publicador escribe) en la Fase 8. Un "Novedades" V1
   acá sería o una tabla que la Fase 6 va a reemplazar, o una lista de
   "publicó una actividad nueva" que es exactamente lo que ya muestra
   el tab Actividades ordenado por fecha. **Recomiendo cambiarlo por
   "Opiniones + Q&A"**, que es contenido real ya cargado y que hoy
   nadie ve desde el perfil.
2. **Promedio del publicador con la misma regla que la actividad**
   (null con menos de 3 valoraciones): si el perfil mostrara promedio
   con 1 sola reseña, el número del perfil y el de la actividad se
   contradirían en pantalla. Recomiendo mantener el umbral en 3
   contando **todas** sus valoraciones sumadas.
3. **Destacadas: máximo 3 y manuales.** Con 7 actividades en toda la
   plataforma, más de 3 destacadas no destaca nada. Recomiendo 3.
4. **Tracking del WhatsApp del perfil**: el contrato actual
   (`/api/actividades/{id}/interacciones`) es por actividad y meterle
   el click del perfil ensuciaría las métricas por actividad, que el
   publicador ya está viendo. Recomiendo **agregar el tipo de objeto al
   evento** — `evento_interaccion` ya tiene `tipo`, así que sumar
   `PERFIL_PUBLICADOR` es **una columna nullable** (`perfil_publicador_id`)
   en el mismo script 31, y el panel gana "contactos desde tu perfil"
   separado de los de cada actividad. La alternativa (no medirlo) deja
   ciego justo el botón más importante del perfil.
5. **El endpoint de fotos del publicador entra en esta fase** aunque
   sea deuda vieja: sin él, cada vista del perfil son hasta 10 fetches
   y la fase agrega dos llamadas más. Recomiendo incluirlo.
