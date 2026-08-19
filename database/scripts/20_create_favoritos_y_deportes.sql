-- ============================================================
-- 20 - FAVORITOS Y DEPORTES PREFERIDOS POR CUENTA (sync)
-- ============================================================
--
-- La fuente de verdad de "Guardados" y "Tus deportes" pasa del
-- localStorage del dispositivo a la cuenta (docs/plan-sync-favoritos.md).
--
-- - UNIQUE por (usuario, actividad/deporte): guardar es idempotente.
-- - ON DELETE CASCADE en los DOS lados: un favorito es de la relacion —
--   si el usuario o la actividad/deporte desaparecen, la fila no
--   significa nada (y sin cascade cualquier DELETE con filas vivas
--   falla, como enseño el script 19 con los tests).
-- - Sin soft-delete: dejar de guardar borra la fila, como seguimientos.
--
-- Migracion aditiva e idempotente: no toca tablas ni datos existentes.
-- Ejecutar en su propia transaccion (regla 3).
--
-- PRE (esperado: 0 y 0):
--   SELECT COUNT(*) FROM information_schema.tables
--    WHERE table_schema = 'public'
--      AND table_name IN ('favorito_actividad', 'deporte_preferido');
--
-- POST (esperado: 2 tablas, 0 filas en cada una):
--   SELECT COUNT(*) FROM information_schema.tables
--    WHERE table_schema = 'public'
--      AND table_name IN ('favorito_actividad', 'deporte_preferido');
--   SELECT COUNT(*) FROM favorito_actividad;
--   SELECT COUNT(*) FROM deporte_preferido;
-- ============================================================

CREATE TABLE IF NOT EXISTS favorito_actividad (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL
        REFERENCES usuario(id) ON DELETE CASCADE,
    actividad_id BIGINT NOT NULL
        REFERENCES actividad(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_favorito_usuario_actividad
        UNIQUE (usuario_id, actividad_id)
);

CREATE INDEX IF NOT EXISTS idx_favorito_actividad_usuario
    ON favorito_actividad (usuario_id);

CREATE TABLE IF NOT EXISTS deporte_preferido (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL
        REFERENCES usuario(id) ON DELETE CASCADE,
    deporte_id BIGINT NOT NULL
        REFERENCES deporte(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_deporte_preferido
        UNIQUE (usuario_id, deporte_id)
);

CREATE INDEX IF NOT EXISTS idx_deporte_preferido_usuario
    ON deporte_preferido (usuario_id);
