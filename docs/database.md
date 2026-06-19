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
```

## Orden de uso local

Para preparar una base local desde cero, usar este orden:

```text
1. 01_create_tables.sql
2. 02_seed_data.sql
3. 03_seed_test_data.sql
4. 05_create_solicitud_publicacion.sql
```

Los scripts de validación se ejecutan después de tener la estructura y los datos necesarios:

```text
5. 04_test_queries.sql
6. 06_test_solicitud_publicacion_queries.sql
```

`04_test_queries.sql` contiene consultas de control del MVP inicial.

`06_test_solicitud_publicacion_queries.sql` valida localmente la migración `05`. Comienza con `BEGIN`, termina con `ROLLBACK` y no deja datos de prueba persistidos.

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
* `solicitud_publicacion` puede relacionarse opcionalmente con `usuario` como revisor.
* `solicitud_publicacion` puede relacionarse opcionalmente con `actividad` como actividad finalmente generada.
* `solicitud_publicacion` tiene muchos registros en `solicitud_publicacion_horario`.

Las relaciones opcionales desde `solicitud_publicacion` hacia `deporte`, `ciudad`, `barrio`, `usuario` y `actividad` no eliminan solicitudes en cascada.

La relación `solicitud_publicacion 1:N solicitud_publicacion_horario` sí usa borrado en cascada respecto de su solicitud. Si se borra físicamente una solicitud, se eliminan sus horarios asociados.

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
9. Ejecutar `04_test_queries.sql` para validar el modelo inicial.
10. Ejecutar `06_test_solicitud_publicacion_queries.sql` para validar solicitudes de publicación.

## Ejecución desde terminal

También se pueden ejecutar los scripts usando `psql`.

Ejemplo para preparar estructura y datos locales:

```bash
psql -U postgres -d donde_entreno_db -f database/scripts/01_create_tables.sql
psql -U postgres -d donde_entreno_db -f database/scripts/02_seed_data.sql
psql -U postgres -d donde_entreno_db -f database/scripts/03_seed_test_data.sql
psql -U postgres -d donde_entreno_db -f database/scripts/05_create_solicitud_publicacion.sql
```

Ejemplo para validaciones locales:

```bash
psql -U postgres -d donde_entreno_db -f database/scripts/04_test_queries.sql
psql -U postgres -d donde_entreno_db -f database/scripts/06_test_solicitud_publicacion_queries.sql
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
spring.datasource.url=jdbc:postgresql://localhost:5432/donde_entreno_db
spring.datasource.username=postgres
spring.datasource.password=TU_PASSWORD_LOCAL
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
* Que los endpoints del backend respondan correctamente.

Endpoint útil para probar:

```text
GET http://localhost:8080/api/actividades
```

Si devuelve actividades, la base de datos está correctamente conectada con el backend.

## Estado actual

La base de datos local del MVP cuenta con estructura inicial, datos iniciales, datos de prueba, consultas de control y una migración aditiva para solicitudes públicas de publicación.

Después de aplicar `05_create_solicitud_publicacion.sql`, el modelo local pasa de 11 a 13 tablas.
