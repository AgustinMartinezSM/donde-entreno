-- ============================================================
-- Script 24 — Solicitudes de cambio completas
-- (horarios, ubicación, deporte, edades y enfoque)
-- ============================================================
-- Aditiva pura: columnas nullable (= sin cambio propuesto) y una tabla
-- hija calcada de solicitud_publicacion_horario. El código desplegado
-- la ignora (migración ANTES que código).
--
-- PRE:
--   SELECT column_name FROM information_schema.columns
--    WHERE table_name = 'solicitud_cambio_actividad'
--      AND column_name IN ('deporte_id','edad_minima','edad_maxima',
--                          'enfoque','ubicacion_nombre','cambia_horarios');
--   -- esperado: 0 filas
--   SELECT COUNT(*) FROM information_schema.tables
--    WHERE table_name = 'solicitud_cambio_horario';       -- esperado: 0

BEGIN;

ALTER TABLE solicitud_cambio_actividad
    ADD COLUMN deporte_id BIGINT NULL
        REFERENCES deporte (id),
    ADD COLUMN edad_minima INT NULL,
    ADD COLUMN edad_maxima INT NULL,
    ADD COLUMN enfoque VARCHAR(50) NULL,
    ADD COLUMN ubicacion_nombre VARCHAR(150) NULL,
    ADD COLUMN ubicacion_direccion VARCHAR(255) NULL,
    ADD COLUMN ubicacion_referencia VARCHAR(255) NULL,
    ADD COLUMN ubicacion_barrio_id BIGINT NULL
        REFERENCES barrio (id),
    -- true = la solicitud propone REEMPLAZAR el conjunto de horarios
    -- por las filas de solicitud_cambio_horario. El flag existe porque
    -- "cero filas hijas" no distingue "no toco horarios" de "borrar
    -- todos": con flag true el backend exige al menos un horario.
    ADD COLUMN cambia_horarios BOOLEAN NOT NULL DEFAULT false;

COMMENT ON COLUMN solicitud_cambio_actividad.cambia_horarios IS
    'true = reemplazo total del conjunto de horarios por las filas hijas de solicitud_cambio_horario (>=1 exigido por el backend).';

-- Horarios PROPUESTOS de la solicitud (calco de
-- solicitud_publicacion_horario). Al aprobar, los vigentes pasan a
-- activo=false y se crean estos.
CREATE TABLE solicitud_cambio_horario (
    id BIGSERIAL PRIMARY KEY,

    solicitud_cambio_actividad_id BIGINT NOT NULL,

    dia_semana VARCHAR(20) NOT NULL,

    hora_inicio TIME NOT NULL,

    hora_fin TIME NOT NULL,

    observacion VARCHAR(255),

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    -- La única cascada: un horario propuesto es parte de su solicitud.
    CONSTRAINT fk_solicitud_cambio_horario_solicitud
        FOREIGN KEY (solicitud_cambio_actividad_id)
        REFERENCES solicitud_cambio_actividad (id)
        ON DELETE CASCADE,

    -- Días permitidos, compatibles con horario_actividad.
    CONSTRAINT chk_solicitud_cambio_horario_dia_semana
        CHECK (
            dia_semana IN (
                'LUNES',
                'MARTES',
                'MIERCOLES',
                'JUEVES',
                'VIERNES',
                'SABADO',
                'DOMINGO'
            )
        ),

    -- Un horario que termina antes de empezar no es un horario.
    CONSTRAINT chk_solicitud_cambio_horario_horas
        CHECK (hora_fin > hora_inicio)
);

CREATE INDEX idx_solicitud_cambio_horario_solicitud
    ON solicitud_cambio_horario (solicitud_cambio_actividad_id);

COMMIT;

-- POST:
--   SELECT column_name, is_nullable FROM information_schema.columns
--    WHERE table_name = 'solicitud_cambio_actividad'
--      AND column_name IN ('deporte_id','edad_minima','edad_maxima',
--                          'enfoque','ubicacion_nombre','ubicacion_direccion',
--                          'ubicacion_referencia','ubicacion_barrio_id',
--                          'cambia_horarios');
--   -- esperado: 9 filas (todas YES salvo cambia_horarios NO)
--   SELECT COUNT(*) FROM information_schema.tables
--    WHERE table_name = 'solicitud_cambio_horario';       -- esperado: 1
--
-- Rollback (solo ANTES de desplegar el backend que lo usa):
--   DROP TABLE solicitud_cambio_horario;
--   ALTER TABLE solicitud_cambio_actividad
--     DROP COLUMN cambia_horarios,
--     DROP COLUMN ubicacion_barrio_id,
--     DROP COLUMN ubicacion_referencia,
--     DROP COLUMN ubicacion_direccion,
--     DROP COLUMN ubicacion_nombre,
--     DROP COLUMN enfoque,
--     DROP COLUMN edad_maxima,
--     DROP COLUMN edad_minima,
--     DROP COLUMN deporte_id;
