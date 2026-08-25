# Fase 9 — Eventos y calendario (plan propuesto)

Estado: **PROPUESTO**, pendiente de aprobación de Agustín.
Roadmap: `docs/roadmap-social-dondeentreno.md` (Fase 9).

## Qué problema resuelve

Todo lo que hoy publica DondeEntreno es **permanente y recurrente**:
una actividad con horarios semanales. No hay forma de contar que el
sábado 12 hay un torneo, una clase abierta gratis o un seminario con
un profesor invitado. Eso es justamente lo que tiene fecha, lo que
caduca y lo que la gente comparte.

Además le da al feed —que recién ahora guarda eventos de verdad, tras
el fix de la Fase 8— el tipo de contenido que más engancha: algo que
pasa pronto y que uno se puede perder.

## Relevamiento (verificado en código, no de memoria)

- **`horario_actividad` es recurrencia semanal**: `dia_semana` +
  `hora_inicio`/`hora_fin`, sin fecha. Un evento puntual **no entra
  ahí** ni estirando el modelo: tabla nueva.
- **`interes_actividad` ya existe** (script 29) con el flujo
  `QUIERO_PROBAR` → `YA_PROBE` sobre actividades, y
  `entrenamiento_usuario` con el check-in "entrené acá" (script 26).
- **Ya hay dos cosas llamadas "evento" en el código**: `EventoInteraccion`
  (tracking anónimo de clicks) y `FeedEvent` (el log del feed). Un
  tercer `Evento` a secas sería una trampa de lectura permanente.
- **`feed_event.tipo` y `notificacion.tipo` no tienen CHECK** a
  propósito: sumar un tipo nuevo es código puro, sin migración.
- **`reporte.tipo_objeto` SÍ enumera**: cada objeto reportable nuevo
  cuesta reescribir el CHECK (ya pasó en los scripts 29, 30 y 34).
- **El sitemap no lista actividades individuales** y hay **una sola OG
  image global** (`app/opengraph-image.tsx`); no existe OG por recurso.

## Alcance propuesto (V1)

1. **Entidad `evento_deportivo`** (script 35): cuelga **siempre de un
   perfil publicador** y **opcionalmente de una actividad** ya
   publicada. Campos: título, descripción, `inicia_at` / `termina_at`
   (timestamptz), ubicación (reusa `ubicacion`, con las coordenadas de
   la Fase 7), cupo opcional, precio opcional con `mostrar_precio`,
   `es_gratis`, estado (`PUBLICADO` / `CANCELADO` /
   `OCULTO_POR_ADMIN` / `ELIMINADO_POR_PUBLICADOR`), imagen opcional
   elegida entre las ya publicadas (mismo patrón que la novedad).
2. **`/publicador/eventos`**: crear, editar, cancelar y borrar.
   Publicación **directa**, sin cola de moderación.
3. **`/eventos`**: el calendario público con rangos **Hoy / Este finde
   / Esta semana / Próximos**, más filtros de ciudad y deporte
   (reusando el catálogo real, no uno espejo).
4. **`/eventos/[slug]`**: el detalle, con "Me interesa", contador de
   interesados, cómo llegar (Fase 7), contacto con tracking y
   `BotonReportar` tipo `EVENTO`.
5. **Superficies existentes**: card del evento en el feed
   (`EVENTO_NUEVO`), solapa "Eventos" en el perfil público del
   publicador (solo si hay), bloque "Esta semana" en la home, y aviso
   en el detalle de la actividad cuando tiene un evento próximo.
6. **Notificación a seguidores** al publicar, con el mismo cuidado que
   la Fase 8 (ver recomendación 4).

## Decisiones que pide este plan (con recomendación)

### 1. Publicación DIRECTA, no cola de moderación admin

La filosofía vigente es "estricta solo para actividades nuevas,
flexible para todo lo social". Un evento se parece a una actividad
—tiene lugar, precio y horario— así que la tentación es moderarlo.
**Recomiendo publicación directa igual**, por una razón práctica que
no tiene vuelta: **un evento caduca**. Un torneo del sábado que espera
aprobación hasta el lunes no es un evento moderado, es un evento
perdido, y la función entera deja de usarse.

El riesgo real está acotado: para publicar un evento hay que **ser
publicador con al menos una actividad aprobada**, o sea que ya pasó el
filtro humano una vez. Y se cubre por atrás con reportes + ocultar por
admin, igual que fotos, comentarios y novedades.

### 2. El evento cuelga del PERFIL, y la actividad es opcional

Colgarlo de una actividad resolvería deporte, ubicación y contacto de
una sola vez, pero deja afuera el caso más típico: **el torneo del
club**, que no es "una clase de karate" sino algo del club entero.
Colgarlo solo del perfil, en cambio, obliga a re-pedir el deporte.

**Recomiendo perfil obligatorio + actividad opcional**: si el evento
se asocia a una actividad, hereda deporte y ubicación (y aparece en el
detalle de esa actividad); si no, el publicador elige deporte y sede
entre las suyas. Es una columna nullable, no dos modelos.

### 3. Nombre `evento_deportivo` / `EventoDeportivo`, no `Evento`

Ya hay `EventoInteraccion` (tracking) y `FeedEvent` (log del feed).
Un `Evento` a secas obliga a mirar el import cada vez que se lee una
línea, para siempre. **Recomiendo el nombre largo**: se escribe una
vez y se lee mil.

### 4. Notificación al publicar, pero con tope y sin recordatorio en V1

Misma trampa que la Fase 8: `NotificacionService` no agrupa. Un
publicador con 50 seguidores que carga la agenda del mes de una
sentada dispara cientos de avisos.

**Recomiendo**: notificar al publicar, con **tope de 2 eventos con
campanita por día** (los demás entran igual al feed), y **sin
recordatorio "es mañana"** en V1 — ese recordatorio necesita un job
programado, que hoy **no existe en el proyecto** (no hay scheduler ni
cron) y en Render free tier con spin-down por inactividad es una
promesa que se incumple sola. Merece su propia decisión de infra.

### 5. Sin recurrencia y sin inscripción en V1

- **Recurrencia**: la cubre `horario_actividad`. Un evento que se
  repite todas las semanas **es una actividad**, y duplicar el
  concepto ensucia los dos. Un evento es puntual.
- **Inscripción/cupo con reserva**: el cupo se muestra como dato
  (“20 lugares”), pero **no se reserva**. Reservar implica estados,
  cancelaciones, no-shows y una discusión de pagos. El contacto sigue
  siendo WhatsApp, que es lo que ya funciona y ya se mide.

Lo que sí entra es **"Me interesa"** (tabla propia `interes_evento`,
una fila por usuario+evento, sin los estados de `interes_actividad`:
`QUIERO_PROBAR`/`YA_PROBE` no significan nada acá) con **contador
público**, que es la prueba social barata.

## Qué NO entra (y por qué)

- **OG image dinámica por evento**. El roadmap pide "invitaciones por
  WhatsApp con OG". Compartir con título y descripción propios entra;
  la **imagen** generada por evento es una superficie nueva (Satori,
  que además no soporta `var()` — ya está documentado) y conviene
  hacerla cuando existan eventos reales para mirar. Se puede sumar
  después sin tocar el modelo.
- **Eventos pasados**: no se borran ni se archivan. Dejan de listarse
  en el calendario y el detalle sobrevive, porque el link ya circuló
  por WhatsApp.
- **Eventos de usuarios** (no publicadores): otra discusión entera de
  confianza y moderación.

## Verificación y deploy

1. Unit (tope de campanita, cancelar ≠ borrar, el evento pasado sale
   del calendario, "me interesa" idempotente, reporte de evento) +
   **IT propio del endpoint**, con el camino feliz completo: publicar →
   aparece en el calendario, en el feed del seguidor y en el perfil →
   me interesa suma → cancelarlo lo saca del calendario pero deja vivo
   el detalle. **Cada aserción de desaparición prueba antes la
   aparición** (regla de la Fase 8), y el IT ejerce **el camino real de
   emisión del feed**, no inserta `feed_event` a mano (que es cómo se
   escapó el bug de la Fase 6).
2. Script 35 (Agustín) → ITs → backend → frontend → su smoke.

---

## Estado: IMPLEMENTADO (2026-08-25), pendiente script 35 + ITs + deploy

Las 5 recomendaciones fueron aprobadas y están las cinco en código.
Dos commits, en el orden de los dos pushes.

### Backend

- `database/scripts/35_eventos_deportivos.sql` — **PENDIENTE de aplicar
  en Supabase y local**. Aditivo salvo el CHECK de `reporte.tipo_objeto`.
- `EventoDeportivo`, `InteresEvento`, sus repositorios,
  `EventoDeportivoDTO`, `EventoDeportivoService`, `EventoSlugService`.
- `GET /api/eventos` (calendario con rango), `GET /api/eventos/{slug}`,
  `GET /api/eventos/de-actividad/{id}`,
  `GET /api/perfiles-publicadores/{id}/eventos`,
  `GET|POST /api/publicador/eventos`,
  `PATCH /api/publicador/eventos/{id}/cancelar`,
  `DELETE /api/publicador/eventos/{id}`,
  `PUT|DELETE /api/usuario/eventos/{id}/interes`,
  `PATCH /api/admin/eventos/{id}/ocultar`.
- `FeedEventService`: `TIPO_EVENTO_NUEVO` + `emitirEvento(...)`, y el
  enriquecido descarta el evento cuyo `evento_deportivo` ya no está
  PUBLICADO (un cancelado también se cae del feed: anunciarlo de nuevo
  a quien nunca lo vio sería avisar de algo que no va a pasar).
- `SecurityConfig`: `/api/eventos/**` público con JWT **opcional**.

### Frontend

- `services/eventosService.ts`, `components/eventos/{EventoCard,BotonMeInteresa}`,
  `app/eventos/page.tsx`, `app/eventos/[slug]/page.tsx`,
  `components/publicador/AgendaDeEventos.tsx`,
  `app/publicador/eventos/page.tsx`, `components/home/HomeEventos.tsx`.
- Superficies existentes: card de evento en el feed (con la fecha
  destacada y su chip de urgencia), solapa "Eventos" en el perfil
  público, "Ocultar el evento" en la cola de reportes, entradas en
  `menuCuenta.ts` y en el dashboard, y `/eventos` en el sitemap.
- `formatoFecha.ts` suma `formatearFechaEvento` y `formatearCuantoFalta`.
  Función aparte y no un caso más de `formatearFechaRelativa`: esa
  aplasta todo el futuro a "hoy" a propósito, y de un evento lo único
  que importa es exactamente cuándo es.

### Detalle que apareció al implementar

**La hora que tipea el publicador es la que se guarda.** El formulario
arma el ISO con el offset del dispositivo en vez de usar
`toISOString()`, que convierte a UTC y guardaría otra hora escrita.

### Verificación hecha

- 579 unit tests verdes (12 nuevos en `EventoDeportivoServiceTest`).
- Frontend: typecheck + lint limpios y build con 45 rutas, incluidas
  `/eventos`, `/eventos/[slug]` y `/publicador/eventos`.

### Lo que falta

1. **Agustín aplica el script 35** en Supabase y local.
2. `verify -Pintegration-local` — `EventosCalendarioIT` cubre el camino
   feliz completo, cancelar vs borrar, "me interesa" idempotente, el
   filtro de rango, el ocultar del admin, la fecha pasada y el 401.
   Ejerce **el camino real de emisión del feed** (publica por HTTP, no
   inserta `feed_event` a mano) y cada aserción de desaparición prueba
   antes la aparición — las dos lecciones de la Fase 8.
3. Deploy en dos etapas y smoke de Agustín.

---

## Estado: EN PRODUCCIÓN EN DOS TANDAS (2026-08-25), pendiente el smoke

`main` = `origin/main` = **`3d84704`**. Script 35 aplicado por Agustín
en Supabase y local ANTES del deploy.

**Tanda 1 — backend `832e437`** (`058d485` la fase + `832e437` el fix
del calendario; historia reordenada con worktree para que todo el
backend viajara primero, verificando que el árbol final quedara
idéntico).

Verificado en producción **por comportamiento, no solo por estado**:

- `GET /api/eventos` → **200** con la página bien formada. (Marcador
  real: **401 → 200**, no 404 → 200 como se había anunciado —
  cualquier ruta desconocida cae en `authenticated()`. Es la trampa ya
  documentada en las fases 6 y 8.)
- `?rango=cuandosea` → **400 con el mensaje propio** ("El rango tiene
  que ser hoy, finde, semana o proximos"): prueba que el resolutor de
  rango nuevo se está ejecutando, no que la ruta existe.
- `/api/eventos/torneo-que-no-existe` → **404 con su mensaje propio**.
- Los tres rangos válidos responden 200 — incluido `proximos`, que era
  justamente el que tiraba 500.
- Las tres privadas nuevas (POST publicador, PUT interés, PATCH admin)
  dan 401 anónimas; catálogo público y novedades de la Fase 8 intactos.

**Tanda 2 — frontend `3d84704`.** Marcador `/eventos` **404 → 200**
(ruta pública: acá el middleware no lo enmascara). Verificado en el
navegador en producción: la agenda carga con su estado vacío correcto,
cambiar de rango cambia el texto ("No hay eventos en ese rango" + "Ver
todos los próximos"), la **home NO dibuja** la sección de eventos
—medido en el DOM, que es lo que promete el componente—, el perfil
público no muestra la solapa Eventos, las privadas dan 307, `/eventos`
está en el sitemap y la consola está limpia.

### Lo que falta

**El smoke de Agustín con su cuenta de publicador.** Sugerido, en este
orden:

1. `/publicador/eventos` → crear uno para esta semana (probá con
   actividad asociada y sin ella).
2. Verlo en `/eventos`, y que el chip de urgencia diga lo correcto
   (hoy / mañana / en N días).
3. Verlo en la home ("Lo que se viene"), que recién ahora aparece.
4. Con otra cuenta: verlo en el feed y marcar **Me interesa** (el
   contador tiene que subir y quedar marcado al recargar).
5. Compartir el link por WhatsApp y mirar la previsualización.
6. **Cancelarlo**: sale del calendario y del feed, pero el link
   compartido sigue abriendo y avisa que se canceló.
7. Borrar otro: ese sí desaparece del todo.
8. Como admin: reportar un evento y ocultarlo desde `/admin/reportes`.
