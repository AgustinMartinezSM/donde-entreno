# Base de datos - DondeEntreno

Este documento explica cómo preparar, validar y entender la base de datos local del proyecto DondeEntreno.

La base de datos está desarrollada en PostgreSQL y se administra mediante scripts SQL versionados dentro del repositorio.

## Tecnología utilizada

* PostgreSQL.
* SQL.
* DBeaver o pgAdmin como herramienta visual opcional.
* Scripts SQL manuales dentro de `database/scripts/`.

El backend Spring Boot usa `spring.jpa.hibernate.ddl-auto=none`, por lo que Hibernate no crea ni modifica tablas automáticamente.

## Ubicación de los scripts

Los scripts de base de datos se encuentran en:

```text
database/scripts/
```

Archivos disponibles:

```text
01_create_tables.sql
02_seed_data.sql
03_seed_test_data.sql
04_test_queries.sql
05_create_solicitud_publicacion.sql
06_test_solicitud_publicacion_queries.sql
07_prepare_auth_security.sql
08_test_auth_security_queries.sql
09_test_approval_traceability_queries.sql
10_prepare_city_navigation.sql
11_test_city_navigation_queries.sql
12_prepare_publisher_accounts.sql
13_test_publisher_accounts_queries.sql
```

## Orden de uso local

Para preparar una base local desde cero, usar este orden:

```text
1. 01_create_tables.sql
2. 02_seed_data.sql
3. 03_seed_test_data.sql
4. 05_create_solicitud_publicacion.sql
5. 07_prepare_auth_security.sql
6. 10_prepare_city_navigation.sql
7. 12_prepare_publisher_accounts.sql
```

Los scripts de validación se ejecutan después de tener la estructura y los datos necesarios:

```text
8. 04_test_queries.sql
9. 06_test_solicitud_publicacion_queries.sql
10. 08_test_auth_security_queries.sql
11. 09_test_approval_traceability_queries.sql
12. 11_test_city_navigation_queries.sql
13. 13_test_publisher_accounts_queries.sql
```

`04_test_queries.sql` contiene consultas de control del MVP inicial.

`06_test_solicitud_publicacion_queries.sql` valida localmente la migración `05`. Comienza con `BEGIN`, termina con `ROLLBACK` y no deja datos de prueba persistidos.

`08_test_auth_security_queries.sql` valida localmente la migración `07`. Comienza con `BEGIN`, termina con `ROLLBACK` y no conserva usuarios temporales.

`09_test_approval_traceability_queries.sql` valida localmente que la trazabilidad entre una solicitud aprobada y la actividad creada pueda representarse con el modelo actual. Comienza con `BEGIN`, termina con `ROLLBACK` y no persiste datos.

`11_test_city_navigation_queries.sql` valida localmente la migración `10`. Comienza con `BEGIN`, termina con `ROLLBACK` y no persiste ciudades temporales.

`13_test_publisher_accounts_queries.sql` valida localmente la migracion `12`. Comienza con `BEGIN`, termina con `ROLLBACK` y no persiste usuarios, perfiles ni solicitudes temporales.

## Descripción de cada script

### 01_create_tables.sql

Crea la estructura principal inicial de la base de datos.

Incluye estas tablas reales:

* `rol`
* `ciudad`
* `categoria_deportiva`
* `usuario`
* `barrio`
* `deporte`
* `perfil_publicador`
* `ubicacion`
* `actividad`
* `horario_actividad`
* `imagen`

También define relaciones, claves primarias, claves foráneas, restricciones e índices necesarios para el funcionamiento inicial del proyecto.

No existen tablas independientes llamadas `club`, `profesor`, `horario` ni `estado_publicacion`. Esos conceptos se representan así:

* Clubes, gimnasios, profesores e instituciones se modelan con `perfil_publicador.tipo_publicador`.
* Los horarios de actividades se modelan con `horario_actividad`.
* El estado de publicación de una actividad se guarda en `actividad.estado_publicacion`.

### 02_seed_data.sql

Carga datos iniciales necesarios para que la aplicación pueda funcionar.

Incluye datos base como:

* roles;
* ciudad inicial;
* barrios;
* categorías deportivas;
* deportes.

### 03_seed_test_data.sql

Carga datos de prueba para visualizar y testear el MVP.

Incluye ejemplos de:

* usuarios;
* perfiles publicadores de distintos tipos, como clubes, profesores y gimnasios;
* ubicaciones;
* actividades;
* horarios de actividades;
* imágenes.

Estos datos permiten probar el backend y el frontend sin tener que cargar información manualmente.

Este script es exclusivamente local. No debe ejecutarse en producción. Sus usuarios ficticios se crean inactivos y no verificados, y sus `password_hash` son placeholders que no sirven para login real.

### 04_test_queries.sql

Contiene consultas de control para verificar que la base de datos inicial esté funcionando correctamente.

Sirve para revisar:

* actividades cargadas;
* relaciones entre tablas;
* actividades publicadas;
* horarios asociados;
* imágenes asociadas;
* datos usados por el frontend.

### 05_create_solicitud_publicacion.sql

Es una migración aditiva y no destructiva.

Crea estas tablas:

* `solicitud_publicacion`
* `solicitud_publicacion_horario`

No altera tablas existentes, no borra datos y no inserta datos de prueba.

### 06_test_solicitud_publicacion_queries.sql

Es un script de validación local para ejecutar únicamente después de aplicar correctamente `05_create_solicitud_publicacion.sql`.

Valida:

* existencia de tablas, columnas, claves foráneas, restricciones e índices;
* inserción de una solicitud válida;
* inserción de horarios válidos;
* rechazo de datos inválidos mediante constraints;
* borrado en cascada de horarios al borrar físicamente una solicitud temporal;
* ausencia de datos persistidos al finalizar.

El script comienza con `BEGIN`, termina con `ROLLBACK` y no deja datos de prueba persistidos.

### 07_prepare_auth_security.sql

Es una migración aditiva y no destructiva para preparar reglas mínimas de Auth y Seguridad.

Realiza estos cambios:

* amplía la constraint `chk_rol_nombre` para permitir `SUPER_ADMIN`, `ADMIN`, `PUBLICADOR` y `USUARIO`;
* inserta el rol `USUARIO` de forma idempotente;
* agrega constraints para evitar `usuario.nombre`, `usuario.email` y `usuario.password_hash` vacíos o compuestos solo por espacios;
* crea el índice único funcional `idx_usuario_email_normalizado_unico` sobre `LOWER(BTRIM(email))`.

No crea administradores, no modifica hashes existentes, no agrega tokens, no agrega secretos y no cambia relaciones existentes.

### 08_test_auth_security_queries.sql

Es un script de validación local para ejecutar únicamente después de aplicar correctamente `07_prepare_auth_security.sql`.

Valida:

* existencia del rol `USUARIO`;
* valores permitidos por `chk_rol_nombre`;
* existencia de las constraints nuevas de `usuario`;
* existencia y carácter único de `idx_usuario_email_normalizado_unico`;
* inserción temporal de un usuario válido;
* rechazo de usuarios con nombre, email o `password_hash` vacíos;
* rechazo de roles no permitidos;
* rechazo de emails duplicados por mayúsculas, minúsculas o espacios externos;
* confirmación de que la unicidad también contempla usuarios con `deleted_at`.

El script comienza con `BEGIN`, termina con `ROLLBACK` y no conserva usuarios temporales.

### 09_test_approval_traceability_queries.sql

Es un script de validación local para revisar el flujo de aprobación de solicitudes sin crear una migración estructural nueva.

Valida que el modelo actual permite representar:

* una solicitud aprobada;
* la actividad creada a partir de esa solicitud;
* el usuario administrador que revisó;
* la fecha de finalización de la revisión;
* los horarios copiados a `horario_actividad`.

También incluye pruebas negativas para confirmar que:

* una misma `actividad_generada_id` no pueda vincularse a dos solicitudes;
* `RECHAZADA` requiera `motivo_rechazo`;
* `APROBADA` no permita `motivo_rechazo`;
* `revision_finalizada_at` no pueda ser anterior a `revision_iniciada_at`.

El script comienza con `BEGIN`, termina con `ROLLBACK`, no contiene confirmación de transacción y no deja datos persistidos.

### 10_prepare_city_navigation.sql

Es una migración aditiva y no destructiva para preparar navegación territorial por ciudad.

Agrega a `ciudad`:

* `slug`, usado para rutas públicas como `/ciudades/mar-del-plata`;
* `orden`, usado para ordenar ciudades de forma editorial.

También completa los datos existentes de forma segura. Mar del Plata queda como ciudad inicial/default con:

* `slug = 'mar-del-plata'`;
* `orden = 1`;
* `activa = true`.

Para cualquier otra ciudad ya existente en una base local o futura, el script genera un slug seguro sin usar extensiones de PostgreSQL como `unaccent`. Si hay colisiones, agrega un sufijo con el `id`.

La migración agrega constraints para asegurar slug obligatorio, único, no vacío, con formato válido y orden no negativo. También agrega un índice compuesto para listar ciudades activas ordenadas por `orden` y `nombre`.

No carga ciudades futuras persistentes como Miramar, Balcarce, Necochea, Otamendi o Tandil. Esas ciudades podrán cargarse más adelante cuando exista contenido real o un flujo administrativo específico.

### 11_test_city_navigation_queries.sql

Es un script de validación local para ejecutar después de aplicar correctamente `10_prepare_city_navigation.sql`.

Valida:

* existencia de `ciudad.slug` y `ciudad.orden`;
* `slug` obligatorio;
* constraint única `uq_ciudad_slug`;
* constraints de formato de slug y orden no negativo;
* índice para ciudades activas ordenadas;
* datos esperados de Mar del Plata;
* ausencia de slugs vacíos, duplicados o inválidos;
* ausencia de orden negativo.

Incluye consultas de diagnóstico para ciudades activas, ciudades con y sin actividades publicadas, actividades por ciudad, barrios por ciudad, búsqueda por slug y navegación territorial hacia actividades de Mar del Plata.

También crea ciudades futuras temporales dentro de la transacción para probar orden y slug. El script comienza con `BEGIN`, termina con `ROLLBACK` y no deja datos persistidos.

### 12_prepare_publisher_accounts.sql

Es una migracion aditiva y no destructiva para preparar login unificado, registro y futuro panel publicador V1.

Realiza estos cambios:

* agrega `usuario.telefono_normalizado` y `usuario.telefono_verificado`;
* normaliza telefonos existentes de `usuario.telefono` sin modificar el valor original;
* agrega constraint de formato e indice no unico para `usuario.telefono_normalizado`;
* agrega `perfil_publicador.estado`;
* agrega `perfil_publicador.ciudad_principal_id`;
* agrega `perfil_publicador.whatsapp_normalizado` y `perfil_publicador.telefono_contacto_normalizado`;
* completa `perfil_publicador.estado` para registros existentes segun `activo`, `verificado` y `deleted_at`;
* normaliza contactos existentes de `perfil_publicador` sin modificar los valores visibles;
* agrega constraints e indices no unicos para los contactos normalizados;
* agrega `solicitud_publicacion.usuario_id`;
* agrega `solicitud_publicacion.perfil_publicador_id`;
* agrega claves foraneas e indices para listar solicitudes por usuario o perfil publicador;
* mantiene `usuario_id` y `perfil_publicador_id` como campos opcionales para compatibilidad historica.

No modifica roles existentes, no cambia los valores de `perfil_publicador.tipo_publicador`, no crea usuarios reales, no crea administradores, no toca `actividad` ni horarios y no borra datos.

### 13_test_publisher_accounts_queries.sql

Es un script de validacion local para ejecutar despues de aplicar correctamente `12_prepare_publisher_accounts.sql`.

Valida:

* existencia de columnas nuevas en `usuario`, `perfil_publicador` y `solicitud_publicacion`;
* existencia de constraints nuevas;
* existencia de indices nuevos;
* creacion temporal de un usuario comun;
* creacion temporal de un usuario `PUBLICADOR`;
* creacion temporal de un perfil publicador asociado al usuario publicador;
* creacion temporal de una solicitud asociada a `usuario_id` y `perfil_publicador_id`;
* compatibilidad de una solicitud historica con `usuario_id` y `perfil_publicador_id` en `NULL`;
* rechazo de telefonos o WhatsApp normalizados con letras;
* rechazo de estados invalidos de perfil publicador;
* rechazo de solicitud con perfil informado y usuario ausente;
* rechazo de referencias inexistentes a usuario o perfil.

Incluye consultas de diagnostico para usuarios por rol, publicadores activos, perfiles por estado, perfiles sin ciudad principal, solicitudes historicas, solicitudes asociadas a usuarios, solicitudes asociadas a perfiles y contactos normalizados.

El script comienza con `BEGIN`, termina con `ROLLBACK` y no deja datos temporales persistidos.

## Modelo actual de tablas

La base de datos local actual contempla estas tablas:

* `usuario`
* `rol`
* `perfil_publicador`
* `ciudad`
* `barrio`
* `categoria_deportiva`
* `deporte`
* `ubicacion`
* `actividad`
* `horario_actividad`
* `imagen`
* `solicitud_publicacion`
* `solicitud_publicacion_horario`

## Relaciones principales

Relaciones del modelo inicial:

* `usuario` pertenece a `rol`.
* `perfil_publicador` pertenece a `usuario`.
* `barrio` pertenece a `ciudad`.
* `deporte` pertenece a `categoria_deportiva`.
* `ubicacion` pertenece a `perfil_publicador`, `ciudad` y `barrio`.
* `actividad` pertenece a `perfil_publicador`, `deporte` y `ubicacion`.
* `horario_actividad` pertenece a `actividad`.
* `imagen` puede pertenecer a `perfil_publicador` o a `actividad`, pero no a ambos al mismo tiempo.

Relaciones del modelo de solicitudes:

* `solicitud_publicacion` puede relacionarse opcionalmente con `deporte`.
* `solicitud_publicacion` puede relacionarse opcionalmente con `ciudad`.
* `solicitud_publicacion` puede relacionarse opcionalmente con `barrio`.
* `solicitud_publicacion` puede relacionarse opcionalmente con `usuario` como usuario solicitante.
* `solicitud_publicacion` puede relacionarse opcionalmente con `perfil_publicador` como perfil solicitante.
* `solicitud_publicacion` puede relacionarse opcionalmente con `usuario` como revisor.
* `solicitud_publicacion` puede relacionarse opcionalmente con `actividad` como actividad finalmente generada.
* `solicitud_publicacion` tiene muchos registros en `solicitud_publicacion_horario`.

Las relaciones opcionales desde `solicitud_publicacion` hacia `deporte`, `ciudad`, `barrio`, `usuario`, `perfil_publicador` y `actividad` no eliminan solicitudes en cascada.

`solicitud_publicacion.usuario_id` y `solicitud_publicacion.perfil_publicador_id` pueden ser `NULL` para conservar compatibilidad con solicitudes historicas o anonimas. Si `perfil_publicador_id` tiene valor, `usuario_id` tambien debe tener valor.

La relación `solicitud_publicacion 1:N solicitud_publicacion_horario` sí usa borrado en cascada respecto de su solicitud. Si se borra físicamente una solicitud, se eliminan sus horarios asociados.

## Ciudades y navegación territorial

La plataforma soporta múltiples ciudades desde el modelo relacional. Mar del Plata es la ciudad inicial/default de DondeEntreno.

La tabla `ciudad` incluye:

* `nombre`;
* `slug`;
* `provincia`;
* `pais`;
* `activa`;
* `orden`;
* fechas de creación y actualización.

`ciudad.slug` se usa para rutas territoriales públicas como:

```text
/ciudades
/ciudades/[slug]
/ciudades/mar-del-plata
```

`ciudad.orden` permite ordenar ciudades editorialmente, por ejemplo dejando Mar del Plata primero aunque existan otras ciudades activas.

`ciudad.activa` indica si una ciudad está disponible para uso público o administrativo.

Las actividades no guardan `ciudad_id` directamente. Llegan a ciudad mediante esta relación:

```text
actividad -> ubicacion -> ciudad
```

Los barrios pertenecen a una ciudad mediante:

```text
barrio.ciudad_id -> ciudad.id
```

La navegación pública debería mostrar preferentemente ciudades activas que tengan actividades publicadas y activas. Esto evita mostrar ciudades sin contenido al visitante.

No se agregaron campos SEO ni de landing avanzada, como `descripcion`, `imagen_url`, `latitud`, `longitud`, `meta_title` o `meta_description`.

No se cargaron ciudades futuras persistentes todavía. Miramar, Balcarce, Necochea, Otamendi y Tandil podrán cargarse más adelante cuando haya contenido real o un flujo administrativo definido.

La ciudad activa elegida por el usuario se recordará desde el frontend en el navegador. PostgreSQL no guarda la preferencia de ciudad del visitante.

### Riesgo conocido de ciudad y barrio

Actualmente no hay una constraint en PostgreSQL que garantice que `ubicacion.barrio_id` pertenezca a la misma ciudad que `ubicacion.ciudad_id`.

Esa coherencia se valida desde el backend en los flujos actuales y se controla con consultas de diagnóstico en `11_test_city_navigation_queries.sql`.

Más adelante puede evaluarse una FK compuesta o una estrategia equivalente si se quiere que PostgreSQL garantice esa regla directamente.

## Auth y Seguridad

### Roles

La tabla `rol` define los roles permitidos para cuentas de usuario:

* `SUPER_ADMIN`
* `ADMIN`
* `PUBLICADOR`
* `USUARIO`

Cada registro de `usuario` tiene un solo rol mediante `usuario.rol_id`.

`SUPER_ADMIN` y `ADMIN` quedan reservados para administración. `PUBLICADOR` representa cuentas que gestionan perfiles y actividades. `USUARIO` representa una cuenta común para funciones básicas de una persona registrada.

### Usuario

La tabla `usuario` guarda cuentas de acceso al sistema.

Reglas y campos relevantes:

* `email` es obligatorio.
* `password_hash` es obligatorio.
* No existe una columna para contraseña plana.
* `activo` controla si la cuenta puede autenticarse.
* `email_verificado` indica si el email fue confirmado.
* `ultimo_login_at` permite registrar el último inicio de sesión.
* `deleted_at` representa baja lógica.
* `email` mantiene la restricción única original.
* Además, `idx_usuario_email_normalizado_unico` asegura unicidad case-insensitive y tolerante a espacios externos mediante `LOWER(BTRIM(email))`.

La unicidad normalizada aplica a todos los usuarios, incluidos los que tengan `deleted_at`. Por eso un email de una cuenta eliminada lógicamente no se reutiliza automáticamente.

### BCrypt

El hash BCrypt se generará en el backend.

PostgreSQL no recibe ni almacena la contraseña plana. Solo persiste el valor de `password_hash`.

Todavía no se aplica una regex BCrypt en la base de datos porque existen placeholders históricos de prueba en seeds locales. Esa validación deberá incorporarse más adelante, cuando los datos históricos estén normalizados o reemplazados.

### Primer SUPER_ADMIN

La estrategia aprobada para crear el primer `SUPER_ADMIN` real es un bootstrap posterior desde Spring Boot.

Ese bootstrap deberá:

* recibir email y contraseña mediante variables de entorno;
* generar el hash con BCrypt en runtime;
* crear la cuenta de forma idempotente usando el email normalizado configurado;
* no guardar contraseña ni hash real en Git;
* retirar o desactivar las variables de bootstrap después de crear la cuenta;
* no usar como única condición “no existe ningún SUPER_ADMIN”, porque podrían existir usuarios de prueba históricos.

### Seeds de usuarios

`03_seed_test_data.sql` es exclusivamente local y no debe ejecutarse en producción.

Sus usuarios ficticios se crean con:

* `activo = false`;
* `email_verificado = false`;
* hashes placeholder que no sirven para login real.

Modificar ese seed no cambia bases donde el script ya fue ejecutado. Las bases existentes deben inspeccionarse antes de habilitar Auth real, especialmente si tienen usuarios históricos activos, verificados o con hashes de prueba.

## Login, registro y panel publicador V1

El login sera unificado para estos roles:

* `USUARIO`
* `PUBLICADOR`
* `ADMIN`
* `SUPER_ADMIN`

Cada usuario mantiene un unico rol mediante `usuario.rol_id`. No hay multirol, no existe una tabla `usuario_roles` y no se agrego una tabla intermedia para permisos.

`PUBLICADOR` tendra permisos de usuario comun por logica backend. Es decir, no necesita tener dos roles para poder usar funcionalidades basicas de una cuenta registrada.

### Usuario registrado

`usuario.telefono_normalizado` permite buscar, comparar o validar telefonos usando solo digitos. No es unico y no hace obligatorio informar telefono.

`usuario.telefono_verificado` queda preparado para futuras verificaciones. La migracion no agrega tokens, recuperacion de contrasena, login por telefono ni login con Google.

El email sigue siendo el identificador principal de login. La unicidad normalizada de email se conserva mediante `idx_usuario_email_normalizado_unico`.

### Perfil publicador

`perfil_publicador.estado` representa el estado funcional del publicador para registro, revision y panel publicador. Los valores permitidos son:

* `INCOMPLETO`
* `PENDIENTE_REVISION`
* `ACTIVO`
* `SUSPENDIDO`

El backfill inicial mantiene compatibilidad con los datos existentes:

* perfiles con `deleted_at` o `activo = false` quedan como `SUSPENDIDO`;
* perfiles activos y verificados quedan como `ACTIVO`;
* perfiles activos no verificados quedan como `PENDIENTE_REVISION`;
* nuevos perfiles usan `INCOMPLETO` por defecto.

`perfil_publicador.ciudad_principal_id` permite asociar un perfil a una ciudad principal. Es opcional y usa una clave foranea a `ciudad(id)`.

`perfil_publicador.whatsapp_normalizado` y `perfil_publicador.telefono_contacto_normalizado` permiten validaciones futuras de contacto usando solo digitos. No son unicos y no modifican los campos visibles `whatsapp` ni `telefono_contacto`.

No se cambiaron los valores actuales de `perfil_publicador.tipo_publicador`. Siguen siendo:

* `CLUB`
* `GIMNASIO`
* `PROFESOR_INDEPENDIENTE`
* `INSTITUCION`
* `ESCUELA_DEPORTIVA`
* `ESPACIO_ENTRENAMIENTO`

### Solicitudes asociadas a cuentas

`solicitud_publicacion.usuario_id` permite saber que usuario envio la solicitud cuando el flujo sea autenticado.

`solicitud_publicacion.perfil_publicador_id` permite saber desde que perfil publicador se envio la solicitud.

Ambos campos son opcionales para conservar solicitudes historicas o anonimas. El backend V1 debera exigirlos para nuevas solicitudes autenticadas del panel publicador.

PostgreSQL valida una regla minima: si `perfil_publicador_id` tiene valor, `usuario_id` no puede ser `NULL`. La validacion de que el perfil pertenezca al mismo usuario se resolvera desde el backend V1.

## Solicitudes de publicación

### Objetivo

Una persona, club, gimnasio o profesor puede enviar una solicitud pública sin iniciar sesión.

La solicitud:

* no se guarda directamente como actividad;
* no aparece automáticamente en el catálogo;
* queda pendiente de revisión administrativa;
* puede terminar aprobada o rechazada.

La aprobación no crea ni publica automáticamente una actividad desde PostgreSQL. La creación de la actividad aprobada queda bajo control del backend y del flujo administrativo.

### Estados

Los estados permitidos para `solicitud_publicacion.estado` son:

* `PENDIENTE`
* `EN_REVISION`
* `APROBADA`
* `RECHAZADA`

### Reglas principales

La tabla `solicitud_publicacion` aplica reglas de integridad para mantener datos mínimos consistentes:

* Debe indicarse un deporte existente o un deporte escrito como “otro”, pero no ambos.
* Debe indicarse una ciudad existente o una ciudad escrita como “otra”, pero no ambas.
* Debe indicarse un barrio existente o un barrio escrito como “otro”, pero no ambos.
* Debe existir al menos WhatsApp o email.
* Si se informa WhatsApp, debe existir `whatsapp_normalizado`.
* El WhatsApp normalizado debe contener solo dígitos.
* El precio es opcional.
* Si `mostrar_precio = false`, la interfaz puede mostrar “Consultar”.
* Los horarios se guardan de forma estructurada por día, hora de inicio y hora de fin.
* Debe existir al menos un horario, validado por el backend dentro de una transacción.
* La aceptación de condiciones es obligatoria.
* El código de seguimiento es único.
* La baja lógica se representa con `deleted_at`.
* Si el estado es `RECHAZADA`, debe existir un motivo de rechazo.
* Si el estado no es `RECHAZADA`, el motivo de rechazo debe quedar vacío.
* La fecha de finalización de revisión no puede ser anterior a la fecha de inicio.

La tabla `solicitud_publicacion_horario` evita horarios duplicados exactos para una misma solicitud y valida que `hora_inicio < hora_fin`.

### Responsabilidades del backend

El backend deberá encargarse de:

* generar `codigo_seguimiento`;
* normalizar email;
* normalizar WhatsApp;
* exigir al menos un horario dentro de una transacción;
* validar que el barrio corresponda con la ciudad;
* manejar las transiciones de estados;
* actualizar `updated_at`;
* crear de forma controlada la actividad aprobada;
* aplicar medidas contra spam y duplicados.

### Aprobación de solicitudes y trazabilidad

No hizo falta una migración estructural nueva para representar la aprobación de solicitudes, porque `solicitud_publicacion` ya tiene los campos necesarios:

* `actividad_generada_id`;
* `revisado_por_usuario_id`;
* `revision_finalizada_at`;
* `estado`;
* `motivo_rechazo`;
* `observaciones_revision`.

`actividad_generada_id` vincula la solicitud con la actividad creada finalmente en `actividad`.

`revisado_por_usuario_id` representa al usuario administrador que tomó, aprobó o rechazó la solicitud.

`revision_finalizada_at` representa la fecha de cierre de la revisión. Según el valor de `estado`, esa fecha puede interpretarse como fecha de aprobación o de rechazo.

`motivo_rechazo` aplica solamente cuando `estado = 'RECHAZADA'`. Para estados como `PENDIENTE`, `EN_REVISION` o `APROBADA`, debe permanecer vacío.

La aprobación real debe hacerla el backend dentro de una transacción controlada. PostgreSQL no crea actividades automáticamente, no copia horarios por su cuenta y no cambia estados mediante triggers.

No se agregaron campos como `aprobada_en`, `aprobada_por_usuario_id`, `rechazada_en` ni `rechazada_por_usuario_id` porque serían redundantes con `estado`, `revisado_por_usuario_id` y `revision_finalizada_at`.

## Crear la base de datos local

Desde PostgreSQL, crear una base de datos para el proyecto.

Nombre sugerido:

```text
donde_entreno_db
```

Ejemplo SQL:

```sql
CREATE DATABASE donde_entreno_db;
```

Después conectarse a esa base antes de ejecutar los scripts.

## Ejecución con DBeaver

Pasos sugeridos:

1. Abrir DBeaver.
2. Conectarse a PostgreSQL.
3. Crear la base de datos `donde_entreno_db`, si todavía no existe.
4. Abrir el script `01_create_tables.sql`.
5. Ejecutarlo completo.
6. Repetir el proceso con `02_seed_data.sql`.
7. Repetir el proceso con `03_seed_test_data.sql`.
8. Ejecutar `05_create_solicitud_publicacion.sql`.
9. Ejecutar `07_prepare_auth_security.sql`.
10. Ejecutar `10_prepare_city_navigation.sql`.
11. Ejecutar `12_prepare_publisher_accounts.sql`.
12. Ejecutar `04_test_queries.sql` para validar el modelo inicial.
13. Ejecutar `06_test_solicitud_publicacion_queries.sql` para validar solicitudes de publicación.
14. Ejecutar `08_test_auth_security_queries.sql` para validar Auth y Seguridad.
15. Ejecutar `09_test_approval_traceability_queries.sql` para validar trazabilidad de aprobación.
16. Ejecutar `11_test_city_navigation_queries.sql` para validar navegación territorial.
17. Ejecutar `13_test_publisher_accounts_queries.sql` para validar cuentas publicador y solicitudes asociadas.

## Ejecución desde terminal

También se pueden ejecutar los scripts usando `psql`.

Ejemplo para preparar estructura y datos locales:

```bash
psql -U postgres -d donde_entreno_db -f database/scripts/01_create_tables.sql
psql -U postgres -d donde_entreno_db -f database/scripts/02_seed_data.sql
psql -U postgres -d donde_entreno_db -f database/scripts/03_seed_test_data.sql
psql -U postgres -d donde_entreno_db -f database/scripts/05_create_solicitud_publicacion.sql
psql -U postgres -d donde_entreno_db -f database/scripts/07_prepare_auth_security.sql
psql -U postgres -d donde_entreno_db -f database/scripts/10_prepare_city_navigation.sql
psql -U postgres -d donde_entreno_db -f database/scripts/12_prepare_publisher_accounts.sql
```

Ejemplo para validaciones locales:

```bash
psql -U postgres -d donde_entreno_db -f database/scripts/04_test_queries.sql
psql -U postgres -d donde_entreno_db -f database/scripts/06_test_solicitud_publicacion_queries.sql
psql -U postgres -d donde_entreno_db -f database/scripts/08_test_auth_security_queries.sql
psql -U postgres -d donde_entreno_db -f database/scripts/09_test_approval_traceability_queries.sql
psql -U postgres -d donde_entreno_db -f database/scripts/11_test_city_navigation_queries.sql
psql -U postgres -d donde_entreno_db -f database/scripts/13_test_publisher_accounts_queries.sql
```

El usuario `postgres` puede cambiar según la configuración local de cada máquina.

## Configuración del backend

El backend utiliza un perfil local para conectarse a la base de datos.

Archivo local esperado:

```text
backend/donde-entreno-api/donde-entreno-api/src/main/resources/application-local.properties
```

Este archivo no debe subirse al repositorio porque puede contener datos sensibles.

Ejemplo de configuración local:

```properties
spring.datasource.url=${SPRING_DATASOURCE_URL}
spring.datasource.username=${SPRING_DATASOURCE_USERNAME}
spring.datasource.password=${SPRING_DATASOURCE_PASSWORD}
```

## Seguridad

No subir al repositorio archivos con contraseñas o credenciales reales.

Archivos que deben mantenerse fuera de Git:

```text
application-local.properties
application-dev.properties
application-prod.properties
.env
.env.local
```

El proyecto ya cuenta con reglas en `.gitignore` para evitar subir estos archivos.

## Validación rápida

Después de ejecutar los scripts, se recomienda verificar:

* Que existan las 13 tablas actuales.
* Que haya datos en `ciudad`, `barrio`, `deporte` y `categoria_deportiva`.
* Que existan actividades de prueba.
* Que las actividades tengan horarios asociados en `horario_actividad`.
* Que las actividades tengan ubicación.
* Que existan las tablas `solicitud_publicacion` y `solicitud_publicacion_horario`.
* Que `06_test_solicitud_publicacion_queries.sql` termine con `ROLLBACK` y no deje datos temporales persistidos.
* Que exista el rol `USUARIO`.
* Que exista el índice `idx_usuario_email_normalizado_unico`.
* Que `08_test_auth_security_queries.sql` termine con `ROLLBACK` y no conserve usuarios temporales.
* Que `09_test_approval_traceability_queries.sql` termine con `ROLLBACK` y no conserve datos temporales.
* Que una solicitud aprobada pueda vincularse con una actividad mediante `actividad_generada_id`.
* Que `ciudad.slug` y `ciudad.orden` existan.
* Que Mar del Plata tenga `slug = 'mar-del-plata'`, `orden = 1` y `activa = true`.
* Que `11_test_city_navigation_queries.sql` termine con `ROLLBACK` y no conserve ciudades temporales.
* Que `usuario.telefono_normalizado` y `usuario.telefono_verificado` existan.
* Que `perfil_publicador.estado`, `ciudad_principal_id`, `whatsapp_normalizado` y `telefono_contacto_normalizado` existan.
* Que `solicitud_publicacion.usuario_id` y `solicitud_publicacion.perfil_publicador_id` existan y acepten `NULL`.
* Que `13_test_publisher_accounts_queries.sql` termine con `ROLLBACK` y no conserve usuarios, perfiles ni solicitudes temporales.
* Que los endpoints del backend respondan correctamente.

Endpoint útil para probar:

```text
GET http://localhost:8080/api/actividades
```

Si devuelve actividades, la base de datos está correctamente conectada con el backend.

## Estado actual

La base de datos local del MVP cuenta con estructura inicial, datos iniciales, datos de prueba, consultas de control, una migración aditiva para solicitudes públicas de publicación, una migración aditiva de preparación de Auth y Seguridad, una migración aditiva para navegación territorial por ciudad, una migracion aditiva para cuentas publicador y scripts de validación local para trazabilidad de aprobación, navegación territorial y solicitudes asociadas a cuentas.

Después de aplicar `05_create_solicitud_publicacion.sql`, el modelo local pasa de 11 a 13 tablas.

Después de aplicar `07_prepare_auth_security.sql`, no se agregan tablas nuevas. Se endurecen reglas sobre roles, usuarios y unicidad de email normalizado.

Después de aplicar `10_prepare_city_navigation.sql`, no se agregan tablas nuevas. Se agregan `slug` y `orden` a `ciudad`, se deja Mar del Plata como ciudad inicial/default y se prepara el modelo para rutas por ciudad.

Después de aplicar `12_prepare_publisher_accounts.sql`, no se agregan tablas nuevas. Se agregan campos, constraints e indices para registro de usuarios, estado funcional de perfiles publicadores, contactos normalizados, ciudad principal del publicador y asociacion opcional de solicitudes con `usuario` y `perfil_publicador`.
