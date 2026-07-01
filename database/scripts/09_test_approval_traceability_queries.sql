BEGIN;

-- ============================================================
-- DondeEntreno - Validacion de trazabilidad de aprobacion
-- Archivo: 09_test_approval_traceability_queries.sql
-- Base de datos: PostgreSQL
-- Descripcion:
-- Este script valida localmente que el modelo actual permite
-- representar el flujo:
--
-- solicitud_publicacion -> actividad creada
--
-- sin agregar columnas nuevas ni modificar tablas existentes.
--
-- Todo corre dentro de una transaccion y termina con ROLLBACK
-- para no dejar datos persistidos.
-- ============================================================

-- ============================================================
-- 1. VERIFICACION ESTRUCTURAL BASICA
-- ------------------------------------------------------------
-- Validamos que existan las tablas requeridas para simular una
-- aprobacion completa de solicitud a actividad.
-- ============================================================

DO $$
DECLARE
    tabla_esperada TEXT;
BEGIN
    FOREACH tabla_esperada IN ARRAY ARRAY[
        'rol',
        'usuario',
        'perfil_publicador',
        'ciudad',
        'barrio',
        'deporte',
        'ubicacion',
        'actividad',
        'horario_actividad',
        'solicitud_publicacion',
        'solicitud_publicacion_horario'
    ]
    LOOP
        IF to_regclass('public.' || tabla_esperada) IS NULL THEN
            RAISE EXCEPTION
                'Falta la tabla public.%. Ejecutar primero las migraciones estructurales requeridas.',
                tabla_esperada;
        END IF;
    END LOOP;
END; $$;

-- ============================================================
-- 2. VALIDACION DE DATOS BASE
-- ------------------------------------------------------------
-- No usamos IDs fijos. Resolvemos referencias por datos estables
-- cargados por los scripts de seed.
-- ============================================================

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM rol
        WHERE nombre IN ('ADMIN', 'SUPER_ADMIN')
    ) THEN
        RAISE EXCEPTION 'Falta un rol ADMIN o SUPER_ADMIN para simular el revisor.';
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM rol
        WHERE nombre = 'PUBLICADOR'
    ) THEN
        RAISE EXCEPTION 'Falta el rol PUBLICADOR para crear el perfil publicador temporal.';
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM deporte
        WHERE slug = 'boxeo'
    ) THEN
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
END; $$;

-- Guardamos referencias resueltas y valores temporales sin depender
-- de IDs numericos fijos.
CREATE TEMP TABLE tmp_aprobacion_trazabilidad_contexto (
    admin_rol_id BIGINT,
    publicador_rol_id BIGINT,
    deporte_id BIGINT,
    ciudad_id BIGINT,
    barrio_id BIGINT,
    codigo_solicitud VARCHAR(40),
    slug_actividad VARCHAR(180),
    email_admin VARCHAR(150),
    email_publicador VARCHAR(150)
);

WITH admin_rol AS (
    SELECT id
    FROM rol
    WHERE nombre IN ('ADMIN', 'SUPER_ADMIN')
    ORDER BY
        CASE nombre
            WHEN 'ADMIN' THEN 1
            WHEN 'SUPER_ADMIN' THEN 2
        END
    LIMIT 1
),
publicador_rol AS (
    SELECT id
    FROM rol
    WHERE nombre = 'PUBLICADOR'
    LIMIT 1
),
ciudad_base AS (
    SELECT id
    FROM ciudad
    WHERE nombre = 'Mar del Plata'
      AND provincia = 'Buenos Aires'
      AND pais = 'Argentina'
    LIMIT 1
),
barrio_base AS (
    SELECT b.id
    FROM barrio b
    JOIN ciudad_base c ON c.id = b.ciudad_id
    WHERE b.nombre = 'Centro'
    LIMIT 1
),
deporte_base AS (
    SELECT id
    FROM deporte
    WHERE slug = 'boxeo'
    LIMIT 1
)
INSERT INTO tmp_aprobacion_trazabilidad_contexto (
    admin_rol_id,
    publicador_rol_id,
    deporte_id,
    ciudad_id,
    barrio_id,
    codigo_solicitud,
    slug_actividad,
    email_admin,
    email_publicador
)
SELECT
    ar.id,
    pr.id,
    d.id,
    c.id,
    b.id,
    CONCAT('TEST-APR-', txid_current(), '-', pg_backend_pid()),
    CONCAT('aprobacion-solicitud-', txid_current(), '-', pg_backend_pid()),
    CONCAT('approval.admin.', txid_current(), '.', pg_backend_pid(), '@example.invalid'),
    CONCAT('approval.publisher.', txid_current(), '.', pg_backend_pid(), '@example.invalid')
FROM admin_rol ar
CROSS JOIN publicador_rol pr
CROSS JOIN deporte_base d
CROSS JOIN ciudad_base c
CROSS JOIN barrio_base b;

SELECT
    'Referencias base resueltas' AS verificacion,
    r_admin.nombre AS rol_admin,
    r_publicador.nombre AS rol_publicador,
    d.slug AS deporte_slug,
    c.nombre AS ciudad,
    b.nombre AS barrio,
    ctx.codigo_solicitud,
    ctx.slug_actividad
FROM tmp_aprobacion_trazabilidad_contexto ctx
JOIN rol r_admin ON r_admin.id = ctx.admin_rol_id
JOIN rol r_publicador ON r_publicador.id = ctx.publicador_rol_id
JOIN deporte d ON d.id = ctx.deporte_id
JOIN ciudad c ON c.id = ctx.ciudad_id
JOIN barrio b ON b.id = ctx.barrio_id;

CREATE TEMP TABLE tmp_aprobacion_trazabilidad_ids (
    nombre VARCHAR(80),
    id BIGINT
);

-- Funcion temporal para crear solicitudes auxiliares en las pruebas
-- negativas sin repetir todos los campos obligatorios.
CREATE FUNCTION pg_temp.crear_solicitud_aprobacion_test(
    p_codigo VARCHAR,
    p_estado VARCHAR DEFAULT 'PENDIENTE',
    p_actividad_generada_id BIGINT DEFAULT NULL,
    p_motivo_rechazo TEXT DEFAULT NULL,
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
    FROM tmp_aprobacion_trazabilidad_contexto
    LIMIT 1;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'No hay contexto temporal para crear solicitudes de prueba.';
    END IF;

    INSERT INTO solicitud_publicacion (
        codigo_seguimiento,
        origen,
        estado,
        tipo_publicador,
        nombre_publicador,
        nombre_actividad,
        deporte_id,
        descripcion,
        nivel,
        enfoque,
        modalidad,
        edad_minima,
        edad_maxima,
        precio_referencia,
        mostrar_precio,
        ciudad_id,
        barrio_id,
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
        p_codigo,
        'FORMULARIO_WEB',
        p_estado,
        'GIMNASIO',
        'Gimnasio Temporal de Aprobacion',
        'Boxeo temporal para aprobacion',
        contexto.deporte_id,
        'Solicitud temporal creada para validar trazabilidad de aprobacion.',
        'PRINCIPIANTE',
        'RECREATIVO',
        'PRESENCIAL',
        18,
        60,
        15000.00,
        true,
        contexto.ciudad_id,
        contexto.barrio_id,
        'Sede temporal de aprobacion',
        'San Martin 1000',
        'Ingreso principal',
        '+54 9 223 555 0101',
        '5492235550101',
        '@aprobacion_test',
        CONCAT('solicitud.', p_codigo, '@example.invalid'),
        'Registro temporal para pruebas de aprobacion.',
        true,
        NULL,
        p_actividad_generada_id,
        p_motivo_rechazo,
        NULL,
        p_revision_iniciada_at,
        p_revision_finalizada_at,
        '127.0.0.1'::inet
    )
    RETURNING id INTO solicitud_id;

    RETURN solicitud_id;
END; $$;

-- ============================================================
-- 3. FLUJO POSITIVO
-- ------------------------------------------------------------
-- Creamos datos temporales suficientes para simular aprobacion:
-- usuario admin, usuario publicador, perfil, ubicacion, solicitud,
-- horarios de solicitud, actividad y horarios de actividad.
-- ============================================================

WITH nuevo_admin AS (
    INSERT INTO usuario (
        rol_id,
        nombre,
        apellido,
        email,
        password_hash,
        activo,
        email_verificado
    )
    SELECT
        admin_rol_id,
        'Admin Temporal',
        'Aprobacion',
        email_admin,
        'hash-temporal-no-vacio',
        true,
        true
    FROM tmp_aprobacion_trazabilidad_contexto
    RETURNING id
)
INSERT INTO tmp_aprobacion_trazabilidad_ids (nombre, id)
SELECT 'usuario_admin', id
FROM nuevo_admin;

WITH nuevo_publicador AS (
    INSERT INTO usuario (
        rol_id,
        nombre,
        apellido,
        email,
        password_hash,
        activo,
        email_verificado
    )
    SELECT
        publicador_rol_id,
        'Publicador Temporal',
        'Aprobacion',
        email_publicador,
        'hash-temporal-no-vacio',
        true,
        true
    FROM tmp_aprobacion_trazabilidad_contexto
    RETURNING id
)
INSERT INTO tmp_aprobacion_trazabilidad_ids (nombre, id)
SELECT 'usuario_publicador', id
FROM nuevo_publicador;

WITH nueva_solicitud AS (
    INSERT INTO solicitud_publicacion (
        codigo_seguimiento,
        origen,
        estado,
        tipo_publicador,
        nombre_publicador,
        nombre_actividad,
        deporte_id,
        descripcion,
        nivel,
        enfoque,
        modalidad,
        edad_minima,
        edad_maxima,
        precio_referencia,
        mostrar_precio,
        ciudad_id,
        barrio_id,
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
        motivo_rechazo,
        observaciones_revision,
        revision_iniciada_at,
        ip_origen
    )
    SELECT
        ctx.codigo_solicitud,
        'FORMULARIO_WEB',
        'EN_REVISION',
        'GIMNASIO',
        'Gimnasio Temporal de Aprobacion',
        'Boxeo temporal para aprobacion',
        ctx.deporte_id,
        'Solicitud temporal creada para validar la trazabilidad entre solicitud y actividad.',
        'PRINCIPIANTE',
        'RECREATIVO',
        'PRESENCIAL',
        18,
        60,
        15000.00,
        true,
        ctx.ciudad_id,
        ctx.barrio_id,
        'Sede temporal de aprobacion',
        'San Martin 1000',
        'Ingreso principal',
        '+54 9 223 555 0101',
        '5492235550101',
        '@aprobacion_test',
        'solicitud.aprobacion@example.invalid',
        'Solicitud temporal para validar aprobacion administrativa.',
        true,
        admin.id,
        NULL,
        'Solicitud tomada para revision en prueba local.',
        CURRENT_TIMESTAMP,
        '127.0.0.1'::inet
    FROM tmp_aprobacion_trazabilidad_contexto ctx
    JOIN tmp_aprobacion_trazabilidad_ids admin
        ON admin.nombre = 'usuario_admin'
    RETURNING id
)
INSERT INTO tmp_aprobacion_trazabilidad_ids (nombre, id)
SELECT 'solicitud', id
FROM nueva_solicitud;

INSERT INTO solicitud_publicacion_horario (
    solicitud_publicacion_id,
    dia_semana,
    hora_inicio,
    hora_fin,
    observacion
)
SELECT
    s.id,
    h.dia_semana,
    h.hora_inicio,
    h.hora_fin,
    h.observacion
FROM tmp_aprobacion_trazabilidad_ids s
CROSS JOIN (
    VALUES
        ('LUNES', TIME '18:00', TIME '19:00', 'Horario temporal de solicitud'),
        ('MIERCOLES', TIME '18:00', TIME '19:00', 'Horario temporal de solicitud')
) AS h(dia_semana, hora_inicio, hora_fin, observacion)
WHERE s.nombre = 'solicitud';

WITH nuevo_perfil AS (
    INSERT INTO perfil_publicador (
        usuario_id,
        nombre,
        tipo_publicador,
        descripcion,
        email_contacto,
        telefono_contacto,
        whatsapp,
        instagram,
        activo,
        verificado
    )
    SELECT
        u.id,
        'Gimnasio Temporal de Aprobacion',
        'GIMNASIO',
        'Perfil temporal creado para validar trazabilidad de aprobacion.',
        'publicador.aprobacion@example.invalid',
        '+54 223 555 0101',
        '+54 9 223 555 0101',
        '@aprobacion_test',
        true,
        false
    FROM tmp_aprobacion_trazabilidad_ids u
    WHERE u.nombre = 'usuario_publicador'
    RETURNING id
)
INSERT INTO tmp_aprobacion_trazabilidad_ids (nombre, id)
SELECT 'perfil_publicador', id
FROM nuevo_perfil;

WITH nueva_ubicacion AS (
    INSERT INTO ubicacion (
        perfil_publicador_id,
        ciudad_id,
        barrio_id,
        nombre,
        direccion,
        referencia,
        activa
    )
    SELECT
        p.id,
        ctx.ciudad_id,
        ctx.barrio_id,
        'Sede temporal de aprobacion',
        'San Martin 1000',
        'Ingreso principal',
        true
    FROM tmp_aprobacion_trazabilidad_ids p
    CROSS JOIN tmp_aprobacion_trazabilidad_contexto ctx
    WHERE p.nombre = 'perfil_publicador'
    RETURNING id
)
INSERT INTO tmp_aprobacion_trazabilidad_ids (nombre, id)
SELECT 'ubicacion', id
FROM nueva_ubicacion;

WITH nueva_actividad AS (
    INSERT INTO actividad (
        perfil_publicador_id,
        deporte_id,
        ubicacion_id,
        titulo,
        slug,
        descripcion,
        edad_minima,
        edad_maxima,
        nivel,
        enfoque,
        modalidad,
        precio_referencia,
        mostrar_precio,
        requiere_inscripcion,
        cupos_limitados,
        whatsapp_contacto,
        instagram_contacto,
        email_contacto,
        estado_publicacion,
        motivo_rechazo,
        activa
    )
    SELECT
        p.id,
        ctx.deporte_id,
        u.id,
        'Boxeo temporal aprobado',
        ctx.slug_actividad,
        'Actividad temporal creada para validar trazabilidad desde una solicitud aprobada.',
        18,
        60,
        'PRINCIPIANTE',
        'RECREATIVO',
        'PRESENCIAL',
        15000.00,
        true,
        true,
        false,
        '+54 9 223 555 0101',
        '@aprobacion_test',
        'publicador.aprobacion@example.invalid',
        'PUBLICADA',
        NULL,
        true
    FROM tmp_aprobacion_trazabilidad_contexto ctx
    JOIN tmp_aprobacion_trazabilidad_ids p
        ON p.nombre = 'perfil_publicador'
    JOIN tmp_aprobacion_trazabilidad_ids u
        ON u.nombre = 'ubicacion'
    RETURNING id
)
INSERT INTO tmp_aprobacion_trazabilidad_ids (nombre, id)
SELECT 'actividad', id
FROM nueva_actividad;

INSERT INTO horario_actividad (
    actividad_id,
    dia_semana,
    hora_inicio,
    hora_fin,
    observacion,
    activo
)
SELECT
    a.id,
    h.dia_semana,
    h.hora_inicio,
    h.hora_fin,
    'Copiado desde solicitud temporal',
    true
FROM tmp_aprobacion_trazabilidad_ids a
JOIN tmp_aprobacion_trazabilidad_ids s
    ON s.nombre = 'solicitud'
JOIN solicitud_publicacion_horario h
    ON h.solicitud_publicacion_id = s.id
WHERE a.nombre = 'actividad';

UPDATE solicitud_publicacion sp
SET
    estado = 'APROBADA',
    actividad_generada_id = a.id,
    revisado_por_usuario_id = admin.id,
    revision_finalizada_at = CURRENT_TIMESTAMP,
    updated_at = CURRENT_TIMESTAMP,
    motivo_rechazo = NULL,
    observaciones_revision = 'Solicitud aprobada en prueba local de trazabilidad.'
FROM tmp_aprobacion_trazabilidad_ids s
JOIN tmp_aprobacion_trazabilidad_ids a
    ON a.nombre = 'actividad'
JOIN tmp_aprobacion_trazabilidad_ids admin
    ON admin.nombre = 'usuario_admin'
WHERE s.nombre = 'solicitud'
  AND sp.id = s.id;

-- Trazabilidad principal entre solicitud aprobada y actividad creada.
SELECT
    'Trazabilidad solicitud a actividad' AS verificacion,
    sp.codigo_seguimiento,
    sp.estado,
    sp.actividad_generada_id,
    u.email AS admin_revisor,
    sp.revision_finalizada_at,
    a.titulo AS titulo_actividad,
    a.slug AS slug_actividad,
    a.estado_publicacion
FROM solicitud_publicacion sp
JOIN actividad a ON a.id = sp.actividad_generada_id
JOIN usuario u ON u.id = sp.revisado_por_usuario_id
JOIN tmp_aprobacion_trazabilidad_ids s ON s.id = sp.id
WHERE s.nombre = 'solicitud';

-- Horarios de la actividad generada desde los horarios de la solicitud.
SELECT
    'Horarios de la actividad generada' AS verificacion,
    sp.codigo_seguimiento,
    a.titulo AS titulo_actividad,
    h.dia_semana,
    h.hora_inicio,
    h.hora_fin,
    h.observacion
FROM solicitud_publicacion sp
JOIN actividad a ON a.id = sp.actividad_generada_id
JOIN horario_actividad h ON h.actividad_id = a.id
JOIN tmp_aprobacion_trazabilidad_ids s ON s.id = sp.id
WHERE s.nombre = 'solicitud'
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

-- ============================================================
-- 4. PRUEBAS NEGATIVAS
-- ------------------------------------------------------------
-- Cada bloque captura el error esperado. Si una operacion invalida
-- fuera aceptada, se emite WARNING para dejarlo visible.
-- ============================================================

DO $$
DECLARE
    actividad_id_existente BIGINT;
BEGIN
    SELECT id
    INTO actividad_id_existente
    FROM tmp_aprobacion_trazabilidad_ids
    WHERE nombre = 'actividad';

    PERFORM pg_temp.crear_solicitud_aprobacion_test(
        CONCAT('TEST-APR-DUP-', txid_current()),
        p_estado => 'APROBADA',
        p_actividad_generada_id => actividad_id_existente
    );

    RAISE WARNING 'FALLO: se acepto la misma actividad_generada_id en dos solicitudes.';
EXCEPTION
    WHEN unique_violation THEN
        RAISE NOTICE 'OK: actividad_generada_id duplicada fue rechazada por la constraint unica. %', SQLERRM;
END; $$;

DO $$
BEGIN
    PERFORM pg_temp.crear_solicitud_aprobacion_test(
        CONCAT('TEST-APR-RECH-', txid_current()),
        p_estado => 'RECHAZADA',
        p_motivo_rechazo => NULL
    );

    RAISE WARNING 'FALLO: se acepto estado RECHAZADA sin motivo_rechazo.';
EXCEPTION
    WHEN check_violation THEN
        RAISE NOTICE 'OK: RECHAZADA sin motivo_rechazo fue rechazada. %', SQLERRM;
END; $$;

DO $$
BEGIN
    PERFORM pg_temp.crear_solicitud_aprobacion_test(
        CONCAT('TEST-APR-MOT-', txid_current()),
        p_estado => 'APROBADA',
        p_motivo_rechazo => 'Motivo que no corresponde a una aprobacion'
    );

    RAISE WARNING 'FALLO: se acepto estado APROBADA con motivo_rechazo informado.';
EXCEPTION
    WHEN check_violation THEN
        RAISE NOTICE 'OK: APROBADA con motivo_rechazo fue rechazada. %', SQLERRM;
END; $$;

DO $$
BEGIN
    PERFORM pg_temp.crear_solicitud_aprobacion_test(
        CONCAT('TEST-APR-FECH-', txid_current()),
        p_revision_iniciada_at => CURRENT_TIMESTAMP,
        p_revision_finalizada_at => CURRENT_TIMESTAMP - INTERVAL '1 hour'
    );

    RAISE WARNING 'FALLO: se acepto revision_finalizada_at anterior a revision_iniciada_at.';
EXCEPTION
    WHEN check_violation THEN
        RAISE NOTICE 'OK: revision_finalizada_at anterior a revision_iniciada_at fue rechazada. %', SQLERRM;
END; $$;

-- ============================================================
-- 5. CONSULTAS DE APOYO PARA DIAGNOSTICO
-- ------------------------------------------------------------
-- Consultas de solo lectura para revisar trazabilidad y estados.
-- Sirven para diagnostico local y soporte futuro.
-- ============================================================

-- Solicitudes aprobadas que ya tienen actividad generada.
SELECT
    'Solicitudes APROBADAS con actividad generada' AS diagnostico,
    sp.id AS solicitud_id,
    sp.codigo_seguimiento,
    sp.estado,
    sp.actividad_generada_id,
    a.titulo AS titulo_actividad,
    sp.revision_finalizada_at
FROM solicitud_publicacion sp
JOIN actividad a ON a.id = sp.actividad_generada_id
WHERE sp.estado = 'APROBADA'
  AND sp.actividad_generada_id IS NOT NULL
ORDER BY sp.revision_finalizada_at DESC NULLS LAST, sp.created_at DESC;

-- Solicitudes aprobadas sin actividad generada.
SELECT
    'Solicitudes APROBADAS sin actividad generada' AS diagnostico,
    sp.id AS solicitud_id,
    sp.codigo_seguimiento,
    sp.estado,
    sp.revision_finalizada_at
FROM solicitud_publicacion sp
WHERE sp.estado = 'APROBADA'
  AND sp.actividad_generada_id IS NULL
ORDER BY sp.revision_finalizada_at DESC NULLS LAST, sp.created_at DESC;

-- Solicitudes con actividad generada pero con estado distinto de APROBADA.
SELECT
    'Solicitudes con actividad_generada_id y estado distinto de APROBADA' AS diagnostico,
    sp.id AS solicitud_id,
    sp.codigo_seguimiento,
    sp.estado,
    sp.actividad_generada_id,
    a.titulo AS titulo_actividad
FROM solicitud_publicacion sp
JOIN actividad a ON a.id = sp.actividad_generada_id
WHERE sp.estado <> 'APROBADA'
ORDER BY sp.created_at DESC;

-- Solicitudes rechazadas con motivo.
SELECT
    'Solicitudes RECHAZADAS con motivo' AS diagnostico,
    sp.id AS solicitud_id,
    sp.codigo_seguimiento,
    sp.estado,
    sp.motivo_rechazo,
    sp.revision_finalizada_at,
    u.email AS usuario_revisor
FROM solicitud_publicacion sp
LEFT JOIN usuario u ON u.id = sp.revisado_por_usuario_id
WHERE sp.estado = 'RECHAZADA'
  AND sp.motivo_rechazo IS NOT NULL
ORDER BY sp.revision_finalizada_at DESC NULLS LAST, sp.created_at DESC;

-- Cantidad de solicitudes por estado.
SELECT
    'Solicitudes por estado' AS diagnostico,
    sp.estado,
    COUNT(*) AS cantidad
FROM solicitud_publicacion sp
GROUP BY sp.estado
ORDER BY sp.estado;

-- Actividades generadas desde solicitudes.
SELECT
    'Actividades generadas desde solicitudes' AS diagnostico,
    a.id AS actividad_id,
    a.titulo,
    a.slug,
    a.estado_publicacion,
    sp.id AS solicitud_id,
    sp.codigo_seguimiento,
    sp.revision_finalizada_at,
    COUNT(h.id) AS cantidad_horarios
FROM solicitud_publicacion sp
JOIN actividad a ON a.id = sp.actividad_generada_id
LEFT JOIN horario_actividad h ON h.actividad_id = a.id
GROUP BY
    a.id,
    a.titulo,
    a.slug,
    a.estado_publicacion,
    sp.id,
    sp.codigo_seguimiento,
    sp.revision_finalizada_at
ORDER BY sp.revision_finalizada_at DESC NULLS LAST, sp.created_at DESC;

-- ============================================================
-- 6. CONTROL FINAL ANTES DEL ROLLBACK
-- ------------------------------------------------------------
-- Estas cantidades muestran los datos temporales creados por el
-- script. Al finalizar se revierten con ROLLBACK.
-- ============================================================

SELECT
    'Control final de datos temporales' AS verificacion,
    (SELECT COUNT(*) FROM usuario u
     JOIN tmp_aprobacion_trazabilidad_contexto ctx
       ON u.email IN (ctx.email_admin, ctx.email_publicador)) AS usuarios_temporales,
    (SELECT COUNT(*) FROM perfil_publicador p
     JOIN tmp_aprobacion_trazabilidad_ids ids
       ON ids.nombre = 'perfil_publicador'
      AND ids.id = p.id) AS perfiles_temporales,
    (SELECT COUNT(*) FROM ubicacion ub
     JOIN tmp_aprobacion_trazabilidad_ids ids
       ON ids.nombre = 'ubicacion'
      AND ids.id = ub.id) AS ubicaciones_temporales,
    (SELECT COUNT(*) FROM actividad a
     JOIN tmp_aprobacion_trazabilidad_ids ids
       ON ids.nombre = 'actividad'
      AND ids.id = a.id) AS actividades_temporales,
    (SELECT COUNT(*) FROM solicitud_publicacion sp
     JOIN tmp_aprobacion_trazabilidad_contexto ctx
       ON sp.codigo_seguimiento = ctx.codigo_solicitud) AS solicitudes_temporales;

ROLLBACK;
