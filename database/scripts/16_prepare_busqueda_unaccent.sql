-- ============================================================
-- 16 - BUSQUEDA INSENSIBLE A TILDES
-- ============================================================
--
-- Habilita la extension unaccent de PostgreSQL para que la
-- busqueda publica por texto encuentre resultados sin importar
-- las tildes ("futbol" -> "Futbol", "natacion" -> "Natacion").
-- Completa la etapa backend del Bloque 9.
--
-- unaccent es una extension estandar de PostgreSQL (incluida en
-- Supabase). La usa la query publica principal via una funcion
-- registrada en Hibernate (UnaccentFunctionContributor).
--
-- Migracion aditiva e idempotente: no crea ni modifica tablas ni
-- datos. Rollback: DROP EXTENSION unaccent; (no es necesario).
-- Ver docs/plan-busqueda-sin-tildes.md
-- ============================================================

CREATE EXTENSION IF NOT EXISTS unaccent;
