-- ============================================================
-- Script 31 — Fase 5 social: el perfil del publicador
-- actividad.destacada_orden + interacciones a nivel perfil
-- ============================================================
-- Aditiva. Plan: docs/plan-fase5-perfil-publicador.md
--
-- Las dos columnas nacen NULL, así que el código desplegado hoy
-- sigue funcionando igual mientras esta migración está aplicada y
-- el backend nuevo todavía no (migración ANTES que código).
--
-- PRE:
--   SELECT COUNT(*) FROM information_schema.columns
--    WHERE table_name = 'actividad' AND column_name = 'destacada_orden';
--   -- esperado: 0
--   SELECT COUNT(*) FROM information_schema.columns
--    WHERE table_name = 'evento_interaccion'
--      AND column_name = 'perfil_publicador_id';
--   -- esperado: 0
--   SELECT is_nullable FROM information_schema.columns
--    WHERE table_name = 'evento_interaccion' AND column_name = 'actividad_id';
--   -- esperado: NO  (queda en YES al final)

BEGIN;

-- ============================================================
-- Actividades destacadas del publicador: elige hasta 3 que van
-- primero en su perfil. NULL = no destacada (todas las de hoy).
-- ============================================================
-- Sin CHECK que enumere el rango a propósito: el tope (3) se valida
-- en el service. Un CHECK BETWEEN 1 AND 3 obligaría a migrar el día
-- que el tope cambie, que es exactamente la trampa del script 25.
ALTER TABLE actividad
    ADD COLUMN destacada_orden SMALLINT NULL;

-- Índice parcial: solo las destacadas (3 por publicador como máximo),
-- que es justo el filtro que corre el perfil público.
CREATE INDEX idx_actividad_destacada
    ON actividad (perfil_publicador_id, destacada_orden)
    WHERE destacada_orden IS NOT NULL;

-- ============================================================
-- Interacciones a nivel PERFIL (el WhatsApp del perfil hoy no se
-- mide: el botón queda ciego). Se suma una columna nullable en vez
-- de colgar el click de una actividad inventada, que ensuciaría las
-- métricas por actividad que el publicador ya está mirando.
-- ============================================================
ALTER TABLE evento_interaccion
    ADD COLUMN perfil_publicador_id BIGINT NULL;

ALTER TABLE evento_interaccion
    ADD CONSTRAINT fk_evento_interaccion_perfil
        FOREIGN KEY (perfil_publicador_id)
        REFERENCES perfil_publicador (id)
        ON DELETE CASCADE;

-- Un evento es de actividad O de perfil: al aparecer el segundo caso,
-- actividad_id deja de ser obligatorio. Que al menos uno esté cargado
-- lo valida el service (mismo criterio que el rango de destacadas: no
-- quiero migrar un CHECK cuando aparezca un tercer tipo de objeto).
ALTER TABLE evento_interaccion
    ALTER COLUMN actividad_id DROP NOT NULL;

CREATE INDEX idx_evento_interaccion_perfil
    ON evento_interaccion (perfil_publicador_id, tipo, created_at)
    WHERE perfil_publicador_id IS NOT NULL;

COMMIT;

-- El CHECK de tipo NO se toca: el click del perfil reusa
-- 'CLICK_WHATSAPP', que ya está en chk_evento_interaccion_tipo.

-- POST:
--   SELECT column_name, data_type, is_nullable
--     FROM information_schema.columns
--    WHERE table_name = 'actividad' AND column_name = 'destacada_orden';
--   -- esperado: smallint, YES
--   SELECT column_name, is_nullable FROM information_schema.columns
--    WHERE table_name = 'evento_interaccion'
--      AND column_name IN ('actividad_id','perfil_publicador_id');
--   -- esperado: actividad_id YES, perfil_publicador_id YES
--   SELECT indexname FROM pg_indexes
--    WHERE indexname IN ('idx_actividad_destacada','idx_evento_interaccion_perfil');
--   -- esperado: 2 filas
--   SELECT COUNT(*) FROM actividad WHERE destacada_orden IS NOT NULL;
--   -- esperado: 0 (nadie destacó nada todavía)
--
-- Rollback (solo ANTES de desplegar el backend que lo usa):
--   DROP INDEX idx_evento_interaccion_perfil;
--   DELETE FROM evento_interaccion WHERE actividad_id IS NULL;
--   ALTER TABLE evento_interaccion ALTER COLUMN actividad_id SET NOT NULL;
--   ALTER TABLE evento_interaccion DROP CONSTRAINT fk_evento_interaccion_perfil;
--   ALTER TABLE evento_interaccion DROP COLUMN perfil_publicador_id;
--   DROP INDEX idx_actividad_destacada;
--   ALTER TABLE actividad DROP COLUMN destacada_orden;
