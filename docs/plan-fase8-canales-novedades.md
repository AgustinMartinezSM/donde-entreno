# Plan — Fase 8 social: canales de novedades (y solo eso)

Estado: **propuesto, pendiente de aprobación de Agustín**.

La Fase 8 del roadmap son **cuatro features** (canales de novedades,
inbox de consultas, grupos por actividad, chat libre) más una landing
social. Este plan propone hacer **la primera** y dejar el resto para
fases propias, con el fundamento abajo.

## Por qué recortar (y no es pereza)

El propio roadmap las ordena y marca la primera como "el más seguro:
solo el publicador escribe", y condiciona el chat a tener "la
moderación probada". La auditoría del código lo confirma con números:

| Feature | Qué existe hoy |
|---|---|
| **Canal de novedades** | Casi todo: fan-out a seguidores probado, feed de eventos paginado, campanita, cola de reportes, patrón de moderación **triple-probado** |
| **Inbox usuario↔publicador** | **Nada.** Cero entidades, cero tablas, cero endpoints |
| **Grupos** | **Nada.** "Ningún ladrillo existe" |
| **Chat / realtime** | Nada, y el proyecto dice explícitamente *"Nunca websockets"* |

Y hay algo más de fondo que un tema de esfuerzo: **el inbox cambia el
modelo de moderación**. Los tres textos libres que existen hoy
(comentarios, valoraciones, preguntas) son **públicos**, y por eso
alcanzan los reportes de terceros: cualquiera ve el abuso y lo marca.
Un mensaje privado **nadie más lo ve**, así que necesita reporte del
propio participante, acceso del admin al hilo, y una decisión de
producto sobre qué puede leer el admin y cuándo. Eso es una discusión
de privacidad, no una tarea de implementación — merece su fase.

## Qué ve la gente al final

1. **El publicador cuenta algo** sin tener que crear una actividad:
   "cambiamos el horario del sábado", "quedan 3 lugares", "arranca el
   torneo". Hoy solo puede comunicarse publicando o editando una
   actividad.
2. **Sus seguidores lo ven en el feed** que ya existe (Fase 6) y les
   llega la campanita.
3. **La novedad se modera como todo lo social**: publica directo, es
   reportable, el admin la oculta.

## Lo que ya está y se reusa tal cual

- **Fan-out a seguidores**: `emitirATodos(usuarioIdsSeguidoresDe(...))`
  ya se usa para "actividad nueva". Es exactamente lo que necesita un
  canal.
- **`feed_event` fue diseñado para esto**: el script 32 dice textual
  *"cada fase suma tipos (novedad del canal en la 8...)"*, y su `tipo`
  **no tiene CHECK**, así que sumar `NOVEDAD` es **código puro, sin
  migración**. Lo mismo `notificacion.tipo`.
- **El frontend ya tolera tipos nuevos**: `FeedEventoCard` tiene
  fallback (`FRASES[tipo] ?? "publicó algo nuevo"`), así que un tipo
  desconocido no rompe la UI.
- **Moderación**: el patrón de `ComentarioImagenService` (estado
  string + `esVisible()` + ocultar por rol + reporte) es el más
  completo de los tres y se copia entero.
- Campanita, cola de admin, botón Reportar, paginación "Ver más",
  404-en-vez-de-403: todo existente.

## Migración (script 34)

```sql
CREATE TABLE novedad (
    id BIGSERIAL PRIMARY KEY,
    perfil_publicador_id BIGINT NOT NULL,
    texto VARCHAR(1000) NOT NULL,
    imagen_id BIGINT NULL,              -- una foto ya publicada del perfil
    estado VARCHAR(30) NOT NULL DEFAULT 'VISIBLE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_novedad_perfil FOREIGN KEY (perfil_publicador_id)
        REFERENCES perfil_publicador (id) ON DELETE CASCADE,
    CONSTRAINT fk_novedad_imagen FOREIGN KEY (imagen_id)
        REFERENCES imagen (id) ON DELETE SET NULL,
    CONSTRAINT chk_novedad_estado CHECK (estado IN (
        'VISIBLE', 'OCULTA_POR_ADMIN', 'ELIMINADA_POR_PUBLICADOR'))
);

CREATE INDEX idx_novedad_perfil_fecha ON novedad (perfil_publicador_id, created_at DESC);

-- El CHECK de reporte ENUMERA valores: sumar NOVEDAD exige migrarlo
-- (ya pasó en los scripts 29 y 30 por lo mismo).
ALTER TABLE reporte DROP CONSTRAINT chk_reporte_tipo_objeto;
ALTER TABLE reporte ADD CONSTRAINT chk_reporte_tipo_objeto
    CHECK (tipo_objeto IN ('IMAGEN','PERFIL_PUBLICADOR','ACTIVIDAD',
                           'VALORACION','PREGUNTA','COMENTARIO','NOVEDAD'));

ALTER TABLE feed_event ADD COLUMN novedad_id BIGINT NULL;
ALTER TABLE feed_event ADD CONSTRAINT fk_feed_event_novedad
    FOREIGN KEY (novedad_id) REFERENCES novedad (id) ON DELETE CASCADE;
```

- **Tabla propia y no `feed_event.resumen`**: ese campo son 200
  caracteres pensados para "3 fotos nuevas", y una novedad necesita
  texto largo, **estado de moderación** y foto. Meter el cuerpo ahí
  mezclaría el log de hechos con el contenido.
- El CHECK de `estado` **sí** enumera (es un catálogo cerrado de
  estados, como en las otras tablas sociales); el de `reporte` hay que
  ampliarlo, que es el costo conocido.
- `ON DELETE SET NULL` en la imagen: si el publicador borra la foto,
  la novedad sobrevive sin ella (el texto sigue teniendo sentido).

## Backend

1. `Novedad` + repo + `NovedadService` con el patrón de moderación ya
   probado: publicar directo, `eliminarPropia` (soft, del publicador),
   `ocultarPorAdmin`, `esVisible()` para el reporte.
2. **Tope de publicación**: `MAX_NOVEDADES_POR_DIA` contra la base
   (día argentino), igual que comentarios y preguntas. Ver decisión 2.
3. Al publicar: **evento de feed** (`NOVEDAD`) + **notificación a los
   seguidores**, ambos best-effort. El evento va en `afterCommit`,
   como fijó la Fase 6.
4. Endpoints: `POST/GET/DELETE /api/publicador/novedades` (del dueño),
   `GET /api/perfiles-publicadores/{idOSlug}/novedades` (público) y
   `PATCH /api/admin/novedades/{id}/ocultar`.
5. `ReporteService`: sumar `NOVEDAD` al catálogo y su `case`.
6. El feed ya paginado incorpora el tipo nuevo con su texto e imagen.

## Frontend

- **Componer novedad** en el panel del publicador: textarea, foto
  opcional elegida entre las suyas ya publicadas, y el listado de las
  últimas con opción de eliminar.
- **Tab "Novedades" en el perfil público**, con la misma regla que
  Opiniones: aparece **solo si hay contenido**.
- **La novedad en el feed**, reusando `FeedEventoCard` con su formato
  propio (texto del publicador + foto), y `BotonReportar` con el tipo
  nuevo.
- Admin: la novedad reportada se puede **ocultar desde la cola**, como
  ya se hace con fotos y comentarios.

## Verificación y deploy

1. Unit (tope diario, ocultar por admin, eliminar propia, reporte de
   novedad, el fan-out que no rompe si falla) + **IT propio del
   endpoint** con el camino feliz: publicar → aparece en el feed del
   seguidor → notificación emitida → ocultarla la saca del feed.
   (Regla que dejó la Fase 7: endpoint nuevo, IT propio.)
2. Script 34 (vos) → ITs → backend → frontend → tu smoke.

## Decisiones que pide este plan (con recomendación)

1. **Solo canales de novedades en esta fase.** Inbox y grupos merecen
   fases propias; el inbox además abre una decisión de privacidad
   (quién puede leer un mensaje privado y cuándo) que no conviene
   resolver de apuro. Recomiendo esta fase acotada.
2. **Tope de 3 novedades por día por publicador.** Hoy **no existe
   ningún límite para el rol publicador** — todos los topes actuales
   son sobre el usuario que consume. Un canal sin tope es un canal que
   puede inundar el feed de sus seguidores, y el costo de equivocarse
   lo paga la gente que lo sigue. Recomiendo 3/día, que permite contar
   lo que pasa sin volverse ruido.
3. **Notificar solo la primera novedad del día por publicador.**
   `NotificacionService` **no agrupa ni deduplica**: hoy emite una fila
   por seguidor por evento. Tres novedades × 50 seguidores = 150
   notificaciones en un día para la misma gente. Recomiendo emitir la
   campanita solo en la primera del día; las demás igual entran al
   feed. Es la diferencia entre un canal que se sigue y uno que se
   silencia.
4. **Sin reacciones en V1.** El roadmap las menciona ("usuarios
   reaccionan"), pero son una tabla más, otro contador y otra
   superficie. Recomiendo dejarlas para cuando haya novedades reales
   publicadas y se vea si hacen falta.
5. **La landing social por deporte+ciudad NO entra.** Ya existe y
   funciona (`/ciudades/{ciudad}/{deporte}` con actividades reales,
   SEO y CTA de captación); lo que el roadmap quiere es enriquecerla
   con contenido social que **todavía no existe**. Tiene más sentido
   después de que haya novedades y opiniones para mostrar ahí. El
   soft-404 que arrastra sigue con impacto bajo (el sitemap solo lista
   slugs válidos) y se puede atacar aparte.

---

## Estado: IMPLEMENTADO (2026-08-24), pendiente script 34 + deploy

Las 5 recomendaciones fueron aprobadas por Agustín y están las cinco en
código. Dos commits, en el orden de los dos pushes.

### Backend (commit "canal de novedades del publicador - backend")

- `database/scripts/34_canales_novedades.sql` — **PENDIENTE de aplicar
  en Supabase y local**. Aditivo salvo el CHECK de `reporte.tipo_objeto`,
  que enumera y hay que reescribir (mismo costo que los scripts 29 y 30).
- `entity/Novedad.java`, `repository/NovedadRepository.java`,
  `dto/NovedadDTO.java`, `service/NovedadService.java`,
  `controller/PublicadorNovedadController.java`.
- `GET /api/perfiles-publicadores/{id}/novedades` (público),
  `GET|POST /api/publicador/novedades`, `DELETE /api/publicador/novedades/{id}`,
  `PATCH /api/admin/novedades/{id}/ocultar`.
- `FeedEventService`: `TIPO_NOVEDAD` + `emitirNovedad(...)` como
  **overload** (no un parámetro más en `emitir`, para no tocar a los
  cuatro llamadores que ya existen), también con `afterCommit`.
- `ReporteService`: `NOVEDAD` en `TIPOS_OBJETO` y su `case` delegando en
  `novedadService.esVisible`.

Lo que quedó en código y conviene no re-litigar:

- **El tope cuenta también las borradas** (`countBy...CreatedAtGreaterThanEqual`
  sin filtrar estado): borrar y republicar no puede ser la forma de
  saltear el límite.
- **Ocultar por admin la saca del feed**, no solo del perfil: el
  enriquecido de `FeedEventService` descarta el evento cuya novedad no
  está VISIBLE. Efecto lateral aceptado: el `totalElementos` de la
  página queda un pelo alto, igual que con cualquier filtro de
  moderación posterior al query.
- **Una foto ajena no voltea la publicación**: sale sin foto.
- **404 y no 403** al borrar una ajena.

### Frontend (commit "canal de novedades - frontend")

- `services/novedadesService.ts`, `components/publicador/CanalDeNovedades.tsx`,
  `app/publicador/novedades/page.tsx`,
  `components/publicadores/NovedadesDelPublicador.tsx`.
- Solapa "Novedades" en el perfil público (solo si hay contenido; entrar
  por URL sin novedades cae en actividades), novedad en el feed con su
  `BotonReportar` tipo `NOVEDAD`, "Ocultar la novedad" en la cola de
  reportes del admin, entradas en `menuCuenta.ts` y en el dashboard.
- La foto de la novedad **se elige entre las ya publicadas**, no se sube
  acá: subir es un flujo propio (el centro de fotos) y duplicarlo sería
  una segunda cola de moderación con las mismas reglas.

### Verificación hecha

- 567 unit tests verdes (8 nuevos en `NovedadServiceTest`).
- Frontend: typecheck + lint limpios y build con 42 rutas, incluida
  `/publicador/novedades`.

### Lo que falta

1. **Agustín aplica el script 34** en Supabase y local (migración antes
   que código).
2. `verify -Pintegration-local` — `CanalNovedadesIT` cubre el camino
   feliz completo (publicar → perfil público → feed del seguidor →
   campanita), el ocultar del admin, el borrado propio, el tope diario y
   el 401 anónimo. **No se pudo correr todavía**: sin la tabla, el
   contexto con `ddl-auto=validate` no arranca.
3. Deploy en dos etapas (backend → marcador anónimo → frontend) y smoke
   de Agustín con su cuenta de publicador.

---

## ⚠️ Hallazgo del IT: el feed no guardaba NINGÚN evento desde la Fase 6

El `CanalNovedadesIT` falló en el caso más obvio —publicar y ver la
novedad en el feed del seguidor— con esta línea en el log:

```
FEED_EVENT_NO_EMITIDO tipo=NOVEDAD perfil=1795: no transaction is in progress
```

**La causa**: dentro de `afterCommit` el `EntityManager` de la request
sigue atado al hilo, pero ya **sin transacción** (el commit ocurrió y
la limpieza de recursos pasa después de disparar los callbacks). Con un
`EntityManager` presente, `saveAndFlush` **no abre una transacción
nueva**: usa ese y muere con `TransactionRequiredException`. Como la
emisión es best-effort, el `catch` lo tragaba y solo quedaba el warning.

**El alcance es de la Fase 6, no de esta**: afecta a los tres tipos de
evento que se emiten desde `afterCommit` (`ACTIVIDAD_NUEVA`,
`FOTOS_NUEVAS`, `ACTIVIDAD_ACTUALIZADA`). Desde `72f00fc` el feed
productivo venía mostrando **solo el backfill del script 32**, y nada
de lo que pasó después. `spring.jpa.open-in-view` no está configurado
(default `true`) tanto en producción como en los ITs, así que el
comportamiento es el mismo en los dos lados.

**El fix**: el guardado corre en transacción propia
(`TransactionTemplate` con `PROPAGATION_REQUIRES_NEW`). Acá
`REQUIRES_NEW` **es seguro y no repite el error de la Fase 6**
justamente porque corre *después* del commit: las filas que el evento
referencia por FK ya existen y otra conexión las ve. Es
`TransactionTemplate` y no `@Transactional` porque la llamada sale del
mismo bean y el proxy no la interceptaría.

**Por qué se escapó**, que es la parte que conviene no repetir:

- El `FeedSocialIT` de la Fase 6 **inserta los `feed_event` a mano**
  (`crearEvento`) para probar la paginación. Nunca ejerció el camino
  real de emisión.
- El IT de subida de fotos sí lo ejerce, pero solo verifica que **no
  rompa** (era el bug del 500 con `REQUIRES_NEW`), no que el evento
  aparezca después.
- Los unit tests mockean el repositorio, así que `saveAndFlush` nunca
  necesita una transacción de verdad.

**Y un falso verde propio de esta fase**: el caso "ocultar por admin la
saca del feed" pasaba igual con el bug, porque afirmaba que el feed
quedaba **vacío** — cosa que también pasa si el evento nunca se emitió.
Ahora afirma primero que la novedad **está**. Regla que deja: un test
que verifica una desaparición tiene que probar antes la aparición.

**Verificación en producción** (post-deploy): el feed de alguien que
sigue a un publicador tiene que mostrar los hechos NUEVOS, no solo los
del backfill. La novedad del canal es la forma más rápida de probarlo.

---

## Estado: EN PRODUCCIÓN EN DOS TANDAS (2026-08-24), pendiente el smoke

`main` = `origin/main` = **`ce67fab`**. Script 34 aplicado por Agustín
en Supabase y local ANTES del deploy.

**Tanda 1 — backend `112d537`** (`853ee55` canal + `112d537` el fix del
feed; la historia se reordenó con worktree para que TODO el backend
viajara primero, verificando que el árbol final quedara idéntico).

Marcador: `GET /api/perfiles-publicadores/8/novedades` pasó de **404 a
200 `[]`** — esa ruta no existía en el build viejo y caía en el handler
genérico ("Recurso no encontrado."). Verificado además **contra datos
reales**, que es lo que la Fase 7 dejó como regla: un perfil inexistente
(`/99999/novedades`) responde 404 con el **mensaje propio** ("El perfil
publicador solicitado no existe o no está disponible"), o sea que el
endpoint consulta la tabla y no es una ruta que devuelve 200 y nada más.
Las tres privadas nuevas (GET/POST `/api/publicador/novedades`, PATCH
`/api/admin/novedades/{id}/ocultar`) dan 401 anónimas, y el catálogo
público, las fotos del publicador y `/api/health` quedaron intactos.

**Tanda 2 — frontend `ce67fab`.** Verificado en el navegador en
producción: el perfil público **no dibuja** la solapa "Novedades"
(correcto: ninguna existe todavía), `?tab=novedades` cae en Actividades
sin romper, las cuatro privadas dan 307 a login, las públicas 200 y la
consola está limpia.

**⚠️ Marcador que NO sirve para el frontend de esta fase**:
`/publicador/novedades` devuelve **307 anónimo tanto en el build viejo
como en el nuevo**, porque el middleware redirige cualquier subruta de
`/publicador`, exista o no. Es la misma trampa que `/api/usuario/feed`
en la Fase 6. Se usó en cambio un texto del `FeedEventoCard` nuevo
("Ver el perfil de"), que por ser componente `"use client"` viaja a los
chunks de la **home pública** y se puede buscar sin sesión.

### Lo que falta

**El smoke de Agustín con su cuenta de publicador.** Lo que conviene
probar, en este orden:

1. `/publicador/novedades` → publicar una novedad (con y sin foto).
2. Verla en su perfil público, en la solapa "Novedades" que recién
   ahora tiene que aparecer.
3. Con otra cuenta que lo siga: verla **en el feed** y recibir la
   campanita. Esto además **verifica el fix del feed**: si aparece, los
   eventos vuelven a guardarse.
4. Publicar una segunda: entra al feed pero **no** vuelve a notificar.
5. La cuarta del día tiene que ser rechazada con el mensaje del tope.
6. Borrar una propia y ver que desaparece de las dos superficies.
7. Como admin: reportar una novedad desde el feed y ocultarla desde
   `/admin/reportes`.
