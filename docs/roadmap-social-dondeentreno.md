# Roadmap social DondeEntreno (etapa "app deportiva social local")

Estado: **oficial desde 2026-08-24** (prompt maestro de Agustín).
Reemplaza como norte al roadmap anterior de bloques
(`docs/roadmap-proximos-bloques.md`, que queda como histórico).
Auditoría de base: `docs/auditoria-social-dondeentreno.md`.
Normas: `docs/normas-comunidad-draft.md`.

## Visión

DondeEntreno evoluciona a **app deportiva social local** — Instagram
visual + Google Maps local + reviews + WhatsApp + Dondi — sin perder
el foco: **ENCONTRAR DÓNDE ENTRENAR Y DECIDIR CON CONFIANZA**.

Regla de diseño transversal: **todo elemento social termina en una
acción útil** (ver actividad, seguir, guardar, consultar, probar).
Nada de social por el social mismo.

## Filosofía de moderación (decisión de producto, 2026-08-24)

- **Estricto (se mantiene)**: aprobación de actividades nuevas;
  intervención de admin/superadmin (eliminar/ocultar/pausar) ante
  incumplimiento; publicadores falsos; contenido grave.
- **Flexible (nuevo)**: fotos, comentarios, likes, novedades, stories,
  chats, grupos → **publicación directa + reportes + intervención**,
  no preaprobación. Esto REEMPLAZA el flujo actual de moderación
  previa de fotos (cambio deliberado, se implementa en su fase).
- **Prerrequisito duro de todo texto libre**: normas públicas +
  sistema de reportes + capacidad admin de ocultar/eliminar + estados
  (visible / oculto_por_publicador / eliminado_por_admin /
  eliminado_por_usuario / reportado). Sin esa base, no se abre.
- Mitigaciones de escala (moderación humana = 1 persona):
  auto-ocultar tras N reportes, rate limits por usuario, bloqueo,
  y palabras ocultas a futuro.

## Estado de las 51 funciones objetivo (resumen; detalle en auditoría)

- **Ya existen**: seguir publicadores (6), likes en fotos (9, en
  lightbox), "estoy entrenando acá" (25, con deshacer), URLs amigables
  de actividades y publicadores (41), OG metadata dinámico (42),
  compartir con Web Share (24-V1), colecciones de guardados, social
  proof del detalle, Dondi.
- **Parciales**: feed (1: seguidos top-20 + Para vos, sin eventos
  sociales ni paginación), perfil publicador (3: falta Opiniones/
  Novedades/stats), galerías (4: falta grilla única, secciones,
  comentarios, guardar), stories (19: accesos visuales, no contenido),
  skeletons (46), modo oscuro (49, público completo), landings
  deporte/ciudad (13, con soft-404).
- **Inexistentes**: comentarios, reportes, notificaciones, chats,
  grupos, canales, valoraciones, Q&A, "quiero probar"/"ya probé",
  mapa/cerca mío, eventos, calendario, invitaciones, match,
  comparador, rankings, guías, bandeja de consultas, respuestas
  rápidas, perfil de calidad, estadísticas con tracking, destacados,
  PWA, configuración, campanita, alertas, normas, menciones, guardar
  fotos, referidos, QR.

## Fases (orden recomendado)

Ajustes razonados sobre el orden sugerido en el prompt:
**(a)** Notificaciones internas se adelantan (eran Fase 11): son
dependencia de comentarios, Q&A, valoraciones, canales y alertas — sin
campanita, cada feature social nace muda. **(b)** PWA + skeletons +
normas se adelantan a la Fase 1 por ser quick wins frontend-only que
además son prerrequisito (normas) de todo lo social.

### Fase 0 — Auditoría social ✅ HECHA (2026-08-24)
Este documento + la auditoría + el draft de normas.

### Fase 1 — Base de convivencia, configuración y app instalable
✅ **CERRADA EN PRODUCCIÓN** (smoke de Agustín OK 2026-08-24; `42bccd5`):
normas/términos/privacidad en el footer, `/configuracion` V1 con
buscador y secciones por rol, PWA instalable (manifest + íconos +
iOS), skeletons con Header real en Home/Explorar/detalle/perfil.
**Regla de producto nueva del smoke (estilo Instagram)**: la
configuración se maneja EN la configuración — los datos y ajustes se
editan inline ahí, no con links que te llevan a otra sección. La
edición de datos del usuario entra en Fase 2; las secciones que hoy
linkean afuera (deportes, guardados) migran a inline a medida que sus
funciones lo permitan.
Alcance original:
- Páginas **/normas, /terminos, /privacidad** desde el draft, en el
  footer.
- **Centro de Configuración V1** (`/configuracion`): hub con secciones
  Cuenta / Perfil deportivo / Apariencia / Publicador (por rol) /
  Seguridad / Ayuda y normas — reordena lo que YA existe (editar
  datos, avatar, contraseña, tema, cerrar sesión, perfil publicador)
  sin romper `/mi-cuenta`. Mobile: accordion + buscador local.
  Desktop: sidebar + contenido. Estructura preparada para las
  secciones futuras (privacidad, notificaciones, chats).
- **PWA base**: manifest + íconos + theme color + apple-touch +
  safe areas verificadas; install prompt; SW básico después.
- **Skeletons sistemáticos**: Home, detalle, perfil publicador,
  galerías.
- **Menú Más** (desktop) con Configuración/Ayuda/Normas/Cerrar sesión.

### Fase 2 — Infraestructura social mínima (backend + migraciones)
✅ **CERRADA EN PRODUCCIÓN** (smoke de Agustín OK 2026-08-24; backend
`0a48544` + frontend `992e171`, script 28): campanita con contador y
panel, emisores en los 5 flujos existentes (con fan-out a seguidores),
reportes con cola admin (/admin/reportes), tracking anónimo
(vistas/WhatsApp/compartir → métricas del publicador 30 días) y
edición inline de nombre/apellido en /configuracion. Alcance original:
- **Notificaciones internas V1**: tabla `notificacion` (usuario_id,
  tipo, título, ruta, leída, created_at), campanita con contador,
  panel (drawer desktop / pantalla mobile), marcar leídas. Sin email
  ni push.
- **Reportes V1**: tabla `reporte` genérica (tipo_objeto, objeto_id,
  usuario_id, motivo, estado) + panel admin. Es EL prerrequisito de
  la moderación flexible.
- **Tracking de interés V1**: tabla `evento_interaccion` (actividad,
  tipo: click_whatsapp / click_mapa / vista_detalle, created_at, sin
  usuario o con usuario opcional) — alimenta estadísticas del
  publicador y rankings. Barato y muy valioso: hoy no se mide nada.

### Fase 3 — Confianza: valoraciones, Q&A y el flujo propio
✅ **CERRADA EN PRODUCCIÓN** (smoke de Agustín OK 2026-08-24; backend
`b94de22`+`0afa8b3`, frontend `0f1c57e`, script 29): flujo quiero
probar → ya probé → valorar con insignia Verificada, valoraciones 1-5
con reseña directa + tags + promedio con N≥3, Q&A con respuesta del
dueño y notificaciones cruzadas, reportes de valoración/pregunta,
social proof con estrellas y métricas con "quieren probar". Lección
nueva: SMALLINT en la base exige @JdbcTypeCode en la entidad o
validate no arranca (lo cazó el IT). Alcance original:
- **"Quiero probar"** (misma receta que check-in) y **"Ya probé"**.
  Flujo propio: Guardé → Quiero probar → Ya probé → Valoro / Entreno acá.
- **Valoraciones 1-5** (etapa B del diseño previo, ya pensada): solo
  cuentas con señal de uso (favorito/checkin/ya-probé) = **opinión
  verificada**; única por usuario+actividad, editable; promedio
  visible con N≥3; tags rápidos; distribución de estrellas.
  Comentario de texto de la reseña: entra con reportes ya vivos.
- **Q&A por actividad** (estilo MercadoLibre): pregunta pública +
  respuesta del publicador; borrar propia si no respondida; reportes.
- Calificación por aspectos: solo diseñada (columna JSON o tabla
  aparte), no implementada.
- Notificaciones: nueva pregunta / respuesta / valoración.

### Fase 4 — Galería social y moderación flexible de fotos
✅ **CERRADA EN PRODUCCIÓN** (smoke de Agustín OK 2026-08-24; script 30
aplicado por él; backend `3b59493` + frontend `2517c39`). Detalle y
hallazgos en
`docs/plan-fase4-galeria-social.md` — incluido el que destaparon los
ITs: al sacar la compuerta previa, `rechazar()` seguía exigiendo
PENDIENTE y el admin quedaba **sin ninguna forma de bajar una foto
reportada**; la baja reactiva y las acciones directas del panel de
reportes entran en el mismo bloque.

- **Cambio de flujo**: fotos de publicadores publican DIRECTO
  (estado aprobado al subir), con reportes + ocultar por admin +
  ocultar por publicador. El circuito de moderación previa queda como
  herramienta admin, no como compuerta.
- **Comentarios en fotos** de publicadores (publicar directo, estados
  completos, desactivar por foto, ocultar por publicador, eliminar
  propio, admin todo). Rate limit por usuario.
- **Guardar fotos** + vista de guardadas.
- **Secciones de galería** (diseño ya escrito: catálogo fijo).
- **Grilla única del publicador** (todas sus fotos).

### Fase 5 — Perfil publicador Instagram-like completo
✅ **CERRADA EN PRODUCCIÓN** (smoke de Agustín OK 2026-08-24; script 31
aplicado por él; backend `cfc36e0` + frontend `8118a36`). Plan y
hallazgos en `docs/plan-fase5-perfil-publicador.md`.

- Cabecera con stats reales (actividades, seguidores, fotos, promedio),
  cada uno navegando a su solapa y **ninguno dibujado sin dato**.
- Tab **Opiniones**: valoraciones de TODAS sus actividades + preguntas
  ya respondidas, cada una con link a su actividad. **"Novedades" NO
  entró a propósito**: la tabla `feed_event` es de la Fase 6 y los
  canales del publicador de la Fase 8 — un Novedades V1 acá habría
  sido una tabla condenada a ser reemplazada.
- Actividades **destacadas manuales** (hasta 3, orden elegido).
- WhatsApp del perfil **con tracking propio** (`perfil_publicador_id`,
  `actividad_id` NULL): antes ese botón no registraba nada.
- Endpoint agregado de fotos del publicador (mata el fan-out de hasta
  6 requests por vista).
- **Pendiente que quedó fuera**: el "perfil de calidad" (checklist %
  completado) sigue viviendo solo en el Centro de fotos, no se movió a
  componente compartido ni se mostró en el perfil.

### Fase 6 — Feed social V2
✅ **CERRADA EN PRODUCCIÓN** (smoke de Agustín OK 2026-08-24; script 32
aplicado por él; backend `72f00fc` + frontend `bc73be7`). Plan y
hallazgo en `docs/plan-fase6-feed-social.md`.

- Tabla `feed_event` poblada por los flujos existentes (actividad
  aprobada, foto publicada, cambio aprobado), con **backfill** de las
  actividades ya publicadas para que el feed no naciera vacío.
- Feed **paginado** con "Ver más" que acumula (cierra la deuda del
  top-20 fijo). Card por tipo de hecho.
- **"Lo más visto" real** derivado del tracking de la Fase 2: la home
  tenía seis deportes hardcodeados llamados "populares". Si no hay
  señal suficiente (mínimo 3 deportes), vuelve a la selección curada.
- **Deuda saldada de paso**: `HomeFeedSeguidos` duplicaba el fetch del
  hook existente (un logueado pedía el feed DOS veces al pasar por
  home y /mi-cuenta) y `EsqueletoCard` estaba copiado en dos archivos.
- **⚠️ Hallazgo grande**: emitir eventos con `REQUIRES_NEW` **rompía la
  subida de fotos con un 500** — ver el plan. Los eventos que
  referencian filas de la misma transacción van en `afterCommit`.
- **Fuera de alcance a propósito**: mezclar recomendaciones y
  descubrimiento DENTRO del feed. La home ya los tiene como secciones
  separadas y fundirlos borraría el valor de seguir a alguien.

### Fase 7 — Cercanía (el mapa quedó para después)
✅ **CERRADA EN PRODUCCIÓN** (smoke de Agustín OK 2026-08-24; script 33
aplicado por él; backend `9411d42` + frontend `0ae069d`). Plan y
hallazgos en `docs/plan-fase7-mapa-cercania.md`.

- **Carga de coordenadas**: el publicador pega el link de Google Maps
  (sin geocoding: es impreciso en direcciones argentinas y un pin mal
  puesto manda gente al lugar equivocado). El resolutor prioriza el
  punto del LUGAR sobre el de la cámara.
- **"Cerca mío"** con geolocation, radios 1/3/5/10 km y distancia. La
  ubicación del usuario no se guarda ni se loguea.
- Las actividades sin punto quedan fuera **y se informa cuántas**:
  ubicarlas en el centro del barrio inventaría precisión.
- **"Cómo llegar"** (link, no mapa embebido: abre la app nativa).
- **Zonas por barrio**, que de paso hacen navegable el barrio.
- **EL MAPA NO ENTRÓ, a propósito**: con 4 de 9 sedes sin punto
  mostraría más huecos que pines, y sería la primera dependencia de UI
  del proyecto. Se retoma cuando haya coordenadas cargadas.
- **Cero dependencias nuevas**: el frontend sigue con next/react/
  react-dom.
- ⚠️ Bug que llegó a producción y su lección (IT por endpoint nuevo):
  ver el plan.
- **Pendiente**: sacar "cerca mío" de las stopwords del buscador — hoy
  quien lo escribe recibe en silencio una búsqueda sin cercanía.

### Fase 8 — Comunidad (en este orden)
1. ✅ **Canales de novedades CERRADOS EN PRODUCCIÓN** (smoke de Agustín
   OK 2026-08-24; script 34 aplicado antes del deploy; backend
   `112d537` + frontend `ce67fab`). Broadcast: solo el publicador
   escribe. Tope de 3 por día (el primer límite sobre el rol
   PUBLICADOR) y campanita solo en la primera del día. **Las
   reacciones entraron después**, el 2026-08-25 (script 37, `3a2fc93`):
   un "me gusta" por novedad, en el perfil y en el feed. **Los eventos
   NO llevan reacción**: ya tienen "Me interesa", que es más accionable
   (`docs/plan-reacciones.md`). De paso destapó y arregló un bug que dejaba el feed sin
   guardar NINGÚN evento desde la Fase 6 (ver
   `docs/plan-fase8-canales-novedades.md`).
2. ✅ **Inbox de consultas CERRADO EN PRODUCCIÓN** (smoke de Agustín OK
   2026-08-25; script 36 aplicado antes del deploy; backend `07e7fbd` +
   frontend `48e2c7a`). "Consultar sin dar tu teléfono" al lado del
   WhatsApp —nunca en su lugar—, bandejas para los dos lados, leídos y
   cierre por parte del usuario. **Sin realtime**: polling de 30 s solo
   en el hilo abierto y con la pestaña visible (Render free tier hace
   spin-down; un WebSocket sería una promesa incumplible).
   **La decisión de privacidad quedó estructural**: no existe endpoint
   que devuelva un hilo completo a un admin — de un mensaje reportado
   ve ese mensaje y a lo sumo dos anteriores, y lo fija un IT.
   **Las respuestas rápidas guardadas NO entraron**: son Fase 11.
   Pendiente que importa: escribir la promesa de privacidad en
   `/privacidad` y `/normas` (ver `docs/plan-inbox-consultas.md`).
3. ✅ **Grupos por actividad CERRADOS EN PRODUCCIÓN** (smoke de Agustín
   OK 2026-08-25; script 38 aplicado antes del deploy; backend
   `e76c89e` + frontend `4975ae5`). El publicador avisa, los miembros
   comentan y reaccionan. **Se entra explícitamente** —el check-in no
   suma a nadie—, el grupo es **privado de sus miembros** (el contenido
   no sale del backend para quien no lo es, ni para un admin) y el
   dueño de la actividad lo ve porque es quien lo modera. Detalle en
   `docs/plan-grupos-actividad.md`.
4. Chat libre de grupos (V2) y realtime (V3, evaluar Supabase
   Realtime) — solo con la moderación probada. **La condición para
   reabrir V2 no es que pase tiempo: es que haya grupos VIVOS** y se
   vea cómo se comportan con el tablón moderado que ya existe.
- Landing social por deporte+ciudad (arregla de paso el soft-404).

### Fase 9 — Eventos y calendario
✅ **CERRADA EN PRODUCCIÓN** (smoke de Agustín OK 2026-08-25; script 35
aplicado antes del deploy; backend `832e437` + frontend `3d84704`):
`evento_deportivo` (clase abierta, torneo, seminario), "me interesa"
con contador, compartir, y presencia en home / perfil / feed;
calendario `/eventos` con rangos hoy / finde / semana / próximos
resueltos en el backend en zona argentina.
**Quedó fuera a propósito**: la **imagen OG generada por evento** (el
compartir con título y descripción propios sí entra; la imagen es una
superficie nueva con Satori y se puede sumar sin tocar el modelo), la
**recurrencia** (eso ES una actividad con sus horarios) y la **reserva
de cupo** (el cupo se muestra, el contacto sigue siendo WhatsApp).

### Fase 10 — Descubrimiento inteligente
- Match deportivo (test guiado + Dondi), comparador (sobre guardados,
  frontend), rankings semanales (del tracking), "para arrancar de
  cero", guías deportivas (editorial/SEO; pueden adelantarse — no
  dependen de nada).

### Fase 11 — Publicador Pro y monetización futura
- Bandeja de consultas unificada (Q&A + inbox + clicks), respuestas
  rápidas activables, estadísticas por actividad (del tracking),
  tabla `actividad_destacada` (inicio/fin, ubicación del destacado,
  prioridad, manual por superadmin — SIN cobrar), QR del publicador
  (puede adelantarse: es frontend puro).

### Fase 12 — Multimedia avanzada
- Stories reales de publicadores (expiran/archivan), reels (V1 por
  EMBED según diseño previo; subida nativa solo con presupuesto),
  compartir como historia (imagen generada), feed de fotos recientes
  (Explore).

## Crecimiento y monetización (transversal, sin cobrar ahora)

- **Quick wins**: QR del publicador (frontend), OG image generada
  linda, compartir como historia V2.
- **Referidos** (diseño): código por usuario, tabla `referido`
  (invitador, invitado, hitos), premiar hitos reales (perfil completo,
  primer guardado, primer seguido, primera valoración, PWA instalada)
  — no cuentas vacías. Insignias antes que dinero.
- **Monetización futura** (solo estructura): destacados manuales
  primero → planes pro de publicador después (más fotos/videos,
  stats avanzadas, badge) → publicidad local controlada. **Nunca**
  cobrar al usuario común ni bloquear funciones básicas.
- Embajadores locales: insignia/ranking, sin dinero al inicio.

## Reglas técnicas de la etapa (permanentes)

- Nada se implementa sin plan aprobado por bloque; push solo con
  aprobación; Supabase solo Agustín; secrets jamás; dominio/DNS/email
  sigue PAUSADO.
- Migraciones: aditivas, idempotentes, PRE/POST, local antes que
  Supabase, SIEMPRE antes que el código que las usa.
- No romper: login/refresh, roles, Dondi, sync, galerías, publicador,
  admin, actividades aprobadas, modo oscuro, navegación mobile.
- Checks por bloque: backend `test` + `verify -Pintegration-local` +
  `clean package`; frontend typecheck+lint+build; smoke por rol ×
  ancho × tema; accesibilidad (contraste, foco, aria, sin overflow).
- Realtime/video/push: nunca sin plan de costos.

## Próximo bloque recomendado

**Fase 1 completa** ("Base de convivencia, configuración y app
instalable"): normas + configuración V1 + PWA base + skeletons + menú
Más. Es 100% frontend (cero migración, cero riesgo de backend), da el
salto de "web" a "app" (PWA), destraba el prerrequisito legal/social
de TODO lo que viene (normas), y ordena la casa antes de sumar
notificaciones/mensajes. Después, Fase 2 (notificaciones + reportes +
tracking), que es la infraestructura de la que depende el resto.
