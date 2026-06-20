# Testing y cierre - Solicitudes reales de publicación

Este documento registra el cierre del bloque de solicitudes reales de publicación en DondeEntreno.

El objetivo del bloque fue permitir que una persona pueda enviar una solicitud real desde la página pública `/publicar`, dejando la actividad pendiente de revisión antes de ser publicada.

## Alcance del bloque

### Base de datos

* Se creó la tabla `solicitud_publicacion`.
* Se creó la tabla `solicitud_publicacion_horario`.
* Se agregó una migración no destructiva en `05_create_solicitud_publicacion.sql`.
* Se agregaron pruebas locales con rollback en `06_test_solicitud_publicacion_queries.sql`.
* No se ejecutó el script 06 en producción.
* La migración 05 quedó aplicada en Supabase.
* Las tablas nuevas fueron verificadas en producción.

### Backend

* Se creó un endpoint público para recibir solicitudes.
* Se agregaron validaciones.
* Se implementó persistencia transaccional.
* Las solicitudes se crean con estado inicial `PENDIENTE`.
* Se guarda un código de seguimiento.
* Se guarda la información principal de la solicitud.
* Se guardan los horarios asociados.
* Se agregaron tests.
* Se validó el build final.
* Se desplegó correctamente en Render.

### Frontend

* La página `/publicar` fue convertida en un formulario real.
* Se agregaron campos para datos del publicador, actividad, ubicación, contacto y horarios.
* Se agregaron horarios dinámicos.
* Se agregaron validaciones.
* El formulario envía datos al backend de producción.
* Se muestra mensaje de éxito.
* Se muestra código de seguimiento.
* Se muestra estado pendiente.
* Se manejan errores de validación.
* Se validó lint/build.
* Se desplegó correctamente en Vercel.

## Estado de deploy

```text
GitHub: actualizado
Backend Render: actualizado
Frontend Vercel: actualizado
Base Supabase: actualizada
```

## Commits principales del bloque

```text
5d0ed85 feat(database): add publication request schema and validation
146bd57 feat: add public publication request backend flow
49580ef feat(frontend): conectar solicitudes de publicación
```

## Verificación en Supabase

Se verificó que existen las tablas:

```text
solicitud_publicacion
solicitud_publicacion_horario
```

También se verificó la estructura principal de ambas tablas, incluyendo:

```text
codigo_seguimiento
estado
origen
tipo_publicador
nombre_publicador
nombre_actividad
horarios asociados por solicitud_publicacion_id
created_at
updated_at
```

## Prueba de endpoint público en producción

Endpoint probado:

```text
POST https://donde-entreno-api.onrender.com/api/solicitudes-publicacion
```

Se envió un JSON vacío para validar errores.

Resultado:

```text
400 Bad Request
```

Respuesta esperada recibida:

```text
La solicitud contiene datos invalidos.
```

Se confirmaron errores de validación para campos obligatorios.

## Prueba real desde la web pública

URL probada:

```text
https://donde-entreno-web.vercel.app/publicar
```

Se envió una solicitud real de prueba desde producción.

Resultado:

```text
Solicitud enviada correctamente
Estado: Pendiente
Código de seguimiento: DEP-20260620-A3390F54
```

## Verificación de persistencia

La solicitud quedó guardada en Supabase con:

```text
ID: 2
Código de seguimiento: DEP-20260620-A3390F54
Estado: PENDIENTE
Origen: FORMULARIO_WEB
Tipo publicador: GIMNASIO
Nombre publicador: Test Produccion DondeEntreno
Nombre actividad: Actividad Test Produccion
```

## Verificación de horarios

Se confirmó que la solicitud tiene horario asociado:

```text
solicitud_publicacion_id: 2
dia_semana: LUNES
hora_inicio: 10:00:00
hora_fin: 11:00:00
```

## Validaciones frontend

Se probó enviar el formulario con campos obligatorios vacíos.

Resultado:

```text
El formulario marca el campo obligatorio.
No se genera código de seguimiento.
No se guarda una solicitud inválida en Supabase.
```

## Verificación de flujo público existente

Se verificó que el deploy nuevo no rompió las rutas públicas existentes:

```text
Home: OK
Explorar: OK
Detalle: OK
Publicar: OK
404: OK
```

## Decisión sobre solicitud de prueba

La solicitud de prueba quedó identificada con el código:

```text
DEP-20260620-A3390F54
```

Queda pendiente decidir si se conserva como registro de prueba o si se elimina manualmente más adelante desde Supabase.

No se ejecutó ningún script destructivo durante el cierre del bloque.

## Resultado final

El bloque de solicitudes reales de publicación queda cerrado correctamente.

Criterios cumplidos:

* Supabase tiene las tablas nuevas.
* Backend Render responde correctamente.
* Frontend Vercel muestra `/publicar` funcionando.
* Una solicitud enviada desde producción queda guardada como `PENDIENTE`.
* Los horarios quedan asociados.
* Los errores de validación se muestran correctamente.
* No se rompió el flujo público existente.
* No se ejecutaron scripts destructivos.
* No se ejecutó el script 06 en producción.
