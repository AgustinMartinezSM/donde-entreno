# Testing y cierre - Ciudades y navegación territorial en producción

Este documento registra el cierre del bloque backend de ciudades y navegación territorial en producción para DondeEntreno.

## Alcance del bloque

El objetivo fue preparar el backend y la base de datos para navegación pública por ciudad, comenzando por Mar del Plata y dejando la base lista para soportar múltiples ciudades a futuro.

El bloque incluye:

- Migración de tabla `ciudad`.
- Campo `slug` para rutas territoriales.
- Campo `orden` para orden editorial.
- Uso de `activa` para ciudades visibles.
- Endpoint `GET /api/ciudades`.
- Endpoint `GET /api/ciudades/{slug}`.
- Filtro `ciudadSlug` en `GET /api/actividades`.
- Compatibilidad con filtro existente `ciudadId`.
- Inclusión de `ciudadSlug` en `ActividadDTO`.

## Commits desplegados

```text
54546ba feat(database): prepare city navigation
bda3146 feat(backend): agregar navegacion territorial por ciudad
Migración aplicada

Se aplicó en Supabase:

database/scripts/10_prepare_city_navigation.sql

La migración fue revisada previamente y no contiene:

DROP TABLE
DROP COLUMN
TRUNCATE
DELETE FROM
CREATE DATABASE
ALTER DATABASE
Script no ejecutado en producción

No se ejecutó en producción:

database/scripts/11_test_city_navigation_queries.sql

El script 11 queda reservado para validaciones locales/controladas.

Cambios aplicados en Supabase

Se confirmó que la tabla ciudad tiene:

slug
orden
activa

Estado de columnas:

ciudad.slug: existe y es NOT NULL
ciudad.orden: existe y es NOT NULL
ciudad.activa: existe y es NOT NULL
Configuración de Mar del Plata

Se confirmó:

id: 1
nombre: Mar del Plata
provincia: Buenos Aires
pais: Argentina
slug: mar-del-plata
orden: 1
activa: true
Constraints e índice

Se confirmó la existencia de:

uq_ciudad_slug
chk_ciudad_slug_no_vacio
chk_ciudad_slug_formato
chk_ciudad_orden_no_negativo
idx_ciudad_activa_orden_nombre
Deploy backend

Render desplegó correctamente el backend actualizado:

Commit: bda3146
Estado: Deploy live
Endpoints probados en producción
GET /api/ciudades

Resultado:

OK
Incluye Mar del Plata
slug: mar-del-plata
orden: 1
activa: true
GET /api/ciudades/mar-del-plata

Resultado:

OK
Devuelve Mar del Plata por slug
GET /api/actividades?ciudadSlug=mar-del-plata

Resultado:

OK
Devuelve actividades de Mar del Plata
totalElementos: 6
ActividadDTO incluye ciudadSlug: mar-del-plata
GET /api/actividades?ciudadId=1

Resultado:

OK
Compatibilidad con ciudadId conservada
Regresión de endpoints públicos existentes

Se confirmó que siguen funcionando:

GET /api/actividades: OK
GET /api/filtros/opciones: OK
GET /api/actividades/{slug}: OK
POST /api/solicitudes-publicacion con JSON vacío: 400 Bad Request controlado
Observaciones

En PowerShell algunos textos con tilde se visualizan con caracteres incorrectos, por ejemplo:

AtlÃ©tico
FÃºtbol
ConstituciÃ³n
GÃ¼emes

Esto corresponde a visualización/encoding de la consola y no bloquea el funcionamiento de la API.

Logs

Se revisaron logs de Render después de las pruebas.

Resultado:

Sin errores 500 visibles.
Sin exceptions graves visibles.
Sin errores de columnas inexistentes.
Sin errores de relaciones inexistentes.
400 esperado para prueba de validación de solicitud pública con JSON vacío.
Resultado final

El bloque backend de ciudades y navegación territorial queda cerrado correctamente en producción.

Criterios cumplidos:

Migración 10 aplicada en Supabase.
ciudad.slug existe en producción.
ciudad.orden existe en producción.
ciudad.activa existe en producción.
Mar del Plata quedó configurada con slug = mar-del-plata, orden = 1, activa = true.
Backend nuevo desplegado en Render.
Endpoints de ciudades funcionan en producción.
ciudadSlug funciona en actividades.
ciudadId sigue funcionando.
Endpoints públicos existentes no se rompieron.
Logs Render OK.
Script 11 no ejecutado en producción.
No se ejecutó SQL destructivo.