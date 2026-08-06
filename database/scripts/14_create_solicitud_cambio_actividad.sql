-- ============================================================
-- 14 - SOLICITUD DE CAMBIO DE ACTIVIDAD
-- ============================================================
--
-- Crea la tabla que soporta el flujo de edicion con revision:
-- el publicador propone cambios sobre una actividad ya publicada
-- y la actividad publica NO cambia hasta que un admin aprueba.
--
-- Cada columna de datos propuesta es NULLABLE:
-- NULL significa "sin cambio propuesto" para ese campo.
--
-- La tabla tambien funciona como historial: las solicitudes
-- aprobadas o rechazadas no se borran.
--
-- Migracion aditiva: no modifica tablas existentes.
-- Ver docs/plan-solicitud-cambio-actividad.md
-- ============================================================

CREATE TABLE solicitud_cambio_actividad (
    id BIGSERIAL PRIMARY KEY,

    -- Actividad y ownership
    actividad_id BIGINT NOT NULL,

    perfil_publicador_id BIGINT NOT NULL,

    usuario_id BIGINT NOT NULL,

    -- Flujo de revision
    estado VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE',

    -- Campos propuestos (NULL = sin cambio propuesto)
    titulo VARCHAR(150),

    descripcion TEXT,

    precio_referencia NUMERIC(10,2),

    mostrar_precio BOOLEAN,

    whatsapp_contacto VARCHAR(30),

    instagram_contacto VARCHAR(150),

    email_contacto VARCHAR(150),

    nivel VARCHAR(50),

    modalidad VARCHAR(50),

    -- Resolucion administrativa
    motivo_rechazo TEXT,

    resuelto_por_usuario_id BIGINT,

    resuelto_at TIMESTAMPTZ,

    -- Auditoria y baja logica
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Las actualizaciones posteriores de updated_at seran responsabilidad del backend.
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    deleted_at TIMESTAMPTZ,

    -- Relaciones. No borran solicitudes en cascada.
    CONSTRAINT fk_solicitud_cambio_actividad_actividad
        FOREIGN KEY (actividad_id)
        REFERENCES actividad(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_solicitud_cambio_actividad_perfil
        FOREIGN KEY (perfil_publicador_id)
        REFERENCES perfil_publicador(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_solicitud_cambio_actividad_usuario
        FOREIGN KEY (usuario_id)
        REFERENCES usuario(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_solicitud_cambio_actividad_resuelto_por
        FOREIGN KEY (resuelto_por_usuario_id)
        REFERENCES usuario(id)
        ON DELETE RESTRICT,

    -- Estados permitidos del flujo.
    CONSTRAINT chk_solicitud_cambio_actividad_estado
        CHECK (
            estado IN (
                'PENDIENTE',
                'EN_REVISION',
                'APROBADA',
                'RECHAZADA'
            )
        ),

    -- Si se rechaza, debe haber motivo. Si no se rechaza, el motivo debe quedar vacio.
    CONSTRAINT chk_solicitud_cambio_actividad_motivo_rechazo
        CHECK (
            (
                estado = 'RECHAZADA'
                AND motivo_rechazo IS NOT NULL
                AND btrim(motivo_rechazo) <> ''
            )
            OR (
                estado <> 'RECHAZADA'
                AND motivo_rechazo IS NULL
            )
        ),

    -- Una solicitud sin ningun campo propuesto no tiene sentido.
    CONSTRAINT chk_solicitud_cambio_actividad_algun_campo
        CHECK (
            titulo IS NOT NULL
            OR descripcion IS NOT NULL
            OR precio_referencia IS NOT NULL
            OR mostrar_precio IS NOT NULL
            OR whatsapp_contacto IS NOT NULL
            OR instagram_contacto IS NOT NULL
            OR email_contacto IS NOT NULL
            OR nivel IS NOT NULL
            OR modalidad IS NOT NULL
        ),

    -- Dominios alineados con la tabla actividad.
    CONSTRAINT chk_solicitud_cambio_actividad_nivel
        CHECK (
            nivel IS NULL
            OR nivel IN (
                'PRINCIPIANTE',
                'INTERMEDIO',
                'AVANZADO',
                'TODOS'
            )
        ),

    CONSTRAINT chk_solicitud_cambio_actividad_modalidad
        CHECK (
            modalidad IS NULL
            OR modalidad IN (
                'PRESENCIAL',
                'ONLINE',
                'MIXTA'
            )
        ),

    CONSTRAINT chk_solicitud_cambio_actividad_precio
        CHECK (
            precio_referencia IS NULL
            OR precio_referencia >= 0
        ),

    -- Una solicitud resuelta debe tener quien y cuando; una abierta, no.
    CONSTRAINT chk_solicitud_cambio_actividad_resolucion
        CHECK (
            (
                estado IN ('APROBADA', 'RECHAZADA')
                AND resuelto_por_usuario_id IS NOT NULL
                AND resuelto_at IS NOT NULL
            )
            OR (
                estado IN ('PENDIENTE', 'EN_REVISION')
                AND resuelto_por_usuario_id IS NULL
                AND resuelto_at IS NULL
            )
        )
);


-- ============================================================
-- INDICES
-- ============================================================

-- Solo puede haber UNA solicitud abierta por actividad.
CREATE UNIQUE INDEX uq_solicitud_cambio_actividad_abierta
ON solicitud_cambio_actividad (actividad_id)
WHERE estado IN ('PENDIENTE', 'EN_REVISION') AND deleted_at IS NULL;

-- Listados del publicador.
CREATE INDEX idx_solicitud_cambio_actividad_perfil
ON solicitud_cambio_actividad (perfil_publicador_id);

-- Cola de revision del admin.
CREATE INDEX idx_solicitud_cambio_actividad_estado
ON solicitud_cambio_actividad (estado);

-- Historial por actividad.
CREATE INDEX idx_solicitud_cambio_actividad_actividad
ON solicitud_cambio_actividad (actividad_id);
