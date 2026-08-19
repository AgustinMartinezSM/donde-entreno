# Plan — Sincronizar favoritos (y deportes) con la cuenta

## 1. Problema

Los favoritos y los deportes elegidos viven en `localStorage` del
dispositivo (con scope por cuenta desde `4c525b8`, así que no se mezclan
entre cuentas — pero tampoco viajan). La UI promete otra cosa: "Guardados"
en la barra y en `/mi-cuenta` se lee como algo de la cuenta, no de esta
computadora. Mismo problema para "Tus deportes" (los chips de la home,
cuyo bug de visitante ya se corrigió aparte: `9f6fb6b`).

Además, el snapshot local se congela al momento de guardar: si la
actividad cambió el precio o la foto después, "Mis favoritos" muestra lo
viejo.

## 2. La decisión vigente que este plan respeta

**"Sin migración automática ni sync" se decidió sobre el scope del
INVITADO**: pasar lo del navegador a la primera cuenta que se loguea es
cómo se contaminan cuentas nuevas. Eso no cambia. Lo que este bloque
agrega es la fuente de verdad en el backend **para lo que ya es de la
cuenta** — que es exactamente lo que aquella decisión dejó como "pendiente
de backend (B)".

## 3. Backend

### 3.1 Migración — script `20_create_favoritos_y_deportes.sql`

```sql
CREATE TABLE IF NOT EXISTS favorito_actividad (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
    actividad_id BIGINT NOT NULL REFERENCES actividad(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_favorito_usuario_actividad UNIQUE (usuario_id, actividad_id)
);

CREATE TABLE IF NOT EXISTS deporte_preferido (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
    deporte_id BIGINT NOT NULL REFERENCES deporte(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_deporte_preferido UNIQUE (usuario_id, deporte_id)
);
```

Aditiva, idempotente, `ON DELETE CASCADE` en ambos lados (un favorito es
de la relación usuario-actividad: si cualquiera de los dos se va, la fila
no significa nada). Índices por usuario. PRE/POST en el encabezado.

### 3.2 Endpoints (todos bajo `/api/usuario/**`, autenticados)

- `GET /api/usuario/favoritos` → lista de `ActividadDTO` público (el
  MISMO que los listados, con `imagenPrincipalUrl`, precio, etc.):
  snapshot vivo desde la base — se acabó el favorito congelado. Solo
  actividades publicadas y activas (un favorito de una actividad
  despublicada no rompe: desaparece de la lista, la fila queda).
- `PUT /api/usuario/favoritos/{actividadId}` → 204 (idempotente; 404 si
  la actividad no existe/no está publicada).
- `DELETE /api/usuario/favoritos/{actividadId}` → 204 (idempotente).
- `GET /api/usuario/deportes` → lista de slugs.
- `PUT /api/usuario/deportes` `{slugs: [...]}` → reemplaza el conjunto
  completo (la UI de `/mi-cuenta` edita el set entero; tope: el tamaño
  del catálogo). Slugs desconocidos se ignoran en silencio.
- **Deuda E que este bloque salda**: regla explícita
  `.requestMatchers("/api/usuario/**").authenticated()` en
  `SecurityConfig` (hoy los cubre `anyRequest()`; explícito es lo que
  pedía el pendiente, y este es su "plan previo").

### 3.3 Sin sorpresas operativas

Tablas nuevas vacías, cero migración de datos del lado servidor, config
sin variables nuevas, contratos existentes intactos.

## 4. Frontend

`lib/favoritos.ts` y `lib/preferenciasDeportivas.ts` se escribieron para
esto: "cuando exista sincronización, este módulo es el único punto a
reemplazar — los componentes consumen los hooks".

- **Logueado**: la fuente es el backend. Al iniciar sesión (o al boot con
  sesión), un fetch trae la lista y pisa la cache local del scope
  `u<id>`; los toggles pegan al backend con actualización optimista y
  revert si falla. La cache local del scope sigue existiendo solo para
  pintar instantáneo mientras llega el fetch.
- **Visitante**: exactamente como hoy (el botón de guardar ya manda a
  login; lo legacy del scope invitado queda inerte).
- **`MisFavoritos`** pasa a render del backend (snapshot vivo) con la
  cache como estado inicial.
- **Contadores de `/mi-cuenta`** ya consumen los hooks → gratis.
- **"Tus deportes"**: con el sync, los chips funcionan entre
  dispositivos. Se suma el deep-link real: solapa por URL en `/mi-cuenta`
  (`?tab=deportes`) y "Editar preferencias" apunta ahí.

## 5. Decisiones para Agustín

1. **¿Deportes preferidos entran en este bloque?** Recomendación: **sí**
   — mismo mecanismo, una migración, y cierra la historia de los chips
   que encontraste hoy. (Si no, el plan se recorta a favoritos solo.)
2. **Lo guardado como CUENTA antes del sync** (scope local `u<id>` de
   quien ya usaba la app logueado): ¿se sube al backend en el primer
   login post-deploy? Recomendación: **sí, una única vez por
   dispositivo** — es la propia lista de esa cuenta (la prohibición de
   migrar es del scope de invitado, no del propio). Marca local de
   "ya subido" para no re-subir borrados.
3. **El visitante sigue sin poder guardar** (hoy el botón manda a
   login). Recomendación: mantener.

## 6. Orden de deploy (reglas 2, 12 y dos pushes)

1. Script 20 en Supabase (Agustín) — y en el PostgreSQL local para ITs.
2. Push backend (aditivo) + verificación en producción con curl
   (401 sin token; con token real, alta/listado/baja — smoke de Agustín
   o token de mi usuario de prueba local... en producción lo autenticado
   es de Agustín, como siempre).
3. Push frontend verificado E2E contra backend local.

## 7. Tests

- Unit services + IT del flujo completo (guardar → listar → repetir es
  idempotente → borrar → listar vacío; actividad despublicada no
  aparece; deportes replace-all).
- Frontend: E2E en navegador (guardar en una "computadora", ver en otra
  — simulado con dos perfiles de storage), typecheck/lint/build.

## 8. Fuera de alcance

- Colecciones de guardados, actividad reciente, notificaciones (backlog
  de `/mi-cuenta` §5).
- Merge inteligente multi-dispositivo (last-write-wins por operación
  alcanza para listas de este tamaño).
- Favoritos de visitante con cuenta diferida.
