Testing y cierre - Aprobación de solicitudes como actividades reales en producción

Este documento registra el cierre del bloque de aprobación de solicitudes de publicación como actividades reales en DondeEntreno.

Alcance del bloque

El objetivo fue desplegar y validar en producción el flujo que permite aprobar una solicitud de publicación desde el panel admin y convertirla en una actividad real visible públicamente.

Este bloque incluye:

Endpoint de aprobación en backend.
Acción de aprobación desde el panel admin.
Creación de actividad real.
Creación de horarios reales.
Vinculación mediante actividad_generada_id.
Cambio de estado de la solicitud a APROBADA.
Link desde el panel admin a la actividad pública.
Validaciones para evitar aprobaciones duplicadas.
Validaciones para impedir aprobar solicitudes rechazadas.
Commits desplegados
d0ea8f5 feat(backend): aprobar solicitudes de publicacion como actividades
10035ae feat(frontend): aprobar solicitudes desde panel admin
Estado de deploy
GitHub: actualizado
Render backend: deploy live
Vercel frontend: deploy ready
Branch: main
Commit producción: 10035ae
Scripts SQL

No se ejecutaron scripts SQL destructivos en producción.

No se ejecutó en producción:

database/scripts/09_test_approval_traceability_queries.sql

El script 09 queda reservado como validación local/documentación, no como script productivo.

No hizo falta aplicar una migración estructural nueva para este bloque.

Regresión de endpoints públicos

Se confirmó que los endpoints públicos siguen funcionando después del deploy:

GET /api/actividades: OK
GET /api/filtros/opciones: OK
POST /api/solicitudes-publicacion con JSON vacío: 400 Bad Request controlado
Login y panel admin

Se confirmó en producción:

Login admin: OK
/admin/solicitudes: OK
Listado real: OK
Detalle real: OK
Acción "Aprobar y publicar actividad": OK
Solicitud aprobada

Se aprobó una solicitud de prueba claramente identificable:

Solicitud ID: 2
Código: DEP-20260620-A3390F54
Nombre actividad: Actividad Test Produccion
Estado anterior: EN_REVISION
Estado final: APROBADA

Resultado desde el panel admin:

Solicitud aprobada correctamente.
Actividad creada: Actividad Test Produccion
ID actividad: 8
Link público generado: OK
Actividad real creada

Se confirmó en producción:

Actividad ID: 8
Título: Actividad Test Produccion
Slug: actividad-test-produccion
Estado publicación: PUBLICADA
Visible públicamente: OK

URL pública:

https://donde-entreno-web.vercel.app/actividades/actividad-test-produccion
Trazabilidad en base de datos

Se verificó en Supabase que la solicitud quedó vinculada a la actividad real:

solicitud_id: 2
estado: APROBADA
actividad_generada_id: 8
actividad_id: 8
actividad_titulo: Actividad Test Produccion
actividad_slug: actividad-test-produccion
estado_publicacion: PUBLICADA
Horarios reales

Se verificó que se creó el horario real asociado a la actividad:

horario_actividad.id: 14
actividad_id: 8
dia_semana: LUNES
hora_inicio: 10:00:00
hora_fin: 11:00:00
Validación de aprobación duplicada

Se intentó aprobar nuevamente la solicitud ya aprobada.

Resultado:

Status: 400 Bad Request
Mensaje: La solicitud ya tiene una actividad generada.

Conclusión:

No se puede aprobar dos veces: OK
No se duplicó actividad: OK
No se duplicaron horarios: OK
Validación de solicitud rechazada

Se intentó aprobar una solicitud rechazada:

Solicitud ID: 3
Estado: RECHAZADA

Resultado:

Status: 400 Bad Request
Mensaje: No se puede aprobar una solicitud rechazada.

Conclusión:

No se puede aprobar una solicitud RECHAZADA: OK
No se creó actividad real: OK
Seguridad admin

Se confirmó:

Admin sin sesión redirige a login: OK
Backend admin sin token devuelve 401: OK
Regresión pública visual

Se confirmó que la actividad aprobada aparece públicamente y que el sitio sigue funcionando:

Home: OK
Explorar muestra actividad aprobada: OK
Detalle actividad aprobada: OK
Horarios visibles: OK
Logs

Se revisaron logs de Render después de las pruebas.

Resultado:

Sin errores 500 visibles.
Sin exceptions graves visibles.
Errores 400 esperados para pruebas negativas.
401 esperado para prueba sin token.
Decisión sobre actividad de prueba

La actividad de prueba creada en producción queda identificada como:

Actividad Test Produccion
ID: 8
Slug: actividad-test-produccion

Queda pendiente decidir si se conserva como evidencia de prueba o si se limpia manualmente más adelante de forma controlada.

Resultado final

El bloque de aprobación de solicitudes como actividades reales queda cerrado correctamente en producción.

Criterios cumplidos:

GitHub actualizado.
Render desplegó backend nuevo.
Vercel desplegó frontend nuevo.
Login admin producción sigue funcionando.
Admin puede aprobar una solicitud en producción.
La solicitud queda APROBADA.
Se crea actividad real.
Se crean horarios reales.
actividad_generada_id queda vinculado.
La actividad aparece públicamente en Explorar y Detalle.
No se puede aprobar dos veces.
No se puede aprobar una solicitud RECHAZADA.
Endpoints públicos siguen funcionando.
No se ejecutaron scripts SQL destructivos.
No se ejecutó el script 09 en producción.
No se tocaron variables sensibles.
No se activó bootstrap.
Logs Render OK.