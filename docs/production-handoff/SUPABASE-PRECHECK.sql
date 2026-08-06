-- DondeEntreno: control previo de Supabase.
-- Solo lectura. Ejecutar el bloque completo y conservar el resultado.

-- 1. Contexto efectivo de la sesion.
SELECT
    current_database() AS base_actual,
    current_user AS usuario_actual,
    session_user AS usuario_sesion,
    current_schema() AS schema_actual,
    current_setting('search_path') AS search_path,
    current_schemas(true) AS schemas_efectivos,
    version() AS version_postgresql;

-- 2. Disponibilidad e instalacion de extensiones.
SELECT
    name,
    default_version,
    installed_version,
    comment
FROM pg_available_extensions
ORDER BY name;

SELECT
    e.extname AS extension,
    e.extversion AS version,
    n.nspname AS schema_extension,
    e.extrelocatable AS relocalizable
FROM pg_extension e
JOIN pg_namespace n ON n.oid = e.extnamespace
ORDER BY e.extname;

-- 3. Objetos de unaccent y resolucion por search_path o schema explicito.
SELECT
    n.nspname AS schema_funcion,
    p.proname AS funcion,
    pg_get_function_identity_arguments(p.oid) AS argumentos,
    pg_get_function_result(p.oid) AS retorno,
    p.prokind AS tipo_objeto
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

-- 4. Tablas base y tablas objetivo visibles en schemas de usuario.
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

-- 5. Columnas requeridas y metadata actual.
WITH columnas_requeridas(tabla, columna) AS (
    VALUES
        ('usuario', 'id'),
        ('perfil_publicador', 'id'),
        ('actividad', 'id'),
        ('imagen', 'id'),
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

-- 6. Nombres de constraints que usarán las migraciones.
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

-- El control del nombre de este CHECK es global en el script 15.
SELECT
    con.conname,
    n.nspname AS schema_encontrado,
    rel.relname AS tabla_encontrada,
    pg_get_constraintdef(con.oid, true) AS definicion
FROM pg_constraint con
JOIN pg_class rel ON rel.oid = con.conrelid
JOIN pg_namespace n ON n.oid = rel.relnamespace
WHERE con.conname = 'chk_imagen_estado_moderacion';

-- 7. Indices que usarán las migraciones.
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

-- 8. Estado y volumen estimado de las tablas relacionadas.
SELECT
    schemaname,
    relname AS tabla,
    n_live_tup AS filas_vivas_estimadas,
    n_dead_tup AS filas_muertas_estimadas,
    last_analyze,
    last_autoanalyze
FROM pg_stat_user_tables
WHERE relname IN (
    'usuario',
    'perfil_publicador',
    'actividad',
    'imagen',
    'solicitud_cambio_actividad',
    'seguimiento_publicador'
)
ORDER BY schemaname, relname;

-- 9. Filas de imagen alcanzadas por el default del script 15.
-- to_jsonb permite inspeccionar las claves aun si las columnas nuevas faltan.
SELECT
    count(*) AS imagenes_totales,
    count(*) FILTER (
        WHERE NOT (to_jsonb(i) ? 'estado_moderacion')
    ) AS filas_sin_clave_estado,
    count(*) FILTER (
        WHERE to_jsonb(i) ? 'estado_moderacion'
          AND to_jsonb(i) ->> 'estado_moderacion' IS NULL
    ) AS filas_con_estado_nulo,
    count(*) FILTER (
        WHERE to_jsonb(i) ? 'estado_moderacion'
          AND to_jsonb(i) ->> 'estado_moderacion' NOT IN (
              'PENDIENTE', 'APROBADA', 'RECHAZADA'
          )
    ) AS filas_con_estado_fuera_de_dominio
FROM imagen i;

-- 10. Secuencias objetivo y cualquier colision de nombre.
WITH secuencias_requeridas(nombre) AS (
    VALUES
        ('solicitud_cambio_actividad_id_seq'),
        ('seguimiento_publicador_id_seq')
)
SELECT
    r.nombre AS secuencia,
    n.nspname AS schema_encontrado,
    c.relkind AS tipo_relacion
FROM secuencias_requeridas r
LEFT JOIN pg_class c ON c.relname = r.nombre
LEFT JOIN pg_namespace n
    ON n.oid = c.relnamespace
   AND n.nspname NOT IN ('pg_catalog', 'information_schema')
ORDER BY r.nombre, n.nspname;
