package com.dondeentreno.api.dto;

/**
 * Métricas de resumen del panel del publicador autenticado.
 *
 * Son conteos de solo lectura, siempre acotados al perfil del
 * publicador que hace la consulta (no exponen datos de terceros):
 *
 * - actividadesPublicadas: actividades propias activas y publicadas.
 * - actividadesPausadas: actividades propias en pausa voluntaria
 *   (fase 6) — invisibles para el público, gestionables por el dueño.
 * - solicitudesPublicacionPendientes: solicitudes de publicación
 *   propias que siguen en estado PENDIENTE.
 * - solicitudesCambioPendientes: solicitudes de cambio propias aún
 *   abiertas (PENDIENTE o EN_REVISION).
 * - imagenesPendientesModeracion: imágenes propias que esperan
 *   moderación del equipo.
 * - seguidores: usuarios que siguen al publicador (capa social).
 */
public class PublicadorMetricasDTO {

    private final long actividadesPublicadas;
    private final long actividadesPausadas;
    private final long solicitudesPublicacionPendientes;
    private final long solicitudesCambioPendientes;
    private final long imagenesPendientesModeracion;
    private final long seguidores;

    /*
      Tracking de interacciones (Fase 2 social): agregados anónimos de
      los últimos 30 días sobre TODAS las actividades del perfil.
    */
    private final long vistas30Dias;
    private final long contactosWhatsapp30Dias;

    /** Fase 3: cuántas personas quieren probar sus actividades (agregado). */
    private final long quierenProbar;

    public PublicadorMetricasDTO(
            long actividadesPublicadas,
            long actividadesPausadas,
            long solicitudesPublicacionPendientes,
            long solicitudesCambioPendientes,
            long imagenesPendientesModeracion,
            long seguidores,
            long vistas30Dias,
            long contactosWhatsapp30Dias,
            long quierenProbar
    ) {
        this.actividadesPublicadas = actividadesPublicadas;
        this.actividadesPausadas = actividadesPausadas;
        this.solicitudesPublicacionPendientes = solicitudesPublicacionPendientes;
        this.solicitudesCambioPendientes = solicitudesCambioPendientes;
        this.imagenesPendientesModeracion = imagenesPendientesModeracion;
        this.seguidores = seguidores;
        this.vistas30Dias = vistas30Dias;
        this.contactosWhatsapp30Dias = contactosWhatsapp30Dias;
        this.quierenProbar = quierenProbar;
    }

    public long getActividadesPublicadas() {
        return actividadesPublicadas;
    }

    public long getActividadesPausadas() {
        return actividadesPausadas;
    }

    public long getSolicitudesPublicacionPendientes() {
        return solicitudesPublicacionPendientes;
    }

    public long getSolicitudesCambioPendientes() {
        return solicitudesCambioPendientes;
    }

    public long getImagenesPendientesModeracion() {
        return imagenesPendientesModeracion;
    }

    public long getSeguidores() {
        return seguidores;
    }

    public long getVistas30Dias() {
        return vistas30Dias;
    }

    public long getContactosWhatsapp30Dias() {
        return contactosWhatsapp30Dias;
    }

    public long getQuierenProbar() {
        return quierenProbar;
    }
}
