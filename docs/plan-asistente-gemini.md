# Bloque G — Asistente con Gemini: diseño técnico

Estado: **implementado y desplegado**. Escrito el 2026-08-08 como propuesta, sobre
`main = f2ec9dc`; cerrado el 2026-08-10 en `d311435`. Al final está lo que cambió entre
el plan y lo que quedó.

Se escribió antes de tocar código porque el bloque toca backend, seguridad, un servicio
externo pago y un endpoint público: las reglas de producción piden plan primero.

Commits del bloque:

| Commit | Qué |
|---|---|
| `3d0ba10` | Endpoint público con límites, resolución contra el catálogo real y búsqueda de verdad, **sin** Gemini |
| `9e1cead` | Cliente de Gemini detrás del flag, apagado |
| `a594d88` | Fix: `response_format` va como lista, no como objeto |
| `d311435` | Frontend en cascada y aviso de privacidad |

---

## 1. Qué problema resuelve (y cuál no)

La auditoría del bloque F midió el motor local contra 50 consultas reales. El resultado
importa para dimensionar esto:

- **Acierta** los pedidos concretos: deportes sueltos y en frase, alias, categorías,
  el flujo guiado, y las intenciones de publicar, contacto, precios, filtros y ciudades.
- **Cae en el fallback** cuando la consulta tiene condiciones que el catálogo no modela:
  edad ("tengo 50 años y quiero moverme"), días u horarios ("hay clases los sábados"),
  barrios ("algo en Constitución"), y objetivos difusos ("algo para hacer con mi hijo").

> **Decisión de alcance: Gemini no reemplaza al motor local, lo cubre en el fallback.**
>
> El motor local es instantáneo, gratis, determinístico y hoy resuelve bien la mayoría.
> Reemplazarlo por un LLM cambiaría respuestas que ya son correctas por respuestas
> probabilísticas, más lentas y pagas. La alternativa (Gemini primero, local de respaldo)
> queda documentada abajo como opción B, pero no es la recomendada.

**Fuera de alcance de esta V1:** memoria de conversación, responder precios u horarios
concretos de una actividad (para eso está el detalle), y cualquier cosa que implique que
el modelo hable de datos que no vengan de nuestra base.

---

## 2. Dónde vive: backend, no frontend

La API key de Gemini **no puede vivir en el frontend**. Las variables `NEXT_PUBLIC_*` se
inlinean en el bundle y son legibles por cualquiera; no hay forma de esconder una clave en
un cliente. Va en el backend de Render, con el mismo tratamiento que la service key de
Supabase Storage: referenciada por nombre de variable, cargada por panel, jamás en git.

Consecuencia: hace falta un endpoint público nuevo, y eso arrastra rate limiting (ver §6).

---

## 3. El problema central: que no invente

Un LLM suelto inventa clubes, precios, horarios y URLs. La regla del proyecto —no mocks,
no métricas inventadas— aplica igual acá, y con más razón: una actividad inventada manda
a alguien a llamar a un teléfono que no existe.

La mitigación no es pedirle por prompt que no invente. Es **no dejarlo escribir datos**:

> **Gemini nunca escribe URLs ni nombres de actividades. Traduce lenguaje natural a
> filtros. El backend valida esos filtros contra la base y arma los enlaces.**

Concretamente, se le pide salida estructurada (JSON con `responseSchema`, que Gemini
soporta nativamente) con esta forma:

```jsonc
{
  "respuesta": "texto corto en español rioplatense, sin links ni nombres propios",
  "deporteSlug": "yoga",        // o null
  "categoriaSlug": null,        // o null
  "barrioNombre": "Constitución", // o null
  "nivel": "PRINCIPIANTE",      // o null
  "modalidad": null,            // o null
  "noSeInterpreta": false
}
```

Y el backend, después:

1. **Valida cada slug contra la base.** Si `deporteSlug` no existe en `deporte`, se
   descarta el campo. El modelo no puede inventar un destino.
2. **Arma los enlaces él mismo**, con los mismos helpers que hoy usa el motor local
   (`/explorar?deporteSlug=...&page=0`, `/deportes?categoria=...`).
3. **Corre la búsqueda real** con los filtros resueltos y mira si hay resultados. Esto es
   lo que permite ser honesto: "no hay yoga en Constitución, pero sí en Centro" en vez de
   mandar a una página vacía.

El prompt de sistema lleva el catálogo real (27 deportes, 8 categorías, barrios, niveles y
modalidades), que ya está disponible en un solo lugar: `FiltroService.obtenerOpciones()`,
el mismo que alimenta los filtros de Explorar. Se cachea en memoria y se refresca cada N
minutos; no hace falta tabla nueva ni migración.

**Nada de esto necesita cambios de schema.** El bloque G es aditivo puro.

---

## 4. Contrato de API

```
POST /api/asistente/consulta        (público)
```

Request:

```jsonc
{ "texto": "hay clases de yoga los sabados en constitucion", "rutaActual": "/explorar" }
```

Response 200 — **la misma forma que ya usa la UI**:

```jsonc
{
  "texto": "En Constitución no encontré yoga, pero sí hay en Centro. Los días los ves en el detalle de cada actividad.",
  "enlaces": [{ "href": "/explorar?deporteSlug=yoga&page=0", "etiqueta": "Ver Yoga" }],
  "opcionesRapidas": ["¿Cómo filtro por barrio?", "¿Dónde veo precios y horarios?"]
}
```

Otros códigos: `400` texto vacío o demasiado largo · `429` límite de uso ·
`503` no configurado, deshabilitado o Gemini caído.

**El frontend no cambia de forma.** `lib/asistente/tipos.ts` ya define
`MotorAsistente.procesar()` como `async` justamente para esto, y el comentario del archivo
lo dice explícito: se puede enchufar una IA real implementando la misma interfaz sin tocar
la UI. Se agrega un `motorRemoto.ts` que hace el fetch y un `motorCascada.ts` que compone
los dos. `AsistenteWidget` y `AsistenteConversacion` no se tocan.

---

## 5. Flujo

```
usuario escribe
      │
      ▼
motor local  ──── resuelve (deporte, categoría, intención) ──▶ responde YA (0 ms, $0)
      │
      └── cae en fallback
                │
                ▼
      POST /api/asistente/consulta
                │
                ├─ no configurado / deshabilitado / 429 / timeout ──▶ fallback actual
                │
                ▼
      Gemini (JSON schema) ──▶ backend valida slugs ──▶ corre la búsqueda real
                                                              │
                                                              ▼
                                                    respuesta con enlaces reales
```

Dos propiedades que valen la pena: el costo solo se paga en las consultas que hoy fallan,
y **si Gemini se cae, el asistente sigue funcionando exactamente como hoy**. No es una
dependencia dura.

---

## 6. Riesgos, y qué hacemos con cada uno

| Riesgo | Por qué importa acá | Mitigación |
|---|---|---|
| **Abuso de un endpoint público pago** | Es el riesgo #1. No hay auth en el asistente y hoy **no existe ninguna infraestructura de rate limiting en el backend** | Límite por IP en memoria (ventana deslizante; Render corre una sola instancia, alcanza un `ConcurrentHashMap`), tope de 300 caracteres de entrada, y **tope diario global configurable** que devuelve 503 al agotarse |
| **Que invente datos** | Manda gente a contactos falsos | §3: el modelo no escribe URLs; los slugs se validan contra la base |
| **Latencia** | Render free tier duerme; el cold start son decenas de segundos | El local responde al instante y el remoto es solo fallback. Timeout de 8 s y caída al fallback |
| **Privacidad** | El texto libre del usuario sale hacia Google | No se manda nada de sesión, usuario ni token. Aviso visible en el panel del asistente. Decisión de Agustín (§9) |
| **Prompt injection** | El input es texto libre | La salida es JSON con schema cerrado y los slugs se validan; el peor caso es un texto raro, nunca un link a otro dominio |
| **Costo imprevisible** | Sin tope, una cuota se vacía en una tarde | El tope diario es la protección real, no la estimación |
| **Deploy en el orden equivocado** | Ya cobrado antes: el cliente no distingue "no existe el recurso" de "no existe el endpoint" | Dos pushes: backend primero, verificado en producción, después el frontend |

---

## 7. Componentes nuevos

Todo espeja el patrón que ya existe para Supabase Storage (`AlmacenArchivos*`), que es la
única integración externa del proyecto y funciona bien.

**Backend** (paquete `com.dondeentreno.api`):

| Archivo | Rol |
|---|---|
| `asistente/MotorAsistenteRemoto.java` | Interfaz, con `estaConfigurado()` como en `AlmacenArchivos` |
| `asistente/AsistenteGemini.java` | Implementación con `RestClient` (HTTP puro, **sin SDK ni dependencia nueva**) |
| `asistente/AsistenteProperties.java` | `@ConfigurationProperties("dondeentreno.asistente.gemini")` |
| `asistente/LimitadorConsultas.java` | Rate limit por IP + tope diario |
| `config/AsistenteConfig.java` | Bean siempre presente, configurado o no |
| `exception/AsistenteNoConfiguradoException.java` | → 503, como `AlmacenNoConfiguradoException` |
| `service/AsistenteService.java` | Orquesta: contexto del catálogo, prompt, validación de slugs, búsqueda real, armado de enlaces |
| `controller/AsistenteController.java` | `POST /api/asistente/consulta` |
| `dto/AsistenteConsultaRequestDTO`, `AsistenteRespuestaDTO` | Contrato |

Un cambio en `SecurityConfig`: `.requestMatchers(HttpMethod.POST, "/api/asistente/consulta").permitAll()`.
Es el único punto sensible del bloque y hay que revisarlo con cuidado, porque abre un POST
público nuevo.

**Frontend:** `lib/asistente/motorRemoto.ts` y `motorCascada.ts`. Nada más.

**Variables de entorno** (por nombre; los valores van al panel de Render):

- `DONDEENTRENO_ASISTENTE_GEMINI_API_KEY` — secreta
- `DONDEENTRENO_ASISTENTE_GEMINI_MODELO` — id del modelo, configurable a propósito para no
  hornear una versión en el código (usar la familia Flash, la barata y rápida; **confirmar
  el id vigente en la doc de Google al implementar**)
- `DONDEENTRENO_ASISTENTE_HABILITADO` — kill switch sin redeploy
- `DONDEENTRENO_ASISTENTE_LIMITE_DIARIO` — tope de consultas por día

Sin las variables, la app arranca igual y el endpoint responde 503: el deploy queda
tolerante al orden, como el de storage.

---

## 8. Implementación por pasos

1. **Backend sin Gemini**: endpoint, contrato, validación de entrada, rate limit y el 503
   de no-configurado. Deploy y verificación en producción con `curl`. Sin costo ni riesgo
   de modelo.
2. **Backend con Gemini**: cliente real detrás del flag, con el catálogo en el prompt y la
   validación de slugs. Tests unitarios con la respuesta del modelo mockeada, incluido el
   caso "devuelve un slug que no existe".
3. **Frontend**: cascada local → remoto. Segundo push, recién con el backend verificado.
4. **Medir**: cuántas consultas llegan al remoto y qué resuelven. Recién ahí se sabe si
   valió la pena.

Los pasos 1 y 2 se pueden verificar sin tocar el frontend, que es lo que hace este bloque
menos riesgoso de lo que parece.

## Opción B, descartada

Gemini primero y el motor local de respaldo. Se descarta porque cambia respuestas que hoy
ya son correctas por otras probabilísticas, pagas y más lentas, y porque haría que **toda**
consulta dependa de un servicio externo. Si alguna vez se quiere ir para ahí, el diseño de
arriba lo permite sin reescribir: es invertir el orden en `motorCascada.ts`.

---

## 9. Qué cambió entre el plan y lo implementado

Tres cosas, y las tres para mejor.

**El modelo terminó escribiendo menos de lo previsto.** El plan lo dejaba devolver un
campo `respuesta` con texto. Al implementarlo se sacó: hoy devuelve **solo cinco términos**
(deporte, categoría, barrio, nivel, modalidad) y el backend redacta todo. El beneficio no
es solo que no puede inventar destinos —eso ya lo cubría la validación de slugs— sino que
**no tiene forma de poner una sola palabra en la pantalla del usuario**, así que una
inyección de instrucciones en el mensaje no llega a ningún lado. De paso la respuesta es
más corta, más barata y más rápida.

**La validación quedó más simple que lo diseñado.** En vez de validar campo por campo
contra la base, los términos que devuelve el modelo se pasan por el **mismo resolutor
determinístico** que procesa la consulta del usuario. Lo que el modelo invente no matchea
contra el catálogo y se descarta solo, sin lista negra y sin código nuevo. Hay un test que
le hace devolver "Quidditch" y "Hogwarts" y verifica que la respuesta sea "no entendí".

**La API de Gemini es otra.** El plan asumía `models/{model}:generateContent`. La familia
actual usa **`POST /v1beta/interactions`**, con `system_instruction`, `generation_config`
(donde va `thinking_level`) y `response_format` en snake_case, y el texto sale de
`steps[].content[].text` del paso `model_output`, que puede venir después de pasos
`thought`.

Y un error que costó un deploy: **`response_format` es una lista, no un objeto**:

```json
"response_format": [
  { "type": "text", "mime_type": "application/json", "schema": { } }
]
```

Mandarlo como objeto con `type: "json_schema"` devuelve **400 en cada llamada**. Esa forma
equivocada salía de la página de *referencia* de la API; las de *migración* y *structured
output* coinciden entre sí en la correcta. La lección: ante documentación contradictoria de
un proveedor, mandan las dos fuentes que coinciden, no la que parece más autoritativa.

Lo importante es que **el error no se vio desde afuera**: el cliente capturaba el 400 y
respondía con el motor local. Eso es exactamente lo que la cascada prometía, y se verificó
sin querer.

### Cómo diagnosticarlo en producción

Filtrar los logs de Render por `Asistente`:

- `resuelta=true/false ...` sale de `AsistenteService` en **cada** consulta.
- `el motor remoto no respondio (...)` sale de `AsistenteGemini` **solo si la llamada se
  intentó**.

Si no hay warning, el modelo ni se intentó: el problema es el flag o el binding. Si lo hay,
el problema es el request. Ninguna de esas líneas contiene la API key: viaja en un header,
nunca en la URL.

### Pendiente conocido

El motor local del navegador gana siempre que reconozca un deporte y **no conoce barrios**,
así que "yoga en Constitución" se resuelve como "yoga" y nunca llega al backend, que sí
sabría filtrar por barrio. Arreglarlo es hacer que el local ceda cuando la consulta tiene
más señal de la que él puede aprovechar.

---

## 10. Lo que se definió antes de implementar

Estas tres preguntas bloqueaban el arranque. Quedaron resueltas así:

1. **API key**: la creó Agustín en Google AI Studio y la cargó **solo en el panel de
   Render**. Nunca pasó por el chat, ni por Vercel, ni por git, ni por los logs.
2. **Tope de gasto**: 300 caracteres por mensaje, rate limit por IP, y **30 llamadas
   diarias** a Gemini. Al agotarse el tope **no hay error**: se responde con el motor
   local, que es gratis.
3. **Aviso de privacidad**: sí, arriba de la conversación. Se sumó recién con el encendido
   de Gemini, no antes: mientras estuvo apagado, decir que el mensaje podía ir a Google
   habría sido falso.

Modelo elegido: `gemini-3.5-flash-lite`, configurable por `..._GEMINI_MODEL` para no
hornear una versión en el código.
