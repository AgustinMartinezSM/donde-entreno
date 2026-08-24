# Plan — Fase 4 social: galería social y moderación flexible de fotos

Estado: ✅ **CERRADA EN PRODUCCIÓN** (smoke de Agustín OK, 2026-08-24).
`main` = `origin/main` = **`2517c39`**; script 30 aplicado por él en
Supabase y local antes del deploy; backend `3b59493` y frontend
`2517c39` desplegados en dos tandas con marcadores anónimos
verificados (401→200 en `GET /api/imagenes/9/comentarios`, y el DTO
público trayendo `cantidadComentarios`/`comentariosActivados`/
`seccion`). 92 ITs + suite unit en verde con el script aplicado. Es el
bloque que MATERIALIZA tu filosofía nueva de moderación: las fotos de
publicadores dejan de esperar aprobación (publican directo, moderadas
por reportes), y las fotos se vuelven sociales — comentarios, guardar,
secciones. Todo sobre la infraestructura ya probada (reportes,
notificaciones, admin).

## Qué ve la gente al final

1. **El publicador sube una foto y la ve publicada al instante** —
   sin cola de moderación. Si algo incumple normas, la comunidad lo
   reporta y el admin lo oculta (la cola admin queda como herramienta
   reactiva). La aprobación estricta sigue SOLO para actividades
   nuevas, como definiste.
2. **Comentarios en fotos** (visor): la gente comenta, borra lo suyo,
   reporta lo ajeno; el publicador puede ocultar comentarios en sus
   fotos o desactivar comentarios en una foto puntual; el admin oculta
   cualquiera.
3. **Guardar fotos**: bookmark en el visor; las guardadas viven en un
   bloque propio dentro de Guardados (junto a las actividades).
4. **Secciones de galería** (Instalaciones / Entrenamientos / Eventos /
   Equipo): el publicador etiqueta cada foto y las galerías se filtran
   con chips (solo si hay 2+ secciones en uso).

## Migración (script 30 — dos tablas + dos ALTER, una transacción)

- `comentario_imagen`: imagen_id FK CASCADE, usuario_id FK CASCADE,
  texto VARCHAR(500), estado CHECK ('VISIBLE',
  'OCULTO_POR_PUBLICADOR', 'OCULTO_POR_ADMIN',
  'ELIMINADO_POR_USUARIO') DEFAULT VISIBLE, created_at. Índices
  (imagen_id, estado, created_at) y (usuario_id, created_at) para el
  tope diario.
- `foto_guardada`: usuario_id FK CASCADE, imagen_id FK CASCADE,
  created_at, UNIQUE (usuario, imagen) — el patrón exacto de likes.
- `ALTER imagen`: + `comentarios_activados BOOLEAN NOT NULL DEFAULT
  true` y + `seccion VARCHAR(30) NULL` CHECK (INSTALACIONES,
  ENTRENAMIENTOS, EVENTOS, EQUIPO) — NULL = "General", sin backfill.
- **ALTER del CHECK de reporte**: + 'COMENTARIO' (la lección del 25,
  otra vez a tiempo).

## Backend

- **Subida directa** (el cambio de flujo): `ImagenPublicadorService`
  sube al bucket PÚBLICO con estado APROBADA y activa — la lógica de
  "la PRINCIPAL nueva desactiva la anterior" (hoy en la aprobación) se
  muda a la subida. La cola PENDIENTE deja de recibir fotos de
  publicadores; el panel admin de imágenes queda para lo reportado y
  lo histórico. Las notificaciones FOTO_APROBADA/RECHAZADA quedan solo
  para el camino admin (que ahora es excepcional).
- **Comentarios**: crear (auth, solo fotos visibles con comentarios
  activados, tope 20/día por usuario contra la base), listar visibles
  por foto (público, batch de conteos para las galerías), eliminar
  propio, ocultar por el DUEÑO de la foto, ocultar por admin
  (endpoints bajo /api/usuario/**, /api/publicador/** y
  /api/admin/**). Notificación COMENTARIO_NUEVO al publicador. Autor
  como "Nombre A." (el criterio de las valoraciones).
- **Toggle por foto**: PATCH del publicador sobre su imagen
  (`comentariosActivados`).
- **Guardar fotos**: PUT/DELETE/GET `/api/usuario/fotos-guardadas`
  (patrón likes, calcado).
- **Secciones**: el PATCH de imagen del publicador acepta `seccion`
  (catálogo validado); los listados públicos la exponen.
- **Reportes**: tipo COMENTARIO con validación de visibilidad.

## Frontend

- **Visor (lightbox)**: bloque de comentarios (lista + form + borrar
  propio + reportar ajeno + ocultar si sos el dueño) y bookmark de
  guardar foto junto al corazón.
- **Guardados** (`/favoritos`): bloque "Fotos guardadas" (grilla
  chica que abre el visor, con link a la actividad).
- **Gestor del publicador**: selector de sección por foto, toggle de
  comentarios, y el copy de subida ya no habla de moderación
  ("tu foto queda publicada al instante; la comunidad puede
  reportarla").
- **Galerías públicas**: chips de sección (Todo · Instalaciones · ...)
  solo con 2+ secciones en uso.

## Verificación y deploy

1. Unit tests (subida directa, principal que reemplaza, tope de
   comentarios, permisos de ocultar, toggle, secciones) + IT de flujo
   completo en contexto compartido (subir foto → visible al instante
   → comentar → contador → ocultar por publicador → reportar
   comentario → ocultar admin → guardar foto → listar guardadas).
2. Script 30 (vos) → ITs → backend (marcador: OPTIONS de
   fotos-guardadas) → frontend (marcador en chunks) → tu smoke.

## Hallazgo de la implementación: el admin se quedaba sin moderar

Los ITs destaparon algo que el plan no había previsto y que rompía la
mitad del contrato de "moderación flexible": `ImagenAdminService`
validaba `PENDIENTE` **tanto para aprobar como para rechazar**. Con la
subida directa ninguna foto vuelve a nacer PENDIENTE, así que
`rechazar()` respondía 400 **siempre**: llegaba un reporte de una foto
y el admin no tenía ninguna forma de bajarla.

Arreglado en el mismo bloque:

- **`rechazar()` acepta también una APROBADA** (baja reactiva con
  motivo obligatorio). El borrado del archivo elige bucket según el
  estado: `eliminarPublicoPorUrl` para una publicada,
  `eliminar` para una pendiente del legado. Sigue siendo best-effort.
- **`aprobar()` no cambia**: sigue exigiendo PENDIENTE, porque las
  filas que quedaron en la cola de antes del cambio se aprueban igual.
- **Panel `/admin/imagenes`**: el filtro arranca en **Aprobadas** (es
  donde está el trabajo ahora), el botón dice "Dar de baja" sobre una
  publicada y "Rechazar" sobre una pendiente, y "Aprobar" solo aparece
  en las pendientes.
- Tests: IT nuevo `elAdminBajaUnaFotoPublicadaYRequiereMotivo` (incluye
  que bajarla dos veces da 400) y unit
  `bajarUnaFotoPublicadaBorraDelBucketPublicoYGuardaElMotivo`.

Lección para las próximas fases sociales: **al sacar una compuerta
previa hay que verificar que la herramienta reactiva que la reemplaza
esté realmente habilitada** — la validación vieja se quedó en el
código y volvió inalcanzable la única acción del admin.

## Decisiones que pide este plan (con recomendación)

1. **Subida directa YA**: es tu filosofía nueva y el prerrequisito
   (reportes + admin) está vivo y probado. El riesgo real es bajo:
   solo publican fotos los publicadores (cuentas identificadas), no
   cualquier usuario.
2. **Comentarios V1 solo en fotos** (no en actividades ni perfiles):
   la superficie más acotada para estrenar texto libre de la
   comunidad; tope 20/día por usuario; "Nombre A." como autor.
3. **Fotos guardadas dentro de Guardados** (no una página nueva):
   una sola casa para todo lo guardado.
4. **Secciones de catálogo fijo** (sin nombres libres): cuatro valen
   para el caso real y evitan moderar texto en nombres.
