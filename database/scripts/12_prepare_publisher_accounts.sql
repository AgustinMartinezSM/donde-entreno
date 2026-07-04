BEGIN;

-- ============================================================
-- DondeEntreno - Preparacion de cuentas publicador
-- Archivo: 12_prepare_publisher_accounts.sql
-- Base de datos: PostgreSQL
-- Descripcion:
-- Esta migracion prepara la base para registro unificado,
-- cuentas de publicador, asociacion de solicitudes con usuarios
-- y futuro panel publicador V1.
--
-- Es una migracion aditiva, segura y no destructiva:
-- - no borra datos;
-- - no modifica roles existentes;
-- - no inserta usuarios reales;
-- - no inserta administradores;
-- - no modifica actividad ni horarios;
-- - mantiene compatibles las solicitudes historicas anonimas.
-- ============================================================

-- ============================================================
-- 1. USUARIO
-- ------------------------------------------------------------
-- Preparamos telefono normalizado para busquedas o validaciones
-- futuras sin exigir telefono obligatorio ni unicidad.
-- telefono_verificado queda listo para una futura verificacion,
-- pero no habilita todavia login por telefono ni recuperacion.
-- ============================================================

ALTER TABLE usuario
ADD COLUMN telefono_normalizado VARCHAR(30);

ALTER TABLE usuario
ADD COLUMN telefono_verificado BOOLEAN NOT NULL DEFAULT false;

COMMENT ON COLUMN usuario.telefono_normalizado IS
    'Telefono del usuario normalizado solo con digitos para busqueda o validaciones futuras. No es unico.';

COMMENT ON COLUMN usuario.telefono_verificado IS
    'Indica si el telefono fue verificado. Preparado para futuras verificaciones.';

UPDATE usuario
SET telefono_normalizado = NULLIF(
    REGEXP_REPLACE(telefono, '[^0-9]+', '', 'g'),
    ''
)
WHERE telefono IS NOT NULL
  AND BTRIM(telefono) <> '';

ALTER TABLE usuario
ADD CONSTRAINT chk_usuario_telefono_normalizado_formato
    CHECK (
        telefono_normalizado IS NULL
        OR telefono_normalizado ~ '^[0-9]+$'
    );

CREATE INDEX idx_usuario_telefono_normalizado
ON usuario (telefono_normalizado);

-- ============================================================
-- 2. PERFIL PUBLICADOR
-- ------------------------------------------------------------
-- estado representa la situacion funcional del publicador para
-- el futuro panel. No reemplaza por ahora activo/verificado.
--
-- ciudad_principal_id permite asociar territorialmente el perfil
-- sin hacer obligatorio ese dato.
--
-- Los contactos normalizados permiten validaciones futuras sin
-- cambiar los valores visibles ni exigir unicidad.
-- ============================================================

ALTER TABLE perfil_publicador
ADD COLUMN estado VARCHAR(30);

ALTER TABLE perfil_publicador
ADD COLUMN ciudad_principal_id BIGINT;

ALTER TABLE perfil_publicador
ADD COLUMN whatsapp_normalizado VARCHAR(30);

ALTER TABLE perfil_publicador
ADD COLUMN telefono_contacto_normalizado VARCHAR(30);

COMMENT ON COLUMN perfil_publicador.estado IS
    'Estado funcional del perfil publicador para registro, revision y panel publicador.';

COMMENT ON COLUMN perfil_publicador.ciudad_principal_id IS
    'Ciudad principal asociada al perfil publicador. Dato opcional.';

COMMENT ON COLUMN perfil_publicador.whatsapp_normalizado IS
    'WhatsApp del perfil publicador normalizado solo con digitos. No es unico.';

COMMENT ON COLUMN perfil_publicador.telefono_contacto_normalizado IS
    'Telefono de contacto del perfil publicador normalizado solo con digitos. No es unico.';

UPDATE perfil_publicador
SET estado = CASE
    WHEN deleted_at IS NOT NULL OR activo = false THEN 'SUSPENDIDO'
    WHEN activo = true AND verificado = true THEN 'ACTIVO'
    WHEN activo = true AND verificado = false THEN 'PENDIENTE_REVISION'
    ELSE 'INCOMPLETO'
END
WHERE estado IS NULL;

UPDATE perfil_publicador
SET whatsapp_normalizado = NULLIF(
    REGEXP_REPLACE(whatsapp, '[^0-9]+', '', 'g'),
    ''
)
WHERE whatsapp IS NOT NULL
  AND BTRIM(whatsapp) <> '';

UPDATE perfil_publicador
SET telefono_contacto_normalizado = NULLIF(
    REGEXP_REPLACE(telefono_contacto, '[^0-9]+', '', 'g'),
    ''
)
WHERE telefono_contacto IS NOT NULL
  AND BTRIM(telefono_contacto) <> '';

ALTER TABLE perfil_publicador
ALTER COLUMN estado SET DEFAULT 'INCOMPLETO';

ALTER TABLE perfil_publicador
ALTER COLUMN estado SET NOT NULL;

ALTER TABLE perfil_publicador
ADD CONSTRAINT fk_perfil_publicador_ciudad_principal
    FOREIGN KEY (ciudad_principal_id)
    REFERENCES ciudad(id)
    ON DELETE RESTRICT;

ALTER TABLE perfil_publicador
ADD CONSTRAINT chk_perfil_publicador_estado
    CHECK (
        estado IN (
            'INCOMPLETO',
            'PENDIENTE_REVISION',
            'ACTIVO',
            'SUSPENDIDO'
        )
    );

ALTER TABLE perfil_publicador
ADD CONSTRAINT chk_perfil_publicador_whatsapp_normalizado_formato
    CHECK (
        whatsapp_normalizado IS NULL
        OR whatsapp_normalizado ~ '^[0-9]+$'
    );

ALTER TABLE perfil_publicador
ADD CONSTRAINT chk_perfil_publicador_telefono_contacto_normalizado_formato
    CHECK (
        telefono_contacto_normalizado IS NULL
        OR telefono_contacto_normalizado ~ '^[0-9]+$'
    );

CREATE INDEX idx_perfil_publicador_ciudad_principal_id
ON perfil_publicador (ciudad_principal_id);

CREATE INDEX idx_perfil_publicador_whatsapp_normalizado
ON perfil_publicador (whatsapp_normalizado);

CREATE INDEX idx_perfil_publicador_telefono_contacto_normalizado
ON perfil_publicador (telefono_contacto_normalizado);

CREATE INDEX idx_perfil_publicador_estado
ON perfil_publicador (estado);

CREATE INDEX idx_perfil_publicador_usuario_estado
ON perfil_publicador (usuario_id, estado);

-- ============================================================
-- 3. SOLICITUD PUBLICACION
-- ------------------------------------------------------------
-- Las columnas nuevas permiten saber que usuario y que perfil
-- enviaron una solicitud autenticada.
--
-- Permanecen NULL para mantener compatibles solicitudes viejas
-- o anonimas enviadas por el formulario publico actual.
-- ============================================================

ALTER TABLE solicitud_publicacion
ADD COLUMN usuario_id BIGINT;

ALTER TABLE solicitud_publicacion
ADD COLUMN perfil_publicador_id BIGINT;

COMMENT ON COLUMN solicitud_publicacion.usuario_id IS
    'Usuario que envio la solicitud cuando el flujo sea autenticado. NULL para solicitudes historicas o anonimas.';

COMMENT ON COLUMN solicitud_publicacion.perfil_publicador_id IS
    'Perfil publicador desde el que se envio la solicitud. NULL para solicitudes historicas o anonimas.';

ALTER TABLE solicitud_publicacion
ADD CONSTRAINT fk_solicitud_publicacion_usuario
    FOREIGN KEY (usuario_id)
    REFERENCES usuario(id)
    ON DELETE RESTRICT;

ALTER TABLE solicitud_publicacion
ADD CONSTRAINT fk_solicitud_publicacion_perfil_publicador
    FOREIGN KEY (perfil_publicador_id)
    REFERENCES perfil_publicador(id)
    ON DELETE RESTRICT;

ALTER TABLE solicitud_publicacion
ADD CONSTRAINT chk_solicitud_publicacion_perfil_requiere_usuario
    CHECK (
        perfil_publicador_id IS NULL
        OR usuario_id IS NOT NULL
    );

CREATE INDEX idx_solicitud_publicacion_usuario_id
ON solicitud_publicacion (usuario_id);

CREATE INDEX idx_solicitud_publicacion_perfil_publicador_id
ON solicitud_publicacion (perfil_publicador_id);

CREATE INDEX idx_solicitud_publicacion_perfil_estado
ON solicitud_publicacion (perfil_publicador_id, estado);

CREATE INDEX idx_solicitud_publicacion_usuario_created_at
ON solicitud_publicacion (usuario_id, created_at DESC);

COMMIT;
