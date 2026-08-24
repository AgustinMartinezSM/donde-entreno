-- ============================================================
-- Script 33 — Fase 7 social: coordenadas en las solicitudes
-- ============================================================
-- Aditiva pura: cuatro columnas nullable. El código desplegado hoy
-- las ignora (migración ANTES que código).
-- Plan: docs/plan-fase7-mapa-cercania.md
--
-- Por qué acá y no solo en `ubicacion`: la tabla `ubicacion` YA tiene
-- latitud/longitud desde el schema original, pero el publicador carga
-- su sede a través de una SOLICITUD, y esas tablas no tenían dónde
-- guardar el punto. Por eso toda ubicación creada por el flujo real
-- nace con lat/lng en NULL (medido en producción: 4 de 9 ubicaciones
-- sin coordenadas, y son justo las de publicadores reales).
--
-- PRE:
--   SELECT COUNT(*) FROM information_schema.columns
--    WHERE table_name = 'solicitud_publicacion'
--      AND column_name IN ('latitud','longitud');
--   -- esperado: 0
--   SELECT COUNT(*) FROM information_schema.columns
--    WHERE table_name = 'solicitud_cambio_actividad'
--      AND column_name IN ('ubicacion_latitud','ubicacion_longitud');
--   -- esperado: 0
--   SELECT COUNT(*) FILTER (WHERE latitud IS NULL) AS sin_coords,
--          COUNT(*) AS total
--     FROM ubicacion WHERE activa = true AND deleted_at IS NULL;
--   -- anotá estos dos números: son el "antes" de la carga de coordenadas

BEGIN;

-- ============================================================
-- Alta de actividad: el publicador puede indicar el punto exacto.
-- ============================================================
ALTER TABLE solicitud_publicacion
    ADD COLUMN latitud NUMERIC(9,6) NULL,
    ADD COLUMN longitud NUMERIC(9,6) NULL;

-- Mismos CHECK de rango que ya tiene `ubicacion` (01_create_tables):
-- si se carga un valor, tiene que ser posible.
ALTER TABLE solicitud_publicacion
    ADD CONSTRAINT chk_solicitud_publicacion_latitud
        CHECK (latitud IS NULL OR (latitud >= -90 AND latitud <= 90));

ALTER TABLE solicitud_publicacion
    ADD CONSTRAINT chk_solicitud_publicacion_longitud
        CHECK (longitud IS NULL OR (longitud >= -180 AND longitud <= 180));

-- ============================================================
-- Cambio de actividad: mismo dato, mismo criterio.
-- ============================================================
ALTER TABLE solicitud_cambio_actividad
    ADD COLUMN ubicacion_latitud NUMERIC(9,6) NULL,
    ADD COLUMN ubicacion_longitud NUMERIC(9,6) NULL;

ALTER TABLE solicitud_cambio_actividad
    ADD CONSTRAINT chk_solicitud_cambio_latitud
        CHECK (ubicacion_latitud IS NULL
               OR (ubicacion_latitud >= -90 AND ubicacion_latitud <= 90));

ALTER TABLE solicitud_cambio_actividad
    ADD CONSTRAINT chk_solicitud_cambio_longitud
        CHECK (ubicacion_longitud IS NULL
               OR (ubicacion_longitud >= -180 AND ubicacion_longitud <= 180));

COMMIT;

-- SIN índice geográfico a propósito: con 7 actividades publicadas, un
-- índice espacial (o PostGIS) es ceremonia. La distancia se calcula en
-- el service sobre el resultado ya filtrado por ciudad/deporte.
-- Umbral anotado para volver a mirarlo: ~500 actividades.

-- POST:
--   SELECT column_name, data_type, is_nullable
--     FROM information_schema.columns
--    WHERE table_name = 'solicitud_publicacion'
--      AND column_name IN ('latitud','longitud');
--   -- esperado: numeric, YES (las dos)
--   SELECT column_name FROM information_schema.columns
--    WHERE table_name = 'solicitud_cambio_actividad'
--      AND column_name IN ('ubicacion_latitud','ubicacion_longitud');
--   -- esperado: 2 filas
--   SELECT conname FROM pg_constraint
--    WHERE conname LIKE 'chk_solicitud_%latitud'
--       OR conname LIKE 'chk_solicitud_%longitud';
--   -- esperado: 4 filas
--
-- Rollback (solo ANTES de desplegar el backend que lo usa):
--   ALTER TABLE solicitud_cambio_actividad
--     DROP CONSTRAINT chk_solicitud_cambio_latitud,
--     DROP CONSTRAINT chk_solicitud_cambio_longitud,
--     DROP COLUMN ubicacion_latitud, DROP COLUMN ubicacion_longitud;
--   ALTER TABLE solicitud_publicacion
--     DROP CONSTRAINT chk_solicitud_publicacion_latitud,
--     DROP CONSTRAINT chk_solicitud_publicacion_longitud,
--     DROP COLUMN latitud, DROP COLUMN longitud;
