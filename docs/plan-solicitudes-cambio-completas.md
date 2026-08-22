# Plan — Solicitudes de cambio completas (horarios, ubicación, deporte, edades y enfoque)

Estado: **propuesto, pendiente de aprobación de Agustín**. Cierra el
gap funcional más señalado del panel (freno 2026-08 §B): hoy esos cinco
aspectos no son editables por NINGÚN camino. Trae migración (script
24, versionado junto a este plan): nada se aplica sin tu autorización,
y siempre ANTES que el código.

## El diseño existente que se extiende (no se inventa nada)

El circuito de cambios ya probado: cada campo propuesto en NULL = "sin
cambio"; el mapper arma el antes/después para el admin
(`construirCambios`), lista los campos tocados y aplica en la
aprobación dentro de la transacción (`aplicarCambios`). Una sola
solicitud abierta por actividad (409). En pausa también se puede pedir
(fase 6). Este bloque SOLO suma campos a ese contrato.

## Migración (script `24_solicitud_cambio_completa.sql`)

1. Columnas nuevas en `solicitud_cambio_actividad` (todas nullable =
   sin cambio propuesto):
   - `deporte_id BIGINT` (FK a deporte).
   - `edad_minima INT`, `edad_maxima INT`.
   - `enfoque VARCHAR(50)`.
   - Ubicación propuesta: `ubicacion_nombre VARCHAR(150)`,
     `ubicacion_direccion VARCHAR(255)`, `ubicacion_referencia
     VARCHAR(255)`, `ubicacion_barrio_id BIGINT` (FK a barrio).
   - `cambia_horarios BOOLEAN NOT NULL DEFAULT false` — el flag existe
     porque "cero filas hijas" no puede distinguir "no toco horarios"
     de "propongo borrar todos"; con el flag, true exige ≥1 horario.
2. Tabla `solicitud_cambio_horario` — calco exacto de
   `solicitud_publicacion_horario` (FK con ON DELETE CASCADE, CHECK de
   días, hora_inicio/fin TIME): el conjunto PROPUESTO completo.

Aditiva pura; rollback = drop de columnas y tabla.

## Reglas de negocio (las decisiones del plan)

1. **Horarios = REEMPLAZO TOTAL del conjunto** (mismo criterio que los
   deportes preferidos y que la solicitud de publicación): el
   publicador propone la grilla completa nueva; al aprobar, los
   horarios vigentes pasan a `activo=false` (historial, no borrado) y
   se crean los propuestos.
2. **Ubicación dentro de la MISMA ciudad**: se proponen
   nombre/dirección/referencia/barrio; el barrio debe pertenecer a la
   ciudad actual de la actividad. Cambiar de ciudad NO entra en este
   flujo (es otra decisión de producto). Dirección y barrio van juntos
   u omitidos.
3. **Regla anti-efecto colateral al aplicar ubicación**: si la
   ubicación actual es EXCLUSIVA de esta actividad → se edita en el
   lugar; si está COMPARTIDA con otra actividad activa → se crea una
   ubicación nueva del perfil y la actividad apunta ahí. Editar una
   sede compartida movería de dirección a otras actividades sin que
   nadie lo pidiera.
4. **Deporte cambiable** (validado contra el catálogo activo). El slug
   de la actividad no cambia (viene del título): ningún link se rompe.
5. **Edades y enfoque**: min/max proponibles por separado; la
   validación corre sobre el RESULTADO combinado (propuesto + vigente):
   min ≤ max. Enfoque contra el dominio existente
   (RECREATIVO/COMPETITIVO/FORMATIVO — el mismo del alta).

## Backend

- `SolicitudCambioActividadRequestDTO` suma los campos + lista
  `horarios[{diaSemana, horaInicio, horaFin, observacion}]`; las
  validaciones son espejo de la solicitud de publicación (días del
  dominio, fin > inicio, sin solapamientos exactos duplicados).
- `SolicitudCambioActividadMapper`: `construirCambios` suma los campos
  nuevos (deporte por nombre, ubicación como resumen "Sede X, Dirección
  Y (Barrio Z)", horarios como "N horarios → M horarios" con el detalle
  en líneas), `listarCamposPropuestos` y `aplicarCambios` extendidos —
  la aplicación de horarios y de ubicación vive en el ADMIN service
  (necesita repos), el mapper aplica solo los campos planos.
- El detalle del admin y del publicador muestran el antes/después de
  todo con el mismo `CampoCambioDTO` de siempre.

## Frontend

- `SolicitarCambiosForm` suma secciones: Deporte (select del catálogo),
  Edades, Enfoque, Ubicación (nombre/dirección/referencia/barrio de la
  ciudad actual) y Horarios (editor de filas día+inicio+fin+observación
  con agregar/quitar, prellenado con los vigentes al activar "cambiar
  horarios").
- El detalle de solicitud del publicador y la cola del admin muestran
  los campos nuevos sin cambios de estructura (ya listan campos
  dinámicos).

## Verificación y deploy

- Unit del mapper y services (validaciones, regla
  exclusiva/compartida, reemplazo de horarios) + IT de flujo completo:
  crear solicitud con TODO propuesto → aprobar → la actividad pública
  refleja deporte/edades/enfoque/ubicación/horarios nuevos, los
  horarios viejos quedan inactivos, y la ubicación compartida NO se
  toca (se crea nueva).
- Dos pushes (backend con marcador OPTIONS/campo nuevo en respuesta,
  frontend con marcador en chunks), regla 12. Orden: script 24 en
  Supabase y local (vos) → ITs → backend → frontend → tu smoke.

## Decisiones que pide este plan

1. **Horarios como reemplazo total del conjunto** (con flag explícito).
   **Recomendación: sí.**
2. **Ubicación solo dentro de la misma ciudad**, con la regla
   exclusiva-vs-compartida al aplicar. **Recomendación: sí.**
3. **Deporte cambiable vía este flujo** (moderado, catálogo activo).
   **Recomendación: sí.**
4. **Script 24**: autorizar y aplicar (Supabase + local) cuando
   arranquemos.
