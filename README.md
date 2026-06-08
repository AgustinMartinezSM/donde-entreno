# DondeEntreno

DondeEntreno es una plataforma web full stack para encontrar deportes, clubes, profesores independientes, gimnasios, espacios de entrenamiento y actividades deportivas dentro de una ciudad.

El proyecto nace con la idea de resolver un problema simple: muchas veces una persona quiere empezar una actividad deportiva, pero no sabe dónde buscar, qué opciones hay cerca, qué horarios existen o cómo contactar con el club, profesor o espacio de entrenamiento.

DondeEntreno centraliza esa información en una plataforma clara, moderna y fácil de usar.

## Estado del proyecto

El proyecto se encuentra en desarrollo y actualmente cuenta con un MVP público funcional.

El MVP ya permite navegar actividades deportivas, buscar, filtrar, ordenar, paginar resultados y ver el detalle de cada actividad conectándose a un backend real desarrollado con Spring Boot y una base de datos PostgreSQL.

## Objetivo del MVP

El objetivo del MVP es validar la idea principal de la plataforma:

* Permitir que una persona encuentre actividades deportivas disponibles en una ciudad.
* Mostrar información clara de clubes, profesores o espacios de entrenamiento.
* Facilitar el contacto entre usuarios y publicadores.
* Probar una arquitectura full stack real con frontend, backend y base de datos.
* Dejar una base escalable para futuras funcionalidades.

## Funcionalidades actuales

* Página de inicio.
* Página de exploración de actividades.
* Página de detalle de actividad.
* Página de publicación en desarrollo.
* Página 404 personalizada.
* Búsqueda por texto.
* Filtros combinados.
* Ordenamiento de resultados.
* Paginación.
* Visualización de horarios.
* Visualización de imágenes.
* Datos de clubes, profesores y espacios de entrenamiento.
* Diseño responsive mobile-first.
* Scroll to top.
* Footer simple.
* SEO básico.
* Conexión real entre frontend y backend.
* Base de datos PostgreSQL con datos iniciales y datos de prueba.

## Filtros disponibles

El buscador de actividades permite filtrar por distintos criterios, entre ellos:

* Texto de búsqueda.
* Deporte.
* Slug de deporte.
* Ciudad.
* Barrio.
* Tipo de publicador.
* Nivel.
* Modalidad.
* Página.
* Tamaño de página.
* Ordenamiento.

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
* Scripts SQL versionados para creación de tablas.
* Scripts de datos iniciales.
* Scripts de datos de prueba.
* Consultas de control.

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
|-- backend/
|   `-- donde-entreno-api/
|-- frontend/
|   `-- donde-entreno-web/
|-- database/
|   `-- scripts/
|       |-- 01_create_tables.sql
|       |-- 02_seed_data.sql
|       |-- 03_seed_test_data.sql
|       `-- 04_test_queries.sql
|-- .gitignore
`-- README.md
```

## Base de datos

La base de datos está diseñada en PostgreSQL y se administra mediante scripts SQL versionados.

Scripts disponibles:

```text
database/scripts/01_create_tables.sql
database/scripts/02_seed_data.sql
database/scripts/03_seed_test_data.sql
database/scripts/04_test_queries.sql
```

### Contenido de la base de datos

La base cubre las principales entidades necesarias para el MVP:

* Roles.
* Ciudades.
* Barrios.
* Categorías deportivas.
* Deportes.
* Clubes.
* Profesores.
* Actividades.
* Ubicaciones.
* Horarios.
* Imágenes.
* Estados de publicación.
* Datos iniciales.
* Datos de prueba.

## Backend

El backend está desarrollado con Java 21 y Spring Boot.

La API expone endpoints públicos para consultar actividades deportivas, filtros, ciudades, barrios, deportes, categorías, perfiles publicadores y ubicaciones.

Backend local:

```text
http://localhost:8080
```

### Endpoints principales

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

### Parámetros del endpoint de actividades

El endpoint principal:

```text
GET /api/actividades
```

permite usar los siguientes parámetros:

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

## Frontend

El frontend está desarrollado con Next.js, React, TypeScript y Tailwind CSS.

Frontend local:

```text
http://localhost:3000
```

### Páginas principales

* Home.
* Explorar actividades.
* Detalle de actividad.
* Publicar actividad.
* Página 404 personalizada.

### Componentes principales

* Cards de actividades.
* Panel de filtros.
* Paginación.
* Selector de ordenamiento.
* Estados de carga.
* Estados de error.
* Header.
* Footer.
* Botón de scroll to top.
* Botón de contacto.
* Imagen de actividad.

## Identidad visual

La identidad visual inicial del MVP busca ser cercana, moderna, clara y deportiva, sin parecer una app fitness exagerada.

### Paleta de colores

* Azul profundo: `#0F3D5E`
* Verde activo: `#2EB872`
* Celeste suave: `#4FB3D9`
* Fondo claro: `#F8FAFC`
* Texto principal: `#102A43`

### Tipografía

* Inter.

### Logo textual

El logo textual utiliza el nombre DondeEntreno, diferenciando visualmente:

* “Donde” en azul.
* “Entreno” en verde.

## Cómo ejecutar el proyecto localmente

### Requisitos previos

Para correr el proyecto de forma local se necesita tener instalado:

* Java 21.
* Maven.
* Node.js.
* npm.
* PostgreSQL.
* Git.

## Ejecutar la base de datos

Crear una base de datos PostgreSQL para el proyecto y ejecutar los scripts en este orden:

```text
01_create_tables.sql
02_seed_data.sql
03_seed_test_data.sql
04_test_queries.sql
```

Los scripts se encuentran en:

```text
database/scripts/
```

## Ejecutar el backend

Entrar a la carpeta del backend:

```bash
cd backend/donde-entreno-api/donde-entreno-api
```

Ejecutar la aplicación:

```bash
./mvnw spring-boot:run
```

En Windows también se puede usar:

```bash
mvnw.cmd spring-boot:run
```

El backend queda disponible en:

```text
http://localhost:8080
```

## Ejecutar el frontend

Entrar a la carpeta del frontend:

```bash
cd frontend/donde-entreno-web
```

Instalar dependencias:

```bash
npm install
```

Ejecutar el servidor de desarrollo:

```bash
npm run dev
```

El frontend queda disponible en:

```text
http://localhost:3000
```

## Variables de entorno

El frontend utiliza una variable de entorno para conectarse con el backend:

```env
NEXT_PUBLIC_API_URL=http://localhost:8080
```

Archivo local sugerido:

```text
frontend/donde-entreno-web/.env.local
```

Este archivo no debe subirse al repositorio.

## Seguridad del repositorio

El proyecto utiliza `.gitignore` para evitar subir archivos sensibles o innecesarios, como:

* `.env`
* `.env.local`
* `application-local.properties`
* `application-dev.properties`
* `application-prod.properties`
* `target/`
* `node_modules/`
* `.next/`
* `.idea/`
* `.vscode/`

El archivo `application.properties` puede estar versionado siempre que no contenga contraseñas, tokens ni datos privados.

## Testing actual

Actualmente el proyecto cuenta con una base para pruebas y validaciones manuales.

Se verifican principalmente:

* Conexión frontend-backend.
* Carga de actividades.
* Búsqueda por texto.
* Filtros combinados.
* Ordenamiento.
* Paginación.
* Página de detalle.
* Estados de carga.
* Estados de error.
* Responsive mobile.
* Manejo de rutas inexistentes.
* Consultas SQL de control.

## Checklist manual de prueba

Antes de mostrar o publicar el proyecto, revisar:

* El backend levanta correctamente en `http://localhost:8080`.
* El frontend levanta correctamente en `http://localhost:3000`.
* La Home carga sin errores.
* La página Explorar muestra actividades.
* La búsqueda por texto funciona.
* Los filtros funcionan individualmente.
* Los filtros funcionan combinados.
* El botón de limpiar filtros funciona.
* El ordenamiento cambia los resultados.
* La paginación funciona correctamente.
* El detalle de actividad abre por slug.
* Los horarios se muestran correctamente.
* Las imágenes se muestran correctamente o usan fallback si no existen.
* Los botones de contacto se muestran correctamente.
* La página Publicar se visualiza sin romper el flujo.
* La página 404 personalizada aparece en rutas inexistentes.
* El diseño se ve bien en mobile.
* El diseño se ve bien en escritorio.
* No hay errores visibles en consola del navegador.
* No hay contraseñas ni archivos sensibles versionados en Git.

## Deploy

El deploy todavía no está realizado.

Posibles opciones a analizar:

* Frontend: Vercel.
* Backend: Render, Railway o VPS.
* Base de datos: Supabase, Neon, Render o Railway.
* Dominio: proveedor externo a definir.

Antes del deploy se planea revisar:

* Variables de entorno.
* Configuración de CORS para producción.
* URL pública del backend.
* URL pública del frontend.
* Base de datos remota.
* Build de frontend.
* Build de backend.
* Seguridad de archivos sensibles.

## Roadmap futuro

Algunas mejoras pensadas para próximas etapas:

* Autenticación de usuarios.
* Panel administrativo.
* Publicación real de actividades.
* Aprobación de publicaciones.
* Favoritos.
* Reseñas.
* Métricas de visitas.
* Métricas de contactos.
* Ranking de actividades o clubes.
* Perfiles públicos de clubes.
* Perfiles públicos de profesores.
* Sistema de destacados.
* Monetización por publicaciones destacadas.
* Monetización por suscripciones para clubes o profesores.
* Dashboard para publicadores.

## Objetivo como proyecto de portfolio

DondeEntreno busca demostrar el desarrollo de una aplicación full stack real, organizada por capas y conectada de punta a punta.

El proyecto muestra conocimientos en:

* Diseño de base de datos relacional.
* Modelado de entidades.
* Desarrollo de API REST.
* Organización por capas en Spring Boot.
* Consumo de API desde frontend.
* Desarrollo con Next.js y TypeScript.
* Diseño responsive con Tailwind CSS.
* Manejo de filtros, búsqueda, paginación y estados de carga.
* Uso de Git y GitHub.
* Separación de configuración sensible.
* Preparación de documentación técnica.

## Autor

Proyecto desarrollado por Agustín Martínez como aplicación full stack para portfolio profesional.
