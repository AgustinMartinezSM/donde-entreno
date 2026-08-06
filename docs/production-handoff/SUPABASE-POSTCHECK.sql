-- DondeEntreno: control posterior de Supabase.
-- Solo lectura. Ejecutar después de las migraciones 14, 15, 16 y 17.

-- 1. Contexto efectivo de la sesion.
SELECT
    current_database() AS base_actual,
    current_user AS usuario_actual,
    current_schema() AS schema_actual,
    current_setting('search_path') AS search_path,
    current_schemas(true) AS schemas_efectivos,
    version() AS version_postgresql;

-- 2. Tablas y secuencias esperadas.
SELECT
    to_regclass('solicitud_cambio_actividad') AS tabla_solicitudes,
    to_regclass('seguimiento_publicador') AS tabla_seguimientos,
    to_regclass('solicitud_cambio_actividad_id_seq') AS secuencia_solicitudes,
    to_regclass('seguimiento_publicador_id_seq') AS secuencia_seguimientos;

WITH tablas_requeridas(nombre) AS (
    VALUES
        ('usuario'),
        ('perfil_publicador'),
        ('actividad'),
        ('imagen'),
        ('solicitud_cambio_actividad'),
        ('seguimiento_publicador')
)
SELECT
    r.nombre AS tabla,
    n.nspname AS schema_tabla,
    c.relkind AS tipo_relacion,
    to_regclass(r.nombre) AS resolucion_sin_schema
FROM tablas_requeridas r
LEFT JOIN pg_class c
    ON c.relname = r.nombre
   AND c.relkind IN ('r', 'p')
LEFT JOIN pg_namespace n
    ON n.oid = c.relnamespace
   AND n.nspname NOT IN ('pg_catalog', 'information_schema')
ORDER BY r.nombre, n.nspname;

-- 3. Columnas, tipos, nulabilidad y defaults.
WITH columnas_requeridas(tabla, columna) AS (
    VALUES
        ('imagen', 'estado_moderacion'),
        ('imagen', 'motivo_rechazo'),
        ('solicitud_cambio_actividad', 'id'),
        ('solicitud_cambio_actividad', 'actividad_id'),
        ('solicitud_cambio_actividad', 'perfil_publicador_id'),
        ('solicitud_cambio_actividad', 'usuario_id'),
        ('solicitud_cambio_actividad', 'estado'),
        ('solicitud_cambio_actividad', 'titulo'),
        ('solicitud_cambio_actividad', 'descripcion'),
        ('solicitud_cambio_actividad', 'precio_referencia'),
        ('solicitud_cambio_actividad', 'mostrar_precio'),
        ('solicitud_cambio_actividad', 'whatsapp_contacto'),
        ('solicitud_cambio_actividad', 'instagram_contacto'),
        ('solicitud_cambio_actividad', 'email_contacto'),
        ('solicitud_cambio_actividad', 'nivel'),
        ('solicitud_cambio_actividad', 'modalidad'),
        ('solicitud_cambio_actividad', 'motivo_rechazo'),
        ('solicitud_cambio_actividad', 'resuelto_por_usuario_id'),
        ('solicitud_cambio_actividad', 'resuelto_at'),
        ('solicitud_cambio_actividad', 'created_at'),
        ('solicitud_cambio_actividad', 'updated_at'),
        ('solicitud_cambio_actividad', 'deleted_at'),
        ('seguimiento_publicador', 'id'),
        ('seguimiento_publicador', 'usuario_id'),
        ('seguimiento_publicador', 'perfil_publicador_id'),
        ('seguimiento_publicador', 'created_at')
)
SELECT
    r.tabla,
    r.columna,
    c.table_schema,
    c.column_name IS NOT NULL AS presente,
    c.data_type,
    c.udt_name,
    c.is_nullable,
    c.column_default
FROM columnas_requeridas r
LEFT JOIN information_schema.columns c
    ON c.table_name = r.tabla
   AND c.column_name = r.columna
   AND c.table_schema NOT IN ('pg_catalog', 'information_schema')
ORDER BY r.tabla, r.columna, c.table_schema;

-- 4. Constraints y validacion efectiva.
WITH constraints_requeridos(tabla, constraint_name) AS (
    VALUES
        ('solicitud_cambio_actividad', 'solicitud_cambio_actividad_pkey'),
        ('solicitud_cambio_actividad', 'fk_solicitud_cambio_actividad_actividad'),
        ('solicitud_cambio_actividad', 'fk_solicitud_cambio_actividad_perfil'),
        ('solicitud_cambio_actividad', 'fk_solicitud_cambio_actividad_usuario'),
        ('solicitud_cambio_actividad', 'fk_solicitud_cambio_actividad_resuelto_por'),
        ('solicitud_cambio_actividad', 'chk_solicitud_cambio_actividad_estado'),
        ('solicitud_cambio_actividad', 'chk_solicitud_cambio_actividad_motivo_rechazo'),
        ('solicitud_cambio_actividad', 'chk_solicitud_cambio_actividad_algun_campo'),
        ('solicitud_cambio_actividad', 'chk_solicitud_cambio_actividad_nivel'),
        ('solicitud_cambio_actividad', 'chk_solicitud_cambio_actividad_modalidad'),
        ('solicitud_cambio_actividad', 'chk_solicitud_cambio_actividad_precio'),
        ('solicitud_cambio_actividad', 'chk_solicitud_cambio_actividad_resolucion'),
        ('imagen', 'chk_imagen_estado_moderacion'),
        ('seguimiento_publicador', 'seguimiento_publicador_pkey'),
        ('seguimiento_publicador', 'seguimiento_publicador_usuario_id_fkey'),
        ('seguimiento_publicador', 'seguimiento_publicador_perfil_publicador_id_fkey'),
        ('seguimiento_publicador', 'uq_seguimiento_usuario_perfil')
)
SELECT
    r.tabla AS tabla_esperada,
    r.constraint_name,
    n.nspname AS schema_encontrado,
    rel.relname AS tabla_encontrada,
    con.contype,
    con.convalidated,
    pg_get_constraintdef(con.oid, true) AS definicion
FROM constraints_requeridos r
LEFT JOIN pg_constraint con ON con.conname = r.constraint_name
LEFT JOIN pg_class rel ON rel.oid = con.conrelid
LEFT JOIN pg_namespace n ON n.oid = rel.relnamespace
ORDER BY r.tabla, r.constraint_name, n.nspname;

-- 5. Indices esperados y definiciones reales.
WITH indices_requeridos(tabla, indice) AS (
    VALUES
        ('solicitud_cambio_actividad', 'uq_solicitud_cambio_actividad_abierta'),
        ('solicitud_cambio_actividad', 'idx_solicitud_cambio_actividad_perfil'),
        ('solicitud_cambio_actividad', 'idx_solicitud_cambio_actividad_estado'),
        ('solicitud_cambio_actividad', 'idx_solicitud_cambio_actividad_actividad'),
        ('imagen', 'idx_imagen_estado_moderacion'),
        ('seguimiento_publicador', 'idx_seguimiento_usuario'),
        ('seguimiento_publicador', 'idx_seguimiento_perfil')
)
SELECT
    r.tabla AS tabla_esperada,
    r.indice,
    i.schemaname AS schema_encontrado,
    i.tablename AS tabla_encontrada,
    i.indexdef AS definicion
FROM indices_requeridos r
LEFT JOIN pg_indexes i ON i.indexname = r.indice
ORDER BY r.tabla, r.indice, i.schemaname;

-- 6. Integridad de imagenes y efecto del default.
SELECT
    estado_moderacion,
    count(*) AS cantidad
FROM imagen
GROUP BY estado_moderacion
ORDER BY estado_moderacion;

SELECT
    count(*) AS imagenes_totales,
    count(*) FILTER (
        WHERE estado_moderacion IS NULL
    ) AS estados_nulos,
    count(*) FILTER (
        WHERE estado_moderacion NOT IN ('PENDIENTE', 'APROBADA', 'RECHAZADA')
    ) AS estados_fuera_de_dominio,
    count(*) FILTER (
        WHERE estado_moderacion = 'APROBADA'
    ) AS imagenes_aprobadas
FROM imagen;

-- 7. Integridad de solicitudes de cambio.
SELECT
    count(*) AS solicitudes_totales,
    count(*) FILTER (
        WHERE estado NOT IN ('PENDIENTE', 'EN_REVISION', 'APROBADA', 'RECHAZADA')
    ) AS estados_fuera_de_dominio,
    count(*) FILTER (
        WHERE titulo IS NULL
          AND descripcion IS NULL
          AND precio_referencia IS NULL
          AND mostrar_precio IS NULL
          AND whatsapp_contacto IS NULL
          AND instagram_contacto IS NULL
          AND email_contacto IS NULL
          AND nivel IS NULL
          AND modalidad IS NULL
    ) AS solicitudes_sin_cambios,
    count(*) FILTER (
        WHERE estado = 'RECHAZADA'
          AND (motivo_rechazo IS NULL OR btrim(motivo_rechazo) = '')
    ) AS rechazos_sin_motivo,
    count(*) FILTER (
        WHERE estado <> 'RECHAZADA'
          AND motivo_rechazo IS NOT NULL
    ) AS motivos_fuera_de_rechazo,
    count(*) FILTER (
        WHERE estado IN ('APROBADA', 'RECHAZADA')
          AND (resuelto_por_usuario_id IS NULL OR resuelto_at IS NULL)
    ) AS resueltas_sin_auditoria,
    count(*) FILTER (
        WHERE estado IN ('PENDIENTE', 'EN_REVISION')
          AND (resuelto_por_usuario_id IS NOT NULL OR resuelto_at IS NOT NULL)
    ) AS abiertas_con_resolucion
FROM solicitud_cambio_actividad;

SELECT
    count(*) AS referencias_huerfanas
FROM solicitud_cambio_actividad s
LEFT JOIN actividad a ON a.id = s.actividad_id
LEFT JOIN perfil_publicador p ON p.id = s.perfil_publicador_id
LEFT JOIN usuario u ON u.id = s.usuario_id
LEFT JOIN usuario r ON r.id = s.resuelto_por_usuario_id
WHERE a.id IS NULL
   OR p.id IS NULL
   OR u.id IS NULL
   OR (s.resuelto_por_usuario_id IS NOT NULL AND r.id IS NULL);

SELECT
    actividad_id,
    count(*) AS solicitudes_abiertas
FROM solicitud_cambio_actividad
WHERE estado IN ('PENDIENTE', 'EN_REVISION')
  AND deleted_at IS NULL
GROUP BY actividad_id
HAVING count(*) > 1;

-- 8. Integridad de seguimiento.
SELECT
    count(*) AS seguimientos_totales
FROM seguimiento_publicador;

SELECT
    count(*) AS referencias_huerfanas
FROM seguimiento_publicador s
LEFT JOIN usuario u ON u.id = s.usuario_id
LEFT JOIN perfil_publicador p ON p.id = s.perfil_publicador_id
WHERE u.id IS NULL OR p.id IS NULL;

SELECT
    usuario_id,
    perfil_publicador_id,
    count(*) AS repeticiones
FROM seguimiento_publicador
GROUP BY usuario_id, perfil_publicador_id
HAVING count(*) > 1;

-- 9. Extension, funciones y resolucion explícita.
SELECT
    e.extname AS extension,
    e.extversion AS version,
    n.nspname AS schema_extension,
    e.extrelocatable AS relocalizable
FROM pg_extension e
JOIN pg_namespace n ON n.oid = e.extnamespace
WHERE e.extname = 'unaccent';

SELECT
    n.nspname AS schema_funcion,
    p.proname AS funcion,
    pg_get_function_identity_arguments(p.oid) AS argumentos,
    pg_get_function_result(p.oid) AS retorno
FROM pg_proc p
JOIN pg_namespace n ON n.oid = p.pronamespace
WHERE p.proname = 'unaccent'
ORDER BY n.nspname, argumentos;

SELECT
    current_setting('search_path') AS search_path,
    to_regprocedure('unaccent(text)') AS unaccent_sin_schema,
    to_regprocedure('public.unaccent(text)') AS unaccent_en_public,
    to_regprocedure('extensions.unaccent(text)') AS unaccent_en_extensions;

SELECT
    n.nspname AS schema_extension,
    to_regprocedure(format('%I.unaccent(text)', n.nspname)) AS firma_explicita
FROM pg_extension e
JOIN pg_namespace n ON n.oid = e.extnamespace
WHERE e.extname = 'unaccent';

-- 10. Llamada real sin schema, igual que el SQL emitido por Hibernate.
-- Si esta sentencia falla o devuelve false, el release es NO-GO.
SELECT
    unaccent(lower('Natación')) = 'natacion' AS natacion_sin_tilde,
    unaccent(lower('Fútbol')) = 'futbol' AS futbol_sin_tilde;
