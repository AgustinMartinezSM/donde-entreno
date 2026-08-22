# Freno de tareas pendientes — 2026-08-22

El barrido completo que pidió Agustín tras cerrar los bloques 12, 13 y
14. Objetivo: ver TODO lo abierto en un solo lugar y decidir qué
mejorar después. Nada de esta lista se implementa sin pedido.

## Dónde está parada la app hoy

- **Bloque visual completo** (fases 0–8), **roadmap 1–5 cerrados**, y de
  los grandes: 12 (Home feed real), 13 (colecciones), 14 (likes en
  fotos) **en producción con smoke**. Pausar/reanudar, avatar de
  usuario, nombre editable del publicador, cambio de contraseña, visor
  de fotos con likes: todo vivo.
- **465 unit + 74 ITs verdes**; 4 migraciones aplicadas este mes (19,
  20, 21, 22, 23).
- **En pausa por decisión de producto**: dominio propio + email
  transaccional completo (`docs/pendiente-dominio-y-email.md`).

## A. Ajustes UX detectados por Agustín (2026-08-22) — EN CURSO

1. **Identidad única de avatar/logo** → implementado en este mismo
   freno: `actualizarUsuario` en el provider + `AvatarUsuario`
   compartido (header, sheet, barra inferior, cabecera) y
   `perfilLogoUrl`/`logoUrl` del backend en cards, feed, sugeridos,
   seguidos, detalle y listados — todo sale de la misma consulta batch.
2. **Nombre del club ilegible (blanco sobre claro)** → causa raíz
   encontrada e implementada: `from-[#FAFDFF]` hardcodeado en la card
   social quedaba claro en modo oscuro bajo tinta clara. Ahora es
   token (`--color-surface-soft`) y se invierte con el tema. Era el
   "fantasma" de las capturas de Agustín del pulido 7d.
3. **Botón volver arriba** → rediseñado: gradiente de marca, icono SVG,
   siempre montado con transición suave de opacidad/desplazamiento,
   `shadow-lifted` y foco accesible. Posiciones (coreografía con
   Dondi) intactas.

## B. Producto / UX — lo más valioso de lo abierto

- **Contenido real**: sigue siendo EL bloqueo del feed visual — pocas
  actividades con fotos, pocos logos de publicador cargados. No es
  código: es carga de contenido (y el Centro de fotos ya guía).
- **Perfil público de usuario** (hoy no existe): prerequisito para que
  likes/check-ins muestren personas. Si se encara, la moderación del
  avatar se diseña ANTES (condición escrita en 5d).
- **Fase 8 diseñada y lista para pedir**: check-in "Estoy entrenando
  acá", valoraciones (etapas A/B), secciones de galería, video por
  embed. Orden recomendado en `docs/fase8-diseno-futuro.md`.
- **Roadmap 9** (Publicador UX V2 guiado) quedó parcialmente cubierto
  por F3+checklist; lo que falta es fino: guías, textos, onboarding del
  publicador nuevo.
- **Solicitudes de cambio**: horarios, ubicación, deporte, edades y
  enfoque siguen SIN vía de edición (ni directa ni moderada). Es el gap
  más señalado del panel.

## C. Visual / marca

- **Hero con foto real de Mar del Plata** (necesita asset aprobado por
  Agustín — el salto visual más grande pendiente).
- 9 deportes sin badge de historias; ilustraciones de cards viejas.
- `favicon.ico` legacy (necesita tooling .ico).
- Wordmark horizontal claro para el header en dark (asset).
- Stories con discos blancos en dark (aceptado V1).
- Decidir destino de `assets/` originales (~22MB untracked).

## D. Técnico / deuda

- **Fan-out N+1 del perfil público** (fotos por actividad): pide
  endpoint público agregado. Anotado desde F4.
- **Soft-404 de landings dinámicas** (`docs/nota-soft-404-landings.md`):
  la condición para arreglarlo ya se cumple; impacto SEO nulo hoy.
- **Slug amigable de perfiles** (`/publicadores/club-x`): migración.
- Paginación del feed de seguidos (top 20 fijo).
- IT del orden del feed con >1 actividad; data de QA en producción
  (conservar o limpiar); normalización WhatsApp `54` fijo.
- Menores del asistente (sanitizador sin patrón de disponibilidad;
  "dónde veo mis imágenes" cae al fallback).
- localStorage del refresh: se reevalúa recién con dominio propio.

## E. Recomendación de próximos 3 (si Agustín pide)

1. **Solicitudes de cambio completas** (horarios/ubicación al menos):
   es el reclamo funcional más concreto del publicador.
2. **Valoraciones etapa A + check-in** (fase 8): confianza en el
   detalle con datos reales, sin moderación de texto.
3. **Hero real + badges faltantes** cuando haya assets: el mayor salto
   visual por peso invertido.
