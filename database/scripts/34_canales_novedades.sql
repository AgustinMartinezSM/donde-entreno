-- ============================================================
-- Script 34 — Fase 8 social: canales de novedades del publicador
-- ============================================================
-- Aditiva salvo el CHECK de reporte, que hay que reescribir porque
-- ENUMERA valores (mismo costo conocido de los scripts 29 y 30).
-- Plan: docs/plan-fase8-canales-novedades.md
--
-- PRE:
--   SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'novedad';
--   -- esperado: 0
--   SELECT COUNT(*) FROM information_schema.columns
--    WHERE table_name = 'feed_event' AND column_name = 'novedad_id';
--   -- esperado: 0
--   SELECT pg_get_constraintdef(oid) FROM pg_constraint
--    WHERE conname = 'chk_reporte_tipo_objeto';
--   -- esperado: la lista de 6 tipos (sin NOVEDAD)

BEGIN;

-- ============================================================
-- La novedad del canal: lo que el publicador quiere contar sin tener
-- que crear o editar una actividad.
--
-- Tabla propia y NO feed_event.resumen: ese campo son 200 caracteres
-- pensados para "3 fotos nuevas" y no tiene estado de moderación.
-- Meter el cuerpo ahí mezclaría el LOG de hechos con el CONTENIDO.
-- ============================================================
CREATE TABLE novedad (
    id BIGSERIAL PRIMARY KEY,

    perfil_publicador_id BIGINT NOT NULL,

    texto VARCHAR(1000) NOT NULL,

    -- Una foto YA publicada del publicador (no hay subida acá: la
    -- galería de la Fase 4 ya resuelve eso).
    imagen_id BIGINT NULL,

    estado VARCHAR(30) NOT NULL DEFAULT 'VISIBLE',

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_novedad_perfil
        FOREIGN KEY (perfil_publicador_id)
        REFERENCES perfil_publicador (id)
        ON DELETE CASCADE,

    -- SET NULL y no CASCADE: si el publicador borra la foto, la
    -- novedad sobrevive sin ella — el texto sigue teniendo sentido.
    CONSTRAINT fk_novedad_imagen
        FOREIGN KEY (imagen_id)
        REFERENCES imagen (id)
        ON DELETE SET NULL,

    -- Este CHECK SÍ enumera: es un catálogo CERRADO de estados, como
    -- en el resto de las tablas sociales. Lo que no se enumera son los
    -- catálogos que cada fase amplía (tipos de feed, de notificación).
    CONSTRAINT chk_novedad_estado
        CHECK (estado IN (
            'VISIBLE',
            'OCULTA_POR_ADMIN',
            'ELIMINADA_POR_PUBLICADOR'
        ))
);

-- El acceso real: las novedades de un publicador, más nuevas primero.
CREATE INDEX idx_novedad_perfil_fecha
    ON novedad (perfil_publicador_id, created_at DESC);

-- Tope diario por publicador (se cuenta contra la base, día argentino).
CREATE INDEX idx_novedad_perfil_created
    ON novedad (perfil_publicador_id, created_at);

-- ============================================================
-- El evento de feed apunta a la novedad (el feed sigue siendo el LOG;
-- el contenido vive en su tabla).
-- ============================================================
ALTER TABLE feed_event
    ADD COLUMN novedad_id BIGINT NULL;

ALTER TABLE feed_event
    ADD CONSTRAINT fk_feed_event_novedad
        FOREIGN KEY (novedad_id)
        REFERENCES novedad (id)
        ON DELETE CASCADE;

-- ============================================================
-- El CHECK de reporte ENUMERA valores: cada objeto reportable nuevo
-- cuesta una migración (ya pasó en los scripts 29 y 30).
-- ============================================================
ALTER TABLE reporte
    DROP CONSTRAINT chk_reporte_tipo_objeto;

ALTER TABLE reporte
    ADD CONSTRAINT chk_reporte_tipo_objeto
        CHECK (tipo_objeto IN (
            'IMAGEN',
            'PERFIL_PUBLICADOR',
            'ACTIVIDAD',
            'VALORACION',
            'PREGUNTA',
            'COMENTARIO',
            'NOVEDAD'
        ));

COMMIT;

-- El tipo de feed ('NOVEDAD') y el de notificación NO se tocan acá:
-- esas columnas no tienen CHECK a propósito, así que sumar valores es
-- código puro (decisión del script 28 y del 32).

-- POST:
--   SELECT table_name FROM information_schema.tables WHERE table_name = 'novedad';
--   -- esperado: 1 fila
--   SELECT column_name, is_nullable FROM information_schema.columns
--    WHERE table_name = 'novedad' ORDER BY ordinal_position;
--   -- esperado: id, perfil_publicador_id, texto, imagen_id, estado,
--   --           created_at, updated_at  (imagen_id YES)
--   SELECT pg_get_constraintdef(oid) FROM pg_constraint
--    WHERE conname = 'chk_reporte_tipo_objeto';
--   -- esperado: incluye NOVEDAD
--   SELECT COUNT(*) FROM information_schema.columns
--    WHERE table_name = 'feed_event' AND column_name = 'novedad_id';
--   -- esperado: 1
--
-- Rollback (solo ANTES de desplegar el backend que lo usa):
--   volver el CHECK de reporte a los 6 tipos anteriores;
--   ALTER TABLE feed_event DROP CONSTRAINT fk_feed_event_novedad;
--   ALTER TABLE feed_event DROP COLUMN novedad_id;
--   DROP TABLE novedad;
