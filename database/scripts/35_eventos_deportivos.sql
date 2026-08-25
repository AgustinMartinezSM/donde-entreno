-- ============================================================
-- Script 35 — Fase 9: eventos deportivos y calendario
-- ============================================================
-- Aditiva salvo el CHECK de reporte, que hay que reescribir porque
-- ENUMERA valores (mismo costo conocido de los scripts 29, 30 y 34).
-- Plan: docs/plan-fase9-eventos-calendario.md
--
-- Por qué una tabla nueva y no `horario_actividad`: esa tabla es
-- RECURRENCIA SEMANAL (dia_semana + hora_inicio/hora_fin) y no tiene
-- donde poner una fecha. Un evento es puntual y caduca; lo que se
-- repite todas las semanas ES una actividad y ya esta modelado.
--
-- Por qué `evento_deportivo` y no `evento`: en el código ya conviven
-- `EventoInteraccion` (tracking de clicks) y `FeedEvent` (el log del
-- feed). Un `Evento` a secas obligaría a mirar el import cada vez que
-- se lee una línea, para siempre.
--
-- PRE:
--   SELECT COUNT(*) FROM information_schema.tables
--    WHERE table_name IN ('evento_deportivo','interes_evento');
--   -- esperado: 0
--   SELECT COUNT(*) FROM information_schema.columns
--    WHERE table_name = 'feed_event' AND column_name = 'evento_deportivo_id';
--   -- esperado: 0
--   SELECT pg_get_constraintdef(oid) FROM pg_constraint
--    WHERE conname = 'chk_reporte_tipo_objeto';
--   -- esperado: la lista de 7 tipos (sin EVENTO)

BEGIN;

-- ============================================================
-- El evento: lo que pasa una vez y tiene fecha.
--
-- Cuelga SIEMPRE de un perfil publicador y OPCIONALMENTE de una
-- actividad. Si se asocia a una actividad hereda su contexto y
-- aparece en su detalle; si no —el caso del torneo del club, que no
-- es "una clase de karate"— el publicador elige deporte y sede.
-- ============================================================
CREATE TABLE evento_deportivo (
    id BIGSERIAL PRIMARY KEY,

    perfil_publicador_id BIGINT NOT NULL,

    -- Nullable a propósito: ver arriba. ON DELETE SET NULL para que
    -- borrar la actividad no se lleve puesto el evento.
    actividad_id BIGINT NULL,

    deporte_id BIGINT NOT NULL,

    -- Una sede del publicador. NOT NULL: de acá salen la ciudad y el
    -- barrio del calendario, y el punto de "Cómo llegar" (Fase 7).
    ubicacion_id BIGINT NOT NULL,

    -- Una foto YA publicada del publicador (mismo patrón que la
    -- novedad de la Fase 8: subir es un flujo propio).
    imagen_id BIGINT NULL,

    titulo VARCHAR(150) NOT NULL,
    slug VARCHAR(180) NOT NULL,
    descripcion VARCHAR(2000) NOT NULL,

    inicia_at TIMESTAMPTZ NOT NULL,
    -- Opcional: no todo evento sabe a qué hora termina.
    termina_at TIMESTAMPTZ NULL,

    -- Dato informativo, NO reserva. Reservar implica estados,
    -- cancelaciones, no-shows y pagos: el contacto sigue siendo
    -- WhatsApp, que es lo que ya funciona y ya se mide.
    cupo INTEGER NULL,

    es_gratis BOOLEAN NOT NULL DEFAULT false,
    precio_referencia NUMERIC(10,2) NULL,
    mostrar_precio BOOLEAN NOT NULL DEFAULT true,

    -- CANCELADO no es lo mismo que ELIMINADO: un evento cancelado
    -- sigue teniendo que decir que se canceló, porque su link ya
    -- circuló por WhatsApp.
    estado VARCHAR(30) NOT NULL DEFAULT 'PUBLICADO',

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_evento_perfil
        FOREIGN KEY (perfil_publicador_id)
        REFERENCES perfil_publicador (id)
        ON DELETE CASCADE,

    CONSTRAINT fk_evento_actividad
        FOREIGN KEY (actividad_id)
        REFERENCES actividad (id)
        ON DELETE SET NULL,

    CONSTRAINT fk_evento_deporte
        FOREIGN KEY (deporte_id)
        REFERENCES deporte (id),

    CONSTRAINT fk_evento_ubicacion
        FOREIGN KEY (ubicacion_id)
        REFERENCES ubicacion (id),

    CONSTRAINT fk_evento_imagen
        FOREIGN KEY (imagen_id)
        REFERENCES imagen (id)
        ON DELETE SET NULL,

    CONSTRAINT uq_evento_slug UNIQUE (slug),

    -- Catálogo CERRADO de estados, como en el resto de lo social.
    CONSTRAINT chk_evento_estado
        CHECK (estado IN (
            'PUBLICADO',
            'CANCELADO',
            'OCULTO_POR_ADMIN',
            'ELIMINADO_POR_PUBLICADOR'
        )),

    -- Si termina, que termine después de empezar.
    CONSTRAINT chk_evento_rango
        CHECK (termina_at IS NULL OR termina_at > inicia_at),

    CONSTRAINT chk_evento_cupo
        CHECK (cupo IS NULL OR cupo > 0),

    CONSTRAINT chk_evento_precio
        CHECK (precio_referencia IS NULL OR precio_referencia >= 0)
);

-- El acceso del calendario público: lo que viene, por fecha.
CREATE INDEX idx_evento_estado_inicia
    ON evento_deportivo (estado, inicia_at);

-- La agenda del publicador y la solapa de su perfil.
CREATE INDEX idx_evento_perfil_inicia
    ON evento_deportivo (perfil_publicador_id, inicia_at DESC);

-- El aviso "hay un evento próximo" en el detalle de la actividad.
CREATE INDEX idx_evento_actividad_inicia
    ON evento_deportivo (actividad_id, inicia_at)
    WHERE actividad_id IS NOT NULL;

-- ============================================================
-- "Me interesa": prueba social barata.
--
-- Tabla propia y NO `interes_actividad`: los estados de esa tabla
-- (QUIERO_PROBAR / YA_PROBE) no significan nada sobre un evento que
-- pasa una vez. Acá la fila existe o no existe.
-- ============================================================
CREATE TABLE interes_evento (
    id BIGSERIAL PRIMARY KEY,

    usuario_id BIGINT NOT NULL,
    evento_deportivo_id BIGINT NOT NULL,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_interes_evento_usuario
        FOREIGN KEY (usuario_id)
        REFERENCES usuario (id)
        ON DELETE CASCADE,

    CONSTRAINT fk_interes_evento_evento
        FOREIGN KEY (evento_deportivo_id)
        REFERENCES evento_deportivo (id)
        ON DELETE CASCADE,

    -- El UNIQUE es lo que hace idempotente al "me interesa".
    CONSTRAINT uq_interes_evento UNIQUE (usuario_id, evento_deportivo_id)
);

CREATE INDEX idx_interes_evento_evento
    ON interes_evento (evento_deportivo_id);

-- ============================================================
-- El evento en el feed (el feed sigue siendo el LOG; el contenido
-- vive en su tabla, igual que la novedad del script 34).
-- ============================================================
ALTER TABLE feed_event
    ADD COLUMN evento_deportivo_id BIGINT NULL;

ALTER TABLE feed_event
    ADD CONSTRAINT fk_feed_event_evento
        FOREIGN KEY (evento_deportivo_id)
        REFERENCES evento_deportivo (id)
        ON DELETE CASCADE;

-- ============================================================
-- El CHECK de reporte ENUMERA valores: cada objeto reportable nuevo
-- cuesta una migración (ya pasó en los scripts 29, 30 y 34).
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
            'EVENTO'
        ));

COMMIT;

-- El tipo de feed ('EVENTO_NUEVO') y el de notificación NO se tocan
-- acá: esas columnas no tienen CHECK a propósito, así que sumar
-- valores es código puro (decisión del script 28 y del 32).

-- POST:
--   SELECT table_name FROM information_schema.tables
--    WHERE table_name IN ('evento_deportivo','interes_evento');
--   -- esperado: 2 filas
--   SELECT column_name, is_nullable FROM information_schema.columns
--    WHERE table_name = 'evento_deportivo' ORDER BY ordinal_position;
--   -- esperado: actividad_id, imagen_id, termina_at, cupo y
--   --           precio_referencia en YES; el resto en NO
--   SELECT pg_get_constraintdef(oid) FROM pg_constraint
--    WHERE conname = 'chk_reporte_tipo_objeto';
--   -- esperado: incluye EVENTO
--   SELECT COUNT(*) FROM information_schema.columns
--    WHERE table_name = 'feed_event' AND column_name = 'evento_deportivo_id';
--   -- esperado: 1
--
-- Rollback (solo ANTES de desplegar el backend que lo usa):
--   volver el CHECK de reporte a los 7 tipos anteriores;
--   ALTER TABLE feed_event DROP CONSTRAINT fk_feed_event_evento;
--   ALTER TABLE feed_event DROP COLUMN evento_deportivo_id;
--   DROP TABLE interes_evento;
--   DROP TABLE evento_deportivo;
