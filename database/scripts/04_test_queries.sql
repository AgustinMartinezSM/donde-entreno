-- ============================================================
-- DondeEntreno - Consultas SQL de prueba
-- Archivo: 04_test_queries.sql
-- Base de datos: PostgreSQL
-- Descripción:
-- Consultas útiles para probar búsquedas, filtros y detalles
-- del MVP de DondeEntreno.
--
-- Este archivo NO crea tablas ni inserta datos.
-- Solo sirve para probar consultas.
-- ============================================================


-- ============================================================
-- 1. VER TODAS LAS ACTIVIDADES PUBLICADAS
-- ------------------------------------------------------------
-- Esta consulta simula la búsqueda principal del visitante.
-- ============================================================

SELECT 
    a.id,
    a.titulo AS actividad,
    d.nombre AS deporte,
    cd.nombre AS categoria,
    pp.nombre AS publicador,
    u.nombre AS ubicacion,
    c.nombre AS ciudad,
    b.nombre AS barrio,
    a.nivel,
    a.enfoque,
    a.modalidad,
    a.precio_referencia,
    a.mostrar_precio,
    a.estado_publicacion
FROM actividad a
JOIN perfil_publicador pp ON pp.id = a.perfil_publicador_id
JOIN deporte d ON d.id = a.deporte_id
JOIN categoria_deportiva cd ON cd.id = d.categoria_deportiva_id
JOIN ubicacion u ON u.id = a.ubicacion_id
JOIN ciudad c ON c.id = u.ciudad_id
JOIN barrio b ON b.id = u.barrio_id
WHERE a.estado_publicacion = 'PUBLICADA'
  AND a.activa = true
  AND a.deleted_at IS NULL
  AND pp.activo = true
  AND pp.deleted_at IS NULL
  AND u.activa = true
  AND u.deleted_at IS NULL
  AND d.activo = true
  AND cd.activa = true
  AND c.activa = true
  AND b.activo = true
ORDER BY a.titulo;


-- ============================================================
-- 2. BUSCAR ACTIVIDADES PUBLICADAS POR CIUDAD
-- ------------------------------------------------------------
-- Ejemplo: actividades en Mar del Plata.
-- ============================================================

SELECT 
    a.titulo AS actividad,
    d.nombre AS deporte,
    pp.nombre AS publicador,
    c.nombre AS ciudad,
    b.nombre AS barrio
FROM actividad a
JOIN perfil_publicador pp ON pp.id = a.perfil_publicador_id
JOIN deporte d ON d.id = a.deporte_id
JOIN ubicacion u ON u.id = a.ubicacion_id
JOIN ciudad c ON c.id = u.ciudad_id
JOIN barrio b ON b.id = u.barrio_id
WHERE a.estado_publicacion = 'PUBLICADA'
  AND a.activa = true
  AND c.nombre = 'Mar del Plata'
ORDER BY a.titulo;


-- ============================================================
-- 3. BUSCAR ACTIVIDADES PUBLICADAS POR BARRIO
-- ------------------------------------------------------------
-- Ejemplo: actividades en Centro.
-- ============================================================

SELECT 
    a.titulo AS actividad,
    d.nombre AS deporte,
    pp.nombre AS publicador,
    b.nombre AS barrio,
    u.nombre AS ubicacion
FROM actividad a
JOIN perfil_publicador pp ON pp.id = a.perfil_publicador_id
JOIN deporte d ON d.id = a.deporte_id
JOIN ubicacion u ON u.id = a.ubicacion_id
JOIN barrio b ON b.id = u.barrio_id
WHERE a.estado_publicacion = 'PUBLICADA'
  AND a.activa = true
  AND b.nombre = 'Centro'
ORDER BY a.titulo;


-- ============================================================
-- 4. BUSCAR ACTIVIDADES POR DEPORTE
-- ------------------------------------------------------------
-- Ejemplo: Boxeo.
-- ============================================================

SELECT 
    a.titulo AS actividad,
    d.nombre AS deporte,
    pp.nombre AS publicador,
    b.nombre AS barrio,
    a.nivel,
    a.enfoque
FROM actividad a
JOIN deporte d ON d.id = a.deporte_id
JOIN perfil_publicador pp ON pp.id = a.perfil_publicador_id
JOIN ubicacion u ON u.id = a.ubicacion_id
JOIN barrio b ON b.id = u.barrio_id
WHERE a.estado_publicacion = 'PUBLICADA'
  AND a.activa = true
  AND d.slug = 'boxeo'
ORDER BY a.titulo;


-- ============================================================
-- 5. BUSCAR ACTIVIDADES POR CATEGORÍA DEPORTIVA
-- ------------------------------------------------------------
-- Ejemplo: Deportes de combate.
-- ============================================================

SELECT 
    a.titulo AS actividad,
    d.nombre AS deporte,
    cd.nombre AS categoria,
    pp.nombre AS publicador,
    b.nombre AS barrio
FROM actividad a
JOIN deporte d ON d.id = a.deporte_id
JOIN categoria_deportiva cd ON cd.id = d.categoria_deportiva_id
JOIN perfil_publicador pp ON pp.id = a.perfil_publicador_id
JOIN ubicacion u ON u.id = a.ubicacion_id
JOIN barrio b ON b.id = u.barrio_id
WHERE a.estado_publicacion = 'PUBLICADA'
  AND a.activa = true
  AND cd.slug = 'deportes-de-combate'
ORDER BY d.nombre, a.titulo;


-- ============================================================
-- 6. BUSCAR ACTIVIDADES POR DÍA DE LA SEMANA
-- ------------------------------------------------------------
-- Ejemplo: actividades los lunes.
-- ============================================================

SELECT 
    a.titulo AS actividad,
    d.nombre AS deporte,
    pp.nombre AS publicador,
    h.dia_semana,
    h.hora_inicio,
    h.hora_fin,
    b.nombre AS barrio
FROM actividad a
JOIN horario_actividad h ON h.actividad_id = a.id
JOIN deporte d ON d.id = a.deporte_id
JOIN perfil_publicador pp ON pp.id = a.perfil_publicador_id
JOIN ubicacion u ON u.id = a.ubicacion_id
JOIN barrio b ON b.id = u.barrio_id
WHERE a.estado_publicacion = 'PUBLICADA'
  AND a.activa = true
  AND h.activo = true
  AND h.dia_semana = 'LUNES'
ORDER BY h.hora_inicio;


-- ============================================================
-- 7. BUSCAR ACTIVIDADES DESPUÉS DE UNA HORA
-- ------------------------------------------------------------
-- Ejemplo: actividades que empiezan desde las 18:00.
-- ============================================================

SELECT 
    a.titulo AS actividad,
    d.nombre AS deporte,
    pp.nombre AS publicador,
    h.dia_semana,
    h.hora_inicio,
    h.hora_fin,
    b.nombre AS barrio
FROM actividad a
JOIN horario_actividad h ON h.actividad_id = a.id
JOIN deporte d ON d.id = a.deporte_id
JOIN perfil_publicador pp ON pp.id = a.perfil_publicador_id
JOIN ubicacion u ON u.id = a.ubicacion_id
JOIN barrio b ON b.id = u.barrio_id
WHERE a.estado_publicacion = 'PUBLICADA'
  AND a.activa = true
  AND h.activo = true
  AND h.hora_inicio >= '18:00'
ORDER BY h.dia_semana, h.hora_inicio;


-- ============================================================
-- 8. BUSCAR ACTIVIDADES POR NIVEL
-- ------------------------------------------------------------
-- Ejemplo: principiantes.
-- ============================================================

SELECT 
    a.titulo AS actividad,
    d.nombre AS deporte,
    pp.nombre AS publicador,
    a.nivel,
    a.enfoque,
    b.nombre AS barrio
FROM actividad a
JOIN deporte d ON d.id = a.deporte_id
JOIN perfil_publicador pp ON pp.id = a.perfil_publicador_id
JOIN ubicacion u ON u.id = a.ubicacion_id
JOIN barrio b ON b.id = u.barrio_id
WHERE a.estado_publicacion = 'PUBLICADA'
  AND a.activa = true
  AND a.nivel IN ('PRINCIPIANTE', 'TODOS')
ORDER BY a.titulo;


-- ============================================================
-- 9. BUSCAR ACTIVIDADES POR EDAD DEL USUARIO
-- ------------------------------------------------------------
-- Ejemplo: usuario de 18 años.
-- La actividad sirve si:
-- edad_minima es NULL o <= 18
-- edad_maxima es NULL o >= 18
-- ============================================================

SELECT 
    a.titulo AS actividad,
    d.nombre AS deporte,
    pp.nombre AS publicador,
    a.edad_minima,
    a.edad_maxima,
    b.nombre AS barrio
FROM actividad a
JOIN deporte d ON d.id = a.deporte_id
JOIN perfil_publicador pp ON pp.id = a.perfil_publicador_id
JOIN ubicacion u ON u.id = a.ubicacion_id
JOIN barrio b ON b.id = u.barrio_id
WHERE a.estado_publicacion = 'PUBLICADA'
  AND a.activa = true
  AND (a.edad_minima IS NULL OR a.edad_minima <= 18)
  AND (a.edad_maxima IS NULL OR a.edad_maxima >= 18)
ORDER BY a.titulo;


-- ============================================================
-- 10. VER DETALLE COMPLETO DE UNA ACTIVIDAD POR SLUG
-- ------------------------------------------------------------
-- Esta consulta sirve para una futura pantalla:
-- /actividades/{slug}
-- ============================================================

SELECT 
    a.id,
    a.titulo,
    a.slug,
    a.descripcion,
    a.edad_minima,
    a.edad_maxima,
    a.nivel,
    a.enfoque,
    a.modalidad,
    a.precio_referencia,
    a.mostrar_precio,
    a.requiere_inscripcion,
    a.cupos_limitados,

    d.nombre AS deporte,
    cd.nombre AS categoria,

    pp.nombre AS publicador,
    pp.tipo_publicador,
    pp.descripcion AS descripcion_publicador,

    u.nombre AS ubicacion,
    u.direccion,
    u.referencia,
    u.latitud,
    u.longitud,
    u.google_maps_url,

    c.nombre AS ciudad,
    b.nombre AS barrio,

    -- Contacto final:
    -- si la actividad tiene contacto propio, usamos ese;
    -- si no, usamos el contacto del perfil publicador.
    COALESCE(a.whatsapp_contacto, pp.whatsapp) AS whatsapp_final,
    COALESCE(a.instagram_contacto, pp.instagram) AS instagram_final,
    COALESCE(a.email_contacto, pp.email_contacto) AS email_final

FROM actividad a
JOIN deporte d ON d.id = a.deporte_id
JOIN categoria_deportiva cd ON cd.id = d.categoria_deportiva_id
JOIN perfil_publicador pp ON pp.id = a.perfil_publicador_id
JOIN ubicacion u ON u.id = a.ubicacion_id
JOIN ciudad c ON c.id = u.ciudad_id
JOIN barrio b ON b.id = u.barrio_id
WHERE a.slug = 'boxeo-recreativo-adultos-principiantes';


-- ============================================================
-- 11. VER HORARIOS DE UNA ACTIVIDAD
-- ------------------------------------------------------------
-- Ejemplo usando slug.
-- ============================================================

SELECT 
    a.titulo AS actividad,
    h.dia_semana,
    h.hora_inicio,
    h.hora_fin,
    h.observacion
FROM actividad a
JOIN horario_actividad h ON h.actividad_id = a.id
WHERE a.slug = 'boxeo-recreativo-adultos-principiantes'
  AND h.activo = true
ORDER BY 
    CASE h.dia_semana
        WHEN 'LUNES' THEN 1
        WHEN 'MARTES' THEN 2
        WHEN 'MIERCOLES' THEN 3
        WHEN 'JUEVES' THEN 4
        WHEN 'VIERNES' THEN 5
        WHEN 'SABADO' THEN 6
        WHEN 'DOMINGO' THEN 7
    END,
    h.hora_inicio;


-- ============================================================
-- 12. VER IMAGEN PRINCIPAL DE UNA ACTIVIDAD
-- ============================================================

SELECT 
    a.titulo AS actividad,
    i.url,
    i.tipo_imagen,
    i.titulo AS titulo_imagen
FROM actividad a
JOIN imagen i ON i.actividad_id = a.id
WHERE a.slug = 'boxeo-recreativo-adultos-principiantes'
  AND i.tipo_imagen = 'PRINCIPAL'
  AND i.activa = true;


-- ============================================================
-- 13. VER LOGO Y PORTADA DE UN PERFIL PUBLICADOR
-- ============================================================

SELECT 
    pp.nombre AS publicador,
    i.tipo_imagen,
    i.url,
    i.titulo
FROM perfil_publicador pp
JOIN imagen i ON i.perfil_publicador_id = pp.id
WHERE pp.nombre = 'Club Atlético Kimberley'
  AND i.tipo_imagen IN ('LOGO', 'PORTADA')
  AND i.activa = true
ORDER BY i.tipo_imagen;


-- ============================================================
-- 14. VER ACTIVIDADES DE UN PERFIL PUBLICADOR
-- ------------------------------------------------------------
-- Ejemplo: Club Atlético Kimberley.
-- ============================================================

SELECT 
    pp.nombre AS publicador,
    a.titulo AS actividad,
    d.nombre AS deporte,
    a.estado_publicacion,
    a.activa
FROM perfil_publicador pp
JOIN actividad a ON a.perfil_publicador_id = pp.id
JOIN deporte d ON d.id = a.deporte_id
WHERE pp.nombre = 'Club Atlético Kimberley'
ORDER BY a.titulo;


-- ============================================================
-- 15. VER ACTIVIDADES PENDIENTES DE REVISIÓN
-- ------------------------------------------------------------
-- Esta consulta sería útil para el panel admin.
-- ============================================================

SELECT 
    a.id,
    a.titulo AS actividad,
    pp.nombre AS publicador,
    d.nombre AS deporte,
    a.estado_publicacion,
    a.created_at
FROM actividad a
JOIN perfil_publicador pp ON pp.id = a.perfil_publicador_id
JOIN deporte d ON d.id = a.deporte_id
WHERE a.estado_publicacion = 'PENDIENTE_REVISION'
  AND a.activa = true
ORDER BY a.created_at ASC;


-- ============================================================
-- 16. CONTAR ACTIVIDADES PUBLICADAS POR DEPORTE
-- ------------------------------------------------------------
-- Útil para estadísticas básicas o filtros con contador.
-- ============================================================

SELECT 
    d.nombre AS deporte,
    COUNT(a.id) AS cantidad_actividades
FROM deporte d
LEFT JOIN actividad a 
    ON a.deporte_id = d.id
    AND a.estado_publicacion = 'PUBLICADA'
    AND a.activa = true
GROUP BY d.nombre
ORDER BY cantidad_actividades DESC, d.nombre;


-- ============================================================
-- 17. CONTAR ACTIVIDADES PUBLICADAS POR BARRIO
-- ============================================================

SELECT 
    b.nombre AS barrio,
    COUNT(a.id) AS cantidad_actividades
FROM barrio b
LEFT JOIN ubicacion u ON u.barrio_id = b.id
LEFT JOIN actividad a 
    ON a.ubicacion_id = u.id
    AND a.estado_publicacion = 'PUBLICADA'
    AND a.activa = true
GROUP BY b.nombre
ORDER BY cantidad_actividades DESC, b.nombre;


-- ============================================================
-- 18. BÚSQUEDA SIMPLE POR TEXTO EN TÍTULO O DESCRIPCIÓN
-- ------------------------------------------------------------
-- Ejemplo: buscar "boxeo".
-- Más adelante esto puede mejorar con full text search.
-- ============================================================

SELECT 
    a.titulo AS actividad,
    d.nombre AS deporte,
    pp.nombre AS publicador,
    b.nombre AS barrio
FROM actividad a
JOIN deporte d ON d.id = a.deporte_id
JOIN perfil_publicador pp ON pp.id = a.perfil_publicador_id
JOIN ubicacion u ON u.id = a.ubicacion_id
JOIN barrio b ON b.id = u.barrio_id
WHERE a.estado_publicacion = 'PUBLICADA'
  AND a.activa = true
  AND (
      LOWER(a.titulo) LIKE LOWER('%boxeo%')
      OR LOWER(a.descripcion) LIKE LOWER('%boxeo%')
      OR LOWER(d.nombre) LIKE LOWER('%boxeo%')
  )
ORDER BY a.titulo;


-- ============================================================
-- 19. CONSULTA GENERAL PARA TARJETAS DEL FRONTEND
-- ------------------------------------------------------------
-- Esta consulta devuelve datos típicos para una card:
-- título, deporte, publicador, barrio, precio, horario e imagen.
-- ============================================================

SELECT 
    a.id,
    a.titulo,
    a.slug,
    d.nombre AS deporte,
    pp.nombre AS publicador,
    b.nombre AS barrio,
    c.nombre AS ciudad,
    a.nivel,
    a.enfoque,
    CASE 
        WHEN a.mostrar_precio = true AND a.precio_referencia IS NOT NULL
            THEN '$' || a.precio_referencia::TEXT
        ELSE 'Consultar precio'
    END AS precio_visible,
    i.url AS imagen_principal
FROM actividad a
JOIN deporte d ON d.id = a.deporte_id
JOIN perfil_publicador pp ON pp.id = a.perfil_publicador_id
JOIN ubicacion u ON u.id = a.ubicacion_id
JOIN ciudad c ON c.id = u.ciudad_id
JOIN barrio b ON b.id = u.barrio_id
LEFT JOIN imagen i 
    ON i.actividad_id = a.id
    AND i.tipo_imagen = 'PRINCIPAL'
    AND i.activa = true
WHERE a.estado_publicacion = 'PUBLICADA'
  AND a.activa = true
ORDER BY a.titulo;


-- ============================================================
-- 20. VERIFICACIÓN FINAL DE CANTIDADES
-- ============================================================

SELECT 
    (SELECT COUNT(*) FROM rol) AS roles,
    (SELECT COUNT(*) FROM usuario) AS usuarios,
    (SELECT COUNT(*) FROM perfil_publicador) AS perfiles_publicadores,
    (SELECT COUNT(*) FROM ciudad) AS ciudades,
    (SELECT COUNT(*) FROM barrio) AS barrios,
    (SELECT COUNT(*) FROM ubicacion) AS ubicaciones,
    (SELECT COUNT(*) FROM categoria_deportiva) AS categorias_deportivas,
    (SELECT COUNT(*) FROM deporte) AS deportes,
    (SELECT COUNT(*) FROM actividad) AS actividades,
    (SELECT COUNT(*) FROM horario_actividad) AS horarios,
    (SELECT COUNT(*) FROM imagen) AS imagenes;