-- ============================================================
-- Script 25 — Fix del CHECK "algún campo propuesto" de
-- solicitud_cambio_actividad
-- ============================================================
-- El script 14 creó chk_solicitud_cambio_actividad_algun_campo
-- enumerando las 9 columnas originales; el 24 agregó los campos
-- nuevos (deporte, edades, enfoque, ubicación, horarios) sin
-- redefinirlo, así que una solicitud que propone SOLO campos nuevos
-- viola el constraint (lo detectó el IT de flujo completo: INSERT
-- rechazado con SQLState 23514).
--
-- Redefinición: mismo espíritu ("al menos un campo propuesto"),
-- sumando las columnas del 24. ubicacion_nombre/referencia/barrio no
-- entran solos a propósito: proponer ubicación exige dirección
-- (regla del backend), y el ancla acá es ubicacion_direccion.
--
-- PRE:
--   SELECT COUNT(*) FROM pg_constraint
--    WHERE conname = 'chk_solicitud_cambio_actividad_algun_campo';
--   -- esperado: 1

BEGIN;

ALTER TABLE solicitud_cambio_actividad
    DROP CONSTRAINT chk_solicitud_cambio_actividad_algun_campo;

ALTER TABLE solicitud_cambio_actividad
    ADD CONSTRAINT chk_solicitud_cambio_actividad_algun_campo
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
            OR deporte_id IS NOT NULL
            OR edad_minima IS NOT NULL
            OR edad_maxima IS NOT NULL
            OR enfoque IS NOT NULL
            OR ubicacion_direccion IS NOT NULL
            OR cambia_horarios = true
        );

COMMIT;

-- POST:
--   SELECT pg_get_constraintdef(oid) FROM pg_constraint
--    WHERE conname = 'chk_solicitud_cambio_actividad_algun_campo';
--   -- esperado: la definición incluye deporte_id y cambia_horarios
--
-- Rollback (solo si se revierte el 24 completo):
--   volver a la definición del script 14 (las 9 columnas originales).
