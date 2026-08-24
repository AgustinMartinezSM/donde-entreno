# Plan — Fase 2 social: notificaciones, reportes, tracking y edición de datos

Estado: **propuesto, pendiente de aprobación de Agustín**. Es la
infraestructura de la que depende todo lo social que viene
(valoraciones, Q&A, comentarios, canales), más la regla de producto
del smoke de Fase 1: los datos del usuario se editan INLINE en
`/configuracion`, estilo Instagram.

## Qué ve la gente al final

1. **Campanita** en el header (desktop y mobile) con contador de no
   leídas; panel de notificaciones (drawer en desktop, pantalla en
   mobile) con marcar leída / todas leídas. Primeros emisores: los
   flujos que YA existen — actividad aprobada/rechazada, cambio
   aprobado/rechazado, foto aprobada/rechazada (publicador), nuevo
   seguidor (publicador), y actividad nueva de un publicador que
   seguís (usuarios).
2. **Reportar** en las fotos (lightbox) y en el perfil del publicador,
   con motivos predefinidos; cola de reportes en el panel admin.
3. **Edición de datos inline en `/configuracion`**: nombre y apellido
   editables ahí mismo (el diálogo de solo lectura muere); el email
   queda visible con nota (cambiarlo es credencial de login y sin
   verificación de email —PAUSADA— sería inseguro).
4. Invisible pero clave: **tracking de interacciones** (vista de
   detalle, click en WhatsApp, compartir) — anónimo, agregado, sin
   usuario — que alimenta las métricas del publicador ("tu actividad
   tuvo N vistas y M contactos este mes") y los rankings futuros.

## Migración (script 28 — tres tablas aditivas, una transacción)

- `notificacion`: usuario_id FK CASCADE, tipo VARCHAR(40), titulo
  VARCHAR(150), ruta VARCHAR(255) NULL (link interno), leida BOOLEAN
  NOT NULL DEFAULT false, created_at. Índices (usuario_id, leida) y
  (usuario_id, created_at).
- `reporte`: usuario_id FK CASCADE, tipo_objeto VARCHAR(30) CHECK
  (IMAGEN, PERFIL_PUBLICADOR, ACTIVIDAD), objeto_id BIGINT, motivo
  VARCHAR(40) CHECK (catálogo fijo), detalle VARCHAR(280) NULL,
  estado VARCHAR(20) CHECK (PENDIENTE, REVISADO, DESESTIMADO,
  ACCIONADO) DEFAULT PENDIENTE, created_at. UNIQUE (usuario_id,
  tipo_objeto, objeto_id): reportar dos veces lo mismo no duplica.
- `evento_interaccion`: actividad_id FK CASCADE, tipo VARCHAR(30)
  CHECK (VISTA_DETALLE, CLICK_WHATSAPP, CLICK_COMPARTIR), created_at.
  **Sin usuario_id a propósito** (privacidad: agregado puro). Índice
  (actividad_id, tipo, created_at).

## Backend

- **Notificaciones** (`/api/usuario/notificaciones`): GET paginado,
  GET `/contador` (no leídas), PATCH `/{id}/leida`, PATCH
  `/todas-leidas`. `NotificacionService.emitir(...)` interno, llamado
  desde los 5 flujos existentes (aprobaciones, moderación de fotos,
  seguimiento, fan-out a seguidores al aprobar actividad — batch, no
  N+1). Falla de emisión NUNCA rompe el flujo que la origina
  (try/catch + log).
- **Reportes**: POST `/api/usuario/reportes` (valida objeto existente
  y visible; idempotente por el UNIQUE); admin: GET
  `/api/admin/reportes?estado=` paginado + PATCH `/{id}/estado`.
  Notificación al admin NO en V1 (lo ve en su cola).
- **Tracking**: POST `/api/actividades/{id}/interacciones` público
  (los visitantes también cuentan), cuerpo `{tipo}`, con rate limit
  por IP en memoria (patrón del asistente: p. ej. 30/min) y 204 de
  respuesta. Métricas del publicador suman conteos 30 días por
  actividad (query agrupada, sin N+1).
- **Edición de datos**: PATCH `/api/usuario/perfil` `{nombre,
  apellido}` (sin migración: las columnas existen), validaciones de
  longitud, devuelve el usuario actualizado.

## Frontend

- Campanita en header + barra mobile (badge con contador; polling
  suave cada 60s + al recuperar foco, nunca websockets). Panel con
  lista, agrupación simple por fecha, click → navega a `ruta` y marca
  leída.
- Botón "Reportar" en LightboxFotos y en el perfil público
  (menú/acción secundaria), con diálogo de motivos. `/admin/reportes`
  nueva página con la cola y acciones de estado.
- `/configuracion`: "Datos de mi cuenta" pasa a formulario inline
  (nombre, apellido editables; email solo lectura con nota); el
  diálogo de solo lectura se retira. `actualizarUsuario` del provider
  refleja el cambio en toda la app al instante (patrón del avatar).
- Tracking: beacon en la vista del detalle (una vez por carga), en el
  click de WhatsApp (ContactButton) y en compartir. `sendBeacon` con
  fallback fetch; jamás bloquea la navegación.

## Verificación y deploy

1. Unit tests (emisión no rompe flujos, contador, idempotencia del
   reporte, validaciones del PATCH) + ITs en contexto compartido
   (flujo completo de notificación al aprobar, reporte + cola admin,
   tracking + métricas, edición de datos persistida).
2. Script 28 en Supabase y local (vos) → ITs → **backend** (marcador:
   `OPTIONS /api/usuario/notificaciones` con `Allow` solo en el build
   nuevo) → **frontend** (marcador campanita en chunks) → tu smoke.

## Decisiones que pide este plan (con recomendación)

1. **Emisores V1 de notificaciones**: los 5 flujos existentes (sin
   email/push, contador por polling 60s + foco). Todo lo demás se
   suma en su fase.
2. **Reportes V1 sin auto-ocultar**: solo cola admin. El auto-ocultar
   tras N reportes entra con los comentarios (Fase 4), donde el
   riesgo es texto libre; hoy todo lo reportable ya pasó por
   moderación.
3. **Tracking anónimo puro** (sin usuario_id, ni siquiera nullable):
   privacidad primero y alcanza para métricas y rankings. Endpoint
   público con rate limit por IP.
4. **Email no editable en V1**: es la credencial de login; cambiarlo
   sin verificación (proveedor PAUSADO por tu decisión) permite
   perder la cuenta con un typo o robarla con sesión abierta. Nombre
   y apellido sí, inline. La bio espera a que exista superficie
   pública de usuario.
