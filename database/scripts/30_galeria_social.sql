-- ============================================================
-- Script 30 — Fase 4 social: galería social
-- comentario_imagen + foto_guardada + columnas en imagen
-- + ALTER del CHECK de reporte (tipo COMENTARIO)
-- ============================================================
-- Aditiva. Plan: docs/plan-fase4-galeria-social.md
--
-- PRE:
--   SELECT COUNT(*) FROM information_schema.tables
--    WHERE table_name IN ('comentario_imagen','foto_guardada');
--   -- esperado: 0
--   SELECT COUNT(*) FROM information_schema.columns
--    WHERE table_name = 'imagen'
--      AND column_name IN ('comentarios_activados','seccion');
--   -- esperado: 0

BEGIN;

-- ============================================================
-- Comentarios en fotos: el primer texto libre de la comunidad.
-- Publica directo; se modera por estados (publicador/admin/autor).
-- ============================================================
CREATE TABLE comentario_imagen (
    id BIGSERIAL PRIMARY KEY,

    imagen_id BIGINT NOT NULL,

    usuario_id BIGINT NOT NULL,

    texto VARCHAR(500) NOT NULL,

    estado VARCHAR(30) NOT NULL DEFAULT 'VISIBLE',

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_comentario_imagen_imagen
        FOREIGN KEY (imagen_id) REFERENCES imagen (id) ON DELETE CASCADE,

    CONSTRAINT fk_comentario_imagen_usuario
        FOREIGN KEY (usuario_id) REFERENCES usuario (id) ON DELETE CASCADE,

    CONSTRAINT chk_comentario_imagen_estado
        CHECK (estado IN (
            'VISIBLE',
            'OCULTO_POR_PUBLICADOR',
            'OCULTO_POR_ADMIN',
            'ELIMINADO_POR_USUARIO'
        ))
);

CREATE INDEX idx_comentario_imagen_imagen
    ON comentario_imagen (imagen_id, estado, created_at);

-- Tope diario por usuario (consulta por fecha).
CREATE INDEX idx_comentario_imagen_usuario_fecha
    ON comentario_imagen (usuario_id, created_at);

-- ============================================================
-- Fotos guardadas (patrón exacto de me_gusta_imagen).
-- ============================================================
CREATE TABLE foto_guardada (
    id BIGSERIAL PRIMARY KEY,

    usuario_id BIGINT NOT NULL,

    imagen_id BIGINT NOT NULL,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_foto_guardada_usuario
        FOREIGN KEY (usuario_id) REFERENCES usuario (id) ON DELETE CASCADE,

    CONSTRAINT fk_foto_guardada_imagen
        FOREIGN KEY (imagen_id) REFERENCES imagen (id) ON DELETE CASCADE,

    CONSTRAINT uq_foto_guardada_usuario_imagen
        UNIQUE (usuario_id, imagen_id)
);

-- ============================================================
-- Columnas nuevas de imagen: toggle de comentarios y sección de
-- galería (catálogo fijo; NULL = "General", sin backfill).
-- ============================================================
ALTER TABLE imagen
    ADD COLUMN comentarios_activados BOOLEAN NOT NULL DEFAULT true,
    ADD COLUMN seccion VARCHAR(30) NULL;

ALTER TABLE imagen
    ADD CONSTRAINT chk_imagen_seccion
        CHECK (seccion IS NULL OR seccion IN (
            'INSTALACIONES',
            'ENTRENAMIENTOS',
            'EVENTOS',
            'EQUIPO'
        ));

-- ============================================================
-- El CHECK de reporte ENUMERA valores (lección del script 25).
-- ============================================================
ALTER TABLE reporte
    DROP CONSTRAINT chk_reporte_tipo_objeto;

ALTER TABLE reporte
    ADD CONSTRAINT chk_reporte_tipo_objeto
        CHECK (tipo_objeto IN (
            'IMAGEN',
            'PERFIL_PUBLICADOR',
            'ACTIVIDAD',
            'VALORACION',
            'PREGUNTA',
            'COMENTARIO'
        ));

COMMIT;

-- POST:
--   SELECT table_name FROM information_schema.tables
--    WHERE table_name IN ('comentario_imagen','foto_guardada');
--   -- esperado: 2 filas
--   SELECT column_name, is_nullable, column_default
--     FROM information_schema.columns
--    WHERE table_name = 'imagen'
--      AND column_name IN ('comentarios_activados','seccion');
--   -- esperado: comentarios_activados NO default true; seccion YES
--   SELECT pg_get_constraintdef(oid) FROM pg_constraint
--    WHERE conname = 'chk_reporte_tipo_objeto';
--   -- esperado: incluye COMENTARIO
--
-- Rollback (solo ANTES de desplegar el backend que lo usa):
--   volver el CHECK de reporte a los 5 tipos anteriores;
--   ALTER TABLE imagen DROP CONSTRAINT chk_imagen_seccion;
--   ALTER TABLE imagen DROP COLUMN seccion, DROP COLUMN comentarios_activados;
--   DROP TABLE foto_guardada; DROP TABLE comentario_imagen;
