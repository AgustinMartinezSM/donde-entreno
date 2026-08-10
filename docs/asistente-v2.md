# Asistente IA V2 — coach deportivo local conversacional

Estado: implementado, **sin desplegar**. Continúa y reemplaza el diseño de
`docs/plan-asistente-gemini.md`, que describe el asistente V1 (bloque G) y sigue
siendo la referencia de la infraestructura: endpoint, límites, cliente HTTP de
Gemini y cómo diagnosticarlo por los logs de Render.

---

## 1. Qué problema resuelve

El asistente V1 funcionaba y no inventaba nada, pero se sentía como un
formulario. Las tres causas eran concretas y estaban en el código:

1. **El modelo tenía prohibido escribir.** Devolvía cinco términos (deporte,
   categoría, barrio, nivel, modalidad) y el backend redactaba con plantillas.
   Era una defensa completa contra inyección de instrucciones, y también la
   razón por la que todas las respuestas sonaban iguales.
2. **No había memoria de ninguna clase.** El request tenía `texto` y
   `rutaActual`, nada más. Entonces `no quiero básquet` se procesaba como
   mensaje suelto, el resolutor encontraba la palabra "básquet" y el asistente
   contestaba *"¡Básquet es una gran elección!"*. Un rechazo leído como pedido.
3. **El catálogo era la única fuente de deportes.** Si un deporte no estaba en
   la base, no existía para el asistente: no podía recomendar pádel, escalada ni
   baile, y no podía explicar de qué se trata ninguno.

V2 corrige los tres sin resignar la regla que sostenía todo: **el asistente no
afirma nada que la base no respalde.**

---

## 2. Las dos capas: consejo general y actividades reales

Es la distinción central del bloque, y está en el tipo `DeporteSugerido`, no en
el texto. Todo deporte propuesto cae en uno de tres estados:

| Estado | Qué significa | Qué se muestra |
| --- | --- | --- |
| `slug != null` y `publicadas > 0` | Está en el catálogo y hay actividades | Se nombra, se declara disponible y **se enlaza** |
| `slug != null` y `publicadas == 0` | El deporte existe, nadie publicó todavía | Se nombra como recomendación general, **sin enlace** |
| `slug == null` | No está en el catálogo (pádel, escalada, baile) | Se nombra como recomendación general, **sin enlace** |

La línea que separa una cosa de la otra la escribe siempre el backend
(`RedactorRespuesta.describirDisponibilidad`), nunca el modelo:

> En DondeEntreno ya hay actividades de Natación, Yoga y Fútbol. El resto va
> como recomendación general: todavía no hay nada publicado.

Los conteos salen de **una** búsqueda real sin filtros, agrupada en memoria
(`DisponibilidadCatalogo`). No hay estimaciones. No se genera enlace para un
deporte sin actividades: mandar a alguien a una búsqueda vacía es peor que no
ofrecer el botón.

---

## 3. Cómo se evita inventar

Cuatro candados, todos en código, ninguno confiado al modelo:

1. **Los deportes se validan contra el catálogo.**
   `RecomendadorDeportes.validar` toma los nombres que propuso el modelo y los
   resuelve contra el conocimiento y contra la base. Lo que no matchea
   desaparece; si no queda nada, decide el recomendador determinístico.
2. **Los rechazos se aplican como filtro duro.**
   `AnalizadorConversacion` lee toda la charla y arma un `PerfilConversacion`.
   Un deporte rechazado no vuelve *aunque el modelo lo proponga de nuevo*. El
   rechazo de un grupo ("no me gustan los deportes de pelea") saca todo el grupo
   de una, no solo lo nombrado.
3. **El texto del modelo se sanitiza.**
   `SanitizadorTexto` borra URLs, enlaces markdown, mails y teléfonos, y
   **descarta la oración entera** si contiene un precio o una hora de reloj. Un
   precio inventado manda a una persona a un lugar con una expectativa falsa.
4. **Los enlaces los arma el backend.**
   El modelo no tiene campo para proponer destinos. Cada `href` sale de un slug
   de la base, y el frontend además descarta cualquier enlace que no sea una
   ruta interna (`motorRemoto.normalizarEnlaces`).

### Lo que cambió respecto de V1, y a cambio de qué

V1 tenía una propiedad más fuerte: el modelo **no podía poner una sola palabra
en la pantalla**, así que una inyección de instrucciones en el mensaje del
usuario no tenía por dónde llegar a ningún lado. V2 la resigna: ahora el modelo
escribe el párrafo de consejo.

Lo que la reemplaza es el reparto de responsabilidades: **el modelo aporta el
tono y el criterio, el backend aporta todos los hechos.** El modelo no escribe
la lista de deportes (la arma el backend con los nombres validados), no escribe
los conteos, no escribe los enlaces y no escribe la línea de disponibilidad. Lo
único suyo que llega a la pantalla es prosa sin datos, y pasa por el
sanitizador.

Riesgo residual, dicho explícitamente: alguien puede lograr que el modelo
escriba una frase rara **en su propia pantalla**. No hay datos de otros usuarios
en el contexto, ni herramientas que el modelo pueda invocar, ni forma de que esa
frase salga del navegador de quien la provocó.

---

## 4. Cómo funciona Gemini en V2

```
frontend (motorCascada)
  ├─ ayuda de la app / saludo suelto ......... se resuelve en el navegador
  ├─ un deporte a secas, sin charla previa ... se resuelve en el navegador
  └─ todo lo demás ........................... POST /api/asistente/consulta
                                                  │
                          AnalizadorConversacion ──┤  perfil: rechazos, preferencias,
                                                  │  ya sugeridos, señales de salud
                          ResolutorConsulta ──────┤  ¿nombró algo concreto del catálogo?
                                                  │
                          ┌───────────────────────┴───────────────────────┐
                          │ sí → búsqueda real     │ no → camino de coach │
                          │  (total y títulos      │   1. Gemini (si hay  │
                          │   reales, enlace)      │      flag y cuota)   │
                          │                        │   2. validación      │
                          │                        │   3. recomendador    │
                          └────────────────────────┴──────────────────────┘
                                                  │
                          RedactorRespuesta ──────┘  escribe TODO lo que se ve
```

**Qué recibe el modelo** (`ConsultaRemota`): el mensaje nuevo, los últimos
turnos, el vocabulario de deportes que puede nombrar (sin los rechazados), el
catálogo real para el campo `filtros`, los rechazos explícitos y qué deportes
tienen actividades hoy. Todo es información pública; no viaja ningún dato de
cuenta, sesión ni identificación.

**Qué devuelve** (`RespuestaModelo`, JSON con esquema): `mensaje` (1-2 frases de
apertura, sin lista), `deportes[]` (nombre + motivo), `filtros`,
`preguntaSeguimiento` y `tipoRespuesta`.

**Detalle de implementación que conviene recordar:** el historial va como
**texto dentro de `input`**, no como lista de turnos. La forma multi-turno de la
Interactions API no se puede verificar sin la key (que vive solo en Render), y
este archivo ya costó un 400 en cada llamada por mandar `response_format` como
objeto cuando es una **lista**. Un string es la forma que sabemos que funciona.
Hay un test que fija la forma de `response_format` (`AsistenteGeminiTest`).

---

## 5. Qué pasa si Gemini falla

Nada visible. Hay tres compuertas antes de gastar (encendido con credenciales →
cuota diaria → llamada exitosa) y cualquiera que se cierre cae al recomendador
determinístico, que **entiende los mismos ejes y respeta los mismos rechazos**;
lo único que cambia es que la redacción es nuestra.

Además, si el modelo contesta pero su respuesta no sobrevive a la validación
(inventó los deportes, o todos estaban rechazados), también se cae al
recomendador. Y si el frontend no puede llegar al backend (sin red, timeout,
429), usa lo que resolvió el navegador. El asistente nunca queda mudo ni tira un
error en pantalla.

Con el flag apagado el asistente V2 sigue siendo mejor que el V1: la memoria de
la conversación, los rechazos, la separación consejo/realidad y las
explicaciones de cada deporte son todas determinísticas.

---

## 6. Privacidad

- **No se guarda nada.** La conversación vive en la pestaña del usuario. No hay
  tabla, no hay migración, no hay estado en el servidor. Si recarga, arranca de
  cero.
- **El texto del usuario no se loguea nunca.** De cada consulta se registra solo
  metadata: si se entendió, qué deporte/categoría/barrio, cuántos turnos,
  cuántos rechazos y cuántas preferencias.
- **El aviso está arriba de la conversación** y dice "puede enviarse", que es
  literal: lo que resuelve el navegador nunca sale del dispositivo. Una vez que
  la charla arrancó se encoge a un renglón plegable.
- La API key viaja en un header, nunca en la URL, y no aparece en ningún log.

---

## 7. Límites de uso y costo

| Variable | Default | Para qué |
| --- | --- | --- |
| `DONDEENTRENO_ASISTENTE_MAX_INPUT_CHARS` | 300 | Largo del mensaje; el input tiene `maxLength` igual |
| `DONDEENTRENO_ASISTENTE_MAX_CONSULTAS_POR_MINUTO` | 8 | Ventana por IP |
| `DONDEENTRENO_ASISTENTE_MAX_CONSULTAS_POR_HORA` | 60 | Ventana por IP |
| `DONDEENTRENO_ASISTENTE_MAX_MENSAJES_HISTORIAL` | 8 | Turnos que viajan al modelo (**nueva en V2**) |
| `DONDEENTRENO_ASISTENTE_GEMINI_ENABLED` | false | Interruptor |
| `DONDEENTRENO_ASISTENTE_GEMINI_API_KEY` | — | Solo panel de Render |
| `DONDEENTRENO_ASISTENTE_GEMINI_MODEL` | — | Id del modelo |
| `DONDEENTRENO_ASISTENTE_GEMINI_DAILY_LIMIT` | 30 | Tope de gasto |

**Atención al tope diario antes de encender V2.** Treinta llamadas por día
alcanzaban cuando el modelo entraba solo en las consultas que el motor local no
entendía. Ahora entra en toda la conversación, y una charla de cinco turnos se
lleva el 15% del día. Con 30 el asistente queda encendido para las primeras
visitas del día y determinístico para el resto — que funciona, pero no es lo que
se está buscando. **El valor lo decide Agustín en el panel de Render**; el
default del código sigue en 30 a propósito, para no cambiar el gasto sin que
alguien lo apruebe.

El costo por llamada también subió respecto de V1: la instrucción de sistema es
más larga y ahora viajan el historial y el vocabulario. A cambio, el frontend
manda muchas menos consultas al backend de las que podría, porque la ayuda de la
app y los deportes nombrados a secas se siguen resolviendo en el navegador.

---

## 8. El reparto navegador / backend

Esto es lo que arregla la limitación conocida del bloque G ("el motor local gana
siempre que reconozca un deporte y no conoce barrios").

**Se resuelve en el navegador** (instantáneo, gratis, funciona sin conexión):

- Ayuda de la app: cómo publicar, cómo contactar, filtros, precios, registro,
  ciudades, **dónde veo mis imágenes**, **qué es la imagen principal**,
  **dónde veo mis solicitudes**, **cómo guardo**, **cómo sigo a un club**.
- Un saludo suelto: `hola` y nada más.
- Un deporte nombrado a secas (`busco karate`), **sin charla previa y sin
  señales conversacionales**.

**Va al backend** todo lo demás: preferencias, rechazos, correcciones, consultas
abiertas y cualquier mensaje con conversación previa. Incluido
`holis, algún deporte que recomiendes?`, que en V1 se comía el saludo.

---

## 9. Casos de prueba

### Automatizados (backend, 101 tests)

| Archivo | Cubre |
| --- | --- |
| `AnalizadorConversacionTest` | rechazos directos y de grupo, falsos positivos (`no sé si yoga o pilates`, `nunca hice yoga`), preferencias, salud, acumulación por historial |
| `RecomendadorDeportesTest` | recomendación por defecto y por eje, filtro de rechazados, postergar lo ya sugerido, cruce con el catálogo, validación de lo que dice el modelo |
| `SanitizadorTextoTest` | URLs, markdown, mails, teléfonos, precios, horarios, recorte |
| `AsistenteGeminiTest` | parseo de `model_output`, pasos `thought`, cercos de código, forma de `response_format`, historial en la entrada |
| `AsistenteServiceTest` | los dos caminos completos, rechazos end-to-end, separación consejo/realidad, fallback sin modelo, validaciones de entrada |
| `AsistenteControllerTest` | 200, 429, 400 con texto vacío, 400 con historial desmedido, historial que llega al service |

### Verificado a mano contra datos reales

Ver la sección 11 del handoff de la sesión. Los casos del pedido original
(1-14) están cubiertos entre los tests y ese smoke.

---

## 10. Pendientes

1. **Encender el flag con un tope diario acorde** (ver sección 7). Hasta
   entonces, producción responde con el recomendador determinístico.
2. **El conteo de disponibilidad carga todas las actividades publicadas** en una
   consulta y agrupa en memoria. Hoy el catálogo publicado son unidades y sale
   más barato que un conteo por deporte; cuando crezca, va una consulta agrupada
   en la base (`DisponibilidadCatalogo.desde`).
3. **Los barrios no participan de la recomendación**, solo de la búsqueda
   concreta. "Algo social en Constitución" recomienda bien pero no filtra por
   zona.
4. **El conocimiento deportivo es un archivo, no una tabla.** Sumar un deporte a
   la recomendación es agregar una línea en `ConocimientoDeportes`. Un deporte
   que esté en la base y no ahí sigue funcionando por búsqueda directa; lo único
   que no puede es aparecer en una recomendación abierta.
5. **Smoke autenticado**: nada de este bloque depende de sesión, pero conviene
   confirmar que el widget se comporta igual logueado.
