## Descripción del proyecto

DondeEntreno es una plataforma web que surge a partir de una idea simple: hacer más fácil el proceso de encontrar dónde entrenar.

La idea nació pensando en situaciones bastante comunes. Por ejemplo, una persona que nunca entrenó y quiere empezar alguna actividad física, pero no sabe qué deporte elegir ni dónde buscar. O alguien que llega a una ciudad nueva y quiere practicar un deporte específico, pero no conoce clubes, profesores, gimnasios o espacios disponibles cerca.

A mí también me pasó de querer encontrar dónde entrenar algo puntual y darme cuenta de que la información suele estar muy dispersa: publicaciones en redes sociales, recomendaciones de conocidos, perfiles incompletos, ubicaciones poco claras o datos difíciles de comparar.

DondeEntreno busca resolver ese problema centralizando la información en un solo lugar. La plataforma permite explorar actividades deportivas, ver dónde se realizan, conocer horarios, ubicaciones, referencias básicas y datos del publicador, para que encontrar una actividad sea un proceso mucho más simple, claro y accesible.

El objetivo del proyecto es ayudar tanto a quienes quieren empezar a moverse como a quienes ya practican algún deporte y buscan nuevas opciones dentro de una ciudad.

## Problema que resuelve

Encontrar dónde practicar un deporte o actividad física dentro de una ciudad no siempre es tan simple. Muchas veces la información está repartida entre redes sociales, Google Maps, recomendaciones personales o publicaciones aisladas.

Esto puede ser todavía más difícil para alguien que recién empieza, que no conoce el ambiente deportivo, que no sabe qué actividad elegir o que acaba de mudarse a una ciudad nueva.

DondeEntreno busca simplificar ese proceso reuniendo en una sola plataforma distintas opciones deportivas, permitiendo buscar y filtrar por ciudad, barrio, deporte, categoría, edad, nivel y horarios.

## Funcionalidades principales:

Visualización de actividades deportivas publicadas.
Búsqueda de actividades por texto.
Filtros por ciudad, barrio, deporte, categoría, edad, nivel y horario.
Página de detalle para cada actividad.
Información sobre ubicación, horarios, imágenes y datos del publicador.
Diseño responsive adaptado a mobile, tablet y escritorio.
Página inicial con actividades destacadas.
Página de exploración con filtros combinados.
Página provisoria para publicar actividades.
Manejo de rutas inexistentes con página 404 personalizada.
Backend con endpoints REST para actividades, filtros, catálogos y detalles.
Base de datos relacional con datos iniciales y datos de prueba.
Tecnologías utilizadas
Frontend
Next.js
React
TypeScript
Tailwind CSS
Vercel
Backend
Java 21
Spring Boot
Spring Data JPA
Maven
Docker
Render
Base de datos
PostgreSQL
Supabase
Herramientas generales
Git
GitHub
VS Code
IntelliJ IDEA
Postman / navegador para pruebas de endpoints
Arquitectura general
Usuario
  ↓
Frontend Next.js - Vercel
  ↓
Backend Spring Boot - Render
  ↓
Base de datos PostgreSQL - Supabase

El frontend consume los datos desde una API REST desarrollada con Spring Boot.
El backend se conecta a una base de datos PostgreSQL alojada en Supabase.
La aplicación está desplegada online usando Vercel para el frontend y Render para el backend.

Deploy online
Frontend:
https://donde-entreno-web.vercel.app

Backend:
https://donde-entreno-api.onrender.com

Endpoint principal:
https://donde-entreno-api.onrender.com/api/actividades
Estado actual del proyecto

El MVP full stack se encuentra desplegado y funcionando correctamente.

Actualmente permite explorar actividades deportivas, aplicar filtros, ver detalles de cada actividad y consumir datos reales desde una base PostgreSQL online.

Qué aprendí desarrollando este proyecto

Durante el desarrollo de DondeEntreno trabajé sobre varias partes importantes de una aplicación real:

Diseño de una base de datos relacional desde cero.
Creación de scripts SQL para tablas, datos iniciales y datos de prueba.
Desarrollo de una API REST con Java y Spring Boot.
Conexión entre backend y PostgreSQL.
Creación de un frontend moderno con Next.js y TypeScript.
Consumo de endpoints desde el frontend.
Manejo de filtros, paginación y rutas dinámicas.
Configuración de variables de entorno.
Deploy full stack usando servicios cloud.
Documentación técnica del proyecto.
Organización del proyecto en GitHub con commits separados y claros.
Próximas mejoras posibles
Crear sistema de login y registro.
Agregar panel de administración.
Permitir que clubes y profesores publiquen actividades.
Agregar subida de imágenes usando Supabase Storage.
Implementar favoritos o actividades guardadas.
Agregar métricas de visitas e interacciones.
Mejorar el SEO de las páginas dinámicas.
Agregar testing automatizado.
Incorporar funcionalidades con inteligencia artificial en una etapa futura.