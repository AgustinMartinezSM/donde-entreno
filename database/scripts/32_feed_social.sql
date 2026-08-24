-- ============================================================
-- Script 32 — Fase 6 social: feed de eventos
-- feed_event + backfill de las actividades ya publicadas
-- ============================================================
-- Aditiva: tabla nueva, nada existente cambia de forma. El código
-- desplegado hoy la ignora (migración ANTES que código).
-- Plan: docs/plan-fase6-feed-social.md
--
-- PRE:
--   SELECT COUNT(*) FROM information_schema.tables
--    WHERE table_name = 'feed_event';
--   -- esperado: 0
--   SELECT COUNT(*) FROM actividad
--    WHERE activa = true AND estado_publicacion = 'PUBLICADA'
--      AND deleted_at IS NULL;
--   -- anotá este número: es la cantidad de filas que va a sembrar el backfill

BEGIN;

-- ============================================================
-- Línea de tiempo de hechos de los publicadores. Hasta ahora el
-- "feed" era una consulta a `actividad`, así que solo podía contar
-- un tipo de hecho: que se publicó una actividad.
-- ============================================================
CREATE TABLE feed_event (
    id BIGSERIAL PRIMARY KEY,

    -- Catálogo ABIERTO a propósito (sin CHECK): cada fase suma tipos
    -- (novedad del canal en la 8, evento en la 9). Un CHECK que
    -- enumera obliga a migrar por cada valor nuevo — la lección que
    -- dejó el script 25. El catálogo se valida en el service.
    tipo VARCHAR(40) NOT NULL,

    perfil_publicador_id BIGINT NOT NULL,

    -- Nullable: hay hechos del perfil que no cuelgan de una actividad.
    actividad_id BIGINT NULL,
    imagen_id BIGINT NULL,

    -- Texto corto SOLO para lo que no se deduce del join (por ejemplo
    -- "3 fotos nuevas"). Para una actividad nueva viaja NULL: el
    -- título sale de la actividad y así nunca queda desactualizado.
    resumen VARCHAR(200) NULL,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_feed_event_perfil
        FOREIGN KEY (perfil_publicador_id)
        REFERENCES perfil_publicador (id)
        ON DELETE CASCADE,

    CONSTRAINT fk_feed_event_actividad
        FOREIGN KEY (actividad_id)
        REFERENCES actividad (id)
        ON DELETE CASCADE,

    CONSTRAINT fk_feed_event_imagen
        FOREIGN KEY (imagen_id)
        REFERENCES imagen (id)
        ON DELETE CASCADE
);

-- Los dos accesos reales: el feed de un seguidor filtra por perfiles
-- y ordena por fecha; la home mira la línea global.
CREATE INDEX idx_feed_event_perfil_fecha
    ON feed_event (perfil_publicador_id, created_at DESC);

CREATE INDEX idx_feed_event_fecha
    ON feed_event (created_at DESC);

-- ============================================================
-- Backfill: un evento por cada actividad publicada, con su fecha
-- REAL. Sin esto el feed queda en blanco el día del deploy —incluso
-- para quien sigue a alguien— y parece roto.
-- ============================================================
INSERT INTO feed_event (tipo, perfil_publicador_id, actividad_id, resumen, created_at)
SELECT
    'ACTIVIDAD_NUEVA',
    a.perfil_publicador_id,
    a.id,
    NULL,
    a.created_at
FROM actividad a
WHERE a.activa = true
  AND a.estado_publicacion = 'PUBLICADA'
  AND a.deleted_at IS NULL;

COMMIT;

-- POST:
--   SELECT table_name FROM information_schema.tables
--    WHERE table_name = 'feed_event';
--   -- esperado: 1 fila
--   SELECT tipo, COUNT(*) FROM feed_event GROUP BY tipo;
--   -- esperado: ACTIVIDAD_NUEVA con la cantidad anotada en el PRE
--   SELECT MIN(created_at), MAX(created_at) FROM feed_event;
--   -- esperado: fechas REALES de las actividades, no todas iguales a hoy
--   SELECT indexname FROM pg_indexes WHERE tablename = 'feed_event';
--   -- esperado: pkey + 2 índices
--
-- Rollback (solo ANTES de desplegar el backend que lo usa):
--   DROP TABLE feed_event;
