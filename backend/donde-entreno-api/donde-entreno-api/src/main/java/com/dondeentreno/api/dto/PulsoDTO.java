package com.dondeentreno.api.dto;

import java.util.List;

/**
 * El pulso del producto: qué hay y qué se usó.
 *
 * No es un dashboard de métricas de negocio: es la respuesta a una
 * pregunta concreta —"de todo lo que construimos, ¿qué se está
 * usando?"— antes de construir más encima.
 */
public class PulsoDTO {

    private List<Bloque> bloques;

    public PulsoDTO() {
    }

    public PulsoDTO(List<Bloque> bloques) {
        this.bloques = bloques;
    }

    public List<Bloque> getBloques() {
        return bloques;
    }

    public void setBloques(List<Bloque> bloques) {
        this.bloques = bloques;
    }

    /** Un grupo de métricas ("Catálogo", "Social", "Uso"). */
    public static class Bloque {
        private String titulo;
        private List<Metrica> metricas;

        public Bloque() {
        }

        public Bloque(String titulo, List<Metrica> metricas) {
            this.titulo = titulo;
            this.metricas = metricas;
        }

        public String getTitulo() {
            return titulo;
        }

        public void setTitulo(String titulo) {
            this.titulo = titulo;
        }

        public List<Metrica> getMetricas() {
            return metricas;
        }

        public void setMetricas(List<Metrica> metricas) {
            this.metricas = metricas;
        }
    }

    /**
     * Un número con su etiqueta y, cuando aplica, cuántos de esos son
     * de los últimos 30 días: un total sin ventana no dice si algo
     * está vivo o si pasó una vez hace tres meses.
     */
    public static class Metrica {
        private String etiqueta;
        private Long total;
        private Long ultimos30Dias;

        public Metrica() {
        }

        public Metrica(String etiqueta, Long total, Long ultimos30Dias) {
            this.etiqueta = etiqueta;
            this.total = total;
            this.ultimos30Dias = ultimos30Dias;
        }

        public String getEtiqueta() {
            return etiqueta;
        }

        public void setEtiqueta(String etiqueta) {
            this.etiqueta = etiqueta;
        }

        public Long getTotal() {
            return total;
        }

        public void setTotal(Long total) {
            this.total = total;
        }

        public Long getUltimos30Dias() {
            return ultimos30Dias;
        }

        public void setUltimos30Dias(Long ultimos30Dias) {
            this.ultimos30Dias = ultimos30Dias;
        }
    }
}
