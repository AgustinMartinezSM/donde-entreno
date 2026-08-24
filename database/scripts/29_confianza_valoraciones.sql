-- ============================================================
-- Script 29 — Fase 3 social: confianza
-- interes_actividad + valoracion + pregunta_actividad
-- + ALTER del CHECK de reporte (tipos nuevos)
-- ============================================================
-- Aditiva. Plan: docs/plan-fase3-confianza.md
--
-- PRE:
--   SELECT COUNT(*) FROM information_schema.tables
--    WHERE table_name IN ('interes_actividad','valoracion','pregunta_actividad');
--   -- esperado: 0
--   SELECT pg_get_constraintdef(oid) FROM pg_constraint
--    WHERE conname = 'chk_reporte_tipo_objeto';
--   -- esperado: IMAGEN, PERFIL_PUBLICADOR, ACTIVIDAD (sin los nuevos)

BEGIN;

-- ============================================================
-- El flujo propio: Guardé → Quiero probar → Ya probé.
-- UNIQUE (usuario, actividad): es una transición de estado, no filas.
-- ============================================================
CREATE TABLE interes_actividad (
    id BIGSERIAL PRIMARY KEY,

    usuario_id BIGINT NOT NULL,

    actividad_id BIGINT NOT NULL,

    estado VARCHAR(20) NOT NULL,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_interes_actividad_usuario
        FOREIGN KEY (usuario_id) REFERENCES usuario (id) ON DELETE CASCADE,

    CONSTRAINT fk_interes_actividad_actividad
        FOREIGN KEY (actividad_id) REFERENCES actividad (id) ON DELETE CASCADE,

    CONSTRAINT chk_interes_actividad_estado
        CHECK (estado IN ('QUIERO_PROBAR', 'YA_PROBE')),

    CONSTRAINT uq_interes_actividad_usuario_actividad
        UNIQUE (usuario_id, actividad_id)
);

CREATE INDEX idx_interes_actividad_actividad
    ON interes_actividad (actividad_id, estado);

-- ============================================================
-- Valoraciones 1-5 con reseña y tags de catálogo fijo.
-- UNIQUE (usuario, actividad): una valoración por persona, editable.
-- ============================================================
CREATE TABLE valoracion (
    id BIGSERIAL PRIMARY KEY,

    usuario_id BIGINT NOT NULL,

    actividad_id BIGINT NOT NULL,

    puntaje SMALLINT NOT NULL,

    comentario VARCHAR(500),

    -- CSV de un catálogo fijo, validado en el service.
    tags VARCHAR(255),

    -- true = al valorar tenía señal FUERTE (YA_PROBE o check-in).
    verificada BOOLEAN NOT NULL DEFAULT false,

    estado VARCHAR(20) NOT NULL DEFAULT 'VISIBLE',

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_valoracion_usuario
        FOREIGN KEY (usuario_id) REFERENCES usuario (id) ON DELETE CASCADE,

    CONSTRAINT fk_valoracion_actividad
        FOREIGN KEY (actividad_id) REFERENCES actividad (id) ON DELETE CASCADE,

    CONSTRAINT chk_valoracion_puntaje
        CHECK (puntaje BETWEEN 1 AND 5),

    CONSTRAINT chk_valoracion_estado
        CHECK (estado IN ('VISIBLE', 'OCULTA_POR_ADMIN')),

    CONSTRAINT uq_valoracion_usuario_actividad
        UNIQUE (usuario_id, actividad_id)
);

CREATE INDEX idx_valoracion_actividad
    ON valoracion (actividad_id, estado);

-- ============================================================
-- Preguntas y respuestas (patrón MercadoLibre: la respuesta única
-- del publicador vive EN la fila de la pregunta).
-- ============================================================
CREATE TABLE pregunta_actividad (
    id BIGSERIAL PRIMARY KEY,

    actividad_id BIGINT NOT NULL,

    usuario_id BIGINT NOT NULL,

    pregunta VARCHAR(500) NOT NULL,

    respuesta VARCHAR(1000),

    respondida_at TIMESTAMPTZ,

    estado VARCHAR(30) NOT NULL DEFAULT 'VISIBLE',

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_pregunta_actividad_actividad
        FOREIGN KEY (actividad_id) REFERENCES actividad (id) ON DELETE CASCADE,

    CONSTRAINT fk_pregunta_actividad_usuario
        FOREIGN KEY (usuario_id) REFERENCES usuario (id) ON DELETE CASCADE,

    CONSTRAINT chk_pregunta_actividad_estado
        CHECK (estado IN ('VISIBLE', 'OCULTA_POR_ADMIN', 'ELIMINADA_POR_USUARIO'))
);

CREATE INDEX idx_pregunta_actividad_actividad
    ON pregunta_actividad (actividad_id, estado, created_at);

-- Rate limit de preguntas por día (consulta por usuario y fecha).
CREATE INDEX idx_pregunta_actividad_usuario_fecha
    ON pregunta_actividad (usuario_id, created_at);

-- ============================================================
-- El CHECK de reporte ENUMERA valores (lección del script 25):
-- los tipos nuevos exigen redefinirlo.
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
            'PREGUNTA'
        ));

COMMIT;

-- POST:
--   SELECT table_name FROM information_schema.tables
--    WHERE table_name IN ('interes_actividad','valoracion','pregunta_actividad');
--   -- esperado: 3 filas
--   SELECT pg_get_constraintdef(oid) FROM pg_constraint
--    WHERE conname = 'chk_reporte_tipo_objeto';
--   -- esperado: incluye VALORACION y PREGUNTA
--
-- Rollback (solo ANTES de desplegar el backend que las usa):
--   volver el CHECK de reporte a los 3 tipos originales;
--   DROP TABLE pregunta_actividad; DROP TABLE valoracion;
--   DROP TABLE interes_actividad;
