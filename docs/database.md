# Base de datos - DondeEntreno

Este documento explica cómo preparar y ejecutar la base de datos local del proyecto DondeEntreno.

La base de datos está desarrollada en PostgreSQL y se administra mediante scripts SQL versionados dentro del repositorio.

## Tecnología utilizada

* PostgreSQL.
* SQL.
* DBeaver o pgAdmin como herramienta visual opcional.

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
```

## Orden de ejecución

Los scripts deben ejecutarse en este orden:

```text
1. 01_create_tables.sql
2. 02_seed_data.sql
3. 03_seed_test_data.sql
4. 04_test_queries.sql
```

## Descripción de cada script

### 01_create_tables.sql

Crea la estructura principal de la base de datos.

Incluye tablas como:

* rol
* ciudad
* barrio
* categoria_deportiva
* deporte
* usuario
* club
* profesor
* perfil_publicador
* actividad
* ubicacion
* horario
* imagen
* estado_publicacion

También define relaciones, claves primarias, claves foráneas e índices necesarios para el funcionamiento inicial del proyecto.

### 02_seed_data.sql

Carga datos iniciales necesarios para que la aplicación pueda funcionar.

Incluye datos base como:

* roles
* ciudades
* barrios
* categorías deportivas
* deportes
* estados de publicación

### 03_seed_test_data.sql

Carga datos de prueba para poder visualizar y testear el MVP.

Incluye ejemplos de:

* clubes
* profesores
* actividades
* ubicaciones
* horarios
* imágenes
* perfiles publicadores

Estos datos permiten probar el backend y el frontend sin tener que cargar información manualmente.

### 04_test_queries.sql

Contiene consultas de control para verificar que la base de datos esté funcionando correctamente.

Sirve para revisar:

* actividades cargadas
* relaciones entre tablas
* actividades publicadas
* horarios asociados
* imágenes asociadas
* datos usados por el frontend

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
8. Ejecutar `04_test_queries.sql` para validar que todo esté cargado correctamente.

## Ejecución desde terminal

También se pueden ejecutar los scripts usando `psql`.

Ejemplo:

```bash
psql -U postgres -d donde_entreno_db -f database/scripts/01_create_tables.sql
psql -U postgres -d donde_entreno_db -f database/scripts/02_seed_data.sql
psql -U postgres -d donde_entreno_db -f database/scripts/03_seed_test_data.sql
psql -U postgres -d donde_entreno_db -f database/scripts/04_test_queries.sql
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

* Que existan las tablas principales.
* Que haya datos en `ciudad`, `barrio`, `deporte` y `categoria_deportiva`.
* Que existan actividades de prueba.
* Que las actividades tengan horarios asociados.
* Que las actividades tengan ubicación.
* Que los endpoints del backend respondan correctamente.

Endpoint útil para probar:

```text
GET http://localhost:8080/api/actividades
```

Si devuelve actividades, la base de datos está correctamente conectada con el backend.

## Estado actual

La base de datos local del MVP ya cuenta con estructura, datos iniciales, datos de prueba y consultas de control.
