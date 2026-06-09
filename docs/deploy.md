# Deploy - DondeEntreno

Este documento describe la estrategia prevista para desplegar DondeEntreno en producción.

El proyecto todavía no está desplegado. Antes de realizar el deploy se documentan las partes necesarias, variables de entorno y decisiones técnicas.

## Partes del deploy

DondeEntreno tiene tres partes principales:

```text
1. Base de datos PostgreSQL
2. Backend Spring Boot
3. Frontend Next.js
Estrategia inicial recomendada

Para una primera versión online del MVP, se puede usar una estrategia simple:

Base de datos: Supabase, Neon, Railway o Render PostgreSQL.
Backend: Render, Railway o VPS.
Frontend: Vercel.
Repositorio: GitHub.
Orden recomendado

El orden recomendado para desplegar es:

1. Crear base de datos PostgreSQL online.
2. Ejecutar scripts SQL en la base remota.
3. Configurar variables de entorno del backend.
4. Desplegar backend.
5. Probar endpoints públicos del backend.
6. Configurar variable de entorno del frontend.
7. Desplegar frontend.
8. Probar flujo completo online.
Variables de entorno del frontend

El frontend necesita conocer la URL pública del backend.

Variable:

NEXT_PUBLIC_API_URL=https://url-publica-del-backend

En desarrollo local se usa:

NEXT_PUBLIC_API_URL=http://localhost:8080

Archivo local:

frontend/donde-entreno-web/.env.local

Archivo de ejemplo versionado:

frontend/donde-entreno-web/.env.example

## Variables de entorno del backend

El backend necesita conectarse a PostgreSQL y conocer qué frontend tiene permitido consumir la API.

Archivo de ejemplo versionado:

```text
backend/donde-entreno-api/donde-entreno-api/src/main/resources/application-prod.example.properties

Variables necesarias para producción:

PORT
SPRING_DATASOURCE_URL
SPRING_DATASOURCE_USERNAME
SPRING_DATASOURCE_PASSWORD
APP_CORS_ALLOWED_ORIGINS

Ejemplo:

server.port=${PORT:8080}

spring.datasource.url=${SPRING_DATASOURCE_URL}
spring.datasource.username=${SPRING_DATASOURCE_USERNAME}
spring.datasource.password=${SPRING_DATASOURCE_PASSWORD}

app.cors.allowed-origins=${APP_CORS_ALLOWED_ORIGINS}

Ejemplo de valor para CORS en producción:

APP_CORS_ALLOWED_ORIGINS=https://donde-entreno.vercel.app

En desarrollo local se usa:

http://localhost:3000

Actualmente el backend permite conexión desde:

http://localhost:3000

Antes del deploy, se debe agregar la URL pública del frontend.

Ejemplo:

https://donde-entreno.vercel.app
Base de datos en producción

La base de datos de producción debe cargarse ejecutando los scripts en este orden:

1. 01_create_tables.sql
2. 02_seed_data.sql
3. 03_seed_test_data.sql
4. 04_test_queries.sql

Para una versión real, más adelante se puede separar:

datos iniciales obligatorios
datos de prueba
datos reales de producción
Checklist antes de deploy
 El backend compila correctamente.
 El frontend compila correctamente.
 El repositorio está limpio.
 No hay archivos sensibles versionados.
 Existe .env.example.
 La documentación está actualizada.
 El backend usa variables seguras para la base de datos.
 CORS contempla la URL pública del frontend.
 La base remota tiene los scripts ejecutados.
 Los endpoints públicos responden correctamente.
 El frontend apunta a la URL pública del backend.
Posibles plataformas
Frontend
Vercel.
Netlify.
Backend
Render.
Railway.
Fly.io.
VPS.
Base de datos
Supabase.
Neon.
Railway PostgreSQL.
Render PostgreSQL.
Estado actual

El proyecto está validado localmente y listo para preparar el deploy, pero todavía no fue desplegado.

## Revisión de scripts SQL para deploy

Se revisaron los scripts ubicados en `database/scripts/` antes de preparar el deploy.

Resultado:

- No usan `CREATE DATABASE`.
- No usan `DROP DATABASE`.
- No usan `DROP TABLE`.
- No usan `ALTER DATABASE`.
- No usan `OWNER TO`.
- No usan comandos `\c`.
- Usan sintaxis compatible con PostgreSQL como `BIGSERIAL`, `TIMESTAMPTZ`, `NOW()`, `CONSTRAINT` y `FOREIGN KEY`.
- Los datos insertados fueron revisados y no presentan caracteres rotos en nombres o descripciones.
- Los scripts pueden ejecutarse en una base PostgreSQL online como Supabase o Neon, siempre respetando el orden definido.

Orden de ejecución:

```text
01_create_tables.sql
02_seed_data.sql
03_seed_test_data.sql
04_test_queries.sql