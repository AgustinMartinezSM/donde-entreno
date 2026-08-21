# Plan Fases 5d + 5e — Perfil editable V1: avatar de usuario y nombre del publicador

Estado: **propuesto, pendiente de aprobación de Agustín**. Cierra los
gates #3 (nombre público del publicador) y el pendiente 5d del bloque
(`docs/bloque-contenido-visual-v1.md`). **5d trae la primera migración
del bloque**: nada se aplica en Supabase sin tu autorización expresa, y
como siempre va ANTES que el código que la usa.

## 5e — Nombre público del publicador (SIN migración)

**Diagnóstico**: hoy el publicador edita solo descripción, Instagram y
email de contacto (`PATCH /api/publicador/me`). El nombre público no
tiene NINGUNA vía de cambio, y el "flujo con revisión" que promete el
copy no existe.

**La decisión del gate: ¿edición directa o moderada?**
**Recomendación: directa.** El argumento es de consistencia: la
descripción —que es texto libre mucho más largo y con el mismo riesgo
de contenido ofensivo— ya se edita directo sin moderación. Moderar solo
el nombre protegería menos que lo que ya está abierto. La moderación de
identidad real (¿este club es quien dice ser?) ya tiene su herramienta:
el badge de **verificado**, que es de otro circuito y no se toca.

**Implementación**:
- Backend: `PATCH /api/publicador/me` acepta `nombre` (mismas
  validaciones del registro: requerido al editar, largo máximo del
  schema). Log `PERFIL_NOMBRE_CAMBIADO perfilId={}` (solo metadata).
- Frontend: campo "Nombre público" en `MiPerfilEditor` junto a los que
  ya edita, con la nota de que impacta en el perfil público y en todas
  sus actividades.

## 5d — Avatar de usuario (CON migración: script 21)

**Diagnóstico**: el avatar es de iniciales, sin foto ni backend. La
tabla `imagen` no se puede reusar tal cual: su CHECK exige dueño
`perfil_publicador XOR actividad`. La vía barata ya identificada en
Fase 0: **columna `usuario.avatar_url`**.

**La decisión clave: ¿con o sin moderación?**
**Recomendación: SIN moderación en V1, con este fundamento**: el avatar
de usuario **no tiene ninguna superficie pública hoy** — no existe el
perfil público de usuario; la foto solo la ve su propio dueño (cabecera
de `/mi-cuenta`, menú de cuenta, sheet). Moderar una imagen que solo ve
quien la subió es costo sin beneficio. **Condición explícita**: si
algún bloque futuro crea el perfil público de usuario o muestra
avatares a terceros (likes, check-ins), la moderación se diseña ANTES
de esa superficie, no después.

**Migración (script `21_avatar_usuario.sql`)** — la aplicás vos en
Supabase (SQL Editor, transacción propia, PRE/POST) antes del push del
backend:
```sql
ALTER TABLE usuario ADD COLUMN avatar_url VARCHAR(500);
```
Aditiva pura: nullable, sin default, sin backfill, sin índices. El
código viejo la ignora; el rollback es `DROP COLUMN` sin pérdida ajena.

**Backend** (después de la migración):
- `PUT /api/usuario/avatar` (multipart, autenticado): reutiliza el
  pipeline probado de imágenes — validación por firma de bytes
  (JPEG/PNG/WebP), 2 MB, nombre UUID — pero directo al bucket
  **público** (sin cola de moderación, por lo de arriba), carpeta
  `avatares/{usuarioId}/`. Reemplazo: sube el nuevo, pisa la columna y
  borra el anterior best-effort (patrón `eliminarPublicoPorUrl` de F2).
- `DELETE /api/usuario/avatar`: vuelve a iniciales (columna a NULL +
  borrado best-effort).
- `GET /api/auth/me` y el DTO de sesión suman `avatarUrl` (aditivo).

**Frontend**:
- Recorte 1:1 en el cliente (el editor de encuadre de F2, preset LOGO
  400×400) antes de subir.
- `CabeceraPerfil` y los menús muestran la foto si hay, iniciales si no
  (fallback intacto). Entrada: "Cambiar foto" sobre el avatar de la
  cabecera de `/mi-cuenta` + acción en "Datos de mi cuenta".
- Tolerante al orden de deploys: `avatarUrl` opcional en los tipos.

## Verificación y deploy

- Unit + IT nuevos (subida con bytes reales al almacén en memoria,
  reemplazo borra el anterior, DELETE anula, anónimo 401; nombre del
  publicador: validaciones y persistencia).
- Orden estricto: **script 21 en Supabase (vos) → push backend
  (marcador OPTIONS de `/api/usuario/avatar`) → push frontend**
  (marcador "Cambiar foto" en chunks). Regla 12 antes del deploy.
- Smoke tuyo: subir tu foto, verla en cabecera/menú, reemplazarla,
  quitarla; cambiar el nombre público del club y verlo en el perfil
  público y sus actividades.

## Decisiones que pide este plan

1. **5e nombre público: edición directa** (sin migración). ¿Ok?
2. **5d avatar: columna `usuario.avatar_url`** — autorizar el script 21
   (te paso el SQL final con PRE/POST cuando lo aprobemos).
3. **Avatar sin moderación en V1** (sin superficie pública hoy; la
   condición queda escrita). ¿Ok?
4. **Bucket público directo con reemplazo best-effort** (patrón F2).
   ¿Ok?
