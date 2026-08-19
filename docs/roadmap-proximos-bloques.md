# Roadmap de próximos bloques (definido por Agustín, 2026-08-19)

> **Este documento orienta, no dispara trabajo**: ningún bloque de esta
> lista se implementa sin pedido explícito. Sirve para que cada bloque
> nuevo se elija con dirección y para chequear que ningún cambio vaya
> en contra de ella.

## El criterio estratégico

DondeEntreno **no** es una red social genérica. La dirección es:
**Instagram visual + Google Maps local + marketplace deportivo +
asistente IA**, siempre al servicio de una única acción principal:
**encontrar dónde entrenar**.

Antes de funciones sociales grandes (comentarios, mensajes privados,
publicaciones libres), priorizar: navegación clara, perfiles
confiables, guardados útiles, publicadores fuertes, contenido visual,
recomendaciones, sesión persistente, confianza y descubrimiento local.

## El ranking

1. **Mi Perfil / navegación por rol V2** — ✅ hecho en este bloque
   (`docs/mi-perfil-navegacion-v2.md`). "Mi perfil" es el espacio
   personal; "Espacio publicador" es una opción separada.
2. **Refresh token completo frontend + producción** — sesión
   persistente sin pérdidas molestas, con logout real y seguridad.
   *Nota de estado: el grueso ya está en producción con smoke cerrado
   (`docs/plan-refresh-token.md`); lo que quede acá es pulido fino de
   la experiencia de sesión, no arquitectura nueva.*
3. **Publicador UX V2** — espacio publicador más claro, guiado y
   profesional: perfil público, mis actividades, solicitudes, imágenes,
   métricas, checklist de presencia, guías para publicar mejor.
4. **Detalle de actividad premium** — imagen principal fuerte, galería,
   horarios claros, precio, publicador destacado, seguir, guardar,
   contactar, actividades similares, referencias futuras.
5. **Perfil público de publicador premium** — portada, logo,
   descripción, actividades, galería, seguir, WhatsApp, métricas
   reales, confianza/verificación.
6. **Home feed con más contenido real** — actividades nuevas,
   publicadores para seguir, deportes destacados, recomendaciones por
   ciudad, contenido visual real, menos sensación de landing.
7. **Guardados avanzados / colecciones** — "Para probar", "Cerca de
   casa", "Con amigos"; notas personales; comparar; ordenar por
   deporte, barrio o precio.
8. **Valoraciones y "Estoy entrenando acá"** — señales sociales propias
   de DondeEntreno: referencias, valoración simple, confianza
   comunitaria. Sin comentarios libres todavía.
9. **Slugs amigables + SEO** — `/publicadores/club-atletico-sur` en vez
   de `/publicadores/8` (necesita migración), corregir soft-404,
   mejorar SEO por deporte/ciudad/publicador, sitemap limpio.
10. **Mapa / cercanía / distancia** — mapa, actividades cerca,
    distancia aproximada, filtros por zona, eventualmente cómo llegar.

## Cruce con los pendientes técnicos ya conocidos

El ranking convive con los pendientes de CLAUDE.md que no son bloques
de producto: el motor local del asistente que no cede ante
barrios/días (A3), el contenido/imágenes reales (bloqueo del feed
visual, toca a los bloques 4–6), el hero con foto real, y el backend de
editar perfil de usuario. Al elegir bloque, mirar los dos listados.
