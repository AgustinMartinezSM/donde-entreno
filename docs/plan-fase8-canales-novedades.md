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
