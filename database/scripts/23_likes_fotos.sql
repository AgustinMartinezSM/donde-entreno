-- ============================================================
-- Script 23 — Likes en fotos (bloque 14, diseño de fase 8 §1)
-- ============================================================
-- Aditiva pura: tabla nueva con el patrón exacto de favoritos.
--
-- PRE:
--   SELECT COUNT(*) FROM information_schema.tables
--    WHERE table_name = 'me_gusta_imagen';               -- esperado: 0

BEGIN;

CREATE TABLE me_gusta_imagen (
    id BIGSERIAL PRIMARY KEY,

    usuario_id BIGINT NOT NULL,

    imagen_id BIGINT NOT NULL,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_me_gusta_imagen_usuario
        FOREIGN KEY (usuario_id) REFERENCES usuario (id)
        ON DELETE CASCADE,

    CONSTRAINT fk_me_gusta_imagen_imagen
        FOREIGN KEY (imagen_id) REFERENCES imagen (id)
        ON DELETE CASCADE,

    -- Un like por persona por foto; repetir es idempotente en el service.
    CONSTRAINT uq_me_gusta_imagen_usuario_imagen
        UNIQUE (usuario_id, imagen_id)
);

COMMENT ON TABLE me_gusta_imagen IS
    'Likes de usuarios sobre fotos (bloque 14). Solo se cuentan públicamente sobre imágenes aprobadas y activas; la fila sobrevive si la foto se despublica (patrón favoritos).';

-- El contador público agrupa por imagen; el listado propio filtra por usuario.
CREATE INDEX idx_me_gusta_imagen_imagen
    ON me_gusta_imagen (imagen_id);

CREATE INDEX idx_me_gusta_imagen_usuario
    ON me_gusta_imagen (usuario_id);

COMMIT;

-- POST:
--   SELECT COUNT(*) FROM information_schema.tables
--    WHERE table_name = 'me_gusta_imagen';               -- esperado: 1
--   SELECT indexname FROM pg_indexes
--    WHERE tablename = 'me_gusta_imagen';                -- esperado: pkey + unique + 2 índices
--
-- Rollback (solo ANTES de desplegar el backend que lo usa):
--   DROP TABLE me_gusta_imagen;
