# Plan — Fase 2: controles del publicador sobre sus imágenes

> Fase 2 del bloque visual (`docs/bloque-contenido-visual-v1.md`).
> Backend **sin migración** + su frontend. Este plan se presenta ANTES
> de codear (regla 8 de CLAUDE.md). Base técnica: el diagnóstico de
> Fase 0 (§A/§B del doc del bloque) — la tabla `imagen` ya tiene
> `orden`, `activa`, tipos y estados; faltan endpoints.

## Qué gana el publicador

Hoy: sube fotos y espera moderación; no puede ordenar la galería, ni
elegir cuál es la principal sin re-subir y re-moderar, ni borrar nada
ya aprobado, ni escribir el texto alternativo, y no hay ningún límite
de cantidad. Con esta fase: **ordena, promueve a principal, elimina,
describe y tiene límites sanos** — todo con las columnas que ya existen.

## 2.1 Eliminar imágenes APROBADAS (logo, portada y fotos de actividad)

- Hoy `DELETE .../imagenes/{id}` solo acepta PENDIENTE
  (`ImagenPublicadorService.retirarPendiente`). Se agrega la rama
  APROBADA → **baja lógica**: `activa=false` + `updatedAt` (la tabla no
  tiene `deleted_at`; las queries públicas ya filtran `activa=true`).
- **Storage**: borrar el objeto del bucket público **best-effort**
  (mismo criterio que ya usa el retiro de pendientes sobre el bucket
  privado). Si falla, la baja lógica avanza igual. El CDN puede servir
  la copia cacheada un rato — aceptado.
- Efectos ya soportados por el frontend: actividad sin PRINCIPAL → cae
  al fallback por deporte; perfil sin LOGO/PORTADA → iniciales y banda
  fina.
- Frontend: botón "Eliminar" con confirmación en `GestionImagenesPerfil`
  y `GestionImagenesActividad` (hoy solo muestran "Retirar" en
  pendientes).

## 2.2 Orden manual de la galería

- **`PUT /api/publicador/actividades/{id}/imagenes/orden`** con
  `{"imagenIds": [...]}`. Validación estricta: los ids deben ser
  EXACTAMENTE el conjunto de imágenes GALERIA activas de esa actividad
  (propia); se asigna `orden` 1..n en una transacción. Las queries
  públicas ya ordenan por `orden` — no se toca nada más.
- De paso se corrige **`calcularSiguienteOrden()`**: hoy cuenta TODAS
  las imágenes (rechazadas e inactivas incluidas) y puede duplicar
  valores; pasa a `max(orden)+1` sobre activas.
- Frontend V1: flechas subir/bajar por foto (accesible y sin librerías
  de drag; el drag puede venir con el Media Center de Fase 3).

## 2.3 Elegir la PRINCIPAL entre las aprobadas

- **`PUT /api/publicador/actividades/{id}/imagenes/{imagenId}/principal`**.
  Requisitos: imagen APROBADA + activa + tipo GALERIA + de esa
  actividad. **Sin re-moderación**: el archivo ya fue aprobado; cambiar
  el rol de una foto no cambia su contenido.
- Efecto **swap**: la elegida pasa a PRINCIPAL y la PRINCIPAL vigente
  baja a GALERIA (al final del orden). Nada se desactiva — no se pierde
  contenido.
- Frontend: botón "Hacer principal" en cada aprobada de la galería.

## 2.4 Límites de cantidad (hoy no hay NINGUNO)

- En la subida: **GALERIA activas por actividad ≤ 12**; pendientes de
  moderación por actividad ≤ 15; por perfil, 1 pendiente por tipo
  (LOGO/PORTADA) a la vez. Excedido → 400 con mensaje claro.
- Defaults en código con properties opcionales (patrón del asistente:
  la app arranca sin configurar nada): `dondeentreno.media.max-galeria`
  etc. **No hay que tocar Render** — aviso por la regla de env vars.

## 2.5 Título / texto alternativo editable

- **`PATCH /api/publicador/actividades/{id}/imagenes/{imagenId}`** con
  `{titulo?, descripcion?}` (≤150/≤255 — los límites del DDL; trim;
  vacío = limpiar). Los campos existen huérfanos desde el script 01 y
  el detalle público YA los usa como alt/caption — hoy siempre null.
- ⚠️ Es texto visible públicamente SIN moderación. Recomendación:
  permitirlo — texto plano (nunca se renderiza como link/HTML) y el
  mismo nivel de confianza que la descripción del perfil, que ya se
  edita sin moderación. Alternativa conservadora: solo editable
  mientras la imagen está PENDIENTE (viaja con la moderación), pero
  pierde el caso principal: describir lo ya aprobado.
- Frontend: campos inline en la gestión de imágenes.

## 2.6 Preview pública del perfil — ✅ HECHA (frontend-only)

"Ver mi perfil público" en el dashboard y en el editor de perfil,
solo con perfil activo (la ruta pública devuelve 404 para inactivos).
Es la vista PUBLICADA: lo pendiente de moderación no aparece ahí.

## Seguridad y contratos

- Todo bajo `/api/publicador/**` (ya `hasRole("PUBLICADOR")` en
  `SecurityConfig`); ownership con el patrón existente del service.
- **Aditivo puro**: ningún contrato público cambia; el `ImagenDTO`
  público no cambia de forma (solo empieza a traer `titulo`/
  `descripcion` con contenido cuando el publicador los cargue).
- Cero migración: columnas, CHECKs y estados quedan como están.

## Tests y despliegue

1. Unit tests de cada rama nueva del service + fix de
   `calcularSiguienteOrden`.
2. ITs contra PostgreSQL local (flujo completo: subir → aprobar →
   ordenar → promover → editar texto → eliminar → límites).
3. **Dos pushes** (regla aprendida): backend primero — marcador de
   build: `OPTIONS` de las rutas nuevas responde `Allow` con
   PUT/PATCH — verificado en producción; el frontend después.
4. Regla 12 antes del deploy de Render: backend local apagado (está) y
   Hikari chico (ya configurado en Render).
5. Smoke autenticado de Agustín con su cuenta de publicador: subir,
   ordenar, hacer principal, editar alt, eliminar, y validar que lo
   público refleje todo.

## Decisiones para aprobar (con recomendación)

1. **Borrar el archivo del bucket público** al eliminar una aprobada:
   SÍ, best-effort *(recomendado)* / solo baja lógica.
2. **Alt/título sin moderación**: SÍ *(recomendado)* / solo sobre
   pendientes.
3. **Límite de galería por actividad**: 12 *(recomendado)* / otro.
4. **"Eliminar" también para fotos de actividad aprobadas** (además de
   logo/portada): SÍ *(recomendado — mismo mecanismo)* / solo perfil.

## Fuera de esta fase

Ocultar/pausar actividad (Fase 6 — tiene efectos colaterales propios:
las 4 queries del panel, favoritos, feed, sitemap), nombre público
editable (Fase 5e — gate), secciones de galería (migración — F8),
Media Center UI completo (Fase 3, se apoya en estos endpoints).
