-- ============================================================
-- DondeEntreno - Script inicial de creación de tablas
-- Archivo: 01_create_tables.sql
-- Base de datos: PostgreSQL
-- Descripción:
-- Este script crea las tablas principales del MVP.
-- En este primer bloque creamos las tablas base que no dependen
-- de otras tablas: rol, ciudad y categoria_deportiva.
-- ============================================================


-- ============================================================
-- TABLA: rol
-- ------------------------------------------------------------
-- Guarda los roles del sistema.
-- Ejemplos:
-- SUPER_ADMIN: control total del sistema.
-- ADMIN: puede revisar y administrar publicaciones.
-- PUBLICADOR: puede crear perfiles y actividades.
-- ============================================================

CREATE TABLE rol (
    id BIGSERIAL PRIMARY KEY,

    nombre VARCHAR(50) NOT NULL UNIQUE,

    descripcion VARCHAR(255),

    activo BOOLEAN NOT NULL DEFAULT true,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    -- Validamos que solo existan los roles permitidos.
    CONSTRAINT chk_rol_nombre
        CHECK (nombre IN ('SUPER_ADMIN', 'ADMIN', 'PUBLICADOR'))
);


-- ============================================================
-- TABLA: ciudad
-- ------------------------------------------------------------
-- Guarda las ciudades donde DondeEntreno tendrá actividades.
-- Aunque el MVP arranque en Mar del Plata, dejamos la estructura
-- lista para sumar más ciudades en el futuro.
-- ============================================================

CREATE TABLE ciudad (
    id BIGSERIAL PRIMARY KEY,

    nombre VARCHAR(100) NOT NULL,

    provincia VARCHAR(100) NOT NULL,

    pais VARCHAR(100) NOT NULL,

    activa BOOLEAN NOT NULL DEFAULT true,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    -- Evita cargar dos veces la misma ciudad en la misma provincia y país.
    CONSTRAINT uq_ciudad_nombre_provincia_pais
        UNIQUE (nombre, provincia, pais)
);


-- ============================================================
-- TABLA: categoria_deportiva
-- ------------------------------------------------------------
-- Guarda grupos grandes de deportes.
-- Ejemplos:
-- Deportes de combate, Fitness y entrenamiento,
-- Deportes de equipo, Actividades acuáticas.
-- ============================================================

CREATE TABLE categoria_deportiva (
    id BIGSERIAL PRIMARY KEY,

    nombre VARCHAR(100) NOT NULL UNIQUE,

    slug VARCHAR(120) NOT NULL UNIQUE,

    descripcion TEXT,

    icono_url VARCHAR(255),

    activa BOOLEAN NOT NULL DEFAULT true,

    orden INTEGER NOT NULL DEFAULT 0,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ============================================================
-- TABLA: usuario
-- ------------------------------------------------------------
-- Guarda las cuentas que pueden iniciar sesión en el sistema.
-- No representa al visitante común, porque el visitante puede
-- buscar actividades sin registrarse.
--
-- Un usuario pertenece a un rol:
-- SUPER_ADMIN, ADMIN o PUBLICADOR.
-- ============================================================

CREATE TABLE usuario (
    id BIGSERIAL PRIMARY KEY,

    rol_id BIGINT NOT NULL,

    nombre VARCHAR(100) NOT NULL,

    apellido VARCHAR(100),

    email VARCHAR(150) NOT NULL UNIQUE,

    password_hash VARCHAR(255) NOT NULL,

    telefono VARCHAR(30),

    activo BOOLEAN NOT NULL DEFAULT true,

    email_verificado BOOLEAN NOT NULL DEFAULT false,

    ultimo_login_at TIMESTAMPTZ,

    deleted_at TIMESTAMPTZ,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    -- Relación con la tabla rol.
    CONSTRAINT fk_usuario_rol
        FOREIGN KEY (rol_id)
        REFERENCES rol(id)
);


-- ============================================================
-- TABLA: barrio
-- ------------------------------------------------------------
-- Guarda barrios o zonas dentro de una ciudad.
-- Ejemplos para Mar del Plata:
-- Centro, La Perla, Güemes, Puerto, Constitución.
-- ============================================================

CREATE TABLE barrio (
    id BIGSERIAL PRIMARY KEY,

    ciudad_id BIGINT NOT NULL,

    nombre VARCHAR(100) NOT NULL,

    activo BOOLEAN NOT NULL DEFAULT true,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    -- Relación con la tabla ciudad.
    CONSTRAINT fk_barrio_ciudad
        FOREIGN KEY (ciudad_id)
        REFERENCES ciudad(id),

    -- Evita repetir el mismo barrio dentro de la misma ciudad.
    CONSTRAINT uq_barrio_ciudad_nombre
        UNIQUE (ciudad_id, nombre)
);


-- ============================================================
-- TABLA: deporte
-- ------------------------------------------------------------
-- Guarda las disciplinas deportivas concretas.
-- Ejemplos:
-- Boxeo, Fútbol, Jiu Jitsu, Yoga, Natación, Funcional.
--
-- Cada deporte pertenece a una categoría deportiva principal.
-- ============================================================

CREATE TABLE deporte (
    id BIGSERIAL PRIMARY KEY,

    categoria_deportiva_id BIGINT NOT NULL,

    nombre VARCHAR(100) NOT NULL,

    slug VARCHAR(120) NOT NULL UNIQUE,

    descripcion TEXT,

    icono_url VARCHAR(255),

    activo BOOLEAN NOT NULL DEFAULT true,

    orden INTEGER NOT NULL DEFAULT 0,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    -- Relación con la tabla categoria_deportiva.
    CONSTRAINT fk_deporte_categoria_deportiva
        FOREIGN KEY (categoria_deportiva_id)
        REFERENCES categoria_deportiva(id),

    -- Evita repetir el mismo deporte dentro de la misma categoría.
    CONSTRAINT uq_deporte_categoria_nombre
        UNIQUE (categoria_deportiva_id, nombre)
);

-- ============================================================
-- TABLA: perfil_publicador
-- ------------------------------------------------------------
-- Guarda el perfil visible de quien publica actividades.
--
-- Puede representar:
-- - Club
-- - Gimnasio
-- - Profesor independiente
-- - Institución
-- - Escuela deportiva
-- - Espacio de entrenamiento
--
-- Importante:
-- usuario = cuenta de acceso/login.
-- perfil_publicador = información pública visible en la plataforma.
-- ============================================================

CREATE TABLE perfil_publicador (
    id BIGSERIAL PRIMARY KEY,

    usuario_id BIGINT NOT NULL,

    nombre VARCHAR(150) NOT NULL,

    tipo_publicador VARCHAR(50) NOT NULL,

    descripcion TEXT,

    email_contacto VARCHAR(150),

    telefono_contacto VARCHAR(30),

    whatsapp VARCHAR(30),

    instagram VARCHAR(150),

    sitio_web VARCHAR(255),

    activo BOOLEAN NOT NULL DEFAULT true,

    verificado BOOLEAN NOT NULL DEFAULT false,

    deleted_at TIMESTAMPTZ,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    -- Relación con la tabla usuario.
    CONSTRAINT fk_perfil_publicador_usuario
        FOREIGN KEY (usuario_id)
        REFERENCES usuario(id),

    -- Validamos los tipos de publicador permitidos.
    CONSTRAINT chk_perfil_publicador_tipo
        CHECK (
            tipo_publicador IN (
                'CLUB',
                'GIMNASIO',
                'PROFESOR_INDEPENDIENTE',
                'INSTITUCION',
                'ESCUELA_DEPORTIVA',
                'ESPACIO_ENTRENAMIENTO'
            )
        )
);

-- ============================================================
-- TABLA: ubicacion
-- ------------------------------------------------------------
-- Guarda los lugares físicos donde se realizan las actividades.
--
-- Ejemplos:
-- - Sede principal de un club
-- - Gimnasio
-- - Plaza
-- - Polideportivo
-- - Cancha auxiliar
-- - Estudio privado
--
-- Una ubicación pertenece a un perfil publicador, a una ciudad
-- y a un barrio o zona.
-- ============================================================

CREATE TABLE ubicacion (
    id BIGSERIAL PRIMARY KEY,

    perfil_publicador_id BIGINT NOT NULL,

    ciudad_id BIGINT NOT NULL,

    barrio_id BIGINT NOT NULL,

    nombre VARCHAR(150) NOT NULL,

    direccion VARCHAR(255) NOT NULL,

    referencia VARCHAR(255),

    latitud NUMERIC(9,6),

    longitud NUMERIC(9,6),

    google_maps_url VARCHAR(500),

    activa BOOLEAN NOT NULL DEFAULT true,

    deleted_at TIMESTAMPTZ,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    -- Relación con el perfil publicador dueño de esta ubicación.
    CONSTRAINT fk_ubicacion_perfil_publicador
        FOREIGN KEY (perfil_publicador_id)
        REFERENCES perfil_publicador(id),

    -- Relación con la ciudad donde está la ubicación.
    CONSTRAINT fk_ubicacion_ciudad
        FOREIGN KEY (ciudad_id)
        REFERENCES ciudad(id),

    -- Relación con el barrio o zona donde está la ubicación.
    CONSTRAINT fk_ubicacion_barrio
        FOREIGN KEY (barrio_id)
        REFERENCES barrio(id),

    -- Validamos que, si se carga latitud, esté en un rango posible.
    CONSTRAINT chk_ubicacion_latitud
        CHECK (latitud IS NULL OR (latitud >= -90 AND latitud <= 90)),

    -- Validamos que, si se carga longitud, esté en un rango posible.
    CONSTRAINT chk_ubicacion_longitud
        CHECK (longitud IS NULL OR (longitud >= -180 AND longitud <= 180))
);

-- ============================================================
-- TABLA: actividad
-- ------------------------------------------------------------
-- Guarda las actividades concretas que el visitante puede buscar.
--
-- Ejemplos:
-- - Boxeo recreativo para adultos
-- - Jiu Jitsu para principiantes
-- - Fútbol infantil categoría 2014
-- - Yoga inicial
-- - Funcional femenino
--
-- Una actividad pertenece a:
-- - un perfil publicador
-- - un deporte
-- - una ubicación
-- ============================================================

CREATE TABLE actividad (
    id BIGSERIAL PRIMARY KEY,

    perfil_publicador_id BIGINT NOT NULL,

    deporte_id BIGINT NOT NULL,

    ubicacion_id BIGINT NOT NULL,

    titulo VARCHAR(150) NOT NULL,

    slug VARCHAR(180) NOT NULL UNIQUE,

    descripcion TEXT,

    edad_minima INTEGER,

    edad_maxima INTEGER,

    nivel VARCHAR(50) NOT NULL,

    enfoque VARCHAR(50) NOT NULL,

    modalidad VARCHAR(50) NOT NULL,

    precio_referencia NUMERIC(10,2),

    mostrar_precio BOOLEAN NOT NULL DEFAULT false,

    requiere_inscripcion BOOLEAN NOT NULL DEFAULT true,

    cupos_limitados BOOLEAN NOT NULL DEFAULT false,

    whatsapp_contacto VARCHAR(30),

    instagram_contacto VARCHAR(150),

    email_contacto VARCHAR(150),

    estado_publicacion VARCHAR(50) NOT NULL DEFAULT 'BORRADOR',

    motivo_rechazo TEXT,

    activa BOOLEAN NOT NULL DEFAULT true,

    deleted_at TIMESTAMPTZ,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    -- Relación con el perfil publicador que ofrece la actividad.
    CONSTRAINT fk_actividad_perfil_publicador
        FOREIGN KEY (perfil_publicador_id)
        REFERENCES perfil_publicador(id),

    -- Relación con el deporte principal de la actividad.
    CONSTRAINT fk_actividad_deporte
        FOREIGN KEY (deporte_id)
        REFERENCES deporte(id),

    -- Relación con la ubicación donde se realiza la actividad.
    CONSTRAINT fk_actividad_ubicacion
        FOREIGN KEY (ubicacion_id)
        REFERENCES ubicacion(id),

    -- Validamos los niveles permitidos.
    CONSTRAINT chk_actividad_nivel
        CHECK (
            nivel IN (
                'PRINCIPIANTE',
                'INTERMEDIO',
                'AVANZADO',
                'TODOS'
            )
        ),

    -- Validamos los enfoques permitidos.
    CONSTRAINT chk_actividad_enfoque
        CHECK (
            enfoque IN (
                'RECREATIVO',
                'COMPETITIVO',
                'MIXTO'
            )
        ),

    -- Validamos las modalidades permitidas.
    CONSTRAINT chk_actividad_modalidad
        CHECK (
            modalidad IN (
                'PRESENCIAL',
                'ONLINE',
                'MIXTA'
            )
        ),

    -- Validamos los estados de publicación permitidos.
    CONSTRAINT chk_actividad_estado_publicacion
        CHECK (
            estado_publicacion IN (
                'BORRADOR',
                'PENDIENTE_REVISION',
                'PUBLICADA',
                'PAUSADA',
                'RECHAZADA'
            )
        ),

    -- La edad mínima, si se carga, no puede ser negativa.
    CONSTRAINT chk_actividad_edad_minima
        CHECK (edad_minima IS NULL OR edad_minima >= 0),

    -- La edad máxima, si se carga, no puede ser negativa.
    CONSTRAINT chk_actividad_edad_maxima
        CHECK (edad_maxima IS NULL OR edad_maxima >= 0),

    -- Si existen edad mínima y edad máxima, la mínima no puede ser mayor a la máxima.
    CONSTRAINT chk_actividad_rango_edad
        CHECK (
            edad_minima IS NULL
            OR edad_maxima IS NULL
            OR edad_minima <= edad_maxima
        ),

    -- El precio, si se carga, no puede ser negativo.
    CONSTRAINT chk_actividad_precio_referencia
        CHECK (precio_referencia IS NULL OR precio_referencia >= 0)
);

-- ============================================================
-- TABLA: horario_actividad
-- ------------------------------------------------------------
-- Guarda los días y horarios en los que se realiza una actividad.
--
-- Ejemplos:
-- - Lunes 18:00 a 19:30
-- - Miércoles 18:00 a 19:30
-- - Viernes 19:00 a 20:30
--
-- Importante:
-- No guardamos los horarios como texto libre porque después
-- necesitamos filtrar por día, hora, mañana, tarde o noche.
-- ============================================================

CREATE TABLE horario_actividad (
    id BIGSERIAL PRIMARY KEY,

    actividad_id BIGINT NOT NULL,

    dia_semana VARCHAR(20) NOT NULL,

    hora_inicio TIME NOT NULL,

    hora_fin TIME NOT NULL,

    observacion VARCHAR(255),

    activo BOOLEAN NOT NULL DEFAULT true,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    -- Relación con la actividad a la que pertenece este horario.
    CONSTRAINT fk_horario_actividad_actividad
        FOREIGN KEY (actividad_id)
        REFERENCES actividad(id),

    -- Validamos los días permitidos.
    CONSTRAINT chk_horario_actividad_dia_semana
        CHECK (
            dia_semana IN (
                'LUNES',
                'MARTES',
                'MIERCOLES',
                'JUEVES',
                'VIERNES',
                'SABADO',
                'DOMINGO'
            )
        ),

    -- La hora de inicio debe ser menor a la hora de fin.
    -- Para el MVP no contemplamos actividades que crucen medianoche.
    CONSTRAINT chk_horario_actividad_rango_horario
        CHECK (hora_inicio < hora_fin),

    -- Evita cargar dos veces exactamente el mismo horario
    -- para la misma actividad.
    CONSTRAINT uq_horario_actividad_unico
        UNIQUE (actividad_id, dia_semana, hora_inicio, hora_fin)
);

-- ============================================================
-- TABLA: imagen
-- ------------------------------------------------------------
-- Guarda imágenes relacionadas a perfiles publicadores o
-- actividades.
--
-- Ejemplos:
-- - Logo de un club
-- - Portada de un gimnasio
-- - Imagen principal de una actividad
-- - Galería de fotos de una clase
--
-- Importante:
-- No guardamos el archivo binario dentro de PostgreSQL.
-- Guardamos solamente la URL o ruta del archivo.
-- ============================================================

CREATE TABLE imagen (
    id BIGSERIAL PRIMARY KEY,

    perfil_publicador_id BIGINT,

    actividad_id BIGINT,

    url VARCHAR(500) NOT NULL,

    tipo_imagen VARCHAR(50) NOT NULL,

    titulo VARCHAR(150),

    descripcion VARCHAR(255),

    orden INTEGER NOT NULL DEFAULT 0,

    activa BOOLEAN NOT NULL DEFAULT true,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    -- Relación opcional con perfil_publicador.
    CONSTRAINT fk_imagen_perfil_publicador
        FOREIGN KEY (perfil_publicador_id)
        REFERENCES perfil_publicador(id),

    -- Relación opcional con actividad.
    CONSTRAINT fk_imagen_actividad
        FOREIGN KEY (actividad_id)
        REFERENCES actividad(id),

    -- Validamos los tipos de imagen permitidos.
    CONSTRAINT chk_imagen_tipo
        CHECK (
            tipo_imagen IN (
                'LOGO',
                'PORTADA',
                'PRINCIPAL',
                'GALERIA'
            )
        ),

    -- Regla clave:
    -- Una imagen debe pertenecer a un perfil publicador
    -- o a una actividad, pero no a ambos al mismo tiempo.
    CONSTRAINT chk_imagen_duenio_unico
        CHECK (
            (
                perfil_publicador_id IS NOT NULL
                AND actividad_id IS NULL
            )
            OR
            (
                perfil_publicador_id IS NULL
                AND actividad_id IS NOT NULL
            )
        )
);

-- ============================================================
-- ÍNDICES RECOMENDADOS
-- ------------------------------------------------------------
-- Estos índices ayudan a mejorar las búsquedas y filtros más
-- comunes del MVP.
--
-- Importante:
-- No reemplazan las claves primarias ni las restricciones UNIQUE.
-- Son una ayuda extra para consultas frecuentes.
-- ============================================================


-- ============================================================
-- ÍNDICES: usuario
-- ============================================================

-- Permite buscar usuarios rápidamente por rol.
CREATE INDEX idx_usuario_rol_id
ON usuario (rol_id);

-- Permite filtrar usuarios activos.
CREATE INDEX idx_usuario_activo
ON usuario (activo);


-- ============================================================
-- ÍNDICES: perfil_publicador
-- ============================================================

-- Permite buscar perfiles pertenecientes a un usuario.
CREATE INDEX idx_perfil_publicador_usuario_id
ON perfil_publicador (usuario_id);

-- Permite filtrar perfiles por tipo: CLUB, GIMNASIO, etc.
CREATE INDEX idx_perfil_publicador_tipo
ON perfil_publicador (tipo_publicador);

-- Permite filtrar perfiles activos.
CREATE INDEX idx_perfil_publicador_activo
ON perfil_publicador (activo);


-- ============================================================
-- ÍNDICES: ciudad
-- ============================================================

-- Permite filtrar ciudades activas.
CREATE INDEX idx_ciudad_activa
ON ciudad (activa);


-- ============================================================
-- ÍNDICES: barrio
-- ============================================================

-- Permite buscar barrios de una ciudad.
CREATE INDEX idx_barrio_ciudad_id
ON barrio (ciudad_id);

-- Permite filtrar barrios activos.
CREATE INDEX idx_barrio_activo
ON barrio (activo);


-- ============================================================
-- ÍNDICES: ubicacion
-- ============================================================

-- Permite buscar ubicaciones de un perfil publicador.
CREATE INDEX idx_ubicacion_perfil_publicador_id
ON ubicacion (perfil_publicador_id);

-- Permite filtrar ubicaciones por ciudad.
CREATE INDEX idx_ubicacion_ciudad_id
ON ubicacion (ciudad_id);

-- Permite filtrar ubicaciones por barrio.
CREATE INDEX idx_ubicacion_barrio_id
ON ubicacion (barrio_id);

-- Permite buscar por ciudad y barrio juntos.
CREATE INDEX idx_ubicacion_ciudad_barrio
ON ubicacion (ciudad_id, barrio_id);

-- Permite filtrar ubicaciones activas.
CREATE INDEX idx_ubicacion_activa
ON ubicacion (activa);


-- ============================================================
-- ÍNDICES: categoria_deportiva
-- ============================================================

-- Permite filtrar categorías activas.
CREATE INDEX idx_categoria_deportiva_activa
ON categoria_deportiva (activa);

-- Permite ordenar categorías en pantalla.
CREATE INDEX idx_categoria_deportiva_orden
ON categoria_deportiva (orden);


-- ============================================================
-- ÍNDICES: deporte
-- ============================================================

-- Permite buscar deportes por categoría.
CREATE INDEX idx_deporte_categoria_deportiva_id
ON deporte (categoria_deportiva_id);

-- Permite filtrar deportes activos.
CREATE INDEX idx_deporte_activo
ON deporte (activo);

-- Permite ordenar deportes dentro de una categoría.
CREATE INDEX idx_deporte_orden
ON deporte (orden);


-- ============================================================
-- ÍNDICES: actividad
-- ============================================================

-- Permite buscar actividades de un perfil publicador.
CREATE INDEX idx_actividad_perfil_publicador_id
ON actividad (perfil_publicador_id);

-- Permite filtrar actividades por deporte.
CREATE INDEX idx_actividad_deporte_id
ON actividad (deporte_id);

-- Permite filtrar actividades por ubicación.
CREATE INDEX idx_actividad_ubicacion_id
ON actividad (ubicacion_id);

-- Permite buscar actividades por estado.
CREATE INDEX idx_actividad_estado_publicacion
ON actividad (estado_publicacion);

-- Permite filtrar actividades activas.
CREATE INDEX idx_actividad_activa
ON actividad (activa);

-- Índice útil para búsquedas públicas:
-- actividades publicadas y activas.
CREATE INDEX idx_actividad_estado_activa
ON actividad (estado_publicacion, activa);

-- Permite filtrar por nivel.
CREATE INDEX idx_actividad_nivel
ON actividad (nivel);

-- Permite filtrar por enfoque.
CREATE INDEX idx_actividad_enfoque
ON actividad (enfoque);

-- Permite filtrar por modalidad.
CREATE INDEX idx_actividad_modalidad
ON actividad (modalidad);


-- ============================================================
-- ÍNDICES: horario_actividad
-- ============================================================

-- Permite buscar horarios de una actividad.
CREATE INDEX idx_horario_actividad_actividad_id
ON horario_actividad (actividad_id);

-- Permite filtrar por día de la semana.
CREATE INDEX idx_horario_actividad_dia_semana
ON horario_actividad (dia_semana);

-- Permite filtrar por hora de inicio.
CREATE INDEX idx_horario_actividad_hora_inicio
ON horario_actividad (hora_inicio);

-- Índice útil para buscar actividades por día y horario.
CREATE INDEX idx_horario_actividad_dia_hora
ON horario_actividad (dia_semana, hora_inicio);

-- Permite filtrar horarios activos.
CREATE INDEX idx_horario_actividad_activo
ON horario_actividad (activo);


-- ============================================================
-- ÍNDICES: imagen
-- ============================================================

-- Permite buscar imágenes de un perfil publicador.
CREATE INDEX idx_imagen_perfil_publicador_id
ON imagen (perfil_publicador_id);

-- Permite buscar imágenes de una actividad.
CREATE INDEX idx_imagen_actividad_id
ON imagen (actividad_id);

-- Permite filtrar por tipo de imagen.
CREATE INDEX idx_imagen_tipo_imagen
ON imagen (tipo_imagen);

-- Permite filtrar imágenes activas.
CREATE INDEX idx_imagen_activa
ON imagen (activa);

-- Permite buscar logo, portada o galería activa de un perfil.
CREATE INDEX idx_imagen_perfil_tipo_activa
ON imagen (perfil_publicador_id, tipo_imagen, activa);

-- Permite buscar imagen principal o galería activa de una actividad.
CREATE INDEX idx_imagen_actividad_tipo_activa
ON imagen (actividad_id, tipo_imagen, activa);


