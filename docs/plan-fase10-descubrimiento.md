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
