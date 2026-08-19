# Roadmap oficial de bloques (Agustín, 2026-08-19 — v2, 15 bloques)

> **Este es el orden oficial de trabajo.** Se trabaja sobre estos 15
> puntos antes de abrir frentes nuevos, salvo urgencia técnica o bug
> productivo. Ningún bloque se implementa sin pedido explícito. La v1
> de este doc (10 ítems) queda reemplazada por esta.

## El criterio estratégico

DondeEntreno **no** es una red social genérica. La dirección es:
**Instagram visual + Google Maps local + marketplace deportivo +
asistente IA**, siempre al servicio de una única acción principal:
**encontrar dónde entrenar**.

No sumar comentarios, mensajes privados ni publicaciones libres hasta
que estén sólidos: navegación, perfiles, fotos, guardados, sesiones,
publicadores, confianza, detalle, Home y señales sociales básicas.

## El ranking

1. **Mi Perfil / navegación por rol V2** — ✅ **CERRADO en producción**
   (`7099d14`, smoke OK; `docs/mi-perfil-navegacion-v2.md`). "Mi
   perfil" = espacio personal; "Espacio publicador" = opción separada.
2. **Refresh token completo frontend + producción** — ✅ grueso **en
   producción con smoke cerrado** (`75fa5c4`;
   `docs/plan-refresh-token.md`). Queda pulido fino de experiencia de
   sesión, no arquitectura.
3. **Contenido visual / Media Center / Fotos reales V1** — logos,
   portada, imagen principal, galerías por actividad, orden, estados,
   aprobación, previews, fallbacks. *Diagnóstico hecho en
   `docs/bloque-contenido-visual-v1.md` (Fase 0).*
4. **Modo oscuro base** — apariencia Sistema/Claro/Oscuro, azul noche
   de marca, sin negro puro.
5. **Login UX V1** — redirección a Home para usuario/publicador,
   placeholders, ojito mostrar/ocultar, errores humanos. *Implementado
   como Fase 1 del bloque visual (ver doc del bloque).*
6. **Perfil usuario editable** — foto de perfil, nombre visible,
   ciudad/preferencias, deportes, apariencia. *Incluye el sub-bloque de
   seguridad de cuenta: verificación de email, cambiar contraseña,
   recuperar contraseña (diagnóstico en el doc del bloque, §4 BIS).*
7. **Perfil publicador editable** — nombre visible, descripción, logo,
   portada, contacto, preview pública, identidad.
8. **Gestión de actividades por publicador** — editar título/
   descripción, imagen principal, galería, horarios/precio,
   ocultar/pausar/mostrar/archivar, sin borrado destructivo sin diseño.
9. **Publicador UX V2** — espacio claro, guiado y profesional: perfil
   público, actividades, solicitudes, imágenes, métricas, checklist,
   guías.
10. **Detalle de actividad premium** — imagen fuerte, galería, horarios
    claros, precio, publicador destacado, seguir, guardar, contactar,
    similares, referencias futuras.
11. **Perfil público de publicador premium** — portada, logo,
    descripción, actividades, galería, seguir, WhatsApp, métricas,
    confianza/verificación.
12. **Home feed con contenido real** — actividades nuevas, publicadores
    para seguir, deportes destacados, recomendaciones por ciudad,
    contenido visual real, menos landing.
13. **Guardados avanzados / colecciones** — colecciones, notas,
    ordenar, comparar, "para probar", "cerca de casa".
14. **Likes en fotos / señales sociales** — likes en fotos, ordenar por
    populares, reportar foto, compartir. Sin comentarios abiertos.
15. **Valoraciones, "Estoy entrenando acá", SEO, mapa y futuros** —
    valoraciones/referencias, slugs amigables, SEO, mapa/cercanía,
    videos; comentarios solo cuando haya moderación.

## Cruce con pendientes técnicos

Siguen vivos fuera del ranking: el motor local del asistente que no
cede ante barrios/días (A3 de CLAUDE.md), el hero con foto real de Mar
del Plata (necesita asset aprobado), y las deudas menores de CLAUDE.md
§E. Al elegir bloque, mirar los dos listados.
