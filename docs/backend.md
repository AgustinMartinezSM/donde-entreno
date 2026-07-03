# Backend - DondeEntreno

Este documento explica cómo ejecutar y entender el backend del proyecto DondeEntreno.

El backend está desarrollado con Java 21 y Spring Boot. Expone una API REST pública que permite consultar actividades deportivas, filtros, ciudades, barrios, deportes, categorías deportivas, perfiles publicadores y ubicaciones.

## Tecnología utilizada

- Java 21.
- Spring Boot.
- Spring Data JPA.
- Maven.
- PostgreSQL.

## Ubicación del backend

El backend se encuentra en:

```text
backend/donde-entreno-api/donde-entreno-api
Puerto local

Por defecto, el backend se ejecuta en:

http://localhost:8080

La configuración principal está en:

src/main/resources/application.properties
Configuración local

El proyecto utiliza el perfil local:

spring.profiles.active=local

Para ejecutar el backend localmente, se espera un archivo:

src/main/resources/application-local.properties

Este archivo no debe subirse al repositorio porque puede contener datos sensibles.

Ejemplo de configuración local:

spring.datasource.url=${SPRING_DATASOURCE_URL}
spring.datasource.username=${SPRING_DATASOURCE_USERNAME}
spring.datasource.password=${SPRING_DATASOURCE_PASSWORD}
Seguridad de configuración

No subir al repositorio archivos con contraseñas, tokens o credenciales reales.

Archivos que deben mantenerse fuera de Git:

application-local.properties
application-dev.properties
application-prod.properties
.env
.env.local
Cómo ejecutar el backend

Desde la raíz del proyecto:

cd backend/donde-entreno-api/donde-entreno-api

Ejecutar con Maven Wrapper:

./mvnw spring-boot:run

En Windows:

mvnw.cmd spring-boot:run

Si Maven está instalado globalmente, también se puede usar:

mvn spring-boot:run
Verificar que el backend está funcionando

Una vez iniciado, abrir en el navegador o en Postman:

http://localhost:8080/api/actividades

Si devuelve una respuesta JSON, el backend está funcionando y conectado correctamente con la base de datos.

Endpoints principales
GET /api/actividades
GET /api/actividades/{slug}
GET /api/actividades/{slug}/detalle
GET /api/actividades/{slug}/horarios
GET /api/actividades/{slug}/imagenes
GET /api/filtros/opciones
GET /api/categorias-deportivas
GET /api/deportes
GET /api/ciudades
GET /api/ciudades/{slug}
GET /api/barrios
GET /api/perfiles-publicadores
GET /api/ubicaciones
POST /api/solicitudes-publicacion

Endpoint publico de solicitudes de publicacion
POST /api/solicitudes-publicacion

Objetivo:
Recibir una solicitud publica sin login y dejarla en estado inicial PENDIENTE, sin crear todavia una actividad real.

Respuesta exitosa:
201 Created

Campos de respuesta:
id
codigoSeguimiento
estado
createdAt
mensaje

Errores:
400 por Bean Validation.
400 por JSON mal formado.
400 por reglas de negocio o referencias invalidas.

Transaccion:
La solicitud principal y sus horarios se guardan en una unica transaccion. Si falla cualquier parte de la operacion, se revierte todo y no queda una solicitud parcial.
Endpoint principal de actividades
GET /api/actividades

Parámetros disponibles:

texto
deporteId
deporteSlug
ciudadId
ciudadSlug
barrioId
perfilPublicadorId
nivel
modalidad
page
size
orden

Ejemplo:

http://localhost:8080/api/actividades?page=0&size=6

Ejemplo con búsqueda:

http://localhost:8080/api/actividades?texto=jiu&page=0&size=6

Ejemplo con filtro por deporte:

http://localhost:8080/api/actividades?deporteSlug=jiu-jitsu&page=0&size=6

Ejemplo con filtro por ciudad:

http://localhost:8080/api/actividades?ciudadSlug=mar-del-plata&page=0&size=6

Endpoint de ciudades
GET /api/ciudades
GET /api/ciudades/{slug}

Ejemplo:

http://localhost:8080/api/ciudades/mar-del-plata
CORS

El backend tiene CORS configurado para permitir conexiones desde el frontend local:

http://localhost:3000

Antes de hacer deploy, esta configuración deberá actualizarse para permitir también la URL pública del frontend.

Organización del backend

El backend está organizado por capas:

controller/
service/
repository/
entity/
dto/
mapper/
exception/
config/
controller

Expone los endpoints REST.

service

Contiene la lógica de negocio y coordina las operaciones.

repository

Accede a la base de datos usando Spring Data JPA.

entity

Representa las tablas de la base de datos.

dto

Define los objetos que se devuelven al frontend.

mapper

Convierte entidades en DTOs.

exception

Maneja errores personalizados y respuestas de error.

config

Contiene configuraciones generales, como CORS.

Validaciones manuales recomendadas

Antes de mostrar el proyecto, revisar:

El backend levanta sin errores.
Se conecta correctamente a PostgreSQL.
GET /api/actividades devuelve actividades.
Los filtros funcionan correctamente.
El endpoint de detalle por slug devuelve una actividad válida.
Un slug inexistente devuelve un error controlado.
Los endpoints de filtros devuelven datos.
No hay contraseñas en archivos versionados.
CORS permite conexión desde el frontend local.
Estado actual

El backend del MVP está funcional y expone los endpoints necesarios para que el frontend pueda buscar, filtrar y mostrar actividades deportivas.
