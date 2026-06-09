# Testing - DondeEntreno

Este documento contiene el checklist de pruebas manuales para validar el MVP de DondeEntreno antes de mostrarlo como proyecto de portfolio o avanzar con deploy.

## Objetivo del testing

El objetivo es comprobar que el proyecto funcione correctamente de punta a punta:

- Base de datos.
- Backend.
- Frontend.
- Conexión frontend-backend.
- Búsqueda.
- Filtros.
- Paginación.
- Detalle de actividad.
- Responsive design.
- Manejo de errores.
- Seguridad básica del repositorio.

## Estado esperado antes de probar

Antes de iniciar las pruebas, deben estar funcionando:

```text
Backend:  http://localhost:8080
Frontend: http://localhost:3000

También debe estar creada y cargada la base de datos PostgreSQL con los scripts:

01_create_tables.sql
02_seed_data.sql
03_seed_test_data.sql
04_test_queries.sql
Checklist general
 El repositorio está limpio con git status.
 No hay archivos sensibles versionados.
 La base de datos está creada.
 Los scripts SQL fueron ejecutados en orden.
 El backend levanta sin errores.
 El frontend levanta sin errores.
 El frontend se conecta correctamente al backend.
 No hay errores críticos en consola del navegador.
Pruebas de base de datos
 Existen las tablas principales.
 Existen datos en ciudad.
 Existen datos en barrio.
 Existen datos en categoria_deportiva.
 Existen datos en deporte.
 Existen actividades de prueba.
 Las actividades tienen estado de publicación.
 Las actividades tienen ubicación asociada.
 Las actividades tienen horarios asociados.
 Las actividades tienen imágenes asociadas o fallback esperado.
 Las consultas de 04_test_queries.sql funcionan correctamente.
Pruebas del backend
Levantar backend
 El backend inicia correctamente.
 No aparecen errores de conexión a PostgreSQL.
 El puerto 8080 queda activo.
 El perfil local carga correctamente.
Endpoints principales

Probar que respondan correctamente:

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
Filtros del endpoint de actividades

Probar combinaciones con:

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
Casos de error backend
 Slug inexistente devuelve error controlado.
 Parámetros inválidos no rompen el backend.
 Página sin resultados devuelve respuesta válida.
 El backend no expone contraseñas ni datos sensibles.
 CORS permite requests desde http://localhost:3000.
Pruebas del frontend
Levantar frontend
 El frontend inicia correctamente con npm run dev.
 El puerto 3000 queda activo.
 La variable NEXT_PUBLIC_API_URL apunta al backend local.
 No aparecen errores iniciales en consola.
Home
 La Home carga correctamente.
 El diseño se ve prolijo.
 El logo textual se visualiza bien.
 El buscador principal funciona.
 Los links principales navegan correctamente.
 El footer se muestra correctamente.
Página Explorar
 La página /explorar carga correctamente.
 Se muestran actividades desde el backend.
 Las cards se ven correctamente.
 Se muestra información relevante de cada actividad.
 Se muestran estados de carga cuando corresponde.
 Se muestra estado de error si falla la API.
Búsqueda
 Buscar por texto devuelve resultados correctos.
 Buscar texto inexistente muestra estado sin resultados.
 La búsqueda no rompe la URL.
 La búsqueda se combina correctamente con filtros.
Filtros
 Filtro por deporte funciona.
 Filtro por ciudad funciona.
 Filtro por barrio funciona.
 Filtro por categoría funciona, si aplica.
 Filtro por nivel funciona.
 Filtro por modalidad funciona.
 Filtros combinados funcionan.
 Limpiar filtros resetea la vista.
 Los filtros se reflejan correctamente en la URL, si aplica.
Ordenamiento
 El ordenamiento cambia los resultados.
 El selector de ordenamiento mantiene el estado correctamente.
 Ordenar no rompe filtros activos.
 Ordenar no rompe paginación.
Paginación
 La paginación muestra la página correcta.
 Cambiar de página actualiza resultados.
 La paginación respeta filtros activos.
 La paginación respeta búsqueda activa.
 No permite navegar a páginas inválidas.
Detalle de actividad
 Al hacer click en una card, abre el detalle.
 El detalle carga por slug.
 Se muestra nombre de actividad.
 Se muestra deporte.
 Se muestra modalidad.
 Se muestra nivel.
 Se muestra publicador.
 Se muestra ubicación.
 Se muestran horarios.
 Se muestran imágenes o fallback.
 Se muestran botones de contacto.
 Slug inexistente muestra error o 404 controlado.
Página Publicar
 La página /publicar carga correctamente.
 Se entiende que está en desarrollo o próximamente.
 No rompe navegación.
 El diseño mantiene coherencia con el resto del sitio.
Página 404
 Una ruta inexistente muestra la página 404 personalizada.
 La página 404 permite volver a una sección válida.
 El diseño se mantiene prolijo.
Pruebas responsive

Probar en navegador usando herramientas de desarrollo:

 Mobile pequeño.
 Mobile grande.
 Tablet.
 Escritorio.
 Las cards se adaptan correctamente.
 Los filtros se usan cómodamente en mobile.
 El header no se rompe.
 El footer no se rompe.
 No hay scroll horizontal innecesario.
 Los botones son fáciles de tocar en mobile.
Pruebas de build
Frontend

Ejecutar:

npm run build

Validar:

 El build termina correctamente.
 No aparecen errores de TypeScript.
 No aparecen errores de ESLint.
 No aparecen errores por variables de entorno faltantes.
Backend

Ejecutar:

mvnw.cmd clean package

Validar:

 El build termina correctamente.
 Los tests base pasan.
 No aparecen errores de compilación.
 No se generan errores por configuración local.
Pruebas de Git y seguridad
 git status muestra el repo limpio.
 .env.local no está versionado.
 application-local.properties no está versionado.
 target/ no está versionado.
 node_modules/ no está versionado.
 .next/ no está versionado.
 No hay contraseñas en application.properties.
 Existe .env.example.
 Existe documentación en docs/.
Resultado esperado

El MVP se considera listo para mostrar cuando:

La base de datos está cargada.
El backend funciona correctamente.
El frontend funciona correctamente.
La búsqueda y filtros responden bien.
El detalle de actividad carga bien.
El diseño se ve bien en mobile y escritorio.
No hay errores visibles.
El repo está limpio.
La documentación está actualizada.
Notas

Este checklist se irá actualizando a medida que el proyecto sume nuevas funcionalidades como login, panel administrativo, favoritos, reseñas, métricas o publicación real de actividades.

## Resultado de pruebas ejecutadas

Fecha de validación: 08/06/2026

Se ejecutó una validación manual del MVP público con backend, frontend y base de datos corriendo en entorno local.

### Resultado general

- Backend local funcionando en `http://localhost:8080`.
- Frontend local funcionando en `http://localhost:3000`.
- Base de datos PostgreSQL conectada correctamente.
- Endpoint `GET /api/actividades` respondiendo con datos reales.
- Home cargando correctamente.
- Página Explorar cargando actividades desde el backend.
- Búsqueda funcionando correctamente.
- Filtros y ordenamiento funcionando correctamente.
- Página de detalle por slug funcionando correctamente.
- Ruta de actividad inexistente mostrando pantalla controlada.
- Página Publicar funcionando como formulario inicial del MVP.
- Página 404 personalizada funcionando correctamente.
- Responsive probado en mobile, tablet y escritorio.
- Build del backend finalizado correctamente.
- Build del frontend finalizado correctamente.
- Repositorio limpio y sin archivos sensibles versionados.

### Observaciones

- La página `Publicar` todavía no guarda datos en el backend. Actualmente funciona como interfaz inicial del flujo de publicación.
- En modo desarrollo, una actividad inexistente puede mostrar overlay de error de Next.js antes de la pantalla controlada. Para el MVP es aceptable, pero puede mejorarse más adelante usando `notFound()` o un manejo específico de errores.

### Estado

El MVP público queda validado manualmente y listo para la próxima etapa de preparación para deploy.


# Testing manual - DondeEntreno

Este documento registra las pruebas manuales realizadas sobre el MVP de DondeEntreno.

## Entornos probados

```text
Frontend local: http://localhost:3000
Backend local: http://localhost:8080
Frontend producción: https://donde-entreno-web.vercel.app
Backend producción: https://donde-entreno-api.onrender.com
Checklist general
Frontend
 La Home carga correctamente.
 El logo DondeEntreno se visualiza correctamente.
 El buscador se muestra correctamente.
 Las actividades destacadas se cargan.
 Las cards muestran título, deporte, ubicación y botón de detalle.
 El botón "Ver detalle" navega correctamente.
 La página Explorar carga correctamente.
 Los filtros se muestran correctamente.
 El ordenamiento funciona.
 La paginación funciona.
 El botón de limpiar filtros funciona.
 La página Publicar carga correctamente.
 La página 404 personalizada funciona.
 El footer se visualiza correctamente.
 El diseño se adapta correctamente a mobile.
Backend
 GET /api/actividades devuelve actividades.
 GET /api/actividades/{slug} devuelve una actividad existente.
 GET /api/actividades/{slug}/detalle devuelve el detalle.
 GET /api/actividades/{slug}/horarios devuelve horarios.
 GET /api/actividades/{slug}/imagenes devuelve imágenes.
 GET /api/filtros/opciones devuelve opciones para filtros.
 Los endpoints de catálogos responden correctamente.
Base de datos
 Las tablas principales existen.
 Los datos iniciales fueron cargados.
 Los datos de prueba fueron cargados.
 Las actividades publicadas aparecen correctamente.
 Los filtros tienen datos disponibles.
Integración
 El frontend local consume correctamente el backend local.
 El frontend online consume correctamente el backend online.
 El backend online se conecta correctamente a Supabase.
 CORS permite peticiones desde Vercel.
 Las rutas dinámicas de detalle funcionan en producción.
Pruebas de rutas principales
/
 /explorar
 /publicar
 /actividades/boxeo-recreativo-adultos-principiantes
 /actividades/actividad-inexistente
 /ruta-inexistente
Resultado actual
MVP full stack desplegado y funcionando correctamente.