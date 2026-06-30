-- ============================================================
-- DondeEntreno - Preparacion de Auth y Seguridad
-- Archivo: 07_prepare_auth_security.sql
-- Base de datos: PostgreSQL
-- Descripcion:
-- Esta migracion prepara reglas minimas de seguridad para usuarios
-- y roles antes de implementar autenticacion real en el backend.
--
-- Es una migracion aditiva, segura y no destructiva:
-- - no crea usuarios administradores;
-- - no modifica hashes existentes;
-- - no agrega tokens ni secretos;
-- - no cambia relaciones existentes.
-- ============================================================

BEGIN;

-- ============================================================
-- VERIFICACIONES PREVIAS
-- ------------------------------------------------------------
-- Antes de cambiar constraints o crear el indice unico funcional,
-- verificamos que los datos actuales no rompan las nuevas reglas.
-- Los mensajes no exponen emails concretos.
-- ============================================================

DO $$
DECLARE
    cantidad_nombres_vacios INTEGER;
    cantidad_emails_vacios INTEGER;
    cantidad_hashes_vacios INTEGER;
    cantidad_emails_normalizados_duplicados INTEGER;
BEGIN
    SELECT COUNT(*)
    INTO cantidad_nombres_vacios
    FROM usuario
    WHERE nombre IS NULL
       OR BTRIM(nombre) = '';

    IF cantidad_nombres_vacios > 0 THEN
        RAISE EXCEPTION
            'No se puede aplicar la migracion: existen usuarios con nombre vacio o compuesto solo por espacios. Cantidad: %',
            cantidad_nombres_vacios;
    END IF;

    SELECT COUNT(*)
    INTO cantidad_emails_vacios
    FROM usuario
    WHERE email IS NULL
       OR BTRIM(email) = '';

    IF cantidad_emails_vacios > 0 THEN
        RAISE EXCEPTION
            'No se puede aplicar la migracion: existen usuarios con email vacio o compuesto solo por espacios. Cantidad: %',
            cantidad_emails_vacios;
    END IF;

    SELECT COUNT(*)
    INTO cantidad_hashes_vacios
    FROM usuario
    WHERE password_hash IS NULL
       OR BTRIM(password_hash) = '';

    IF cantidad_hashes_vacios > 0 THEN
        RAISE EXCEPTION
            'No se puede aplicar la migracion: existen usuarios con password_hash vacio o compuesto solo por espacios. Cantidad: %',
            cantidad_hashes_vacios;
    END IF;

    SELECT COUNT(*)
    INTO cantidad_emails_normalizados_duplicados
    FROM (
        SELECT LOWER(BTRIM(email)) AS email_normalizado
        FROM usuario
        GROUP BY LOWER(BTRIM(email))
        HAVING COUNT(*) > 1
    ) duplicados;

    IF cantidad_emails_normalizados_duplicados > 0 THEN
        RAISE EXCEPTION
            'No se puede aplicar la migracion: existen emails duplicados al normalizar mayusculas, minusculas o espacios externos. Cantidad de grupos duplicados: %',
            cantidad_emails_normalizados_duplicados;
    END IF;
END; $$;

-- ============================================================
-- ROLES
-- ------------------------------------------------------------
-- Ampliamos los roles permitidos para sumar cuentas comunes.
-- No cambiamos IDs de roles existentes.
-- ============================================================

ALTER TABLE rol
DROP CONSTRAINT chk_rol_nombre;

ALTER TABLE rol
ADD CONSTRAINT chk_rol_nombre
    CHECK (
        nombre IN (
            'SUPER_ADMIN',
            'ADMIN',
            'PUBLICADOR',
            'USUARIO'
        )
    );

-- Insertamos el rol USUARIO de forma idempotente.
-- Representa una cuenta comun de visitante registrado.
INSERT INTO rol (nombre, descripcion, activo)
VALUES (
    'USUARIO',
    'Cuenta comun para personas registradas que pueden usar funciones basicas de la plataforma.',
    true
)
ON CONFLICT (nombre) DO NOTHING;

-- ============================================================
-- RESTRICCIONES DE USUARIO
-- ------------------------------------------------------------
-- Evitamos valores vacios o compuestos solo por espacios en campos
-- obligatorios para autenticacion.
-- No agregamos todavia una regex BCrypt porque existen placeholders
-- historicos de prueba.
-- ============================================================

ALTER TABLE usuario
ADD CONSTRAINT chk_usuario_nombre_no_vacio
    CHECK (BTRIM(nombre) <> '');

ALTER TABLE usuario
ADD CONSTRAINT chk_usuario_email_no_vacio
    CHECK (BTRIM(email) <> '');

ALTER TABLE usuario
ADD CONSTRAINT chk_usuario_password_hash_no_vacio
    CHECK (BTRIM(password_hash) <> '');

-- ============================================================
-- EMAIL UNICO NORMALIZADO
-- ------------------------------------------------------------
-- Este indice evita duplicados por mayusculas, minusculas o espacios
-- externos.
--
-- El backend igualmente debe guardar y buscar emails normalizados.
--
-- La unicidad aplica tambien a usuarios con deleted_at, por lo que
-- un email de una cuenta eliminada logicamente no se reutiliza
-- automaticamente.
--
-- Conservamos la restriccion UNIQUE(email) existente.
-- ============================================================

CREATE UNIQUE INDEX idx_usuario_email_normalizado_unico
ON usuario (LOWER(BTRIM(email)));

COMMIT;
