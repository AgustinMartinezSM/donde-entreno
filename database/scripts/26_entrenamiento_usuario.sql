-- ============================================================
-- Script 26 — Check-in "Entrené acá" (entrenamiento_usuario)
-- ============================================================
-- Aditiva pura: tabla nueva, el código desplegado la ignora
-- (migración ANTES que código). Plan: docs/plan-valoraciones-a-checkin.md
--
-- Sin UNIQUE (usuario, actividad): se puede entrenar muchas veces.
-- La regla "1 check-in por actividad por día" es del service, validada
-- contra la base (Render reinicia y en rotación hay dos instancias:
-- un contador en memoria dejaría colar duplicados).
--
-- PRE:
--   SELECT COUNT(*) FROM information_schema.tables
--    WHERE table_name = 'entrenamiento_usuario';   -- esperado: 0

BEGIN;

CREATE TABLE entrenamiento_usuario (
    id BIGSERIAL PRIMARY KEY,

    usuario_id BIGINT NOT NULL,

    actividad_id BIGINT NOT NULL,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    -- Borrar la cuenta o la actividad se lleva sus check-ins.
    CONSTRAINT fk_entrenamiento_usuario_usuario
        FOREIGN KEY (usuario_id)
        REFERENCES usuario (id)
        ON DELETE CASCADE,

    CONSTRAINT fk_entrenamiento_usuario_actividad
        FOREIGN KEY (actividad_id)
        REFERENCES actividad (id)
        ON DELETE CASCADE
);

-- Contador público por ventana ("N personas este mes").
CREATE INDEX idx_entrenamiento_usuario_actividad_fecha
    ON entrenamiento_usuario (actividad_id, created_at);

-- Regla del día por usuario y el futuro historial propio.
CREATE INDEX idx_entrenamiento_usuario_usuario_actividad_fecha
    ON entrenamiento_usuario (usuario_id, actividad_id, created_at);

COMMENT ON TABLE entrenamiento_usuario IS
    'Check-ins "Entrené acá" (script 26). Sin UNIQUE a propósito: 1 por día lo valida el service contra la base. El contador público es agregado y anónimo (usuarios DISTINTOS, últimos 30 días).';

COMMIT;

-- POST:
--   SELECT COUNT(*) FROM information_schema.tables
--    WHERE table_name = 'entrenamiento_usuario';   -- esperado: 1
--   SELECT indexname FROM pg_indexes
--    WHERE tablename = 'entrenamiento_usuario';    -- esperado: pkey + 2
--
-- Rollback (solo ANTES de desplegar el backend que la usa):
--   DROP TABLE entrenamiento_usuario;
