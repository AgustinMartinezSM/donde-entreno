-- ============================================================
-- DondeEntreno - Validacion local de Auth y Seguridad
-- Archivo: 08_test_auth_security_queries.sql
-- Base de datos: PostgreSQL
-- Descripcion:
-- Este script valida la migracion 07 en una base local.
--
-- Debe ejecutarse solo despues de aplicar:
-- 07_prepare_auth_security.sql
--
-- Todo corre dentro de una transaccion y termina con ROLLBACK para
-- no conservar usuarios ni roles temporales.
-- ============================================================

BEGIN;

-- ============================================================
-- 1. VALIDACION ESTRUCTURAL DE ROLES, CONSTRAINTS E INDICE
-- ============================================================

-- Existencia del rol USUARIO.
SELECT
    'Rol USUARIO existente' AS verificacion,
    id,
    nombre,
    descripcion,
    activo
FROM rol
WHERE nombre = 'USUARIO';

DO $$
DECLARE
    cantidad_roles_usuario INTEGER;
BEGIN
    SELECT COUNT(*)
    INTO cantidad_roles_usuario
    FROM rol
    WHERE nombre = 'USUARIO';

    IF cantidad_roles_usuario = 1 THEN
        RAISE NOTICE 'OK: existe exactamente un rol USUARIO.';
    ELSE
        RAISE WARNING 'FALLO: se esperaban 1 rol USUARIO y se encontraron %.', cantidad_roles_usuario;
    END IF;
END; $$;

-- Valores permitidos por chk_rol_nombre.
SELECT
    'Constraint chk_rol_nombre' AS verificacion,
    conname AS constraint_name,
    pg_get_constraintdef(oid) AS definition
FROM pg_constraint
WHERE conrelid = 'rol'::regclass
  AND conname = 'chk_rol_nombre';

DO $$
DECLARE
    definicion TEXT;
BEGIN
    SELECT pg_get_constraintdef(oid)
    INTO definicion
    FROM pg_constraint
    WHERE conrelid = 'rol'::regclass
      AND conname = 'chk_rol_nombre';

    IF definicion IS NULL THEN
        RAISE WARNING 'FALLO: no existe chk_rol_nombre.';
    ELSIF definicion LIKE '%SUPER_ADMIN%'
       AND definicion LIKE '%ADMIN%'
       AND definicion LIKE '%PUBLICADOR%'
       AND definicion LIKE '%USUARIO%' THEN
        RAISE NOTICE 'OK: chk_rol_nombre contempla los roles esperados.';
    ELSE
        RAISE WARNING 'FALLO: chk_rol_nombre no contempla los roles esperados.';
    END IF;
END; $$;

-- Existencia de las nuevas constraints de usuario.
SELECT
    'Constraints de usuario' AS verificacion,
    conname AS constraint_name,
    pg_get_constraintdef(oid) AS definition
FROM pg_constraint
WHERE conrelid = 'usuario'::regclass
  AND conname IN (
      'chk_usuario_nombre_no_vacio',
      'chk_usuario_email_no_vacio',
      'chk_usuario_password_hash_no_vacio'
  )
ORDER BY conname;

DO $$
DECLARE
    cantidad_constraints INTEGER;
BEGIN
    SELECT COUNT(*)
    INTO cantidad_constraints
    FROM pg_constraint
    WHERE conrelid = 'usuario'::regclass
      AND conname IN (
          'chk_usuario_nombre_no_vacio',
          'chk_usuario_email_no_vacio',
          'chk_usuario_password_hash_no_vacio'
      );

    IF cantidad_constraints = 3 THEN
        RAISE NOTICE 'OK: existen las tres constraints nuevas de usuario.';
    ELSE
        RAISE WARNING 'FALLO: se esperaban 3 constraints nuevas de usuario y se encontraron %.', cantidad_constraints;
    END IF;
END; $$;

-- Existencia y caracter unico del indice funcional de email normalizado.
SELECT
    'Indice unico de email normalizado' AS verificacion,
    i.relname AS index_name,
    ix.indisunique AS es_unico,
    pg_get_indexdef(ix.indexrelid) AS definition
FROM pg_class i
JOIN pg_index ix ON ix.indexrelid = i.oid
WHERE i.relname = 'idx_usuario_email_normalizado_unico';

DO $$
DECLARE
    indice_unico BOOLEAN;
BEGIN
    SELECT ix.indisunique
    INTO indice_unico
    FROM pg_class i
    JOIN pg_index ix ON ix.indexrelid = i.oid
    WHERE i.relname = 'idx_usuario_email_normalizado_unico';

    IF indice_unico IS TRUE THEN
        RAISE NOTICE 'OK: idx_usuario_email_normalizado_unico existe y es unico.';
    ELSE
        RAISE WARNING 'FALLO: idx_usuario_email_normalizado_unico no existe o no es unico.';
    END IF;
END; $$;

-- ============================================================
-- 2. CONSULTAS INFORMATIVAS DE SOLO LECTURA
-- ------------------------------------------------------------
-- No modifican usuarios existentes.
-- ============================================================

SELECT
    'Usuarios activos' AS metrica,
    COUNT(*) AS cantidad
FROM usuario
WHERE activo = true;

SELECT
    'Usuarios con baja logica' AS metrica,
    COUNT(*) AS cantidad
FROM usuario
WHERE deleted_at IS NOT NULL;

SELECT
    'Usuarios activos cuyo password_hash no parece BCrypt' AS metrica,
    COUNT(*) AS cantidad
FROM usuario
WHERE activo = true
  AND password_hash !~ '^\$2[aby]\$[0-9]{2}\$[./A-Za-z0-9]{53}$';

SELECT
    'Usuarios por rol' AS metrica,
    r.nombre AS rol,
    COUNT(u.id) AS cantidad
FROM rol r
LEFT JOIN usuario u ON u.rol_id = r.id
GROUP BY r.nombre
ORDER BY r.nombre;

-- ============================================================
-- 3. INSERCION TEMPORAL VALIDA
-- ============================================================

DO $$
DECLARE
    rol_usuario_id BIGINT;
    usuario_temporal_id BIGINT;
BEGIN
    SELECT id
    INTO rol_usuario_id
    FROM rol
    WHERE nombre = 'USUARIO';

    INSERT INTO usuario (
        rol_id,
        nombre,
        apellido,
        email,
        password_hash,
        telefono,
        activo,
        email_verificado
    )
    VALUES (
        rol_usuario_id,
        'Usuario Temporal Auth',
        'Prueba',
        'auth.valid.user@example.invalid',
        'placeholder-no-bcrypt-para-pruebas',
        NULL,
        true,
        false
    )
    RETURNING id INTO usuario_temporal_id;

    RAISE NOTICE 'OK: se inserto un usuario temporal valido con id %.', usuario_temporal_id;
END; $$;

SELECT
    'Usuario temporal valido' AS verificacion,
    u.id,
    u.nombre,
    u.email,
    r.nombre AS rol,
    u.activo,
    u.email_verificado
FROM usuario u
JOIN rol r ON r.id = u.rol_id
WHERE u.email = 'auth.valid.user@example.invalid';

-- ============================================================
-- 4. PRUEBAS NEGATIVAS DE USUARIO Y ROL
-- ------------------------------------------------------------
-- Cada bloque captura la violacion esperada.
-- ============================================================

DO $$
DECLARE
    rol_usuario_id BIGINT;
BEGIN
    SELECT id INTO rol_usuario_id FROM rol WHERE nombre = 'USUARIO';

    INSERT INTO usuario (
        rol_id,
        nombre,
        apellido,
        email,
        password_hash,
        telefono,
        activo,
        email_verificado
    )
    VALUES (
        rol_usuario_id,
        '   ',
        'Prueba',
        'auth.empty.name@example.invalid',
        'placeholder-no-bcrypt-para-pruebas',
        NULL,
        true,
        false
    );

    RAISE WARNING 'FALLO: se acepto un usuario con nombre vacio.';
EXCEPTION
    WHEN check_violation THEN
        RAISE NOTICE 'OK: nombre vacio rechazado. %', SQLERRM;
END; $$;

DO $$
DECLARE
    rol_usuario_id BIGINT;
BEGIN
    SELECT id INTO rol_usuario_id FROM rol WHERE nombre = 'USUARIO';

    INSERT INTO usuario (
        rol_id,
        nombre,
        apellido,
        email,
        password_hash,
        telefono,
        activo,
        email_verificado
    )
    VALUES (
        rol_usuario_id,
        'Usuario Email Vacio',
        'Prueba',
        '   ',
        'placeholder-no-bcrypt-para-pruebas',
        NULL,
        true,
        false
    );

    RAISE WARNING 'FALLO: se acepto un usuario con email vacio.';
EXCEPTION
    WHEN check_violation THEN
        RAISE NOTICE 'OK: email vacio rechazado. %', SQLERRM;
END; $$;

DO $$
DECLARE
    rol_usuario_id BIGINT;
BEGIN
    SELECT id INTO rol_usuario_id FROM rol WHERE nombre = 'USUARIO';

    INSERT INTO usuario (
        rol_id,
        nombre,
        apellido,
        email,
        password_hash,
        telefono,
        activo,
        email_verificado
    )
    VALUES (
        rol_usuario_id,
        'Usuario Hash Vacio',
        'Prueba',
        'auth.empty.hash@example.invalid',
        '   ',
        NULL,
        true,
        false
    );

    RAISE WARNING 'FALLO: se acepto un usuario con password_hash vacio.';
EXCEPTION
    WHEN check_violation THEN
        RAISE NOTICE 'OK: password_hash vacio rechazado. %', SQLERRM;
END; $$;

DO $$
BEGIN
    INSERT INTO rol (
        nombre,
        descripcion,
        activo
    )
    VALUES (
        'ROL_INVALIDO_AUTH_TEST',
        'Rol temporal invalido para validar la constraint.',
        true
    );

    RAISE WARNING 'FALLO: se acepto un rol no permitido.';
EXCEPTION
    WHEN check_violation THEN
        RAISE NOTICE 'OK: rol no permitido rechazado. %', SQLERRM;
END; $$;

DO $$
DECLARE
    rol_usuario_id BIGINT;
BEGIN
    SELECT id INTO rol_usuario_id FROM rol WHERE nombre = 'USUARIO';

    INSERT INTO usuario (
        rol_id,
        nombre,
        apellido,
        email,
        password_hash,
        telefono,
        activo,
        email_verificado
    )
    VALUES (
        rol_usuario_id,
        'Usuario Duplicado Case',
        'Prueba',
        'auth.duplicate.case@example.invalid',
        'placeholder-no-bcrypt-para-pruebas',
        NULL,
        true,
        false
    );

    BEGIN
        INSERT INTO usuario (
            rol_id,
            nombre,
            apellido,
            email,
            password_hash,
            telefono,
            activo,
            email_verificado
        )
        VALUES (
            rol_usuario_id,
            'Usuario Duplicado Case Dos',
            'Prueba',
            'AUTH.DUPLICATE.CASE@EXAMPLE.INVALID',
            'placeholder-no-bcrypt-para-pruebas',
            NULL,
            true,
            false
        );

        RAISE WARNING 'FALLO: se acepto un email duplicado con distintas mayusculas.';
    EXCEPTION
        WHEN unique_violation THEN
            RAISE NOTICE 'OK: email duplicado por mayusculas rechazado. %', SQLERRM;
    END;
END; $$;

DO $$
DECLARE
    rol_usuario_id BIGINT;
BEGIN
    SELECT id INTO rol_usuario_id FROM rol WHERE nombre = 'USUARIO';

    INSERT INTO usuario (
        rol_id,
        nombre,
        apellido,
        email,
        password_hash,
        telefono,
        activo,
        email_verificado
    )
    VALUES (
        rol_usuario_id,
        'Usuario Duplicado Espacios',
        'Prueba',
        'auth.duplicate.spaces@example.invalid',
        'placeholder-no-bcrypt-para-pruebas',
        NULL,
        true,
        false
    );

    BEGIN
        INSERT INTO usuario (
            rol_id,
            nombre,
            apellido,
            email,
            password_hash,
            telefono,
            activo,
            email_verificado
        )
        VALUES (
            rol_usuario_id,
            'Usuario Duplicado Espacios Dos',
            'Prueba',
            '  auth.duplicate.spaces@example.invalid  ',
            'placeholder-no-bcrypt-para-pruebas',
            NULL,
            true,
            false
        );

        RAISE WARNING 'FALLO: se acepto un email duplicado con espacios externos.';
    EXCEPTION
        WHEN unique_violation THEN
            RAISE NOTICE 'OK: email duplicado por espacios externos rechazado. %', SQLERRM;
    END;
END; $$;

DO $$
DECLARE
    rol_usuario_id BIGINT;
BEGIN
    SELECT id INTO rol_usuario_id FROM rol WHERE nombre = 'USUARIO';

    INSERT INTO usuario (
        rol_id,
        nombre,
        apellido,
        email,
        password_hash,
        telefono,
        activo,
        email_verificado,
        deleted_at
    )
    VALUES (
        rol_usuario_id,
        'Usuario Baja Logica',
        'Prueba',
        'auth.deleted.unique@example.invalid',
        'placeholder-no-bcrypt-para-pruebas',
        NULL,
        false,
        false,
        CURRENT_TIMESTAMP
    );

    BEGIN
        INSERT INTO usuario (
            rol_id,
            nombre,
            apellido,
            email,
            password_hash,
            telefono,
            activo,
            email_verificado
        )
        VALUES (
            rol_usuario_id,
            'Usuario Reuso Email Baja',
            'Prueba',
            'AUTH.DELETED.UNIQUE@EXAMPLE.INVALID',
            'placeholder-no-bcrypt-para-pruebas',
            NULL,
            true,
            false
        );

        RAISE WARNING 'FALLO: se reutilizo el email de un usuario con deleted_at.';
    EXCEPTION
        WHEN unique_violation THEN
            RAISE NOTICE 'OK: la unicidad contempla usuarios con deleted_at. %', SQLERRM;
    END;
END; $$;

-- ============================================================
-- 5. CONTROL FINAL DE DATOS TEMPORALES
-- ------------------------------------------------------------
-- Estas consultas muestran los usuarios temporales creados dentro
-- de la transaccion. Todo se revierte con ROLLBACK.
-- ============================================================

SELECT
    'Usuarios temporales de auth antes de revertir' AS verificacion,
    COUNT(*) AS cantidad
FROM usuario
WHERE email LIKE '%@example.invalid'
   OR LOWER(BTRIM(email)) LIKE '%@example.invalid';

ROLLBACK;
