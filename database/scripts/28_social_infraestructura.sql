-- ============================================================
-- Script 28 — Infraestructura social (Fase 2)
-- notificacion + reporte + evento_interaccion
-- ============================================================
-- Aditiva pura: tres tablas nuevas, el código desplegado las ignora
-- (migración ANTES que código). Plan:
-- docs/plan-fase2-social-infraestructura.md
--
-- PRE:
--   SELECT COUNT(*) FROM information_schema.tables
--    WHERE table_name IN ('notificacion','reporte','evento_interaccion');
--   -- esperado: 0

BEGIN;

-- ============================================================
-- Notificaciones internas (campanita). Sin email ni push en V1.
-- ============================================================
CREATE TABLE notificacion (
    id BIGSERIAL PRIMARY KEY,

    usuario_id BIGINT NOT NULL,

    -- Catálogo abierto a propósito (cada fase suma tipos); el CHECK
    -- fijo obligaría una migración por tipo nuevo.
    tipo VARCHAR(40) NOT NULL,

    titulo VARCHAR(150) NOT NULL,

    -- Link interno al que navega el click (nullable: hay avisos sin destino).
    ruta VARCHAR(255),

    leida BOOLEAN NOT NULL DEFAULT false,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_notificacion_usuario
        FOREIGN KEY (usuario_id)
        REFERENCES usuario (id)
        ON DELETE CASCADE
);

-- Contador de no leídas y listado por fecha.
CREATE INDEX idx_notificacion_usuario_leida
    ON notificacion (usuario_id, leida);
CREATE INDEX idx_notificacion_usuario_fecha
    ON notificacion (usuario_id, created_at DESC);

-- ============================================================
-- Reportes (la base de la moderación flexible).
-- ============================================================
CREATE TABLE reporte (
    id BIGSERIAL PRIMARY KEY,

    usuario_id BIGINT NOT NULL,

    tipo_objeto VARCHAR(30) NOT NULL,

    objeto_id BIGINT NOT NULL,

    motivo VARCHAR(40) NOT NULL,

    detalle VARCHAR(280),

    estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_reporte_usuario
        FOREIGN KEY (usuario_id)
        REFERENCES usuario (id)
        ON DELETE CASCADE,

    CONSTRAINT chk_reporte_tipo_objeto
        CHECK (tipo_objeto IN ('IMAGEN', 'PERFIL_PUBLICADOR', 'ACTIVIDAD')),

    CONSTRAINT chk_reporte_motivo
        CHECK (motivo IN (
            'CONTENIDO_INAPROPIADO',
            'INFORMACION_FALSA',
            'SPAM',
            'SUPLANTACION',
            'OTRO'
        )),

    CONSTRAINT chk_reporte_estado
        CHECK (estado IN ('PENDIENTE', 'REVISADO', 'DESESTIMADO', 'ACCIONADO')),

    -- Reportar dos veces lo mismo no duplica (idempotente en el service).
    CONSTRAINT uq_reporte_usuario_objeto
        UNIQUE (usuario_id, tipo_objeto, objeto_id)
);

CREATE INDEX idx_reporte_estado_fecha
    ON reporte (estado, created_at DESC);

-- ============================================================
-- Interacciones (tracking anónimo y agregado).
-- SIN usuario_id A PROPÓSITO: privacidad primero — solo se cuentan
-- eventos, nunca quién.
-- ============================================================
CREATE TABLE evento_interaccion (
    id BIGSERIAL PRIMARY KEY,

    actividad_id BIGINT NOT NULL,

    tipo VARCHAR(30) NOT NULL,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_evento_interaccion_actividad
        FOREIGN KEY (actividad_id)
        REFERENCES actividad (id)
        ON DELETE CASCADE,

    CONSTRAINT chk_evento_interaccion_tipo
        CHECK (tipo IN ('VISTA_DETALLE', 'CLICK_WHATSAPP', 'CLICK_COMPARTIR'))
);

CREATE INDEX idx_evento_interaccion_actividad
    ON evento_interaccion (actividad_id, tipo, created_at);

COMMIT;

-- POST:
--   SELECT table_name FROM information_schema.tables
--    WHERE table_name IN ('notificacion','reporte','evento_interaccion');
--   -- esperado: 3 filas
--   SELECT indexname FROM pg_indexes WHERE tablename = 'notificacion';
--   -- esperado: pkey + 2
--
-- Rollback (solo ANTES de desplegar el backend que las usa):
--   DROP TABLE evento_interaccion;
--   DROP TABLE reporte;
--   DROP TABLE notificacion;
