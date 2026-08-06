# Plan técnico — Solicitud de cambios sobre actividades aprobadas

Bloque 3 del roadmap. **Estado: IMPLEMENTADO** (B3.1 `c256414`, B3.2+B3.3 `bbdcc34`, B3.4 `9769a55`, B3.5 `1bcb164`, B3.6 `b6038af`).
Modelo elegido (definido en el roadmap): **edición con revisión, no edición directa** — la actividad pública no cambia hasta que un admin aprueba.

## Decisiones de diseño

1. **Tabla nueva `solicitud_cambio_actividad`**, espejo del patrón ya probado de `solicitud_publicacion` (estados, CHECKs, motivo de rechazo, borrado lógico). Migración aditiva `14_create_solicitud_cambio_actividad.sql` — no toca tablas existentes, rollback = no correr el script.
2. **Columnas espejo tipadas, no JSONB**: coherente con el estilo del schema actual (todo columnas explícitas con CHECKs). Cada columna nullable significa "sin cambio propuesto"; con valor, es el valor propuesto.
3. **Alcance V1 de campos editables**: `titulo`, `descripcion`, `precio_referencia`, `mostrar_precio`, `whatsapp_contacto`, `instagram_contacto`, `email_contacto`, `nivel`, `modalidad`. **Fuera de V1**: deporte, ubicación/barrio, horarios e imágenes (los dos primeros cambian el "qué es" de la actividad — ameritan flujo propio; horarios necesitan tabla hija como `solicitud_publicacion_horario`, va en V2).
4. **Una solicitud pendiente por actividad**: índice único parcial sobre `(actividad_id)` con `estado IN ('PENDIENTE','EN_REVISION') AND deleted_at IS NULL`. Evita colas ambiguas de diffs.
5. **Historial**: las solicitudes aprobadas/rechazadas no se borran — la tabla ES el historial de cambios (quién pidió, qué, cuándo, quién resolvió, motivo).

## Schema propuesto (resumen)

```sql
CREATE TABLE solicitud_cambio_actividad (
    id BIGSERIAL PRIMARY KEY,
    actividad_id BIGINT NOT NULL REFERENCES actividad(id),
    perfil_publicador_id BIGINT NOT NULL REFERENCES perfil_publicador(id),
    usuario_id BIGINT NOT NULL REFERENCES usuario(id),
    estado VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE',
    -- Campos propuestos (NULL = sin cambio)
    titulo VARCHAR(150),
    descripcion TEXT,
    precio_referencia NUMERIC(10,2),
    mostrar_precio BOOLEAN,
    whatsapp_contacto VARCHAR(50),
    instagram_contacto VARCHAR(150),
    email_contacto VARCHAR(150),
    nivel VARCHAR(30),
    modalidad VARCHAR(30),
    -- Resolución
    motivo_rechazo TEXT,
    resuelto_por_usuario_id BIGINT REFERENCES usuario(id),
    resuelto_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ
    -- CHECKs: estado en (PENDIENTE, EN_REVISION, APROBADA, RECHAZADA);
    -- motivo_rechazo obligatorio si RECHAZADA y vacío en el resto;
    -- al menos un campo propuesto no nulo;
    -- nivel/modalidad dentro de los dominios ya usados por actividad.
);
-- Índice único parcial: una pendiente/en revisión por actividad.
-- Índices por actividad_id, perfil_publicador_id y estado.
```

## Endpoints

**Publicador** (rol PUBLICADOR, ownership validado como en `PublicadorActividadService`):
- `POST /api/publicador/actividades/{id}/solicitudes-cambio` — crea (409 si ya hay una pendiente; 404 si la actividad no es propia o no está PUBLICADA).
- `GET /api/publicador/solicitudes-cambio` (+ filtro estado, paginado) y `GET .../{id}`.

**Admin** (ADMIN/SUPER_ADMIN):
- `GET /api/admin/solicitudes-cambio` (+ estado, paginado).
- `GET /api/admin/solicitudes-cambio/{id}` — detalle con **antes/después**: DTO con valor actual y valor propuesto por campo (el diff lo arma el backend; el frontend solo pinta).
- `PATCH .../{id}/estado` — PENDIENTE ↔ EN_REVISION, RECHAZADA (con motivo obligatorio).
- `POST .../{id}/aprobar` — transaccional: aplica solo los campos no nulos sobre `actividad`, marca APROBADA + resuelto_por/resuelto_at. La actividad pública recién cambia acá.

## Frontend

- **Publicador**: botón "Solicitar cambios" en el detalle de actividad propia → formulario prellenado con los valores actuales (solo campos V1); lista "Mis solicitudes de cambio" con estado y motivo de rechazo.
- **Admin**: cola de solicitudes de cambio + vista de comparación antes/después (dos columnas, resaltando solo los campos que cambian) + aprobar/rechazar con motivo.

## Riesgos y mitigaciones

- **Carrera aprobación/edición simultánea**: el índice único parcial + transacción en aprobar lo acotan; la aprobación relee la actividad dentro de la transacción.
- **Datos propuestos que quedan inválidos** (ej. la actividad cambió de dueño o se despublicó entre pedido y aprobación): la aprobación revalida ownership y estado PUBLICADA; si no, rechaza con motivo automático claro.
- **Divergencia de validaciones**: reutilizar las mismas reglas de Bean Validation que usa la creación de solicitudes (tamaños alineados al schema).
- **Rollback**: feature 100% aditiva — borrar endpoints/UI y no correr la migración; nada existente depende de la tabla nueva.

## División en bloques chicos (orden de implementación)

1. **B3.1** — Migración 14 + entidad JPA + repository (+ script de verificación 15). Validar: `mvnw test`.
2. **B3.2** — Endpoints publicador (crear/listar/detalle) + tests service y controller.
3. **B3.3** — Endpoints admin (listar/detalle con diff/estado/aprobar transaccional) + tests, incluyendo el caso de revalidación en aprobar.
4. **B3.4** — UI publicador (botón + form + lista).
5. **B3.5** — UI admin (cola + diff + resolver).
6. **B3.6** — IT de flujo completo con perfil `integration-local`.
