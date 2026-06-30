# Testing y cierre - Auth real en producción

Este documento registra el cierre del bloque de autenticación real en producción para DondeEntreno.

## Alcance del bloque

Se validó en producción el sistema de autenticación real implementado en el backend.

El bloque incluye:

- Spring Security.
- BCrypt.
- JWT Bearer.
- Endpoint `POST /api/auth/login`.
- Protección de rutas `/api/admin/**`.
- Roles `SUPER_ADMIN`, `ADMIN`, `PUBLICADOR` y `USUARIO`.
- Bootstrap seguro del primer `SUPER_ADMIN`.
- Manejo de errores `401`, `403` y `404`.
- Variables de entorno Auth cargadas en Render.
- Migración de seguridad Auth aplicada en Supabase.

## Estado de GitHub y deploy

```text
GitHub: actualizado
Branch: main
Último commit Auth backend: 50eb263 feat: add backend auth with JWT and super admin bootstrap
Render backend: actualizado
Deploy Render: live
Variables Auth en Render

Se verificó que existen las variables necesarias:

DONDEENTRENO_AUTH_JWT_SECRET
DONDEENTRENO_AUTH_JWT_ISSUER
DONDEENTRENO_AUTH_JWT_EXPIRATION_MINUTES
DONDEENTRENO_BOOTSTRAP_SUPER_ADMIN_ENABLED

El bootstrap quedó apagado:

DONDEENTRENO_BOOTSTRAP_SUPER_ADMIN_ENABLED=false

También se revisó que el bootstrap puede utilizar estas variables para crear el primer SUPER_ADMIN si hiciera falta:

DONDEENTRENO_BOOTSTRAP_SUPER_ADMIN_EMAIL
DONDEENTRENO_BOOTSTRAP_SUPER_ADMIN_PASSWORD
DONDEENTRENO_BOOTSTRAP_SUPER_ADMIN_NOMBRE
DONDEENTRENO_BOOTSTRAP_SUPER_ADMIN_APELLIDO

En este cierre no fue necesario activar bootstrap porque ya existía un SUPER_ADMIN activo.

Migración 07 aplicada en Supabase

Se aplicó correctamente:

database/scripts/07_prepare_auth_security.sql

Antes de aplicarla se verificó que no hubiera usuarios con:

nombre vacío
email vacío
password_hash vacío
emails duplicados al normalizar mayúsculas, minúsculas y espacios externos

Resultado del precheck:

usuarios_con_nombre_vacio: 0
usuarios_con_email_vacio: 0
usuarios_con_password_hash_vacio: 0
grupos_email_duplicado: 0
Script 08

No se ejecutó en producción:

database/scripts/08_test_auth_security_queries.sql

El script 08 queda reservado para pruebas controladas/locales.

Validaciones posteriores a la migración

Se confirmó en Supabase:

Roles:
ADMIN
PUBLICADOR
SUPER_ADMIN
USUARIO

Constraints:
chk_rol_nombre
chk_usuario_email_no_vacio
chk_usuario_nombre_no_vacio
chk_usuario_password_hash_no_vacio

Índice:
idx_usuario_email_normalizado_unico
SUPER_ADMIN

Se confirmó la existencia de un usuario SUPER_ADMIN activo:

Rol: SUPER_ADMIN
Activo: true
Email verificado: true
Deleted at: NULL

El bootstrap permaneció apagado.

Durante la validación se detectó que el password_hash inicial no tenía longitud BCrypt válida.
Se generó un nuevo hash BCrypt localmente y se actualizó únicamente el password_hash del SUPER_ADMIN existente.

Resultado esperado luego del ajuste:

prefijo_hash: $2a$
largo_hash: 60

No se guardaron contraseñas ni hashes reales en el repositorio.

Login en producción

Endpoint probado:

POST https://donde-entreno-api.onrender.com/api/auth/login

Resultado:

Login OK
Token type: Bearer
Access token recibido correctamente

No se documenta el token por seguridad.

Endpoint /api/auth/me

Se probó:

GET https://donde-entreno-api.onrender.com/api/auth/me

Resultado:

404 Not Found

Conclusión:

Endpoint no implementado todavía.
No bloquea el cierre del Auth real.
Protección de /api/admin/**

Se probó una ruta bajo /api/admin/** sin token:

GET /api/admin/test

Resultado:

401 Unauthorized

Luego se probó la misma ruta con token válido de SUPER_ADMIN.

Resultado:

404 Not Found

Conclusión:

Sin token, la ruta queda protegida correctamente.
Con token SUPER_ADMIN, la seguridad permite pasar.
La respuesta 404 ocurre porque todavía no existe un endpoint admin real implementado.

Se confirmó en código que actualmente no existen controllers reales bajo /api/admin/**; solo existe la regla de seguridad preparada para el futuro panel admin.

Prueba de 403

No se ejecutó una prueba real de 403 Forbidden en producción porque todavía no se creó un usuario no admin con login funcional para forzar un rol sin permisos.

Queda pendiente para un bloque futuro cuando existan usuarios USUARIO o PUBLICADOR reales.

Endpoints públicos

Se confirmó que Auth no rompió los endpoints públicos:

GET /api/actividades: OK
GET /api/filtros/opciones: OK
POST /api/solicitudes-publicacion con JSON vacío: 400 Bad Request controlado
Logs Render

Se revisaron logs de Render después de las pruebas.

Resultado:

Sin errores 500 visibles.
Sin exceptions graves visibles.
Consultas Hibernate normales.
Resultado final

El bloque de Auth real en producción queda cerrado correctamente.

Criterios cumplidos:

Migración 07 aplicada en Supabase.
Roles nuevos existentes en producción.
Constraints de usuario aplicadas.
Índice único normalizado para email creado.
Render actualizado.
Variables Auth presentes.
SUPER_ADMIN existente y funcional.
Bootstrap apagado.
Login en producción probado.
Token JWT Bearer recibido correctamente.
/api/admin/** protegido correctamente.
Endpoints públicos siguen funcionando.
Logs de Render revisados.
No se ejecutó script 08 en producción.
No se ejecutaron scripts destructivos.
No se subieron contraseñas ni hashes reales al repo.