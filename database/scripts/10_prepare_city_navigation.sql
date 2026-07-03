BEGIN;

-- ============================================================
-- DondeEntreno - Preparacion de navegacion territorial
-- Archivo: 10_prepare_city_navigation.sql
-- Base de datos: PostgreSQL
-- Descripcion:
-- Esta migracion prepara la tabla ciudad para navegacion publica
-- con rutas territoriales como:
--
-- /ciudades
-- /ciudades/[slug]
-- /ciudades/mar-del-plata
--
-- Es una migracion no destructiva:
-- - no borra datos;
-- - no modifica actividades existentes;
-- - no modifica barrios existentes;
-- - no modifica ubicaciones existentes;
-- - no modifica solicitudes;
-- - no inserta ciudades futuras persistentes;
-- - no crea triggers, vistas ni funciones permanentes.
-- ============================================================

-- ============================================================
-- 1. COLUMNAS NUEVAS
-- ------------------------------------------------------------
-- slug permite construir URLs estables por ciudad.
-- orden permite ordenar ciudades editorialmente sin depender
-- solamente del nombre.
-- ============================================================

ALTER TABLE ciudad
ADD COLUMN slug VARCHAR(120);

ALTER TABLE ciudad
ADD COLUMN orden INTEGER NOT NULL DEFAULT 100;

COMMENT ON COLUMN ciudad.slug IS
    'Slug publico de la ciudad para rutas territoriales como /ciudades/mar-del-plata.';

COMMENT ON COLUMN ciudad.orden IS
    'Orden editorial para listar ciudades. Los valores mas bajos aparecen primero.';

-- ============================================================
-- 2. BACKFILL DE CIUDADES EXISTENTES
-- ------------------------------------------------------------
-- No asumimos que una base existente tenga solamente Mar del Plata.
--
-- Mar del Plata queda como ciudad inicial/default:
-- - slug = mar-del-plata
-- - orden = 1
-- - activa = true
--
-- Para cualquier otra ciudad existente se genera un slug seguro
-- a partir del nombre. No usamos extensiones como unaccent.
-- Si hay colisiones de slug, se agrega un sufijo con el id.
-- ============================================================

WITH candidatos AS (
    SELECT
        id,
        nombre,
        provincia,
        pais,
        CASE
            WHEN nombre = 'Mar del Plata'
             AND provincia = 'Buenos Aires'
             AND pais = 'Argentina'
                THEN 'mar-del-plata'
            ELSE COALESCE(
                NULLIF(
                    LEFT(
                        BTRIM(
                            REGEXP_REPLACE(
                                TRANSLATE(
                                    LOWER(BTRIM(nombre)),
                                    'áàäâãåéèëêíìïîóòöôõúùüûñç',
                                    'aaaaaaeeeeiiiiooooouuuunc'
                                ),
                                '[^a-z0-9]+',
                                '-',
                                'g'
                            ),
                            '-'
                        ),
                        100
                    ),
                    ''
                ),
                'ciudad-' || id::TEXT
            )
        END AS slug_base
    FROM ciudad
),
ordenados AS (
    SELECT
        id,
        nombre,
        provincia,
        pais,
        slug_base,
        ROW_NUMBER() OVER (
            PARTITION BY slug_base
            ORDER BY
                CASE
                    WHEN nombre = 'Mar del Plata'
                     AND provincia = 'Buenos Aires'
                     AND pais = 'Argentina'
                        THEN 0
                    ELSE 1
                END,
                id
        ) AS posicion_slug
    FROM candidatos
)
UPDATE ciudad c
SET
    slug = CASE
        WHEN o.posicion_slug = 1 THEN LEFT(o.slug_base, 120)
        ELSE
            LEFT(
                o.slug_base,
                GREATEST(1, 120 - LENGTH('-' || c.id::TEXT))
            ) || '-' || c.id::TEXT
    END,
    orden = CASE
        WHEN c.nombre = 'Mar del Plata'
         AND c.provincia = 'Buenos Aires'
         AND c.pais = 'Argentina'
            THEN 1
        ELSE c.orden
    END,
    activa = CASE
        WHEN c.nombre = 'Mar del Plata'
         AND c.provincia = 'Buenos Aires'
         AND c.pais = 'Argentina'
            THEN true
        ELSE c.activa
    END,
    updated_at = CURRENT_TIMESTAMP
FROM ordenados o
WHERE o.id = c.id;

-- ============================================================
-- 3. VERIFICACIONES PREVIAS AL ENDURECIMIENTO
-- ------------------------------------------------------------
-- Antes de marcar slug como obligatorio y crear constraints,
-- abortamos con mensajes claros si el backfill dejo datos invalidos.
-- ============================================================

DO $$
DECLARE
    cantidad_slugs_vacios INTEGER;
    cantidad_slugs_invalidos INTEGER;
    cantidad_slugs_duplicados INTEGER;
    cantidad_ordenes_negativos INTEGER;
BEGIN
    SELECT COUNT(*)
    INTO cantidad_slugs_vacios
    FROM ciudad
    WHERE slug IS NULL
       OR BTRIM(slug) = '';

    IF cantidad_slugs_vacios > 0 THEN
        RAISE EXCEPTION
            'No se puede aplicar la migracion: existen ciudades sin slug valido. Cantidad: %',
            cantidad_slugs_vacios;
    END IF;

    SELECT COUNT(*)
    INTO cantidad_slugs_invalidos
    FROM ciudad
    WHERE slug !~ '^[a-z0-9]+(-[a-z0-9]+)*$';

    IF cantidad_slugs_invalidos > 0 THEN
        RAISE EXCEPTION
            'No se puede aplicar la migracion: existen ciudades con slug de formato invalido. Cantidad: %',
            cantidad_slugs_invalidos;
    END IF;

    SELECT COUNT(*)
    INTO cantidad_slugs_duplicados
    FROM (
        SELECT slug
        FROM ciudad
        GROUP BY slug
        HAVING COUNT(*) > 1
    ) duplicados;

    IF cantidad_slugs_duplicados > 0 THEN
        RAISE EXCEPTION
            'No se puede aplicar la migracion: existen slugs duplicados en ciudad. Cantidad de grupos duplicados: %',
            cantidad_slugs_duplicados;
    END IF;

    SELECT COUNT(*)
    INTO cantidad_ordenes_negativos
    FROM ciudad
    WHERE orden < 0;

    IF cantidad_ordenes_negativos > 0 THEN
        RAISE EXCEPTION
            'No se puede aplicar la migracion: existen ciudades con orden negativo. Cantidad: %',
            cantidad_ordenes_negativos;
    END IF;
END; $$;

-- ============================================================
-- 4. ENDURECIMIENTO DEL MODELO
-- ------------------------------------------------------------
-- Una vez completado el backfill, slug pasa a ser obligatorio
-- y se protegen unicidad, formato y orden.
-- ============================================================

ALTER TABLE ciudad
ALTER COLUMN slug SET NOT NULL;

ALTER TABLE ciudad
ADD CONSTRAINT uq_ciudad_slug
    UNIQUE (slug);

ALTER TABLE ciudad
ADD CONSTRAINT chk_ciudad_slug_no_vacio
    CHECK (BTRIM(slug) <> '');

ALTER TABLE ciudad
ADD CONSTRAINT chk_ciudad_slug_formato
    CHECK (slug ~ '^[a-z0-9]+(-[a-z0-9]+)*$');

ALTER TABLE ciudad
ADD CONSTRAINT chk_ciudad_orden_no_negativo
    CHECK (orden >= 0);

-- ============================================================
-- 5. INDICE PARA NAVEGACION PUBLICA
-- ------------------------------------------------------------
-- Ya existe idx_ciudad_activa sobre activa.
-- Este indice compuesto cubre el listado publico de ciudades
-- activas ordenadas por orden editorial y nombre.
-- ============================================================

CREATE INDEX idx_ciudad_activa_orden_nombre
ON ciudad (activa, orden, nombre);

COMMIT;
