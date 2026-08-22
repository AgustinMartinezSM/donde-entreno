-- ============================================================
-- Script 22 — Colecciones de guardados (bloque 13)
-- ============================================================
-- Aditiva pura: tabla nueva + dos columnas nullable en favoritos.
-- El código desplegado la ignora (migración ANTES que código).
--
-- PRE:
--   SELECT COUNT(*) FROM information_schema.tables
--    WHERE table_name = 'coleccion_guardados';           -- esperado: 0
--   SELECT column_name FROM information_schema.columns
--    WHERE table_name = 'favorito_actividad'
--      AND column_name IN ('coleccion_id', 'nota');      -- esperado: 0 filas

BEGIN;

CREATE TABLE coleccion_guardados (
    id BIGSERIAL PRIMARY KEY,

    usuario_id BIGINT NOT NULL,

    nombre VARCHAR(60) NOT NULL,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_coleccion_guardados_usuario
        FOREIGN KEY (usuario_id) REFERENCES usuario (id)
        ON DELETE CASCADE,

    -- Dos colecciones con el mismo nombre confunden más de lo que suman.
    CONSTRAINT uq_coleccion_guardados_usuario_nombre
        UNIQUE (usuario_id, nombre)
);

COMMENT ON TABLE coleccion_guardados IS
    'Colecciones con nombre propio para organizar guardados (bloque 13). Los nombres los escribe la persona.';

-- Filtrar "mis colecciones" es la consulta de siempre.
CREATE INDEX idx_coleccion_guardados_usuario
    ON coleccion_guardados (usuario_id);

-- Un guardado vive en UNA colección o en ninguna ("Todos"). Borrar la
-- colección NO borra guardados: vuelven a "Todos" (SET NULL).
ALTER TABLE favorito_actividad
    ADD COLUMN coleccion_id BIGINT NULL
        REFERENCES coleccion_guardados (id) ON DELETE SET NULL;

ALTER TABLE favorito_actividad
    ADD COLUMN nota VARCHAR(280);

-- Los chips filtran por colección dentro del usuario.
CREATE INDEX idx_favorito_actividad_coleccion
    ON favorito_actividad (coleccion_id);

COMMIT;

-- POST:
--   SELECT COUNT(*) FROM information_schema.tables
--    WHERE table_name = 'coleccion_guardados';           -- esperado: 1
--   SELECT column_name, is_nullable FROM information_schema.columns
--    WHERE table_name = 'favorito_actividad'
--      AND column_name IN ('coleccion_id', 'nota');      -- esperado: 2 filas, YES/YES
--
-- Rollback (solo ANTES de desplegar el backend que lo usa):
--   ALTER TABLE favorito_actividad DROP COLUMN nota;
--   ALTER TABLE favorito_actividad DROP COLUMN coleccion_id;
--   DROP TABLE coleccion_guardados;
