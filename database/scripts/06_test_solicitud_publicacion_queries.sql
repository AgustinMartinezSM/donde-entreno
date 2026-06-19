-- ============================================================
-- DondeEntreno - Validacion de solicitudes publicas
-- Archivo: 06_test_solicitud_publicacion_queries.sql
-- Base de datos: PostgreSQL
-- Descripcion:
-- Este script valida localmente la migracion 05.
--
-- Debe ejecutarse solo despues de aplicar correctamente:
-- 05_create_solicitud_publicacion.sql
--
-- Todo el script corre dentro de una transaccion y termina con
-- ROLLBACK para no dejar datos persistentes.
-- ============================================================

BEGIN;

-- ============================================================
-- 1. VERIFICACION ESTRUCTURAL BASICA
-- ------------------------------------------------------------
-- Validamos que existan las tablas nuevas antes de ejecutar
-- inserciones temporales.
-- ============================================================

DO $$
BEGIN
    IF to_regclass('public.solicitud_publicacion') IS NULL THEN
        RAISE EXCEPTION 'Falta la tabla public.solicitud_publicacion. Ejecutar primero la migracion 05.';
    END IF;

    IF to_regclass('public.solicitud_publicacion_horario') IS NULL THEN
        RAISE EXCEPTION 'Falta la tabla public.solicitud_publicacion_horario. Ejecutar primero la migracion 05.';
    END IF;
END; $$;

-- Existencia de las tablas principales.
SELECT
    table_name,
    table_type
FROM information_schema.tables
WHERE table_schema = 'public'
  AND table_name IN (
      'solicitud_publicacion',
      'solicitud_publicacion_horario'
  )
ORDER BY table_name;

-- Columnas principales creadas por la migracion.
SELECT
    table_name,
    column_name,
    data_type,
    is_nullable,
    column_default
FROM information_schema.columns
WHERE table_schema = 'public'
  AND table_name IN (
      'solicitud_publicacion',
      'solicitud_publicacion_horario'
  )
ORDER BY table_name, ordinal_position;

-- Claves foraneas creadas por la migracion.
SELECT
    conname AS constraint_name,
    conrelid::regclass AS table_name,
    confrelid::regclass AS referenced_table,
    pg_get_constraintdef(oid) AS definition
FROM pg_constraint
WHERE connamespace = 'public'::regnamespace
  AND conrelid IN (
      'solicitud_publicacion'::regclass,
      'solicitud_publicacion_horario'::regclass
  )
  AND contype = 'f'
ORDER BY conname;

-- Restricciones CHECK creadas por la migracion.
SELECT
    conname AS constraint_name,
    conrelid::regclass AS table_name,
    pg_get_constraintdef(oid) AS definition
FROM pg_constraint
WHERE connamespace = 'public'::regnamespace
  AND conrelid IN (
      'solicitud_publicacion'::regclass,
      'solicitud_publicacion_horario'::regclass
  )
  AND contype = 'c'
ORDER BY conname;

-- Restricciones UNIQUE creadas por la migracion.
SELECT
    conname AS constraint_name,
    conrelid::regclass AS table_name,
    pg_get_constraintdef(oid) AS definition
FROM pg_constraint
WHERE connamespace = 'public'::regnamespace
  AND conrelid IN (
      'solicitud_publicacion'::regclass,
      'solicitud_publicacion_horario'::regclass
  )
  AND contype = 'u'
ORDER BY conname;

-- Indices creados por la migracion.
SELECT
    tablename,
    indexname,
    indexdef
FROM pg_indexes
WHERE schemaname = 'public'
  AND tablename IN (
      'solicitud_publicacion',
      'solicitud_publicacion_horario'
  )
ORDER BY tablename, indexname;

-- Validacion estricta de columnas esperadas.
DO $$
DECLARE
    columna_esperada RECORD;
BEGIN
    FOR columna_esperada IN
        SELECT *
        FROM (
            VALUES
                ('solicitud_publicacion', 'codigo_seguimiento'),
                ('solicitud_publicacion', 'origen'),
                ('solicitud_publicacion', 'estado'),
                ('solicitud_publicacion', 'tipo_publicador'),
                ('solicitud_publicacion', 'nombre_publicador'),
                ('solicitud_publicacion', 'nombre_actividad'),
                ('solicitud_publicacion', 'deporte_id'),
                ('solicitud_publicacion', 'deporte_otro'),
                ('solicitud_publicacion', 'ciudad_id'),
                ('solicitud_publicacion', 'ciudad_otra'),
                ('solicitud_publicacion', 'barrio_id'),
                ('solicitud_publicacion', 'barrio_otro'),
                ('solicitud_publicacion', 'whatsapp_normalizado'),
                ('solicitud_publicacion', 'revisado_por_usuario_id'),
                ('solicitud_publicacion', 'actividad_generada_id'),
                ('solicitud_publicacion', 'revision_iniciada_at'),
                ('solicitud_publicacion', 'revision_finalizada_at'),
                ('solicitud_publicacion', 'deleted_at'),
                ('solicitud_publicacion_horario', 'solicitud_publicacion_id'),
                ('solicitud_publicacion_horario', 'dia_semana'),
                ('solicitud_publicacion_horario', 'hora_inicio'),
                ('solicitud_publicacion_horario', 'hora_fin')
        ) AS columnas(table_name, column_name)
    LOOP
        IF NOT EXISTS (
            SELECT 1
            FROM information_schema.columns c
            WHERE c.table_schema = 'public'
              AND c.table_name = columna_esperada.table_name
              AND c.column_name = columna_esperada.column_name
        ) THEN
            RAISE EXCEPTION
                'Falta la columna %.% requerida para estas pruebas.',
                columna_esperada.table_name,
                columna_esperada.column_name;
        END IF;
    END LOOP;
END; $$;

-- ============================================================
-- 2. VALIDACION DE DATOS BASE DE LOS SEEDS
-- ------------------------------------------------------------
-- No usamos IDs numericos fijos. Resolvemos referencias por
-- datos estables cargados por los scripts de seed.
-- ============================================================

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM deporte WHERE slug = 'boxeo') THEN
        RAISE EXCEPTION 'Falta el deporte con slug boxeo.';
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM ciudad
        WHERE nombre = 'Mar del Plata'
          AND provincia = 'Buenos Aires'
          AND pais = 'Argentina'
    ) THEN
        RAISE EXCEPTION 'Falta la ciudad Mar del Plata, Buenos Aires, Argentina.';
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM barrio b
        JOIN ciudad c ON c.id = b.ciudad_id
        WHERE b.nombre = 'Centro'
          AND c.nombre = 'Mar del Plata'
          AND c.provincia = 'Buenos Aires'
          AND c.pais = 'Argentina'
    ) THEN
        RAISE EXCEPTION 'Falta el barrio Centro perteneciente a Mar del Plata.';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM usuario WHERE email = 'admin@dondeentreno.com') THEN
        RAISE EXCEPTION 'Falta el usuario administrador admin@dondeentreno.com.';
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM actividad
        WHERE slug = 'boxeo-recreativo-adultos-principiantes'
    ) THEN
        RAISE EXCEPTION 'Falta la actividad boxeo-recreativo-adultos-principiantes.';
    END IF;
END; $$;

-- Guardamos IDs resueltos en una tabla temporal de apoyo.
CREATE TEMP TABLE tmp_solicitud_publicacion_contexto (
    deporte_id BIGINT NOT NULL,
    ciudad_id BIGINT NOT NULL,
    barrio_id BIGINT NOT NULL,
    usuario_admin_id BIGINT NOT NULL,
    actividad_id BIGINT NOT NULL
);

INSERT INTO tmp_solicitud_publicacion_contexto (
    deporte_id,
    ciudad_id,
    barrio_id,
    usuario_admin_id,
    actividad_id
)
SELECT
    d.id,
    c.id,
    b.id,
    u.id,
    a.id
FROM deporte d
CROSS JOIN ciudad c
JOIN barrio b
    ON b.ciudad_id = c.id
   AND b.nombre = 'Centro'
CROSS JOIN usuario u
CROSS JOIN actividad a
WHERE d.slug = 'boxeo'
  AND c.nombre = 'Mar del Plata'
  AND c.provincia = 'Buenos Aires'
  AND c.pais = 'Argentina'
  AND u.email = 'admin@dondeentreno.com'
  AND a.slug = 'boxeo-recreativo-adultos-principiantes';

-- Mostramos las referencias resueltas sin exponer ni depender de IDs fijos.
SELECT
    'Referencias base para pruebas' AS verificacion,
    d.slug AS deporte_slug,
    c.nombre AS ciudad,
    b.nombre AS barrio,
    u.email AS usuario_revisor,
    a.slug AS actividad_slug
FROM tmp_solicitud_publicacion_contexto ctx
JOIN deporte d ON d.id = ctx.deporte_id
JOIN ciudad c ON c.id = ctx.ciudad_id
JOIN barrio b ON b.id = ctx.barrio_id
JOIN usuario u ON u.id = ctx.usuario_admin_id
JOIN actividad a ON a.id = ctx.actividad_id;

-- Tabla temporal para reutilizar IDs generados por estas pruebas.
CREATE TEMP TABLE tmp_solicitud_publicacion_test_ids (
    nombre VARCHAR(80) PRIMARY KEY,
    solicitud_publicacion_id BIGINT NOT NULL
);

-- Funcion temporal para crear solicitudes de prueba sin repetir todo el INSERT.
-- La funcion vive en pg_temp y se descarta al cerrar la sesion o al revertir.
CREATE FUNCTION pg_temp.insertar_solicitud_publicacion_test(
    p_codigo_seguimiento VARCHAR,
    p_origen VARCHAR DEFAULT 'FORMULARIO_WEB',
    p_estado VARCHAR DEFAULT 'PENDIENTE',
    p_tipo_publicador VARCHAR DEFAULT 'GIMNASIO',
    p_nombre_publicador VARCHAR DEFAULT 'Gimnasio Temporal de Pruebas',
    p_nombre_actividad VARCHAR DEFAULT 'Boxeo temporal para pruebas',
    p_usar_deporte_existente BOOLEAN DEFAULT true,
    p_deporte_otro VARCHAR DEFAULT NULL,
    p_descripcion TEXT DEFAULT 'Solicitud temporal creada para validar la migracion 05.',
    p_nivel VARCHAR DEFAULT 'PRINCIPIANTE',
    p_enfoque VARCHAR DEFAULT 'RECREATIVO',
    p_modalidad VARCHAR DEFAULT 'PRESENCIAL',
    p_edad_minima INTEGER DEFAULT 18,
    p_edad_maxima INTEGER DEFAULT 60,
    p_precio_referencia NUMERIC DEFAULT 15000.00,
    p_mostrar_precio BOOLEAN DEFAULT true,
    p_usar_ciudad_existente BOOLEAN DEFAULT true,
    p_ciudad_otra VARCHAR DEFAULT NULL,
    p_usar_barrio_existente BOOLEAN DEFAULT true,
    p_barrio_otro VARCHAR DEFAULT NULL,
    p_nombre_lugar VARCHAR DEFAULT 'Sede temporal de pruebas',
    p_direccion VARCHAR DEFAULT 'San Martin 1000',
    p_referencia_ubicacion VARCHAR DEFAULT 'Ingreso principal',
    p_whatsapp VARCHAR DEFAULT '+54 9 223 555 0101',
    p_whatsapp_normalizado VARCHAR DEFAULT '5492235550101',
    p_instagram VARCHAR DEFAULT '@solicitud_test',
    p_email VARCHAR DEFAULT 'solicitud.test@dondeentreno.com',
    p_observaciones_solicitante TEXT DEFAULT 'Registro temporal para pruebas.',
    p_acepta_condiciones BOOLEAN DEFAULT true,
    p_revisado_por_usuario_id BIGINT DEFAULT NULL,
    p_actividad_generada_id BIGINT DEFAULT NULL,
    p_motivo_rechazo TEXT DEFAULT NULL,
    p_observaciones_revision TEXT DEFAULT NULL,
    p_revision_iniciada_at TIMESTAMPTZ DEFAULT NULL,
    p_revision_finalizada_at TIMESTAMPTZ DEFAULT NULL
)
RETURNS BIGINT
LANGUAGE plpgsql
AS $$
DECLARE
    contexto RECORD;
    solicitud_id BIGINT;
BEGIN
    SELECT *
    INTO contexto
    FROM tmp_solicitud_publicacion_contexto
    LIMIT 1;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'No hay contexto temporal para insertar solicitudes de prueba.';
    END IF;

    INSERT INTO solicitud_publicacion (
        codigo_seguimiento,
        origen,
        estado,
        tipo_publicador,
        nombre_publicador,
        nombre_actividad,
        deporte_id,
        deporte_otro,
        descripcion,
        nivel,
        enfoque,
        modalidad,
        edad_minima,
        edad_maxima,
        precio_referencia,
        mostrar_precio,
        ciudad_id,
        ciudad_otra,
        barrio_id,
        barrio_otro,
        nombre_lugar,
        direccion,
        referencia_ubicacion,
        whatsapp,
        whatsapp_normalizado,
        instagram,
        email,
        observaciones_solicitante,
        acepta_condiciones,
        revisado_por_usuario_id,
        actividad_generada_id,
        motivo_rechazo,
        observaciones_revision,
        revision_iniciada_at,
        revision_finalizada_at,
        ip_origen
    )
    VALUES (
        p_codigo_seguimiento,
        p_origen,
        p_estado,
        p_tipo_publicador,
        p_nombre_publicador,
        p_nombre_actividad,
        CASE WHEN p_usar_deporte_existente THEN contexto.deporte_id ELSE NULL END,
        p_deporte_otro,
        p_descripcion,
        p_nivel,
        p_enfoque,
        p_modalidad,
        p_edad_minima,
        p_edad_maxima,
        p_precio_referencia,
        p_mostrar_precio,
        CASE WHEN p_usar_ciudad_existente THEN contexto.ciudad_id ELSE NULL END,
        p_ciudad_otra,
        CASE WHEN p_usar_barrio_existente THEN contexto.barrio_id ELSE NULL END,
        p_barrio_otro,
        p_nombre_lugar,
        p_direccion,
        p_referencia_ubicacion,
        p_whatsapp,
        p_whatsapp_normalizado,
        p_instagram,
        p_email,
        p_observaciones_solicitante,
        p_acepta_condiciones,
        p_revisado_por_usuario_id,
        p_actividad_generada_id,
        p_motivo_rechazo,
        p_observaciones_revision,
        p_revision_iniciada_at,
        p_revision_finalizada_at,
        '127.0.0.1'::inet
    )
    RETURNING id INTO solicitud_id;

    RETURN solicitud_id;
END; $$;

-- ============================================================
-- 3. PRUEBA VALIDA PRINCIPAL
-- ------------------------------------------------------------
-- Insertamos una solicitud valida y dos horarios validos.
-- Todos estos datos son temporales y se revierten al finalizar.
-- ============================================================

INSERT INTO tmp_solicitud_publicacion_test_ids (
    nombre,
    solicitud_publicacion_id
)
SELECT
    'principal',
    pg_temp.insertar_solicitud_publicacion_test('TEST-SP-VALIDA-001');

INSERT INTO solicitud_publicacion_horario (
    solicitud_publicacion_id,
    dia_semana,
    hora_inicio,
    hora_fin,
    observacion
)
SELECT
    solicitud_publicacion_id,
    'LUNES',
    TIME '18:00',
    TIME '19:00',
    'Horario temporal de prueba'
FROM tmp_solicitud_publicacion_test_ids
WHERE nombre = 'principal';

INSERT INTO solicitud_publicacion_horario (
    solicitud_publicacion_id,
    dia_semana,
    hora_inicio,
    hora_fin,
    observacion
)
SELECT
    solicitud_publicacion_id,
    'MIERCOLES',
    TIME '18:00',
    TIME '19:00',
    'Segundo horario temporal de prueba'
FROM tmp_solicitud_publicacion_test_ids
WHERE nombre = 'principal';

-- Solicitud completa insertada.
SELECT
    'Solicitud temporal completa' AS verificacion,
    sp.*
FROM solicitud_publicacion sp
JOIN tmp_solicitud_publicacion_test_ids t
    ON t.solicitud_publicacion_id = sp.id
WHERE t.nombre = 'principal';

-- Relaciones con deporte, ciudad y barrio.
SELECT
    'Relaciones de la solicitud' AS verificacion,
    sp.codigo_seguimiento,
    d.slug AS deporte_slug,
    c.nombre AS ciudad,
    b.nombre AS barrio
FROM solicitud_publicacion sp
JOIN deporte d ON d.id = sp.deporte_id
JOIN ciudad c ON c.id = sp.ciudad_id
JOIN barrio b ON b.id = sp.barrio_id
JOIN tmp_solicitud_publicacion_test_ids t
    ON t.solicitud_publicacion_id = sp.id
WHERE t.nombre = 'principal';

-- Horarios asociados, ordenados por dia y hora.
SELECT
    'Horarios de la solicitud' AS verificacion,
    sp.codigo_seguimiento,
    h.dia_semana,
    h.hora_inicio,
    h.hora_fin,
    h.observacion
FROM solicitud_publicacion sp
JOIN solicitud_publicacion_horario h
    ON h.solicitud_publicacion_id = sp.id
JOIN tmp_solicitud_publicacion_test_ids t
    ON t.solicitud_publicacion_id = sp.id
WHERE t.nombre = 'principal'
ORDER BY
    CASE h.dia_semana
        WHEN 'LUNES' THEN 1
        WHEN 'MARTES' THEN 2
        WHEN 'MIERCOLES' THEN 3
        WHEN 'JUEVES' THEN 4
        WHEN 'VIERNES' THEN 5
        WHEN 'SABADO' THEN 6
        WHEN 'DOMINGO' THEN 7
    END,
    h.hora_inicio;

-- Datos de contacto y estado inicial.
SELECT
    'Contacto y estado inicial' AS verificacion,
    codigo_seguimiento,
    whatsapp,
    whatsapp_normalizado,
    email,
    estado
FROM solicitud_publicacion sp
JOIN tmp_solicitud_publicacion_test_ids t
    ON t.solicitud_publicacion_id = sp.id
WHERE t.nombre = 'principal';

-- Cantidades temporales insertadas hasta este punto.
SELECT
    'Cantidades temporales iniciales' AS verificacion,
    COUNT(DISTINCT sp.id) AS solicitudes_temporales,
    COUNT(h.id) AS horarios_temporales
FROM solicitud_publicacion sp
LEFT JOIN solicitud_publicacion_horario h
    ON h.solicitud_publicacion_id = sp.id
WHERE sp.codigo_seguimiento LIKE 'TEST-SP-%';

-- ============================================================
-- 4. PRUEBA DEL FLUJO ADMINISTRATIVO
-- ------------------------------------------------------------
-- Validamos cambios controlados de estado sobre la solicitud
-- temporal principal.
-- ============================================================

UPDATE solicitud_publicacion sp
SET
    estado = 'EN_REVISION',
    revisado_por_usuario_id = ctx.usuario_admin_id,
    revision_iniciada_at = CURRENT_TIMESTAMP,
    updated_at = CURRENT_TIMESTAMP
FROM tmp_solicitud_publicacion_test_ids t
CROSS JOIN tmp_solicitud_publicacion_contexto ctx
WHERE t.nombre = 'principal'
  AND sp.id = t.solicitud_publicacion_id;

UPDATE solicitud_publicacion sp
SET
    estado = 'RECHAZADA',
    motivo_rechazo = 'Prueba controlada de rechazo dentro del script de validacion.',
    revision_finalizada_at = sp.revision_iniciada_at + INTERVAL '1 hour',
    updated_at = CURRENT_TIMESTAMP
FROM tmp_solicitud_publicacion_test_ids t
WHERE t.nombre = 'principal'
  AND sp.id = t.solicitud_publicacion_id;

SELECT
    'Resultado del flujo administrativo' AS verificacion,
    sp.codigo_seguimiento,
    sp.estado,
    u.email AS revisor,
    sp.revision_iniciada_at,
    sp.revision_finalizada_at,
    sp.motivo_rechazo
FROM solicitud_publicacion sp
LEFT JOIN usuario u ON u.id = sp.revisado_por_usuario_id
JOIN tmp_solicitud_publicacion_test_ids t
    ON t.solicitud_publicacion_id = sp.id
WHERE t.nombre = 'principal';

-- ============================================================
-- 5. PRUEBAS NEGATIVAS
-- ------------------------------------------------------------
-- Cada bloque captura el error esperado para que el script no
-- aborte. Si una operacion invalida se acepta, se emite WARNING.
-- ============================================================

DO $$
BEGIN
    PERFORM pg_temp.insertar_solicitud_publicacion_test(
        'TEST-SP-NEG-001',
        p_deporte_otro => 'Deporte duplicado'
    );
    RAISE WARNING 'FALLO: se acepto deporte_id y deporte_otro al mismo tiempo.';
EXCEPTION
    WHEN check_violation THEN
        RAISE NOTICE 'OK: deporte_id y deporte_otro simultaneos fueron rechazados. %', SQLERRM;
END; $$;

DO $$
BEGIN
    PERFORM pg_temp.insertar_solicitud_publicacion_test(
        'TEST-SP-NEG-002',
        p_usar_deporte_existente => false,
        p_deporte_otro => NULL
    );
    RAISE WARNING 'FALLO: se acepto solicitud sin deporte_id ni deporte_otro.';
EXCEPTION
    WHEN check_violation THEN
        RAISE NOTICE 'OK: solicitud sin deporte fue rechazada. %', SQLERRM;
END; $$;

DO $$
BEGIN
    PERFORM pg_temp.insertar_solicitud_publicacion_test(
        'TEST-SP-NEG-003',
        p_ciudad_otra => 'Ciudad duplicada'
    );
    RAISE WARNING 'FALLO: se acepto ciudad_id y ciudad_otra al mismo tiempo.';
EXCEPTION
    WHEN check_violation THEN
        RAISE NOTICE 'OK: ciudad_id y ciudad_otra simultaneas fueron rechazadas. %', SQLERRM;
END; $$;

DO $$
BEGIN
    PERFORM pg_temp.insertar_solicitud_publicacion_test(
        'TEST-SP-NEG-004',
        p_usar_ciudad_existente => false,
        p_ciudad_otra => 'Otra ciudad',
        p_usar_barrio_existente => true
    );
    RAISE WARNING 'FALLO: se acepto barrio_id existente con ciudad_otra.';
EXCEPTION
    WHEN check_violation THEN
        RAISE NOTICE 'OK: barrio_id existente con ciudad_otra fue rechazado. %', SQLERRM;
END; $$;

DO $$
BEGIN
    PERFORM pg_temp.insertar_solicitud_publicacion_test(
        'TEST-SP-NEG-005',
        p_nombre_lugar => NULL,
        p_direccion => NULL
    );
    RAISE WARNING 'FALLO: se acepto solicitud sin nombre_lugar ni direccion.';
EXCEPTION
    WHEN check_violation THEN
        RAISE NOTICE 'OK: solicitud sin lugar ni direccion fue rechazada. %', SQLERRM;
END; $$;

DO $$
BEGIN
    PERFORM pg_temp.insertar_solicitud_publicacion_test(
        'TEST-SP-NEG-006',
        p_whatsapp => NULL,
        p_whatsapp_normalizado => NULL,
        p_email => NULL
    );
    RAISE WARNING 'FALLO: se acepto solicitud sin WhatsApp ni email.';
EXCEPTION
    WHEN check_violation THEN
        RAISE NOTICE 'OK: solicitud sin contacto minimo fue rechazada. %', SQLERRM;
END; $$;

DO $$
BEGIN
    PERFORM pg_temp.insertar_solicitud_publicacion_test(
        'TEST-SP-NEG-007',
        p_whatsapp => '+54 9 223 555 0199',
        p_whatsapp_normalizado => NULL
    );
    RAISE WARNING 'FALLO: se acepto WhatsApp sin whatsapp_normalizado.';
EXCEPTION
    WHEN check_violation THEN
        RAISE NOTICE 'OK: WhatsApp sin normalizar fue rechazado. %', SQLERRM;
END; $$;

DO $$
BEGIN
    PERFORM pg_temp.insertar_solicitud_publicacion_test(
        'TEST-SP-NEG-008',
        p_whatsapp_normalizado => '549223ABC'
    );
    RAISE WARNING 'FALLO: se acepto whatsapp_normalizado con caracteres no numericos.';
EXCEPTION
    WHEN check_violation THEN
        RAISE NOTICE 'OK: whatsapp_normalizado no numerico fue rechazado. %', SQLERRM;
END; $$;

DO $$
BEGIN
    PERFORM pg_temp.insertar_solicitud_publicacion_test(
        'TEST-SP-NEG-009',
        p_edad_minima => -1
    );
    RAISE WARNING 'FALLO: se acepto edad_minima negativa.';
EXCEPTION
    WHEN check_violation THEN
        RAISE NOTICE 'OK: edad_minima negativa fue rechazada. %', SQLERRM;
END; $$;

DO $$
BEGIN
    PERFORM pg_temp.insertar_solicitud_publicacion_test(
        'TEST-SP-NEG-010',
        p_edad_minima => 60,
        p_edad_maxima => 18
    );
    RAISE WARNING 'FALLO: se acepto edad_minima mayor que edad_maxima.';
EXCEPTION
    WHEN check_violation THEN
        RAISE NOTICE 'OK: rango de edad invalido fue rechazado. %', SQLERRM;
END; $$;

DO $$
BEGIN
    PERFORM pg_temp.insertar_solicitud_publicacion_test(
        'TEST-SP-NEG-011',
        p_precio_referencia => -100.00
    );
    RAISE WARNING 'FALLO: se acepto precio negativo.';
EXCEPTION
    WHEN check_violation THEN
        RAISE NOTICE 'OK: precio negativo fue rechazado. %', SQLERRM;
END; $$;

DO $$
BEGIN
    PERFORM pg_temp.insertar_solicitud_publicacion_test(
        'TEST-SP-NEG-012',
        p_precio_referencia => NULL,
        p_mostrar_precio => true
    );
    RAISE WARNING 'FALLO: se acepto mostrar_precio true sin precio.';
EXCEPTION
    WHEN check_violation THEN
        RAISE NOTICE 'OK: mostrar_precio sin precio fue rechazado. %', SQLERRM;
END; $$;

DO $$
BEGIN
    PERFORM pg_temp.insertar_solicitud_publicacion_test(
        'TEST-SP-NEG-013',
        p_acepta_condiciones => false
    );
    RAISE WARNING 'FALLO: se acepto acepta_condiciones false.';
EXCEPTION
    WHEN check_violation THEN
        RAISE NOTICE 'OK: acepta_condiciones false fue rechazado. %', SQLERRM;
END; $$;

DO $$
BEGIN
    PERFORM pg_temp.insertar_solicitud_publicacion_test(
        'TEST-SP-NEG-014',
        p_estado => 'RECHAZADA',
        p_motivo_rechazo => NULL
    );
    RAISE WARNING 'FALLO: se acepto estado RECHAZADA sin motivo.';
EXCEPTION
    WHEN check_violation THEN
        RAISE NOTICE 'OK: RECHAZADA sin motivo fue rechazada. %', SQLERRM;
END; $$;

DO $$
BEGIN
    PERFORM pg_temp.insertar_solicitud_publicacion_test(
        'TEST-SP-NEG-015',
        p_estado => 'PENDIENTE',
        p_motivo_rechazo => 'Motivo que no corresponde'
    );
    RAISE WARNING 'FALLO: se acepto motivo_rechazo con estado no rechazado.';
EXCEPTION
    WHEN check_violation THEN
        RAISE NOTICE 'OK: motivo_rechazo fuera de RECHAZADA fue rechazado. %', SQLERRM;
END; $$;

DO $$
BEGIN
    PERFORM pg_temp.insertar_solicitud_publicacion_test(
        'TEST-SP-NEG-016',
        p_revision_iniciada_at => CURRENT_TIMESTAMP,
        p_revision_finalizada_at => CURRENT_TIMESTAMP - INTERVAL '1 hour'
    );
    RAISE WARNING 'FALLO: se acepto revision_finalizada_at anterior a revision_iniciada_at.';
EXCEPTION
    WHEN check_violation THEN
        RAISE NOTICE 'OK: fecha de finalizacion anterior al inicio fue rechazada. %', SQLERRM;
END; $$;

DO $$
DECLARE
    solicitud_id BIGINT;
BEGIN
    SELECT solicitud_publicacion_id
    INTO solicitud_id
    FROM tmp_solicitud_publicacion_test_ids
    WHERE nombre = 'principal';

    INSERT INTO solicitud_publicacion_horario (
        solicitud_publicacion_id,
        dia_semana,
        hora_inicio,
        hora_fin
    )
    VALUES (
        solicitud_id,
        'JUEVES',
        TIME '20:00',
        TIME '20:00'
    );

    RAISE WARNING 'FALLO: se acepto horario con hora_inicio >= hora_fin.';
EXCEPTION
    WHEN check_violation THEN
        RAISE NOTICE 'OK: horario con rango invalido fue rechazado. %', SQLERRM;
END; $$;

DO $$
DECLARE
    solicitud_id BIGINT;
BEGIN
    SELECT solicitud_publicacion_id
    INTO solicitud_id
    FROM tmp_solicitud_publicacion_test_ids
    WHERE nombre = 'principal';

    INSERT INTO solicitud_publicacion_horario (
        solicitud_publicacion_id,
        dia_semana,
        hora_inicio,
        hora_fin
    )
    VALUES (
        solicitud_id,
        'FERIADO',
        TIME '10:00',
        TIME '11:00'
    );

    RAISE WARNING 'FALLO: se acepto dia_semana invalido.';
EXCEPTION
    WHEN check_violation THEN
        RAISE NOTICE 'OK: dia_semana invalido fue rechazado. %', SQLERRM;
END; $$;

DO $$
DECLARE
    solicitud_id BIGINT;
BEGIN
    SELECT solicitud_publicacion_id
    INTO solicitud_id
    FROM tmp_solicitud_publicacion_test_ids
    WHERE nombre = 'principal';

    INSERT INTO solicitud_publicacion_horario (
        solicitud_publicacion_id,
        dia_semana,
        hora_inicio,
        hora_fin,
        observacion
    )
    VALUES (
        solicitud_id,
        'LUNES',
        TIME '18:00',
        TIME '19:00',
        'Horario duplicado'
    );

    RAISE WARNING 'FALLO: se acepto horario exacto duplicado.';
EXCEPTION
    WHEN unique_violation THEN
        RAISE NOTICE 'OK: horario duplicado fue rechazado. %', SQLERRM;
END; $$;

DO $$
BEGIN
    PERFORM pg_temp.insertar_solicitud_publicacion_test('TEST-SP-VALIDA-001');
    RAISE WARNING 'FALLO: se acepto codigo_seguimiento duplicado.';
EXCEPTION
    WHEN unique_violation THEN
        RAISE NOTICE 'OK: codigo_seguimiento duplicado fue rechazado. %', SQLERRM;
END; $$;

DO $$
DECLARE
    actividad_existente_id BIGINT;
BEGIN
    SELECT actividad_id
    INTO actividad_existente_id
    FROM tmp_solicitud_publicacion_contexto
    LIMIT 1;

    PERFORM pg_temp.insertar_solicitud_publicacion_test(
        'TEST-SP-ACT-001',
        p_estado => 'APROBADA',
        p_actividad_generada_id => actividad_existente_id
    );

    BEGIN
        PERFORM pg_temp.insertar_solicitud_publicacion_test(
            'TEST-SP-ACT-002',
            p_estado => 'APROBADA',
            p_actividad_generada_id => actividad_existente_id
        );

        RAISE WARNING 'FALLO: se acepto la misma actividad_generada_id en dos solicitudes.';
    EXCEPTION
        WHEN unique_violation THEN
            RAISE NOTICE 'OK: actividad_generada_id duplicada fue rechazada. %', SQLERRM;
    END;
END; $$;

-- ============================================================
-- 6. PRUEBA DE CASCADA
-- ------------------------------------------------------------
-- Creamos una solicitud adicional con un horario. Luego eliminamos
-- fisicamente solo esa solicitud temporal y comprobamos que sus
-- horarios se eliminen por ON DELETE CASCADE.
-- ============================================================

INSERT INTO tmp_solicitud_publicacion_test_ids (
    nombre,
    solicitud_publicacion_id
)
SELECT
    'cascade',
    pg_temp.insertar_solicitud_publicacion_test('TEST-SP-CASCADE-001');

INSERT INTO solicitud_publicacion_horario (
    solicitud_publicacion_id,
    dia_semana,
    hora_inicio,
    hora_fin,
    observacion
)
SELECT
    solicitud_publicacion_id,
    'VIERNES',
    TIME '09:00',
    TIME '10:00',
    'Horario para prueba de cascada'
FROM tmp_solicitud_publicacion_test_ids
WHERE nombre = 'cascade';

SELECT
    'Horarios antes de borrar solicitud cascade' AS verificacion,
    COUNT(*) AS horarios_antes
FROM solicitud_publicacion_horario h
JOIN tmp_solicitud_publicacion_test_ids t
    ON t.solicitud_publicacion_id = h.solicitud_publicacion_id
WHERE t.nombre = 'cascade';

DELETE FROM solicitud_publicacion sp
USING tmp_solicitud_publicacion_test_ids t
WHERE t.nombre = 'cascade'
  AND sp.id = t.solicitud_publicacion_id;

SELECT
    'Horarios despues de borrar solicitud cascade' AS verificacion,
    COUNT(*) AS horarios_despues
FROM solicitud_publicacion_horario h
JOIN tmp_solicitud_publicacion_test_ids t
    ON t.solicitud_publicacion_id = h.solicitud_publicacion_id
WHERE t.nombre = 'cascade';

DO $$
DECLARE
    horarios_restantes INTEGER;
BEGIN
    SELECT COUNT(*)
    INTO horarios_restantes
    FROM solicitud_publicacion_horario h
    JOIN tmp_solicitud_publicacion_test_ids t
        ON t.solicitud_publicacion_id = h.solicitud_publicacion_id
    WHERE t.nombre = 'cascade';

    IF horarios_restantes = 0 THEN
        RAISE NOTICE 'OK: la eliminacion fisica de la solicitud temporal borro sus horarios por cascada.';
    ELSE
        RAISE WARNING 'FALLO: quedaron % horarios asociados a la solicitud temporal borrada.', horarios_restantes;
    END IF;
END; $$;

-- ============================================================
-- 7. CONTROL FINAL DE DATOS TEMPORALES
-- ------------------------------------------------------------
-- Estas cantidades sirven para revisar que las pruebas insertaron
-- datos solo con codigos TEST-SP-*. Todo se revierte con ROLLBACK.
-- ============================================================

SELECT
    'Control final antes del ROLLBACK' AS verificacion,
    COUNT(*) AS solicitudes_temporales
FROM solicitud_publicacion
WHERE codigo_seguimiento LIKE 'TEST-SP-%';

SELECT
    'Horarios temporales antes del ROLLBACK' AS verificacion,
    COUNT(*) AS horarios_temporales
FROM solicitud_publicacion_horario h
JOIN solicitud_publicacion sp
    ON sp.id = h.solicitud_publicacion_id
WHERE sp.codigo_seguimiento LIKE 'TEST-SP-%';

-- ============================================================
-- 8. ROLLBACK OBLIGATORIO
-- ------------------------------------------------------------
-- No se confirma ningun dato ni objeto temporal creado por este
-- script. La base queda como estaba antes de ejecutarlo.
-- ============================================================

ROLLBACK;
