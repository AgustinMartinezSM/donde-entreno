# Grupos por actividad (plan propuesto)

Estado: **PROPUESTO**, pendiente de aprobación de Agustín.
Roadmap: `docs/roadmap-social-dondeentreno.md` (Fase 8, punto 3).

## Antes que nada: el trade-off de fondo

Este es **el bloque de mayor costo y mayor riesgo** de todos los que
quedan, y el que menos evidencia de demanda tiene hoy:

- Es la primera superficie donde **una persona le escribe a una
  audiencia de desconocidos** dentro de la app. El inbox es 1:1 —si
  alguien se pasa de la raya, lo lee una sola persona—; un grupo lo
  lee todo el grupo.
- La moderación sigue siendo **unipersonal**.
- Y en producción hoy hay **poco contenido y pocos usuarios**: no hay
  todavía una masa de gente pidiendo un lugar donde hablar entre sí.

**No es una razón para no hacerlo**, y el plan de abajo lo hace. Es una
razón para que el V1 sea **el mínimo que entrega el valor real** y deje
afuera lo que abre el riesgo sin necesidad. El propio roadmap ya lo
anticipa: "chat libre de grupos (V2) y realtime (V3) — solo con la
moderación probada".

## Relevamiento (verificado en código)

- **No existe ninguna forma de "ser miembro" de una actividad.** Lo que
  hay son tres vínculos distintos: `favorito_actividad` (guardar),
  `interes_actividad` (QUIERO_PROBAR / YA_PROBE) y
  `entrenamiento_usuario` (el check-in "entrené acá").
- **Se sigue al PUBLICADOR, no a la actividad**
  (`seguimiento_publicador`). Un grupo por actividad es una audiencia
  que hoy no está modelada.
- **El patrón de comentarios ya existe y es moderable**:
  `comentario_imagen` con estados, `ocultarPorPublicador` y
  `ocultarPorAdmin`. Un comentario en un grupo puede reusar ese molde
  entero en vez de inventar otro.
- Las reacciones (script 37) también son reusables.

## Alcance propuesto (V1)

El grupo es **el espacio de una actividad para quienes van**: el
publicador avisa, los miembros responden. Concretamente:

1. **`miembro_actividad`** (script 38): usuario + actividad + estado
   (`ACTIVO` / `SALIO`). Unirse y salir son del usuario.
2. **`aviso_grupo`**: lo que el publicador le cuenta a ese grupo. Mismo
   molde que `novedad` (texto + foto opcional ya publicada + estado).
3. **Comentarios de los miembros** en cada aviso, reusando el molde de
   `comentario_imagen` (estados, ocultar por publicador, ocultar por
   admin, reportes).
4. **Reacciones** en el aviso, reusando el molde del script 37.
5. **Superficies**: pestaña "Grupo" en el detalle de la actividad
   (visible solo para miembros), y `/publicador/actividades/{id}/grupo`
   para el publicador.

## Decisiones que pide este plan (con recomendación)

### 1. Se entra al grupo EXPLÍCITAMENTE, no por check-in

La tentación es usar `entrenamiento_usuario` ("entrené acá") como
pertenencia automática: el dato ya está. **Recomiendo no hacerlo.**
Marcar que entrenaste una vez es un acto distinto de sumarte a un
espacio donde vas a recibir avisos y donde otros van a ver lo que
escribís. Que la app te meta en un grupo por algo que hiciste con otra
intención es exactamente cómo se pierde la confianza.

Botón explícito: **"Sumarme al grupo"**, y salir cuando quieras.

### 2. En V1 escribe el publicador; los miembros comentan y reaccionan

Es decir: **no hay chat libre miembro↔miembro**. El grupo es un tablón
con respuestas, no una sala de chat.

Esto entrega lo que de verdad hace falta —"mañana se suspende por
lluvia", "cambiamos de cancha"— sin abrir el canal más difícil de
moderar de todo el producto. El chat libre es V2 del roadmap, y
conviene decidirlo cuando haya grupos vivos y se vea cómo se comportan.

### 3. El grupo es PRIVADO para sus miembros, y el admin no lo lee

Mismo criterio que el inbox: no existe endpoint que devuelva el
contenido de un grupo a un admin. Ante un **reporte**, ve el comentario
reportado con contexto mínimo. Y, como en el inbox, esto se escribe en
`/privacidad` y `/normas` en el mismo bloque.

### 4. Notificación solo del AVISO, y con tope

El aviso del publicador notifica a los miembros (es la razón de ser del
grupo). Los **comentarios de otros miembros NO notifican a todos**: un
aviso con quince comentarios sería quince campanitas para cada
miembro. El publicador sí recibe aviso de los comentarios en su grupo,
como ya pasa con los comentarios de sus fotos.

Tope: **2 avisos por día por actividad** — mismo criterio que las
novedades, y acá pesa más porque el grupo es más íntimo.

### 5. Solo actividades PUBLICADAS tienen grupo

Sin esto habría grupos colgando de actividades pausadas o en revisión,
con miembros que no pueden ir a ninguna parte.

## Qué NO entra

- **Chat libre entre miembros** (V2, ver recomendación 2).
- **Realtime** (V3): mismo argumento de siempre — Render free tier hace
  spin-down.
- **Roles dentro del grupo** (moderadores auxiliares): no hay volumen
  que lo justifique.
- **Adjuntar fotos en comentarios**: otra cola de moderación.

## Verificación y deploy

1. Unit (unirse/salir idempotentes, solo miembros ven y comentan, el
   tope de avisos, solo actividades publicadas) + **IT propio** con el
   camino completo: unirse → ver el grupo → el publicador avisa →
   llega la notificación → comentar → el publicador oculta un
   comentario → salir deja de mostrar el grupo. Cada aserción de
   desaparición prueba antes la aparición.
2. **IT de privacidad**: un no-miembro recibe 404 (no 403) al pedir el
   grupo, y **no existe endpoint de admin que devuelva su contenido**.
3. Script 38 (Agustín) → ITs → backend → frontend → su smoke.
