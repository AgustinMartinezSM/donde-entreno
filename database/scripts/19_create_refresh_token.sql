-- ============================================================
-- 19 - REFRESH TOKENS (sesion persistente)
-- ============================================================
--
-- Un refresh token opaco y rotativo por sesion iniciada. El token en
-- claro NUNCA se guarda: solo su SHA-256 en hex (64 chars) — un dump
-- de esta tabla no le sirve a nadie para autenticarse.
--
-- - familia: todos los tokens que descienden del mismo login comparten
--   familia (UUID). Si un token ya usado vuelve a aparecer (reuso =
--   robo detectado), se revoca la familia entera.
-- - usado_en: la rotacion marca el token al consumirse; el nuevo token
--   de la misma familia lo reemplaza.
-- - revocado_en: logout o reuso detectado. Un token revocado no rota.
-- - La higiene corre en el login (borra vencidos viejos del usuario):
--   no hay scheduler y la tabla no crece sin limite.
--
-- Migracion aditiva e idempotente: no toca tablas ni datos existentes.
-- Ejecutar en su propia transaccion (regla 3).
--
-- PRE (esperado: la tabla no existe -> 0 filas):
--   SELECT COUNT(*) FROM information_schema.tables
--    WHERE table_schema = 'public' AND table_name = 'refresh_token';
--
-- POST (esperado: 1 tabla, 0 filas, 3 indices):
--   SELECT COUNT(*) FROM information_schema.tables
--    WHERE table_schema = 'public' AND table_name = 'refresh_token';
--   SELECT COUNT(*) FROM refresh_token;
--   SELECT indexname FROM pg_indexes WHERE tablename = 'refresh_token';
-- ============================================================

-- ON DELETE CASCADE a proposito: un refresh token es una sesion del
-- usuario, no un dato propio — borrar la cuenta (baja fisica, limpieza
-- de tests) arrastra sus sesiones. Sin esto, cualquier DELETE de
-- usuario con sesiones vivas falla por la FK.
CREATE TABLE IF NOT EXISTS refresh_token (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL
        REFERENCES usuario(id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL,
    familia UUID NOT NULL,
    emitido_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expira_en TIMESTAMPTZ NOT NULL,
    usado_en TIMESTAMPTZ,
    revocado_en TIMESTAMPTZ,
    CONSTRAINT uq_refresh_token_hash UNIQUE (token_hash)
);

CREATE INDEX IF NOT EXISTS idx_refresh_token_usuario
    ON refresh_token (usuario_id);

CREATE INDEX IF NOT EXISTS idx_refresh_token_familia
    ON refresh_token (familia);
