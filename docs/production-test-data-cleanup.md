# Limpieza y control de datos de prueba en producción

Este documento registra el bloque corto de revisión y limpieza controlada de datos de prueba en producción para DondeEntreno.

## Contexto

Luego de cerrar el circuito completo:

```text
Usuario publica solicitud
→ admin revisa
→ admin aprueba
→ se crea actividad real
→ aparece públicamente

se revisaron los datos de prueba generados durante las validaciones de producción.

Reglas aplicadas

Durante este bloque:

No se ejecutaron scripts SQL destructivos.
No se ejecutó DELETE.
No se borraron solicitudes.
No se borraron actividades.
No se borraron horarios.
No se modificaron variables sensibles.
No se activó bootstrap.
Se usaron primero consultas SELECT de verificación.
La limpieza se resolvió mediante inactivación/despublicación controlada.
Solicitudes de prueba identificadas

Se identificaron tres solicitudes de prueba en producción:

ID 1
Código: DEP-20260620-8EBA9DC1
Estado: PENDIENTE
Actividad: PRUEBA FRONTEND - Boxeo QA
Publicador: PRUEBA FRONTEND - Club QA
actividad_generada_id: NULL

ID 2
Código: DEP-20260620-A3390F54
Estado: APROBADA
Actividad: Actividad Test Produccion
Publicador: Test Produccion DondeEntreno
actividad_generada_id: 8

ID 3
Código: DEP-20260620-E761719E
Estado: RECHAZADA
Actividad: b
Publicador: a
actividad_generada_id: NULL
Actividad real de prueba identificada

Se identificó una actividad real creada desde la solicitud aprobada:

ID: 8
Título: Actividad Test Produccion
Slug: actividad-test-produccion
Estado publicación anterior: PUBLICADA
Activa anterior: true
deleted_at: NULL
Solicitud vinculada: ID 2
Decisión tomada

Se decidió no borrar la actividad ni la solicitud.

La opción elegida fue:

Inactivar/despublicar la actividad de prueba.

Motivos:

Conserva la trazabilidad.
Evita borrar datos por error.
Es reversible.
Evita que una actividad de prueba quede visible públicamente.
No afecta solicitudes ni horarios asociados.
Cambio aplicado

Se aplicó un UPDATE controlado únicamente sobre la actividad de prueba:

Actividad ID: 8
Slug: actividad-test-produccion
Título: Actividad Test Produccion

Resultado final:

estado_publicacion: PENDIENTE_REVISION
activa: false
deleted_at: NULL
Verificación pública posterior

Se confirmó que la actividad de prueba ya no aparece públicamente:

Home: OK, no aparece actividad test
Explorar: OK, no aparece actividad test
Detalle directo: no disponible/controlado OK
Estado final

La limpieza de datos de prueba queda cerrada correctamente.

Resultado:

Solicitudes de prueba conservadas.
Actividad de prueba conservada pero no visible públicamente.
Horarios conservados.
Trazabilidad conservada.
Web pública funcionando.
Sin deletes.
Sin scripts destructivos.