-- ============================================================
-- Script 37 — Reacciones a las novedades del publicador
-- ============================================================
-- ADITIVO PURO: una tabla nueva y nada más. No toca ningún CHECK
-- existente (a diferencia de los scripts 29, 30, 34, 35 y 36, que
-- tuvieron que reescribir el de `reporte.tipo_objeto`).
-- Plan: docs/plan-reacciones.md
--
-- Mismo molde que `me_gusta_imagen` (bloque 14): la fila existe o no
-- existe. Sin tipo de reacción — una sola, "me gusta" — porque un set
-- de emojis multiplica tabla, contadores, UI y decisiones de producto
-- sin resolver nada que un gesto simple no resuelva.
--
-- Las NOVEDADES son el único lugar donde esto hace falta: los eventos
-- ya tienen "Me interesa" (script 35), que ademas es mas accionable.
--
-- PRE:
--   SELECT COUNT(*) FROM information_schema.tables
--    WHERE table_name = 'me_gusta_novedad';
--   -- esperado: 0

BEGIN;

CREATE TABLE me_gusta_novedad (
    id BIGSERIAL PRIMARY KEY,

    usuario_id BIGINT NOT NULL,
    novedad_id BIGINT NOT NULL,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_me_gusta_novedad_usuario
        FOREIGN KEY (usuario_id)
        REFERENCES usuario (id)
        ON DELETE CASCADE,

    CONSTRAINT fk_me_gusta_novedad_novedad
        FOREIGN KEY (novedad_id)
        REFERENCES novedad (id)
        ON DELETE CASCADE,

    -- El UNIQUE es lo que hace idempotente al botón: reaccionar dos
    -- veces no suma dos.
    CONSTRAINT uq_me_gusta_novedad UNIQUE (usuario_id, novedad_id)
);

-- El contador de cada novedad (perfil público y feed).
CREATE INDEX idx_me_gusta_novedad_novedad
    ON me_gusta_novedad (novedad_id);

COMMIT;

-- POST:
--   SELECT table_name FROM information_schema.tables
--    WHERE table_name = 'me_gusta_novedad';
--   -- esperado: 1 fila
--   SELECT indexname FROM pg_indexes WHERE tablename = 'me_gusta_novedad';
--   -- esperado: la PK, uq_me_gusta_novedad y idx_me_gusta_novedad_novedad
--
-- Rollback (solo ANTES de desplegar el backend que lo usa):
--   DROP TABLE me_gusta_novedad;
