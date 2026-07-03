BEGIN;

-- ============================================================
-- DondeEntreno - Validacion de navegacion territorial
-- Archivo: 11_test_city_navigation_queries.sql
-- Base de datos: PostgreSQL
-- Descripcion:
-- Este script valida localmente la migracion 10.
--
-- Debe ejecutarse despues de aplicar:
-- 10_prepare_city_navigation.sql
--
-- Todo corre dentro de una transaccion y termina con ROLLBACK
-- para no dejar datos temporales persistidos.
-- ============================================================

-- ============================================================
-- 1. VALIDACIONES ESTRUCTURALES
-- ------------------------------------------------------------
-- Verificamos columnas, constraints e indice necesarios para
-- navegacion territorial por ciudad.
-- ============================================================

DO $$
BEGIN
    IF to_regclass('public.ciudad') IS NULL THEN
        RAISE EXCEPTION 'Falta la tabla public.ciudad.';
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'ciudad'
          AND column_name = 'slug'
    ) THEN
        RAISE EXCEPTION 'Falta la columna ciudad.slug. Ejecutar primero la migracion 10.';
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'ciudad'
          AND column_name = 'orden'
    ) THEN
        RAISE EXCEPTION 'Falta la columna ciudad.orden. Ejecutar primero la migracion 10.';
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'ciudad'
          AND column_name = 'slug'
          AND is_nullable = 'NO'
    ) THEN
        RAISE EXCEPTION 'La columna ciudad.slug debe ser NOT NULL.';
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE connamespace = 'public'::regnamespace
          AND conrelid = 'ciudad'::regclass
          AND conname = 'uq_ciudad_slug'
          AND contype = 'u'
    ) THEN
        RAISE EXCEPTION 'Falta la constraint unica uq_ciudad_slug.';
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE connamespace = 'public'::regnamespace
          AND conrelid = 'ciudad'::regclass
          AND conname = 'chk_ciudad_slug_no_vacio'
          AND contype = 'c'
    ) THEN
        RAISE EXCEPTION 'Falta la constraint chk_ciudad_slug_no_vacio.';
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE connamespace = 'public'::regnamespace
          AND conrelid = 'ciudad'::regclass
          AND conname = 'chk_ciudad_slug_formato'
          AND contype = 'c'
    ) THEN
        RAISE EXCEPTION 'Falta la constraint chk_ciudad_slug_formato.';
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE connamespace = 'public'::regnamespace
          AND conrelid = 'ciudad'::regclass
          AND conname = 'chk_ciudad_orden_no_negativo'
          AND contype = 'c'
    ) THEN
        RAISE EXCEPTION 'Falta la constraint chk_ciudad_orden_no_negativo.';
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_indexes
        WHERE schemaname = 'public'
          AND tablename = 'ciudad'
          AND indexname = 'idx_ciudad_activa_orden_nombre'
    ) THEN
        RAISE EXCEPTION 'Falta el indice idx_ciudad_activa_orden_nombre.';
    END IF;
END; $$;

-- Columnas territoriales de ciudad.
SELECT
    'Columnas territoriales de ciudad' AS verificacion,
    column_name,
    data_type,
    is_nullable,
    column_default
FROM information_schema.columns
WHERE table_schema = 'public'
  AND table_name = 'ciudad'
  AND column_name IN ('slug', 'orden', 'activa')
ORDER BY ordinal_position;

-- Constraints territoriales de ciudad.
SELECT
    'Constraints territoriales de ciudad' AS verificacion,
    conname AS constraint_name,
    pg_get_constraintdef(oid) AS definition
FROM pg_constraint
WHERE connamespace = 'public'::regnamespace
  AND conrelid = 'ciudad'::regclass
  AND conname IN (
      'uq_ciudad_slug',
      'chk_ciudad_slug_no_vacio',
      'chk_ciudad_slug_formato',
      'chk_ciudad_orden_no_negativo'
  )
ORDER BY conname;

-- Indices relevantes para navegacion por ciudad.
SELECT
    'Indices de ciudad' AS verificacion,
    indexname,
    indexdef
FROM pg_indexes
WHERE schemaname = 'public'
  AND tablename = 'ciudad'
  AND indexname IN (
      'idx_ciudad_activa',
      'idx_ciudad_activa_orden_nombre'
  )
ORDER BY indexname;

-- ============================================================
-- 2. VALIDACIONES DE DATOS EXISTENTES
-- ------------------------------------------------------------
-- Confirmamos que Mar del Plata quedo como ciudad inicial/default
-- y que no hay datos invalidos en ciudad.
-- ============================================================

DO $$
BEGIN
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
        FROM ciudad
        WHERE nombre = 'Mar del Plata'
          AND provincia = 'Buenos Aires'
          AND pais = 'Argentina'
          AND slug = 'mar-del-plata'
          AND orden = 1
          AND activa = true
    ) THEN
        RAISE EXCEPTION 'Mar del Plata debe tener slug mar-del-plata, orden 1 y activa true.';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM ciudad
        WHERE slug IS NULL
           OR BTRIM(slug) = ''
    ) THEN
        RAISE EXCEPTION 'Existen ciudades con slug vacio.';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM ciudad
        WHERE slug !~ '^[a-z0-9]+(-[a-z0-9]+)*$'
    ) THEN
        RAISE EXCEPTION 'Existen ciudades con slug de formato invalido.';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM ciudad
        WHERE orden < 0
    ) THEN
        RAISE EXCEPTION 'Existen ciudades con orden negativo.';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM ciudad
        GROUP BY slug
        HAVING COUNT(*) > 1
    ) THEN
        RAISE EXCEPTION 'Existen slugs duplicados en ciudad.';
    END IF;
END; $$;

SELECT
    'Mar del Plata preparada para navegacion' AS verificacion,
    id,
    nombre,
    slug,
    provincia,
    pais,
    activa,
    orden
FROM ciudad
WHERE slug = 'mar-del-plata';

-- ============================================================
-- 3. CIUDADES FUTURAS TEMPORALES
-- ------------------------------------------------------------
-- Creamos ciudades solo dentro de esta transaccion para probar
-- orden y formato de slug. No quedan persistidas por el ROLLBACK.
-- ============================================================

INSERT INTO ciudad (
    nombre,
    provincia,
    pais,
    slug,
    orden,
    activa
)
VALUES
    ('Miramar', 'Provincia Temporal Navegacion', 'Pais Temporal Navegacion', CONCAT('test-miramar-', txid_current()), 20, true),
    ('Balcarce', 'Provincia Temporal Navegacion', 'Pais Temporal Navegacion', CONCAT('test-balcarce-', txid_current()), 30, true),
    ('Necochea', 'Provincia Temporal Navegacion', 'Pais Temporal Navegacion', CONCAT('test-necochea-', txid_current()), 40, true),
    ('Otamendi', 'Provincia Temporal Navegacion', 'Pais Temporal Navegacion', CONCAT('test-otamendi-', txid_current()), 50, true),
    ('Tandil', 'Provincia Temporal Navegacion', 'Pais Temporal Navegacion', CONCAT('test-tandil-', txid_current()), 60, true);

SELECT
    'Ciudades futuras temporales' AS verificacion,
    nombre,
    slug,
    orden,
    activa
FROM ciudad
WHERE provincia = 'Provincia Temporal Navegacion'
  AND pais = 'Pais Temporal Navegacion'
ORDER BY orden, nombre;

-- ============================================================
-- 4. CONSULTAS TERRITORIALES DE DIAGNOSTICO
-- ------------------------------------------------------------
-- Estas consultas ayudan a revisar navegacion por ciudad,
-- barrios, actividades e inconsistencias de ubicacion.
-- ============================================================

-- Ciudades activas ordenadas por orden editorial y nombre.
SELECT
    'Ciudades activas ordenadas' AS diagnostico,
    id,
    nombre,
    slug,
    provincia,
    pais,
    orden
FROM ciudad
WHERE activa = true
ORDER BY orden, nombre;

-- Ciudades activas con cantidad de actividades publicadas y activas.
SELECT
    'Ciudades activas con actividades publicadas' AS diagnostico,
    c.id AS ciudad_id,
    c.nombre AS ciudad,
    c.slug,
    c.orden,
    COUNT(DISTINCT a.id) AS cantidad_actividades_publicadas
FROM ciudad c
LEFT JOIN ubicacion u
    ON u.ciudad_id = c.id
   AND u.activa = true
   AND u.deleted_at IS NULL
LEFT JOIN actividad a
    ON a.ubicacion_id = u.id
   AND a.estado_publicacion = 'PUBLICADA'
   AND a.activa = true
   AND a.deleted_at IS NULL
WHERE c.activa = true
GROUP BY c.id, c.nombre, c.slug, c.orden
ORDER BY c.orden, c.nombre;

-- Ciudades activas sin actividades publicadas.
SELECT
    'Ciudades activas sin actividades publicadas' AS diagnostico,
    c.id AS ciudad_id,
    c.nombre AS ciudad,
    c.slug,
    c.orden
FROM ciudad c
LEFT JOIN ubicacion u
    ON u.ciudad_id = c.id
   AND u.activa = true
   AND u.deleted_at IS NULL
LEFT JOIN actividad a
    ON a.ubicacion_id = u.id
   AND a.estado_publicacion = 'PUBLICADA'
   AND a.activa = true
   AND a.deleted_at IS NULL
WHERE c.activa = true
GROUP BY c.id, c.nombre, c.slug, c.orden
HAVING COUNT(DISTINCT a.id) = 0
ORDER BY c.orden, c.nombre;

-- Actividades publicadas agrupadas por ciudad.
SELECT
    'Actividades publicadas por ciudad' AS diagnostico,
    c.nombre AS ciudad,
    c.slug,
    a.id AS actividad_id,
    a.titulo AS actividad,
    a.slug AS actividad_slug
FROM actividad a
JOIN ubicacion u ON u.id = a.ubicacion_id
JOIN ciudad c ON c.id = u.ciudad_id
WHERE a.estado_publicacion = 'PUBLICADA'
  AND a.activa = true
  AND a.deleted_at IS NULL
  AND u.activa = true
  AND u.deleted_at IS NULL
  AND c.activa = true
ORDER BY c.orden, c.nombre, a.titulo;

-- Barrios por ciudad.
SELECT
    'Barrios por ciudad' AS diagnostico,
    c.nombre AS ciudad,
    c.slug AS ciudad_slug,
    b.id AS barrio_id,
    b.nombre AS barrio,
    b.activo
FROM barrio b
JOIN ciudad c ON c.id = b.ciudad_id
ORDER BY c.orden, c.nombre, b.nombre;

-- Ubicaciones cuyo barrio no pertenece a la misma ciudad.
-- Riesgo conocido: todavia no existe una FK compuesta en la base.
SELECT
    'Ubicaciones con ciudad y barrio inconsistentes' AS diagnostico,
    u.id AS ubicacion_id,
    u.nombre AS ubicacion,
    u.ciudad_id AS ubicacion_ciudad_id,
    c.nombre AS ubicacion_ciudad,
    u.barrio_id,
    b.nombre AS barrio,
    b.ciudad_id AS barrio_ciudad_id,
    cb.nombre AS barrio_ciudad
FROM ubicacion u
JOIN ciudad c ON c.id = u.ciudad_id
JOIN barrio b ON b.id = u.barrio_id
JOIN ciudad cb ON cb.id = b.ciudad_id
WHERE b.ciudad_id <> u.ciudad_id
ORDER BY u.id;

-- Buscar una ciudad por slug.
SELECT
    'Busqueda de ciudad por slug' AS diagnostico,
    id,
    nombre,
    slug,
    activa,
    orden
FROM ciudad
WHERE slug = 'mar-del-plata';

-- Simular navegacion territorial hacia actividades de Mar del Plata.
SELECT
    'Navegacion territorial mar-del-plata' AS diagnostico,
    c.slug AS ciudad_slug,
    c.nombre AS ciudad,
    a.titulo AS actividad,
    a.slug AS actividad_slug,
    b.nombre AS barrio
FROM ciudad c
JOIN ubicacion u ON u.ciudad_id = c.id
JOIN barrio b ON b.id = u.barrio_id
JOIN actividad a ON a.ubicacion_id = u.id
WHERE c.slug = 'mar-del-plata'
  AND c.activa = true
  AND u.activa = true
  AND u.deleted_at IS NULL
  AND a.estado_publicacion = 'PUBLICADA'
  AND a.activa = true
  AND a.deleted_at IS NULL
ORDER BY a.titulo;

-- ============================================================
-- 5. PRUEBAS NEGATIVAS
-- ------------------------------------------------------------
-- Capturamos errores esperados. Si un dato invalido fuera
-- aceptado, se emite WARNING y se borra el dato temporal para no
-- interferir con las consultas siguientes.
-- ============================================================

DO $$
BEGIN
    INSERT INTO ciudad (nombre, provincia, pais, slug, orden, activa)
    VALUES (
        'Ciudad Temporal Slug Duplicado',
        'Provincia Temporal Negativa',
        'Pais Temporal Negativa',
        'mar-del-plata',
        100,
        true
    );

    DELETE FROM ciudad
    WHERE nombre = 'Ciudad Temporal Slug Duplicado'
      AND provincia = 'Provincia Temporal Negativa'
      AND pais = 'Pais Temporal Negativa';

    RAISE WARNING 'FALLO: se acepto una ciudad con slug duplicado.';
EXCEPTION
    WHEN unique_violation THEN
        RAISE NOTICE 'OK: slug duplicado fue rechazado. %', SQLERRM;
END; $$;

DO $$
BEGIN
    INSERT INTO ciudad (nombre, provincia, pais, slug, orden, activa)
    VALUES (
        'Ciudad Temporal Slug Vacio',
        'Provincia Temporal Negativa',
        'Pais Temporal Negativa',
        '',
        100,
        true
    );

    DELETE FROM ciudad
    WHERE nombre = 'Ciudad Temporal Slug Vacio'
      AND provincia = 'Provincia Temporal Negativa'
      AND pais = 'Pais Temporal Negativa';

    RAISE WARNING 'FALLO: se acepto una ciudad con slug vacio.';
EXCEPTION
    WHEN check_violation THEN
        RAISE NOTICE 'OK: slug vacio fue rechazado. %', SQLERRM;
END; $$;

DO $$
BEGIN
    INSERT INTO ciudad (nombre, provincia, pais, slug, orden, activa)
    VALUES (
        'Ciudad Temporal Slug Con Espacios',
        'Provincia Temporal Negativa',
        'Pais Temporal Negativa',
        'slug con espacios',
        100,
        true
    );

    DELETE FROM ciudad
    WHERE nombre = 'Ciudad Temporal Slug Con Espacios'
      AND provincia = 'Provincia Temporal Negativa'
      AND pais = 'Pais Temporal Negativa';

    RAISE WARNING 'FALLO: se acepto una ciudad con slug con espacios.';
EXCEPTION
    WHEN check_violation THEN
        RAISE NOTICE 'OK: slug con espacios fue rechazado. %', SQLERRM;
END; $$;

DO $$
BEGIN
    INSERT INTO ciudad (nombre, provincia, pais, slug, orden, activa)
    VALUES (
        'Ciudad Temporal Slug Mayusculas',
        'Provincia Temporal Negativa',
        'Pais Temporal Negativa',
        'Slug-Mayusculas',
        100,
        true
    );

    DELETE FROM ciudad
    WHERE nombre = 'Ciudad Temporal Slug Mayusculas'
      AND provincia = 'Provincia Temporal Negativa'
      AND pais = 'Pais Temporal Negativa';

    RAISE WARNING 'FALLO: se acepto una ciudad con slug con mayusculas.';
EXCEPTION
    WHEN check_violation THEN
        RAISE NOTICE 'OK: slug con mayusculas fue rechazado. %', SQLERRM;
END; $$;

DO $$
BEGIN
    INSERT INTO ciudad (nombre, provincia, pais, slug, orden, activa)
    VALUES (
        'Ciudad Temporal Slug Inicio Guion',
        'Provincia Temporal Negativa',
        'Pais Temporal Negativa',
        '-slug-invalido',
        100,
        true
    );

    DELETE FROM ciudad
    WHERE nombre = 'Ciudad Temporal Slug Inicio Guion'
      AND provincia = 'Provincia Temporal Negativa'
      AND pais = 'Pais Temporal Negativa';

    RAISE WARNING 'FALLO: se acepto una ciudad con slug empezando con guion.';
EXCEPTION
    WHEN check_violation THEN
        RAISE NOTICE 'OK: slug empezando con guion fue rechazado. %', SQLERRM;
END; $$;

DO $$
BEGIN
    INSERT INTO ciudad (nombre, provincia, pais, slug, orden, activa)
    VALUES (
        'Ciudad Temporal Slug Fin Guion',
        'Provincia Temporal Negativa',
        'Pais Temporal Negativa',
        'slug-invalido-',
        100,
        true
    );

    DELETE FROM ciudad
    WHERE nombre = 'Ciudad Temporal Slug Fin Guion'
      AND provincia = 'Provincia Temporal Negativa'
      AND pais = 'Pais Temporal Negativa';

    RAISE WARNING 'FALLO: se acepto una ciudad con slug terminando con guion.';
EXCEPTION
    WHEN check_violation THEN
        RAISE NOTICE 'OK: slug terminando con guion fue rechazado. %', SQLERRM;
END; $$;

DO $$
BEGIN
    INSERT INTO ciudad (nombre, provincia, pais, slug, orden, activa)
    VALUES (
        'Ciudad Temporal Orden Negativo',
        'Provincia Temporal Negativa',
        'Pais Temporal Negativa',
        CONCAT('test-orden-negativo-', txid_current()),
        -1,
        true
    );

    DELETE FROM ciudad
    WHERE nombre = 'Ciudad Temporal Orden Negativo'
      AND provincia = 'Provincia Temporal Negativa'
      AND pais = 'Pais Temporal Negativa';

    RAISE WARNING 'FALLO: se acepto una ciudad con orden negativo.';
EXCEPTION
    WHEN check_violation THEN
        RAISE NOTICE 'OK: orden negativo fue rechazado. %', SQLERRM;
END; $$;

-- ============================================================
-- 6. CONTROL FINAL DE DATOS TEMPORALES
-- ------------------------------------------------------------
-- Este control muestra las ciudades temporales creadas por el
-- script antes del ROLLBACK.
-- ============================================================

SELECT
    'Control final de ciudades temporales' AS verificacion,
    COUNT(*) AS ciudades_temporales
FROM ciudad
WHERE provincia IN (
      'Provincia Temporal Navegacion',
      'Provincia Temporal Negativa'
  )
  AND pais IN (
      'Pais Temporal Navegacion',
      'Pais Temporal Negativa'
  );

ROLLBACK;
