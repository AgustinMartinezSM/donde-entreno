-- ============================================================
-- DondeEntreno - Solicitudes publicas de publicacion
-- Archivo: 05_create_solicitud_publicacion.sql
-- Base de datos: PostgreSQL
-- Descripcion:
-- Este script agrega las tablas necesarias para recibir solicitudes
-- de publicacion desde un formulario publico sin login.
--
-- La solicitud queda pendiente de revision administrativa.
-- Este script NO crea actividades automaticamente.
-- Este script NO modifica tablas existentes.
-- Este script NO inserta datos.
--
-- La migracion completa se ejecuta dentro de una transaccion para
-- que la creacion de tablas, restricciones e indices sea atomica.
-- ============================================================

BEGIN;

-- ============================================================
-- TABLA: solicitud_publicacion
-- ------------------------------------------------------------
-- Guarda solicitudes enviadas por personas, clubes, gimnasios,
-- profesores o instituciones que quieren publicar una actividad.
--
-- La solicitud es independiente de la tabla actividad.
-- Una actividad puede asociarse luego, si el equipo administrativo
-- aprueba y carga la publicacion correspondiente.
-- ============================================================

CREATE TABLE solicitud_publicacion (
    id BIGSERIAL PRIMARY KEY,

    -- Identificacion y flujo
    codigo_seguimiento VARCHAR(40) NOT NULL,

    origen VARCHAR(30) NOT NULL DEFAULT 'FORMULARIO_WEB',

    estado VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE',

    -- Datos del publicador solicitante
    tipo_publicador VARCHAR(50) NOT NULL,

    nombre_publicador VARCHAR(150) NOT NULL,

    -- Datos de la actividad solicitada
    nombre_actividad VARCHAR(150) NOT NULL,

    deporte_id BIGINT,

    deporte_otro VARCHAR(100),

    descripcion TEXT NOT NULL,

    nivel VARCHAR(50) NOT NULL,

    enfoque VARCHAR(50) NOT NULL,

    modalidad VARCHAR(50) NOT NULL,

    edad_minima INTEGER,

    edad_maxima INTEGER,

    precio_referencia NUMERIC(10,2),

    mostrar_precio BOOLEAN NOT NULL DEFAULT false,

    -- Ubicacion propuesta
    ciudad_id BIGINT,

    ciudad_otra VARCHAR(100),

    barrio_id BIGINT,

    barrio_otro VARCHAR(100),

    nombre_lugar VARCHAR(150),

    direccion VARCHAR(255),

    referencia_ubicacion VARCHAR(255),

    -- Datos de contacto
    whatsapp VARCHAR(40),

    whatsapp_normalizado VARCHAR(30),

    instagram VARCHAR(150),

    email VARCHAR(150),

    -- Consentimiento y observaciones
    observaciones_solicitante TEXT,

    acepta_condiciones BOOLEAN NOT NULL,

    -- Revision administrativa
    revisado_por_usuario_id BIGINT,

    actividad_generada_id BIGINT,

    motivo_rechazo TEXT,

    observaciones_revision TEXT,

    revision_iniciada_at TIMESTAMPTZ,

    revision_finalizada_at TIMESTAMPTZ,

    -- Auditoria y baja logica
    ip_origen INET,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Las actualizaciones posteriores de updated_at seran responsabilidad del backend.
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    deleted_at TIMESTAMPTZ,

    -- Un codigo de seguimiento identifica publicamente una solicitud.
    CONSTRAINT uq_solicitud_publicacion_codigo_seguimiento
        UNIQUE (codigo_seguimiento),

    -- Una actividad generada no debe quedar asociada a mas de una solicitud.
    CONSTRAINT uq_solicitud_publicacion_actividad_generada
        UNIQUE (actividad_generada_id),

    -- Relaciones opcionales con tablas existentes.
    -- No borran solicitudes en cascada.
    CONSTRAINT fk_solicitud_publicacion_deporte
        FOREIGN KEY (deporte_id)
        REFERENCES deporte(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_solicitud_publicacion_ciudad
        FOREIGN KEY (ciudad_id)
        REFERENCES ciudad(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_solicitud_publicacion_barrio
        FOREIGN KEY (barrio_id)
        REFERENCES barrio(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_solicitud_publicacion_revisado_por_usuario
        FOREIGN KEY (revisado_por_usuario_id)
        REFERENCES usuario(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_solicitud_publicacion_actividad_generada
        FOREIGN KEY (actividad_generada_id)
        REFERENCES actividad(id)
        ON DELETE RESTRICT,

    -- Valores permitidos para el origen de la solicitud.
    CONSTRAINT chk_solicitud_publicacion_origen
        CHECK (
            origen IN (
                'FORMULARIO_WEB',
                'ADMIN',
                'IMPORTACION'
            )
        ),

    -- Estados propios del flujo de solicitudes.
    CONSTRAINT chk_solicitud_publicacion_estado
        CHECK (
            estado IN (
                'PENDIENTE',
                'EN_REVISION',
                'APROBADA',
                'RECHAZADA'
            )
        ),

    -- Valores compatibles con perfil_publicador.tipo_publicador.
    CONSTRAINT chk_solicitud_publicacion_tipo_publicador
        CHECK (
            tipo_publicador IN (
                'CLUB',
                'GIMNASIO',
                'PROFESOR_INDEPENDIENTE',
                'INSTITUCION',
                'ESCUELA_DEPORTIVA',
                'ESPACIO_ENTRENAMIENTO'
            )
        ),

    -- Valores compatibles con actividad.nivel.
    CONSTRAINT chk_solicitud_publicacion_nivel
        CHECK (
            nivel IN (
                'PRINCIPIANTE',
                'INTERMEDIO',
                'AVANZADO',
                'TODOS'
            )
        ),

    -- Valores compatibles con actividad.enfoque.
    CONSTRAINT chk_solicitud_publicacion_enfoque
        CHECK (
            enfoque IN (
                'RECREATIVO',
                'COMPETITIVO',
                'MIXTO'
            )
        ),

    -- No se permite ONLINE puro porque actividad requiere ubicacion obligatoria.
    CONSTRAINT chk_solicitud_publicacion_modalidad
        CHECK (
            modalidad IN (
                'PRESENCIAL',
                'MIXTA'
            )
        ),

    -- Debe existir exactamente un deporte: uno existente o uno escrito por el solicitante.
    CONSTRAINT chk_solicitud_publicacion_deporte_unico
        CHECK (
            (
                deporte_id IS NOT NULL
                AND deporte_otro IS NULL
            )
            OR
            (
                deporte_id IS NULL
                AND deporte_otro IS NOT NULL
                AND btrim(deporte_otro) <> ''
            )
        ),

    -- Debe existir exactamente una ciudad: una existente o una escrita por el solicitante.
    CONSTRAINT chk_solicitud_publicacion_ciudad_unica
        CHECK (
            (
                ciudad_id IS NOT NULL
                AND ciudad_otra IS NULL
            )
            OR
            (
                ciudad_id IS NULL
                AND ciudad_otra IS NOT NULL
                AND btrim(ciudad_otra) <> ''
            )
        ),

    -- Debe existir exactamente un barrio: uno existente o uno escrito por el solicitante.
    CONSTRAINT chk_solicitud_publicacion_barrio_unico
        CHECK (
            (
                barrio_id IS NOT NULL
                AND barrio_otro IS NULL
            )
            OR
            (
                barrio_id IS NULL
                AND barrio_otro IS NOT NULL
                AND btrim(barrio_otro) <> ''
            )
        ),

    -- Si se usa un barrio existente, tambien debe usarse una ciudad existente.
    CONSTRAINT chk_solicitud_publicacion_barrio_ciudad_existentes
        CHECK (
            barrio_id IS NULL
            OR ciudad_id IS NOT NULL
        ),

    -- Debe informarse al menos nombre del lugar o direccion.
    CONSTRAINT chk_solicitud_publicacion_lugar_o_direccion
        CHECK (
            (
                nombre_lugar IS NOT NULL
                AND btrim(nombre_lugar) <> ''
            )
            OR
            (
                direccion IS NOT NULL
                AND btrim(direccion) <> ''
            )
        ),

    -- Debe informarse al menos WhatsApp o email.
    CONSTRAINT chk_solicitud_publicacion_contacto_minimo
        CHECK (
            (
                whatsapp IS NOT NULL
                AND btrim(whatsapp) <> ''
            )
            OR
            (
                email IS NOT NULL
                AND btrim(email) <> ''
            )
        ),

    -- Si hay WhatsApp original, debe existir normalizado. Si no hay WhatsApp, no debe quedar normalizado.
    CONSTRAINT chk_solicitud_publicacion_whatsapp_normalizado_requerido
        CHECK (
            (
                whatsapp IS NOT NULL
                AND btrim(whatsapp) <> ''
                AND whatsapp_normalizado IS NOT NULL
                AND btrim(whatsapp_normalizado) <> ''
            )
            OR
            (
                whatsapp IS NULL
                AND whatsapp_normalizado IS NULL
            )
        ),

    -- El WhatsApp normalizado debe contener solo digitos.
    CONSTRAINT chk_solicitud_publicacion_whatsapp_normalizado_formato
        CHECK (
            whatsapp_normalizado IS NULL
            OR whatsapp_normalizado ~ '^[0-9]+$'
        ),

    -- Las edades no pueden ser negativas.
    CONSTRAINT chk_solicitud_publicacion_edad_minima
        CHECK (edad_minima IS NULL OR edad_minima >= 0),

    CONSTRAINT chk_solicitud_publicacion_edad_maxima
        CHECK (edad_maxima IS NULL OR edad_maxima >= 0),

    -- Si ambas edades existen, la minima no puede superar a la maxima.
    CONSTRAINT chk_solicitud_publicacion_rango_edad
        CHECK (
            edad_minima IS NULL
            OR edad_maxima IS NULL
            OR edad_minima <= edad_maxima
        ),

    -- El precio no puede ser negativo.
    CONSTRAINT chk_solicitud_publicacion_precio_referencia
        CHECK (precio_referencia IS NULL OR precio_referencia >= 0),

    -- Si se pide mostrar precio, debe existir un precio informado.
    CONSTRAINT chk_solicitud_publicacion_mostrar_precio
        CHECK (
            mostrar_precio = false
            OR precio_referencia IS NOT NULL
        ),

    -- La aceptacion de condiciones es obligatoria.
    CONSTRAINT chk_solicitud_publicacion_acepta_condiciones
        CHECK (acepta_condiciones = true),

    -- Si se rechaza, debe haber motivo. Si no se rechaza, el motivo debe quedar vacio.
    CONSTRAINT chk_solicitud_publicacion_motivo_rechazo
        CHECK (
            (
                estado = 'RECHAZADA'
                AND motivo_rechazo IS NOT NULL
                AND btrim(motivo_rechazo) <> ''
            )
            OR
            (
                estado <> 'RECHAZADA'
                AND motivo_rechazo IS NULL
            )
        ),

    -- La fecha de finalizacion de revision no puede ser anterior a la fecha de inicio.
    CONSTRAINT chk_solicitud_publicacion_fechas_revision
        CHECK (
            revision_iniciada_at IS NULL
            OR revision_finalizada_at IS NULL
            OR revision_finalizada_at >= revision_iniciada_at
        ),

    -- Textos obligatorios: no pueden contener solo espacios.
    CONSTRAINT chk_solicitud_publicacion_textos_obligatorios
        CHECK (
            btrim(codigo_seguimiento) <> ''
            AND btrim(origen) <> ''
            AND btrim(estado) <> ''
            AND btrim(tipo_publicador) <> ''
            AND btrim(nombre_publicador) <> ''
            AND btrim(nombre_actividad) <> ''
            AND btrim(descripcion) <> ''
            AND btrim(nivel) <> ''
            AND btrim(enfoque) <> ''
            AND btrim(modalidad) <> ''
        ),

    -- Textos opcionales: si se informan, no pueden contener solo espacios.
    CONSTRAINT chk_solicitud_publicacion_textos_opcionales
        CHECK (
            (deporte_otro IS NULL OR btrim(deporte_otro) <> '')
            AND (ciudad_otra IS NULL OR btrim(ciudad_otra) <> '')
            AND (barrio_otro IS NULL OR btrim(barrio_otro) <> '')
            AND (nombre_lugar IS NULL OR btrim(nombre_lugar) <> '')
            AND (direccion IS NULL OR btrim(direccion) <> '')
            AND (referencia_ubicacion IS NULL OR btrim(referencia_ubicacion) <> '')
            AND (whatsapp IS NULL OR btrim(whatsapp) <> '')
            AND (whatsapp_normalizado IS NULL OR btrim(whatsapp_normalizado) <> '')
            AND (instagram IS NULL OR btrim(instagram) <> '')
            AND (email IS NULL OR btrim(email) <> '')
            AND (observaciones_solicitante IS NULL OR btrim(observaciones_solicitante) <> '')
            AND (observaciones_revision IS NULL OR btrim(observaciones_revision) <> '')
        )
);


-- ============================================================
-- TABLA: solicitud_publicacion_horario
-- ------------------------------------------------------------
-- Guarda los horarios estructurados informados para una solicitud.
-- Los horarios se eliminan junto con su solicitud.
-- No tienen baja logica propia.
-- ============================================================

CREATE TABLE solicitud_publicacion_horario (
    id BIGSERIAL PRIMARY KEY,

    solicitud_publicacion_id BIGINT NOT NULL,

    dia_semana VARCHAR(20) NOT NULL,

    hora_inicio TIME NOT NULL,

    hora_fin TIME NOT NULL,

    observacion VARCHAR(255),

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Las actualizaciones posteriores de updated_at seran responsabilidad del backend.
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Relacion con la solicitud principal.
    -- Es la unica relacion de esta migracion que borra en cascada.
    CONSTRAINT fk_solicitud_publicacion_horario_solicitud_publicacion
        FOREIGN KEY (solicitud_publicacion_id)
        REFERENCES solicitud_publicacion(id)
        ON DELETE CASCADE,

    -- Dias permitidos, compatibles con horario_actividad.
    CONSTRAINT chk_solicitud_publicacion_horario_dia_semana
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

    -- No se contemplan horarios que crucen medianoche.
    CONSTRAINT chk_solicitud_publicacion_horario_rango_horario
        CHECK (hora_inicio < hora_fin),

    -- Evita cargar dos veces el mismo horario exacto para la misma solicitud.
    CONSTRAINT uq_solicitud_publicacion_horario_unico
        UNIQUE (
            solicitud_publicacion_id,
            dia_semana,
            hora_inicio,
            hora_fin
        ),

    -- La observacion opcional no puede contener solo espacios.
    CONSTRAINT chk_solicitud_publicacion_horario_observacion
        CHECK (
            observacion IS NULL
            OR btrim(observacion) <> ''
        )
);


-- ============================================================
-- INDICES: solicitud_publicacion
-- ============================================================

-- Permite ordenar y auditar solicitudes por fecha de creacion.
CREATE INDEX idx_solicitud_publicacion_created_at
ON solicitud_publicacion (created_at);

-- Indice util para bandejas administrativas: pendientes primero y orden temporal.
-- Tambien cubre consultas que filtran por estado como primera columna.
CREATE INDEX idx_solicitud_publicacion_estado_created_at
ON solicitud_publicacion (estado, created_at);

-- Permite filtrar solicitudes asociadas a un deporte existente.
CREATE INDEX idx_solicitud_publicacion_deporte_id
ON solicitud_publicacion (deporte_id);

-- Permite filtrar solicitudes asociadas a una ciudad existente.
CREATE INDEX idx_solicitud_publicacion_ciudad_id
ON solicitud_publicacion (ciudad_id);

-- Permite filtrar solicitudes asociadas a un barrio existente.
CREATE INDEX idx_solicitud_publicacion_barrio_id
ON solicitud_publicacion (barrio_id);

-- Permite consultar solicitudes tomadas o revisadas por un usuario.
CREATE INDEX idx_solicitud_publicacion_revisado_por_usuario_id
ON solicitud_publicacion (revisado_por_usuario_id);

-- Permite buscar solicitudes por WhatsApp normalizado sin exigir unicidad.
CREATE INDEX idx_solicitud_publicacion_whatsapp_normalizado
ON solicitud_publicacion (whatsapp_normalizado);

-- Permite buscar solicitudes por email sin exigir unicidad.
CREATE INDEX idx_solicitud_publicacion_email
ON solicitud_publicacion (email);

-- Permite separar registros activos de solicitudes con baja logica.
CREATE INDEX idx_solicitud_publicacion_deleted_at
ON solicitud_publicacion (deleted_at);


-- ============================================================
-- INDICES: solicitud_publicacion_horario
-- ============================================================

-- Permite filtrar o analizar solicitudes por dia y hora de inicio.
CREATE INDEX idx_solicitud_publicacion_horario_dia_hora
ON solicitud_publicacion_horario (dia_semana, hora_inicio);

COMMIT;
