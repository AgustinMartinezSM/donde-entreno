# Inbox de consultas usuario ↔ publicador (plan propuesto)

Estado: **PROPUESTO**, pendiente de aprobación de Agustín.
Roadmap: `docs/roadmap-social-dondeentreno.md` (Fase 8, punto 2).

## Qué problema resuelve

Hoy **todo el contacto se va a WhatsApp**. Eso tiene tres costos que ya
se pueden nombrar:

1. **Exige dar el teléfono.** Es la barrera más alta del producto para
   una primera consulta: "¿cuánto sale?" no debería costar entregar un
   dato personal a un desconocido.
2. **No deja rastro para el publicador.** No tiene historial, ni forma
   de retomar una consulta de hace una semana, ni de responder
   ordenado. Lo que se pierde en el chat personal, se perdió.
3. **No se puede medir nada después del click.** El tracking registra
   que alguien tocó "Contactar" y ahí se corta la historia.

## Relevamiento (verificado en código)

- **No existe NADA de mensajería**: ninguna entidad, ningún endpoint.
  Es un módulo nuevo completo.
- Lo más parecido es **`pregunta_actividad`** (script 29): pública, de
  **un solo ida y vuelta** (campo `pregunta` + campo `respuesta`) y
  anclada a una actividad. No sirve como base de una conversación, y
  **no se toca**: son cosas distintas (una es vidriera, la otra es
  privada).
- **`NotificacionService.emitir`** ya existe para avisar a UNA persona
  (además del fan-out), así que la campanita del mensaje nuevo no
  necesita infraestructura nueva.
- Los topes por día ya tienen patrón en el proyecto
  (`MAX_COMENTARIOS_POR_DIA = 20`, `MAX_NOVEDADES_POR_DIA = 3`).
- `reporte.tipo_objeto` **enumera**: sumar `MENSAJE` cuesta migración.

## Alcance propuesto (V1)

1. **`conversacion`** (script 36): un usuario ↔ un perfil publicador,
   con `actividad_id` **opcional** para saber de qué le están hablando.
   Una conversación viva por par (usuario, publicador, actividad).
2. **`mensaje`**: autor (`USUARIO` / `PUBLICADOR`), texto, `leido_at`,
   estado para moderación.
3. **Bandeja del usuario** en `/mi-cuenta` y **bandeja del publicador**
   en `/publicador/consultas`, ambas con contador de no leídos.
4. **Hilo**: enviar, marcar leído al abrir, notificación al otro lado.
5. **Reportar un mensaje** + acción del admin (ocultarlo).
6. **Entrada desde el detalle de la actividad y el perfil**, al lado
   del botón de WhatsApp (ver recomendación 4).

## Decisiones que pide este plan (con recomendación)

### 1. El admin NO lee conversaciones: solo el mensaje reportado

Esta es **la decisión de privacidad** que se viene difiriendo, y
conviene cerrarla antes de escribir una línea.

La opción simple es que el admin vea los hilos completos desde el panel
(más fácil de moderar). **Recomiendo la contraria**: sería el único
lugar del producto donde una persona puede leer lo que dos personas se
escriben **en privado**, y la moderación es unipersonal.

Concretamente: el reporte guarda el `mensaje_id`, y el panel muestra
**ese mensaje y a lo sumo los dos anteriores del hilo** —contexto
mínimo para poder juzgarlo— y nada más. El resto de la conversación no
se expone en ninguna pantalla ni endpoint de admin. Y se dice en
`/privacidad` y en `/normas`, porque una promesa de privacidad que no
está escrita no existe.

### 2. La conversación la inicia SIEMPRE el usuario

El publicador **no puede escribir en frío**. Sin esta regla, el inbox
se convierte en un canal para escribirle a cualquiera que haya mirado
su perfil, y el primer mensaje no solicitado destruye la confianza en
la bandeja entera.

El usuario, además, puede **cerrar** la conversación: deja de recibir
notificaciones y el publicador no puede seguir escribiendo ahí.

### 3. Sin realtime, y con polling honesto

Render free tier hace **spin-down por inactividad**: WebSockets serían
una promesa que se incumple sola (mismo argumento que dejó afuera el
recordatorio de eventos en la Fase 9).

**Recomiendo**: nada de polling global. Dentro de un hilo abierto,
consulta cada ~30 s y **solo con la pestaña visible**
(`document.visibilityState`); fuera del hilo, avisa la campanita que ya
existe. Un polling cada 5 segundos en todas las pantallas es la forma
más rápida de convertir el free tier en un servicio caído.

### 4. WhatsApp sigue siendo el CTA principal

El inbox se ofrece **al lado**, con un texto que dice para qué sirve:
"Consultar sin dar tu teléfono". Mover el CTA principal a un canal
donde todavía no hay nadie respondiendo sería apostar lo único que hoy
convierte a algo sin uso probado. Si con el tiempo el inbox demuestra
mejor respuesta, se cambia con datos.

### 5. Topes desde el día uno

Texto libre, privado y sin fricción es exactamente donde entra el spam.
**Recomiendo**: **5 conversaciones nuevas por día** por usuario y **20
mensajes por día** por usuario; el **publicador sin tope para
responder** dentro de conversaciones existentes, porque responder es
justamente lo que queremos que pase.

## Qué NO entra (y por qué)

- **Adjuntar fotos**: abre otra cola de moderación con reglas propias,
  sobre contenido que nadie más va a ver. V1 es texto.
- **"Visto a las 14:32"**: el `leido_at` se guarda para el contador,
  pero **no se muestra la hora al otro lado**. Genera una expectativa
  de respuesta inmediata que un club no puede cumplir.
- **Respuestas rápidas guardadas**: es Fase 11 (Publicador Pro).
- **Chat de grupos y realtime**: fases propias.

## Verificación y deploy

1. Unit (topes, el publicador no inicia, cerrar corta el hilo, leídos,
   reporte de mensaje) + **IT propio**: usuario consulta → aparece en
   la bandeja del publicador y en su campanita → responde → le llega al
   usuario → el usuario cierra → el publicador ya no puede escribir.
   Cada aserción de desaparición prueba antes la aparición.
2. **Un IT específico de privacidad**: que el endpoint de admin
   **no** devuelva el hilo completo, y que un usuario ajeno reciba 404
   —no 403— al pedir una conversación que no es suya.
3. Script 36 (Agustín) → ITs → backend → frontend → su smoke.

---

## Estado: IMPLEMENTADO (2026-08-25)

Las 5 recomendaciones aprobadas y en código. Dos commits, en el orden
de los dos pushes.

### Backend

- `database/scripts/36_inbox_consultas.sql` (aplicado por Agustín antes
  del deploy), `Conversacion`, `Mensaje`, sus repositorios,
  `ConversacionDTO`, `MensajeDTO`, `InboxService`.
- `GET|POST /api/usuario/consultas`, `GET /api/usuario/consultas/{id}`,
  `PATCH /api/usuario/consultas/{id}/cerrar`,
  `GET /api/publicador/consultas`, `GET /api/publicador/consultas/{id}`,
  `POST /api/publicador/consultas/{id}/respuestas`,
  `PATCH /api/admin/mensajes/{id}/ocultar`,
  `GET /api/admin/mensajes/{id}/contexto`.
- `ReporteService` +`MENSAJE`.

**La privacidad quedó estructural, no declarativa**: no existe método
ni endpoint que devuelva un hilo completo a un admin. Lo fija el IT.

### Detalles que aparecieron al implementar

- **La unicidad del hilo son DOS índices parciales**, no un `UNIQUE`:
  en Postgres los NULL no colisionan entre sí, así que con
  `actividad_id` nulo un UNIQUE normal habría permitido infinitos hilos
  con el mismo club. Por lo mismo, buscar el hilo existente necesita
  una `@Query` explícita: un `findBy...` derivado genera
  `= :actividadId` y NULL nunca es igual a NULL.
- **Las consultas del usuario son página propia** (`/mi-cuenta/consultas`)
  y no una quinta solapa: la grilla de solapas es de cuatro porque a
  375px no entran más.
- **El lint atajó dos bugs reales** en el componente del hilo: una ref
  tocada durante el render y un `setState` en cascada disparado desde
  un efecto. Se corrigieron alineando al patrón del resto del proyecto
  (carga inicial dentro del propio efecto, con bandera de montaje).

### Verificación

- 590 unit + 131 ITs verdes, **al primer intento**.
- `InboxConsultasIT` (8 casos) incluye los dos de privacidad:
  **ni un admin puede abrir un hilo** (403 en el endpoint del
  publicador, 404 en el del usuario) y **el contexto de un reporte
  nunca trae la conversación entera** — el hilo tiene cuatro mensajes,
  el contexto devuelve como mucho tres, y el test **prueba primero que
  los cuatro existan**.
- Frontend: typecheck + lint + build con las dos rutas nuevas.

### Marcador de deploy del backend

`OPTIONS /api/usuario/consultas`: **404 → 200 con `allow`**. Verificado
con control contra una ruta que ya existía (`/api/usuario/favoritos`,
que da 200 con `allow: GET,HEAD,OPTIONS`), porque este bloque **no
agrega ninguna ruta pública** y todos los 401 son indistinguibles entre
builds.

---

## ✅ INBOX CERRADO EN PRODUCCIÓN (smoke de Agustín OK, 2026-08-25)

`main` = `origin/main` = **`48e2c7a`**. Script 36 aplicado por él antes
del deploy. Backend `07e7fbd` + frontend `48e2c7a`.

**Tanda 1 — backend.** Marcador: `OPTIONS /api/usuario/consultas` pasó
de **404 a 200 con `allow: GET,HEAD,POST,OPTIONS`** —justo los métodos
del endpoint nuevo—, verificado además contra una ruta preexistente
como control. Hizo falta este marcador porque **el módulo no agrega
ninguna ruta pública** y todos los 401 son idénticos entre builds.
Post-deploy: catálogo y eventos intactos, y las tres rutas nuevas en
401 anónimo.

**Tanda 2 — frontend.** Marcador: el texto "Consultar sin dar tu
teléfono" en el HTML del perfil público (ahí el botón se renderiza para
cualquiera). Las dos rutas privadas dan 307 anónimo.

### Lo que quedó pendiente, anotado

- **Decir la promesa de privacidad en `/privacidad` y `/normas`.** El
  código ya la cumple —no existe endpoint que devuelva un hilo completo
  a un admin— pero las páginas todavía no la mencionan, y una promesa
  de privacidad que no está escrita no existe para quien la lee. Es el
  pendiente más importante de este bloque.
- **Contador de no leídos en la campanita/navegación**: hoy el aviso
  llega por notificación, pero la bandeja no muestra un badge global.
- **Respuestas rápidas guardadas** (Fase 11) y **adjuntar fotos**:
  fuera de alcance a propósito.

---

## La promesa de privacidad, escrita (2026-08-25, `2dd6f26`)

El pendiente más importante que había dejado el bloque queda cerrado:
`/privacidad` tiene la sección **"Mensajes privados con publicadores"**
y `/normas` explica cómo se modera algo que nadie más ve.

Cada afirmación está sostenida por `InboxService` y fijada por
`InboxConsultasIT`. **Si alguna de esas reglas cambia, estas páginas
cambian con ella** — quedó dicho en un comentario arriba de la sección.

Lo que deliberadamente **NO** se prometió, porque el código no lo
cumple: borrar mensajes propios (no existe) y borrado automático de las
conversaciones al dar de baja la cuenta (no hay endpoint de baja: se
pide por email, como la página ya decía). En su lugar se dice que los
mensajes se conservan mientras exista la cuenta.

Verificado en producción por contenido: las frases "no lee las
conversaciones", "los dos anteriores" y "No ve tu email ni tu
teléfono" están en el HTML servido de `/privacidad`, y "no las leemos"
y "límite diario" en el de `/normas`.

---

## Contador de no leídos (2026-08-25, backend `bd8126e` + frontend `8297428`)

Cierra el segundo pendiente del bloque. `GET /api/usuario/consultas/contador`
y su par del publicador, una query cada uno, y badge en las dos
entradas de menú (desktop y sheet mobile).

**Sin poller nuevo, a propósito.** Cada mensaje ya genera una
notificación, así que el aviso lo da la campanita —que ya consulta cada
60 s— y un segundo poller global sería duplicar tráfico de fondo para
un número que solo importa cuando alguien abre el menú a decidir a
dónde ir. El contador se pide **al abrir el menú**: en desktop sale
gratis porque `MenuDesplegable` solo monta su contenido cuando está
abierto (por eso ese contenido pasó a componente propio), y en mobile
va atado al estado del sheet.

Con 0 el badge **no se dibuja**: un badge en cero le enseña a la gente
a ignorar los badges.

**El commit venía mezclando backend y frontend y se partió antes de
pushear** (misma operación que en la Fase 4), verificando que el árbol
resultante fuera idéntico al original: sin eso, el badge habría llamado
a un endpoint inexistente durante la rotación de Render.

Verificado en producción: marcador `OPTIONS .../contador` **404 → 200**
con `allow: GET,HEAD,OPTIONS`; catálogo intacto; los dos contadores en
401 anónimo; y **como visitante, cero llamadas** a `/consultas` (el
hook no dispara sin sesión), con consola limpia.

**Falta el smoke con sesión**: abrir el menú con una consulta sin
responder y ver el número, y que desaparezca después de abrir el hilo.

### Pendiente que queda del inbox

- **Respuestas rápidas guardadas**: Fase 11 (Publicador Pro).

### ✅ Contador CERRADO (smoke de Agustín OK, 2026-08-25)

Con esto **el inbox queda completo**: bandejas, hilo, cierre, reporte y
moderación, la promesa de privacidad escrita, y el contador de no
leídos. El único pendiente del módulo son las **respuestas rápidas
guardadas**, que son Fase 11 (Publicador Pro).
