# Auditoría social DondeEntreno — estado real del código (2026-08-24)

Auditoría hecha sobre el código real (entidades, controllers, rutas y
componentes verificados en el repo; nada asumido por memoria). Base del
roadmap social (`docs/roadmap-social-dondeentreno.md`).

Leyenda: ✅ completo · 🟡 parcial · ❌ inexistente.

## A. Feed social deportivo — 🟡 PARCIAL

- La Home mobile ya se siente semi-social: fila de stories (badges de
  deportes → catálogo real), feed de seguidos, "Para vos"
  (recomendaciones por ciudad + deportes del usuario), publicadores
  sugeridos, chip "Nueva" (≤14 días) en cards.
- **Backend propio solo para el feed de seguidos**:
  `GET /api/usuario/feed/actividades` (FeedController) devuelve
  actividades de publicadores seguidos, **top 20 fijo, sin paginación**
  (deuda documentada). El resto de la Home es composición frontend
  sobre los listados públicos.
- **No hay eventos sociales** ("X subió fotos", "actualizó horarios"):
  no existe entidad `feed_event` ni nada equivalente. El feed muestra
  actividades, no acontecimientos.
- Personalización: por seguidos y por deportes preferidos (sync
  script 20). No hay señales de popularidad ni cercanía.

## B. Seguir publicadores — ✅ COMPLETO

- Endpoints `/api/usuario/seguimientos/publicadores` (seguir, dejar,
  listar, estado), sincronizado por usuario (JWT), con IT propio.
- Contador público de seguidores (query agrupada, sin N+1).
- Botón Seguir/Siguiendo en perfil público y en el detalle de
  actividad; tab Siguiendo en `/mi-cuenta`; sección de novedades en
  Home; hook `useSeguimientos` compartido.
- **Falta**: notificaciones de novedades (bloqueado por I) y stats
  públicas más ricas.

## C. Perfil público del publicador — 🟡 AVANZADO

- Ruta `/publicadores/[slug]` con **slug estable** (script 27,
  2026-08-24; id sigue funcionando y redirige). Canonical + OG
  metadata + sitemap con slug.
- Tiene: portada, logo/avatar (identidad única con fallback a
  iniciales), nombre, tipo, verificado, descripción, ciudad, botón
  Seguir, Compartir, contador de seguidores, tabs **Actividades /
  Fotos / Info** (enlazables por `?tab=`), galería agregada (fotos de
  sus actividades + LOGO/PORTADA/GALERIA del perfil), contacto en Info.
- **Falta para ser Instagram-like**: tab Opiniones (no hay
  valoraciones), tab Novedades (no hay canales), stats de fotos y
  valoraciones en la cabecera, actividades destacadas, eventos, mapa.

## D. Galerías — 🟡 AVANZADO (por actividad), 🟡 (por perfil)

- Por actividad: subida múltiple, moderación (PENDIENTE→APROBADA/
  RECHAZADA + panel admin), PRINCIPAL/GALERIA con **orden manual**,
  "Hacer principal", eliminar, texto por foto (Fase 2 de controles),
  carrusel en el detalle, **LightboxFotos** (dialog nativo) con
  **likes** (corazón + contador real, script 23).
- Por perfil: LOGO/PORTADA/GALERIA propias
  (`/api/publicador/perfil/imagenes` + editor de encuadre) y grilla
  agregada en el perfil público. Centro de fotos (`/publicador/fotos`).
- La tabla `imagen` ya tiene orden, estados, tipos (Fase 0 previa) y
  las **secciones de galería están diseñadas** (fase 8: catálogo fijo
  INSTALACIONES/ENTRENAMIENTOS/EVENTOS/EQUIPO, columna nueva).
- **Falta**: comentarios, reportes, guardar foto, secciones
  implementadas, y la **decisión nueva de moderación flexible** (hoy
  toda foto pasa por aprobación previa; la filosofía nueva pide
  publicar directo + reportes — es un cambio de flujo deliberado).

## E. Social (likes, comentarios, reportes, etc.)

- ✅ Likes en fotos (script 23, `me_gusta_imagen`, idempotente,
  contador público batch, verificado en producción).
- ✅ Social proof del detalle (script 26): guardados + likes +
  "N personas entrenaron acá este mes" (distintos, 30 días).
- ✅ "Estoy entrenando acá" (script 26): check-in 1/día validado
  contra la base, con deshacer, botón en la barra del detalle.
- ✅ Colecciones y notas por guardado (script 22).
- ❌ Comentarios (ni entidad ni UI). ❌ Reportes (nada). ❌ Q&A.
  ❌ Valoraciones. ❌ "Quiero probar" / "Ya probé". ❌ Grupos/chats.
  ❌ Menciones. ❌ Guardar fotos. ❌ Notificaciones.
- Verificado por listado real de entidades: no existen Notificacion,
  Comentario, Chat, Grupo, Mensaje, Evento, Pregunta ni Valoracion.

## F. Mapa / ubicación — 🟡 MÁS CERCA DE LO ESPERADO

- **La tabla `ubicacion` YA tiene `latitud`/`longitud`**
  (NUMERIC(9,6), del schema original) — verificado en la entidad.
  Presumiblemente **sin datos cargados** (nada las llena hoy) y **sin
  exposición** en DTOs públicos.
- Sí hay: ciudad, barrio, dirección pública, referencia. El buscador
  filtra por barrio.
- Para mapa real falta: cargar lat/lng (geocoding por dirección o
  carga manual del publicador), exponerlas en la API, y elegir librería
  (Leaflet + OpenStreetMap = gratis; Google Maps = key + costos).
- "Cerca mío" V1 sin mapa: geolocation del navegador + distancia
  Haversine contra lat/lng — **sin guardar la ubicación del usuario**.
- V0 posible ya: "zonas" por barrio (sin coordenadas).

## G. PWA / SEO

- **PWA: ❌ INEXISTENTE.** No hay `manifest` (ni archivo ni
  `manifest.ts`), no hay service worker, no hay apple-touch-icon ni
  íconos de instalación. Hay favicon PNG de marca.
- SEO: ✅ sitemap dinámico con dominio real, ✅ robots, ✅ URLs
  amigables de actividades (slug) y de publicadores (script 27),
  ✅ OG metadata dinámico en actividades y perfiles (con la foto real
  si existe; ilustración por deporte si no). ❌ imagen OG generada
  (tipo card visual). 🟡 landings por deporte/ciudad: existen como
  rutas dinámicas pero con el **soft-404 conocido**
  (`docs/nota-soft-404-landings.md`); no hay landing "social" por
  deporte+ciudad.

## H. Configuración / Ajustes — ❌ NO HAY CENTRO

- No existe `/configuracion` ni `/ajustes`. Hoy: `MenuAjustes` dentro
  de `/mi-cuenta` (datos de cuenta solo lectura, avatar, cambiar
  contraseña, cambiar ciudad, cerrar sesión), `SelectorTema` repetido
  en los tres menús de cuenta, `menuCuenta.ts` como fuente única de
  menús por rol (personal / publicador / administración).
- Base sólida para armar el hub: las piezas existen, falta el lugar.

## I. Notificaciones — ❌ INEXISTENTE TOTAL

- Ni entidad, ni endpoint, ni campanita, ni panel, ni contador.
  Es **dependencia transversal** de comentarios, Q&A, canales,
  valoraciones y alertas.

## J. Home social mobile — 🟡 BUENA BASE

- Stories arriba (accesos visuales por deporte con badges reales),
  feed visual central, bottom nav **Inicio · Explorar · Guardados ·
  Mi perfil**, Dondi flotante (izquierda) + volver arriba (derecha)
  ya conviven sin pisarse, sheet de cuenta con dialog nativo.
- Sin mensajes, sin campanita, sin buscador superior dedicado en Home
  (la búsqueda vive en Explorar y en el Hero).

## Otros de la lista corta

- **Modo oscuro**: ✅ público completo (Fase 7 cerrada con smoke +
  pulido posterior); deuda V1: áreas internas (admin/publicador/
  publicar) con **luz forzada** a propósito; auditar superficies
  nuevas al crearlas.
- **Skeleton loaders**: 🟡 parciales (8 componentes con pulse:
  favoritos, feed, sugeridos, nav) — no sistemáticos (detalle, perfil,
  galerías y Home completa sin skeleton).
- **Normas / legal**: ❌ nada (ni rutas ni footer link). Draft nuevo
  en `docs/normas-comunidad-draft.md`.
- **Métricas / analytics**: las métricas del publicador son **solo
  conteos** (actividades, pendientes, seguidores, fotos en
  moderación). **No hay tracking de vistas ni de clicks** (WhatsApp,
  mapa, llamar) en ninguna parte del frontend. No hay analytics de
  terceros.
- **Compartir**: ✅ `CompartirButton` (Web Share API con fallback)
  en cards, detalle y perfil. ❌ pieza visual tipo story, ❌ QR.
- **Destacados / monetización**: ❌ nada (ni entidad ni campo).
- **Dondi**: ✅ funcionando con Gemini + motor local en cascada;
  insumo listo para match deportivo y "para arrancar de cero".

## Qué requiere qué (resumen)

| Necesita | Funciones |
|---|---|
| **Solo frontend** | Normas/términos/privacidad, Centro de Configuración V1, PWA base (manifest+íconos), skeletons, QR publicador, comparador (sobre guardados), buscador de configuración, menú Más, zonas por barrio V0 |
| **Backend sin migración** | Paginación del feed, flexibilizar moderación de fotos (cambio de flujo), OG image generada, exponer lat/lng si hubiera datos |
| **Migración + backend** | Notificaciones, comentarios, reportes, valoraciones, Q&A, "quiero probar"/"ya probé", guardar fotos, canales de novedades, grupos, chat/inbox, eventos, alertas, destacados, feed_event, respuestas rápidas, clicks/vistas (tracking), lat/lng con datos |
| **Servicios externos** | Geocoding (mapa exacto), librería de mapas (OSM gratis), push (futuro), email (PAUSADO), video/reels (storage+costos), realtime (Supabase Realtime/WebSocket) |

## Riesgos principales

1. **Texto libre sin infraestructura de moderación** (comentarios,
   chats, grupos): el primer insulto sale con el logo de la app
   arriba. Prerrequisitos duros: normas públicas + reportes + panel
   admin + estados de ocultamiento. La regla del bloque anterior
   ("comentarios explícitamente NO sin circuito") se reemplaza por la
   filosofía nueva **solo si** esa base mínima existe primero.
2. **Realtime**: websockets/polling agresivo contra Render free tier
   (una instancia chica, pooler de 15). Chat V1 debe ser inbox con
   polling suave, no realtime.
3. **Video**: storage y egress de Supabase + moderación de video.
   V1 = embeds (ya diseñado); subida nativa recién con límites y
   presupuesto.
4. **Moderación humana = una sola persona** (Agustín). Los flujos
   deben degradar solos: auto-ocultar tras N reportes, límites de
   frecuencia, bloqueo por usuario.
5. **Privacidad**: todo lo social público debe ser agregado y anónimo
   salvo opt-in (regla ya aplicada en check-in; extenderla a "quiero
   probar", valoraciones con nombre = decisión explícita).
6. **Deriva de foco**: cada feature social debe terminar en una acción
   útil (ver actividad, seguir, guardar, consultar). Regla de diseño
   del roadmap.
