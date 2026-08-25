package com.dondeentreno.api.service;

import com.dondeentreno.api.dto.PulsoDTO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * El pulso del producto (Fase 10, paso 0).
 *
 * Responde una pregunta concreta antes de construir más encima: de
 * todo lo que se agregó en las fases sociales, **¿qué se está usando?**
 *
 * Una sola query nativa en vez de veinte `count()` repartidos por diez
 * repositorios: es una pantalla de diagnóstico que se abre de vez en
 * cuando, y así el conjunto entero se lee de un vistazo y se cambia en
 * un solo lugar. El test la ejerce contra la base real, así que si una
 * tabla cambia de nombre se entera acá y no en producción.
 */
@Service
public class PulsoService {

    /**
     * Cada entrada es `clave -> SQL que devuelve un número`.
     *
     * Las que terminan en `_30d` son la misma cuenta acotada a los
     * últimos 30 días: un total sin ventana no distingue algo vivo de
     * algo que pasó una vez hace meses.
     */
    private static final Map<String, String> CONTEOS = new LinkedHashMap<>();

    static {
        /* Catálogo: lo que el producto tiene para ofrecer. */
        CONTEOS.put("actividades", """
                SELECT COUNT(*) FROM actividad
                 WHERE activa = true AND estado_publicacion = 'PUBLICADA'
                   AND deleted_at IS NULL""");
        CONTEOS.put("publicadores", """
                SELECT COUNT(*) FROM perfil_publicador
                 WHERE activo = true AND deleted_at IS NULL""");
        CONTEOS.put("fotos", """
                SELECT COUNT(*) FROM imagen
                 WHERE activa = true AND estado_moderacion = 'APROBADA'""");
        CONTEOS.put("usuarios", "SELECT COUNT(*) FROM usuario WHERE activo = true");
        CONTEOS.put("usuarios_30d", """
                SELECT COUNT(*) FROM usuario
                 WHERE activo = true AND created_at >= NOW() - INTERVAL '30 days'""");

        /* Social: lo que se construyó en las fases 4 a 9. */
        CONTEOS.put("novedades", "SELECT COUNT(*) FROM novedad WHERE estado = 'VISIBLE'");
        CONTEOS.put("novedades_30d", """
                SELECT COUNT(*) FROM novedad
                 WHERE estado = 'VISIBLE' AND created_at >= NOW() - INTERVAL '30 days'""");
        CONTEOS.put("eventos", """
                SELECT COUNT(*) FROM evento_deportivo WHERE estado = 'PUBLICADO'""");
        CONTEOS.put("eventos_futuros", """
                SELECT COUNT(*) FROM evento_deportivo
                 WHERE estado = 'PUBLICADO' AND inicia_at >= NOW()""");
        CONTEOS.put("conversaciones", "SELECT COUNT(*) FROM conversacion");
        CONTEOS.put("conversaciones_30d", """
                SELECT COUNT(*) FROM conversacion
                 WHERE created_at >= NOW() - INTERVAL '30 days'""");
        CONTEOS.put("mensajes", "SELECT COUNT(*) FROM mensaje WHERE estado = 'VISIBLE'");
        CONTEOS.put("grupos_con_miembros", """
                SELECT COUNT(DISTINCT actividad_id) FROM miembro_actividad
                 WHERE estado = 'ACTIVO'""");
        CONTEOS.put("miembros", """
                SELECT COUNT(*) FROM miembro_actividad WHERE estado = 'ACTIVO'""");
        CONTEOS.put("avisos_grupo", """
                SELECT COUNT(*) FROM aviso_grupo WHERE estado = 'VISIBLE'""");
        CONTEOS.put("comentarios_fotos", """
                SELECT COUNT(*) FROM comentario_imagen WHERE estado = 'VISIBLE'""");
        CONTEOS.put("valoraciones", """
                SELECT COUNT(*) FROM valoracion WHERE estado = 'VISIBLE'""");
        CONTEOS.put("seguimientos", "SELECT COUNT(*) FROM seguimiento_publicador");
        CONTEOS.put("reportes_pendientes", """
                SELECT COUNT(*) FROM reporte WHERE estado = 'PENDIENTE'""");

        /* Uso real: el tracking anónimo. */
        CONTEOS.put("interacciones_7d", """
                SELECT COUNT(*) FROM evento_interaccion
                 WHERE created_at >= NOW() - INTERVAL '7 days'""");
        CONTEOS.put("interacciones_30d", """
                SELECT COUNT(*) FROM evento_interaccion
                 WHERE created_at >= NOW() - INTERVAL '30 days'""");
        CONTEOS.put("feed_eventos_30d", """
                SELECT COUNT(*) FROM feed_event
                 WHERE created_at >= NOW() - INTERVAL '30 days'""");
    }

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional(readOnly = true)
    public PulsoDTO obtener() {
        Map<String, Long> valores = new LinkedHashMap<>();

        for (Map.Entry<String, String> conteo : CONTEOS.entrySet()) {
            valores.put(conteo.getKey(), contar(conteo.getValue()));
        }

        List<PulsoDTO.Bloque> bloques = new ArrayList<>();

        bloques.add(new PulsoDTO.Bloque("Catálogo", List.of(
                metrica("Actividades publicadas", valores, "actividades", null),
                metrica("Publicadores activos", valores, "publicadores", null),
                metrica("Fotos aprobadas", valores, "fotos", null),
                metrica("Usuarios", valores, "usuarios", "usuarios_30d")
        )));

        bloques.add(new PulsoDTO.Bloque("Lo social", List.of(
                metrica("Novedades", valores, "novedades", "novedades_30d"),
                metrica("Eventos (futuros)", valores, "eventos_futuros", null),
                metrica("Conversaciones", valores, "conversaciones", "conversaciones_30d"),
                metrica("Mensajes", valores, "mensajes", null),
                metrica("Grupos con miembros", valores, "grupos_con_miembros", null),
                metrica("Miembros de grupos", valores, "miembros", null),
                metrica("Avisos de grupo", valores, "avisos_grupo", null),
                metrica("Comentarios en fotos", valores, "comentarios_fotos", null),
                metrica("Valoraciones", valores, "valoraciones", null),
                metrica("Seguimientos", valores, "seguimientos", null)
        )));

        bloques.add(new PulsoDTO.Bloque("Uso y moderación", List.of(
                metrica("Interacciones (7 días)", valores, "interacciones_7d", null),
                metrica("Interacciones (30 días)", valores, "interacciones_30d", null),
                metrica("Hechos del feed (30 días)", valores, "feed_eventos_30d", null),
                metrica("Reportes pendientes", valores, "reportes_pendientes", null)
        )));

        return new PulsoDTO(bloques);
    }

    private PulsoDTO.Metrica metrica(
            String etiqueta,
            Map<String, Long> valores,
            String clave,
            String claveVentana
    ) {
        return new PulsoDTO.Metrica(
                etiqueta,
                valores.getOrDefault(clave, 0L),
                claveVentana != null ? valores.getOrDefault(claveVentana, 0L) : null
        );
    }

    /**
     * Cada conteo va aislado: si una tabla no existiera todavía en un
     * entorno, esa métrica sale en 0 y el panel se dibuja igual. Un
     * diagnóstico que se cae entero por un número es inútil.
     */
    private Long contar(String sql) {
        try {
            Object resultado = entityManager.createNativeQuery(sql).getSingleResult();

            if (resultado instanceof Number numero) {
                return numero.longValue();
            }

            if (resultado instanceof BigInteger entero) {
                return entero.longValue();
            }

            return 0L;
        } catch (RuntimeException excepcion) {
            return 0L;
        }
    }
}
