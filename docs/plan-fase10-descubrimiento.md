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
