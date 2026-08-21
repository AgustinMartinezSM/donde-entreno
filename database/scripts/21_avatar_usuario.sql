-- ============================================================
-- Script 21 — Avatar de usuario (fase 5d del bloque visual)
-- ============================================================
-- Aditiva pura: columna nullable, sin default, sin backfill, sin
-- índices. El código desplegado la ignora hasta que llegue el backend
-- que la usa (regla: migración ANTES que código).
--
-- PRE (verificar que la columna NO existe todavía):
--   SELECT column_name FROM information_schema.columns
--    WHERE table_name = 'usuario' AND column_name = 'avatar_url';
--   -- esperado: 0 filas
--
-- Ejecutar en su propia transacción:

BEGIN;

ALTER TABLE usuario
    ADD COLUMN avatar_url VARCHAR(500);

COMMENT ON COLUMN usuario.avatar_url IS
    'URL pública absoluta del avatar del usuario (Supabase Storage). NULL = iniciales. Sin moderación en V1: el avatar no tiene superficie pública (solo lo ve su dueño); si alguna vez se muestra a terceros, diseñar moderación ANTES.';

COMMIT;

-- POST (verificar):
--   SELECT column_name, data_type, character_maximum_length, is_nullable
--     FROM information_schema.columns
--    WHERE table_name = 'usuario' AND column_name = 'avatar_url';
--   -- esperado: 1 fila, character varying, 500, YES
--
-- Rollback (solo si hace falta deshacer, ANTES de desplegar el backend
-- que la usa):
--   ALTER TABLE usuario DROP COLUMN avatar_url;
