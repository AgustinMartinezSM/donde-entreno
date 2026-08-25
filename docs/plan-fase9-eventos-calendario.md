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
