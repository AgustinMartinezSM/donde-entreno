# Plan — Refresh token y sesión persistente

## 1. Problema

La sesión vive en `sessionStorage`: muere al cerrar la pestaña y no existe
en una pestaña nueva. El access token dura 60 minutos y no hay forma de
renovarlo: pasada la hora, la persona re-tipea la contraseña. Es la
fricción más repetida del producto (se sufrió dos veces solo en la
verificación del 18/08) y `docs/social-sports-experience.md` la lista como
deuda desde la V2.

## 2. La restricción que ordena todo el diseño

**El frontend (Vercel) y la API (Render) están en dominios distintos.**
La solución canónica —refresh token en cookie `HttpOnly` del dominio de la
API— acá es una cookie de terceros: Safari la bloquea hoy y Chrome está en
camino. No hay cookie confiable entre `donde-entreno-web.vercel.app` y
`donde-entreno-api.onrender.com`.

Alternativas evaluadas:

| Opción | Veredicto |
|---|---|
| Cookie `HttpOnly` cross-site | ❌ bloqueada por third-party cookie policies |
| Proxyear TODA la API por Next para cookie first-party | ❌ re-arquitectura: latencia, costo por invocación, toca todos los services |
| Access token largo (30 días) en storage | ❌ un solo token robado vale 30 días y no se puede revocar |
| **Refresh token opaco rotativo en `localStorage` + access corto como hoy** | ✅ persistencia real, robo acotado por rotación + revocación por familia |

El refresh token en `localStorage` tiene la misma clase de exposición XSS
que el `sessionStorage` actual (no empeora el modelo de amenaza existente)
y lo compensa con tres propiedades que hoy no existen: **es opaco** (no es
un JWT, no contiene nada), **rota en cada uso** (un token robado ya usado
delata el robo) y **se revoca en el servidor** (logout real, imposible con
el JWT actual).

> **Decisión de Agustín (2026-08-19), para releer cuando cambie la
> infraestructura**: `localStorage` se acepta **por la infraestructura
> actual** (Vercel/Render en dominios separados) y **no es la opción
> ideal a largo plazo**. Cuando la plataforma tenga dominio propio
> (frontend y API como subdominios del mismo sitio), reevaluar cookie
> `HttpOnly` first-party o arquitectura BFF. La tabla y los endpoints de
> este bloque sirven igual en ese mundo: lo único que cambia es dónde
> guarda el token el cliente.

## 3. Backend

### 3.1 Migración — script `19_create_refresh_token.sql` (ANTES que el código, regla 2)

```sql
CREATE TABLE IF NOT EXISTS refresh_token (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL REFERENCES usuario(id),
    -- SHA-256 hex del token. El token en claro NUNCA se guarda:
    -- un dump de la tabla no sirve para autenticarse.
    token_hash VARCHAR(64) NOT NULL,
    -- Cadena de rotación: todos los tokens que descienden del mismo
    -- login comparten familia. Detectado un reuso, cae la familia entera.
    familia UUID NOT NULL,
    emitido_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expira_en TIMESTAMPTZ NOT NULL,
    usado_en TIMESTAMPTZ,
    revocado_en TIMESTAMPTZ,
    CONSTRAINT uq_refresh_token_hash UNIQUE (token_hash)
);

CREATE INDEX IF NOT EXISTS idx_refresh_token_usuario ON refresh_token (usuario_id);
CREATE INDEX IF NOT EXISTS idx_refresh_token_familia ON refresh_token (familia);
```

Aditiva, idempotente, transacción propia, consultas PRE/POST según el
modelo del checklist. No toca ninguna tabla existente.

### 3.2 Servicio `RefreshTokenService`

- **Emitir** (en login/registro): 256 bits aleatorios (`SecureRandom`) en
  base64url → se guarda SOLO el SHA-256. Familia nueva por login.
- **Rotar** (en `/refresh`): buscar por hash. Inexistente, vencido o
  revocado → 401. **Ya usado → es un reuso: se revoca la familia entera y
  401** (o el ladrón o la víctima quedan afuera; el que tiene la
  contraseña re-entra, el otro no). Válido → marcar `usado_en`, emitir
  token nuevo de la misma familia y un access token nuevo.
- **Revocar familia** (en logout): `revocado_en = NOW()` para toda la
  familia.
- **Higiene sin scheduler**: en cada login se borran los tokens del
  usuario vencidos hace más de 30 días. Sin job nuevo, la tabla no crece
  sin límite.

### 3.3 Endpoints (aditivos, no rompen contratos — regla 7)

- `POST /api/auth/refresh` `{refreshToken}` → misma forma que login
  (`tokenType`, `accessToken`, `expiresIn`, `usuario`) + `refreshToken`
  nuevo. Público (`permitAll`), mismo tratamiento que `/login`.
- `POST /api/auth/logout` `{refreshToken}` → 204 siempre (revoca si
  existe; no filtra si un token era válido).
- `login` y `registro/*` **suman** `refreshToken` y `refreshExpiresIn` a
  la respuesta. Un cliente viejo los ignora: aditivo puro.

### 3.4 Config

`dondeentreno.auth.refresh.expiration-days` (default **30**) →
`DONDEENTRENO_AUTH_REFRESH_EXPIRATION_DAYS` en Render **solo si se quiere
cambiar el default**: sin tocar el panel, funciona.

## 4. Frontend

- **El access token y la sesión siguen en `sessionStorage`** exactamente
  como hoy. Lo único que va a `localStorage` es el refresh token
  (`donde_entreno_refresh_token`), la cuenta del último login (una por
  navegador, como hoy).
- **Boot de pestaña** (`AuthSessionProvider`): sin sesión en
  `sessionStorage` pero con refresh en `localStorage` → `POST /refresh` →
  sesión nueva → la pestaña "recuerda" sin pedir contraseña. Si el
  refresh falla → invitado, refresh descartado.
- **Renovación proactiva**: timer que refresca ~10 minutos antes del
  vencimiento del access token. Una pestaña dormida más de 60 min
  refresca al despertar (el timer corre al recuperar foco). No se agrega
  interceptor global de 401 en esta V1: el token que los services reciben
  del provider ya viene fresco.
- **Logout cross-tab**: el logout borra el refresh de `localStorage`,
  llama `POST /logout` (best effort) y las demás pestañas se enteran por
  el evento `storage` y cierran sesión también. Hoy el marker de logout
  es por pestaña; esto lo vuelve real.
- **Cookie `de_sesion`** (guard del proxy, falsificable a propósito): su
  vencimiento pasa del horizonte del access token (60 min) al del
  refresh (30 días). Si no, al volver mañana el proxy redirige a login
  antes de que el provider pueda refrescar. Sigue sin contener ningún
  token; el backend valida JWT igual que siempre.

## 5. Qué amenaza cubre cada pieza

| Amenaza | Mitigación |
|---|---|
| Dump de la tabla | Solo hashes SHA-256: no autentican |
| Refresh token robado y usado | La próxima rotación del legítimo detecta el reuso → familia revocada |
| Logout "de mentira" (hoy: el JWT sigue siendo válido 60 min) | Revocación de familia en servidor; el access muere solo en ≤60 min |
| XSS | Mismo modelo que hoy (storage accesible por JS); el daño se acota por rotación + revocación. La mitigación real de XSS es no tener XSS |
| Fuerza bruta contra `/refresh` | Token de 256 bits: inviable. Rate limiting de `/auth/*` queda como la misma deuda que ya tiene `/login` (E) |

## 6. Decisiones para Agustín

1. **¿Persistir por defecto o checkbox "Recordarme"?** Recomendación:
   **persistir por defecto, sin checkbox** en V1 — es lo que esperan las
   apps sociales, y el logout limpia todo de verdad (ahora sí, con
   revocación). El checkbox se agrega después si molesta en computadoras
   compartidas. (El login de referencia mostraba "Recordarme", pero ese
   checkbox nunca existió en el código.)
2. **Duraciones**: access 60 min (como hoy) + refresh **30 días
   deslizante** (cada rotación renueva la ventana). Alternativa: tope
   duro por familia (p. ej. 90 días) — se puede sumar después sin
   migración (la familia ya registra su primer `emitido_en`).

## 7. Orden de deploy (reglas 2, 12 y la de los dos pushes)

1. **Script 19 en Supabase** (Agustín, SQL Editor, PRE/POST) — antes que
   cualquier push de backend.
2. Aplicar script 19 en el PostgreSQL local (para los ITs con
   `ddl-auto=validate`).
3. **Push backend** (aditivo). Antes: backend local APAGADO (regla 12).
   Verificar en producción con curl: login devuelve `refreshToken`,
   `/refresh` rota, el refresh viejo da 401 con familia revocada,
   `/logout` 204.
4. **Push frontend** recién con el backend verificado (un 404 de
   `/refresh` no se distingue de un endpoint caído).
5. Smoke: login → cerrar pestaña → abrir → sigue adentro; logout → abrir
   → invitado; segunda pestaña se entera del logout.

## 8. Tests

- Unit `RefreshTokenService`: emitir/rotar/expirado/revocado/**reuso
  revoca familia**/higiene de vencidos.
- IT nuevo (PostgreSQL local): login → refresh → refresh con el token
  viejo → 401 y familia entera revocada → logout → refresh → 401.
- Frontend: typecheck/lint/build + verificación en navegador del ciclo
  completo (pestaña nueva, timer, logout cross-tab, cookie a 30 días).

## 9. Fuera de alcance (anotado, no olvidado)

- Rate limiting de `/api/auth/*` (deuda preexistente de `/login`).
- Lista de sesiones activas / "cerrar todas las sesiones" (la tabla ya
  lo permite: es UI + un endpoint más).
- Múltiples cuentas por navegador.
- Interceptor global de 401 con retry (V2 si el timer no alcanza).
