# DondeEntreno

DondeEntreno es una plataforma web full stack para encontrar deportes, clubes, profesores independientes, gimnasios, espacios de entrenamiento y actividades deportivas dentro de una ciudad.

El objetivo del proyecto es facilitar la búsqueda de actividades deportivas disponibles en una zona determinada, permitiendo filtrar por deporte, ubicación, tipo de publicador, modalidad, nivel y otros criterios útiles para el usuario.

## Estado del proyecto

Proyecto en desarrollo, actualmente con un MVP público funcional.

El MVP incluye:

* Home.
* Página de exploración de actividades.
* Página de detalle de actividad.
* Búsqueda de actividades.
* Filtros combinados.
* Ordenamiento.
* Paginación.
* Página de publicación en desarrollo.
* Página 404 personalizada.
* Diseño responsive mobile-first.
* Conexión real entre frontend y backend.
* Base de datos PostgreSQL con datos iniciales y datos de prueba.

## Tecnologías utilizadas

### Backend

* Java 21.
* Spring Boot.
* Spring Data JPA.
* Maven.
* PostgreSQL.

### Frontend

* Next.js.
* React.
* TypeScript.
* Tailwind CSS.

### Base de datos

* PostgreSQL.
* Scripts SQL versionados para creación de tablas, carga inicial, datos de prueba y consultas de control.

### Herramientas

* Git.
* GitHub.
* IntelliJ IDEA.
* Visual Studio Code.
* Postman.
* DBeaver.

## Estructura del proyecto

```text
DondeEntreno/
├── backend/
│   └── donde-entreno-api/
├── frontend/
│   └── donde-entreno-web/
├── database/
│   └── scripts/
│       ├── 01_create_tables.sql
│       ├── 02_seed_data.sql
│       ├── 03_seed_test_data.sql
│       └── 04_test_queries.sql
├── .gitignore
└── README.md
```

## Funcionalidades principales

* Buscar actividades deportivas dentro de una ciudad.
* Filtrar por deporte, ciudad, barrio, modalidad, nivel y tipo de publicador.
* Ver información detallada de cada actividad.
* Consultar horarios disponibles.
* Ver imágenes asociadas a las actividades.
* Acceder a información de clubes, profesores o espacios de entrenamiento.
* Preparar una base escalable para futuras funcionalidades como favoritos, reseñas, usuarios y panel administrativo.

## Endpoints principales del backend

```text
GET /api/actividades
GET /api/actividades/{slug}
GET /api/actividades/{slug}/detalle
GET /api/actividades/{slug}/horarios
GET /api/actividades/{slug}/imagenes
GET /api/filtros/opciones
GET /api/categorias-deportivas
GET /api/deportes
GET /api/ciudades
GET /api/barrios
GET /api/perfiles-publicadores
GET /api/ubicaciones
```

El endpoint principal de actividades permite usar filtros como:

```text
texto
deporteId
deporteSlug
ciudadId
barrioId
perfilPublicadorId
nivel
modalidad
page
size
orden
```

## Cómo ejecutar el proyecto

La documentación completa para ejecutar el backend, frontend y scripts de base de datos se irá agregando a medida que avance el proyecto.

Actualmente el proyecto se ejecuta localmente con:

```text
Backend:  http://localhost:8080
Frontend: http://localhost:3000
```

## Objetivo como proyecto de portfolio

DondeEntreno busca demostrar el desarrollo de una aplicación full stack real, organizada por capas, con base de datos relacional, API REST, frontend moderno, filtros dinámicos y una estructura preparada para crecer.

El proyecto está pensado como MVP funcional y como base para futuras mejoras, incluyendo autenticación, panel administrativo, publicación de actividades, métricas, favoritos, reseñas y monetización.


