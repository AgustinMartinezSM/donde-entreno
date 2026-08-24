# Plan — Fase 6 social: el feed que se puede seguir leyendo

Estado: **aprobado ("Dale, con las 5 recomendaciones") e IMPLEMENTADO**
(backend `58121b8`, frontend `1737115`). Script 32 aplicado por Agustín
en Supabase y local; suite unit + ITs en verde con el script aplicado;
typecheck y lint limpios. Falta: deploy en dos tandas y su smoke.

## ⚠️ El hallazgo de la fase: REQUIRES_NEW no sirve para emitir eventos

La emisión arrancó copiada de `NotificacionService`
(`@Transactional(REQUIRES_NEW)` + `try/catch`), y **rompió la subida
de fotos con un 500** — lo destaparon los ITs.

La causa: el evento referencia por FK a la actividad/imagen/perfil que
la transacción de negocio **todavía no confirmó**. `REQUIRES_NEW` abre
una transacción nueva, o sea **otra conexión**, que por aislamiento no
ve esas filas → viola la FK. Y lo peor: esa violación explota al hacer
**commit** de la transacción paralela, es decir **fuera** del
`try/catch` del método, así que el "best-effort" no atrapaba nada.

Por qué `NotificacionService` sí funciona con ese patrón: sus filas
guardan `usuario_id` de usuarios que **ya existían** antes de la
transacción. El feed es el primer caso del proyecto que referencia
filas **creadas en la misma transacción**.

La corrección: emitir en `afterCommit`
(`TransactionSynchronizationManager`). Ahí las filas referenciadas ya
están confirmadas, sigue siendo best-effort, y si el negocio hace
rollback el evento simplemente no se emite — que es lo correcto. El
guardado no lleva `@Transactional` porque se llama desde el mismo bean
(el proxy no lo interceptaría igual) y `saveAndFlush` abre su propia
transacción cuando no hay ninguna activa, con el commit **dentro** de
la llamada, o sea dentro del `try`.

Hay un test que fija la conducta nueva: con una transacción en curso,
`emitir` **no** guarda todavía, difiere al commit.

Hoy el feed de seguidos muestra **20 actividades y se termina**: no
hay forma de pedir más, y solo cuenta un tipo de hecho ("se publicó
una actividad"). Esta fase lo convierte en un feed de **eventos**
paginado —foto nueva, actividad nueva, cambio aprobado— y hace reales
dos bloques de la home que hoy son de mentira o de nada.

## Qué ve la gente al final

1. **El feed sigue cuando llegás al fondo**: "Ver más" que trae la
   página siguiente en vez de cortar en 20.
2. **El feed cuenta más cosas**: "subió 3 fotos nuevas", "publicó una
   actividad", "actualizó su actividad" — no solo altas.
3. **"Lo más buscado" real**: hoy la home tiene una sección
   *Deportes populares* con **seis deportes hardcodeados**; pasa a
   salir del tracking de la Fase 2.
4. **La home deja de pedir dos veces lo mismo**: hoy un usuario
   logueado que abre Home y después `/mi-cuenta` dispara **dos
   llamadas idénticas** al feed, porque `HomeFeedSeguidos` duplica el
   fetch en vez de usar el hook que ya existe.

## Estado real hoy (auditado en código)

**Backend**

- `GET /api/usuario/feed/actividades` devuelve una **`List` plana**,
  sin `page`/`size` ni metadata, con tope `TAMANIO_FEED = 20`
  (`SeguimientoPublicadorService:40`, aplicado en `PageRequest.of(0,
  TAMANIO_FEED)` en la línea 140). No hay por dónde pedir la página 2.
- El feed se arma con **un query sobre `actividad`** filtrando por los
  perfiles seguidos y ordenando por `createdAt DESC`. O sea: el "feed"
  es en realidad "las últimas actividades", no una línea de tiempo de
  hechos.
- **Los puntos de emisión ya existen y están identificados**: donde
  hoy se emite una notificación, ahí mismo va el evento de feed. El
  caso canónico es `SolicitudPublicacionAdminService:223`, que ya hace
  `emitirATodos(usuarioIdsSeguidoresDe(perfilId), "ACTIVIDAD_NUEVA",
  ...)` — el fan-out a seguidores ya está resuelto.
- `NotificacionService.emitir/emitirATodos` es el patrón a replicar:
  `@Transactional(REQUIRES_NEW)` + `try/catch` que traga y loguea. Un
  evento de feed que falla **no puede voltear** la aprobación de una
  actividad.
- `PaginaResponseDTO` es el contrato paginado del proyecto (8 services
  lo construyen igual) y el tipo del frontend (`PaginaActividades`) ya
  trae **`ultima`**, que es exactamente lo que necesita un "Ver más".
- **Paginar el feed actual no necesita migración**: el repo ya acepta
  `Pageable` (`ActividadRepository:176`) y trae `@EntityGraph` para
  evitar N+1; lo único hardcodeado es el `PageRequest.of(0, 20)` del
  service. O sea: la deuda de paginación se puede saldar aunque la
  tabla de eventos no existiera.
- **No hay recomendador de contenido**: los únicos hits de "recomendar"
  viven en el asistente (Dondi), que sugiere **deportes** a partir del
  texto de la charla — cero personalización por historial, favoritos o
  seguidos. Lo más cercano a señal social es `SocialProofService`, que
  es de UNA actividad, no un ranking.

**Frontend**

- La home tiene **12 secciones**; las relevantes acá:
  - `HomeFeedSeguidos` (client): una sola llamada, sin paginación,
    `null` para anónimos.
  - `HomeDiscoveryFeed` (server): 6 actividades por SSR, sin paginar.
  - **`HomePopularSports`: array hardcodeado de 6 deportes**
    (líneas 6-47), sin ninguna métrica detrás.
- **Duplicación confirmada y sustancial**: `HomeFeedSeguidos:37-62` es
  el mismo `useEffect` que `useFeedNovedades:25-50` (mismo guard, mismo
  then/catch); `EsqueletoCard` está copiado en dos archivos; el bloque
  "Lo nuevo de quienes seguís" está escrito dos veces; y el estado
  vacío se resuelve distinto en cada lado (la home sugiere
  publicadores **sin filtrar los que ya seguís**, `/mi-cuenta` sí los
  filtra). Paginar sin consolidar esto obliga a escribirlo dos veces.
- **No existe scroll infinito en ningún lado**; los patrones son SSR
  con links (`explorar/Pagination.tsx`) o client con estado que
  **reemplaza** la página (`PublicadorActividadesList`).

## Migración (script 32 — una tabla, una transacción)

```sql
CREATE TABLE feed_event (
    id BIGSERIAL PRIMARY KEY,
    tipo VARCHAR(40) NOT NULL,          -- catálogo ABIERTO (sin CHECK)
    perfil_publicador_id BIGINT NOT NULL,
    actividad_id BIGINT NULL,
    imagen_id BIGINT NULL,
    resumen VARCHAR(200) NULL,          -- texto ya armado por el backend
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_feed_event_perfil FOREIGN KEY (perfil_publicador_id)
        REFERENCES perfil_publicador (id) ON DELETE CASCADE,
    CONSTRAINT fk_feed_event_actividad FOREIGN KEY (actividad_id)
        REFERENCES actividad (id) ON DELETE CASCADE,
    CONSTRAINT fk_feed_event_imagen FOREIGN KEY (imagen_id)
        REFERENCES imagen (id) ON DELETE CASCADE
);

CREATE INDEX idx_feed_event_perfil_fecha
    ON feed_event (perfil_publicador_id, created_at DESC);
CREATE INDEX idx_feed_event_fecha
    ON feed_event (created_at DESC);
```

- **`tipo` sin CHECK a propósito**: cada fase futura suma tipos
  (novedad del canal en la 8, evento en la 9). La lección del script
  25 es justamente que un CHECK que enumera obliga a migrar por cada
  valor nuevo; el catálogo se valida en el service.
- `ON DELETE CASCADE` en las tres FK: si se borra la actividad, su
  evento se va con ella y el feed no queda con huecos.
- Los dos índices cubren los dos accesos: por publicador (perfil) y
  global por fecha (home).
- **La tabla nace vacía**: el feed de seguidos actual sigue
  funcionando igual hasta que haya eventos. Ver decisión 1.

## Backend

1. **Entidad + repo + `FeedEventService`** con `registrar(...)`
   best-effort en `REQUIRES_NEW` y `try/catch`, calcado de
   `NotificacionService`. Un fallo del feed nunca voltea la
   transacción del hecho real.
2. **Emisión en los puntos que ya emiten notificación**: actividad
   aprobada (`SolicitudPublicacionAdminService`), foto publicada
   (`ImagenPublicadorService`, fase 4), cambio de actividad aprobado
   (`SolicitudCambioActividadAdminService`). Mismo lugar, misma línea.
3. **`GET /api/usuario/feed?page=&size=`**: los eventos de los
   publicadores que sigo, paginado con `PaginaResponseDTO`. Cada item
   viaja con lo necesario para pintarlo **sin llamadas extra**: perfil
   (nombre, slug, logo), actividad (título, slug, imagen principal) e
   imagen si aplica — todo con queries agrupadas, sin N+1.
   - El molde exacto es `NotificacionService.listar` (líneas 110-129):
     misma forma (lista propia del usuario + `OrderByCreatedAtDesc` +
     `Page<>` → `PaginaResponseDTO<>`), incluido el saneo inline
     `Math.min(Math.max(size, 1), MAX_PAGINA)` con tope 50, que nunca
     lanza por parámetros feos.
   - **Los 4 enganches, por esfuerzo real**: actividad aprobada
     (`SolicitudPublicacionAdminService:228`, todo servido) y seguir a
     un publicador (`SeguimientoPublicadorService:94`, idempotente por
     construcción) son directos; cambio aprobado
     (`SolicitudCambioActividadAdminService:242`) necesita resolver
     los seguidores (ahí hoy solo se avisa al publicador); y la foto
     (`ImagenPublicadorService:130` y `:183`) es la más cara porque
     **esa clase todavía no inyecta ningún service de emisión**.
4. **El endpoint viejo (`/feed/actividades`) NO se toca**: lo usan la
   home y `/mi-cuenta` hoy. Se deprecia cuando el frontend nuevo esté
   desplegado (regla de los dos pushes).
5. **`GET /api/deportes/populares?dias=30`** (público): los deportes
   con más `VISTA_DETALLE` en la ventana. **No necesita migración**:
   los datos ya están y el índice
   `idx_evento_interaccion_actividad (actividad_id, tipo, created_at)`
   cubre el query. Falta una sola JPQL nueva con `ORDER BY COUNT(e)
   DESC` + `Pageable` (las dos que existen agrupan pero no ordenan ni
   limitan, y exigen los ids por adelantado, que un ranking global no
   tiene).
   - **⚠️ Trampa que dejó la Fase 5**: al hacer `actividad_id`
     nullable para los eventos de perfil, cualquier ranking por
     actividad necesita **`AND e.actividadId IS NOT NULL`** o los
     clicks del perfil se cuelan en el conteo. Es un bug silencioso:
     no falla, solo miente.
   - La ventana hoy es una constante privada
     (`InteraccionService.DIAS_VENTANA_METRICAS = 30`); hay que
     parametrizarla para aceptar 7 o 30.

## Frontend

- **Consolidar primero, paginar después**: `HomeFeedSeguidos` pasa a
  usar `useFeedNovedades`, `EsqueletoCard` sale a componente
  compartido, y el estado vacío usa el mismo criterio en los dos
  lados (filtrando los ya seguidos, como hace `/mi-cuenta`).
- **`useFeedPaginado`**: hook nuevo que **acumula** páginas (el patrón
  B del proyecto reemplaza, acá hay que sumar) y expone
  `{ items, cargando, hayMas, cargarMas }`. Botón "Ver más" explícito,
  no scroll infinito: es más simple, accesible y no rompe el botón
  "volver arriba" que ya vive flotando.
- **Card de evento** que interpreta el tipo: foto nueva muestra la
  foto, actividad nueva la card de actividad. Todo termina en una
  acción útil (regla de la etapa).
- **`HomePopularSports` deja de ser hardcodeada** y se alimenta del
  endpoint nuevo. Si el endpoint no responde o no hay datos
  suficientes, **la sección no se dibuja** (nunca un ranking inventado).

## Verificación y deploy

1. Unit (emisión best-effort que no rompe el flujo, paginación,
   ranking con ventana) + IT de flujo completo: aprobar actividad →
   evento emitido → aparece en el feed del seguidor → página 2 →
   `ultima=true`.
2. Script 32 (vos) → ITs → **backend primero** (marcador: `GET
   /api/usuario/feed` pasa de 404 a 401 anónimo) → frontend → tu smoke.

## Decisiones que pide este plan (con recomendación)

1. **Backfill del feed: SÍ, pero mínimo.** Si la tabla nace vacía, el
   feed queda en blanco el día del deploy para todos —incluso para
   quien sigue a alguien— y parece roto. Recomiendo **sembrar en el
   script un evento `ACTIVIDAD_NUEVA` por cada actividad publicada
   existente**, con su `created_at` real: son 7 filas hoy, el feed
   arranca con contenido y el orden cronológico queda correcto. Es un
   INSERT ... SELECT de una línea.
2. **"Ver más" en vez de scroll infinito.** Con el volumen actual, el
   scroll infinito agrega complejidad (IntersectionObserver, foco,
   restauración de posición) sin beneficio, y el proyecto no lo usa en
   ningún lado. Recomiendo el botón.
3. **Consolidar la duplicación de la home ANTES de paginar.** Es lo
   que evita escribir la paginación dos veces y de paso saca la
   segunda llamada idéntica al backend. Cuesta poco y hay que hacerlo
   igual.
4. **La sección "populares" se calcula sobre 30 días y solo se muestra
   con datos suficientes.** Con tráfico bajo, un top-6 de 7 días puede
   estar armado con dos clicks y miente. Recomiendo ventana de 30 días
   y **mínimo 3 deportes con al menos una vista**; si no llega, la
   sección no aparece (no volver al hardcodeo).
5. **El feed sigue siendo solo de publicadores seguidos** (no un feed
   global de descubrimiento). La home ya tiene descubrimiento con
   `HomeDiscoveryFeed` y recomendaciones; mezclar todo en un solo
   stream haría el bloque menos legible y borraría el valor de seguir.
   Recomiendo mantener la separación.
