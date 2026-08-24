-- ============================================================
-- Script 27 — Slug amigable para perfiles de publicador
-- ============================================================
-- Aditiva: columna nullable + backfill idempotente en el mismo script.
-- Plan: docs/plan-slug-publicadores.md
--
-- NULLABLE a propósito: la migración corre ANTES que el código y el
-- backend viejo sigue creando perfiles sin slug durante la ventana del
-- deploy; NOT NULL lo rompería. El backfill es re-ejecutable
-- (WHERE slug IS NULL): si un perfil se creó en la ventana, volver a
-- correr los dos UPDATE lo sanea.
--
-- unaccent se llama SIN calificar (está instalada en el schema public,
-- regla 3 del CLAUDE.md).
--
-- PRE:
--   SELECT COUNT(*) FROM information_schema.columns
--    WHERE table_name = 'perfil_publicador' AND column_name = 'slug';
--   -- esperado: 0
--   SELECT COUNT(*) FROM perfil_publicador;   -- anotar N para el POST

BEGIN;

ALTER TABLE perfil_publicador
    ADD COLUMN slug VARCHAR(150) NULL;

-- UNIQUE parcial: los NULL de la ventana de deploy no chocan entre sí.
CREATE UNIQUE INDEX uq_perfil_publicador_slug
    ON perfil_publicador (slug)
    WHERE slug IS NOT NULL;

COMMENT ON COLUMN perfil_publicador.slug IS
    'URL amigable del perfil (script 27). ESTABLE: renombrar el perfil no lo cambia. Generado por el backend al crear; backfill en este script.';

-- ============================================================
-- Backfill (idempotente: solo filas con slug NULL)
-- ============================================================

-- Paso 1: la base normalizada, para los perfiles cuya base queda libre
-- y no colisiona con otro candidato.
WITH candidatos AS (
    SELECT id,
           left(
               trim(both '-' from
                   regexp_replace(unaccent(lower(nombre)), '[^a-z0-9]+', '-', 'g')
               ),
               140
           ) AS base
      FROM perfil_publicador
     WHERE slug IS NULL
), unicos AS (
    SELECT c.id, c.base
      FROM candidatos c
     WHERE c.base <> ''
       AND NOT EXISTS (
           SELECT 1 FROM perfil_publicador q WHERE q.slug = c.base
       )
       AND (SELECT COUNT(*) FROM candidatos c2 WHERE c2.base = c.base) = 1
)
UPDATE perfil_publicador p
   SET slug = u.base
  FROM unicos u
 WHERE p.id = u.id;

-- Paso 2: lo que quedó (colisión o nombre sin caracteres válidos)
-- lleva sufijo -id, que es único por construcción.
UPDATE perfil_publicador
   SET slug = CASE
       WHEN trim(both '-' from regexp_replace(unaccent(lower(nombre)), '[^a-z0-9]+', '-', 'g')) = ''
           THEN 'publicador-' || id
       ELSE left(
                trim(both '-' from regexp_replace(unaccent(lower(nombre)), '[^a-z0-9]+', '-', 'g')),
                140
            ) || '-' || id
       END
 WHERE slug IS NULL;

COMMIT;

-- POST:
--   SELECT COUNT(*) FROM perfil_publicador WHERE slug IS NULL;  -- esperado: 0
--   SELECT COUNT(DISTINCT slug) = COUNT(*) FROM perfil_publicador; -- esperado: t
--   SELECT id, nombre, slug FROM perfil_publicador ORDER BY id;  -- revisar a ojo
--
-- Rollback (solo ANTES de desplegar el backend que lo usa):
--   DROP INDEX uq_perfil_publicador_slug;
--   ALTER TABLE perfil_publicador DROP COLUMN slug;
