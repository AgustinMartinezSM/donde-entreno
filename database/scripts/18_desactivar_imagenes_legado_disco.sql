-- ============================================================
-- 18 - DESACTIVAR IMAGENES LEGADO DE DISCO LOCAL
-- ============================================================
--
-- El script 03 siembra filas de imagen con rutas de disco local
-- (prefijo "/uploads/"), de cuando los archivos se guardaban en el
-- filesystem del backend. Desde el bloque de imagenes con moderacion
-- las imagenes viven en Supabase Storage y se guardan con su URL
-- absoluta; la API no expone ningun recurso estatico y el contenedor
-- es efimero, asi que esas rutas no resuelven en ningun entorno.
--
-- Mientras estuvieron activas y aprobadas, el DTO publico las ofrecia
-- como imagenPrincipalUrl y los clientes pedian una imagen inexistente
-- en vez de caer a la ilustracion por deporte. El codigo ya las ignora
-- (ImagenMapper.esUrlPublicable); este script saca del medio el dato.
--
-- Desactivacion logica, no DELETE: es reversible y respeta el filtro
-- activa=true de todas las consultas publicas.
--
-- Idempotente: la segunda corrida no encuentra filas para actualizar.
-- ============================================================

-- ------------------------------------------------------------
-- PRE: que hay antes de tocar nada.
-- Esperado en produccion al 2026-08-08: 8 filas (3 de perfil, 5 de
-- actividad), todas activa=true y estado_moderacion='APROBADA'.
-- ------------------------------------------------------------
SELECT
    count(*) FILTER (WHERE activa) AS activas,
    count(*) FILTER (WHERE NOT activa) AS inactivas,
    count(*) AS total
FROM imagen
WHERE url LIKE '/uploads/%';

SELECT id, url, tipo_imagen, actividad_id, perfil_publicador_id, activa
FROM imagen
WHERE url LIKE '/uploads/%'
ORDER BY id;

-- ------------------------------------------------------------
-- CAMBIO
-- ------------------------------------------------------------
BEGIN;

UPDATE imagen
SET activa = false
WHERE url LIKE '/uploads/%'
  AND activa;

COMMIT;

-- ------------------------------------------------------------
-- POST: no debe quedar ninguna activa.
-- Esperado: activas = 0.
-- ------------------------------------------------------------
SELECT
    count(*) FILTER (WHERE activa) AS activas,
    count(*) AS total
FROM imagen
WHERE url LIKE '/uploads/%';

-- Control final: ninguna imagen visible en publico con URL no absoluta.
-- Esperado: 0 filas.
SELECT id, url, tipo_imagen
FROM imagen
WHERE activa
  AND estado_moderacion = 'APROBADA'
  AND url NOT LIKE 'http://%'
  AND url NOT LIKE 'https://%'
ORDER BY id;
