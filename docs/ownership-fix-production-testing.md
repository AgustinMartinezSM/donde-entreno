Testing y cierre - Fix ownership de aprobación en producción

Este documento registra el cierre del bloque de corrección de ownership al aprobar solicitudes autenticadas de publicadores en DondeEntreno.

Alcance del bloque

El objetivo fue desplegar y validar en producción el fix que corrige la propiedad de la actividad creada al aprobar una solicitud enviada por un publicador autenticado.

Antes del fix, una actividad creada desde una solicitud autenticada podía quedar asociada al perfil del admin revisor en lugar del perfil publicador real.

El comportamiento esperado luego del fix es:

owner de la actividad = publicador real de la solicitud
revisor de la solicitud = admin que aprobó
Commit desplegado
e336051 fix(backend): use solicitud publicador as actividad owner
Deploy
GitHub: actualizado
Render: Deploy live
Commit producción: e336051
Reglas respetadas

Durante este bloque:

No se tocó frontend.
No se ejecutaron migraciones.
No se ejecutó SQL destructivo.
No se borraron datos.
No se cambiaron variables de Render.
No se activó bootstrap.
No se subieron secretos.
No se corrigieron datos existentes manualmente sin verificar primero.
Solo se usaron consultas de verificación y una inactivación controlada de actividad QA.
Regresión pública

Se validó que los endpoints públicos siguieran funcionando:

GET /api/actividades: OK
GET /api/actividades?ciudadSlug=mar-del-plata: OK
GET /api/filtros/opciones: OK
POST /api/solicitudes-publicacion con JSON vacío: 400 Bad Request controlado
Login y roles

Se validó:

Login admin: OK
Login publicador QA: OK

Usuarios usados para la prueba:

Admin:
admin@dondeentreno.com

Publicador QA:
qa.publicador.20260706@example.com
Solicitud QA usada para validar ownership

Se utilizó una solicitud autenticada existente del publicador QA:

Solicitud ID: 4
Código: DEP-20260706-180A2088
Estado previo: PENDIENTE
Nombre actividad: QA Publicador Produccion - Solicitud V1
Usuario publicador ID: 6
Perfil publicador ID: 6

Estado previo confirmado en Supabase:

actividad_generada_id: NULL
revisado_por_usuario_id: NULL
perfil_publicador_id: 6
Aprobación en producción

Se aprobó la solicitud autenticada desde el admin:

POST /api/admin/solicitudes-publicacion/4/aprobar: OK
actividadGeneradaId: 9
Validación principal del fix

Se confirmó en Supabase:

solicitud_publicacion.id: 4
solicitud_publicacion.estado: APROBADA
solicitud_publicacion.perfil_publicador_id: 6
solicitud_publicacion.actividad_generada_id: 9

actividad.id: 9
actividad.perfil_publicador_id: 6

Regla validada:

actividad.perfil_publicador_id = solicitud_publicacion.perfil_publicador_id

Resultado:

Owner de la actividad = publicador real: OK

También se confirmó que el admin quedó como revisor:

solicitud_publicacion.revisado_por_usuario_id = usuario admin

Resultado:

Admin queda solo como revisor: OK
Admin distinto del dueño del perfil: OK
Actividad creada

Actividad generada por la aprobación:

Actividad ID: 9
Título: QA Publicador Produccion - Solicitud V1
Slug: qa-publicador-produccion-solicitud-v1
Estado inicial: PUBLICADA
Activa: true
Perfil publicador ID: 6

Se validó públicamente:

Detalle público carga: OK
Actividad visible: OK
Horarios visibles: OK
Ubicación visible: OK
Solicitudes históricas/anónimas

Se validó que el listado admin y el detalle de solicitudes históricas/anónimas sigan funcionando:

Listado admin: OK
Detalle solicitud histórica/anónima: OK
Fallback histórico/anónimo no se rompió: OK
Limpieza controlada de actividad QA

Como la prueba generó una actividad visible públicamente, se decidió conservar trazabilidad pero ocultar la actividad QA.

Se aplicó una inactivación controlada sobre la actividad ID 9:

estado_publicacion: PENDIENTE_REVISION
activa: false
deleted_at: NULL

No se borró:

actividad: conservada
horarios: conservados
solicitud: conservada
actividad_generada_id: conservado
ownership: conservado
revisor: conservado

Validación pública posterior:

Home OK, no aparece actividad QA
Explorar OK, no aparece actividad QA
Detalle directo no disponible/controlado OK
Verificación final de trazabilidad

Se confirmó en Supabase:

actividad.id: 9
actividad.estado_publicacion: PENDIENTE_REVISION
actividad.activa: false
actividad.deleted_at: NULL
actividad.perfil_publicador_id: 6

solicitud_publicacion.id: 4
solicitud_publicacion.estado: APROBADA
solicitud_publicacion.actividad_generada_id: 9
solicitud_publicacion.perfil_publicador_id: 6
solicitud_publicacion.revisado_por_usuario_id: admin

Resultado:

Supabase ownership final: OK
Trazabilidad intacta: OK
Logs Render

Se revisaron logs de Render después del deploy y pruebas.

Resultado:

Logs Render OK
Sin errores 500
Sin exceptions graves
Sin errores de SQL
Sin errores de columnas o relaciones inexistentes
Resultado final

El bloque de fix ownership queda cerrado correctamente en producción.

Criterios cumplidos:

GitHub actualizado.
Render desplegó correctamente.
Logs Render OK.
Endpoints públicos siguen funcionando.
Login admin sigue funcionando.
Login publicador sigue funcionando.
Aprobación de solicitud autenticada funciona.
Actividad creada queda asociada al publicador real.
Admin queda como revisor.
Actividad aparece públicamente.
Solicitudes históricas/anónimas siguen funcionando.
Actividad QA inactivada de forma controlada.
Trazabilidad conservada.
Producción estable.
Sin migraciones.
Sin SQL destructivo.
Sin cambios de variables sensibles.