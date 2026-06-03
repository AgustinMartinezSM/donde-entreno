-- ============================================================
-- DondeEntreno - Datos iniciales básicos
-- Archivo: 02_seed_data.sql
-- Base de datos: PostgreSQL
-- Descripción:
-- Este script carga datos mínimos para empezar a probar el MVP.
-- Debe ejecutarse después de 01_create_tables.sql.
-- ============================================================


-- ============================================================
-- ROLES DEL SISTEMA
-- ============================================================

INSERT INTO rol (nombre, descripcion)
VALUES
    ('SUPER_ADMIN', 'Control total del sistema. Puede administrar usuarios, catálogos y publicaciones.'),
    ('ADMIN', 'Puede revisar, aprobar, pausar o rechazar publicaciones.'),
    ('PUBLICADOR', 'Puede crear perfiles publicadores, ubicaciones y actividades.');


-- ============================================================
-- CIUDAD INICIAL
-- ============================================================

INSERT INTO ciudad (nombre, provincia, pais)
VALUES
    ('Mar del Plata', 'Buenos Aires', 'Argentina');


-- ============================================================
-- BARRIOS / ZONAS DE MAR DEL PLATA
-- ------------------------------------------------------------
-- Para el MVP usamos barrios y zonas conocidas.
-- Más adelante se pueden agregar, editar o desactivar.
-- ============================================================

INSERT INTO barrio (ciudad_id, nombre)
VALUES
    ((SELECT id FROM ciudad WHERE nombre = 'Mar del Plata' AND provincia = 'Buenos Aires' AND pais = 'Argentina'), 'Centro'),
    ((SELECT id FROM ciudad WHERE nombre = 'Mar del Plata' AND provincia = 'Buenos Aires' AND pais = 'Argentina'), 'La Perla'),
    ((SELECT id FROM ciudad WHERE nombre = 'Mar del Plata' AND provincia = 'Buenos Aires' AND pais = 'Argentina'), 'Güemes'),
    ((SELECT id FROM ciudad WHERE nombre = 'Mar del Plata' AND provincia = 'Buenos Aires' AND pais = 'Argentina'), 'Constitución'),
    ((SELECT id FROM ciudad WHERE nombre = 'Mar del Plata' AND provincia = 'Buenos Aires' AND pais = 'Argentina'), 'Puerto'),
    ((SELECT id FROM ciudad WHERE nombre = 'Mar del Plata' AND provincia = 'Buenos Aires' AND pais = 'Argentina'), 'Punta Mogotes'),
    ((SELECT id FROM ciudad WHERE nombre = 'Mar del Plata' AND provincia = 'Buenos Aires' AND pais = 'Argentina'), 'Los Troncos'),
    ((SELECT id FROM ciudad WHERE nombre = 'Mar del Plata' AND provincia = 'Buenos Aires' AND pais = 'Argentina'), 'Nueva Pompeya'),
    ((SELECT id FROM ciudad WHERE nombre = 'Mar del Plata' AND provincia = 'Buenos Aires' AND pais = 'Argentina'), 'Parque Luro'),
    ((SELECT id FROM ciudad WHERE nombre = 'Mar del Plata' AND provincia = 'Buenos Aires' AND pais = 'Argentina'), 'Zona Norte'),
    ((SELECT id FROM ciudad WHERE nombre = 'Mar del Plata' AND provincia = 'Buenos Aires' AND pais = 'Argentina'), 'Zona Sur');


-- ============================================================
-- CATEGORÍAS DEPORTIVAS
-- ============================================================

INSERT INTO categoria_deportiva (nombre, slug, descripcion, orden)
VALUES
    ('Deportes de combate', 'deportes-de-combate', 'Disciplinas de contacto, combate o defensa personal.', 1),
    ('Artes marciales', 'artes-marciales', 'Disciplinas marciales tradicionales o modernas.', 2),
    ('Fitness y entrenamiento', 'fitness-y-entrenamiento', 'Actividades orientadas al entrenamiento físico general.', 3),
    ('Deportes de equipo', 'deportes-de-equipo', 'Deportes que se practican en equipo.', 4),
    ('Actividades acuáticas', 'actividades-acuaticas', 'Actividades realizadas en pileta, natatorio o espacios acuáticos.', 5),
    ('Bienestar y salud', 'bienestar-y-salud', 'Actividades orientadas al bienestar físico y mental.', 6),
    ('Deportes con raqueta', 'deportes-con-raqueta', 'Deportes que se practican con raqueta o paleta.', 7),
    ('Actividades al aire libre', 'actividades-al-aire-libre', 'Actividades físicas realizadas en espacios abiertos.', 8);


-- ============================================================
-- DEPORTES INICIALES
-- ============================================================

-- Deportes de combate
INSERT INTO deporte (categoria_deportiva_id, nombre, slug, descripcion, orden)
VALUES
    ((SELECT id FROM categoria_deportiva WHERE slug = 'deportes-de-combate'), 'Boxeo', 'boxeo', 'Deporte de combate basado principalmente en golpes de puño.', 1),
    ((SELECT id FROM categoria_deportiva WHERE slug = 'deportes-de-combate'), 'Kickboxing', 'kickboxing', 'Deporte de contacto que combina golpes de puño y patadas.', 2),
    ((SELECT id FROM categoria_deportiva WHERE slug = 'deportes-de-combate'), 'MMA', 'mma', 'Artes marciales mixtas.', 3),
    ((SELECT id FROM categoria_deportiva WHERE slug = 'deportes-de-combate'), 'Muay Thai', 'muay-thai', 'Deporte de combate tailandés con golpes de puño, piernas, codos y rodillas.', 4);


-- Artes marciales
INSERT INTO deporte (categoria_deportiva_id, nombre, slug, descripcion, orden)
VALUES
    ((SELECT id FROM categoria_deportiva WHERE slug = 'artes-marciales'), 'Jiu Jitsu', 'jiu-jitsu', 'Arte marcial enfocada en lucha, control y sumisiones.', 1),
    ((SELECT id FROM categoria_deportiva WHERE slug = 'artes-marciales'), 'Karate', 'karate', 'Arte marcial tradicional basada en golpes, defensas y formas.', 2),
    ((SELECT id FROM categoria_deportiva WHERE slug = 'artes-marciales'), 'Taekwondo', 'taekwondo', 'Arte marcial destacada por sus técnicas de patadas.', 3),
    ((SELECT id FROM categoria_deportiva WHERE slug = 'artes-marciales'), 'Judo', 'judo', 'Arte marcial y deporte olímpico basado en proyecciones y control.', 4);


-- Fitness y entrenamiento
INSERT INTO deporte (categoria_deportiva_id, nombre, slug, descripcion, orden)
VALUES
    ((SELECT id FROM categoria_deportiva WHERE slug = 'fitness-y-entrenamiento'), 'Funcional', 'funcional', 'Entrenamiento físico general con ejercicios variados.', 1),
    ((SELECT id FROM categoria_deportiva WHERE slug = 'fitness-y-entrenamiento'), 'Musculación', 'musculacion', 'Entrenamiento de fuerza en gimnasio.', 2),
    ((SELECT id FROM categoria_deportiva WHERE slug = 'fitness-y-entrenamiento'), 'Cross Training', 'cross-training', 'Entrenamiento intenso y variado de fuerza y resistencia.', 3),
    ((SELECT id FROM categoria_deportiva WHERE slug = 'fitness-y-entrenamiento'), 'Entrenamiento personalizado', 'entrenamiento-personalizado', 'Entrenamiento adaptado a objetivos individuales.', 4);


-- Deportes de equipo
INSERT INTO deporte (categoria_deportiva_id, nombre, slug, descripcion, orden)
VALUES
    ((SELECT id FROM categoria_deportiva WHERE slug = 'deportes-de-equipo'), 'Fútbol', 'futbol', 'Deporte de equipo jugado con pelota.', 1),
    ((SELECT id FROM categoria_deportiva WHERE slug = 'deportes-de-equipo'), 'Básquet', 'basquet', 'Deporte de equipo basado en lanzamiento al aro.', 2),
    ((SELECT id FROM categoria_deportiva WHERE slug = 'deportes-de-equipo'), 'Hockey', 'hockey', 'Deporte de equipo jugado con palo y bocha.', 3),
    ((SELECT id FROM categoria_deportiva WHERE slug = 'deportes-de-equipo'), 'Vóley', 'voley', 'Deporte de equipo jugado con red.', 4);


-- Actividades acuáticas
INSERT INTO deporte (categoria_deportiva_id, nombre, slug, descripcion, orden)
VALUES
    ((SELECT id FROM categoria_deportiva WHERE slug = 'actividades-acuaticas'), 'Natación', 'natacion', 'Actividad acuática de nado y entrenamiento en pileta.', 1),
    ((SELECT id FROM categoria_deportiva WHERE slug = 'actividades-acuaticas'), 'Aqua Gym', 'aqua-gym', 'Actividad física realizada dentro del agua.', 2);


-- Bienestar y salud
INSERT INTO deporte (categoria_deportiva_id, nombre, slug, descripcion, orden)
VALUES
    ((SELECT id FROM categoria_deportiva WHERE slug = 'bienestar-y-salud'), 'Yoga', 'yoga', 'Disciplina física y mental orientada al bienestar.', 1),
    ((SELECT id FROM categoria_deportiva WHERE slug = 'bienestar-y-salud'), 'Pilates', 'pilates', 'Método de entrenamiento enfocado en control, postura y fuerza.', 2),
    ((SELECT id FROM categoria_deportiva WHERE slug = 'bienestar-y-salud'), 'Stretching', 'stretching', 'Actividad orientada a movilidad y elongación.', 3);


-- Deportes con raqueta
INSERT INTO deporte (categoria_deportiva_id, nombre, slug, descripcion, orden)
VALUES
    ((SELECT id FROM categoria_deportiva WHERE slug = 'deportes-con-raqueta'), 'Tenis', 'tenis', 'Deporte con raqueta jugado en cancha dividida por red.', 1),
    ((SELECT id FROM categoria_deportiva WHERE slug = 'deportes-con-raqueta'), 'Pádel', 'padel', 'Deporte de paleta jugado en cancha cerrada.', 2),
    ((SELECT id FROM categoria_deportiva WHERE slug = 'deportes-con-raqueta'), 'Squash', 'squash', 'Deporte de raqueta jugado en cancha cerrada.', 3);


-- Actividades al aire libre
INSERT INTO deporte (categoria_deportiva_id, nombre, slug, descripcion, orden)
VALUES
    ((SELECT id FROM categoria_deportiva WHERE slug = 'actividades-al-aire-libre'), 'Running', 'running', 'Actividad de carrera individual o grupal.', 1),
    ((SELECT id FROM categoria_deportiva WHERE slug = 'actividades-al-aire-libre'), 'Ciclismo', 'ciclismo', 'Actividad deportiva sobre bicicleta.', 2),
    ((SELECT id FROM categoria_deportiva WHERE slug = 'actividades-al-aire-libre'), 'Calistenia', 'calistenia', 'Entrenamiento con peso corporal en barras o espacios abiertos.', 3);