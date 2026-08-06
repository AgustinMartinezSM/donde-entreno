# Plan técnico — Búsqueda insensible a tildes (Bloque 9, etapa backend)

**Estado: implementado.** Objetivo: que la búsqueda por texto del backend encuentre resultados sin importar tildes ("futbol" → "Fútbol", "natacion" → "Natación"), completando el bloque 9 (la etapa frontend, sugerencia de deporte por alias, ya está en `9ea8bfe`).

> **Correcciones tras leer el código real** (la fuente de verdad es el código, no este plan): ver la sección **"Diferencias plan ↔ implementación"** al final. En resumen: hay **dos** queries con rama de texto (no una), y una de ellas no la invoca ningún endpoint HTTP hoy.

## Por qué va con plan (riesgo medio-alto)

Toca la **query pública de búsqueda por texto** (en `ActividadRepository`), que hoy usa `LOWER(...) LIKE LOWER(...)` en JPQL. Un error en el registro de la función rompería **todas** las búsquedas, y **no había test automatizado que lo detecte**: los tests de service/controller mockean el repository, y no existía ningún IT que ejerza `GET /api/actividades?texto=...` contra PostgreSQL real. Por eso la implementación **incluye** ese IT nuevo como red de seguridad.

## Viabilidad ya verificada

`CREATE EXTENSION IF NOT EXISTS unaccent;` funciona en la base local; `unaccent('Fútbol')` → `Futbol`. La extensión es estándar de PostgreSQL (incluida en Supabase).

## Implementación aplicada (aditiva)

1. **Migración `16_prepare_busqueda_unaccent.sql`**: `CREATE EXTENSION IF NOT EXISTS unaccent;` (idempotente). Agregada a los órdenes de `testing.md` y `deploy.md`.
2. **Registrar `unaccent` en Hibernate** como función SQL, para poder llamarla desde JPQL sin pasar a query nativa:
   - Clase `UnaccentFunctionContributor implements FunctionContributor` en `config/` que registra el patrón `unaccent(?1)` devolviendo STRING.
   - En Hibernate 6.6.49 (el que trae Spring Boot 3.5.14) la firma es `SqmFunctionRegistry.registerPattern(String, String, BasicType)`; el `BasicType` se obtiene con `getTypeConfiguration().getBasicTypeForJavaType(String.class)`.
   - Alta vía `META-INF/services/org.hibernate.boot.model.FunctionContributor`.
3. **Queries**: en **las dos** ramas de texto (paginada y no paginada) se cambió cada `LOWER(campo) LIKE LOWER(:patron)` a `unaccent(LOWER(campo)) LIKE unaccent(LOWER(:patron))`. Las guardas de texto (`:texto IS NULL` en la no paginada, `:texto = ''` en la paginada) y el resto (filtros, orden) **no se tocan**.
4. **IT nuevo `BusquedaActividadIT`** (perfil `integration-local`, mismas guardas de host y limpieza por marcador que el resto): crea una actividad con la palabra **"Natación"** (con tilde) en el título y verifica ambas queries:
   - Paginada (endpoint público): `GET /api/actividades?texto=natacion` (sin tilde) la encuentra; `?texto=natación` (con tilde) también; un texto que no matchea no la trae.
   - No paginada (llamada directa al repository, ver diferencia 2): sin tilde la encuentra, un texto que no matchea no.
   Es la única prueba que valida el SQL real.

## Riesgos y mitigaciones

- **Regresión de toda la búsqueda**: mitigada por el IT nuevo (obligatorio en la misma entrega) + `mvnw verify -Pintegration-local`.
- **Función no registrada en algún entorno de test**: los tests unitarios mockean el repo (no la usan); los IT corren contra Postgres real con la extensión. No hay `@DataJpaTest` contra H2 que pudiera romperse.
- **Performance**: `unaccent()` sobre columnas sin índice funcional hace scan; con el volumen actual (decenas de actividades) es irrelevante. Si crece, se agrega un índice funcional `unaccent(lower(titulo))` en una migración posterior (pg_trgm queda para una etapa aparte del roadmap).
- **Rollback**: quitar el cambio de query y el FunctionContributor; la extensión puede quedar (no molesta). 100% aditivo.

## Alcance explícito

Solo tildes en la búsqueda por texto. **Fuera de este plan** (etapas separadas del bloque 9): tolerancia a errores de tipeo (pg_trgm / distancia de edición), autocompletado y sugerencias en vivo.

## Diferencias plan ↔ implementación

Al implementar contra el código real aparecieron dos diferencias respecto de la versión original de este plan. Se corrigió el plan (esta sección), se documentaron en `PROGRESS.md` y se implementó la solución correcta.

1. **Hay dos queries con rama de texto, no una.** `ActividadRepository` tiene `buscarActividadesPublicadasConFiltros` (no paginada) **y** `buscarActividadesPublicadasConFiltrosPaginado` (paginada), ambas con las mismas seis ramas `LIKE`. Se aplicó `unaccent` a las dos para que la búsqueda sea consistente sin importar el método. (Detalle menor: sus guardas de texto difieren —`:texto IS NULL` vs `:texto = ''`—; es pre-existente y se dejó igual.)

2. **La query no paginada no la invoca ningún endpoint HTTP hoy.** El endpoint público `GET /api/actividades` usa solo la paginada (`ActividadController` → `buscarActividadesConFiltrosPaginado`). El método de service que llama a la no paginada (`buscarActividadesConFiltros`) no tiene llamadores. Igual se corrige (consistencia y por si se expone a futuro), y el IT la cubre con una **llamada directa al repository**, ya que no hay ruta HTTP para ejercitarla.

3. **API de Hibernate confirmada por versión.** Se verificó sobre `hibernate-core 6.6.49.Final` que la firma disponible es `registerPattern(String, String, BasicType)` (no `BasicTypeReference`); el plan original decía "devolviendo STRING" sin fijar el tipo Java. Se resuelve con `getBasicTypeForJavaType(String.class)`.

Verificado a nivel SQL que la red de seguridad es real (roja sin el fix): `lower('Natación') LIKE lower('%natacion%')` → `false`; `unaccent(lower('Natación')) LIKE unaccent(lower('%natacion%'))` → `true`.
