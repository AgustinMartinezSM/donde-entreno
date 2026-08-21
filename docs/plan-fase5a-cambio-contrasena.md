# Plan Fase 5a — Cambio de contraseña logueado (sin proveedor de email, sin migración)

Estado: **aprobado por Agustín con las 4 recomendaciones (2026-08-21) e implementado tal cual**. Bloque: `docs/bloque-contenido-visual-v1.md`.

## Qué es y qué no es

Cambio de contraseña **con sesión activa y conociendo la actual**. No es el reset por email ("me olvidé la contraseña"): ese es 5c y sigue bloqueado por el gate del proveedor. No hay migración: usa `usuario.password_hash` y la tabla `refresh_token` (script 19) tal como están. No se toca Supabase.

## Diagnóstico (2026-08-21)

- `AuthService.validarPassword` ya tiene la política de registro (≥8, letra, número, confirmación) — se reutiliza idéntica: una sola política en un solo lugar.
- `PasswordEncoder` (BCrypt) ya está inyectado en `AuthService`; verificar la actual es `passwordEncoder.matches`.
- `RefreshTokenRepository` tiene `revocarFamilia(familia)` pero **no** `revocarTodasDe(usuarioId)` — es la pieza que falta (ya lo decía el diagnóstico de Fase 0).
- Frontend: `guardarSesionAuth()` es **el único punto** que persiste access + refresh ("todos los caminos que crean sesión pasan por acá") — la respuesta del cambio entra por ahí sin tocar nada más. El diálogo "Datos de mi cuenta" (MenuAjustes) ya avisa que los datos son de solo lectura: ahí se cuelga la entrada.

## Backend

**`POST /api/auth/cambiar-password`** (autenticado por JWT, mismo controller `AuthController`).

Request: `{ passwordActual, passwordNueva, confirmarPassword }`.

Flujo en `AuthService.cambiarPassword(userId, request)`, **una sola transacción**:
1. Cargar usuario activo (`findByIdAndActivoTrueAndDeletedAtIsNull`).
2. `passwordEncoder.matches(passwordActual, hash)` — si no coincide → **400** con "La contraseña actual no es correcta." (no 401: el 401 queda reservado a sesión inválida, así el frontend nunca confunde "te equivocaste de contraseña" con "se te venció la sesión").
3. `validarPassword(passwordNueva, confirmarPassword)` — la misma de registro. Además `passwordNueva != passwordActual` → 400 "La contraseña nueva no puede ser igual a la actual."
4. Actualizar `password_hash` + `updated_at`.
5. **Revocar TODOS los refresh tokens vivos del usuario** — repo nuevo `revocarTodasDe(usuarioId)` (`SET revocado_en = now WHERE usuario_id = :id AND revocado_en IS NULL`). Es el beneficio de seguridad real: una sesión robada muere acá.
6. Emitir sesión nueva (familia nueva) y responder **`LoginResponseDTO` igual que el login** → el dispositivo donde se hizo el cambio queda logueado sin fricción.

Log (solo metadata, nunca contraseñas): `Auth: PASSWORD_CAMBIADO usuarioId={} familiasRevocadas={}` — grepeable como el resto.

**Rate limit**: contador en memoria por usuario de **5 intentos fallidos / 15 min → 429** ("Demasiados intentos. Probá de nuevo en unos minutos."). Protege del escenario concreto: un atacante con una sesión robada (access token) probando contraseñas para tomar la cuenta completa. Clase chica autocontenida (patrón `LimitadorConsultas` del asistente); constantes, sin env vars nuevas. El contador se limpia con un cambio exitoso.

**Limitación documentada** (la misma del logout): los access tokens ya emitidos en otros dispositivos siguen válidos hasta 60 min (no hay blacklist de JWT). El refresh muerto garantiza que esas sesiones no sobreviven la hora.

## Frontend

- **MenuAjustes** (engranaje de `/mi-cuenta`, lo ven los tres roles): opción nueva **"Cambiar contraseña"** que abre un diálogo propio (mismo patrón `<dialog>` showModal que "Datos de mi cuenta").
- Formulario: contraseña actual + nueva + confirmar, cada campo con el ojito mostrar/ocultar de Fase 1 (mismo componente/patrón accesible). Ayuda visible: "Mínimo 8 caracteres, con al menos una letra y un número."
- Submit → `authService.cambiarPassword(accessToken, request)` → éxito: `guardarSesionAuth(respuesta)` (persiste la sesión nueva por el camino único existente) + mensaje "Contraseña actualizada. Por seguridad, cerramos tu sesión en los demás dispositivos." y el diálogo queda en estado de éxito.
- Errores: 400 mapeado al campo que corresponda; 429 con el mensaje de espera; 401 (sesión realmente vencida) cae en el manejo de sesión normal.
- El texto "estos datos son de solo lectura" del diálogo de datos se ajusta: la contraseña ya se puede cambiar.

## Tests

- **Unit** (`AuthServiceTest`): actual incorrecta → 400; política aplicada a la nueva; nueva == actual → 400; éxito llama a `revocarTodasDe` y emite sesión nueva; el limitador corta al 6.º intento fallido y se limpia con el éxito.
- **IT** (patrón `RefreshTokenIT`, contra PostgreSQL local): login en dos "dispositivos" → cambio en uno → el refresh del otro muere con 401, el refresh devuelto por el cambio sigue vivo, y el login con la contraseña nueva funciona (y con la vieja ya no).

## Deploy (regla de los dos pushes)

1. Backend primero. Marcador: `OPTIONS /api/auth/cambiar-password` responde `Allow` con `POST` solo en el build nuevo. Regla 12 antes del deploy de Render (backend local apagado, verificado).
2. Frontend después. Marcador: "Cambiar contraseña" en los chunks de `/mi-cuenta`.
3. Smoke de Agustín: cambiar su contraseña real, verificar que la sesión del dispositivo sigue viva, que otro dispositivo logueado pide login de nuevo (al vencer el access), y volver a cambiarla si quiere restaurarla.

## Decisiones que pide este plan

1. **La sesión actual sobrevive al cambio** (respuesta = sesión nueva, como el login). Alternativa: desloguear todo y forzar re-login — más "seguro" en apariencia pero peor UX sin beneficio real (quien cambia la contraseña acaba de probar que la sabe). **Recomendación: sobrevive.**
2. **400 para "actual incorrecta"** (401 solo para sesión inválida). **Recomendación: sí.**
3. **Rate limit 5 fallos/15 min por usuario, en memoria.** Se reinicia con cada deploy (igual que la cuota de Gemini) — aceptado a propósito. **Recomendación: sí.**
4. **Revocación total de refresh tokens en la misma transacción.** **Recomendación: sí.**
