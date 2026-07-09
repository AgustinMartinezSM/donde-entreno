Testing y cierre - Publicador V2: Mis actividades aprobadas en producción

Este documento registra el cierre del bloque backend Publicador V2 - Mis actividades aprobadas en producción para DondeEntreno.

Alcance del bloque

El objetivo fue desplegar y validar en producción los endpoints que permiten a un publicador autenticado consultar sus propias actividades aprobadas.

La regla funcional esperada es:

El publicador solo puede ver actividades donde:
actividad.perfil_publicador_id = perfil_publicador.id del publicador autenticado
Commit desplegado
3f3a8e5 feat: agregar mis actividades aprobadas para publicador

Se confirmó previamente que el fix de ownership ya estaba en origin/main:

e336051 fix(backend): use solicitud publicador as actividad owner

Por lo tanto, el único commit pendiente real antes del push era el feature nuevo de actividades propias del publicador.

Estado de deploy
GitHub: actualizado
Render: Deploy live
Commit producción: 3f3a8e5
Frontend: sin cambios
Supabase: sin migraciones nuevas
Reglas respetadas

Durante este bloque:

No se tocó frontend.
No se ejecutaron migraciones.
No se ejecutó SQL destructivo.
No se borraron datos.
No se cambiaron secrets.
No se activó bootstrap.
No se modificaron variables de Render.
No se corrigieron datos productivos sin verificación previa.
Solo se usaron consultas de verificación y una inactivación controlada de actividad QA.
Regresión pública

Se validó que los endpoints públicos sigan funcionando:

GET /api/actividades: OK
GET /api/actividades?ciudadSlug=mar-del-plata: OK
GET /api/filtros/opciones: OK
Login publicador

Se validó login del publicador QA:

Usuario: qa.publicador.20260706@example.com
Rol: PUBLICADOR
Login: OK
GET /api/publicador/me: OK
Endpoint de actividades propias

Se probó:

GET /api/publicador/actividades

Resultado inicial:

0 elementos

Este resultado fue correcto porque la actividad QA anterior del publicador había quedado inactivada/despublicada tras pruebas previas.

Solicitud QA V2 creada

Para validar el caso positivo real, se creó una nueva solicitud QA desde el publicador autenticado:

Solicitud ID: 5
Código: DEP-20260709-983254A5
Nombre: QA Publicador Produccion - Actividad Propia V2
Estado inicial: PENDIENTE
Publicador: QA Publicador Produccion
Perfil publicador ID: 6

Antes de aprobar, se verificó en Supabase que la solicitud pertenecía al publicador QA correcto.

Aprobación de solicitud QA V2

Se aprobó la solicitud como admin:

POST /api/admin/solicitudes-publicacion/5/aprobar: OK
Estado final solicitud: APROBADA
Actividad generada ID: 10
Validación de ownership

Se confirmó en Supabase:

solicitud_publicacion.id: 5
solicitud_publicacion.perfil_publicador_id: 6
solicitud_publicacion.actividad_generada_id: 10

actividad.id: 10
actividad.perfil_publicador_id: 6

Regla validada:

actividad.perfil_publicador_id = solicitud_publicacion.perfil_publicador_id

Resultado:

Owner de la actividad = publicador real: OK
Admin queda como revisor: OK
Actividades propias del publicador

Luego de aprobar la solicitud, se validó:

GET /api/publicador/actividades: OK
Devuelve actividad propia del publicador: OK
Actividad ID 10 aparece: OK

También se validó el detalle:

GET /api/publicador/actividades/10: OK
Publicador puede ver detalle de actividad propia: OK
Seguridad: actividad ajena

Se probó que el publicador QA no pueda ver una actividad ajena:

GET /api/publicador/actividades/1 con token PUBLICADOR

Resultado:

Publicador no ve actividad ajena: OK
No se filtró actividad de otro perfil: OK
Seguridad: sin token

Se validó:

GET /api/publicador/actividades sin token: 401 OK
GET /api/publicador/actividades/10 sin token: 401 OK
Seguridad: usuario común

Se creó un usuario común QA controlado para validar permisos:

Usuario: qa.usuario.20260709@example.com
Rol: USUARIO
Registro usuario común QA: OK
Token USUARIO: OK

Se confirmó:

USUARIO no accede a /api/publicador/actividades: OK
USUARIO no accede a /api/admin/**: OK
Admin

Se confirmó que el admin sigue funcionando después del deploy:

GET /api/admin/solicitudes-publicacion: OK
GET /api/admin/solicitudes-publicacion/{id}: OK
Panel admin backend sigue funcionando: OK
Inactivación controlada de actividad QA

Como la prueba generó una actividad visible públicamente, se decidió no borrar nada y despublicar/inactivar la actividad QA.

Actividad QA creada:

Actividad ID: 10
Nombre: QA Publicador Produccion - Actividad Propia V2
Perfil publicador ID: 6

Cambio aplicado:

estado_publicacion: PENDIENTE_REVISION
activa: false
deleted_at: NULL

Se conservó:

actividad: conservada
horarios: conservados
solicitud: conservada
actividad_generada_id: conservado
ownership: conservado
revisor: conservado
Verificación pública posterior

Se confirmó:

Home: OK, no aparece actividad QA 10
Explorar: OK, no aparece actividad QA 10
Detalle directo: no disponible/controlado OK
Endpoint publicador después de inactivar

Luego de inactivar la actividad QA 10, se validó:

GET /api/publicador/actividades: OK
Listado vuelve a vacío: OK

Esto confirma que el endpoint lista actividades propias aprobadas/visibles según el estado activo/publicado esperado.

Logs Render

Se revisaron logs de Render después del deploy y pruebas.

Resultado:

Logs Render OK
Sin errores 500
Sin exceptions graves
Sin errores de SQL
Sin errores de columnas o relaciones inexistentes

Los errores 401/403/404 esperados durante pruebas de seguridad fueron controlados.

Datos QA creados

Quedan identificados como datos de prueba:

Publicador QA:
qa.publicador.20260706@example.com

Solicitud QA V2:
DEP-20260709-983254A5
QA Publicador Produccion - Actividad Propia V2
Estado: APROBADA

Actividad QA V2:
ID 10
Estado publicación: PENDIENTE_REVISION
Activa: false

Usuario común QA:
qa.usuario.20260709@example.com

Queda pendiente decidir en un bloque futuro si estos datos QA se conservan, se inactivan más profundamente o se limpian de forma controlada.

Resultado final

El bloque Backend Publicador V2 - Mis actividades aprobadas queda cerrado correctamente en producción.

Criterios cumplidos:

GitHub actualizado correctamente.
Render desplegó backend nuevo.
Logs Render OK.
Endpoints públicos siguen funcionando.
Login publicador OK.
/api/publicador/me OK.
GET /api/publicador/actividades OK.
GET /api/publicador/actividades/{id} OK.
Actividad propia visible para el publicador cuando está publicada/activa.
Publicador no ve actividades ajenas.
Sin token devuelve 401.
Usuario común no accede.
Admin sigue funcionando.
Ownership validado.
Actividad QA inactivada de forma controlada.
Producción estable.
Frontend sin tocar.
Sin migraciones.
Sin SQL destructivo.