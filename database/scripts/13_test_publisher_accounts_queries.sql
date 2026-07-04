BEGIN;

-- ============================================================
-- DondeEntreno - Validacion local de cuentas publicador
-- Archivo: 13_test_publisher_accounts_queries.sql
-- Base de datos: PostgreSQL
-- Descripcion:
-- Este script valida localmente la migracion 12.
--
-- Debe ejecutarse despues de aplicar:
-- 12_prepare_publisher_accounts.sql
--
-- Todo corre dentro de una transaccion y termina con ROLLBACK
-- para no dejar datos temporales persistidos.
-- ============================================================

-- ============================================================
-- 1. VALIDACIONES ESTRUCTURALES
-- ------------------------------------------------------------
-- Verificamos columnas, constraints e indices agregados para
-- cuentas de publicador y asociacion de solicitudes.
-- ============================================================

DO $$
DECLARE
    columnas_usuario INTEGER;
    columnas_perfil INTEGER;
    columnas_solicitud INTEGER;
    constraints_nuevas INTEGER;
    indices_nuevos INTEGER;
BEGIN
    SELECT COUNT(*)
    INTO columnas_usuario
    FROM information_schema.columns
    WHERE table_schema = 'public'
      AND table_name = 'usuario'
      AND column_name IN (
          'telefono_normalizado',
          'telefono_verificado'
      );

    IF columnas_usuario = 2 THEN
        RAISE NOTICE 'OK: existen las columnas nuevas de usuario.';
    ELSE
        RAISE WARNING 'FALLO: se esperaban 2 columnas nuevas de usuario y se encontraron %.', columnas_usuario;
    END IF;

    SELECT COUNT(*)
    INTO columnas_perfil
    FROM information_schema.columns
    WHERE table_schema = 'public'
      AND table_name = 'perfil_publicador'
      AND column_name IN (
          'estado',
          'ciudad_principal_id',
          'whatsapp_normalizado',
          'telefono_contacto_normalizado'
      );

    IF columnas_perfil = 4 THEN
        RAISE NOTICE 'OK: existen las columnas nuevas de perfil_publicador.';
    ELSE
        RAISE WARNING 'FALLO: se esperaban 4 columnas nuevas de perfil_publicador y se encontraron %.', columnas_perfil;
    END IF;

    SELECT COUNT(*)
    INTO columnas_solicitud
    FROM information_schema.columns
    WHERE table_schema = 'public'
      AND table_name = 'solicitud_publicacion'
      AND column_name IN (
          'usuario_id',
          'perfil_publicador_id'
      );

    IF columnas_solicitud = 2 THEN
        RAISE NOTICE 'OK: existen las columnas nuevas de solicitud_publicacion.';
    ELSE
        RAISE WARNING 'FALLO: se esperaban 2 columnas nuevas de solicitud_publicacion y se encontraron %.', columnas_solicitud;
    END IF;

    SELECT COUNT(*)
    INTO constraints_nuevas
    FROM pg_constraint
    WHERE connamespace = 'public'::regnamespace
      AND conname IN (
          'chk_usuario_telefono_normalizado_formato',
          'fk_perfil_publicador_ciudad_principal',
          'chk_perfil_publicador_estado',
          'chk_perfil_publicador_whatsapp_normalizado_formato',
          'chk_perfil_publicador_telefono_contacto_normalizado_formato',
          'fk_solicitud_publicacion_usuario',
          'fk_solicitud_publicacion_perfil_publicador',
          'chk_solicitud_publicacion_perfil_requiere_usuario'
      );

    IF constraints_nuevas = 8 THEN
        RAISE NOTICE 'OK: existen las constraints nuevas esperadas.';
    ELSE
        RAISE WARNING 'FALLO: se esperaban 8 constraints nuevas y se encontraron %.', constraints_nuevas;
    END IF;

    SELECT COUNT(*)
    INTO indices_nuevos
    FROM pg_indexes
    WHERE schemaname = 'public'
      AND indexname IN (
          'idx_usuario_telefono_normalizado',
          'idx_perfil_publicador_ciudad_principal_id',
          'idx_perfil_publicador_whatsapp_normalizado',
          'idx_perfil_publicador_telefono_contacto_normalizado',
          'idx_perfil_publicador_estado',
          'idx_perfil_publicador_usuario_estado',
          'idx_solicitud_publicacion_usuario_id',
          'idx_solicitud_publicacion_perfil_publicador_id',
          'idx_solicitud_publicacion_perfil_estado',
          'idx_solicitud_publicacion_usuario_created_at'
      );

    IF indices_nuevos = 10 THEN
        RAISE NOTICE 'OK: existen los indices nuevos esperados.';
    ELSE
        RAISE WARNING 'FALLO: se esperaban 10 indices nuevos y se encontraron %.', indices_nuevos;
    END IF;
END; $$;

SELECT
    'Columnas nuevas' AS verificacion,
    table_name,
    column_name,
    data_type,
    is_nullable,
    column_default
FROM information_schema.columns
WHERE table_schema = 'public'
  AND (
      (table_name = 'usuario'
       AND column_name IN ('telefono_normalizado', 'telefono_verificado'))
      OR
      (table_name = 'perfil_publicador'
       AND column_name IN ('estado', 'ciudad_principal_id', 'whatsapp_normalizado', 'telefono_contacto_normalizado'))
      OR
      (table_name = 'solicitud_publicacion'
       AND column_name IN ('usuario_id', 'perfil_publicador_id'))
  )
ORDER BY table_name, ordinal_position;

SELECT
    'Constraints nuevas' AS verificacion,
    conrelid::regclass AS tabla,
    conname AS constraint_name,
    contype AS tipo,
    pg_get_constraintdef(oid) AS definition
FROM pg_constraint
WHERE connamespace = 'public'::regnamespace
  AND conname IN (
      'chk_usuario_telefono_normalizado_formato',
      'fk_perfil_publicador_ciudad_principal',
      'chk_perfil_publicador_estado',
      'chk_perfil_publicador_whatsapp_normalizado_formato',
      'chk_perfil_publicador_telefono_contacto_normalizado_formato',
      'fk_solicitud_publicacion_usuario',
      'fk_solicitud_publicacion_perfil_publicador',
      'chk_solicitud_publicacion_perfil_requiere_usuario'
  )
ORDER BY 2, 3;

SELECT
    'Indices nuevos' AS verificacion,
    tablename,
    indexname,
    indexdef
FROM pg_indexes
WHERE schemaname = 'public'
  AND indexname IN (
      'idx_usuario_telefono_normalizado',
      'idx_perfil_publicador_ciudad_principal_id',
      'idx_perfil_publicador_whatsapp_normalizado',
      'idx_perfil_publicador_telefono_contacto_normalizado',
      'idx_perfil_publicador_estado',
      'idx_perfil_publicador_usuario_estado',
      'idx_solicitud_publicacion_usuario_id',
      'idx_solicitud_publicacion_perfil_publicador_id',
      'idx_solicitud_publicacion_perfil_estado',
      'idx_solicitud_publicacion_usuario_created_at'
  )
ORDER BY tablename, indexname;

-- ============================================================
-- 2. FLUJO POSITIVO TEMPORAL
-- ------------------------------------------------------------
-- Creamos usuarios, perfil y solicitudes temporales dentro de
-- la transaccion para validar el recorrido esperado.
-- ============================================================

DO $$
DECLARE
    rol_usuario_id BIGINT;
    rol_publicador_id BIGINT;
    ciudad_temporal_id BIGINT;
    usuario_comun_id BIGINT;
    usuario_publicador_id BIGINT;
    perfil_temporal_id BIGINT;
    solicitud_temporal_id BIGINT;
BEGIN
    SELECT id INTO rol_usuario_id FROM rol WHERE nombre = 'USUARIO';
    SELECT id INTO rol_publicador_id FROM rol WHERE nombre = 'PUBLICADOR';
    SELECT id INTO ciudad_temporal_id FROM ciudad ORDER BY id LIMIT 1;

    IF rol_usuario_id IS NULL THEN
        RAISE EXCEPTION 'Falta el rol USUARIO.';
    END IF;

    IF rol_publicador_id IS NULL THEN
        RAISE EXCEPTION 'Falta el rol PUBLICADOR.';
    END IF;

    IF ciudad_temporal_id IS NULL THEN
        RAISE EXCEPTION 'Falta al menos una ciudad para probar ciudad_principal_id.';
    END IF;

    INSERT INTO usuario (
        rol_id,
        nombre,
        apellido,
        email,
        password_hash,
        telefono,
        telefono_normalizado,
        activo,
        email_verificado,
        telefono_verificado
    )
    VALUES (
        rol_usuario_id,
        'Usuario Comun Temporal Panel',
        'Prueba',
        'publisher.accounts.usuario@example.invalid',
        'placeholder-no-bcrypt-para-pruebas',
        '+54 223 555 0001',
        '542235550001',
        true,
        false,
        false
    )
    RETURNING id INTO usuario_comun_id;

    INSERT INTO usuario (
        rol_id,
        nombre,
        apellido,
        email,
        password_hash,
        telefono,
        telefono_normalizado,
        activo,
        email_verificado,
        telefono_verificado
    )
    VALUES (
        rol_publicador_id,
        'Publicador Temporal Panel',
        'Prueba',
        'publisher.accounts.publicador@example.invalid',
        'placeholder-no-bcrypt-para-pruebas',
        '+54 223 555 0002',
        '542235550002',
        true,
        true,
        false
    )
    RETURNING id INTO usuario_publicador_id;

    INSERT INTO perfil_publicador (
        usuario_id,
        nombre,
        tipo_publicador,
        descripcion,
        email_contacto,
        telefono_contacto,
        telefono_contacto_normalizado,
        whatsapp,
        whatsapp_normalizado,
        estado,
        ciudad_principal_id,
        activo,
        verificado
    )
    VALUES (
        usuario_publicador_id,
        'Perfil Publicador Temporal Panel',
        'PROFESOR_INDEPENDIENTE',
        'Perfil temporal para validar panel publicador.',
        'publisher.accounts.contacto@example.invalid',
        '+54 223 555 0003',
        '542235550003',
        '+54 223 555 0004',
        '542235550004',
        'PENDIENTE_REVISION',
        ciudad_temporal_id,
        true,
        false
    )
    RETURNING id INTO perfil_temporal_id;

    INSERT INTO solicitud_publicacion (
        codigo_seguimiento,
        tipo_publicador,
        nombre_publicador,
        nombre_actividad,
        deporte_otro,
        descripcion,
        nivel,
        enfoque,
        modalidad,
        ciudad_otra,
        barrio_otro,
        nombre_lugar,
        email,
        acepta_condiciones,
        usuario_id,
        perfil_publicador_id
    )
    VALUES (
        'PUB-ACC-TEMP-ASOCIADA',
        'PROFESOR_INDEPENDIENTE',
        'Perfil Publicador Temporal Panel',
        'Actividad Temporal Panel',
        'Entrenamiento temporal',
        'Solicitud temporal asociada a usuario y perfil publicador.',
        'TODOS',
        'RECREATIVO',
        'PRESENCIAL',
        'Ciudad Temporal Panel',
        'Barrio Temporal Panel',
        'Lugar Temporal Panel',
        'publisher.accounts.solicitud@example.invalid',
        true,
        usuario_publicador_id,
        perfil_temporal_id
    )
    RETURNING id INTO solicitud_temporal_id;

    RAISE NOTICE
        'OK: flujo temporal creado. usuario comun %, usuario publicador %, perfil %, solicitud %.',
        usuario_comun_id,
        usuario_publicador_id,
        perfil_temporal_id,
        solicitud_temporal_id;
END; $$;

SELECT
    'Navegacion usuario -> perfil -> solicitud' AS verificacion,
    u.id AS usuario_id,
    u.email,
    p.id AS perfil_publicador_id,
    p.estado AS perfil_estado,
    s.id AS solicitud_id,
    s.codigo_seguimiento,
    s.estado AS solicitud_estado
FROM usuario u
JOIN perfil_publicador p ON p.usuario_id = u.id
JOIN solicitud_publicacion s
    ON s.usuario_id = u.id
   AND s.perfil_publicador_id = p.id
WHERE u.email = 'publisher.accounts.publicador@example.invalid';

INSERT INTO solicitud_publicacion (
    codigo_seguimiento,
    tipo_publicador,
    nombre_publicador,
    nombre_actividad,
    deporte_otro,
    descripcion,
    nivel,
    enfoque,
    modalidad,
    ciudad_otra,
    barrio_otro,
    nombre_lugar,
    email,
    acepta_condiciones,
    usuario_id,
    perfil_publicador_id
)
VALUES (
    'PUB-ACC-TEMP-HISTORICA',
    'CLUB',
    'Solicitud Historica Temporal',
    'Actividad Historica Temporal',
    'Deporte historico temporal',
    'Solicitud temporal sin usuario ni perfil para validar compatibilidad historica.',
    'TODOS',
    'RECREATIVO',
    'PRESENCIAL',
    'Ciudad Historica Temporal',
    'Barrio Historico Temporal',
    'Lugar Historico Temporal',
    'publisher.accounts.historica@example.invalid',
    true,
    NULL,
    NULL
);

SELECT
    'Solicitud historica compatible' AS verificacion,
    id,
    codigo_seguimiento,
    usuario_id,
    perfil_publicador_id
FROM solicitud_publicacion
WHERE codigo_seguimiento = 'PUB-ACC-TEMP-HISTORICA';

-- ============================================================
-- 3. PRUEBAS NEGATIVAS
-- ------------------------------------------------------------
-- Cada bloque captura el error esperado. Si un dato invalido
-- fuera aceptado, se informa con WARNING.
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
        telefono_normalizado,
        activo,
        email_verificado
    )
    VALUES (
        rol_usuario_id,
        'Usuario Telefono Invalido',
        'Prueba',
        'publisher.accounts.invalid.phone@example.invalid',
        'placeholder-no-bcrypt-para-pruebas',
        'abc123',
        true,
        false
    );

    DELETE FROM usuario
    WHERE email = 'publisher.accounts.invalid.phone@example.invalid';

    RAISE WARNING 'FALLO: se acepto usuario.telefono_normalizado con letras.';
EXCEPTION
    WHEN check_violation THEN
        RAISE NOTICE 'OK: usuario.telefono_normalizado con letras fue rechazado. %', SQLERRM;
END; $$;

DO $$
DECLARE
    usuario_publicador_id BIGINT;
BEGIN
    SELECT id
    INTO usuario_publicador_id
    FROM usuario
    WHERE email = 'publisher.accounts.publicador@example.invalid';

    INSERT INTO perfil_publicador (
        usuario_id,
        nombre,
        tipo_publicador,
        whatsapp_normalizado,
        estado
    )
    VALUES (
        usuario_publicador_id,
        'Perfil WhatsApp Invalido',
        'PROFESOR_INDEPENDIENTE',
        'abc123',
        'PENDIENTE_REVISION'
    );

    DELETE FROM perfil_publicador
    WHERE nombre = 'Perfil WhatsApp Invalido'
      AND usuario_id = usuario_publicador_id;

    RAISE WARNING 'FALLO: se acepto perfil_publicador.whatsapp_normalizado con letras.';
EXCEPTION
    WHEN check_violation THEN
        RAISE NOTICE 'OK: perfil_publicador.whatsapp_normalizado con letras fue rechazado. %', SQLERRM;
END; $$;

DO $$
DECLARE
    usuario_publicador_id BIGINT;
BEGIN
    SELECT id
    INTO usuario_publicador_id
    FROM usuario
    WHERE email = 'publisher.accounts.publicador@example.invalid';

    INSERT INTO perfil_publicador (
        usuario_id,
        nombre,
        tipo_publicador,
        telefono_contacto_normalizado,
        estado
    )
    VALUES (
        usuario_publicador_id,
        'Perfil Telefono Contacto Invalido',
        'PROFESOR_INDEPENDIENTE',
        'abc123',
        'PENDIENTE_REVISION'
    );

    DELETE FROM perfil_publicador
    WHERE nombre = 'Perfil Telefono Contacto Invalido'
      AND usuario_id = usuario_publicador_id;

    RAISE WARNING 'FALLO: se acepto perfil_publicador.telefono_contacto_normalizado con letras.';
EXCEPTION
    WHEN check_violation THEN
        RAISE NOTICE 'OK: perfil_publicador.telefono_contacto_normalizado con letras fue rechazado. %', SQLERRM;
END; $$;

DO $$
DECLARE
    usuario_publicador_id BIGINT;
BEGIN
    SELECT id
    INTO usuario_publicador_id
    FROM usuario
    WHERE email = 'publisher.accounts.publicador@example.invalid';

    INSERT INTO perfil_publicador (
        usuario_id,
        nombre,
        tipo_publicador,
        estado
    )
    VALUES (
        usuario_publicador_id,
        'Perfil Estado Invalido',
        'PROFESOR_INDEPENDIENTE',
        'ESTADO_INVALIDO'
    );

    DELETE FROM perfil_publicador
    WHERE nombre = 'Perfil Estado Invalido'
      AND usuario_id = usuario_publicador_id;

    RAISE WARNING 'FALLO: se acepto perfil_publicador.estado invalido.';
EXCEPTION
    WHEN check_violation THEN
        RAISE NOTICE 'OK: perfil_publicador.estado invalido fue rechazado. %', SQLERRM;
END; $$;

DO $$
DECLARE
    perfil_temporal_id BIGINT;
BEGIN
    SELECT id
    INTO perfil_temporal_id
    FROM perfil_publicador
    WHERE nombre = 'Perfil Publicador Temporal Panel'
    ORDER BY id DESC
    LIMIT 1;

    INSERT INTO solicitud_publicacion (
        codigo_seguimiento,
        tipo_publicador,
        nombre_publicador,
        nombre_actividad,
        deporte_otro,
        descripcion,
        nivel,
        enfoque,
        modalidad,
        ciudad_otra,
        barrio_otro,
        nombre_lugar,
        email,
        acepta_condiciones,
        usuario_id,
        perfil_publicador_id
    )
    VALUES (
        'PUB-ACC-NEG-PERFIL-SIN-USUARIO',
        'PROFESOR_INDEPENDIENTE',
        'Perfil sin Usuario',
        'Actividad Perfil Sin Usuario',
        'Deporte temporal',
        'Solicitud invalida con perfil sin usuario.',
        'TODOS',
        'RECREATIVO',
        'PRESENCIAL',
        'Ciudad Temporal',
        'Barrio Temporal',
        'Lugar Temporal',
        'publisher.accounts.neg.perfil.sin.usuario@example.invalid',
        true,
        NULL,
        perfil_temporal_id
    );

    DELETE FROM solicitud_publicacion
    WHERE codigo_seguimiento = 'PUB-ACC-NEG-PERFIL-SIN-USUARIO';

    RAISE WARNING 'FALLO: se acepto perfil_publicador_id con usuario_id NULL.';
EXCEPTION
    WHEN check_violation THEN
        RAISE NOTICE 'OK: perfil_publicador_id con usuario_id NULL fue rechazado. %', SQLERRM;
END; $$;

DO $$
DECLARE
    usuario_inexistente_id BIGINT;
BEGIN
    SELECT COALESCE(MAX(id), 0) + 1000000000
    INTO usuario_inexistente_id
    FROM usuario;

    INSERT INTO solicitud_publicacion (
        codigo_seguimiento,
        tipo_publicador,
        nombre_publicador,
        nombre_actividad,
        deporte_otro,
        descripcion,
        nivel,
        enfoque,
        modalidad,
        ciudad_otra,
        barrio_otro,
        nombre_lugar,
        email,
        acepta_condiciones,
        usuario_id,
        perfil_publicador_id
    )
    VALUES (
        'PUB-ACC-NEG-USUARIO-INEXISTENTE',
        'CLUB',
        'Usuario Inexistente',
        'Actividad Usuario Inexistente',
        'Deporte temporal',
        'Solicitud invalida con usuario inexistente.',
        'TODOS',
        'RECREATIVO',
        'PRESENCIAL',
        'Ciudad Temporal',
        'Barrio Temporal',
        'Lugar Temporal',
        'publisher.accounts.neg.usuario.inexistente@example.invalid',
        true,
        usuario_inexistente_id,
        NULL
    );

    DELETE FROM solicitud_publicacion
    WHERE codigo_seguimiento = 'PUB-ACC-NEG-USUARIO-INEXISTENTE';

    RAISE WARNING 'FALLO: se acepto solicitud_publicacion.usuario_id inexistente.';
EXCEPTION
    WHEN foreign_key_violation THEN
        RAISE NOTICE 'OK: solicitud_publicacion.usuario_id inexistente fue rechazado. %', SQLERRM;
END; $$;

DO $$
DECLARE
    usuario_publicador_id BIGINT;
    perfil_inexistente_id BIGINT;
BEGIN
    SELECT id
    INTO usuario_publicador_id
    FROM usuario
    WHERE email = 'publisher.accounts.publicador@example.invalid';

    SELECT COALESCE(MAX(id), 0) + 1000000000
    INTO perfil_inexistente_id
    FROM perfil_publicador;

    INSERT INTO solicitud_publicacion (
        codigo_seguimiento,
        tipo_publicador,
        nombre_publicador,
        nombre_actividad,
        deporte_otro,
        descripcion,
        nivel,
        enfoque,
        modalidad,
        ciudad_otra,
        barrio_otro,
        nombre_lugar,
        email,
        acepta_condiciones,
        usuario_id,
        perfil_publicador_id
    )
    VALUES (
        'PUB-ACC-NEG-PERFIL-INEXISTENTE',
        'CLUB',
        'Perfil Inexistente',
        'Actividad Perfil Inexistente',
        'Deporte temporal',
        'Solicitud invalida con perfil inexistente.',
        'TODOS',
        'RECREATIVO',
        'PRESENCIAL',
        'Ciudad Temporal',
        'Barrio Temporal',
        'Lugar Temporal',
        'publisher.accounts.neg.perfil.inexistente@example.invalid',
        true,
        usuario_publicador_id,
        perfil_inexistente_id
    );

    DELETE FROM solicitud_publicacion
    WHERE codigo_seguimiento = 'PUB-ACC-NEG-PERFIL-INEXISTENTE';

    RAISE WARNING 'FALLO: se acepto solicitud_publicacion.perfil_publicador_id inexistente.';
EXCEPTION
    WHEN foreign_key_violation THEN
        RAISE NOTICE 'OK: solicitud_publicacion.perfil_publicador_id inexistente fue rechazado. %', SQLERRM;
END; $$;

-- ============================================================
-- 4. CONSULTAS DE DIAGNOSTICO
-- ============================================================

SELECT
    'Usuarios por rol' AS diagnostico,
    r.nombre AS rol,
    COUNT(u.id) AS cantidad
FROM rol r
LEFT JOIN usuario u ON u.rol_id = r.id
GROUP BY r.nombre
ORDER BY r.nombre;

SELECT
    'Usuarios PUBLICADOR activos' AS diagnostico,
    u.id,
    u.nombre,
    u.email,
    u.activo,
    u.email_verificado
FROM usuario u
JOIN rol r ON r.id = u.rol_id
WHERE r.nombre = 'PUBLICADOR'
  AND u.activo = true
  AND u.deleted_at IS NULL
ORDER BY u.created_at DESC, u.id DESC;

SELECT
    'Perfiles por estado' AS diagnostico,
    estado,
    COUNT(*) AS cantidad
FROM perfil_publicador
GROUP BY estado
ORDER BY estado;

SELECT
    'Perfiles sin ciudad principal' AS diagnostico,
    COUNT(*) AS cantidad
FROM perfil_publicador
WHERE ciudad_principal_id IS NULL;

SELECT
    'Solicitudes historicas sin usuario/perfil' AS diagnostico,
    COUNT(*) AS cantidad
FROM solicitud_publicacion
WHERE usuario_id IS NULL
  AND perfil_publicador_id IS NULL;

SELECT
    'Solicitudes asociadas a usuarios' AS diagnostico,
    s.id,
    s.codigo_seguimiento,
    s.estado,
    s.usuario_id,
    u.email
FROM solicitud_publicacion s
JOIN usuario u ON u.id = s.usuario_id
ORDER BY s.created_at DESC, s.id DESC;

SELECT
    'Solicitudes asociadas a perfil publicador' AS diagnostico,
    s.id,
    s.codigo_seguimiento,
    s.estado,
    s.perfil_publicador_id,
    p.nombre AS perfil_publicador
FROM solicitud_publicacion s
JOIN perfil_publicador p ON p.id = s.perfil_publicador_id
ORDER BY s.created_at DESC, s.id DESC;

SELECT
    'Solicitudes por perfil y estado' AS diagnostico,
    p.id AS perfil_publicador_id,
    p.nombre AS perfil_publicador,
    s.estado,
    COUNT(s.id) AS cantidad
FROM perfil_publicador p
LEFT JOIN solicitud_publicacion s ON s.perfil_publicador_id = p.id
GROUP BY p.id, p.nombre, s.estado
ORDER BY p.nombre, s.estado;

SELECT
    'Perfiles con WhatsApp normalizado' AS diagnostico,
    id,
    nombre,
    whatsapp_normalizado
FROM perfil_publicador
WHERE whatsapp_normalizado IS NOT NULL
ORDER BY id;

SELECT
    'Usuarios con telefono normalizado' AS diagnostico,
    id,
    nombre,
    email,
    telefono_normalizado,
    telefono_verificado
FROM usuario
WHERE telefono_normalizado IS NOT NULL
ORDER BY id;

-- ============================================================
-- 5. CONTROL FINAL DE DATOS TEMPORALES
-- ------------------------------------------------------------
-- Estas consultas muestran los datos temporales antes del
-- ROLLBACK. Nada queda persistido.
-- ============================================================

SELECT
    'Usuarios temporales publisher accounts' AS verificacion,
    COUNT(*) AS cantidad
FROM usuario
WHERE email LIKE 'publisher.accounts.%@example.invalid';

SELECT
    'Perfiles temporales publisher accounts' AS verificacion,
    COUNT(*) AS cantidad
FROM perfil_publicador
WHERE nombre LIKE '%Temporal Panel%';

SELECT
    'Solicitudes temporales publisher accounts' AS verificacion,
    COUNT(*) AS cantidad
FROM solicitud_publicacion
WHERE codigo_seguimiento LIKE 'PUB-ACC-%';

ROLLBACK;
