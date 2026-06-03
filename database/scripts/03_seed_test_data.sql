-- ============================================================
-- DondeEntreno - Datos de prueba para el MVP
-- Archivo: 03_seed_test_data.sql
-- Base de datos: PostgreSQL
-- Descripción:
-- Este script carga datos ficticios para probar búsquedas,
-- filtros, perfiles publicadores, actividades, horarios e imágenes.
--
-- Debe ejecutarse después de:
-- 01_create_tables.sql
-- 02_seed_data.sql
-- ============================================================


-- ============================================================
-- USUARIOS DE PRUEBA
-- ------------------------------------------------------------
-- IMPORTANTE:
-- Estos password_hash son valores de prueba.
-- Más adelante, cuando conectemos Spring Security, vamos a generar
-- contraseñas reales con BCrypt desde el backend.
-- ============================================================

INSERT INTO usuario (rol_id, nombre, apellido, email, password_hash, telefono, activo, email_verificado)
VALUES
    (
        (SELECT id FROM rol WHERE nombre = 'SUPER_ADMIN'),
        'Agustín',
        'Martínez',
        'admin@dondeentreno.com',
        '$2a$10$ejemploHashTemporalNoUsarEnProduccion123456789',
        '2230000000',
        true,
        true
    ),
    (
        (SELECT id FROM rol WHERE nombre = 'PUBLICADOR'),
        'Contacto',
        'Kimberley',
        'contacto@kimberley.com',
        '$2a$10$ejemploHashTemporalNoUsarEnProduccion123456789',
        '2231111111',
        true,
        true
    ),
    (
        (SELECT id FROM rol WHERE nombre = 'PUBLICADOR'),
        'Juan',
        'Pérez',
        'juan.entrena@gmail.com',
        '$2a$10$ejemploHashTemporalNoUsarEnProduccion123456789',
        '2232222222',
        true,
        true
    ),
    (
        (SELECT id FROM rol WHERE nombre = 'PUBLICADOR'),
        'Laura',
        'Gómez',
        'laura.yoga@gmail.com',
        '$2a$10$ejemploHashTemporalNoUsarEnProduccion123456789',
        '2233333333',
        true,
        true
    ),
    (
        (SELECT id FROM rol WHERE nombre = 'PUBLICADOR'),
        'Martín',
        'Rodríguez',
        'martin.boxeo@gmail.com',
        '$2a$10$ejemploHashTemporalNoUsarEnProduccion123456789',
        '2234444444',
        true,
        true
    );


-- ============================================================
-- PERFILES PUBLICADORES DE PRUEBA
-- ============================================================

INSERT INTO perfil_publicador (
    usuario_id,
    nombre,
    tipo_publicador,
    descripcion,
    email_contacto,
    telefono_contacto,
    whatsapp,
    instagram,
    sitio_web,
    activo,
    verificado
)
VALUES
    (
        (SELECT id FROM usuario WHERE email = 'contacto@kimberley.com'),
        'Club Atlético Kimberley',
        'CLUB',
        'Club deportivo de Mar del Plata con actividades para distintas edades y niveles.',
        'contacto@kimberley.com',
        '2231111111',
        '2231111111',
        '@clubkimberley',
        'https://clubkimberley.com',
        true,
        true
    ),
    (
        (SELECT id FROM usuario WHERE email = 'juan.entrena@gmail.com'),
        'Juan Pérez Entrenador',
        'PROFESOR_INDEPENDIENTE',
        'Entrenador independiente especializado en funcional, musculación y entrenamiento personalizado.',
        'juan.entrena@gmail.com',
        '2232222222',
        '2232222222',
        '@juan.entrena',
        NULL,
        true,
        true
    ),
    (
        (SELECT id FROM usuario WHERE email = 'laura.yoga@gmail.com'),
        'Laura Yoga Studio',
        'ESCUELA_DEPORTIVA',
        'Espacio de yoga y bienestar para principiantes y personas con experiencia.',
        'laura.yoga@gmail.com',
        '2233333333',
        '2233333333',
        '@laurayogastudio',
        NULL,
        true,
        true
    ),
    (
        (SELECT id FROM usuario WHERE email = 'martin.boxeo@gmail.com'),
        'Escuela de Boxeo Norte',
        'ESCUELA_DEPORTIVA',
        'Escuela orientada a boxeo recreativo, competitivo y entrenamiento físico.',
        'martin.boxeo@gmail.com',
        '2234444444',
        '2234444444',
        '@boxeonorte',
        NULL,
        true,
        true
    );


-- ============================================================
-- UBICACIONES DE PRUEBA
-- ============================================================

INSERT INTO ubicacion (
    perfil_publicador_id,
    ciudad_id,
    barrio_id,
    nombre,
    direccion,
    referencia,
    latitud,
    longitud,
    google_maps_url,
    activa
)
VALUES
    (
        (SELECT id FROM perfil_publicador WHERE nombre = 'Club Atlético Kimberley'),
        (SELECT id FROM ciudad WHERE nombre = 'Mar del Plata'),
        (SELECT id FROM barrio WHERE nombre = 'Constitución' AND ciudad_id = (SELECT id FROM ciudad WHERE nombre = 'Mar del Plata')),
        'Sede Club Atlético Kimberley',
        'Av. Independencia 3030',
        'Ingreso principal por avenida.',
        -38.005477,
        -57.542611,
        'https://maps.google.com',
        true
    ),
    (
        (SELECT id FROM perfil_publicador WHERE nombre = 'Juan Pérez Entrenador'),
        (SELECT id FROM ciudad WHERE nombre = 'Mar del Plata'),
        (SELECT id FROM barrio WHERE nombre = 'Güemes' AND ciudad_id = (SELECT id FROM ciudad WHERE nombre = 'Mar del Plata')),
        'Plaza zona Güemes',
        'Güemes y Alvarado',
        'Punto de encuentro frente a la plaza.',
        -38.012300,
        -57.542000,
        'https://maps.google.com',
        true
    ),
    (
        (SELECT id FROM perfil_publicador WHERE nombre = 'Laura Yoga Studio'),
        (SELECT id FROM ciudad WHERE nombre = 'Mar del Plata'),
        (SELECT id FROM barrio WHERE nombre = 'Los Troncos' AND ciudad_id = (SELECT id FROM ciudad WHERE nombre = 'Mar del Plata')),
        'Laura Yoga Studio',
        'Rawson 1250',
        'Estudio privado en primer piso.',
        -38.017500,
        -57.536800,
        'https://maps.google.com',
        true
    ),
    (
        (SELECT id FROM perfil_publicador WHERE nombre = 'Escuela de Boxeo Norte'),
        (SELECT id FROM ciudad WHERE nombre = 'Mar del Plata'),
        (SELECT id FROM barrio WHERE nombre = 'Centro' AND ciudad_id = (SELECT id FROM ciudad WHERE nombre = 'Mar del Plata')),
        'Sede Escuela de Boxeo Norte',
        'Av. Independencia 1234',
        'Entrada por puerta lateral.',
        -38.003800,
        -57.548100,
        'https://maps.google.com',
        true
    ),
    (
        (SELECT id FROM perfil_publicador WHERE nombre = 'Club Atlético Kimberley'),
        (SELECT id FROM ciudad WHERE nombre = 'Mar del Plata'),
        (SELECT id FROM barrio WHERE nombre = 'Puerto' AND ciudad_id = (SELECT id FROM ciudad WHERE nombre = 'Mar del Plata')),
        'Natatorio Kimberley',
        'Juan B. Justo 3500',
        'Sector natatorio cubierto.',
        -38.034200,
        -57.557400,
        'https://maps.google.com',
        true
    );


-- ============================================================
-- ACTIVIDADES DE PRUEBA
-- ============================================================

INSERT INTO actividad (
    perfil_publicador_id,
    deporte_id,
    ubicacion_id,
    titulo,
    slug,
    descripcion,
    edad_minima,
    edad_maxima,
    nivel,
    enfoque,
    modalidad,
    precio_referencia,
    mostrar_precio,
    requiere_inscripcion,
    cupos_limitados,
    whatsapp_contacto,
    instagram_contacto,
    email_contacto,
    estado_publicacion,
    activa
)
VALUES
    (
        (SELECT id FROM perfil_publicador WHERE nombre = 'Escuela de Boxeo Norte'),
        (SELECT id FROM deporte WHERE slug = 'boxeo'),
        (SELECT id FROM ubicacion WHERE nombre = 'Sede Escuela de Boxeo Norte'),
        'Boxeo recreativo para adultos principiantes',
        'boxeo-recreativo-adultos-principiantes',
        'Clases orientadas a personas adultas que quieren iniciarse en boxeo de forma recreativa.',
        18,
        NULL,
        'PRINCIPIANTE',
        'RECREATIVO',
        'PRESENCIAL',
        18000.00,
        true,
        true,
        false,
        NULL,
        NULL,
        NULL,
        'PUBLICADA',
        true
    ),
    (
        (SELECT id FROM perfil_publicador WHERE nombre = 'Club Atlético Kimberley'),
        (SELECT id FROM deporte WHERE slug = 'jiu-jitsu'),
        (SELECT id FROM ubicacion WHERE nombre = 'Sede Club Atlético Kimberley'),
        'Jiu Jitsu para adultos',
        'jiu-jitsu-para-adultos-kimberley',
        'Clases de Jiu Jitsu para adultos, con grupos para principiantes e intermedios.',
        16,
        NULL,
        'TODOS',
        'MIXTO',
        'PRESENCIAL',
        22000.00,
        true,
        true,
        true,
        '2235555555',
        '@jiujitsu.kimberley',
        NULL,
        'PUBLICADA',
        true
    ),
    (
        (SELECT id FROM perfil_publicador WHERE nombre = 'Club Atlético Kimberley'),
        (SELECT id FROM deporte WHERE slug = 'futbol'),
        (SELECT id FROM ubicacion WHERE nombre = 'Sede Club Atlético Kimberley'),
        'Fútbol infantil categoría 2014',
        'futbol-infantil-categoria-2014-kimberley',
        'Entrenamientos de fútbol infantil para chicos de categoría 2014.',
        10,
        12,
        'TODOS',
        'COMPETITIVO',
        'PRESENCIAL',
        NULL,
        false,
        true,
        true,
        NULL,
        NULL,
        NULL,
        'PUBLICADA',
        true
    ),
    (
        (SELECT id FROM perfil_publicador WHERE nombre = 'Juan Pérez Entrenador'),
        (SELECT id FROM deporte WHERE slug = 'funcional'),
        (SELECT id FROM ubicacion WHERE nombre = 'Plaza zona Güemes'),
        'Funcional grupal al aire libre',
        'funcional-grupal-al-aire-libre-guemes',
        'Entrenamiento funcional grupal en plaza, orientado a mejorar fuerza, resistencia y movilidad.',
        16,
        NULL,
        'TODOS',
        'RECREATIVO',
        'PRESENCIAL',
        15000.00,
        true,
        true,
        false,
        NULL,
        NULL,
        NULL,
        'PUBLICADA',
        true
    ),
    (
        (SELECT id FROM perfil_publicador WHERE nombre = 'Laura Yoga Studio'),
        (SELECT id FROM deporte WHERE slug = 'yoga'),
        (SELECT id FROM ubicacion WHERE nombre = 'Laura Yoga Studio'),
        'Yoga inicial para principiantes',
        'yoga-inicial-para-principiantes',
        'Clases suaves de yoga para personas que quieren empezar desde cero.',
        15,
        NULL,
        'PRINCIPIANTE',
        'RECREATIVO',
        'PRESENCIAL',
        20000.00,
        true,
        true,
        false,
        NULL,
        NULL,
        NULL,
        'PUBLICADA',
        true
    ),
    (
        (SELECT id FROM perfil_publicador WHERE nombre = 'Club Atlético Kimberley'),
        (SELECT id FROM deporte WHERE slug = 'natacion'),
        (SELECT id FROM ubicacion WHERE nombre = 'Natatorio Kimberley'),
        'Natación para niños',
        'natacion-para-ninos-kimberley',
        'Clases de natación para niños con profesores especializados.',
        6,
        12,
        'PRINCIPIANTE',
        'RECREATIVO',
        'PRESENCIAL',
        25000.00,
        true,
        true,
        true,
        NULL,
        NULL,
        NULL,
        'PUBLICADA',
        true
    ),
    (
        (SELECT id FROM perfil_publicador WHERE nombre = 'Juan Pérez Entrenador'),
        (SELECT id FROM deporte WHERE slug = 'entrenamiento-personalizado'),
        (SELECT id FROM ubicacion WHERE nombre = 'Plaza zona Güemes'),
        'Entrenamiento personalizado',
        'entrenamiento-personalizado-juan-perez',
        'Plan de entrenamiento individual adaptado al objetivo de cada persona.',
        18,
        NULL,
        'TODOS',
        'RECREATIVO',
        'PRESENCIAL',
        NULL,
        false,
        true,
        true,
        '2232222222',
        '@juan.entrena',
        'juan.entrena@gmail.com',
        'PENDIENTE_REVISION',
        true
    );


-- ============================================================
-- HORARIOS DE PRUEBA
-- ============================================================

INSERT INTO horario_actividad (
    actividad_id,
    dia_semana,
    hora_inicio,
    hora_fin,
    observacion,
    activo
)
VALUES
    (
        (SELECT id FROM actividad WHERE slug = 'boxeo-recreativo-adultos-principiantes'),
        'LUNES',
        '18:00',
        '19:30',
        NULL,
        true
    ),
    (
        (SELECT id FROM actividad WHERE slug = 'boxeo-recreativo-adultos-principiantes'),
        'MIERCOLES',
        '18:00',
        '19:30',
        NULL,
        true
    ),
    (
        (SELECT id FROM actividad WHERE slug = 'boxeo-recreativo-adultos-principiantes'),
        'VIERNES',
        '19:00',
        '20:30',
        'Traer vendas o guantes propios.',
        true
    ),
    (
        (SELECT id FROM actividad WHERE slug = 'jiu-jitsu-para-adultos-kimberley'),
        'MARTES',
        '20:00',
        '21:30',
        NULL,
        true
    ),
    (
        (SELECT id FROM actividad WHERE slug = 'jiu-jitsu-para-adultos-kimberley'),
        'JUEVES',
        '20:00',
        '21:30',
        NULL,
        true
    ),
    (
        (SELECT id FROM actividad WHERE slug = 'futbol-infantil-categoria-2014-kimberley'),
        'LUNES',
        '17:30',
        '19:00',
        NULL,
        true
    ),
    (
        (SELECT id FROM actividad WHERE slug = 'futbol-infantil-categoria-2014-kimberley'),
        'MIERCOLES',
        '17:30',
        '19:00',
        NULL,
        true
    ),
    (
        (SELECT id FROM actividad WHERE slug = 'funcional-grupal-al-aire-libre-guemes'),
        'MARTES',
        '08:00',
        '09:00',
        'Clase sujeta a clima.',
        true
    ),
    (
        (SELECT id FROM actividad WHERE slug = 'funcional-grupal-al-aire-libre-guemes'),
        'JUEVES',
        '08:00',
        '09:00',
        'Clase sujeta a clima.',
        true
    ),
    (
        (SELECT id FROM actividad WHERE slug = 'yoga-inicial-para-principiantes'),
        'LUNES',
        '10:00',
        '11:00',
        NULL,
        true
    ),
    (
        (SELECT id FROM actividad WHERE slug = 'yoga-inicial-para-principiantes'),
        'MIERCOLES',
        '10:00',
        '11:00',
        NULL,
        true
    ),
    (
        (SELECT id FROM actividad WHERE slug = 'natacion-para-ninos-kimberley'),
        'SABADO',
        '09:00',
        '10:30',
        NULL,
        true
    ),
    (
        (SELECT id FROM actividad WHERE slug = 'entrenamiento-personalizado-juan-perez'),
        'VIERNES',
        '15:00',
        '16:00',
        'Horario inicial de prueba. Puede coordinarse otro horario.',
        true
    );


-- ============================================================
-- IMÁGENES FICTICIAS DE PRUEBA
-- ------------------------------------------------------------
-- Las URLs son rutas de ejemplo. Más adelante se reemplazan por
-- rutas reales del servidor o storage.
-- ============================================================

INSERT INTO imagen (
    perfil_publicador_id,
    actividad_id,
    url,
    tipo_imagen,
    titulo,
    descripcion,
    orden,
    activa
)
VALUES
    (
        (SELECT id FROM perfil_publicador WHERE nombre = 'Club Atlético Kimberley'),
        NULL,
        '/uploads/perfiles/kimberley-logo.png',
        'LOGO',
        'Logo Club Kimberley',
        'Logo del club.',
        0,
        true
    ),
    (
        (SELECT id FROM perfil_publicador WHERE nombre = 'Club Atlético Kimberley'),
        NULL,
        '/uploads/perfiles/kimberley-portada.jpg',
        'PORTADA',
        'Portada Club Kimberley',
        'Imagen de portada del club.',
        0,
        true
    ),
    (
        (SELECT id FROM perfil_publicador WHERE nombre = 'Escuela de Boxeo Norte'),
        NULL,
        '/uploads/perfiles/boxeo-norte-logo.png',
        'LOGO',
        'Logo Escuela de Boxeo Norte',
        'Logo de la escuela.',
        0,
        true
    ),
    (
        NULL,
        (SELECT id FROM actividad WHERE slug = 'boxeo-recreativo-adultos-principiantes'),
        '/uploads/actividades/boxeo-principal.jpg',
        'PRINCIPAL',
        'Clase de boxeo',
        'Imagen principal de boxeo recreativo.',
        0,
        true
    ),
    (
        NULL,
        (SELECT id FROM actividad WHERE slug = 'jiu-jitsu-para-adultos-kimberley'),
        '/uploads/actividades/jiu-jitsu-principal.jpg',
        'PRINCIPAL',
        'Clase de Jiu Jitsu',
        'Imagen principal de Jiu Jitsu.',
        0,
        true
    ),
    (
        NULL,
        (SELECT id FROM actividad WHERE slug = 'funcional-grupal-al-aire-libre-guemes'),
        '/uploads/actividades/funcional-principal.jpg',
        'PRINCIPAL',
        'Funcional al aire libre',
        'Imagen principal de entrenamiento funcional.',
        0,
        true
    ),
    (
        NULL,
        (SELECT id FROM actividad WHERE slug = 'yoga-inicial-para-principiantes'),
        '/uploads/actividades/yoga-principal.jpg',
        'PRINCIPAL',
        'Yoga inicial',
        'Imagen principal de yoga.',
        0,
        true
    ),
    (
        NULL,
        (SELECT id FROM actividad WHERE slug = 'natacion-para-ninos-kimberley'),
        '/uploads/actividades/natacion-principal.jpg',
        'PRINCIPAL',
        'Natación para niños',
        'Imagen principal de natación.',
        0,
        true
    );