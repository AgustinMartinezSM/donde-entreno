# Plan — Fase 7 social: cercanía primero, mapa después

Estado: **aprobado ("Dale, con las 5 recomendaciones") e IMPLEMENTADO**
(backend `8b3f88f`, frontend `558f15b`). Script 33 aplicado por Agustín
en Supabase y local; suite unit + ITs en verde con el script aplicado;
typecheck y lint limpios. Falta: deploy en dos tandas y su smoke.

Notas de la implementación, sobre lo planeado:

- **Cero dependencias nuevas**, como se prometió: "Cómo llegar" es un
  link (que además abre la app nativa del teléfono, mejor que un mapa
  embebido) y la distancia se calcula en el backend.
- **La distancia devuelve −1 y no 0 cuando falta el dato.** Con 0, las
  actividades sin punto se ordenarían **primero**, o sea aparecerían
  como "las más cercanas". Hay un test que lo fija.
- **El resolutor prioriza `!3d!4d` sobre `@lat,lng`**: el segundo es el
  centro de la cámara y se corre si la persona arrastró el mapa antes
  de copiar; el primero es el punto del lugar. Con los dos presentes
  gana el del lugar, y hay un test con un link donde difieren.
- El endpoint de zonas quedó en `/api/actividades/zonas` y no en
  `/api/barrios/zonas`: lo que se cuenta son **actividades**, y meterlo
  en el controller de barrios obligaba a que ese recurso dependiera
  del service de actividades (rompía su `@WebMvcTest`).
- **Queda pendiente**: sacar "cerca mío" de las *stopwords* del
  buscador. Hoy quien lo escribe recibe en silencio una búsqueda del
  deporte a secas; con la fase desplegada, esa frase debería activar
  el modo cercanía. No entró en esta tanda.

El roadmap pide "mapa y cercanía". Este plan **separa las dos cosas y
las ordena**, porque el mapa sin datos cargados es una pantalla vacía
y el valor real ("qué tengo cerca") no necesita mapa para existir.

## El dato que manda: hoy faltan las coordenadas que importan

Medido en **producción**, no en el seed:

- **9 ubicaciones en total: 5 con coordenadas y 4 sin.**
- Las 5 que tienen son las del **seed de prueba**
  (`03_seed_test_data.sql`, coordenadas reales de Mar del Plata).
- **Las 4 que faltan son las creadas por el flujo productivo real** —
  incluida la de "club atletico sur", el publicador con actividad viva.

La causa está identificada: `SolicitudPublicacionAdminService`
`crearUbicacion` (~línea 430) y el equivalente de
`SolicitudCambioActividadAdminService` (~313) **nunca setean lat/lng**,
así que toda ubicación nacida del flujo real queda en NULL. Un mapa
encendido hoy mostraría los pines de prueba y **no** los reales: peor
que no tener mapa.

## Lo que ya existe (y sorprende para bien)

- `ubicacion.latitud/longitud` **NUMERIC(9,6) con CHECK de rango**
  (-90..90, -180..180) desde el schema original, más
  `google_maps_url VARCHAR(500)`.
- La entidad ya mapea los tres campos, y **`UbicacionDTO` ya los
  expone** por `GET /api/ubicaciones`, que es **público**.
- El filtro por **barrio ya funciona** de punta a punta (query,
  `/explorar`, panel de filtros).
- Precedente para funciones SQL en JPQL: `unaccent` se registró con
  `UnaccentFunctionContributor` + `CREATE EXTENSION` en el script 16.
  El mismo camino sirve para una función de distancia.

## Lo que falta

- **`ActividadDTO` no lleva coordenadas** (solo `direccion`, `barrioId`,
  `barrioNombre`) — verificado contra el detalle público en producción.
  Sin eso, la ficha necesitaría un segundo request para dibujar nada.
- **Ningún camino de escritura setea lat/lng**, y el publicador **no
  puede editar su ubicación**: `UbicacionController` es solo GET y los
  cambios pasan por solicitud moderada.
- `ciudad` y `barrio` **no tienen coordenadas**, así que un mapa de
  listado no sabría dónde centrarse sin hardcodear Mar del Plata.
- Cero cálculo de distancia, cero índice geográfico, cero PostGIS.
- **El frontend tiene TRES dependencias de producción** (next, react,
  react-dom). Cero librerías de UI: el carrusel es scroll-snap nativo,
  el lightbox es `<dialog>`, el editor de encuadre es canvas propio.
  Un mapa sería **la primera dependencia visual del proyecto**
  (~150 KB gzip entre `leaflet` + `react-leaflet`), y además exigiría
  `dynamic(..., { ssr: false })` porque toca `window` al montar,
  cuando todo el sitio hoy es server-first.
- **Cero `navigator.geolocation`** en todo el proyecto.

## Tres detalles que encontró la auditoría y conviene aprovechar

1. **La política de privacidad YA está escrita** para esto:
   `/privacidad` dice que las funciones de cercanía, *cuando existan*,
   usan la ubicación solo en el momento de la consulta y **no la
   guardan**. El plan se limita a cumplir lo que ya está publicado.
2. **Hoy el buscador TIRA "cerca mío"**: `deporteSearch` y el motor
   local del asistente tratan "cerca", "cerca mío", "en mi barrio"
   como *stopwords* y las descartan. O sea que quien lo escribe
   obtiene una búsqueda del deporte a secas, en silencio. Con la fase
   hecha, esas frases deberían activar el modo cercanía en vez de
   evaporarse.
3. **El barrio no es navegable**: se muestra como texto en cards y
   detalle, pero no linkea a `/explorar?barrioId=...`. Convertirlo en
   link es de las mejoras más baratas de toda la fase y va con las
   "zonas".

## El orden que propone este plan

**Paso 1 — Que las coordenadas existan** (sin esto nada más importa).
**Paso 2 — "Cerca mío" sin mapa**: distancia y orden por proximidad.
**Paso 3 — Mapa**: recién cuando haya pines que mostrar.

## Migración (script 33 — solo lo del paso 1)

```sql
ALTER TABLE solicitud_publicacion
    ADD COLUMN latitud NUMERIC(9,6) NULL,
    ADD COLUMN longitud NUMERIC(9,6) NULL;

ALTER TABLE solicitud_cambio_actividad
    ADD COLUMN ubicacion_latitud NUMERIC(9,6) NULL,
    ADD COLUMN ubicacion_longitud NUMERIC(9,6) NULL;

-- Mismos CHECK de rango que ya tiene `ubicacion`.
ALTER TABLE solicitud_publicacion
    ADD CONSTRAINT chk_solicitud_publicacion_latitud
        CHECK (latitud IS NULL OR (latitud >= -90 AND latitud <= 90)),
    ADD CONSTRAINT chk_solicitud_publicacion_longitud
        CHECK (longitud IS NULL OR (longitud >= -180 AND longitud <= 180));
-- (ídem para solicitud_cambio_actividad)
```

Sin índice geográfico todavía: con 7 actividades, un índice espacial
es ceremonia. Se agrega cuando el volumen lo pida (ver decisión 4).

## Backend

1. **`ActividadDTO` + `ActividadMapper`**: sumar `latitud`, `longitud`
   y `googleMapsUrl` (aditivos, como todos los campos nuevos del
   proyecto). Con eso la ficha ya puede ofrecer "Cómo llegar" sin
   pedir nada extra.
2. **Escritura de coordenadas**: `crearUbicacion` y el camino de
   cambio pasan a setear lat/lng desde la solicitud.
3. **`PATCH /api/publicador/ubicaciones/{id}/coordenadas`**: el
   publicador corrige el punto de SU sede sin pasar por moderación.
   Es un dato objetivo y verificable (o el pin está en su puerta o no),
   y hacerlo pasar por el admin garantiza que nadie lo cargue.
4. **Búsqueda por cercanía**: `GET /api/actividades?lat=&lng=&radioKm=`
   con distancia calculada. Devuelve `distanciaKm` por actividad y
   ordena por proximidad. Las actividades **sin coordenadas quedan
   fuera** del modo cercanía (no se pueden ordenar), y eso se avisa en
   pantalla.
5. **Zonas por barrio (V0, sin coordenadas)**: `GET /api/barrios/populares`
   — cuántas actividades publicadas hay por barrio. Es un dato que ya
   existe y da valor inmediato mientras las coordenadas se cargan.

## Frontend

- **"Cómo llegar"** en el detalle: link a Google/Apple Maps armado con
  las coordenadas si existen, o con la dirección como texto si no.
  Cero dependencias, valor inmediato.
- **"Cerca mío"** en `/explorar`: botón que pide `navigator.geolocation`,
  y con el permiso dado ordena por distancia y muestra "a ~1,2 km".
  **La ubicación del usuario NUNCA se guarda ni se manda a un tercero**:
  viaja al backend propio solo como parámetro de esa consulta.
- **Editor de coordenadas del publicador**: en su panel, un campo donde
  pega el **link de Google Maps** de su sede y el frontend extrae las
  coordenadas del link. Es el camino con menos fricción y sin API keys.
- **Chips de zonas** por barrio con la cuenta real.

## Verificación y deploy

1. Unit (extracción de coords desde link de Maps, distancia contra
   casos conocidos de Mar del Plata, orden por proximidad, actividades
   sin coords fuera del modo cercanía) + IT de flujo completo.
2. Script 33 (vos) → ITs → backend → frontend → tu smoke.

## Decisiones que pide este plan (con recomendación)

1. **El MAPA no entra en esta fase.** Con 4 de 9 ubicaciones sin
   coordenadas —y siendo justo las reales— un mapa mostraría pines de
   prueba y agujeros donde están los publicadores de verdad. Además
   sería la primera dependencia de UI del proyecto, que hasta hoy
   resolvió todo con APIs nativas. Recomiendo **Paso 1 + Paso 2 en
   esta fase** y el mapa como fase propia cuando las coordenadas estén
   cargadas; ahí la decisión de Leaflet se toma con los pines a la
   vista.
2. **Carga de coordenadas: pegar el link de Google Maps**, no
   geocoding automático. El geocoding de direcciones argentinas por
   API gratuita es impreciso (Nominatim tiene límites de uso y falla
   con "Av. Independencia 3030" sin más contexto), y un pin mal puesto
   es peor que ninguno. Pegar el link es un paso que el publicador ya
   sabe hacer, y el resultado es exacto. Recomiendo esto, con el campo
   también disponible para el admin.
3. **El publicador corrige su propio punto sin moderación.** Es dato
   objetivo, no contenido; si lo pone mal se perjudica él. Pasarlo por
   el admin garantizaría que quede vacío para siempre.
4. **Sin índice geográfico ni PostGIS por ahora.** Con 7 actividades,
   la distancia se calcula en el service sobre el resultado filtrado y
   sobra. Recomiendo dejar anotado el umbral (~500 actividades) para
   volver a mirarlo, y no instalar extensiones que después hay que
   sostener.
5. **Las actividades sin coordenadas quedan fuera del modo "cerca
   mío", con aviso explícito** ("3 actividades no tienen ubicación
   exacta cargada"). La alternativa —ubicarlas en el centro del
   barrio— inventa una precisión que no existe y manda gente a la
   dirección equivocada.
