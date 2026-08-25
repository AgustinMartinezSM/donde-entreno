-- ============================================================
-- Script 38 — Grupos por actividad
-- ============================================================
-- Aditivo salvo el CHECK de reporte, que hay que reescribir porque
-- ENUMERA valores (mismo costo conocido de los scripts 29, 30, 34, 35
-- y 36).
-- Plan: docs/plan-grupos-actividad.md
--
-- El grupo es el espacio de una actividad PARA QUIENES VAN: el
-- publicador avisa, los miembros comentan y reaccionan. NO hay chat
-- libre miembro↔miembro en esta versión, y no es un recorte de tiempo:
-- es el canal más difícil de moderar del producto y conviene decidirlo
-- con grupos vivos (V2 del roadmap).
--
-- PRE:
--   SELECT COUNT(*) FROM information_schema.tables
--    WHERE table_name IN ('miembro_actividad','aviso_grupo',
--                         'comentario_aviso','me_gusta_aviso');
--   -- esperado: 0
--   SELECT pg_get_constraintdef(oid) FROM pg_constraint
--    WHERE conname = 'chk_reporte_tipo_objeto';
--   -- esperado: la lista de 9 tipos (sin AVISO_GRUPO ni COMENTARIO_GRUPO)

BEGIN;

-- ============================================================
-- La pertenencia, que HOY NO EXISTE en ninguna forma.
--
-- Se sigue al PUBLICADOR (seguimiento_publicador), se guarda una
-- actividad (favorito_actividad), se marca interés
-- (interes_actividad) y se hace check-in (entrenamiento_usuario):
-- ninguna de las cuatro es "ser miembro".
--
-- Y el check-in NO se usa como pertenencia automática a propósito:
-- marcar que entrenaste una vez es un acto distinto de sumarte a un
-- espacio donde vas a recibir avisos y donde otros ven lo que
-- escribís.
-- ============================================================
CREATE TABLE miembro_actividad (
    id BIGSERIAL PRIMARY KEY,

    usuario_id BIGINT NOT NULL,
    actividad_id BIGINT NOT NULL,

    -- SALIO en vez de borrar la fila: así "volver a entrar" no pierde
    -- la fecha original ni deja huecos raros en los conteos.
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_miembro_usuario
        FOREIGN KEY (usuario_id)
        REFERENCES usuario (id)
        ON DELETE CASCADE,

    CONSTRAINT fk_miembro_actividad
        FOREIGN KEY (actividad_id)
        REFERENCES actividad (id)
        ON DELETE CASCADE,

    CONSTRAINT uq_miembro_actividad UNIQUE (usuario_id, actividad_id),

    CONSTRAINT chk_miembro_estado
        CHECK (estado IN ('ACTIVO', 'SALIO'))
);

-- Los miembros de una actividad (para el fan-out del aviso).
CREATE INDEX idx_miembro_actividad_activos
    ON miembro_actividad (actividad_id)
    WHERE estado = 'ACTIVO';

-- ============================================================
-- El aviso del publicador a ese grupo. Mismo molde que `novedad`
-- (script 34): texto, foto opcional YA publicada, estado.
-- ============================================================
CREATE TABLE aviso_grupo (
    id BIGSERIAL PRIMARY KEY,

    actividad_id BIGINT NOT NULL,

    texto VARCHAR(1000) NOT NULL,

    imagen_id BIGINT NULL,

    estado VARCHAR(30) NOT NULL DEFAULT 'VISIBLE',

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_aviso_actividad
        FOREIGN KEY (actividad_id)
        REFERENCES actividad (id)
        ON DELETE CASCADE,

    CONSTRAINT fk_aviso_imagen
        FOREIGN KEY (imagen_id)
        REFERENCES imagen (id)
        ON DELETE SET NULL,

    CONSTRAINT chk_aviso_estado
        CHECK (estado IN (
            'VISIBLE',
            'OCULTO_POR_ADMIN',
            'ELIMINADO_POR_PUBLICADOR'
        ))
);

CREATE INDEX idx_aviso_actividad_fecha
    ON aviso_grupo (actividad_id, created_at DESC);

-- El tope diario por actividad se cuenta contra esta tabla.
CREATE INDEX idx_aviso_actividad_created
    ON aviso_grupo (actividad_id, created_at);

-- ============================================================
-- Los comentarios de los miembros. Mismo molde que
-- `comentario_imagen` (script 30), que ya trae estados y las dos vías
-- de moderación (publicador y admin).
-- ============================================================
CREATE TABLE comentario_aviso (
    id BIGSERIAL PRIMARY KEY,

    aviso_id BIGINT NOT NULL,
    usuario_id BIGINT NOT NULL,

    texto VARCHAR(500) NOT NULL,

    estado VARCHAR(30) NOT NULL DEFAULT 'VISIBLE',

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_comentario_aviso_aviso
        FOREIGN KEY (aviso_id)
        REFERENCES aviso_grupo (id)
        ON DELETE CASCADE,

    CONSTRAINT fk_comentario_aviso_usuario
        FOREIGN KEY (usuario_id)
        REFERENCES usuario (id)
        ON DELETE CASCADE,

    CONSTRAINT chk_comentario_aviso_estado
        CHECK (estado IN (
            'VISIBLE',
            'OCULTO_POR_PUBLICADOR',
            'OCULTO_POR_ADMIN',
            'ELIMINADO_POR_AUTOR'
        ))
);

CREATE INDEX idx_comentario_aviso_aviso
    ON comentario_aviso (aviso_id, created_at);

-- ============================================================
-- Reacciones al aviso (mismo molde que el script 37).
-- ============================================================
CREATE TABLE me_gusta_aviso (
    id BIGSERIAL PRIMARY KEY,

    usuario_id BIGINT NOT NULL,
    aviso_id BIGINT NOT NULL,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_me_gusta_aviso_usuario
        FOREIGN KEY (usuario_id)
        REFERENCES usuario (id)
        ON DELETE CASCADE,

    CONSTRAINT fk_me_gusta_aviso_aviso
        FOREIGN KEY (aviso_id)
        REFERENCES aviso_grupo (id)
        ON DELETE CASCADE,

    CONSTRAINT uq_me_gusta_aviso UNIQUE (usuario_id, aviso_id)
);

CREATE INDEX idx_me_gusta_aviso_aviso
    ON me_gusta_aviso (aviso_id);

-- ============================================================
-- Dos objetos reportables nuevos. El CHECK enumera, así que cuesta
-- migración (ya pasó en los scripts 29, 30, 34, 35 y 36).
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
            'MENSAJE',
            'AVISO_GRUPO',
            'COMENTARIO_GRUPO'
        ));

COMMIT;

-- POST:
--   SELECT table_name FROM information_schema.tables
--    WHERE table_name IN ('miembro_actividad','aviso_grupo',
--                         'comentario_aviso','me_gusta_aviso');
--   -- esperado: 4 filas
--   SELECT pg_get_constraintdef(oid) FROM pg_constraint
--    WHERE conname = 'chk_reporte_tipo_objeto';
--   -- esperado: incluye AVISO_GRUPO y COMENTARIO_GRUPO
--
-- Rollback (solo ANTES de desplegar el backend que lo usa):
--   volver el CHECK de reporte a los 9 tipos anteriores;
--   DROP TABLE me_gusta_aviso;
--   DROP TABLE comentario_aviso;
--   DROP TABLE aviso_grupo;
--   DROP TABLE miembro_actividad;
