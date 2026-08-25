# Reacciones a novedades (plan propuesto)

Estado: **PROPUESTO**, pendiente de aprobación de Agustín.
Origen: pendiente que la Fase 8 dejó fuera a propósito ("se deciden con
novedades reales ya publicadas").

## Relevamiento (verificado en código)

- **Los likes ya tienen patrón**: `me_gusta_imagen` (una fila por
  usuario+foto, sin estados), `LikesFotosService` con `dar`/`quitar`, y
  `PUT|DELETE /api/usuario/likes-fotos/{id}`. El contador viaja en
  `ImagenDTO.cantidadLikes`, agregado por setter.
- **Los likes de fotos NO notifican a nadie** (cero referencias a
  `notificacion` en su service). Es un precedente, no un olvido.
- **Los eventos YA tienen "Me interesa"** (`interes_evento`, Fase 9)
  con contador público y estado propio.
- Las **novedades no tienen ninguna forma de responder**: se leen y
  listo.

## La propuesta, y lo que deja afuera

### 1. Reacciones SOLO en novedades. En eventos, NO

El pedido decía "novedades y eventos", pero el evento **ya tiene una
reacción**: "Me interesa", con su contador, que además es más
accionable —le dice al publicador cuánta gente piensa ir— que un
"me gusta".

Sumar un segundo botón al evento traería **dos contadores compitiendo
en la misma card** ("12 interesados · 4 me gusta") sin que ninguno de
los dos signifique algo más claro que antes, y le pediría a quien mira
que decida entre dos formas de responder casi iguales.

**Recomiendo dejar el evento como está.** Si más adelante se ve que
"Me interesa" es una barrera muy alta para un simple gesto de apoyo,
ahí sí se evalúa — con datos de uso.

### 2. Una sola reacción ("Me gusta"), no un set de emojis

Un set (👍❤️🔥😮) multiplica la tabla (hay que guardar cuál),
el contador (uno por tipo), la UI (un selector) y las decisiones de
producto (¿cuáles?, ¿se pueden combinar?). Con el volumen actual no
aporta nada que un solo gesto no resuelva.

**Recomiendo una sola reacción**, con el mismo patrón que las fotos: la
fila existe o no existe.

### 3. Sin notificación al publicador

Los likes de fotos no notifican, y es la decisión correcta: una
novedad con veinte "me gusta" serían veinte campanitas por algo que no
pide respuesta. El publicador ve el número cuando entra.

**Recomiendo mantener ese precedente.** La campanita se reserva para lo
que pide una acción (una consulta, un comentario, un reporte).

## Alcance (V1)

1. Script 37: `me_gusta_novedad` (usuario + novedad, UNIQUE), mismo
   molde que `me_gusta_imagen`. Aditivo puro: **no toca ningún CHECK**.
2. `PUT|DELETE /api/usuario/novedades/{id}/me-gusta`, idempotentes,
   devolviendo el contador actualizado.
3. `NovedadDTO` suma `cantidadMeGusta` y `meGusta` (por setter,
   aditivos), resueltos con query agrupada en las dos superficies donde
   se ven novedades: el perfil público y el feed.
4. Botón en la novedad del perfil y en su card del feed. Anónimo va al
   login con `returnTo`, igual que guardar una foto.

## Qué NO entra

- **Reacciones en eventos** (ver recomendación 1).
- **Set de emojis** (ver 2).
- **Notificación por reacción** (ver 3).
- **Lista de quién reaccionó**: el contador es agregado y anónimo, que
  es lo que ya promete `/privacidad` para los contadores sociales.

## Verificación y deploy

1. Unit (idempotencia, quitar, contador) + **IT propio**: reaccionar
   suma y aparece en el perfil y en el feed; repetir no duplica; quitar
   resta; una novedad oculta no acepta reacciones.
2. Script 37 (Agustín) → ITs → backend → frontend → su smoke.
