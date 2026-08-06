-- ============================================================
-- 15 - MODERACION DE IMAGENES
-- ============================================================
--
-- Agrega el estado de moderacion al dominio de imagenes:
--
-- - estado_moderacion: PENDIENTE / APROBADA / RECHAZADA.
--   DEFAULT 'APROBADA' para que las imagenes existentes (seeds)
--   sigan siendo visibles sin tocarlas.
-- - motivo_rechazo: visible para el publicador cuando se rechaza.
--
-- Lo consume el panel de metricas del publicador (conteo de imagenes
-- pendientes) y el filtro de visibilidad publica.
--
-- Migracion aditiva e idempotente: no modifica datos existentes.
-- ============================================================

ALTER TABLE imagen
    ADD COLUMN IF NOT EXISTS estado_moderacion VARCHAR(30) NOT NULL DEFAULT 'APROBADA';

ALTER TABLE imagen
    ADD COLUMN IF NOT EXISTS motivo_rechazo TEXT;

-- CHECK de dominio (DO block porque ADD CONSTRAINT no soporta IF NOT EXISTS).
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'chk_imagen_estado_moderacion'
    ) THEN
        ALTER TABLE imagen
            ADD CONSTRAINT chk_imagen_estado_moderacion
            CHECK (
                estado_moderacion IN (
                    'PENDIENTE',
                    'APROBADA',
                    'RECHAZADA'
                )
            );
    END IF;
END $$;

-- Cola de moderacion del admin.
CREATE INDEX IF NOT EXISTS idx_imagen_estado_moderacion
ON imagen (estado_moderacion);
