# Frontend - DondeEntreno

Este documento explica cómo ejecutar y entender el frontend del proyecto DondeEntreno.

El frontend está desarrollado con Next.js, React, TypeScript y Tailwind CSS. Consume la API REST del backend para mostrar actividades deportivas, aplicar filtros, ordenar resultados, paginar y visualizar detalles de cada actividad.

## Tecnología utilizada

- Next.js.
- React.
- TypeScript.
- Tailwind CSS.
- npm.

## Ubicación del frontend

El frontend se encuentra en:

```text
frontend/donde-entreno-web
Puerto local

Por defecto, el frontend se ejecuta en:

http://localhost:3000
Variables de entorno

El frontend necesita conocer la URL del backend.

Archivo local esperado:

frontend/donde-entreno-web/.env.local

Contenido esperado para desarrollo local:

NEXT_PUBLIC_API_URL=http://localhost:8080

También existe un archivo de ejemplo versionado:

frontend/donde-entreno-web/.env.example

Este archivo sirve como referencia para configurar el entorno local sin subir credenciales reales.

Seguridad

No subir al repositorio archivos de entorno reales.

Archivos que no deben subirse:

.env
.env.local

El archivo .env.example sí puede subirse porque no contiene secretos, solo valores de ejemplo.

Cómo ejecutar el frontend

Desde la raíz del proyecto:

cd frontend/donde-entreno-web

Instalar dependencias:

npm install

Ejecutar el servidor de desarrollo:

npm run dev

Abrir en el navegador:

http://localhost:3000
Scripts disponibles

Los scripts principales se encuentran en package.json.

Comandos habituales:

npm run dev
npm run build
npm run start
npm run lint
npm run dev

Levanta el servidor local de desarrollo.

npm run build

Genera una build de producción para validar que el proyecto compile correctamente.

npm run start

Ejecuta la aplicación compilada en modo producción local.

npm run lint

Revisa posibles problemas de estilo o errores detectables por ESLint.

Páginas principales

El MVP público incluye:

Home.
Explorar actividades.
Detalle de actividad.
Publicar actividad.
Página 404 personalizada.
Funcionalidades actuales
Búsqueda de actividades.
Filtros combinados.
Ordenamiento.
Paginación.
Visualización de detalle por slug.
Estados de carga.
Estados de error.
Diseño responsive.
Scroll to top.
Footer.
SEO básico.
Conexión real con backend.
Componentes principales

El frontend está organizado en componentes reutilizables.

Algunos componentes importantes:

src/components/actividad/
src/components/explorar/
src/components/feedback/
src/components/home/
src/components/layout/
src/components/publicar/
actividad

Componentes relacionados con el detalle de una actividad, imágenes y botones de contacto.

explorar

Componentes para listar actividades, mostrar cards, filtros, paginación y ordenamiento.

feedback

Componentes para estados de carga y error.

home

Componentes de la página principal.

layout

Componentes generales como header, footer y botón de scroll to top.

publicar

Componentes relacionados con la página de publicación de actividades.

Servicios

Los servicios se encuentran en:

src/services/

Servicios principales:

actividadService.ts
filtrosService.ts
healthService.ts

Estos archivos centralizan las llamadas al backend.

Configuración de API

La configuración de la URL base de la API se encuentra en:

src/lib/apiConfig.ts

La URL se toma desde la variable:

NEXT_PUBLIC_API_URL
Validaciones manuales recomendadas

Antes de mostrar el proyecto, revisar:

El frontend levanta correctamente en http://localhost:3000.
La Home carga sin errores.
La página Explorar carga actividades desde el backend.
La búsqueda por texto funciona.
Los filtros funcionan individualmente.
Los filtros funcionan combinados.
El botón de limpiar filtros funciona.
El ordenamiento modifica los resultados.
La paginación funciona correctamente.
El detalle de actividad abre por slug.
La página Publicar carga correctamente.
La página 404 personalizada aparece en rutas inexistentes.
El diseño se ve bien en mobile.
El diseño se ve bien en escritorio.
No aparecen errores en consola del navegador.
El build de producción finaliza correctamente.
Build de producción

Para validar que el frontend compila correctamente:

npm run build

Si el comando termina sin errores, el frontend está listo para ser analizado para deploy.

Relación con el backend

Para que el frontend funcione correctamente, el backend debe estar activo en:

http://localhost:8080

Y la variable de entorno debe apuntar a esa URL:

NEXT_PUBLIC_API_URL=http://localhost:8080
Estado actual

El frontend del MVP está funcional, conectado al backend y preparado para mostrar actividades deportivas, aplicar filtros y visualizar detalles.