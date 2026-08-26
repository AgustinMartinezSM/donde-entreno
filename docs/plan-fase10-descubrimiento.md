# Fase 10 — Descubrimiento inteligente (plan propuesto)

Estado: **PROPUESTO**, pendiente de aprobación de Agustín.
Roadmap: `docs/roadmap-social-dondeentreno.md` (Fase 10).

El roadmap la define así: *match deportivo (test guiado + Dondi),
comparador (sobre guardados), rankings semanales (del tracking), "para
arrancar de cero", guías deportivas (editorial/SEO)*.

## Relevamiento (verificado en código)

- **El "match deportivo" YA EXISTE, y se llama Dondi.**
  `RecomendadorDeportes` + `AnalizadorConversacion` hacen exactamente
  eso: preguntan, entienden rechazos ("nada de pelea") y recomiendan
  deportes reales del catálogo, con el motor local en el navegador y
  Gemini cuando hace falta.
- **Los rankings del tracking también existen, parcialmente**:
  `EventoInteraccionRepository.rankingDeActividades` y
  `/api/deportes/populares` alimentan "Lo más visto" de la home desde
  la Fase 6, **con fallback a la selección curada si no hay señal
  suficiente**.
- El **tracking** registra `tipo`, `actividad_id` y
  `perfil_publicador_id` — anónimo, sin usuario.
- Los **favoritos** ya viven en `lib/favoritos.ts`, sincronizados con
  la cuenta: un comparador no necesita backend nuevo.

## Recomendación 0 (la que pediría hacer primero): mirar los datos

Las fases 8 y 9 sumaron **seis superficies sociales** —novedades,
eventos, inbox, grupos, reacciones, feed— sobre una base con poco
contenido y pocos usuarios. Nadie sabe todavía qué se usó.

Antes de construir descubrimiento **sobre** esa base, propongo una
pantalla chica en el admin —**`/admin/pulso`**— con los conteos reales:
cuántas actividades publicadas, fotos, novedades, eventos,
conversaciones, grupos y miembros hay, y cuántas interacciones registró
el tracking en los últimos 7 y 30 días.

Es media jornada de queries de conteo, no toca ninguna tabla, y
responde la pregunta que decide todo lo demás. **Si el resultado es que
casi no hay uso, el problema no se arregla con más features de
descubrimiento**, y conviene saberlo antes y no después.

## Decisiones que pide este plan (con recomendación)

### 1. NO hacer un test guiado nuevo: darle una puerta a Dondi

Un "test de match deportivo" en paralelo sería **una segunda puerta al
mismo lugar, con reglas distintas**: dos motores que recomiendan
deportes, dos formas de entender "no quiero pelea", dos cosas que
mantener sincronizadas.

**Recomiendo** en cambio una **entrada guiada** en la home y en
`/deportes` —tres o cuatro preguntas con botones— que **arme la primera
consulta y se la pase al asistente que ya existe**. Misma inteligencia,
puerta más amable para quien no sabe qué escribirle a un chat.

### 2. El comparador, sí: es frontend puro sobre datos que ya existen

Comparar hasta 3 actividades guardadas (precio, nivel, modalidad, zona,
horarios, valoración). Sale de `lib/favoritos.ts` y del detalle que ya
se pide. **Sin backend ni migración.**

Y con una regla que ya usamos en toda la etapa: **una fila que ninguna
de las tres tiene, no se dibuja**. Un comparador lleno de guiones se
siente roto.

### 3. Rankings semanales: solo si hay señal, y diciendo de qué

Ya existe el mecanismo con fallback. Lo que suma esta fase es la
**ventana semanal** y decirlo con honestidad: "Lo más visto esta
semana" solo si hay un mínimo de señal real; si no, **se sigue
mostrando la selección curada sin llamarla ranking**.

Poner "lo más visto" con tres visitas es peor que no ponerlo: enseña a
desconfiar de los números del sitio.

### 4. "Para arrancar de cero": una landing, no un motor nuevo

`/empezar`: una página que ordena lo que ya existe para alguien que
nunca entrenó — deportes de bajo umbral, actividades para
principiantes, qué llevar, y el acceso a Dondi. **Reusa la búsqueda con
`nivel=PRINCIPIANTE`**, no inventa un algoritmo.

### 5. Guías deportivas: el contenido lo decidís vos

Es la pieza con más potencial SEO y la única que **no puedo resolver
solo**: son textos que el sitio publica como propios. Puedo dejar la
estructura (`/guias/[slug]`, sitemap, layout, enlaces al catálogo real)
y **un borrador por guía**, pero **publicar contenido editorial escrito
por mí bajo tu marca es tu decisión**, no una consecuencia técnica.

**Recomiendo**: dejar la estructura lista en esta fase con **una sola
guía** que vos revises entera antes de publicar, y decidir el resto con
eso a la vista.

## Qué NO entra

- **Recomendaciones personalizadas por historial** (tipo "porque miraste
  X"): con este volumen, recomendarían ruido.
- **Un motor de match nuevo** (ver recomendación 1).
- **Rankings de publicadores**: convertir el tracking en una tabla de
  posiciones entre clubes es una decisión de producto delicada — y con
  poco volumen, injusta.

## Verificación y deploy

1. `/admin/pulso` primero, y **mirar el resultado antes de seguir**.
2. Comparador y `/empezar`: frontend puro → typecheck + lint + build,
   verificación en el navegador.
3. Ranking semanal: unit del umbral (con señal y sin señal) + IT del
   endpoint.
4. Si toca backend, dos tandas como siempre.

---

## ✅ PULSO CERRADO EN PRODUCCIÓN (smoke de Agustín OK, 2026-08-25)

`main` = `origin/main` = **`e16b2b7`**. Backend `45ec331` + frontend
`e16b2b7`. Sin migración: el pulso solo lee.

### Qué se verificó, y con qué

- **Backend, verificado por mí**: marcador `OPTIONS /api/admin/pulso`
  **404 → 200 con `allow: GET,HEAD,OPTIONS`** (validado contra
  `/api/admin/reportes` como control), GET anónimo en **401**, y
  catálogo, eventos y novedades sanos post-deploy.
- **Frontend: lo confirma el smoke de Agustín**, no una verificación
  mía. **No existe marcador anónimo para esta pantalla**: `/admin/pulso`
  da 307 igual que `/admin/reportes` —que existe hace fases— y que
  cualquier ruta inexistente bajo `/admin`, porque el middleware las
  redirige todas. El cookie-trick tampoco sirve acá (el guard de admin
  no usa la cookie del proxy). Que él haya visto el panel con su cuenta
  ES la evidencia, y es más fuerte que un marcador.
- 604 unit + 142 ITs verdes, con las 22 consultas ejercidas contra el
  schema real.

### La trampa que este panel tiene incorporada

Cada conteo va aislado y devuelve **0** si su SQL falla, para que el
diagnóstico no se caiga entero por un número. El costo: **un SQL roto
sería invisible** —el panel mostraría ceros y parecería "no hay uso",
que es justo la conclusión que este panel existe para informar—. Por
eso `PulsoIT` verifica que actividades, publicadores y usuarios, que el
seed **tiene**, den mayor a cero.

**Si algún día una métrica cae a 0 de golpe: antes de concluir nada,
correr `PulsoIT`.**

### Lo que sigue de la Fase 10

Con los números a la vista, decidir si el resto del plan se ejecuta tal
cual o se replantea. Las piezas propuestas siguen siendo: entrada
guiada a Dondi (no un test nuevo), comparador de guardados, ranking
semanal con umbral, `/empezar`, y la estructura de guías con **una sola
guía** para revisar antes de publicar.

---

## `/empezar` + entrada guiada: EN PRODUCCIÓN (pendiente smoke)

`main` = `origin/main` = **`cb6752e`**. Solo frontend, una sola tanda
(no toca la API ni la base).

- **Marcador**: `/empezar` pasó de **404 a 200**. Sirve porque es una
  ruta PÚBLICA nueva y ningún middleware la enmascara — a diferencia de
  todo lo que cuelga de `/admin` o `/publicador`. Un control con una
  ruta inventada confirmó que las desconocidas siguen dando 404.
- Verificado además por contenido: los seis textos de la página en el
  HTML servido, las tres actividades PRINCIPIANTE reales linkeadas, el
  CTA en la home, el link en `/deportes` y la ruta en el `sitemap.xml`.
- **El traspaso a Dondi se ejercitó en producción**: las tres respuestas
  arman la consulta, el panel la manda sola y Dondi respeta el rechazo
  (propuso Funcional, Yoga, Stretching y Aqua Gym, y separó lo que hay
  publicado de la recomendación general). Consola limpia.

### La trampa de verificación que costó tiempo

Al medir el chip elegido con `getComputedStyle` daba **fondo
transparente** y parecía que la selección no se veía. **No era un bug**:
con el pane del navegador oculto las transiciones CSS quedan
**congeladas en t=0**, y el chip transiciona el fondo en 0.15s. Con
`transition: none` mide azul de marca con tinta blanca.

**Regla: antes de creerle a un `getComputedStyle` sobre una propiedad
que transiciona, anular la transición.**

## Comparador de guardadas: EN PRODUCCIÓN (pendiente smoke)

`main` = `origin/main` = **`cddb802`**. Solo frontend.

- **Marcador**: el string `Comparar guardadas` en el chunk servido de
  `/favoritos`. Como la ruta está detrás del guard, el HTML se obtiene
  con la **cookie del proxy bien formada** — su valor es
  `<rol>.<vencimientoEnMs>`, no un valor cualquiera: con `de_sesion=1`
  el proxy la descarta y sigue devolviendo 307. La cookie **no da
  acceso a datos** (el `AuthGuard` redirige igual y el backend valida
  JWT): sirve para leer qué build se está sirviendo.
- Guards intactos (`/favoritos` y `/mi-cuenta` anónimos siguen en 307) y
  públicas en 200.

### Lo que quedó decidido en el código

- Una fila que **ninguna** de las elegidas tiene, no se dibuja;
  verificado con datos reales (la fila Valoración no aparece porque las
  tres tienen `cantidadValoraciones` en 0). Dentro de una fila que sí
  existe, el guion **sí** se muestra: que a una le falte el precio es
  información para decidir.
- Hasta 3, y con la tercera elegida las demás quedan deshabilitadas.
- Los días se traducen del enum y **se ordenan por semana**: "Jueves,
  Martes" se lee como un error aunque sea fiel al dato.

**Pendiente menor que destapó**: el detalle público sigue mostrando el
enum crudo del día (`MIERCOLES`). Unificarlo es una pasada aparte.

## Ranking semanal: EN PRODUCCIÓN en dos tandas (pendiente smoke)

Backend **`006b731`** + frontend **`c9424a6`**. Sin migración: el
ranking solo lee `evento_interaccion`.

`GET /api/actividades/mas-vistas?dias=7&limite=6`, público. El query
`rankingDeActividades` **existía desde la Fase 6** pero nadie lo usaba
para actividades — solo agrupado por deporte para la fila de populares.

### Los dos candados

1. **Menos de 3 actividades con señal → lista vacía**, y el frontend no
   dibuja la sección.
2. **Solo PUBLICADAS y activas**, y las despublicadas **no cuentan
   siquiera para el umbral**: si contaran, dos publicadas más una
   despublicada llegarían al mínimo y la sección saldría con dos.

### Verificación

- **Marcador**: `/api/actividades/mas-vistas` — pero **el código no
  alcanzaba**: en el build viejo esa ruta cae en `/{slug}` y devuelve
  404, así que el 200 podía confundirse. Lo que distingue es el
  **cuerpo**: un ARRAY contra el JSON de error "Actividad no
  encontrada". El control con un slug inventado sigue dando 404.
- **Conductual, no solo 200**: devuelve 3 actividades con señal real,
  `limite=2` devuelve 2, y el resto de la API quedó sana.
- **El caso vacío se verificó rompiendo el endpoint a propósito** (y
  revirtiendo): la sección desaparece y la home queda entera. Sin eso,
  "es best-effort" era una afirmación, no un hecho medido.

### Lo que corrigió el IT

El test del despublicado fallaba porque, al sacar una, **desaparecían
las otras dos**. No era un bug: quedaban 2 con señal y el umbral apagaba
la sección. Hizo falta una CUARTA actividad para probar las dos cosas
por separado. **Un test que ejerce un umbral tiene que dejar margen por
encima del mínimo, o mide el umbral cuando cree medir otra cosa.**

### Dato de producto

Con la ventana de 7 días hay **tres actividades con señal real** en
producción (karate, jiu jitsu y natación). Hay uso, aunque chico.

---

## ✅ RANKING SEMANAL CERRADO EN PRODUCCIÓN (smoke de Agustín OK, 2026-08-25)

`main` = `origin/main` = **`c9424a6`**. Backend `006b731` + frontend
`c9424a6`. Sin migración.

Verificado en producción: la home sirve la sección con las tres
actividades reales linkeadas, cero desbordes, consola limpia; el
endpoint respeta `limite` y el resto de la API quedó sana.

### Estado de la Fase 10

| Pieza | Estado |
|---|---|
| `/admin/pulso` | **CERRADO** |
| `/empezar` + entrada guiada a Dondi | **CERRADO** |
| Comparador de guardadas | **CERRADO** |
| Ranking semanal | **CERRADO** |
| Guías deportivas | **NO EMPEZADO** — necesita decisión editorial |

Las guías son lo único que queda, y no es una tarea técnica pendiente:
son textos que el sitio publica **como propios**. La recomendación
sigue en pie: dejar la estructura con **una sola guía** para revisar
entera antes de publicar.

### Si el ranking parece flojo

Hoy hay **tres** actividades con señal semanal, que es exactamente el
mínimo. Si con tres se lee poco creíble, la decisión NO es técnica: es
subir `MINIMO_ACTIVIDADES_PARA_RANKING` (en `ActividadController`) y
aceptar que la sección se apague hasta que haya más uso.

---

## Guías: estructura + la primera (karate) — EN PRODUCCIÓN (pendiente smoke)

`main` = `origin/main` = **`b50cd9f`**. Solo frontend.

**Con esto la Fase 10 queda completa en sus cinco piezas.**

`/guias` y `/guias/[slug]`, con el contenido en `lib/guias.ts` — en
código y no en una tabla: son textos que cambian poco y se revisan
antes de publicar; una tabla traería editor, moderación y estados para
algo que hoy escribe una sola persona.

### La regla de contenido (está escrita en el archivo)

**No se afirma nada que la plataforma no pueda sostener.** Sin precios,
sin duraciones de clase, sin beneficios de salud, sin cuánto se tarda
en progresar. El texto cuenta qué es el deporte, cómo es empezar y qué
preguntar; los datos concretos los pone el catálogo real.

Esa separación es lo que hace que una guía envejezca bien: si mañana
abre un club nuevo de karate, aparece sin tocar una línea del texto.

### Lo que decidió Agustín sobre el contenido

- **Fuera la sección "Los cinturones"** (`b50cd9f`): era la más expuesta
  a que un karateca la corrija, y la guía funciona sin ella. Quedan
  cuatro secciones.
- **El tono sobrio queda como molde** para las guías siguientes.

### Tres defectos que aparecieron VIÉNDOLA, no leyendo el código

1. **La fecha se corría un día**: `new Date("2026-08-25")` se parsea
   como medianoche UTC y en Argentina cae el 24. Ahora se arma con los
   números sueltos. Misma familia que el ISO con offset de la Fase 9.
2. Un error de tipeo en una de las preguntas.
3. **La explicación de kyu y dan estaba al revés** — el tipo de error
   que solo ve alguien que lee el texto entero, y la razón por la que
   una guía no se publica sin que Agustín la lea.

### Soft-404

`/guias/slug-inventado` da **200 y no 404**, igual que las otras seis
rutas dinámicas: lo causa el `loading.tsx` de la raíz y está decidido
no tocarlo. El **contenido** sí es el de "no encontrada", con su
`title` propio.

---

# ✅ FASE 10 CERRADA EN PRODUCCIÓN (smoke de Agustín OK, 2026-08-25)

`main` = `origin/main` = **`b50cd9f`**. **Cinco bloques y ninguna
migración**: la primera fase entera de la etapa social que no tocó el
schema.

| Pieza | Commits |
|---|---|
| `/admin/pulso` | `45ec331` + `e16b2b7` |
| `/empezar` + entrada guiada | `cb6752e` |
| Comparador de guardadas | `cddb802` |
| Ranking semanal | `006b731` + `c9424a6` |
| Guías (estructura + karate) | `3a00598` + `b50cd9f` |

## Lo que esta fase deja escrito para las que vengan

- **Una puerta nueva no necesita un motor nuevo.** La entrada guiada
  alimenta a Dondi en vez de competirle. Vale para cualquier superficie
  que "recomiende" algo.
- **Un número sin señal suficiente no se muestra.** El ranking se apaga
  bajo el umbral, el pulso muestra crudo y sin gráficos, y el
  comparador no dibuja filas que nadie tiene.
- **El código de estado no siempre distingue builds**; a veces el
  marcador es el cuerpo, y hay pantallas que directamente no tienen
  marcador anónimo.
- **Lo editorial lo lee Agustín antes de publicarse.** Los tres
  defectos de la guía de karate aparecieron leyéndola, no revisando el
  código.

## Pendientes que quedaron fuera, a propósito

- **Más guías**: el molde está; cada una se escribe y se lee entera
  antes de publicar. El tono sobrio de la de karate es la referencia.
- **La palabra "cinturón"** sigue apareciendo una vez en "Qué
  necesitás" (se compra junto al karategi). Es factual y no afirma nada
  sobre grados.
- **El día crudo del enum** (`MIERCOLES`) en el detalle público: el
  comparador lo traduce, el detalle todavía no.
- **Imagen OG por evento** y **filtros de ciudad/deporte en el
  calendario** siguen sin hacerse desde la Fase 9.

## Un hallazgo que no es de esta fase pero se confirmó acá

El DOM sirve **dos `<main>`**, uno dentro de un `div[hidden]`. Pasa en
`/eventos` también, así que es de toda la app: es el streaming de
Suspense del `loading.tsx` de la raíz — **la misma causa raíz del
soft-404**, y lo que explica la trampa ya documentada de que "cada
frase aparece dos veces".
