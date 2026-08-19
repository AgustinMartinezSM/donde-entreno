package com.dondeentreno.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Límites de las imágenes que sube el publicador (fase 2 del bloque
 * visual). Con defaults en código: sin configurar nada, la app arranca
 * con estos valores; las variables de entorno solo hacen falta para
 * cambiarlos.
 *
 * - max-galeria-por-actividad: cuántas fotos GALERIA "que van a
 *   existir" (activas + pendientes) admite una actividad.
 * - max-pendientes-por-actividad: tope anti-flood de la cola de
 *   moderación por actividad.
 */
@ConfigurationProperties(prefix = "dondeentreno.media")
public class MediaProperties {

    private int maxGaleriaPorActividad = 12;
    private int maxPendientesPorActividad = 15;

    public int getMaxGaleriaPorActividad() {
        return maxGaleriaPorActividad;
    }

    public void setMaxGaleriaPorActividad(int maxGaleriaPorActividad) {
        this.maxGaleriaPorActividad = maxGaleriaPorActividad;
    }

    public int getMaxPendientesPorActividad() {
        return maxPendientesPorActividad;
    }

    public void setMaxPendientesPorActividad(int maxPendientesPorActividad) {
        this.maxPendientesPorActividad = maxPendientesPorActividad;
    }
}
