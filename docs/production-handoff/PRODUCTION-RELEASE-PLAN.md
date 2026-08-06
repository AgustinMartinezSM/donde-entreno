# Plan controlado de release a producción

Este documento prepara el release de `integration/dondeentreno-lab`. No registra una ejecución productiva. Supabase, Render y Vercel deben permanecer sin cambios hasta que una persona responsable apruebe expresamente la ventana de release.

## Fuente y alcance

- Repositorio: `AgustinMartinezSM/donde-entreno`.
- Rama de revisión: `origin/integration/dondeentreno-lab`.
- Seleccionar y registrar un SHA inmutable antes de iniciar la ventana.
- Las migraciones incluidas son 14, 15, 16 y 17, en ese orden.
- Los controles SQL son [SUPABASE-PRECHECK.sql](SUPABASE-PRECHECK.sql) y [SUPABASE-POSTCHECK.sql](SUPABASE-POSTCHECK.sql).
- No se debe pushear `main`, mover tags, hacer merge ni iniciar despliegues como parte de la preparación documental.

## Hallazgo crítico: resolución de `unaccent`

`UnaccentFunctionContributor` registra el patrón SQL `unaccent(?1)` sin schema. `ActividadRepository` lo usa doce veces: seis campos en la consulta paginada y los mismos seis en la consulta no paginada.

`GET /api/actividades` siempre usa la consulta paginada. Aunque `texto` esté vacío, PostgreSQL debe resolver todas las funciones presentes al analizar la sentencia. Si `unaccent(text)` no se resuelve mediante el `search_path` de la conexión JDBC, el listado público completo puede responder 500, no solamente las búsquedas con texto.

El script 16 contiene solamente `CREATE EXTENSION IF NOT EXISTS unaccent;`. Esto no garantiza por sí solo la resolución:

- no fija el schema de instalación;
- no cambia el `search_path` del rol del backend;
- no mueve una extensión ya instalada;
- no califica la función en el SQL generado por Hibernate.

PostgreSQL elige el schema objetivo de una extensión usando la configuración de la extensión, el schema indicado en la sentencia o, en ausencia de ambos, el primer schema efectivo de creación. Supabase documenta que la mayoría de sus extensiones se instalan bajo `extensions`. Tener permiso de uso sobre ese schema no implica que una llamada sin schema lo encuentre.

Referencias oficiales:

- [PostgreSQL: schema objetivo de una extensión](https://www.postgresql.org/docs/current/extend-extensions.html)
- [Supabase: extensiones y schema `extensions`](https://supabase.com/docs/guides/database/extensions)

Gate obligatorio: el POSTCHECK debe mostrar una firma no nula en `to_regprocedure('unaccent(text)')` y la llamada real final debe devolver `true` en ambos casos. Si no sucede, el release queda en **NO-GO**. Antes de desplegar el backend se necesita una decisión técnica separada y revisada: ajustar de forma segura el `search_path` del rol JDBC o cambiar la aplicación/migración para usar el schema efectivo. Este plan no ejecuta ninguna de esas opciones.

## Auditoría de migraciones

### 14 — `database/scripts/14_create_solicitud_cambio_actividad.sql`

Ruta exacta: `database/scripts/14_create_solicitud_cambio_actividad.sql`.

Objetos:

- tabla `solicitud_cambio_actividad`;
- secuencia implícita `solicitud_cambio_actividad_id_seq` por `BIGSERIAL`;
- clave primaria sobre `id`;
- cuatro claves foráneas hacia `actividad(id)`, `perfil_publicador(id)` y `usuario(id)` —dos relaciones distintas con usuario—, todas con `ON DELETE RESTRICT`;
- siete CHECKs de estado, rechazo, presencia de algún cambio, nivel, modalidad, precio y consistencia de resolución;
- índice único parcial `uq_solicitud_cambio_actividad_abierta` para impedir más de una solicitud abierta no eliminada por actividad;
- índices simples por `perfil_publicador_id`, `estado` y `actividad_id`.

Columnas y defaults:

- identidad: `id BIGSERIAL`;
- ownership: `actividad_id`, `perfil_publicador_id`, `usuario_id`, todos `BIGINT NOT NULL`;
- flujo: `estado VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE'`;
- propuestas anulables: `titulo VARCHAR(150)`, `descripcion TEXT`, `precio_referencia NUMERIC(10,2)`, `mostrar_precio BOOLEAN`, contactos, `nivel` y `modalidad`;
- resolución: `motivo_rechazo`, `resuelto_por_usuario_id`, `resuelto_at`;
- auditoría: `created_at` y `updated_at` con `CURRENT_TIMESTAMP`, más `deleted_at` anulable.

Idempotencia y segunda ejecución: **no es idempotente**. Una segunda ejecución falla en la creación de la tabla. Si existe una tabla parcial o distinta con el mismo nombre, no se debe reintentar; se debe comparar metadata y decidir una reparación explícita.

Dependencias: las tablas `actividad`, `perfil_publicador` y `usuario`, sus columnas `id` compatibles con `BIGINT`, permisos de schema y ausencia de colisiones de nombres.

Riesgos:

- pérdida de datos: baja; es aditiva y no toca filas existentes;
- bloqueo: bajo en una base normal, porque la tabla nace vacía, aunque la creación de claves foráneas toma locks breves sobre las tablas referenciadas;
- operación: medio si ya existe un objeto parcial, porque el script no puede reconciliarlo.

PRE necesario: confirmar schema efectivo, dependencias, tipos de IDs, ausencia de tabla/secuencia/constraints/índices homónimos y ausencia de una instalación parcial. Todo está consultado por el PRECHECK.

POST necesario: confirmar tabla, secuencia, 20 columnas, defaults, 12 constraints, cuatro índices explícitos, validación de constraints, cero referencias huérfanas y cero actividades con más de una solicitud abierta. Todo está consultado por el POSTCHECK.

Rollback razonable: el rollback preferido es volver al backend anterior y conservar la tabla aditiva. Si fuera imprescindible retirarla, detener escritores, comprobar que esté vacía, respaldar cualquier fila y retirar la tabla en una ventana aprobada. No retirar si ya contiene historial.

### 15 — `database/scripts/15_prepare_imagen_moderacion.sql`

Ruta exacta: `database/scripts/15_prepare_imagen_moderacion.sql`.

Objetos modificados:

- tabla `imagen`;
- columna `estado_moderacion VARCHAR(30) NOT NULL DEFAULT 'APROBADA'`;
- columna `motivo_rechazo TEXT`;
- CHECK `chk_imagen_estado_moderacion`, que admite `PENDIENTE`, `APROBADA` y `RECHAZADA`;
- índice `idx_imagen_estado_moderacion`.

Idempotencia y segunda ejecución: es idempotente solamente frente a una primera ejecución correcta. `IF NOT EXISTS` evita repetir columnas e índice, y el bloque procedural evita repetir el CHECK. Sin embargo:

- no corrige una columna preexistente con tipo, nulabilidad o default incorrectos;
- no corrige un índice homónimo con otra definición;
- la búsqueda del nombre del CHECK no limita tabla ni schema; un constraint homónimo en otro objeto podría hacer que el CHECK de `imagen` se omita.

Dependencias: tabla `imagen`, permisos suficientes y que las columnas/objetos homónimos, si existen, tengan exactamente la definición esperada.

Riesgos:

- pérdida de datos: baja; no elimina filas. Las imágenes existentes pasan a observar el default `APROBADA`;
- bloqueo: medio y proporcional al tamaño de `imagen`. Las operaciones sobre tabla toman un lock fuerte; la validación del CHECK y la construcción no concurrente del índice pueden escanear la tabla;
- compatibilidad: baja con versiones actuales de PostgreSQL, donde agregar una columna con default constante suele evitar una reescritura física, pero el lock igualmente existe.

PRE necesario: contar imágenes, revisar columnas existentes, cualquier constraint homónimo global, índice homónimo y volumen/actividad de la tabla. El total previo de imágenes debe guardarse para compararlo.

POST necesario: confirmar tipos, `NOT NULL`, default `APROBADA`, CHECK validado, índice correcto, cero estados nulos o fuera de dominio y que todas las filas preexistentes tengan estado esperado.

Rollback razonable: preferir dejar columnas, CHECK e índice al volver al backend anterior; son aditivos. Retirarlos perdería metadata de moderación. Solo considerar esa retirada con el backend detenido, respaldo previo y verificación de que no haya estados distintos de `APROBADA` ni motivos cargados.

### 16 — `database/scripts/16_prepare_busqueda_unaccent.sql`

Ruta exacta: `database/scripts/16_prepare_busqueda_unaccent.sql`.

Objetos: instala la extensión de base `unaccent`, con sus funciones y diccionario en el schema efectivo elegido por PostgreSQL/Supabase. No crea tablas, columnas, constraints ni índices de negocio y no tiene defaults.

Idempotencia y segunda ejecución: `IF NOT EXISTS` hace que una segunda ejecución sea un no-op si la extensión ya está instalada. No cambia su schema, versión, privilegios ni visibilidad por `search_path`.

Dependencias: extensión disponible en el servidor, permisos para habilitarla, schema objetivo seguro y accesible, y resolución de `unaccent(text)` bajo el `search_path` real del rol JDBC de Render.

Riesgos:

- pérdida de datos: nula;
- bloqueo: bajo, limitado principalmente a catálogos;
- disponibilidad: **alta** si la función queda fuera del `search_path`, porque puede romper todo `GET /api/actividades`;
- performance: las expresiones aplican `unaccent(lower(...))` sobre seis campos sin índice funcional, por lo que la rama textual puede requerir scans. Es aceptable solo mientras el volumen sea pequeño y debe monitorearse.

PRE necesario: consultar extensión disponible/instalada, schema de extensión, firmas `unaccent`, `search_path`, `current_schema`, resolución sin schema y candidatos explícitos `public`/`extensions`.

POST necesario: comprobar extensión, schema, firmas, resolución mediante `to_regprocedure('unaccent(text)')` y ejecutar la llamada real sin schema del POSTCHECK. Además, probar HTTP con y sin texto antes de desplegar el frontend.

Rollback razonable: dejar la extensión instalada; es el rollback menos riesgoso. Retirarla exige primero auditar dependencias de toda la base, no solo DondeEntreno. Si el backend falla, volver al backend anterior y mantener producción detenida en NO-GO hasta resolver schema/search path.

### 17 — `database/scripts/17_create_seguimiento_publicador.sql`

Ruta exacta: `database/scripts/17_create_seguimiento_publicador.sql`.

Objetos:

- tabla `seguimiento_publicador`;
- secuencia implícita `seguimiento_publicador_id_seq`;
- clave primaria sobre `id`;
- claves foráneas desde `usuario_id` y `perfil_publicador_id`, con acción referencial predeterminada `NO ACTION`;
- constraint único `uq_seguimiento_usuario_perfil`;
- índices simples `idx_seguimiento_usuario` e `idx_seguimiento_perfil`.

Columnas y defaults: `id BIGSERIAL`, `usuario_id BIGINT NOT NULL`, `perfil_publicador_id BIGINT NOT NULL` y `created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()`.

Idempotencia y segunda ejecución: una segunda ejecución después de una primera correcta es segura por los `IF NOT EXISTS`. La idempotencia es incompleta si ya existe una tabla homónima defectuosa: la sentencia de tabla no agrega columnas, FKs ni UNIQUE faltantes, y los índices posteriores pueden fallar si faltan columnas. Tampoco valida que índices homónimos tengan la definición correcta.

Dependencias: tablas `usuario` y `perfil_publicador`, IDs compatibles, permisos y ausencia de objetos parciales incompatibles.

Riesgos:

- pérdida de datos: baja; es aditiva;
- bloqueo: bajo, con locks breves sobre tablas referenciadas al crear las FKs;
- operación: medio si existe una tabla parcial o un índice homónimo incorrecto.

PRE necesario: confirmar dependencias, schema, colisiones de tabla/secuencia/constraints/índices y posibles instalaciones parciales.

POST necesario: confirmar cuatro columnas, defaults, PK, dos FKs, UNIQUE, índices, cero huérfanos y cero pares duplicados.

Rollback razonable: volver al backend anterior y conservar la tabla. Si fuera imprescindible retirarla, detener escrituras, respaldar filas, confirmar el impacto funcional y retirarla solo en una ventana aprobada.

## Variables de entorno

Los valores reales se cargan únicamente en los paneles autorizados; no deben copiarse a este documento ni a Git.

| Plataforma | Variable | Obligatoriedad | Cuándo se lee | Valor descriptivo | Si falta o está mal | Activación del cambio |
|---|---|---|---|---|---|---|
| Vercel | `NEXT_PUBLIC_API_URL` | Obligatoria | Durante el build de Next.js y luego queda incorporada en el bundle cliente | URL HTTPS pública del backend Render, sin path `/api` y sin slash final preferentemente | El módulo `apiConfig.ts` lanza error si falta; si apunta mal, login y todas las llamadas API fallan o usan el origen equivocado | Nuevo build/deploy del frontend |
| Vercel | `NEXT_PUBLIC_SITE_URL` | Operativamente obligatoria en producción, aunque el código tiene fallback local | Durante build/render de metadata, sitemap, robots y URLs canónicas | URL HTTPS canónica del frontend Vercel | Si falta usa `http://localhost:3000`; si está mal publica metadata, sitemap, robots y canónicas incorrectas | Nuevo build/deploy del frontend |
| Render | `APP_CORS_ALLOWED_ORIGINS` | Obligatoria en producción | Al arrancar Spring, al construir `CorsConfigurationSource` | Lista separada por comas de orígenes HTTPS exactos autorizados, incluyendo el frontend productivo | Si falta prevalece el default local; el navegador bloquea el frontend productivo. Si es demasiado amplia aumenta exposición CORS | Reinicio o redeploy del backend; no exige recompilar el JAR |
| Render | `SPRING_PROFILES_ACTIVE` | Operativamente obligatoria | En el bootstrap de Spring | `prod` para identificar ejecución productiva | Si falta queda activo `local` por `application.properties`. Hoy no existe un `application-prod.properties` empaquetado, por lo que las demás variables siguen siendo necesarias, pero el perfil incorrecto dificulta controles y futuras configuraciones | Reinicio o redeploy del backend; no exige recompilar el JAR |

Antes del backend también deben preservarse las variables de datasource y autenticación ya existentes. Esta preparación no cambia ni revela sus valores.

## Secuencia obligatoria de release

### 1. Ejecutar PRECHECK

- Abrir una sesión SQL autorizada sobre el proyecto correcto.
- Confirmar visualmente proyecto, host y base antes de continuar.
- Ejecutar completo `docs/production-handoff/SUPABASE-PRECHECK.sql`.
- Guardar resultados con timestamp y operador, sin credenciales.

### 2. Evaluar resultados

GO solamente si:

- base, usuario y schema son los esperados;
- las tablas base están en el mismo schema efectivo donde actuarán scripts no calificados;
- `usuario.id`, `perfil_publicador.id`, `actividad.id` e `imagen.id` existen con tipos compatibles;
- no hay instalaciones parciales ni objetos homónimos incompatibles;
- el total de imágenes quedó registrado;
- `unaccent` está disponible y se conoce su schema real;
- hay una ventana de baja escritura y rollback aprobado.

NO-GO ante cualquier discrepancia, resultado ambiguo, objeto parcial o schema incorrecto. No “corregir en vivo” fuera de un cambio revisado.

### 3. Ejecutar migraciones en orden

Ejecutar una por una, esperando resultado exitoso y registrando hora/duración:

1. `database/scripts/14_create_solicitud_cambio_actividad.sql`
2. `database/scripts/15_prepare_imagen_moderacion.sql`
3. `database/scripts/16_prepare_busqueda_unaccent.sql`
4. `database/scripts/17_create_seguimiento_publicador.sql`

Detenerse ante el primer error. No reintentar el script 14 sin inspección, porque no es idempotente. Para 15 y 17, `IF NOT EXISTS` tampoco demuestra que un objeto preexistente sea correcto.

### 4. Ejecutar POSTCHECK

- Ejecutar completo `docs/production-handoff/SUPABASE-POSTCHECK.sql`.
- Comparar el total de imágenes con PRECHECK.
- Exigir cero filas inválidas, huérfanas o duplicadas.
- Exigir todos los objetos, defaults, constraints e índices esperados.
- Exigir que `to_regprocedure('unaccent(text)')` no sea nulo.
- Exigir que la llamada real final de `unaccent` devuelva `true` dos veces.

Si falla cualquier punto: **NO-GO para backend**.

### 5. Desplegar backend

- Confirmar `APP_CORS_ALLOWED_ORIGINS` y `SPRING_PROFILES_ACTIVE=prod` sin modificar otros secretos.
- Desplegar el SHA aprobado de la rama de release.
- No desplegar el frontend todavía.

### 6. Probar `/api/health`

- Esperar HTTP 200.
- Verificar que la respuesta identifique el servicio esperado.
- Revisar logs de arranque por errores de datasource, JPA, CORS o funciones SQL.

### 7. Probar `/api/actividades`

- Probar sin parámetros; debe responder HTTP 200.
- Probar `?texto=natacion`; debe responder HTTP 200 y encontrar resultados con tilde cuando existan.
- Probar filtros válidos e inválidos; los inválidos deben responder 400.
- Si aparece un error de resolución de `unaccent`, rollback inmediato del backend y NO-GO.

### 8. Probar login y endpoints principales

- Login de un usuario de prueba autorizado.
- Lectura pública de detalle, horarios e imágenes existentes.
- Endpoints autenticados de publicador y admin según roles de prueba.
- Solicitudes de cambio, métricas, seguimiento y feed mediante operaciones de prueba reversibles.
- Confirmar CORS desde el origen productivo.

### 9. Desplegar frontend

- Confirmar `NEXT_PUBLIC_API_URL` y `NEXT_PUBLIC_SITE_URL`.
- Iniciar nuevo build sobre el mismo SHA aprobado.
- Mantener disponible el deployment frontend anterior para rollback.

### 10. Pruebas end-to-end

- Home y listado de actividades.
- Búsqueda con y sin tildes.
- Filtros, paginación y orden.
- Login, sesión y cierre de sesión.
- Favoritos y preferencias locales.
- Perfil y actividades del publicador.
- Solicitudes de cambio.
- Seguimiento y feed.
- Robots, sitemap, canónicas y metadata con dominio productivo.
- Navegación en escritorio y móvil sin errores de consola ni CORS.

### 11. Rollback por etapa

Base de datos:

- Ante error durante migraciones, detener la secuencia y evaluar exactamente qué sentencias quedaron confirmadas.
- Preferir conservar objetos aditivos y volver la aplicación; retirar tablas/columnas puede perder datos.
- Nunca retirar `unaccent` sin auditar dependencias de toda la base.

Backend:

- Volver al último deployment conocido en verde.
- Confirmar `/api/health` y endpoints anteriores.
- Las tablas/columnas aditivas 14, 15 y 17 pueden quedar sin uso por el backend anterior.

Frontend:

- Volver al deployment anterior de Vercel.
- Verificar que sus variables sigan apuntando al backend compatible.

Variables:

- Restaurar los valores previamente registrados y reiniciar/reconstruir la plataforma correspondiente.
- No improvisar orígenes CORS amplios como medida temporal.

## Decisión final GO / NO-GO

GO requiere simultáneamente:

- PRECHECK revisado y firmado;
- cuatro migraciones exitosas en orden;
- POSTCHECK completamente verde;
- resolución real de `unaccent` sin schema;
- variables verificadas;
- backend saludable y listado de actividades operativo antes del frontend;
- smoke tests y E2E aprobados;
- rollback disponible por plataforma.

Es NO-GO si falta cualquiera de esos puntos. En particular, no desplegar el backend mientras el `search_path` del rol JDBC no resuelva `unaccent(text)` sin schema.
