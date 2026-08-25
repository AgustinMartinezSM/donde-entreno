-- ============================================================
-- Script 36 — Inbox de consultas usuario ↔ publicador
-- ============================================================
-- Aditiva salvo el CHECK de reporte, que hay que reescribir porque
-- ENUMERA valores (mismo costo conocido de los scripts 29, 30, 34 y 35).
-- Plan: docs/plan-inbox-consultas.md
--
-- Por qué tablas nuevas y no `pregunta_actividad`: esa es PÚBLICA y de
-- un solo ida y vuelta (un campo pregunta y un campo respuesta). Es la
-- vidriera; esto es privado y va y viene. Mezclarlas obligaría a que
-- una misma tabla tenga dos reglas de visibilidad opuestas.
--
-- PRE:
--   SELECT COUNT(*) FROM information_schema.tables
--    WHERE table_name IN ('conversacion','mensaje');
--   -- esperado: 0
--   SELECT pg_get_constraintdef(oid) FROM pg_constraint
--    WHERE conname = 'chk_reporte_tipo_objeto';
--   -- esperado: la lista de 8 tipos (sin MENSAJE)

BEGIN;

-- ============================================================
-- La conversación: un usuario con un publicador, opcionalmente sobre
-- una actividad concreta (para que el publicador sepa de qué le
-- hablan).
--
-- La INICIA siempre el usuario: que el publicador no pueda escribir en
-- frío es regla de producto y se valida en el service, pero queda
-- dicho acá porque explica por qué no hay un "iniciada_por".
-- ============================================================
CREATE TABLE conversacion (
    id BIGSERIAL PRIMARY KEY,

    usuario_id BIGINT NOT NULL,
    perfil_publicador_id BIGINT NOT NULL,

    -- Opcional: se puede consultar por el club en general.
    actividad_id BIGINT NULL,

    -- CERRADA_POR_USUARIO: deja de notificar y el publicador no puede
    -- seguir escribiendo. Solo el usuario puede cerrar.
    estado VARCHAR(30) NOT NULL DEFAULT 'ABIERTA',

    -- Para ordenar la bandeja sin mirar la tabla de mensajes.
    ultimo_mensaje_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_conversacion_usuario
        FOREIGN KEY (usuario_id)
        REFERENCES usuario (id)
        ON DELETE CASCADE,

    CONSTRAINT fk_conversacion_perfil
        FOREIGN KEY (perfil_publicador_id)
        REFERENCES perfil_publicador (id)
        ON DELETE CASCADE,

    CONSTRAINT fk_conversacion_actividad
        FOREIGN KEY (actividad_id)
        REFERENCES actividad (id)
        ON DELETE SET NULL,

    CONSTRAINT chk_conversacion_estado
        CHECK (estado IN ('ABIERTA', 'CERRADA_POR_USUARIO'))
);

-- ============================================================
-- Una sola conversación por (usuario, publicador, actividad).
--
-- Van DOS índices únicos parciales y no un UNIQUE normal por una razón
-- concreta de Postgres: en un UNIQUE los NULL **no colisionan entre
-- sí**, así que `(5, 8, NULL)` podría insertarse infinitas veces y el
-- usuario terminaría con veinte hilos abiertos con el mismo club.
-- ============================================================
CREATE UNIQUE INDEX uq_conversacion_con_actividad
    ON conversacion (usuario_id, perfil_publicador_id, actividad_id)
    WHERE actividad_id IS NOT NULL;

CREATE UNIQUE INDEX uq_conversacion_sin_actividad
    ON conversacion (usuario_id, perfil_publicador_id)
    WHERE actividad_id IS NULL;

-- Las dos bandejas: la del usuario y la del publicador.
CREATE INDEX idx_conversacion_usuario
    ON conversacion (usuario_id, ultimo_mensaje_at DESC);

CREATE INDEX idx_conversacion_perfil
    ON conversacion (perfil_publicador_id, ultimo_mensaje_at DESC);

-- ============================================================
-- El mensaje.
--
-- `autor` es un rol y no un usuario_id: del lado del publicador quien
-- responde es el perfil, no la persona, y así el hilo no cambia de
-- firma si mañana el perfil lo maneja otra cuenta.
-- ============================================================
CREATE TABLE mensaje (
    id BIGSERIAL PRIMARY KEY,

    conversacion_id BIGINT NOT NULL,

    autor VARCHAR(20) NOT NULL,

    texto VARCHAR(2000) NOT NULL,

    -- Se guarda para el contador de no leídos. La HORA no se muestra
    -- al otro lado: un "visto a las 14:32" crea una expectativa de
    -- respuesta inmediata que un club no puede cumplir.
    leido_at TIMESTAMPTZ NULL,

    estado VARCHAR(30) NOT NULL DEFAULT 'VISIBLE',

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_mensaje_conversacion
        FOREIGN KEY (conversacion_id)
        REFERENCES conversacion (id)
        ON DELETE CASCADE,

    CONSTRAINT chk_mensaje_autor
        CHECK (autor IN ('USUARIO', 'PUBLICADOR')),

    CONSTRAINT chk_mensaje_estado
        CHECK (estado IN ('VISIBLE', 'OCULTO_POR_ADMIN'))
);

-- El hilo, en orden.
CREATE INDEX idx_mensaje_conversacion
    ON mensaje (conversacion_id, created_at);

-- El contador de no leídos de cada bandeja.
CREATE INDEX idx_mensaje_no_leidos
    ON mensaje (conversacion_id, autor)
    WHERE leido_at IS NULL;

-- El tope diario por usuario se cuenta contra esta tabla.
CREATE INDEX idx_mensaje_created_at
    ON mensaje (created_at);

-- ============================================================
-- El CHECK de reporte ENUMERA valores: cada objeto reportable nuevo
-- cuesta una migración (ya pasó en los scripts 29, 30, 34 y 35).
--
-- Reportar un MENSAJE es la única puerta por la que el admin puede
-- llegar a ver algo de una conversación privada, y ve ESE mensaje con
-- un contexto mínimo, nunca el hilo entero (decisión del plan).
-- ============================================================
ALTER TABLE reporte
    DROP CONSTRAINT chk_reporte_tipo_objeto;

ALTER TABLE reporte
    ADD CONSTRAINT chk_reporte_tipo_objeto
        CHECK (tipo_objeto IN (
            'IMAGEN',
            'PERFIL_PUBLICADOR',
            'ACTIVIDAD',
            'VALORACION',
            'PREGUNTA',
            'COMENTARIO',
            'NOVEDAD',
            'EVENTO',
            'MENSAJE'
        ));

COMMIT;

-- El tipo de notificación ('MENSAJE_NUEVO') NO se toca acá: esa
-- columna no tiene CHECK a propósito (decisión del script 28).

-- POST:
--   SELECT table_name FROM information_schema.tables
--    WHERE table_name IN ('conversacion','mensaje');
--   -- esperado: 2 filas
--   SELECT indexname FROM pg_indexes WHERE tablename = 'conversacion';
--   -- esperado: incluye uq_conversacion_con_actividad y
--   --           uq_conversacion_sin_actividad
--   SELECT pg_get_constraintdef(oid) FROM pg_constraint
--    WHERE conname = 'chk_reporte_tipo_objeto';
--   -- esperado: incluye MENSAJE
--
-- Rollback (solo ANTES de desplegar el backend que lo usa):
--   volver el CHECK de reporte a los 8 tipos anteriores;
--   DROP TABLE mensaje;
--   DROP TABLE conversacion;
