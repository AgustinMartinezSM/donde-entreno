Testing y cierre - Login, registro y panel publicador V1 backend en producción

Este documento registra el cierre del bloque backend de login, registro y panel publicador V1 en producción para DondeEntreno.

Alcance del bloque

El objetivo fue desplegar y validar en producción el backend necesario para:

Registro de usuario común.
Registro de cuenta publicadora.
Login con JWT.
Endpoint GET /api/auth/me.
Endpoints protegidos de publicador bajo /api/publicador/**.
Asociación de solicitudes con usuario_id y perfil_publicador_id.
Creación de solicitudes desde un publicador autenticado.
Seguridad por roles entre SUPER_ADMIN, PUBLICADOR y rutas admin.

Frontend no fue modificado en este bloque.

Commits desplegados
20f4cbd feat(database): prepare publisher accounts
71255a3 feat(backend): add publicador registration and panel v1
Migración aplicada

Se aplicó en Supabase:

database/scripts/12_prepare_publisher_accounts.sql

La migración agregó/preparó:

usuario.telefono_normalizado
usuario.telefono_verificado

perfil_publicador.estado
perfil_publicador.ciudad_principal_id
perfil_publicador.whatsapp_normalizado
perfil_publicador.telefono_contacto_normalizado

solicitud_publicacion.usuario_id
solicitud_publicacion.perfil_publicador_id
Script no ejecutado en producción

No se ejecutó en producción:

database/scripts/13_test_publisher_accounts_queries.sql

El script 13 queda reservado para validaciones locales/controladas.

Reglas de seguridad aplicadas

Durante este bloque:

No se ejecutaron scripts destructivos.
No se ejecutó DELETE.
No se borraron usuarios.
No se borraron perfiles publicadores.
No se borraron solicitudes.
No se tocaron actividades ni horarios.
No se activó bootstrap.
No se cambiaron secrets.
No se modificaron variables sensibles.
No se tocó frontend.
Verificación de estructura en Supabase

Se confirmó que existen las columnas nuevas:

usuario.telefono_normalizado: OK
usuario.telefono_verificado: OK

perfil_publicador.estado: OK
perfil_publicador.ciudad_principal_id: OK
perfil_publicador.whatsapp_normalizado: OK
perfil_publicador.telefono_contacto_normalizado: OK

solicitud_publicacion.usuario_id: OK
solicitud_publicacion.perfil_publicador_id: OK

También se confirmaron constraints, foreign keys e índices nuevos.

Estado de perfiles publicadores

Se verificó en Supabase:

ACTIVO: 4
PENDIENTE_REVISION: 1
Compatibilidad de solicitudes históricas

Se confirmó que las solicitudes históricas se mantuvieron compatibles:

solicitudes_totales: 3
solicitudes_con_usuario: 0
solicitudes_con_perfil: 0

Esto es correcto porque las solicitudes anteriores fueron anónimas o públicas, sin usuario autenticado.

Deploy backend

Render desplegó correctamente el backend actualizado:

Commit: 71255a3
Estado: Deploy live
Regresión de endpoints públicos

Se validó que los endpoints públicos existentes siguen funcionando:

GET /api/actividades: OK
GET /api/actividades?ciudadSlug=mar-del-plata: OK
GET /api/filtros/opciones: OK
POST /api/solicitudes-publicacion con JSON vacío: 400 Bad Request controlado
Auth producción

Se validó login de SUPER_ADMIN:

POST /api/auth/login: OK
GET /api/auth/me: OK
Token Bearer: OK
Rol SUPER_ADMIN: OK

También se validó que el panel admin actual siga funcionando:

GET /api/admin/solicitudes-publicacion: OK
Admin ve solicitudes: OK
Seguridad publicador sin token

Se probó acceso sin token:

GET /api/publicador/me sin token: 401 OK
GET /api/publicador/solicitudes sin token: 401 OK
Registro de usuario/publicador con JSON vacío

Se validaron errores controlados:

POST /api/auth/registro/usuario con JSON vacío: 400 OK
POST /api/auth/registro/publicador con JSON vacío: 400 OK

Se confirmó luego en Supabase que no se insertaron datos inválidos.

Publicador QA creado en producción

Se creó un publicador de prueba controlado desde el endpoint real:

POST /api/auth/registro/publicador: 201 Created
Token Bearer recibido: OK
Rol: PUBLICADOR

Datos de prueba:

Email: qa.publicador.20260706@example.com
Nombre: QA
Apellido: Publicador
Perfil: QA Publicador Produccion
Tipo publicador: GIMNASIO
Ciudad principal: Mar del Plata

Se intentó registrar el mismo publicador nuevamente y el backend respondió:

409 Conflict: OK

Esto confirma que no permite duplicar el registro.

Publicador autenticado

Se validó:

GET /api/publicador/me: OK
GET /api/publicador/solicitudes: OK

El listado inicial del publicador recién creado devolvió:

contenido: vacío
paginaActual: 0
tamanioPagina: 20
totalElementos: 0
totalPaginas: 0
ultima: true

Esto confirmó que el publicador no ve solicitudes históricas ajenas.

Validación de solicitud publicador

Se probó:

POST /api/publicador/solicitudes con JSON vacío: 400 Bad Request controlado

Errores esperados:

nombreActividad obligatorio
descripcion obligatoria
nivel obligatorio
enfoque obligatorio
modalidad obligatoria
aceptaCondiciones obligatorio
horarios obligatorio
Solicitud creada desde publicador QA

Se creó una solicitud real de prueba desde el publicador autenticado:

Nombre actividad: QA Publicador Produccion - Solicitud V1
Estado: PENDIENTE
Código: DEP-20260706-180A2088
Solicitud ID: 4
Usuario ID: 6
Perfil publicador ID: 6

Se confirmó en Supabase:

usuario_id asociado: OK
perfil_publicador_id asociado: OK
estado PENDIENTE: OK
Horario asociado

Se confirmó el horario de la solicitud del publicador:

solicitud_publicacion_id: 4
dia_semana: MARTES
hora_inicio: 09:00:00
hora_fin: 10:00:00
observacion: Horario QA de prueba
Listado y detalle del publicador

Se validó:

GET /api/publicador/solicitudes: OK
Listado propio muestra 1 solicitud: OK
GET /api/publicador/solicitudes/{id}: OK
Detalle propio del publicador: OK
Horarios visibles: OK
Seguridad de roles

Se validó que un usuario PUBLICADOR no pueda acceder a rutas admin:

GET /api/admin/solicitudes-publicacion con token PUBLICADOR: 403 Forbidden OK

También se validó que el publicador no pueda acceder a solicitudes ajenas:

GET /api/publicador/solicitudes/1 con token PUBLICADOR: 404 Not Found OK

Esto confirma que no se filtran datos de solicitudes ajenas.

Admin y solicitud creada por publicador

Se validó que el admin siga funcionando y vea la solicitud creada por el publicador QA:

Login SUPER_ADMIN: OK
GET /api/admin/solicitudes-publicacion: OK
Solicitud QA aparece en listado admin: OK
GET detalle admin de solicitud QA: OK
Horarios visibles en admin: OK
Frontend actual

Aunque el frontend no fue modificado en este bloque, se verificó que sigue funcionando:

Home: OK
Explorar: OK
Detalle: OK
Publicar: OK
Admin login: OK
Admin solicitudes: OK
Logs Render

Se revisaron logs de Render después de las pruebas.

Resultado:

Sin errores 500 visibles.
Sin exceptions graves visibles.
Errores esperados 400/401/403/404/409 durante pruebas controladas.
Datos de prueba creados

Quedan creados en producción como evidencia de prueba:

Usuario:
qa.publicador.20260706@example.com

Perfil publicador:
QA Publicador Produccion

Solicitud:
DEP-20260706-180A2088
QA Publicador Produccion - Solicitud V1
Estado: PENDIENTE

Queda pendiente decidir en un bloque futuro si se conservan como datos QA o si se inactivan/limpian de forma controlada.

Resultado final

El bloque backend de login, registro y panel publicador V1 queda cerrado correctamente en producción.

Criterios cumplidos:

Migración 12 aplicada en Supabase.
Script 13 no ejecutado en producción.
Backend nuevo desplegado en Render.
Endpoints públicos siguen funcionando.
Login admin sigue funcionando.
Panel admin actual sigue funcionando.
GET /api/auth/me funciona.
Registro publicador funciona.
Registro duplicado se rechaza.
Rutas publicador están protegidas.
Publicador puede ver sus propios datos.
Publicador puede crear solicitud.
Solicitud queda asociada a usuario y perfil.
Horario queda asociado.
Publicador no puede ver solicitudes ajenas.
Publicador no puede acceder a admin.
Admin ve solicitud creada por publicador.
Logs Render OK.
No se tocaron secrets.
No se activó bootstrap.
No se ejecutaron scripts destructivos.