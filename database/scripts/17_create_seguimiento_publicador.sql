-- ============================================================
-- 17 - SEGUIR PUBLICADORES (capa social, Bloque 8 V1)
-- ============================================================
--
-- Un usuario con cuenta puede seguir a un perfil publicador para
-- descubrir sus actividades. Primer ladrillo de la capa social.
--
-- - UNIQUE (usuario_id, perfil_publicador_id): un usuario sigue a un
--   publicador una sola vez (idempotencia del "seguir").
-- - Dejar de seguir borra la fila (no hay soft-delete).
--
-- Migracion aditiva: no toca tablas ni datos existentes.
-- ============================================================

CREATE TABLE IF NOT EXISTS seguimiento_publicador (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL
        REFERENCES usuario(id),
    perfil_publicador_id BIGINT NOT NULL
        REFERENCES perfil_publicador(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_seguimiento_usuario_perfil
        UNIQUE (usuario_id, perfil_publicador_id)
);

CREATE INDEX IF NOT EXISTS idx_seguimiento_usuario
    ON seguimiento_publicador (usuario_id);

CREATE INDEX IF NOT EXISTS idx_seguimiento_perfil
    ON seguimiento_publicador (perfil_publicador_id);
