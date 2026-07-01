# Testing y cierre - Panel Admin inicial protegido en producción

Este documento registra el cierre del bloque de deploy y validación del Panel Admin inicial protegido de DondeEntreno.

## Alcance del bloque

El objetivo fue desplegar en producción el primer panel administrativo protegido, permitiendo revisar solicitudes reales de publicación sin aprobar ni crear actividades reales todavía.

## Commits desplegados

```text
a0858c1 feat: add protected admin publication request endpoints
a9fb018 feat(frontend): agregar panel admin inicial local
Estado de deploy
GitHub: actualizado
Render backend: deploy live
Vercel frontend: deploy ready
Branch: main
Commit producción: a9fb018
Backend Admin

Se validaron en producción los endpoints protegidos bajo /api/admin/**.

Endpoints probados
GET /api/admin/solicitudes-publicacion
GET /api/admin/solicitudes-publicacion/{id}
PATCH /api/admin/solicitudes-publicacion/{id}/estado
Resultados
GET listado admin: OK
GET detalle admin: OK
PATCH EN_REVISION: OK
PATCH RECHAZADA con motivo: OK
APROBADA rechazada con 400 Bad Request: OK
Auth y seguridad

Se validó el login real con el usuario SUPER_ADMIN de producción.

POST /api/auth/login: OK
Token Bearer recibido: OK
SUPER_ADMIN autorizado: OK

También se confirmó:

Sin token no se accede a rutas admin: OK
Con contraseña incorrecta no se accede: OK
Con sesión cerrada /admin/solicitudes redirige a /admin/login: OK
Frontend Admin

Se validó en Vercel:

/admin/login: OK
/admin/solicitudes: OK
Listado real de solicitudes: OK
Filtro por estado: OK
Detalle real de solicitud: OK
Horarios visibles: OK
Marcar EN_REVISION desde UI: OK
Rechazar con motivo desde UI: OK
Cerrar sesión: OK
Guard básico de rutas admin: OK
Estados probados

Se usaron solicitudes reales de prueba ya existentes en producción.

PENDIENTE: OK
EN_REVISION: OK
RECHAZADA: OK
APROBADA: no disponible / no implementada

Se confirmó que APROBADA no aparece en el panel y que el backend la rechaza con error controlado.

Solicitudes usadas para prueba
DEP-20260620-A3390F54
Estado final probado: EN_REVISION

DEP-20260620-E761719E
Estado final probado: RECHAZADA

DEP-20260620-8EBA9DC1
Estado final verificado: PENDIENTE
Actividades reales

No se crearon actividades reales durante este bloque.

actividadGeneradaId: vacío/null
APROBADA: no implementada
Regresión pública

Se confirmó que el deploy del panel admin no rompió el sitio público.

Home: OK
Explorar: OK
Detalle: OK
Publicar: OK
POST solicitudes públicas: sin cambios esperados
Logs

Se revisaron logs de Render después de las pruebas.

Sin errores 500 visibles.
Sin exceptions graves visibles.
Consultas y respuestas esperadas.
Observaciones

Se detectó una observación visual menor:

La pantalla de login todavía muestra el texto "Admin local".

No bloquea el cierre del bloque, pero queda como mejora futura para cambiarlo por:

Panel admin

o:

Acceso administradores

También se observó en PowerShell posible visualización incorrecta de caracteres con tilde, por ejemplo GÃ¼emes. En la web pública/admin se visualizó correctamente.

Resultado final

El bloque de Panel Admin inicial protegido queda cerrado correctamente en producción.

Criterios cumplidos:

Backend admin desplegado en Render.
Frontend admin desplegado en Vercel.
Login admin producción validado.
Rutas admin protegidas.
Listado de solicitudes reales funcionando.
Detalle de solicitud funcionando.
Horarios visibles.
Cambio a EN_REVISION funcionando.
Rechazo con motivo funcionando.
APROBADA no disponible.
No se crearon actividades reales.
Web pública sigue funcionando.
Logs revisados sin errores graves.